const BADGE_ID = "textlens-youtube-detector";
const SETTINGS_MODAL_ID = "textlens-settings-modal";
const SUBTITLE_ID = "textlens-youtube-subtitle";
const BADGE_MINIMIZED_KEY = "textLensBadgeMinimized";
const SUBTITLE_POSITION_KEY = "textLensSubtitlePosition";
const SUBTITLE_SETTINGS_KEY = "textLensSubtitleSettings";
const MANUAL_SUBTITLES_KEY = "textLensManualSubtitles";
const SETTINGS_POSITION_KEY = "textLensSettingsPosition";
const TEXT_COLOR_PRESETS = ["#ffd000", "#ffffff", "#f8fafc", "#38bdf8", "#4ade80", "#fb7185"];
const BACKGROUND_COLOR_PRESETS = ["#000000", "#111827", "#1f2937", "#3f2f00", "#052e2b", "#3b0764"];

let lastVideoId = "";
let loadedSubtitleVideoId = "";
let subtitleCues = [];
let activeCueIndex = -1;
let subtitleDrag = null;
let subtitlePosition = {
  leftPercent: 50,
  topPercent: 76
};
let settingsDrag = null;
let settingsPosition = {
  left: null,
  top: null
};
let isBadgeMinimized = false;
let subtitleSettings = {
  backgroundColor: "#000000",
  fontSize: 34,
  opacity: 68,
  textColor: "#ffd000"
};

function extractVideoIdFromLocation() {
  const url = new URL(window.location.href);
  if (url.pathname !== "/watch") return "";
  return url.searchParams.get("v") || "";
}

function canonicalVideoUrl(videoId) {
  return `https://www.youtube.com/watch?v=${encodeURIComponent(videoId)}`;
}

function getVideoTitle() {
  return (
    document.querySelector("h1.ytd-watch-metadata yt-formatted-string")?.textContent?.trim() ||
    document.querySelector("h1.title yt-formatted-string")?.textContent?.trim() ||
    document.title.replace(/\s+-\s+YouTube$/, "").trim()
  );
}

function getVideoElement() {
  return document.querySelector("video");
}

function formatTime(seconds) {
  if (!Number.isFinite(seconds) || seconds < 0) return "0:00";

  const total = Math.floor(seconds);
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const secs = total % 60;

  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
  }

  return `${minutes}:${String(secs).padStart(2, "0")}`;
}

function parseTimestamp(value) {
  const match = value.trim().match(/(?:(\d+):)?(\d{1,2}):(\d{1,2})[,.](\d{1,3})/);
  if (!match) return null;

  const hours = Number(match[1] || 0);
  const minutes = Number(match[2] || 0);
  const seconds = Number(match[3] || 0);
  const millis = Number(match[4].padEnd(3, "0"));

  return hours * 3600 + minutes * 60 + seconds + millis / 1000;
}

function stripCueSettings(value) {
  return value.trim().split(/\s+/)[0] || "";
}

function parseSrt(text) {
  return text
    .replace(/^\uFEFF/, "")
    .replace(/\r\n/g, "\n")
    .replace(/\r/g, "\n")
    .trim()
    .split(/\n{2,}/)
    .map((block) => {
      const lines = block
        .split("\n")
        .map((line) => line.trim())
        .filter(Boolean);
      const timeLineIndex = lines.findIndex((line) => line.includes("-->"));
      if (timeLineIndex === -1) return null;

      const [rawStart, rawEnd] = lines[timeLineIndex].split("-->");
      const start = parseTimestamp(stripCueSettings(rawStart));
      const end = parseTimestamp(stripCueSettings(rawEnd));
      if (start === null || end === null || end <= start) return null;

      const cueText = lines
        .slice(timeLineIndex + 1)
        .join("\n")
        .replace(/<[^>]+>/g, "")
        .trim();
      if (!cueText) return null;

      return { start, end, text: cueText };
    })
    .filter(Boolean)
    .sort((a, b) => a.start - b.start);
}

function readPlaybackState() {
  const video = getVideoElement();
  if (!video) {
    return {
      duration: 0,
      isFound: false,
      isPaused: true,
      playbackRate: 1,
      time: 0
    };
  }

  return {
    duration: Number.isFinite(video.duration) ? video.duration : 0,
    isFound: true,
    isPaused: video.paused,
    playbackRate: video.playbackRate || 1,
    time: video.currentTime || 0
  };
}

function ensureBadge() {
  let badge = document.getElementById(BADGE_ID);
  if (badge) return badge;

  badge = document.createElement("div");
  badge.id = BADGE_ID;
  badge.innerHTML = `
    <div class="textlens-card">
      <button class="textlens-mini" type="button" data-role="expand" title="Expand TextLens">T</button>
      <div class="textlens-expanded">
        <div class="textlens-main">
          <div class="textlens-label">TextLens detected</div>
          <div class="textlens-link" data-role="link"></div>
          <div class="textlens-live">
            <span data-role="state">waiting</span>
            <span data-role="time">0:00 / 0:00</span>
            <span data-role="rate">1x</span>
          </div>
        </div>
        <div class="textlens-actions">
          <button type="button" data-role="loadSrt">Load SRT</button>
          <input class="textlens-badge-file-input" data-role="badgeSrtFile" type="file" accept=".srt,text/plain" />
          <button class="textlens-icon-button" type="button" data-role="settings" title="Subtitle settings" aria-label="Subtitle settings">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 8.4a3.6 3.6 0 1 0 0 7.2 3.6 3.6 0 0 0 0-7.2Zm8.2 3.6c0-.5 0-1-.1-1.4l2-1.5-2-3.5-2.4 1a8.2 8.2 0 0 0-2.4-1.4L15 2h-4l-.4 2.8c-.8.3-1.6.7-2.4 1.4l-2.4-1-2 3.5 2 1.5a9 9 0 0 0 0 2.8l-2 1.5 2 3.5 2.4-1c.7.6 1.5 1 2.4 1.4L11 22h4l.4-2.8c.8-.3 1.6-.7 2.4-1.4l2.4 1 2-3.5-2-1.5c.1-.5.1-1 .1-1.4Z" fill="currentColor"/>
            </svg>
          </button>
          <button class="textlens-icon-button" type="button" data-role="minimize" title="Minimize" aria-label="Minimize">−</button>
        </div>
      </div>
    </div>
  `;

  const style = document.createElement("style");
  style.textContent = `
    @import url("https://fonts.googleapis.com/css2?family=Vazirmatn:wght@400;500;600&display=swap");
    #${BADGE_ID} {
      position: fixed;
      right: 18px;
      bottom: 18px;
      z-index: 2147483647;
      font-family: Inter, Arial, sans-serif;
      color: #fff;
      transition:
        opacity 180ms ease,
        transform 220ms cubic-bezier(0.2, 0.9, 0.2, 1);
    }
    #${BADGE_ID}.is-minimized {
      opacity: 0.25;
      transform: scale(0.96);
    }
    #${BADGE_ID}.is-minimized:hover {
      opacity: 1;
      transform: scale(1);
    }
    #${BADGE_ID} .textlens-card {
      display: flex;
      align-items: center;
      gap: 12px;
      min-height: 48px;
      max-width: 520px;
      padding: 12px;
      background: rgba(8, 8, 8, 0.92);
      border: 1px solid rgba(255, 208, 0, 0.62);
      border-radius: 14px;
      box-shadow: 0 14px 42px rgba(0, 0, 0, 0.38);
      transition:
        width 240ms cubic-bezier(0.2, 0.9, 0.2, 1),
        max-width 240ms cubic-bezier(0.2, 0.9, 0.2, 1),
        height 240ms cubic-bezier(0.2, 0.9, 0.2, 1),
        min-height 240ms cubic-bezier(0.2, 0.9, 0.2, 1),
        padding 240ms cubic-bezier(0.2, 0.9, 0.2, 1),
        border-radius 240ms cubic-bezier(0.2, 0.9, 0.2, 1),
        gap 180ms ease,
        box-shadow 180ms ease;
    }
    #${BADGE_ID} .textlens-expanded {
      display: flex;
      align-items: center;
      gap: 12px;
      max-width: 490px;
      overflow: hidden;
      opacity: 1;
      transition:
        max-width 220ms cubic-bezier(0.2, 0.9, 0.2, 1),
        opacity 140ms ease;
    }
    #${BADGE_ID}.is-minimized .textlens-card {
      width: 38px;
      height: 38px;
      min-height: 38px;
      max-width: 38px;
      justify-content: center;
      gap: 0;
      padding: 0;
      border-radius: 50%;
    }
    #${BADGE_ID}.is-minimized .textlens-expanded {
      max-width: 0;
      opacity: 0;
      pointer-events: none;
    }
    #${BADGE_ID}:not(.is-minimized) .textlens-mini {
      width: 0;
      opacity: 0;
      pointer-events: none;
      transform: scale(0.72);
    }
    #${BADGE_ID} .textlens-mini {
      width: 38px;
      height: 38px;
      padding: 0;
      border-radius: 50%;
      overflow: hidden;
      font-size: 20px;
      line-height: 1;
      opacity: 1;
      transform: scale(1);
      transition:
        width 220ms cubic-bezier(0.2, 0.9, 0.2, 1),
        opacity 140ms ease,
        transform 220ms cubic-bezier(0.2, 0.9, 0.2, 1);
    }
    #${BADGE_ID} .textlens-main {
      display: grid;
      gap: 5px;
      min-width: 0;
    }
    #${BADGE_ID} .textlens-label {
      color: #ffd000;
      font-size: 12px;
      font-weight: 900;
      text-transform: uppercase;
    }
    #${BADGE_ID} .textlens-link {
      max-width: 390px;
      overflow: hidden;
      color: #f5f5f5;
      font-size: 12px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    #${BADGE_ID} .textlens-live {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      color: #f5f5f5;
      font-size: 12px;
      font-weight: 800;
    }
    #${BADGE_ID} .textlens-live span {
      padding: 4px 7px;
      background: rgba(255, 255, 255, 0.09);
      border: 1px solid rgba(255, 255, 255, 0.1);
    }
    #${BADGE_ID} button {
      border: 0;
      background: #ffd000;
      color: #080808;
      cursor: pointer;
      font-size: 12px;
      font-weight: 900;
      padding: 8px 10px;
    }
    #${BADGE_ID} .textlens-actions {
      display: flex;
      align-items: center;
      gap: 7px;
    }
    #${BADGE_ID} .textlens-badge-file-input {
      display: none;
    }
    #${BADGE_ID} .textlens-icon-button {
      display: grid;
      width: 32px;
      height: 32px;
      place-items: center;
      padding: 0;
      font-size: 18px;
      line-height: 1;
    }
    #${BADGE_ID} .textlens-icon-button svg {
      width: 17px;
      height: 17px;
    }
    #${SETTINGS_MODAL_ID} {
      position: fixed;
      inset: 0;
      z-index: 2147483647;
      display: grid;
      place-items: center;
      padding: 24px;
      background: rgba(0, 0, 0, 0.48);
      font-family: Inter, Arial, sans-serif;
      color: #fff;
    }
    #${SETTINGS_MODAL_ID} {
      pointer-events: none;
    }
    #${SETTINGS_MODAL_ID} .textlens-modal-card {
      width: min(520px, 100%);
      padding: 22px;
      background: #101010;
      border: 1px solid rgba(255, 208, 0, 0.72);
      box-shadow: 0 24px 80px rgba(0, 0, 0, 0.46);
    }
    #${SETTINGS_MODAL_ID} .textlens-modal-card {
      width: min(560px, 100%);
      pointer-events: auto;
      border-radius: 18px;
      background:
        radial-gradient(circle at 18% 0%, rgba(255, 208, 0, 0.18), transparent 28%),
        #101010;
      animation: textlens-modal-in 220ms cubic-bezier(0.2, 0.9, 0.2, 1) both;
    }
    #${SETTINGS_MODAL_ID} .textlens-settings-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 14px;
      margin: -6px -6px 10px;
      padding: 8px 6px;
      cursor: move;
      user-select: none;
      -webkit-user-select: none;
    }
    #${SETTINGS_MODAL_ID} button {
      border: 0;
      background: #ffd000;
      color: #080808;
      cursor: pointer;
      font-family: Inter, Arial, sans-serif;
      font-size: 12px;
      font-weight: 900;
      padding: 9px 12px;
    }
    #${SETTINGS_MODAL_ID} .textlens-close-button {
      display: grid;
      width: 32px;
      height: 32px;
      place-items: center;
      padding: 0;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.12);
      color: #fff;
      font-size: 20px;
      line-height: 1;
    }
    #${SETTINGS_MODAL_ID} .textlens-close-button:hover {
      background: #ffd000;
      color: #080808;
    }
    #${SETTINGS_MODAL_ID} .textlens-modal-title {
      margin: 0 0 10px;
      color: #ffd000;
      font-size: 14px;
      font-weight: 900;
      text-transform: uppercase;
    }
    #${SETTINGS_MODAL_ID} .textlens-settings-grid {
      display: grid;
      gap: 14px;
      margin-top: 18px;
    }
    #${SETTINGS_MODAL_ID} label {
      display: grid;
      gap: 7px;
      color: #cfcfcf;
      font-size: 13px;
      font-weight: 800;
    }
    #${SETTINGS_MODAL_ID} .textlens-swatch-row {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    #${SETTINGS_MODAL_ID} .textlens-swatch {
      width: 30px;
      height: 30px;
      border: 2px solid rgba(255, 255, 255, 0.12);
      border-radius: 999px;
      cursor: pointer;
      box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.5);
    }
    #${SETTINGS_MODAL_ID} .textlens-swatch.is-active {
      border-color: #ffd000;
      box-shadow:
        0 0 0 2px rgba(255, 208, 0, 0.22),
        inset 0 0 0 1px rgba(0, 0, 0, 0.5);
    }
    #${SETTINGS_MODAL_ID} input[type="range"] {
      width: 100%;
      accent-color: #ffd000;
    }
    #${SETTINGS_MODAL_ID} input[type="color"] {
      width: 64px;
      height: 34px;
      border: 1px solid rgba(255, 255, 255, 0.14);
      background: #060606;
      padding: 4px;
    }
    #${SETTINGS_MODAL_ID} .textlens-setting-value {
      color: #ffd000;
      font-size: 12px;
      font-weight: 900;
    }
    #${SETTINGS_MODAL_ID} .textlens-position-action {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 14px;
      padding: 12px;
      border: 1px solid rgba(255, 208, 0, 0.28);
      background: rgba(255, 208, 0, 0.06);
    }
    #${SETTINGS_MODAL_ID} .textlens-position-action span {
      color: #cfcfcf;
      font-size: 13px;
      font-weight: 800;
    }
    #${SETTINGS_MODAL_ID} .textlens-modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      margin-top: 18px;
    }
    @keyframes textlens-backdrop-in {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes textlens-modal-in {
      from {
        opacity: 0;
        transform: translateY(18px) scale(0.96);
      }
      to {
        opacity: 1;
        transform: translateY(0) scale(1);
      }
    }
    #${SUBTITLE_ID} {
      position: absolute;
      left: 50%;
      top: 76%;
      z-index: 2147483647;
      max-width: min(82%, 960px);
      transform: translate(-50%, -50%);
      padding: 8px 18px 10px;
      background: rgba(0, 0, 0, 0.68);
      color: #ffd000;
      font-family: "Vazirmatn", Vazir, Tahoma, Arial, sans-serif;
      font-size: 34px;
      font-weight: 400;
      line-height: 1.35;
      direction: rtl;
      unicode-bidi: plaintext;
      text-align: center;
      text-shadow:
        0 2px 8px rgba(0, 0, 0, 0.95),
        0 0 2px rgba(0, 0, 0, 0.95);
      cursor: grab;
      pointer-events: auto;
      user-select: none;
      -webkit-user-select: none;
      touch-action: none;
    }
    #${SUBTITLE_ID}:active {
      cursor: grabbing;
    }
  `;

  document.documentElement.appendChild(style);
  document.documentElement.appendChild(badge);

  badge.querySelector('[data-role="loadSrt"]').addEventListener("click", () => {
    badge.querySelector('[data-role="badgeSrtFile"]').click();
  });
  badge.querySelector('[data-role="badgeSrtFile"]').addEventListener("change", async (event) => {
    await loadManualSrtFromInput(event.target.files?.[0], {
      triggerButton: badge.querySelector('[data-role="loadSrt"]')
    });
    event.target.value = "";
  });
  badge.querySelector('[data-role="settings"]').addEventListener("click", () => {
    showSettingsModal();
  });
  badge.querySelector('[data-role="minimize"]').addEventListener("click", async () => {
    isBadgeMinimized = true;
    applyBadgeMinimizedState();
    await saveBadgeMinimizedState();
  });
  badge.querySelector('[data-role="expand"]').addEventListener("click", async () => {
    isBadgeMinimized = false;
    applyBadgeMinimizedState();
    await saveBadgeMinimizedState();
  });

  applyBadgeMinimizedState();

  return badge;
}

function applyBadgeMinimizedState() {
  document.getElementById(BADGE_ID)?.classList.toggle("is-minimized", isBadgeMinimized);
}

async function saveBadgeMinimizedState() {
  await chrome.storage.local.set({
    [BADGE_MINIMIZED_KEY]: isBadgeMinimized
  });
}

async function restoreBadgeMinimizedState() {
  const stored = await chrome.storage.local.get(BADGE_MINIMIZED_KEY);
  isBadgeMinimized = Boolean(stored[BADGE_MINIMIZED_KEY]);
}

function hexToRgb(hex) {
  const normalized = hex.replace("#", "");
  const value = Number.parseInt(normalized, 16);
  return {
    b: value & 255,
    g: (value >> 8) & 255,
    r: (value >> 16) & 255
  };
}

function applySubtitleSettings() {
  const subtitle = document.getElementById(SUBTITLE_ID);
  if (!subtitle) return;

  const background = hexToRgb(subtitleSettings.backgroundColor);
  subtitle.style.background =
    `rgba(${background.r}, ${background.g}, ${background.b}, ${subtitleSettings.opacity / 100})`;
  subtitle.style.color = subtitleSettings.textColor;
  subtitle.style.fontSize = `${subtitleSettings.fontSize}px`;
}

async function saveSubtitleSettings() {
  await chrome.storage.local.set({
    [SUBTITLE_SETTINGS_KEY]: subtitleSettings
  });
}

async function restoreSubtitleSettings() {
  const stored = await chrome.storage.local.get(SUBTITLE_SETTINGS_KEY);
  const value = stored[SUBTITLE_SETTINGS_KEY];
  if (!value) return;

  subtitleSettings = {
    backgroundColor: value.backgroundColor || subtitleSettings.backgroundColor,
    fontSize: Number.isFinite(value.fontSize) ? value.fontSize : subtitleSettings.fontSize,
    opacity: Number.isFinite(value.opacity) ? value.opacity : subtitleSettings.opacity,
    textColor: value.textColor || subtitleSettings.textColor
  };
}

function applySettingsModalPosition(modal) {
  const card = modal.querySelector(".textlens-modal-card");
  if (!card || settingsPosition.left === null || settingsPosition.top === null) return;

  modal.style.placeItems = "start";
  card.style.position = "fixed";
  card.style.left = `${settingsPosition.left}px`;
  card.style.top = `${settingsPosition.top}px`;
}

async function saveSettingsPosition() {
  await chrome.storage.local.set({
    [SETTINGS_POSITION_KEY]: settingsPosition
  });
}

async function restoreSettingsPosition() {
  const stored = await chrome.storage.local.get(SETTINGS_POSITION_KEY);
  const value = stored[SETTINGS_POSITION_KEY];
  if (
    value &&
    (value.left === null || Number.isFinite(value.left)) &&
    (value.top === null || Number.isFinite(value.top))
  ) {
    settingsPosition = {
      left: value.left,
      top: value.top
    };
  }
}

function attachSettingsDrag(modal) {
  const card = modal.querySelector(".textlens-modal-card");
  const handle = modal.querySelector('[data-role="dragHandle"]');
  if (!card || !handle) return;

  let drag = null;

  handle.addEventListener("pointerdown", (event) => {
    if (event.target.dataset.role === "close") return;
    const rect = card.getBoundingClientRect();
    drag = {
      offsetX: event.clientX - rect.left,
      offsetY: event.clientY - rect.top,
      pointerId: event.pointerId
    };
    handle.setPointerCapture(event.pointerId);
    modal.style.placeItems = "start";
    card.style.position = "fixed";
    card.style.left = `${rect.left}px`;
    card.style.top = `${rect.top}px`;
  });

  handle.addEventListener("pointermove", (event) => {
    if (!drag || drag.pointerId !== event.pointerId) return;
    const rect = card.getBoundingClientRect();
    const left = clamp(event.clientX - drag.offsetX, 12, window.innerWidth - rect.width - 12);
    const top = clamp(event.clientY - drag.offsetY, 12, window.innerHeight - rect.height - 12);
    card.style.left = `${left}px`;
    card.style.top = `${top}px`;
    settingsPosition = { left, top };
  });

  handle.addEventListener("pointerup", async (event) => {
    if (!drag || drag.pointerId !== event.pointerId) return;
    handle.releasePointerCapture(event.pointerId);
    drag = null;
    await saveSettingsPosition();
  });

  handle.addEventListener("pointercancel", () => {
    drag = null;
  });
}

function showSettingsModal() {
  document.getElementById(SETTINGS_MODAL_ID)?.remove();

  const modal = document.createElement("div");
  modal.id = SETTINGS_MODAL_ID;
  modal.innerHTML = `
    <div class="textlens-modal-card" role="dialog" aria-modal="true">
      <div class="textlens-settings-header" data-role="dragHandle">
        <p class="textlens-modal-title">Subtitle settings</p>
        <button class="textlens-close-button" type="button" data-role="close" aria-label="Close">×</button>
      </div>
      <div class="textlens-settings-grid">
        <label>
          Font size
          <span class="textlens-setting-value" data-role="fontSizeValue">${subtitleSettings.fontSize}px</span>
          <input data-role="fontSize" type="range" min="18" max="64" step="1" value="${subtitleSettings.fontSize}" />
        </label>
        <label>
          Text color
          <div class="textlens-swatch-row" data-role="textColorSwatches">
            ${TEXT_COLOR_PRESETS.map((color) => `<button class="textlens-swatch" type="button" data-role="textColorSwatch" data-color="${color}" style="background:${color}"></button>`).join("")}
          </div>
          <input data-role="textColor" type="color" value="${subtitleSettings.textColor}" />
        </label>
        <label>
          Background color
          <div class="textlens-swatch-row" data-role="backgroundColorSwatches">
            ${BACKGROUND_COLOR_PRESETS.map((color) => `<button class="textlens-swatch" type="button" data-role="backgroundColorSwatch" data-color="${color}" style="background:${color}"></button>`).join("")}
          </div>
          <input data-role="backgroundColor" type="color" value="${subtitleSettings.backgroundColor}" />
        </label>
        <label>
          Background transparency
          <span class="textlens-setting-value" data-role="opacityValue">${subtitleSettings.opacity}%</span>
          <input data-role="opacity" type="range" min="0" max="100" step="1" value="${subtitleSettings.opacity}" />
        </label>
        <div class="textlens-position-action">
          <span>Subtitle position</span>
          <button type="button" data-role="centerSubtitle">Center horizontally</button>
        </div>
      </div>
    </div>
  `;

  applySettingsModalPosition(modal);

  const syncSwatches = () => {
    modal.querySelectorAll('[data-role="textColorSwatch"]').forEach((button) => {
      button.classList.toggle("is-active", button.dataset.color === subtitleSettings.textColor);
    });
    modal.querySelectorAll('[data-role="backgroundColorSwatch"]').forEach((button) => {
      button.classList.toggle("is-active", button.dataset.color === subtitleSettings.backgroundColor);
    });
  };

  const update = async () => {
    subtitleSettings = {
      backgroundColor: modal.querySelector('[data-role="backgroundColor"]').value,
      fontSize: Number(modal.querySelector('[data-role="fontSize"]').value),
      opacity: Number(modal.querySelector('[data-role="opacity"]').value),
      textColor: modal.querySelector('[data-role="textColor"]').value
    };
    modal.querySelector('[data-role="fontSizeValue"]').textContent = `${subtitleSettings.fontSize}px`;
    modal.querySelector('[data-role="opacityValue"]').textContent = `${subtitleSettings.opacity}%`;
    applySubtitleSettings();
    syncSwatches();
    await saveSubtitleSettings();
  };

  modal.querySelectorAll('input[type="range"], input[type="color"]').forEach((input) => {
    input.addEventListener("input", update);
  });
  modal.querySelectorAll('[data-role="textColorSwatch"]').forEach((button) => {
    button.addEventListener("click", () => {
      modal.querySelector('[data-role="textColor"]').value = button.dataset.color;
      update();
    });
  });
  modal.querySelectorAll('[data-role="backgroundColorSwatch"]').forEach((button) => {
    button.addEventListener("click", () => {
      modal.querySelector('[data-role="backgroundColor"]').value = button.dataset.color;
      update();
    });
  });
  modal.querySelector('[data-role="centerSubtitle"]').addEventListener("click", async () => {
    subtitlePosition = {
      ...subtitlePosition,
      leftPercent: 50
    };
    const subtitle = document.getElementById(SUBTITLE_ID);
    if (subtitle) applySubtitlePosition(subtitle);
    await saveSubtitlePosition();
  });
  attachSettingsDrag(modal);
  modal.addEventListener("click", (event) => {
    if (event.target === modal || event.target.dataset.role === "close") {
      modal.remove();
    }
  });

  document.documentElement.appendChild(modal);
  syncSwatches();
}

function updateBadge(videoId) {
  const badge = ensureBadge();
  const url = canonicalVideoUrl(videoId);
  badge.querySelector('[data-role="link"]').textContent = url;
  updatePlaybackBadge();
}

function updatePlaybackBadge() {
  const badge = document.getElementById(BADGE_ID);
  if (!badge) return;

  const playback = readPlaybackState();
  const stateText = playback.isFound ? (playback.isPaused ? "paused" : "playing") : "no video";
  badge.querySelector('[data-role="state"]').textContent = stateText;
  badge.querySelector('[data-role="time"]').textContent =
    `${formatTime(playback.time)} / ${formatTime(playback.duration)}`;
  badge.querySelector('[data-role="rate"]').textContent = `${playback.playbackRate}x`;
  renderSubtitleAt(playback.time);
}

function removeBadge() {
  document.getElementById(BADGE_ID)?.remove();
  document.getElementById(SUBTITLE_ID)?.remove();
  subtitleCues = [];
  loadedSubtitleVideoId = "";
  activeCueIndex = -1;
}

function applySubtitlePosition(subtitle) {
  subtitle.style.left = `${subtitlePosition.leftPercent}%`;
  subtitle.style.top = `${subtitlePosition.topPercent}%`;
}

async function saveSubtitlePosition() {
  await chrome.storage.local.set({
    [SUBTITLE_POSITION_KEY]: subtitlePosition
  });
}

async function restoreSubtitlePosition() {
  const stored = await chrome.storage.local.get(SUBTITLE_POSITION_KEY);
  const value = stored[SUBTITLE_POSITION_KEY];
  if (
    value &&
    Number.isFinite(value.leftPercent) &&
    Number.isFinite(value.topPercent)
  ) {
    subtitlePosition = {
      leftPercent: value.leftPercent,
      topPercent: value.topPercent
    };
  }
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

function attachSubtitleDrag(subtitle, player) {
  if (subtitle.dataset.dragReady === "true") return;
  subtitle.dataset.dragReady = "true";

  subtitle.addEventListener("pointerdown", (event) => {
    event.preventDefault();
    event.stopPropagation();
    subtitle.setPointerCapture(event.pointerId);
    subtitleDrag = {
      player,
      pointerId: event.pointerId
    };
  });

  subtitle.addEventListener("pointermove", (event) => {
    if (!subtitleDrag || subtitleDrag.pointerId !== event.pointerId) return;

    const rect = player.getBoundingClientRect();
    if (rect.width <= 0 || rect.height <= 0) return;

    const leftPercent = ((event.clientX - rect.left) / rect.width) * 100;
    const topPercent = ((event.clientY - rect.top) / rect.height) * 100;

    subtitlePosition = {
      leftPercent: clamp(leftPercent, 8, 92),
      topPercent: clamp(topPercent, 8, 92)
    };
    applySubtitlePosition(subtitle);
  });

  subtitle.addEventListener("pointerup", async (event) => {
    if (!subtitleDrag || subtitleDrag.pointerId !== event.pointerId) return;
    subtitle.releasePointerCapture(event.pointerId);
    subtitleDrag = null;
    await saveSubtitlePosition();
  });

  subtitle.addEventListener("pointercancel", () => {
    subtitleDrag = null;
  });
}

function renderSubtitleAt(time) {
  const subtitle = document.getElementById(SUBTITLE_ID);
  if (!subtitle) return;

  if (subtitleCues.length === 0) {
    subtitle.textContent = "";
    return;
  }

  const activeCue = subtitleCues[activeCueIndex];
  if (activeCue && time >= activeCue.start && time <= activeCue.end) return;

  const cueIndex = subtitleCues.findIndex((cue) => time >= cue.start && time <= cue.end);
  activeCueIndex = cueIndex;
  subtitle.textContent = cueIndex === -1 ? "" : subtitleCues[cueIndex].text;
}

function setSubtitleCues(cues, videoId) {
  subtitleCues = cues;
  loadedSubtitleVideoId = videoId;
  activeCueIndex = -1;
  renderSubtitleAt(readPlaybackState().time);
}

async function getManualSubtitles() {
  const stored = await chrome.storage.local.get(MANUAL_SUBTITLES_KEY);
  return stored[MANUAL_SUBTITLES_KEY] || {};
}

async function saveManualSubtitle(videoId, payload) {
  const subtitles = await getManualSubtitles();
  subtitles[videoId] = payload;
  await chrome.storage.local.set({
    [MANUAL_SUBTITLES_KEY]: subtitles
  });
}

async function loadManualSubtitleForVideo(videoId) {
  const subtitles = await getManualSubtitles();
  const manualSubtitle = subtitles[videoId];
  if (!manualSubtitle?.text) return false;

  const cues = parseSrt(manualSubtitle.text);
  if (cues.length === 0) return false;

  setSubtitleCues(cues, videoId);
  return true;
}

function flashSrtButton(button, label) {
  if (!button) return;
  const originalLabel = button.dataset.defaultLabel || button.textContent;
  button.dataset.defaultLabel = originalLabel;
  button.textContent = label;
  window.setTimeout(() => {
    button.textContent = button.dataset.defaultLabel || "Load SRT";
  }, 1600);
}

async function loadManualSrtFromInput(file, options = {}) {
  const { triggerButton } = options;
  const videoId = extractVideoIdFromLocation();

  if (!file) return;

  if (!videoId) {
    flashSrtButton(triggerButton, "Open video first");
    return;
  }

  try {
    if (triggerButton) triggerButton.textContent = "Loading...";
    const text = await file.text();
    const cues = parseSrt(text);
    if (cues.length === 0) {
      throw new Error("No valid SRT cues found.");
    }

    await saveManualSubtitle(videoId, {
      cueCount: cues.length,
      name: file.name,
      text
    });
    setSubtitleCues(cues, videoId);
    ensureSubtitle();
    flashSrtButton(triggerButton, `Loaded ${cues.length}`);
  } catch (error) {
    console.error("[TextLens] Could not load manual SRT", error);
    flashSrtButton(triggerButton, "Invalid SRT");
  }
}

async function loadSubtitleForVideo(videoId) {
  if (loadedSubtitleVideoId === videoId) return;

  const hasManualSubtitle = await loadManualSubtitleForVideo(videoId);
  if (hasManualSubtitle) return;

  setSubtitleCues([], videoId);
}

function ensureSubtitle() {
  const video = getVideoElement();
  const player = document.querySelector("#movie_player") || video?.parentElement;
  if (!video || !player) return;

  if (getComputedStyle(player).position === "static") {
    player.style.position = "relative";
  }

  let subtitle = document.getElementById(SUBTITLE_ID);
  if (!subtitle) {
    subtitle = document.createElement("div");
    subtitle.id = SUBTITLE_ID;
    player.appendChild(subtitle);
  }

  applySubtitlePosition(subtitle);
  applySubtitleSettings();
  attachSubtitleDrag(subtitle, player);
  renderSubtitleAt(video.currentTime || 0);
}

async function publishVideoState(videoId) {
  const state = videoId
    ? {
        detected: true,
        title: getVideoTitle(),
        url: canonicalVideoUrl(videoId),
        videoId
      }
    : {
        detected: false,
        title: "",
        url: "",
        videoId: ""
      };

  await chrome.storage.local.set({ textLensCurrentVideo: state });
}

function detect() {
  const videoId = extractVideoIdFromLocation();
  if (videoId === lastVideoId) return;

  lastVideoId = videoId;
  if (videoId) {
    updateBadge(videoId);
    loadSubtitleForVideo(videoId);
    ensureSubtitle();
  } else {
    removeBadge();
  }

  publishVideoState(videoId);
}

function watchYouTubeNavigation() {
  const originalPushState = history.pushState;
  const originalReplaceState = history.replaceState;

  history.pushState = function pushState(...args) {
    originalPushState.apply(this, args);
    setTimeout(detect, 50);
  };

  history.replaceState = function replaceState(...args) {
    originalReplaceState.apply(this, args);
    setTimeout(detect, 50);
  };

  window.addEventListener("popstate", () => setTimeout(detect, 50));
  window.addEventListener("yt-navigate-finish", () => setTimeout(detect, 50));
}

Promise.all([
  restoreBadgeMinimizedState(),
  restoreSettingsPosition(),
  restoreSubtitlePosition(),
  restoreSubtitleSettings()
]).then(() => {
  watchYouTubeNavigation();
  detect();
});
setInterval(detect, 1200);
setInterval(updatePlaybackBadge, 250);
setInterval(ensureSubtitle, 1000);

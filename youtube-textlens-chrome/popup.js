const stateElement = document.getElementById("state");
const copyButton = document.getElementById("copyButton");
const copyModal = document.getElementById("copyModal");
const modalUrl = document.getElementById("modalUrl");
const closeModalButton = document.getElementById("closeModalButton");

let currentUrl = "";

function extractVideoId(value) {
  try {
    const url = new URL(value);
    if (!url.hostname.includes("youtube.com") || url.pathname !== "/watch") return "";
    return url.searchParams.get("v") || "";
  } catch {
    return "";
  }
}

function canonicalVideoUrl(videoId) {
  return `https://www.youtube.com/watch?v=${encodeURIComponent(videoId)}`;
}

function renderDetected({ title, url, videoId }) {
  currentUrl = url;
  copyButton.disabled = false;
  stateElement.innerHTML = `
    <strong>${title || "Detected YouTube video"}</strong>
    <div>ID: ${videoId}</div>
    <div>${url}</div>
  `;
}

function renderMissing() {
  currentUrl = "";
  copyButton.disabled = true;
  stateElement.textContent = "Open a YouTube watch page, then click this extension again.";
}

async function loadState() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  const videoId = extractVideoId(tab?.url || "");

  if (videoId) {
    renderDetected({
      title: tab.title?.replace(/\s+-\s+YouTube$/, "") || "",
      url: canonicalVideoUrl(videoId),
      videoId
    });
    return;
  }

  const stored = await chrome.storage.local.get("textLensCurrentVideo");
  if (stored.textLensCurrentVideo?.detected) {
    renderDetected(stored.textLensCurrentVideo);
    return;
  }

  renderMissing();
}

copyButton.addEventListener("click", async () => {
  if (!currentUrl) return;
  await navigator.clipboard.writeText(currentUrl);
  modalUrl.textContent = currentUrl;
  copyModal.hidden = false;
  copyButton.textContent = "Copied";
  setTimeout(() => {
    copyButton.textContent = "Copy Link";
  }, 1200);
});

closeModalButton.addEventListener("click", () => {
  copyModal.hidden = true;
});

copyModal.addEventListener("click", (event) => {
  if (event.target === copyModal) {
    copyModal.hidden = true;
  }
});

loadState();

const stateElement = document.getElementById("state");
const downloadButton = document.getElementById("downloadButton");
const downloadModal = document.getElementById("downloadModal");
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

function subtitleToUrl(videoUrl) {
  return `https://subtitle.to/${videoUrl}`;
}

function renderDetected({ title, url, videoId }) {
  currentUrl = url;
  downloadButton.disabled = false;
  stateElement.innerHTML = `
    <strong>${title || "Detected YouTube video"}</strong>
    <div>ID: ${videoId}</div>
    <div>${url}</div>
  `;
}

function renderMissing() {
  currentUrl = "";
  downloadButton.disabled = true;
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

downloadButton.addEventListener("click", async () => {
  if (!currentUrl) return;
  const targetUrl = subtitleToUrl(currentUrl);
  await chrome.tabs.create({ url: targetUrl });
  modalUrl.textContent = targetUrl;
  downloadModal.hidden = false;
  downloadButton.textContent = "Opened";
  setTimeout(() => {
    downloadButton.textContent = "Download with subtitle.to";
  }, 1200);
});

closeModalButton.addEventListener("click", () => {
  downloadModal.hidden = true;
});

downloadModal.addEventListener("click", (event) => {
  if (event.target === downloadModal) {
    downloadModal.hidden = true;
  }
});

loadState();

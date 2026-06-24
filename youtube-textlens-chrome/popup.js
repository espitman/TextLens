const stateElement = document.getElementById("state");

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
  stateElement.innerHTML = `
    <strong>${title || "Detected YouTube video"}</strong>
    <div>ID: ${videoId}</div>
    <div>${url}</div>
  `;
}

function renderMissing() {
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

loadState();

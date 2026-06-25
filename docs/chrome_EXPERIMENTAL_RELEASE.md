# TextLens YouTube Chrome Experimental Release Guide

This document describes how to publish TextLens YouTube Chrome as a GitHub experimental extension package.

Release type:

```text
Experimental unpacked Chrome extension
GitHub zip package
No Chrome Web Store
No npm install
No build step
```

## 1. Preflight

From the repository root:

```sh
node --check youtube-textlens-chrome/content.js
node --check youtube-textlens-chrome/popup.js
node youtube-textlens-chrome/scripts/verify-srt-render.mjs
```

Expected:

- JavaScript syntax checks pass.
- SRT render verification passes.

## 2. Package Extension

From the repository root:

```sh
./scripts/package_chrome_extension.sh
```

Output:

```text
dist/TextLens-YouTube-Chrome-0.1.0.zip
```

The package contains:

```text
TextLens-YouTube-Chrome-0.1.0/
  manifest.json
  content.js
  popup.html
  popup.css
  popup.js
  README.md
```

No `node_modules`, test scripts, or development files are included.

## 3. Local Smoke Test

1. Unzip `dist/TextLens-YouTube-Chrome-0.1.0.zip`.
2. Open Chrome.
3. Go to:

```text
chrome://extensions
```

4. Enable **Developer mode**.
5. Click **Load unpacked**.
6. Select:

```text
TextLens-YouTube-Chrome-0.1.0
```

7. Open a YouTube watch page.
8. Confirm the floating TextLens badge appears.
9. Load an `.srt` file.
10. Confirm synced subtitles render over the YouTube player.
11. Drag the subtitle position.
12. Open settings and adjust font/background styling.
13. Reload the extension and refresh the YouTube tab.
14. Confirm settings and video-specific SRT storage still work.

## 4. GitHub Release

Create a new GitHub release:

```text
Tag: chrome-v0.1.0
Title: TextLens YouTube Chrome 0.1.0 Experimental
Attachment: dist/TextLens-YouTube-Chrome-0.1.0.zip
```

Use the release notes below.

## 5. Release Notes

```markdown
# TextLens YouTube Chrome 0.1.0 Experimental

TextLens YouTube Chrome is an experimental Chrome extension for rendering a local `.srt` subtitle file over the YouTube player.

It is distributed as an unpacked extension zip through GitHub. No `npm install`, build step, Node.js setup, backend, or Chrome Web Store account is required.

## Features

- Detects YouTube watch pages.
- Shows a floating TextLens badge over YouTube.
- Loads a local `.srt` subtitle file.
- Stores the loaded subtitle in Chrome extension storage for the current video.
- Renders synced subtitles over the YouTube player.
- Supports Persian / RTL subtitle display.
- Draggable subtitle position.
- Subtitle settings for font size, text color, background color, and opacity.

## Install

1. Download `TextLens-YouTube-Chrome-0.1.0.zip`.
2. Unzip it.
3. Open Chrome and go to `chrome://extensions`.
4. Enable **Developer mode**.
5. Click **Load unpacked**.
6. Select the unzipped `TextLens-YouTube-Chrome-0.1.0` folder.
7. Open a YouTube video and use the floating TextLens badge.

## Important

- No `npm install` is required.
- No build step is required.
- This is not a Chrome Web Store release.
- Chrome may show Developer mode warnings.

## Known Limitations

- SRT files must be prepared separately.
- The extension does not download or translate subtitles.
- The extension currently targets YouTube watch pages.
- YouTube layout changes may require updates.
```

## 6. LinkedIn Short Copy

```text
TextLens YouTube Chrome is now packaged as an experimental GitHub release.

It is a lightweight Chrome extension for rendering local SRT subtitles over YouTube. No backend, no npm install, no build step: download the zip, load it as an unpacked extension, open YouTube, and load an SRT file.

The goal is simple: make custom Persian subtitles usable directly on YouTube in the browser.
```

## 7. Launch Checklist

- [ ] Run JS syntax checks.
- [ ] Run SRT render verification.
- [ ] Build zip package.
- [ ] Unzip package.
- [ ] Load unpacked in Chrome.
- [ ] Test on a YouTube watch page.
- [ ] Load an SRT.
- [ ] Test subtitle sync.
- [ ] Test drag position.
- [ ] Test settings.
- [ ] Attach zip to GitHub release.

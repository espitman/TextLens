# TextLens YouTube Extension

Chrome extension for rendering a user-selected `.srt` subtitle file over the YouTube player.

## What It Does

- Runs on `youtube.com` watch pages.
- Detects the current YouTube video.
- Shows a floating TextLens badge over the page.
- Loads a local `.srt` file for the current video.
- Saves the loaded SRT in Chrome extension storage for that video.
- Renders synced subtitles over the YouTube player.
- Lets subtitle position, font size, text color, background color, and opacity be adjusted.

It does not download, extract, or translate subtitles.

## Install In Chrome

1. Open Chrome.
2. Go to `chrome://extensions`.
3. Turn on **Developer mode**.
4. Click **Load unpacked**.
5. Select this folder:

   `/Users/espitman/Documents/Projects/TextLens/youtube-textlens-chrome`

6. Open any YouTube video.
7. Use the floating TextLens badge:
   - File-plus icon: load an existing SRT.
   - Gear icon: subtitle display settings.

## Reload In Chrome

After changing extension files:

1. Open `chrome://extensions`.
2. Find **TextLens YouTube**.
3. Click the reload button on its card.
4. Refresh the YouTube tab.

## Verify

```sh
node --check youtube-textlens-chrome/content.js
node --check youtube-textlens-chrome/popup.js
node youtube-textlens-chrome/scripts/verify-srt-render.mjs
```

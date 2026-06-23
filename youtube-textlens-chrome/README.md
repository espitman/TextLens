# TextLens YouTube Extension

Chrome extension for detecting YouTube videos and rendering synced TextLens subtitles over the player.

## What It Does

- Runs on `youtube.com`.
- Detects `/watch?v=...` pages.
- Builds a canonical YouTube video link.
- Shows a floating TextLens control badge on the YouTube page.
- Shows the detected video in the extension popup.
- Loads a user-selected `.srt` file for the current YouTube video.
- Lets subtitle position, font size, colors, and opacity be adjusted from the on-page settings panel.

## Install In Chrome

1. Open Chrome.
2. Go to `chrome://extensions`.
3. Turn on **Developer mode**.
4. Click **Load unpacked**.
5. Select this folder:

   `/Users/espitman/Documents/Projects/TextLens/youtube-textlens-chrome`

6. Open any YouTube video.
7. Click the TextLens extension icon.
8. Press **Copy Link**.

## Reload In Chrome

After changing extension files:

1. Open `chrome://extensions`.
2. Find **TextLens YouTube**.
3. Click the reload button on its card.
4. Refresh the YouTube tab.

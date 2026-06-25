# TextLens YouTube Chrome

TextLens YouTube Chrome is an experimental Chrome extension for rendering a local `.srt` subtitle file over the YouTube player.

It is intentionally simple: no backend, no build step, no `npm install`, and no Chrome Web Store account required.

## What It Does

- Runs on YouTube watch pages.
- Detects the current YouTube video id and playback state.
- Shows a floating TextLens badge over YouTube.
- Loads a local `.srt` file from your computer.
- Stores the selected SRT in Chrome extension storage for that video.
- Renders synced subtitles over the YouTube player.
- Supports Persian / RTL subtitle rendering.
- Lets you drag the subtitle position.
- Includes settings for:
  - font size
  - text color
  - background color
  - background opacity
  - horizontal centering

## What It Does Not Do

- It does not download subtitles.
- It does not translate subtitles.
- It does not call an API.
- It does not require Node.js.
- It does not require `npm install`.
- It is not published on the Chrome Web Store yet.

## Install From GitHub Release

1. Download `TextLens-YouTube-Chrome-0.1.0.zip` from the GitHub release.
2. Unzip it.
3. Open Chrome.
4. Go to:

```text
chrome://extensions
```

5. Enable **Developer mode**.
6. Click **Load unpacked**.
7. Select the unzipped folder:

```text
TextLens-YouTube-Chrome-0.1.0
```

8. Open a YouTube video.
9. Use the floating TextLens badge:
   - file icon: load SRT
   - gear icon: subtitle settings
   - minus icon: minimize badge

## Install From Source

No build step is required.

1. Clone or download this repository.
2. Open Chrome.
3. Go to `chrome://extensions`.
4. Enable **Developer mode**.
5. Click **Load unpacked**.
6. Select:

```text
youtube-textlens-chrome
```

## Reload After Updating

After changing extension files:

1. Open `chrome://extensions`.
2. Find **TextLens YouTube**.
3. Click the reload button.
4. Refresh the YouTube tab.

## Package Release Zip

From the repository root:

```sh
./scripts/package_chrome_extension.sh
```

Output:

```text
dist/TextLens-YouTube-Chrome-0.1.0.zip
```

The zip contains only the extension files needed by Chrome:

```text
manifest.json
content.js
popup.html
popup.css
popup.js
README.md
```

## Verify

From the repository root:

```sh
node --check youtube-textlens-chrome/content.js
node --check youtube-textlens-chrome/popup.js
node youtube-textlens-chrome/scripts/verify-srt-render.mjs
```

## Known Limitations

- This is an unpacked experimental extension.
- Chrome will show Developer mode warnings.
- Users must manually load the extension through `chrome://extensions`.
- SRT files must be prepared separately.
- The extension currently targets YouTube watch pages.
- YouTube layout changes may require updates.

## Privacy

TextLens YouTube Chrome stores loaded subtitles in local Chrome extension storage for matching videos. It does not send subtitle content or browsing data to a backend.

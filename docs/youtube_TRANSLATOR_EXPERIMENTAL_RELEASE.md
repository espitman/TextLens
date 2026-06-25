# YouTube TextLens Translator Experimental Release Guide

This document describes how to publish YouTube TextLens Translator as a ready-to-run macOS Electron app.

Release type:

```text
Experimental macOS Electron app
GitHub DMG package
Unsigned / not notarized
No npm install for end users
No backend
```

## 1. Preflight

From the repository root:

```sh
node --check youtube-textlens-translator/main.js
node --check youtube-textlens-translator/preload.js
node --check youtube-textlens-translator/renderer.js
node --check youtube-textlens-translator/translator.js
```

Expected:

- JavaScript syntax checks pass.
- `youtube-textlens-translator/node_modules/electron` exists on the maintainer machine.

If Electron is missing on the maintainer machine:

```sh
cd youtube-textlens-translator
npm install
```

End users do not run this.

## 2. Package macOS App

From the repository root:

```sh
./scripts/package_youtube_translator_macos.sh
```

Output:

```text
dist/YouTube-TextLens-Translator-0.1.0-macOS.dmg
```

The DMG contains:

```text
YouTube TextLens Translator.app
Applications link
INSTALL.txt
```

The `.app` includes the Electron runtime and the app source in:

```text
Contents/Resources/app
```

No `node_modules` folder is required inside the app package.

## 3. Local Smoke Test

1. Open `dist/YouTube-TextLens-Translator-0.1.0-macOS.dmg`.
2. Drag `YouTube TextLens Translator.app` to `/Applications`.
3. Right-click -> **Open**.
4. Confirm the macOS unsigned-app warning.
5. Add an API key.
6. Load a small English SRT file.
7. Translate to Persian.
8. Save the result.
9. Confirm the saved file is valid `.srt`.

## 4. GitHub Release

Create a new GitHub release:

```text
Tag: youtube-translator-v0.1.0
Title: YouTube TextLens Translator 0.1.0 Experimental
Attachment: dist/YouTube-TextLens-Translator-0.1.0-macOS.dmg
```

Use the release notes below.

## 5. Release Notes

```markdown
# YouTube TextLens Translator 0.1.0 Experimental

YouTube TextLens Translator is a macOS desktop helper for preparing Persian `.srt` subtitle files.

Load or fetch an English SRT, translate it through your own OpenRouter or Liara API key, preview the Persian result, and save a `fa.srt` file for TextLens TV or TextLens YouTube Chrome.

## Features

- Ready-to-run macOS Electron app.
- No npm install for end users.
- Load local SRT files.
- Fetch English SRT through subtitle.to.
- Translate subtitles into Persian.
- OpenRouter and Liara provider settings.
- Progress logs and Persian preview.
- Save translated SRT.

## Install

1. Download `YouTube-TextLens-Translator-0.1.0-macOS.dmg`.
2. Open the DMG.
3. Drag `YouTube TextLens Translator.app` into Applications.
4. Right-click the app and choose **Open**.
5. Confirm the macOS warning.

No Node.js, npm, or build step is required.

## Requirements

- macOS.
- Your own OpenRouter or Liara API key.
- Internet access for translation requests.

## Known Limitations

- Experimental unsigned macOS app.
- Not notarized.
- subtitle.to fetching depends on a third-party website.
- Translation speed/cost depends on provider and model.
```

## 6. LinkedIn Short Copy

```text
YouTube TextLens Translator is an experimental macOS helper app for preparing Persian SRT subtitles.

It packages as a ready-to-run Electron app: no npm install, no backend, no setup for end users. Load or fetch an English SRT, translate it with your own OpenRouter or Liara API key, then use the generated Persian SRT in TextLens TV or the TextLens YouTube Chrome extension.
```

## 7. Launch Checklist

- [ ] Run JS syntax checks.
- [ ] Package macOS app.
- [ ] Open the DMG and run the packaged app.
- [ ] Load a small SRT.
- [ ] Translate with OpenRouter.
- [ ] Translate with Liara if available.
- [ ] Save output SRT.
- [ ] Attach DMG to GitHub release.

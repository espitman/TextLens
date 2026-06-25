# YouTube TextLens Translator

YouTube TextLens Translator is an experimental macOS desktop helper for preparing Persian `.srt` subtitle files.

It is built with Electron and packaged as a ready-to-run macOS DMG. End users do not need Node.js, npm, or `npm install`.

## What It Does

- Loads a local English `.srt` file.
- Can fetch the first English SRT through `subtitle.to` in the background.
- Translates subtitle cues into Persian through an OpenAI-compatible API.
- Supports OpenRouter and Liara settings.
- Can import API keys from the TextLens macOS app settings on the same machine.
- Shows translation progress, logs, and Persian preview.
- Saves the translated Persian SRT as `VIDEO_ID-fa.srt` when a YouTube video id is known.

## What It Does Not Do

- It is not a Chrome extension.
- It is not published on the Mac App Store.
- It is not notarized.
- It does not require a backend.
- It does not render subtitles over YouTube. Use TextLens TV or TextLens YouTube Chrome for rendering.

## Install Experimental macOS App

1. Download `YouTube-TextLens-Translator-0.1.0-macOS.dmg` from the GitHub release.
2. Open the DMG.
3. Drag `YouTube TextLens Translator.app` into `Applications`.
4. Right-click the app and choose **Open**.
5. Confirm the macOS unsigned-app warning.

No `npm install` is required.

## Use

1. Open the app.
2. Choose provider:
   - OpenRouter
   - Liara
3. Enter your API key.
4. Load or fetch an English SRT.
5. Translate to Persian.
6. Save the generated `.srt`.
7. Use the output file in:
   - TextLens TV
   - TextLens YouTube Chrome

## Build From Source

Maintainers can run the source version with:

```sh
cd youtube-textlens-translator
npm install
npm start
```

Package a ready-to-run macOS app from the repository root:

```sh
./scripts/package_youtube_translator_macos.sh
```

Output:

```text
dist/YouTube-TextLens-Translator-0.1.0-macOS.dmg
```

## Privacy

Subtitle text is sent to the selected translation provider. The app does not include analytics or telemetry.

API keys are stored locally in the app/browser storage used by the Electron app. If imported from TextLens macOS, they are read from local macOS defaults.

## Known Limitations

- Experimental macOS build.
- Unsigned and not notarized.
- macOS may require right-click -> Open on first launch.
- Translation quality, speed, and cost depend on the selected provider/model.
- Very long subtitle files may take time and consume API credits.
- Automatic subtitle fetching depends on `subtitle.to` availability and behavior.

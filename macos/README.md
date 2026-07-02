# TextLens macOS

TextLens for macOS is a menu bar screen-translation app. Press the global shortcut, select an area of the screen, and TextLens captures that area, reads the text with Apple Vision OCR, translates it through an OpenAI-compatible API, and shows the Persian result in a floating popup.

This build is an experimental GitHub release. It is intentionally unsigned/not notarized because it does not require an Apple Developer account.

## Features

- Menu bar app with a compact `T` icon.
- Global shortcut: `Command + Shift + 0`.
- Screenshot area selection overlay.
- Screen capture and OCR through native macOS APIs.
- Persian translation popup with RTL rendering.
- Translation history for the last items.
- Provider settings for:
  - OpenRouter
  - Liara
  - Local OpenAI-compatible server
- Model picker with custom model support.
- Expanded Liara Gemini model options.
- Retry flow with model switching.
- No analytics or telemetry.

## Requirements

- macOS 13 or newer.
- Screen Recording permission.
- Your own OpenAI-compatible API key, unless you use a local provider that does not require one.
- Internet access for translation requests.

## Install The Experimental DMG

1. Download `TextLens-0.1.1.dmg` from the GitHub release.
2. Open the DMG.
3. Drag `TextLens.app` into `Applications`.
4. Open `Applications`.
5. Right-click `TextLens.app` and choose **Open**.
6. Confirm the macOS warning.

Because this experimental release is not notarized, double-clicking the app may show a security warning. Use right-click -> **Open** the first time.

If macOS still blocks the app:

1. Open **System Settings**.
2. Go to **Privacy & Security**.
3. Scroll to the security warning.
4. Click **Open Anyway**.
5. Run TextLens again.

## First Run

1. Launch TextLens.
2. Click the `T` icon in the menu bar.
3. Open **Settings**.
4. Select a provider:
   - `OpenRouter`
   - `Liara`
5. Paste your API key.
6. Confirm the base URL and model.
7. Save settings.
8. Grant Screen Recording permission when prompted.

## Screen Recording Permission

TextLens needs Screen Recording permission to read pixels from the selected screen area. Without this permission it cannot OCR the selected region.

Grant it in:

```text
System Settings -> Privacy & Security -> Screen Recording
```

Enable `TextLens`, then quit and reopen the app.

If the app does not appear in the Screen Recording list:

1. Quit TextLens.
2. Open TextLens again from `/Applications`.
3. Trigger a translation once.
4. Reopen Screen Recording settings.

If permission gets stuck after replacing the app:

1. Remove TextLens from Screen Recording.
2. Quit TextLens.
3. Reopen `/Applications/TextLens.app`.
4. Grant Screen Recording again.

## Usage

Use either method:

- Press `Command + Shift + 0`.
- Click the menu bar `T` icon and choose the translate action.

Then:

1. Drag over the text area.
2. Release the mouse.
3. Wait for OCR and translation.
4. Read the Persian result in the popup.
5. Copy, close, retry, or switch model if needed.

## Provider Defaults

OpenRouter:

```text
Base URL: https://openrouter.ai/api/v1
Default model: google/gemma-4-31b-it:free
```

Liara:

```text
Base URL: https://ai.liara.ir/api/6a0ccd2d298429714a4b3e25/v1
Default model: openai/gpt-5-nano
Additional models: google/gemini-2.5-flash-lite, google/gemini-3.1-flash-lite, google/gemini-2.5-flash
```

Local:

```text
Base URL: http://127.0.0.1:8088/v1
Default model: local-model
API key: optional
```

Target language:

```text
Persian
```

## Privacy

TextLens processes the selected screen area locally for capture and OCR, then sends the recognized text to the selected translation provider.

TextLens does not add analytics or telemetry.

Current experimental limitation:

- API keys are stored in macOS `UserDefaults`.
- For a production-grade public build, API keys should move to Keychain.

## Known Limitations

- This is an experimental, unsigned GitHub release.
- macOS may show security warnings on first launch.
- Screen Recording permission can require quitting and reopening the app.
- OCR quality depends on the selected area, font, contrast, and language.
- Multi-display behavior may vary depending on macOS display arrangement.
- Translation quality and cost depend on the selected provider and model.

## Build Locally

From the repository root:

```sh
./scripts/build-dmg.sh 0.1.1
```

The DMG is created at:

```text
dist/TextLens-0.1.1.dmg
```

For local development:

```sh
cd macos
swift build
```

To install and run a local app bundle from the repository root:

```sh
./run
```

## Release Type

This macOS build is distributed as:

```text
Experimental GitHub release
Unsigned / not notarized
Bring your own API key
```

It is suitable for technical users who are comfortable opening unsigned macOS apps from GitHub.

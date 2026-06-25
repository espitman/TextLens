# TextLens macOS Experimental Release Guide

This document describes how to publish TextLens macOS without an Apple Developer account.

Release type:

```text
Experimental GitHub release
Unsigned / not notarized
Ad-hoc codesigned
Bring your own API key
```

## 1. Preflight

From the repository root:

```sh
git status --short
swift build --package-path macos
```

Expected:

- Working tree is clean or only intentional release docs are changed.
- Swift build succeeds.

## 2. Build DMG

From the repository root:

```sh
./scripts/build-dmg.sh 0.1.0
```

Output:

```text
dist/TextLens-0.1.0.dmg
```

The script:

- Builds the Swift package in release mode.
- Creates `TextLens.app`.
- Adds the app icon and resources.
- Uses bundle id `com.textlens.app`.
- Applies ad-hoc codesigning by default.
- Creates a compressed DMG.

## 3. Local Smoke Test

Install the DMG locally:

1. Open `dist/TextLens-0.1.0.dmg`.
2. Drag `TextLens.app` to `/Applications`.
3. Open `/Applications/TextLens.app` with right-click -> **Open**.
4. Confirm the macOS unsigned-app warning.
5. Add an API key in Settings.
6. Grant Screen Recording permission.
7. Quit and reopen TextLens.
8. Press `Command + Shift + 0`.
9. Select a text area.
10. Confirm the Persian popup appears.

## 4. GitHub Release

Create a new GitHub release:

```text
Tag: macos-v0.1.0
Title: TextLens macOS 0.1.0 Experimental
Attachment: dist/TextLens-0.1.0.dmg
```

Use the release notes below.

## 5. Release Notes

```markdown
# TextLens macOS 0.1.0 Experimental

TextLens is a macOS menu bar app for translating text from a selected area of the screen into Persian.

Press `Command + Shift + 0`, select a region, and TextLens captures the area, performs OCR, sends the recognized text to your selected OpenAI-compatible translation provider, and shows the Persian result in a floating popup.

## Features

- Menu bar app with global shortcut.
- Area selection overlay.
- Native macOS screen capture.
- Apple Vision OCR.
- Persian RTL translation popup.
- Translation history.
- OpenRouter and Liara provider settings.
- Model picker and retry support.
- No analytics or telemetry.

## Requirements

- macOS 13 or newer.
- Screen Recording permission.
- Your own OpenRouter or Liara API key.

## Install

1. Download `TextLens-0.1.0.dmg`.
2. Open the DMG.
3. Drag `TextLens.app` into `Applications`.
4. Right-click `TextLens.app` and choose **Open**.
5. Confirm the macOS warning.
6. Add your API key in Settings.
7. Grant Screen Recording permission.

This is an unsigned experimental build. It is not notarized because this release does not use an Apple Developer account.

## Privacy

TextLens captures only the screen area you select. OCR is performed locally. The recognized text is sent to the translation provider you configure.

TextLens does not include analytics or telemetry.

## Known Limitations

- Unsigned / not notarized.
- macOS may require right-click -> Open on first launch.
- Screen Recording permission may require quitting and reopening the app.
- API keys are currently stored in UserDefaults in this experimental build.
- OCR quality depends on text size, contrast, and selected area.
```

## 6. LinkedIn Short Copy

```text
I’m releasing an experimental macOS build of TextLens.

TextLens is a menu bar screen-translation app: press Command + Shift + 0, select any area of the screen, and it OCRs the text and translates it into Persian using your own OpenRouter or Liara API key.

This first macOS release is intentionally experimental: unsigned, not notarized, and built for technical users who are comfortable installing apps from GitHub.

The goal is simple: make non-copyable text on screen quickly understandable in Persian.
```

## 7. Post-release Checks

After publishing:

- Download the DMG from GitHub, not from local `dist`.
- Install it into `/Applications`.
- Confirm first-run warning appears as expected.
- Confirm Screen Recording permission works.
- Confirm translation works with both providers if possible.
- Confirm the release link in `README.md` points to the latest release.

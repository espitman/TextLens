# TextLens macOS

TextLens is a macOS menu bar app for translating text from a selected area of the screen into Persian.

The MVP focuses only on partial translation: the user selects an area, TextLens captures that area, extracts English text with OCR, sends the text to an OpenAI-compatible translation API, and shows the Persian result in a small popup.

## Requirements

- macOS 13 or later
- Xcode 15 or later
- Swift 5.9 or later
- Screen Recording permission
- An OpenAI-compatible API key

## Build and Run

The macOS app is currently scaffolded as a Swift Package executable that can be opened in Xcode from this directory.

```sh
open Package.swift
```

You can also build from the command line:

```sh
swift build
```

## Screen Recording Permission

TextLens needs Screen Recording permission to read text from the selected area of your screen.

When the permission flow is implemented, grant access in:

```text
System Settings -> Privacy & Security -> Screen Recording
```

## API Key Setup

TextLens defaults to Liara's OpenAI-compatible Chat API.

The settings are:

- API Key
- Base URL, defaulting to `https://ai.liara.ir/api/6a0ccd2d298429714a4b3e25/v1`
- Model, defaulting to `openai/gpt-4.1-mini`
- Target language, defaulting to `Persian`

## Default Shortcut

The planned default shortcut is:

```text
Command + Shift + 0
```

## MVP Limitations

- Only partial area translation is in scope.
- The first version targets English OCR to Persian translation.
- Multi-display support may be limited at first.
- API Key storage starts with `UserDefaults`; Keychain storage is deferred.
- Screenshots, OCR text, and translations must not be written to disk.
- No telemetry or analytics should be added.

## Next TODOs

- Implement the menu bar shell.
- Add global hotkey support.
- Build the area selection overlay.
- Capture and crop the selected screen area.
- Add Vision OCR.
- Add Settings persistence.
- Add the OpenAI-compatible translation client.
- Show the translated result in a popup.

# TextLens MVP TODO

This document breaks the TextLens MVP into implementation phases with tasks and subtasks.

## Phase 0: Project Setup

- [x] Create the macOS app project
  - [x] Create a Swift macOS app named `TextLens`
  - [x] Configure the app as a menu bar app with no default main window
  - [x] Set minimum deployment target to macOS 13+
  - [x] Add required app entitlements and privacy usage descriptions where needed
  - [x] Place the macOS app under `macos` for future cross-platform targets

- [x] Define the initial source structure
  - [x] Create `Core/`
  - [x] Create `UI/`
  - [x] Create `Models/`
  - [x] Create `Utils/`
  - [x] Add placeholder files for the main services and views

- [x] Add documentation
  - [x] Create `README.md`
  - [x] Document build and run steps
  - [x] Document MVP limitations

## Phase 1: Menu Bar Shell

- [x] Implement app lifecycle
  - [x] Add `TextLensApp`
  - [x] Add `AppDelegate`
  - [x] Prevent a default app window from opening on launch

- [x] Implement menu bar controller
  - [x] Add TextLens menu bar icon
  - [x] Add `Translate Area` menu item
  - [x] Add `Settings` menu item
  - [x] Add `Quit` menu item

- [x] Wire basic actions
  - [x] Make `Translate Area` start the translation flow
  - [x] Make `Settings` open the settings window
  - [x] Make `Quit` terminate the app

## Phase 2: Global Hotkey

- [x] Implement global hotkey support
  - [x] Register `Command + Shift + 0`
  - [x] Trigger the same flow as `Translate Area`
  - [x] Unregister the hotkey cleanly on app shutdown

- [ ] Validate hotkey behavior
  - [ ] Confirm the shortcut works while TextLens is not focused
  - [x] Confirm repeated shortcut presses do not create duplicate overlays
  - [x] Add graceful handling if hotkey registration fails

## Phase 3: Selection Overlay

- [x] Create the selection overlay window
  - [x] Use a borderless `NSWindow`
  - [x] Display it above normal windows
  - [x] Apply a semi-transparent dark background
  - [x] Cover at least the primary display for MVP

- [x] Implement drag selection
  - [x] Track mouse down position
  - [x] Update the selection rectangle during drag
  - [x] Draw a clear border around the selected area
  - [x] Normalize rectangles dragged in any direction

- [x] Implement cancellation and completion
  - [x] Close the overlay on Escape
  - [x] Return the selected rectangle on mouse up
  - [x] Ignore selections that are too small
  - [x] Add a TODO for improved multi-display support

## Phase 4: Screen Capture

- [x] Implement `ScreenshotService`
  - [x] Capture the selected display using `CGDisplayCreateImage`
  - [x] Crop the image to the selected rectangle
  - [x] Return a `CGImage`
  - [x] Avoid writing screenshots to disk

- [x] Handle screen recording permission
  - [x] Add `PermissionService`
  - [x] Detect missing Screen Recording permission
  - [x] Show a clear permission message
  - [x] Open the correct System Settings screen when possible
  - [x] Ensure missing permission does not crash the app

- [ ] Validate capture behavior
  - [ ] Confirm capture works on the primary display
  - [ ] Confirm the crop matches the selected area
  - [x] Handle invalid capture or crop failures with user-facing errors

## Phase 5: OCR

- [x] Implement `OCRService`
  - [x] Use `VNRecognizeTextRequest`
  - [x] Set recognition level to `.accurate`
  - [x] Enable language correction
  - [x] Use `en-US` as the MVP recognition language

- [x] Normalize OCR output
  - [x] Extract top text candidates
  - [x] Preserve logical line breaks
  - [x] Trim empty lines
  - [x] Return a plain text string

- [x] Handle OCR errors and empty results
  - [x] Show `No text found in selected area` when no text is detected
  - [x] Surface Vision errors in a simple, readable way
  - [x] Ensure OCR failures do not crash the app

## Phase 6: Settings

- [x] Define settings model
  - [x] Add `TranslationSettings`
  - [x] Store API Key
  - [x] Store Base URL
  - [x] Store Model
  - [x] Store target language, defaulting to Persian

- [x] Implement settings persistence
  - [x] Use `UserDefaults` for MVP
  - [x] Add a TODO for moving API Key storage to Keychain
  - [x] Provide default Base URL: `https://ai.liara.ir/api/6a0ccd2d298429714a4b3e25/v1`
  - [x] Provide default model: `openai/gpt-4.1-mini`

- [x] Build settings UI
  - [x] Add API Key input
  - [x] Add Base URL input
  - [x] Add Model input
  - [x] Add Save behavior
  - [x] Open settings automatically when API Key is missing

## Phase 7: Translation API

- [x] Implement translation service protocol
  - [x] Add `TranslationServiceProtocol`
  - [x] Add `translateToPersian(_:) async throws -> String`

- [x] Implement OpenAI-compatible client
  - [x] Build chat completions request payload
  - [x] Use configurable Base URL
  - [x] Use configurable Model
  - [x] Send API Key in the Authorization header
  - [x] Decode the translated response text

- [x] Implement translation prompt
  - [x] Ask for accurate, natural Persian translation
  - [x] Prevent extra explanations
  - [x] Keep UI labels concise

- [x] Handle API failures
  - [x] Detect missing API Key before sending a request
  - [x] Show simple errors for network failures
  - [x] Show simple errors for non-2xx API responses
  - [x] Handle malformed API responses gracefully

## Phase 8: Translation Popup

- [x] Create popup window
  - [x] Position it near the selected screen area
  - [x] Keep it visually lightweight and macOS-native
  - [x] Ensure it appears above normal windows

- [x] Build popup content
  - [x] Show translated Persian text
  - [x] Add Copy button
  - [x] Add Close button
  - [x] Support multiline translated text

- [x] Implement interactions
  - [x] Copy translation to clipboard
  - [x] Close popup on Escape
  - [x] Close popup when the user clicks outside if feasible for MVP

## Phase 9: Main Flow Integration

- [ ] Connect the end-to-end translation flow
  - [ ] Start from menu item or hotkey
  - [ ] Show selection overlay
  - [ ] Capture selected area
  - [ ] Run OCR
  - [ ] Translate OCR text
  - [ ] Show translation popup

- [ ] Add flow state management
  - [ ] Prevent overlapping translation sessions
  - [ ] Show progress state during OCR and translation
  - [ ] Reset state after success, cancellation, or error

- [ ] Add user-facing error presentation
  - [ ] Create `ErrorPresenter`
  - [ ] Use consistent error messages
  - [ ] Avoid crashes for expected MVP failure modes

## Phase 10: QA and Acceptance

- [ ] Verify acceptance criteria
  - [ ] App launches in the menu bar
  - [ ] `Command + Shift + 0` opens the selection overlay
  - [ ] User can select a screen area
  - [ ] App captures the selected area
  - [ ] OCR extracts simple English text
  - [ ] Text is sent to the configured API
  - [ ] Persian translation appears in the popup
  - [ ] Copy button works
  - [ ] Missing permission does not crash
  - [ ] Missing API Key opens Settings

- [ ] Test important error paths
  - [ ] Missing Screen Recording permission
  - [ ] Missing API Key
  - [ ] Empty OCR result
  - [ ] Network failure
  - [ ] Invalid API response

- [ ] Manual usability pass
  - [ ] Confirm overlay feels responsive
  - [ ] Confirm popup placement is reasonable
  - [ ] Confirm settings are easy to update
  - [ ] Confirm no screenshots or translations are written to disk

## Phase 11: README Completion

- [ ] Document what TextLens does
- [ ] Document requirements
- [ ] Document build and run steps
- [ ] Document Screen Recording permission setup
- [ ] Document API Key setup
- [ ] Document default shortcut
- [ ] Document MVP limitations
- [ ] Document next-version TODOs

## Deferred Post-MVP Work

- [ ] Improve multi-display support
- [ ] Add source and target language selection
- [ ] Add automatic language detection
- [ ] Add translation history
- [ ] Add configurable shortcut in Settings
- [ ] Move API Key storage to Keychain
- [ ] Show original OCR text beside translation
- [ ] Add retry support for network errors
- [ ] Add provider-specific clients for Gemini and DeepL
- [ ] Add full-screen translation mode
- [ ] Add comic mode
- [ ] Add text replacement overlay mode

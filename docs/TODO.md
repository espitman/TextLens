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
  - [x] Register `Command + Shift + T`
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

- [ ] Implement `OCRService`
  - [ ] Use `VNRecognizeTextRequest`
  - [ ] Set recognition level to `.accurate`
  - [ ] Enable language correction
  - [ ] Use `en-US` as the MVP recognition language

- [ ] Normalize OCR output
  - [ ] Extract top text candidates
  - [ ] Preserve logical line breaks
  - [ ] Trim empty lines
  - [ ] Return a plain text string

- [ ] Handle OCR errors and empty results
  - [ ] Show `No text found in selected area` when no text is detected
  - [ ] Surface Vision errors in a simple, readable way
  - [ ] Ensure OCR failures do not crash the app

## Phase 6: Settings

- [ ] Define settings model
  - [ ] Add `TranslationSettings`
  - [ ] Store API Key
  - [ ] Store Base URL
  - [ ] Store Model
  - [ ] Store target language, defaulting to Persian

- [ ] Implement settings persistence
  - [ ] Use `UserDefaults` for MVP
  - [ ] Add a TODO for moving API Key storage to Keychain
  - [ ] Provide default Base URL: `https://api.openai.com/v1`
  - [ ] Provide default model: `gpt-4o-mini`

- [ ] Build settings UI
  - [ ] Add API Key input
  - [ ] Add Base URL input
  - [ ] Add Model input
  - [ ] Add Save behavior
  - [ ] Open settings automatically when API Key is missing

## Phase 7: Translation API

- [ ] Implement translation service protocol
  - [ ] Add `TranslationServiceProtocol`
  - [ ] Add `translateToPersian(_:) async throws -> String`

- [ ] Implement OpenAI-compatible client
  - [ ] Build chat completions request payload
  - [ ] Use configurable Base URL
  - [ ] Use configurable Model
  - [ ] Send API Key in the Authorization header
  - [ ] Decode the translated response text

- [ ] Implement translation prompt
  - [ ] Ask for accurate, natural Persian translation
  - [ ] Prevent extra explanations
  - [ ] Keep UI labels concise

- [ ] Handle API failures
  - [ ] Detect missing API Key before sending a request
  - [ ] Show simple errors for network failures
  - [ ] Show simple errors for non-2xx API responses
  - [ ] Handle malformed API responses gracefully

## Phase 8: Translation Popup

- [ ] Create popup window
  - [ ] Position it near the selected screen area
  - [ ] Keep it visually lightweight and macOS-native
  - [ ] Ensure it appears above normal windows

- [ ] Build popup content
  - [ ] Show translated Persian text
  - [ ] Add Copy button
  - [ ] Add Close button
  - [ ] Support multiline translated text

- [ ] Implement interactions
  - [ ] Copy translation to clipboard
  - [ ] Close popup on Escape
  - [ ] Close popup when the user clicks outside if feasible for MVP

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
  - [ ] `Command + Shift + T` opens the selection overlay
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

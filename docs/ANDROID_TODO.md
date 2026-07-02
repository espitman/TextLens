# TextLens Android Floating Bubble TODO

This document defines the Android implementation plan for TextLens with a floating bubble capture flow.

## Phase 0: Android Project Setup

- [x] Create the Android app workspace
  - [x] Add an `android` or `apps/android` folder
  - [x] Create a Kotlin Android project
  - [x] Use Jetpack Compose for UI
  - [x] Set minimum Android version based on `MediaProjection` and overlay requirements
  - [x] Add a shared product name, package name, and app icon strategy

- [x] Define the Android source structure
  - [x] Create `core/` for services and orchestration
  - [x] Create `ui/` for Compose screens and overlay views
  - [x] Create `data/` for settings, history, and persistence
  - [x] Create `ocr/` for ML Kit text recognition
  - [x] Create `translation/` for OpenRouter and Liara clients

- [x] Add baseline tooling
  - [x] Configure Gradle
  - [x] Add Kotlin serialization or Moshi
  - [x] Add OkHttp
  - [x] Add ML Kit Text Recognition
  - [x] Add lint and basic debug build scripts

## Phase 1: App Shell and Settings

- [x] Build the main Android app shell
  - [x] Add a simple first-run screen
  - [x] Add provider selection: OpenRouter and Liara
  - [x] Add API key input per provider
  - [x] Add base URL configuration per provider
  - [x] Add model selection per provider
  - [x] Add custom model entry
  - [x] Add target language setting, defaulting to Persian

- [x] Persist settings safely
  - [x] Store non-sensitive settings in DataStore
  - [x] Store API keys in EncryptedSharedPreferences or Android Keystore-backed storage
  - [x] Keep Liara and OpenRouter API keys fully separate
  - [x] Add default Liara model: `openai/gpt-5-nano`
  - [x] Add Liara Gemini models: `google/gemini-2.5-flash-lite`, `google/gemini-3.1-flash-lite`, `google/gemini-2.5-flash`
  - [x] Add default OpenRouter model: `google/gemma-4-31b-it:free`

## Phase 2: Permission Flow

- [x] Implement overlay permission handling
  - [x] Detect `SYSTEM_ALERT_WINDOW`
  - [x] Show a clear permission request screen
  - [x] Open the correct Android settings page
  - [x] Re-check permission when the app resumes

- [x] Implement screen capture permission handling
  - [x] Use `MediaProjectionManager`
  - [x] Request screen capture permission before first capture
  - [x] Cache active capture session while allowed
  - [x] Handle denied permission gracefully
  - [x] Add a fallback prompt if the projection token expires

- [x] Add battery/background guidance
  - [x] Keep MVP behavior foreground-safe
  - [x] Use a foreground service only if required for stable capture
  - [x] Show a persistent notification only when the bubble service is active

## Phase 3: Floating Bubble

- [x] Create the floating bubble service
  - [x] Add a foreground-capable service for the bubble
  - [x] Render a draggable floating bubble above other apps
  - [x] Use the TextLens black/gold icon style
  - [x] Save the last bubble position
  - [x] Keep the bubble compact and unobtrusive

- [x] Implement bubble interactions
  - [x] Tap bubble to start area selection
  - [x] Long press or secondary action to open settings
  - [x] Drag bubble around screen edges
  - [x] Snap bubble to screen edge after release
  - [x] Hide bubble while selecting an area

- [x] Add bubble lifecycle controls
  - [x] Start bubble from app
  - [x] Stop bubble from app
  - [x] Stop bubble from notification
  - [x] Restore bubble after device rotation where feasible

## Phase 4: Area Selection Overlay

- [x] Build the selection overlay
  - [x] Draw a full-screen transparent overlay
  - [x] Track touch down, move, and up
  - [x] Draw the selected rectangle
  - [x] Make the selected area visually clear
  - [x] Support cancellation

- [x] Match TextLens selection behavior
  - [x] Keep outside area readable, not fully dark
  - [x] Darken only the selected box slightly
  - [x] Add a black/gold selection border
  - [x] Ignore tiny accidental selections
  - [x] Handle status bar and navigation bar offsets

- [x] Validate screen geometry
  - [x] Support portrait mode
  - [x] Support landscape mode
  - [x] Support display cutouts
  - [x] Support different density buckets

## Phase 5: Screen Capture and Crop

- [x] Capture the screen with `MediaProjection`
  - [x] Create a virtual display
  - [x] Use `ImageReader` for frames
  - [x] Capture one frame after selection completes
  - [x] Convert frame to bitmap
  - [x] Release image resources immediately

- [x] Crop selected area
  - [x] Map overlay coordinates to captured bitmap coordinates
  - [x] Crop exactly the selected rectangle
  - [x] Handle display scaling and rotation
  - [x] Avoid writing screenshots to disk
  - [ ] Add debug-only crop preview if needed

- [x] Handle capture errors
  - [x] Missing projection permission
  - [x] Empty frame
  - [x] Invalid crop rectangle
  - [x] ImageReader timeout

## Phase 6: OCR

- [x] Integrate ML Kit OCR
  - [x] Add ML Kit Text Recognition dependency
  - [x] Run OCR on the cropped bitmap
  - [x] Extract recognized blocks and lines
  - [x] Preserve logical line breaks
  - [x] Trim empty text

- [x] Optimize OCR behavior
  - [x] Run OCR off the main thread
  - [x] Downscale huge crops if needed
  - [x] Keep enough resolution for small text
  - [x] Return a clear empty-text error

- [ ] Validate OCR quality
  - [ ] Test web pages
  - [ ] Test screenshots
  - [ ] Test PDFs viewed on Android
  - [ ] Test dark and light backgrounds

## Phase 7: Translation API

- [x] Port provider model to Android
  - [x] Define `TranslationProvider`
  - [x] Define provider-specific defaults
  - [x] Keep model catalogs separate for Liara and OpenRouter
  - [x] Allow model switching before retry

- [x] Implement OpenAI-compatible chat client
  - [x] Send request with `Authorization: Bearer <apiKey>`
  - [x] Support configurable base URL
  - [x] Support configurable model
  - [x] Set a generous timeout for long translations
  - [x] Decode response content robustly

- [x] Implement translation prompt
  - [x] Request natural Persian translation
  - [x] Preserve meaning and paragraphs
  - [x] Avoid extra explanations
  - [x] Handle long text without truncating where possible

- [x] Add error handling
  - [x] Missing API key
  - [x] Network unavailable
  - [x] 401 authentication errors
  - [x] 429 rate limits
  - [x] Empty or malformed API responses

## Phase 8: Translation Floating Popup

- [x] Build the result popup
  - [x] Show immediately in loading state after selection
  - [x] Use black/gold TextLens styling
  - [x] Keep it compact and draggable
  - [x] Support RTL Persian text
  - [x] Use Vazirmatn or a bundled Persian-friendly font

- [x] Add popup actions
  - [x] Copy translated text
  - [x] Close popup
  - [x] Cancel while loading
  - [x] Retry after error
  - [x] Change model quickly before retry

- [x] Add cost and metadata
  - [x] Show cost badge when available
  - [x] Show selected provider and model subtly
  - [x] Hide irrelevant controls when permission is missing

- [x] Handle long translations
  - [x] Use scrollable RTL text
  - [x] Preserve paragraphs
  - [x] Start at the top when opened from history
  - [x] Keep max popup size within screen bounds

## Phase 9: History

- [ ] Store recent translations
  - [ ] Save the last 5 translation results
  - [ ] Store source text, translated text, provider, model, cost, and timestamp
  - [ ] Avoid storing screenshots

- [ ] Show history in bubble popup or app screen
  - [ ] Add a carousel for recent translations
  - [ ] Tap history item to reopen translation popup
  - [ ] Add clear history action

## Phase 10: End-to-End Flow

- [ ] Wire the full flow
  - [ ] Tap floating bubble
  - [ ] Show selection overlay
  - [ ] Capture selected screen area
  - [ ] Crop image
  - [ ] Run OCR
  - [ ] Translate text
  - [ ] Show popup result

- [ ] Add flow state management
  - [ ] Prevent overlapping selection sessions
  - [ ] Prevent duplicate translation jobs
  - [ ] Cancel active work when popup is dismissed
  - [ ] Restore bubble after success, cancel, or failure

## Phase 11: QA and Release

- [ ] Manual QA
  - [ ] Test fresh install
  - [ ] Test permission denial and recovery
  - [ ] Test bubble movement
  - [ ] Test OCR on common apps
  - [ ] Test translation with Liara
  - [ ] Test translation with OpenRouter
  - [ ] Test retry and model switch
  - [ ] Test history carousel

- [ ] Device coverage
  - [ ] Small phone
  - [ ] Large phone
  - [ ] Tablet if supported
  - [ ] Android 10+
  - [ ] Android 14+

- [ ] Release packaging
  - [ ] Add app signing config
  - [ ] Build release APK
  - [ ] Build AAB if publishing to Play Store
  - [ ] Add privacy notes for screen capture and OCR
  - [ ] Document Android installation steps

## MVP Acceptance Criteria

- [ ] User can start TextLens Android and enable the floating bubble
- [ ] Bubble appears above other apps and can be moved
- [ ] Tapping the bubble starts area selection
- [ ] Selected area is captured and cropped correctly
- [ ] OCR extracts text from the cropped image
- [ ] Text is translated into Persian through the selected provider
- [ ] Result popup is RTL, scrollable, draggable, and copyable
- [ ] Error popup supports retry and quick model switching
- [ ] Last 5 translations are available from history
- [ ] No screenshots are stored on disk

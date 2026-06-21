# TextLens Android Floating Bubble TODO

This document defines the Android implementation plan for TextLens with a floating bubble capture flow.

## Phase 0: Android Project Setup

- [ ] Create the Android app workspace
  - [ ] Add an `android` or `apps/android` folder
  - [ ] Create a Kotlin Android project
  - [ ] Use Jetpack Compose for UI
  - [ ] Set minimum Android version based on `MediaProjection` and overlay requirements
  - [ ] Add a shared product name, package name, and app icon strategy

- [ ] Define the Android source structure
  - [ ] Create `core/` for services and orchestration
  - [ ] Create `ui/` for Compose screens and overlay views
  - [ ] Create `data/` for settings, history, and persistence
  - [ ] Create `ocr/` for ML Kit text recognition
  - [ ] Create `translation/` for OpenRouter and Liara clients

- [ ] Add baseline tooling
  - [ ] Configure Gradle
  - [ ] Add Kotlin serialization or Moshi
  - [ ] Add OkHttp
  - [ ] Add ML Kit Text Recognition
  - [ ] Add lint and basic debug build scripts

## Phase 1: App Shell and Settings

- [ ] Build the main Android app shell
  - [ ] Add a simple first-run screen
  - [ ] Add provider selection: OpenRouter and Liara
  - [ ] Add API key input per provider
  - [ ] Add base URL configuration per provider
  - [ ] Add model selection per provider
  - [ ] Add custom model entry
  - [ ] Add target language setting, defaulting to Persian

- [ ] Persist settings safely
  - [ ] Store non-sensitive settings in DataStore
  - [ ] Store API keys in EncryptedSharedPreferences or Android Keystore-backed storage
  - [ ] Keep Liara and OpenRouter API keys fully separate
  - [ ] Add default Liara model: `openai/gpt-5-nano`
  - [ ] Add default OpenRouter model: `google/gemma-4-31b-it:free`

## Phase 2: Permission Flow

- [ ] Implement overlay permission handling
  - [ ] Detect `SYSTEM_ALERT_WINDOW`
  - [ ] Show a clear permission request screen
  - [ ] Open the correct Android settings page
  - [ ] Re-check permission when the app resumes

- [ ] Implement screen capture permission handling
  - [ ] Use `MediaProjectionManager`
  - [ ] Request screen capture permission before first capture
  - [ ] Cache active capture session while allowed
  - [ ] Handle denied permission gracefully
  - [ ] Add a fallback prompt if the projection token expires

- [ ] Add battery/background guidance
  - [ ] Keep MVP behavior foreground-safe
  - [ ] Use a foreground service only if required for stable capture
  - [ ] Show a persistent notification only when the bubble service is active

## Phase 3: Floating Bubble

- [ ] Create the floating bubble service
  - [ ] Add a foreground-capable service for the bubble
  - [ ] Render a draggable floating bubble above other apps
  - [ ] Use the TextLens black/gold icon style
  - [ ] Save the last bubble position
  - [ ] Keep the bubble compact and unobtrusive

- [ ] Implement bubble interactions
  - [ ] Tap bubble to start area selection
  - [ ] Long press or secondary action to open settings
  - [ ] Drag bubble around screen edges
  - [ ] Snap bubble to screen edge after release
  - [ ] Hide bubble while selecting an area

- [ ] Add bubble lifecycle controls
  - [ ] Start bubble from app
  - [ ] Stop bubble from app
  - [ ] Stop bubble from notification
  - [ ] Restore bubble after device rotation where feasible

## Phase 4: Area Selection Overlay

- [ ] Build the selection overlay
  - [ ] Draw a full-screen transparent overlay
  - [ ] Track touch down, move, and up
  - [ ] Draw the selected rectangle
  - [ ] Make the selected area visually clear
  - [ ] Support cancellation

- [ ] Match TextLens selection behavior
  - [ ] Keep outside area readable, not fully dark
  - [ ] Darken only the selected box slightly
  - [ ] Add a black/gold selection border
  - [ ] Ignore tiny accidental selections
  - [ ] Handle status bar and navigation bar offsets

- [ ] Validate screen geometry
  - [ ] Support portrait mode
  - [ ] Support landscape mode
  - [ ] Support display cutouts
  - [ ] Support different density buckets

## Phase 5: Screen Capture and Crop

- [ ] Capture the screen with `MediaProjection`
  - [ ] Create a virtual display
  - [ ] Use `ImageReader` for frames
  - [ ] Capture one frame after selection completes
  - [ ] Convert frame to bitmap
  - [ ] Release image resources immediately

- [ ] Crop selected area
  - [ ] Map overlay coordinates to captured bitmap coordinates
  - [ ] Crop exactly the selected rectangle
  - [ ] Handle display scaling and rotation
  - [ ] Avoid writing screenshots to disk
  - [ ] Add debug-only crop preview if needed

- [ ] Handle capture errors
  - [ ] Missing projection permission
  - [ ] Empty frame
  - [ ] Invalid crop rectangle
  - [ ] ImageReader timeout

## Phase 6: OCR

- [ ] Integrate ML Kit OCR
  - [ ] Add ML Kit Text Recognition dependency
  - [ ] Run OCR on the cropped bitmap
  - [ ] Extract recognized blocks and lines
  - [ ] Preserve logical line breaks
  - [ ] Trim empty text

- [ ] Optimize OCR behavior
  - [ ] Run OCR off the main thread
  - [ ] Downscale huge crops if needed
  - [ ] Keep enough resolution for small text
  - [ ] Return a clear empty-text error

- [ ] Validate OCR quality
  - [ ] Test web pages
  - [ ] Test screenshots
  - [ ] Test PDFs viewed on Android
  - [ ] Test dark and light backgrounds

## Phase 7: Translation API

- [ ] Port provider model to Android
  - [ ] Define `TranslationProvider`
  - [ ] Define provider-specific defaults
  - [ ] Keep model catalogs separate for Liara and OpenRouter
  - [ ] Allow model switching before retry

- [ ] Implement OpenAI-compatible chat client
  - [ ] Send request with `Authorization: Bearer <apiKey>`
  - [ ] Support configurable base URL
  - [ ] Support configurable model
  - [ ] Set a generous timeout for long translations
  - [ ] Decode response content robustly

- [ ] Implement translation prompt
  - [ ] Request natural Persian translation
  - [ ] Preserve meaning and paragraphs
  - [ ] Avoid extra explanations
  - [ ] Handle long text without truncating where possible

- [ ] Add error handling
  - [ ] Missing API key
  - [ ] Network unavailable
  - [ ] 401 authentication errors
  - [ ] 429 rate limits
  - [ ] Empty or malformed API responses

## Phase 8: Translation Floating Popup

- [ ] Build the result popup
  - [ ] Show immediately in loading state after selection
  - [ ] Use black/gold TextLens styling
  - [ ] Keep it compact and draggable
  - [ ] Support RTL Persian text
  - [ ] Use Vazirmatn or a bundled Persian-friendly font

- [ ] Add popup actions
  - [ ] Copy translated text
  - [ ] Close popup
  - [ ] Cancel while loading
  - [ ] Retry after error
  - [ ] Change model quickly before retry

- [ ] Add cost and metadata
  - [ ] Show cost badge when available
  - [ ] Show selected provider and model subtly
  - [ ] Hide irrelevant controls when permission is missing

- [ ] Handle long translations
  - [ ] Use scrollable RTL text
  - [ ] Preserve paragraphs
  - [ ] Start at the top when opened from history
  - [ ] Keep max popup size within screen bounds

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

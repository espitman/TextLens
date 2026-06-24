# TextLens TV

Android TV app for rendering synced SRT subtitles as a system overlay while YouTube is playing.

The app runs a local Ktor web panel on the TV. Open the panel from another device on the same Wi-Fi, upload a translated `.srt`, and tune the overlay style live.

## Features

- Android TV launcher app.
- `SYSTEM_ALERT_WINDOW` overlay subtitle renderer.
- YouTube playback position sync through `NotificationListenerService` + `MediaSessionManager`.
- Ktor web server on port `5012`.
- React + Tailwind control panel served from the TV.
- Upload translated SRT from the web panel.
- Live controls for:
  - font size
  - text color
  - background color
  - background opacity
  - horizontal position
  - bottom margin
  - max lines
  - debug overlay

## Build

```sh
cd /Users/espitman/Documents/Projects/TextLens/tv-textlens
./gradlew :app:assembleDebug
```

## Install

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Use

1. Open **TextLens TV** on the TV.
2. Grant overlay permission when prompted.
3. Enable **TextLens TV** in Notification Listener settings.
4. Open the panel URL shown on the TV, for example:

   `http://192.168.1.50:5012/`

5. Upload a translated `.srt`.
6. Start YouTube playback.
7. Press **Show Overlay** from the TV app or the web panel.

The panel UI is served by the TV app itself. The browser device must be on the same network as the TV.

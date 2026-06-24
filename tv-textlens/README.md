# TextLens TV

Android TV app for rendering synced SRT subtitles as a system overlay while YouTube is playing.

The app runs a local Ktor web panel on the TV. Open the panel from another device on the same Wi-Fi, upload translated `.srt` files, and tune the overlay style live. Uploaded subtitles are stored on the TV and automatically matched to the active YouTube video.

## Features

- Android TV launcher app.
- `SYSTEM_ALERT_WINDOW` overlay subtitle renderer.
- YouTube playback position sync through `NotificationListenerService` + `MediaSessionManager`.
- Ktor web server on port `5012`.
- Built-in HTML/CSS/JS control panel served from the TV.
- QR code and panel URL shown inside the TV app.
- Upload translated SRT files from the web panel.
- Store multiple SRT files on the TV.
- Bind subtitles to YouTube videos by title, duration, and filename/video-id hints.
- Auto-select a stored subtitle when the matching YouTube video starts playing.
- Hide the subtitle box when the bound video is not currently playing.
- Delete stored subtitle files from the web panel.
- Open a stored subtitle's YouTube video from the TV app when its filename contains a video id.
- Overlay enabled toggle shared between the TV app and web panel.
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

5. Upload one or more translated `.srt` files.
6. Start YouTube playback.
7. Turn **Overlay Enabled** on from the TV app or the web panel.

When the active YouTube title matches a stored subtitle binding, TextLens TV selects that subtitle and renders only the active cue. If the video is paused, an ad or another video is playing, or there is no active cue, the subtitle box is hidden.

The panel UI is served by the TV app itself. The browser device must be on the same network as the TV.

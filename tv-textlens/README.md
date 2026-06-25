# TextLens TV

TextLens TV is an experimental Android TV / Google TV app that renders synced `.srt` subtitles as a system overlay on top of YouTube.

The app is designed for translated subtitles: prepare a Persian `.srt`, upload it to the TV through the built-in web panel, then play the matching YouTube video. TextLens TV reads YouTube playback state through Android media sessions and displays only the active subtitle cue.

## What It Does

- Runs as an Android TV launcher app.
- Shows subtitles through `SYSTEM_ALERT_WINDOW`.
- Syncs with YouTube playback through:
  - `NotificationListenerService`
  - `MediaSessionManager`
- Serves a local web control panel from the TV on port `5012`.
- Shows the panel URL and QR code on the TV.
- Uploads translated `.srt` files from another device on the same Wi-Fi.
- Stores multiple subtitle files on the TV.
- Matches stored subtitles to YouTube videos by:
  - YouTube video id in the file name
  - YouTube title
  - media duration
  - subtitle duration
- Auto-selects a stored subtitle when the matching YouTube video starts.
- Hides the subtitle box when the matching video is not currently playing.
- Lets you tune subtitle style live from the web panel.

## Recommended SRT File Name

For best matching, name the subtitle file with the YouTube video id:

```text
VIDEO_ID-fa.srt
```

Example:

```text
aAPpQC-3EyE-fa.srt
```

TextLens TV can still match by title/duration, but video-id filenames are more reliable.

## Requirements

- Android TV / Google TV device.
- YouTube app installed on the TV.
- Same Wi-Fi network for the TV and the device opening the web panel.
- Overlay permission.
- Notification Listener permission.
- A translated `.srt` file.

## Install Experimental APK

This release is distributed as a sideloaded APK.

1. Enable Developer Options on the TV.
2. Enable USB debugging or network debugging.
3. Connect from your computer:

```sh
adb connect TV_IP:5555
```

4. Install the APK:

```sh
adb install -r TextLens-TV-0.1.0.apk
```

5. Open **TextLens TV** on the TV.

## First Run Setup

1. Open **TextLens TV**.
2. Grant **Display over other apps** permission.
3. Enable **TextLens TV** in Notification Listener settings.
4. Return to TextLens TV.
5. Note the local panel URL, for example:

```text
http://192.168.1.50:5012/
```

6. Open that URL from a phone or computer on the same Wi-Fi.
7. Upload a translated `.srt`.
8. Start the matching YouTube video on the TV.
9. Turn **Overlay Enabled** on if needed.

## Web Panel

The TV app serves its own control panel. No cloud backend is required.

The panel supports:

- Drag and drop SRT upload.
- Stored subtitles list.
- Delete stored subtitles.
- Overlay enable/disable.
- Font size.
- Text color.
- Background color.
- Background opacity.
- Horizontal position.
- Bottom margin.
- Max lines.
- Debug overlay toggle.

## Playback Behavior

TextLens TV only shows the subtitle box when:

- YouTube is the active media session.
- The current video matches a stored subtitle binding.
- Playback is active.
- There is an active subtitle cue for the current position.

If another video is playing, an ad is detected, playback is paused, or there is no matching cue, the subtitle box is hidden.

## Build Debug

```sh
cd /Users/espitman/Documents/Projects/TextLens/tv-textlens
./gradlew :app:assembleDebug
```

Install debug APK:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or from the repository root:

```sh
./scripts/tv_android_run.sh
```

## Build Release

From the repository root:

```sh
./scripts/tv_android_release.sh
```

The release script:

- Builds `tv-textlens`, not the mobile Android app.
- Creates/reuses the local TV release keystore.
- Signs the release APK.
- Copies the APK to the Desktop.

Output:

```text
~/Desktop/TextLens-TV-0.1.0.apk
```

Keep the release keystore safe:

```text
tv-textlens/keystores/textlens-tv-release.jks
```

Future updates must be signed with the same keystore to install over the previous version.

## Known Limitations

- This is an experimental sideloaded APK.
- It is not published on Google Play.
- Matching depends on YouTube's Android media session metadata.
- Some YouTube app updates may change media metadata behavior.
- VPN/proxy setups can break access to the local web panel.
- The app does not download or translate subtitles.
- You must provide the translated SRT yourself.
- Notification Listener permission can be vendor-specific on some TVs.

## Privacy

TextLens TV stores uploaded SRT files locally on the TV.

The web panel is served locally from the TV to devices on the same network. The app does not include analytics or telemetry.

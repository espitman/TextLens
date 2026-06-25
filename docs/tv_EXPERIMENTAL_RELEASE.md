# TextLens TV Experimental Release Guide

This document describes how to publish TextLens TV as a GitHub experimental APK release.

Release type:

```text
Experimental Android TV / Google TV APK
Sideloaded
Signed with local release keystore
No Google Play distribution
No backend
```

## 1. Preflight

From the repository root:

```sh
git status --short
JAVA_HOME="$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./tv-textlens/gradlew -p tv-textlens :app:assembleDebug
```

Expected:

- Debug build succeeds.
- Working tree contains only intentional release changes.

## 2. Build Release APK

From the repository root:

```sh
./scripts/tv_android_release.sh
```

Output:

```text
~/Desktop/TextLens-TV-0.1.0.apk
```

The script signs the APK with:

```text
tv-textlens/keystores/textlens-tv-release.jks
```

Keep this keystore safe. Future versions must use the same key to install as updates.

## 3. Local Smoke Test On TV

1. Enable Developer Options on the Android TV / Google TV device.
2. Enable network debugging.
3. Connect with adb:

```sh
adb connect TV_IP:5555
```

4. Install release APK:

```sh
adb install -r ~/Desktop/TextLens-TV-0.1.0.apk
```

5. Open **TextLens TV**.
6. Grant **Display over other apps** permission.
7. Enable **TextLens TV** in Notification Listener settings.
8. Open the panel URL shown on the TV from another device on the same Wi-Fi.
9. Upload a translated SRT named like:

```text
VIDEO_ID-fa.srt
```

10. Start the matching video in YouTube on the TV.
11. Confirm subtitles appear only while the matching video is playing.
12. Pause, seek, and resume playback.
13. Confirm the subtitle box hides when the wrong video is playing.
14. Confirm the web panel style controls update the overlay.

## 4. GitHub Release

Create a new GitHub release:

```text
Tag: tv-v0.1.0
Title: TextLens TV 0.1.0 Experimental
Attachment: ~/Desktop/TextLens-TV-0.1.0.apk
```

Use the release notes below.

## 5. Release Notes

```markdown
# TextLens TV 0.1.0 Experimental

TextLens TV is an Android TV / Google TV app that renders synced Persian SRT subtitles over YouTube as a system overlay.

Upload a translated `.srt` from the local web panel, start the matching YouTube video, and TextLens TV displays the active subtitle cue over the video.

## Features

- Android TV launcher app.
- System overlay subtitle renderer.
- YouTube playback sync through Android media sessions.
- Local web panel served from the TV on port 5012.
- Drag and drop SRT upload.
- Stored subtitles on the TV.
- Auto-match by YouTube video id, title, and duration.
- Overlay hides when the matching video is not playing.
- Live controls for font size, colors, opacity, position, bottom margin, max lines, and debug overlay.
- No backend.
- No analytics.

## Requirements

- Android TV / Google TV device.
- YouTube app installed.
- Overlay permission.
- Notification Listener permission.
- Same Wi-Fi network for the TV and web panel device.
- A translated SRT file.

## Install

This is a sideloaded experimental APK.

```sh
adb connect TV_IP:5555
adb install -r TextLens-TV-0.1.0.apk
```

Then open TextLens TV, grant overlay permission, enable Notification Listener access, and upload your SRT from the panel URL shown on the TV.

## Recommended File Name

Use the YouTube video id in the SRT filename:

```text
VIDEO_ID-fa.srt
```

Example:

```text
aAPpQC-3EyE-fa.srt
```

## Known Limitations

- Experimental sideloaded APK.
- Not available on Google Play.
- SRT download/translation is not included.
- Matching depends on YouTube media session metadata.
- VPN or network isolation can prevent the browser from reaching the TV web panel.
```

## 6. LinkedIn Short Copy

```text
I’m preparing an experimental Android TV release of TextLens TV.

TextLens TV renders synced Persian SRT subtitles over YouTube as a system overlay. The app runs fully locally on the TV: it serves a web panel on the local network, accepts translated SRT uploads, stores subtitles on the device, and auto-matches them when the corresponding YouTube video starts.

It is a sideloaded experimental APK for Android TV / Google TV, built for people who want custom translated subtitles on TV without a backend.
```

## 7. Launch Checklist

- [ ] Build release APK.
- [ ] Install release APK on a real TV.
- [ ] Grant overlay permission.
- [ ] Grant Notification Listener permission.
- [ ] Open web panel from another device.
- [ ] Upload `VIDEO_ID-fa.srt`.
- [ ] Start matching YouTube video.
- [ ] Confirm subtitles sync after play, pause, seek.
- [ ] Confirm overlay hides on non-matching video.
- [ ] Capture screenshots/photos for GitHub and LinkedIn.
- [ ] Publish GitHub release with APK attachment.

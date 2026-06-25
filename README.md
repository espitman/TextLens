# TextLens

TextLens is a cross-platform toolkit for translating text and subtitles into Persian.

The macOS app is available as an experimental GitHub release. It runs as a menu bar app: press `Command + Shift + 0`, select an area of the screen, and TextLens reads the text with OCR and shows a Persian translation popup.

## Repository Structure

```text
TextLens/
  android/       Android app implementation
  macos/         macOS app implementation
  tv-textlens/   Android TV subtitle overlay app
  youtube-textlens-chrome/
                 Chrome extension for rendering local SRT on YouTube
  youtube-textlens-translator/
                 macOS helper app for translating SRT files
  docs/          product specs and implementation plans
  site/          GitHub Pages landing page
  scripts/       release and packaging scripts
```

## TextLens macOS

The macOS app lives in:

```text
macos
```

Build it locally with:

```sh
cd macos
swift build
```

Build, install, and run it locally from the repository root with:

```sh
./run
```

The script creates `/Applications/TextLens.app` and opens it. This gives macOS a stable app identity for Screen Recording permission.

Create an experimental DMG release with:

```sh
./scripts/build-dmg.sh 0.1.0
```

The DMG is written to:

```text
dist/TextLens-0.1.0.dmg
```

Release guide:

- [macOS experimental release guide](docs/macos_EXPERIMENTAL_RELEASE.md)
- [macOS app README](macos/README.md)

## Android App

The Android app is scaffolded in:

```text
android
```

Build it with:

```sh
cd android
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :app:assembleDebug
```

Build, install, and run it on an emulator from the repository root with:

```sh
./scripts/android_run.sh
```

Build an unsigned release APK with:

```sh
./scripts/android_release.sh
```

## TextLens TV

The Android TV / Google TV subtitle overlay app lives in:

```text
tv-textlens
```

It renders synced SRT subtitles over YouTube and serves a local web panel for uploading translated subtitle files and tuning the overlay style.

Build a signed experimental TV APK with:

```sh
./scripts/tv_android_release.sh
```

The APK is copied to:

```text
~/Desktop/TextLens-TV-0.1.0.apk
```

Release guide:

- [TextLens TV README](tv-textlens/README.md)
- [TV experimental release guide](docs/tv_EXPERIMENTAL_RELEASE.md)

## TextLens YouTube Chrome

The Chrome extension lives in:

```text
youtube-textlens-chrome
```

It renders local `.srt` subtitles over the YouTube player. It does not require `npm install`, a build step, a backend, or Chrome Web Store distribution.

Package the extension for GitHub release with:

```sh
./scripts/package_chrome_extension.sh
```

The zip is written to:

```text
dist/TextLens-YouTube-Chrome-0.1.0.zip
```

Release guide:

- [TextLens YouTube Chrome README](youtube-textlens-chrome/README.md)
- [Chrome experimental release guide](docs/chrome_EXPERIMENTAL_RELEASE.md)

## YouTube TextLens Translator

The macOS desktop helper for translating SRT files lives in:

```text
youtube-textlens-translator
```

It packages as a ready-to-run macOS DMG, so end users do not need `npm install`, Node.js, or a build step.

Package it for GitHub release with:

```sh
./scripts/package_youtube_translator_macos.sh
```

The DMG is written to:

```text
dist/YouTube-TextLens-Translator-0.1.0-macOS.dmg
```

Release guide:

- [YouTube TextLens Translator README](youtube-textlens-translator/README.md)
- [YouTube TextLens Translator experimental release guide](docs/youtube_TRANSLATOR_EXPERIMENTAL_RELEASE.md)

## Latest Release

- [Download TextLens](https://github.com/espitman/TextLens/releases/latest)

## Website

The GitHub Pages site lives in:

```text
site
```

It is deployed by `.github/workflows/pages.yml`.

## Documentation

- [Implementation spec](docs/TextLens_Codex_Spec.md)
- [macOS MVP TODO](docs/macos_TODO.md)
- [macOS experimental release guide](docs/macos_EXPERIMENTAL_RELEASE.md)
- [Android TODO](docs/ANDROID_TODO.md)
- [TV experimental release guide](docs/tv_EXPERIMENTAL_RELEASE.md)
- [Chrome experimental release guide](docs/chrome_EXPERIMENTAL_RELEASE.md)
- [YouTube TextLens Translator experimental release guide](docs/youtube_TRANSLATOR_EXPERIMENTAL_RELEASE.md)

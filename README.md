# TextLens

TextLens is a cross-platform project for translating text from selected screen areas into Persian.

The first implementation target is macOS. Android and other platforms are planned for later.

## Repository Structure

```text
TextLens/
  android/       Android app implementation
  macos/         macOS app implementation
  docs/          product specs and implementation plans
  site/          GitHub Pages landing page
  scripts/       release and packaging scripts
```

## Current App

The macOS app is scaffolded in:

```text
macos
```

Build it with:

```sh
cd macos
swift build
```

Build, install, and run it from the repository root with:

```sh
./run
```

The script creates `/Applications/TextLens.app` and opens it. This gives macOS a stable app identity for Screen Recording permission.

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

## Release Build

Create a distributable macOS DMG with:

```sh
./scripts/build-dmg.sh 0.1.0
```

The DMG is written to `dist/TextLens-0.1.0.dmg`.

Latest release:

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
- [Android TODO](docs/ANDROID_TODO.md)

#!/bin/bash

set -euo pipefail

PROJECT_ROOT="/Users/espitman/Documents/Projects/TextLens"
ANDROID_DIR="$PROJECT_ROOT/android"
DESKTOP_DIR="$HOME/Desktop"

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

if [ ! -x "$ANDROID_DIR/gradlew" ]; then
  echo "Error: gradlew not found at $ANDROID_DIR/gradlew"
  exit 1
fi

echo "Building TextLens release APK..."
cd "$ANDROID_DIR"
./gradlew :app:assembleRelease

APK_PATH="$ANDROID_DIR/app/build/outputs/apk/release/app-release.apk"

if [ ! -f "$APK_PATH" ]; then
  echo "Error: Release APK not found in $ANDROID_DIR/app/build/outputs/apk/release"
  exit 1
fi

OUT_APK="$DESKTOP_DIR/textlens-release.apk"
cp "$APK_PATH" "$OUT_APK"

echo "Release APK copied to:"
echo "$OUT_APK"

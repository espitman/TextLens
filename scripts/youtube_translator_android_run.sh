#!/bin/bash

set -euo pipefail

PROJECT_ROOT="/Users/espitman/Documents/Projects/TextLens"
ANDROID_DIR="$PROJECT_ROOT/youtube-textlens-translator-android"
APK_PATH="$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"
APP_ID="com.textlens.youtubetranslatorandroid"
MAIN_ACTIVITY=".MainActivity"
AVD_NAME="${1:-Medium_Phone_API_36.0}"

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"

ADB_BIN="$ANDROID_SDK_ROOT/platform-tools/adb"
EMU_BIN="$ANDROID_SDK_ROOT/emulator/emulator"

echo "Starting YouTube TextLens Translator Android..."
echo "Target AVD: $AVD_NAME"

if [ ! -x "$ADB_BIN" ]; then
  echo "Error: adb not found at $ADB_BIN"
  exit 1
fi

if [ ! -x "$EMU_BIN" ]; then
  echo "Error: emulator not found at $EMU_BIN"
  exit 1
fi

if [ ! -x "$ANDROID_DIR/gradlew" ]; then
  echo "Error: gradlew not found at $ANDROID_DIR/gradlew"
  exit 1
fi

SERIAL="$($ADB_BIN devices | awk '$1 ~ /^emulator-/ && $2 == "device" {print $1; exit}')"

if [ -z "$SERIAL" ]; then
  echo "No online emulator found. Booting $AVD_NAME..."
  nohup "$EMU_BIN" -avd "$AVD_NAME" -gpu swiftshader_indirect -no-snapshot-load -no-boot-anim >/tmp/youtube_textlens_translator_android_emulator.log 2>&1 &

  for _ in $(seq 1 180); do
    SERIAL="$($ADB_BIN devices | awk '$1 ~ /^emulator-/ && $2 == "device" {print $1; exit}')"
    if [ -n "$SERIAL" ]; then
      break
    fi
    sleep 2
  done
fi

if [ -z "${SERIAL:-}" ]; then
  echo "Error: Emulator did not come online in time."
  "$ADB_BIN" devices -l || true
  exit 1
fi

"$ADB_BIN" -s "$SERIAL" wait-for-device
for _ in $(seq 1 120); do
  BOOT="$("$ADB_BIN" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
  if [ "$BOOT" = "1" ]; then
    break
  fi
  sleep 2
done

cd "$ANDROID_DIR"
./gradlew :app:assembleDebug

if [ ! -f "$APK_PATH" ]; then
  echo "Error: APK not found at $APK_PATH"
  exit 1
fi

"$ADB_BIN" -s "$SERIAL" install -r -t "$APK_PATH"
"$ADB_BIN" -s "$SERIAL" shell am start -W -n "$APP_ID/$MAIN_ACTIVITY"

echo "Success."

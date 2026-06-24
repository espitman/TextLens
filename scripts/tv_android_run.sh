#!/bin/bash

set -euo pipefail

PROJECT_ROOT="/Users/espitman/Documents/Projects/TextLens"
TV_DIR="$PROJECT_ROOT/tv-textlens"
APK_PATH="$TV_DIR/app/build/outputs/apk/debug/app-debug.apk"
APP_ID="com.textlens.tv"
MAIN_ACTIVITY=".MainActivity"
SERIAL="${1:-${ANDROID_SERIAL:-}}"

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"

if [ -z "${JAVA_HOME:-}" ]; then
  if [ -x "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" ]; then
    JAVA_HOME="$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  elif [ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" ]; then
    JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  else
    JAVA_HOME="/opt/homebrew/opt/openjdk@17"
  fi
fi

PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
ADB_BIN="$ANDROID_SDK_ROOT/platform-tools/adb"

echo "Starting TextLens TV build and launch process..."

if [ ! -x "$ADB_BIN" ]; then
  echo "Error: adb not found at $ADB_BIN"
  exit 1
fi

if [ ! -x "$TV_DIR/gradlew" ]; then
  echo "Error: gradlew not found at $TV_DIR/gradlew"
  exit 1
fi

if [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "Error: Java runtime not found at $JAVA_HOME/bin/java"
  echo "Set JAVA_HOME to Android Studio JBR or an installed JDK 17."
  exit 1
fi

if [ -z "$SERIAL" ]; then
  SERIAL="$($ADB_BIN devices | awk 'NR > 1 && $2 == "device" {print $1; exit}')"
fi

if [ -z "$SERIAL" ]; then
  echo "Error: no adb device is online."
  echo "For Sony Google TV, enable Developer options and run:"
  echo "  adb connect TV_IP:5555"
  "$ADB_BIN" devices -l || true
  exit 1
fi

echo "Target device: $SERIAL"
echo "Building TextLens TV debug APK..."
cd "$TV_DIR"
./gradlew :app:assembleDebug

if [ ! -f "$APK_PATH" ]; then
  echo "Error: APK not found at $APK_PATH"
  exit 1
fi

echo "Installing APK..."
"$ADB_BIN" -s "$SERIAL" install -r -t "$APK_PATH"

echo "Launching $APP_ID/$MAIN_ACTIVITY..."
"$ADB_BIN" -s "$SERIAL" shell am start -W -n "$APP_ID/$MAIN_ACTIVITY"

echo "Success."

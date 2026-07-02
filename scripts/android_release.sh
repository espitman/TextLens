#!/bin/bash

set -euo pipefail

PROJECT_ROOT="/Users/espitman/Documents/Projects/TextLens"
ANDROID_DIR="$PROJECT_ROOT/android"
DESKTOP_DIR="$HOME/Desktop"
DIST_DIR="$PROJECT_ROOT/dist"
VERSION_NAME="${VERSION_NAME:-0.1.1}"

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
KEYSTORE_DIR="$ANDROID_DIR/keystores"
KEYSTORE_PATH="${TEXTLENS_KEYSTORE_PATH:-$KEYSTORE_DIR/textlens-release.jks}"
KEYSTORE_PASSWORD="${TEXTLENS_KEYSTORE_PASSWORD:-TextLensRelease123!}"
KEY_ALIAS="${TEXTLENS_KEY_ALIAS:-textlens}"
KEY_PASSWORD="${TEXTLENS_KEY_PASSWORD:-$KEYSTORE_PASSWORD}"
LOCAL_PROPERTIES="$ANDROID_DIR/local.properties"

if [ ! -x "$ANDROID_DIR/gradlew" ]; then
  echo "Error: gradlew not found at $ANDROID_DIR/gradlew"
  exit 1
fi

if [ ! -x "$JAVA_HOME/bin/keytool" ]; then
  echo "Error: keytool not found at $JAVA_HOME/bin/keytool"
  exit 1
fi

mkdir -p "$KEYSTORE_DIR" "$DIST_DIR"

if [ ! -f "$KEYSTORE_PATH" ]; then
  echo "Creating local TextLens release keystore..."
  "$JAVA_HOME/bin/keytool" \
    -genkeypair \
    -v \
    -keystore "$KEYSTORE_PATH" \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=TextLens, OU=TextLens, O=TextLens, L=Tehran, ST=Tehran, C=IR"
fi

cat > "$LOCAL_PROPERTIES" <<EOF
TEXTLENS_KEYSTORE_PATH=$KEYSTORE_PATH
TEXTLENS_KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD
TEXTLENS_KEY_ALIAS=$KEY_ALIAS
TEXTLENS_KEY_PASSWORD=$KEY_PASSWORD
EOF

echo "Building TextLens release APK..."
cd "$ANDROID_DIR"
./gradlew :app:assembleRelease

APK_PATH="$ANDROID_DIR/app/build/outputs/apk/release/app-release.apk"

if [ ! -f "$APK_PATH" ]; then
  echo "Error: Release APK not found in $ANDROID_DIR/app/build/outputs/apk/release"
  exit 1
fi

OUT_NAME="TextLens-Android-$VERSION_NAME.apk"
OUT_APK="$DESKTOP_DIR/$OUT_NAME"
cp "$APK_PATH" "$OUT_APK"
cp "$APK_PATH" "$DIST_DIR/$OUT_NAME"

echo "Release APK copied to:"
echo "$OUT_APK"
echo "$DIST_DIR/$OUT_NAME"

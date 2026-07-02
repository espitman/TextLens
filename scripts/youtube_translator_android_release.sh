#!/bin/bash

set -euo pipefail

PROJECT_ROOT="/Users/espitman/Documents/Projects/TextLens"
ANDROID_DIR="$PROJECT_ROOT/youtube-textlens-translator-android"
DESKTOP_DIR="$HOME/Desktop"
DIST_DIR="$PROJECT_ROOT/dist"
VERSION_NAME="${VERSION_NAME:-0.1.2}"

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/build-tools/35.0.0:$PATH"

KEYSTORE_DIR="$ANDROID_DIR/keystores"
KEYSTORE_PATH="${YTLENS_TRANSLATOR_KEYSTORE_PATH:-$KEYSTORE_DIR/youtube-textlens-translator-android-release.jks}"
KEYSTORE_PASSWORD="${YTLENS_TRANSLATOR_KEYSTORE_PASSWORD:-TextLensTranslatorAndroid123!}"
KEY_ALIAS="${YTLENS_TRANSLATOR_KEY_ALIAS:-youtube-textlens-translator-android}"
KEY_PASSWORD="${YTLENS_TRANSLATOR_KEY_PASSWORD:-$KEYSTORE_PASSWORD}"
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
  echo "Creating local YouTube TextLens Translator Android release keystore..."
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
    -dname "CN=YouTube TextLens Translator Android, OU=TextLens, O=TextLens, L=Tehran, ST=Tehran, C=IR"
fi

cat > "$LOCAL_PROPERTIES" <<EOF
YTLENS_TRANSLATOR_KEYSTORE_PATH=$KEYSTORE_PATH
YTLENS_TRANSLATOR_KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD
YTLENS_TRANSLATOR_KEY_ALIAS=$KEY_ALIAS
YTLENS_TRANSLATOR_KEY_PASSWORD=$KEY_PASSWORD
EOF

cd "$ANDROID_DIR"
./gradlew :app:assembleRelease

APK_PATH="$ANDROID_DIR/app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$APK_PATH" ]; then
  echo "Error: Release APK not found at $APK_PATH"
  exit 1
fi

OUT_NAME="youtube-textlens-translator-android-$VERSION_NAME-release.apk"
cp "$APK_PATH" "$DESKTOP_DIR/$OUT_NAME"
cp "$APK_PATH" "$DIST_DIR/$OUT_NAME"

echo "Release APK copied to:"
echo "$DESKTOP_DIR/$OUT_NAME"
echo "$DIST_DIR/$OUT_NAME"

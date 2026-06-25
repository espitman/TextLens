#!/bin/bash

set -euo pipefail

PROJECT_ROOT="/Users/espitman/Documents/Projects/TextLens"
TV_DIR="$PROJECT_ROOT/tv-textlens"
DESKTOP_DIR="$HOME/Desktop"
VERSION_NAME="${VERSION_NAME:-0.1.0}"

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"

if [ -z "${JAVA_HOME:-}" ]; then
  if [ -x "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" ]; then
    JAVA_HOME="$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  elif [ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" ]; then
    JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  else
    JAVA_HOME="/opt/homebrew/opt/openjdk@17"
  fi
fi

PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
KEYSTORE_DIR="$TV_DIR/keystores"
KEYSTORE_PATH="${TEXTLENS_TV_KEYSTORE_PATH:-$KEYSTORE_DIR/textlens-tv-release.jks}"
KEYSTORE_PASSWORD="${TEXTLENS_TV_KEYSTORE_PASSWORD:-TextLensTvRelease123!}"
KEY_ALIAS="${TEXTLENS_TV_KEY_ALIAS:-textlens-tv}"
KEY_PASSWORD="${TEXTLENS_TV_KEY_PASSWORD:-$KEYSTORE_PASSWORD}"
LOCAL_PROPERTIES="$TV_DIR/local.properties"

if [ ! -x "$TV_DIR/gradlew" ]; then
  echo "Error: gradlew not found at $TV_DIR/gradlew"
  exit 1
fi

if [ ! -x "$JAVA_HOME/bin/keytool" ]; then
  echo "Error: keytool not found at $JAVA_HOME/bin/keytool"
  echo "Set JAVA_HOME to Android Studio JBR or an installed JDK 17."
  exit 1
fi

mkdir -p "$KEYSTORE_DIR"

if [ ! -f "$KEYSTORE_PATH" ]; then
  echo "Creating local TextLens TV release keystore..."
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
    -dname "CN=TextLens TV, OU=TextLens, O=TextLens, L=Tehran, ST=Tehran, C=IR"
fi

cat > "$LOCAL_PROPERTIES" <<EOF
TEXTLENS_TV_KEYSTORE_PATH=$KEYSTORE_PATH
TEXTLENS_TV_KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD
TEXTLENS_TV_KEY_ALIAS=$KEY_ALIAS
TEXTLENS_TV_KEY_PASSWORD=$KEY_PASSWORD
EOF

echo "Building TextLens TV release APK..."
cd "$TV_DIR"
./gradlew :app:assembleRelease

APK_PATH="$TV_DIR/app/build/outputs/apk/release/app-release.apk"

if [ ! -f "$APK_PATH" ]; then
  echo "Error: release APK not found at $APK_PATH"
  exit 1
fi

OUT_APK="$DESKTOP_DIR/TextLens-TV-$VERSION_NAME.apk"
cp "$APK_PATH" "$OUT_APK"

echo "Release APK copied to:"
echo "$OUT_APK"

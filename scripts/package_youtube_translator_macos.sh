#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_SOURCE_DIR="$PROJECT_ROOT/youtube-textlens-translator"
ELECTRON_APP="$APP_SOURCE_DIR/node_modules/electron/dist/Electron.app"
DIST_DIR="$PROJECT_ROOT/dist"
VERSION="${VERSION:-0.1.0}"
APP_NAME="YouTube TextLens Translator"
BUNDLE_ID="com.textlens.youtube-translator"
PACKAGE_NAME="YouTube-TextLens-Translator-$VERSION"
WORK_DIR="$DIST_DIR/$PACKAGE_NAME"
APP_BUNDLE="$WORK_DIR/$APP_NAME.app"
DMG_PATH="$DIST_DIR/$PACKAGE_NAME-macOS.dmg"
PLIST="$APP_BUNDLE/Contents/Info.plist"
APP_RESOURCES="$APP_BUNDLE/Contents/Resources"
APP_CODE_DIR="$APP_RESOURCES/app"
ICON_SOURCE_SVG="$PROJECT_ROOT/site/assets/textlens-icon.svg"
APP_ICON="$DIST_DIR/$PACKAGE_NAME.icns"
PLIST_BUDDY="/usr/libexec/PlistBuddy"

required_files=(
  "package.json"
  "main.js"
  "preload.js"
  "index.html"
  "styles.css"
  "renderer.js"
  "translator.js"
  "README.md"
)

if [ ! -d "$ELECTRON_APP" ]; then
  echo "Error: Electron runtime not found at:"
  echo "$ELECTRON_APP"
  echo
  echo "Install development dependencies once on the maintainer machine:"
  echo "  cd $APP_SOURCE_DIR && npm install"
  exit 1
fi

for file in "${required_files[@]}"; do
  if [ ! -f "$APP_SOURCE_DIR/$file" ]; then
    echo "Error: missing required app file: $file"
    exit 1
  fi
done

mkdir -p "$DIST_DIR"
rm -rf "$WORK_DIR" "$DMG_PATH" "$APP_ICON"
mkdir -p "$WORK_DIR"

echo "Creating $APP_NAME.app..."
cp -R "$ELECTRON_APP" "$APP_BUNDLE"

mv "$APP_BUNDLE/Contents/MacOS/Electron" "$APP_BUNDLE/Contents/MacOS/$APP_NAME"

"$PLIST_BUDDY" -c "Set :CFBundleExecutable $APP_NAME" "$PLIST"
"$PLIST_BUDDY" -c "Set :CFBundleName $APP_NAME" "$PLIST"
"$PLIST_BUDDY" -c "Set :CFBundleDisplayName $APP_NAME" "$PLIST"
"$PLIST_BUDDY" -c "Set :CFBundleIdentifier $BUNDLE_ID" "$PLIST"
"$PLIST_BUDDY" -c "Set :CFBundleShortVersionString $VERSION" "$PLIST"
"$PLIST_BUDDY" -c "Set :CFBundleVersion $VERSION" "$PLIST"

if [ -f "$ICON_SOURCE_SVG" ] && command -v magick >/dev/null 2>&1 && command -v iconutil >/dev/null 2>&1; then
  ICONSET="$DIST_DIR/$PACKAGE_NAME.iconset"
  rm -rf "$ICONSET"
  mkdir -p "$ICONSET"
  magick -background none "$ICON_SOURCE_SVG" -resize 16x16 "$ICONSET/icon_16x16.png"
  magick -background none "$ICON_SOURCE_SVG" -resize 32x32 "$ICONSET/icon_16x16@2x.png"
  magick -background none "$ICON_SOURCE_SVG" -resize 32x32 "$ICONSET/icon_32x32.png"
  magick -background none "$ICON_SOURCE_SVG" -resize 64x64 "$ICONSET/icon_32x32@2x.png"
  magick -background none "$ICON_SOURCE_SVG" -resize 128x128 "$ICONSET/icon_128x128.png"
  magick -background none "$ICON_SOURCE_SVG" -resize 256x256 "$ICONSET/icon_128x128@2x.png"
  magick -background none "$ICON_SOURCE_SVG" -resize 256x256 "$ICONSET/icon_256x256.png"
  magick -background none "$ICON_SOURCE_SVG" -resize 512x512 "$ICONSET/icon_256x256@2x.png"
  magick -background none "$ICON_SOURCE_SVG" -resize 512x512 "$ICONSET/icon_512x512.png"
  magick -background none "$ICON_SOURCE_SVG" -resize 1024x1024 "$ICONSET/icon_512x512@2x.png"
  iconutil -c icns "$ICONSET" -o "$APP_ICON"
  rm -rf "$ICONSET"
elif [ -f "$PROJECT_ROOT/macos/Sources/TextLens/Resources/AppIcon.icns" ]; then
  cp "$PROJECT_ROOT/macos/Sources/TextLens/Resources/AppIcon.icns" "$APP_ICON"
fi

if [ -f "$APP_ICON" ]; then
  cp "$APP_ICON" "$APP_RESOURCES/AppIcon.icns"
  "$PLIST_BUDDY" -c "Set :CFBundleIconFile AppIcon.icns" "$PLIST"
fi

rm -rf "$APP_RESOURCES/default_app.asar"
mkdir -p "$APP_CODE_DIR"

for file in "${required_files[@]}"; do
  cp "$APP_SOURCE_DIR/$file" "$APP_CODE_DIR/$file"
done

cat > "$WORK_DIR/INSTALL.txt" <<EOF
YouTube TextLens Translator $VERSION

Install:
1. Drag "$APP_NAME.app" to Applications.
2. Right-click the app and choose Open on first launch.
3. Confirm the macOS unsigned-app warning.

No npm install is required.
EOF

echo "Signing app bundle with ad-hoc identity..."
codesign --force --deep --sign - "$APP_BUNDLE"

echo "Creating DMG package..."
if command -v create-dmg >/dev/null 2>&1; then
  create-dmg \
    --volname "$APP_NAME $VERSION" \
    --volicon "$APP_RESOURCES/AppIcon.icns" \
    --window-pos 200 120 \
    --window-size 640 420 \
    --icon-size 96 \
    --icon "$APP_NAME.app" 170 190 \
    --app-drop-link 460 190 \
    --no-internet-enable \
    "$DMG_PATH" \
    "$WORK_DIR"
else
  hdiutil create \
    -volname "$APP_NAME $VERSION" \
    -srcfolder "$WORK_DIR" \
    -ov \
    -format UDZO \
    "$DMG_PATH"
fi

rm -rf "$WORK_DIR"
rm -f "$APP_ICON"

echo "Created macOS package:"
echo "$DMG_PATH"

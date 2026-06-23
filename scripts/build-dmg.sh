#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MACOS_DIR="$ROOT_DIR/macos"
APP_NAME="TextLens"
BUNDLE_IDENTIFIER="com.textlens.app"
VERSION="${1:-0.1.0}"
BUILD_NUMBER="${BUILD_NUMBER:-$(date +%Y%m%d%H%M%S)}"
CODESIGN_IDENTITY="${CODESIGN_IDENTITY:--}"
DIST_DIR="$ROOT_DIR/dist"
WORK_DIR="$DIST_DIR/release-work"
APP_BUNDLE="$WORK_DIR/$APP_NAME.app"
DMG_PATH="$DIST_DIR/$APP_NAME-$VERSION.dmg"
VOLUME_NAME="$APP_NAME $VERSION"

echo "Building $APP_NAME $VERSION..."
cd "$MACOS_DIR"
swift build -c release
BIN_DIR="$(swift build -c release --show-bin-path)"

echo "Creating app bundle..."
rm -rf "$WORK_DIR"
mkdir -p "$APP_BUNDLE/Contents/MacOS" "$APP_BUNDLE/Contents/Resources"

cp "$BIN_DIR/$APP_NAME" "$APP_BUNDLE/Contents/MacOS/$APP_NAME"
chmod +x "$APP_BUNDLE/Contents/MacOS/$APP_NAME"

if [ -d "$BIN_DIR/TextLens_TextLens.bundle" ]; then
  cp -R "$BIN_DIR/TextLens_TextLens.bundle" "$APP_BUNDLE/Contents/Resources/"
fi

if [ -f "$MACOS_DIR/Sources/TextLens/Resources/AppIcon.icns" ]; then
  cp "$MACOS_DIR/Sources/TextLens/Resources/AppIcon.icns" "$APP_BUNDLE/Contents/Resources/AppIcon.icns"
fi

cat > "$APP_BUNDLE/Contents/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>$APP_NAME</string>
    <key>CFBundleIdentifier</key>
    <string>$BUNDLE_IDENTIFIER</string>
    <key>CFBundleName</key>
    <string>$APP_NAME</string>
    <key>CFBundleDisplayName</key>
    <string>$APP_NAME</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon.icns</string>
    <key>CFBundleIconName</key>
    <string>AppIcon</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>$VERSION</string>
    <key>CFBundleVersion</key>
    <string>$BUILD_NUMBER</string>
    <key>LSMinimumSystemVersion</key>
    <string>13.0</string>
    <key>LSUIElement</key>
    <true/>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
PLIST

echo "Signing app bundle..."
codesign --force --deep --sign "$CODESIGN_IDENTITY" "$APP_BUNDLE"

echo "Creating DMG..."
rm -f "$DMG_PATH"
ln -s /Applications "$WORK_DIR/Applications"
hdiutil create \
  -volname "$VOLUME_NAME" \
  -srcfolder "$WORK_DIR" \
  -ov \
  -format UDZO \
  "$DMG_PATH"

rm -rf "$WORK_DIR"
echo "Created $DMG_PATH"

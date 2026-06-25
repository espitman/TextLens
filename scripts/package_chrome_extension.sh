#!/bin/bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EXTENSION_DIR="$PROJECT_ROOT/youtube-textlens-chrome"
DIST_DIR="$PROJECT_ROOT/dist"
VERSION="${VERSION:-0.1.0}"
PACKAGE_NAME="TextLens-YouTube-Chrome-$VERSION"
WORK_DIR="$DIST_DIR/$PACKAGE_NAME"
ZIP_PATH="$DIST_DIR/$PACKAGE_NAME.zip"

required_files=(
  "manifest.json"
  "content.js"
  "popup.html"
  "popup.css"
  "popup.js"
  "README.md"
)

if [ ! -d "$EXTENSION_DIR" ]; then
  echo "Error: extension directory not found: $EXTENSION_DIR"
  exit 1
fi

for file in "${required_files[@]}"; do
  if [ ! -f "$EXTENSION_DIR/$file" ]; then
    echo "Error: missing required extension file: $file"
    exit 1
  fi
done

mkdir -p "$DIST_DIR"
rm -rf "$WORK_DIR" "$ZIP_PATH"
mkdir -p "$WORK_DIR"

for file in "${required_files[@]}"; do
  cp "$EXTENSION_DIR/$file" "$WORK_DIR/$file"
done

(
  cd "$DIST_DIR"
  zip -qr "$ZIP_PATH" "$PACKAGE_NAME"
)

rm -rf "$WORK_DIR"

echo "Created Chrome extension package:"
echo "$ZIP_PATH"

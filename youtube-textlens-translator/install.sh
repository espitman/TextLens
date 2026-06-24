#!/bin/bash
# Move to the script's directory
cd "$(dirname "$0")"

echo "=== 1. Cleaning old Electron installation ==="
rm -rf node_modules/electron

echo "=== 2. Installing dependencies using official registry and fast mirror ==="
ELECTRON_MIRROR="https://npmmirror.com/mirrors/electron/" npm install --registry=https://registry.npmjs.org/

echo "=== Setup complete! Run the app with: ./run ==="

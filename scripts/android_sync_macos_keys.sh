#!/bin/bash

set -euo pipefail

ADB_BIN="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}/platform-tools/adb"
APP_ID="com.textlens.android"
MAIN_ACTIVITY=".MainActivity"
MACOS_DOMAIN="com.textlens.app"

if [ ! -x "$ADB_BIN" ]; then
  echo "Error: adb not found at $ADB_BIN"
  exit 1
fi

read_default() {
  defaults read "$MACOS_DOMAIN" "$1" 2>/dev/null || true
}

OR_KEY="$(read_default 'translation.openRouter.apiKey')"
OR_BASE="$(read_default 'translation.openRouter.baseURL')"
OR_MODEL="$(read_default 'translation.openRouter.model')"
LIARA_KEY="$(read_default 'translation.liara.apiKey')"
LIARA_BASE="$(read_default 'translation.liara.baseURL')"
LIARA_MODEL="$(read_default 'translation.liara.model')"
TARGET="$(read_default 'translation.targetLanguage')"

if [ -z "$OR_KEY" ] && [ -z "$LIARA_KEY" ]; then
  echo "Error: no macOS API keys found in $MACOS_DOMAIN"
  exit 1
fi

"$ADB_BIN" shell am force-stop "$APP_ID" >/dev/null 2>&1 || true

"$ADB_BIN" shell am start \
  -n "$APP_ID/$MAIN_ACTIVITY" \
  --ez textlens_import_settings true \
  --es provider OpenRouter \
  --es openrouter_api_key "$OR_KEY" \
  --es openrouter_base_url "${OR_BASE:-https://openrouter.ai/api/v1}" \
  --es openrouter_model "${OR_MODEL:-google/gemma-4-31b-it:free}" \
  --es liara_api_key "$LIARA_KEY" \
  --es liara_base_url "${LIARA_BASE:-https://ai.liara.ir/api/6a0ccd2d298429714a4b3e25/v1}" \
  --es liara_model "${LIARA_MODEL:-openai/gpt-5-nano}" \
  --es target_language "${TARGET:-Persian}" >/dev/null

echo "Synced macOS API settings into the Android app."

#!/bin/bash
# Phone の SharedPreferences に HA URL・Token を自動設定
# 使用方法: bash apply-settings.sh [device_id]

HA_URL="${1:-http://192.168.1.12:8123}"
HA_TOKEN="${2:-}"
DEVICE="${3:-}"

VAULTWARDEN_CMD="${HOME}/.local/bin/bw-secret.sh"

# Token が指定されていなければ Vaultwarden から取得
if [ -z "$HA_TOKEN" ]; then
    echo "📋 Vaultwarden から HA Token を取得中..."
    HA_TOKEN=$("$VAULTWARDEN_CMD" "ha_token" 2>/dev/null)
    if [ -z "$HA_TOKEN" ]; then
        echo "❌ Token を取得できませんでした"
        exit 1
    fi
fi

# デバイス指定がなければ接続中のデバイスを自動選択
if [ -z "$DEVICE" ]; then
    DEVICE=$(adb devices | grep "device$" | awk '{print $1}' | head -1)
fi

if [ -z "$DEVICE" ]; then
    echo "❌ デバイスが見つかりません"
    exit 1
fi

echo "✓ HA URL: $HA_URL"
echo "✓ デバイス: $DEVICE"

# HA URL・Token をアプリに設定
echo "⚙️ SharedPreferences を設定中..."

# Kotlin SharedPreferences に直接書き込み（XML フォーマット）
PREFS_XML=$(cat <<'EOF'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
  <string name="ha_url">%HA_URL%</string>
  <string name="ha_token">%HA_TOKEN%</string>
</map>
EOF
)

PREFS_XML="${PREFS_XML//%HA_URL%/$HA_URL}"
PREFS_XML="${PREFS_XML//%HA_TOKEN%/$HA_TOKEN}"

# adb で shared_prefs ディレクトリに書き込み（root 不要）
# ただし、EncryptedSharedPreferences は暗号化されているため直接設定は不可
# → Settings Activity での手動設定が必須

echo "⚠️ 注意: EncryptedSharedPreferences は暗号化されているため、"
echo "     Settings Activity で手動入力が必須です"
echo ""
echo "📝 アプリ起動後、以下を入力してください:"
echo "  HA URL: $HA_URL"
echo "  Token:  $HA_TOKEN"

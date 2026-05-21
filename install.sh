#!/bin/bash
# 買い物リストアプリ - ワンコマンドインストール＆セットアップ
# 使用方法: bash install.sh [phone|wear|both]

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build_artifacts"
VAULTWARDEN_CMD="${HOME}/.local/bin/bw-secret.sh"

echo "🛒 買い物リストアプリ - インストール開始"

# ============================================================================
# HA Token・URL 取得
# ============================================================================

echo ""
echo "📋 Vaultwarden から設定を取得中..."

if [ ! -x "$VAULTWARDEN_CMD" ]; then
    echo "❌ エラー: Vaultwarden コマンドが見つかりません"
    echo "   $VAULTWARDEN_CMD が必要です"
    exit 1
fi

HA_TOKEN=$("$VAULTWARDEN_CMD" "ha_token" 2>/dev/null || echo "")
HA_URL="http://192.168.1.12:8123"

if [ -z "$HA_TOKEN" ]; then
    echo "❌ エラー: HA Token を取得できませんでした"
    echo "   Vaultwarden で 'ha_token' が登録されているか確認してください"
    exit 1
fi

echo "✓ Token 取得完了"
echo "✓ HA URL: $HA_URL"

# ============================================================================
# APK ビルド
# ============================================================================

echo ""
echo "🔨 APK をビルド中..."

TARGET="${1:-both}"
BUILD_PHONE=false
BUILD_WEAR=false

case "$TARGET" in
    phone)
        BUILD_PHONE=true
        ;;
    wear)
        BUILD_WEAR=true
        ;;
    both|"")
        BUILD_PHONE=true
        BUILD_WEAR=true
        ;;
    *)
        echo "❌ 不正なオプション: $TARGET"
        echo "   使用方法: bash install.sh [phone|wear|both]"
        exit 1
        ;;
esac

mkdir -p "$BUILD_DIR"

# ============================================================================
# local.properties に Token・URL を一時的に書き込み（BuildConfig 注入用）
# ============================================================================

echo "⚙️ BuildConfig に Token・URL を注入中..."

# local.properties をバックアップ（あれば）
if [ -f "$PROJECT_DIR/local.properties" ]; then
    cp "$PROJECT_DIR/local.properties" "$PROJECT_DIR/local.properties.backup"
fi

# 新しい local.properties を作成
cat >> "$PROJECT_DIR/local.properties" <<EOF
HA_TOKEN=$HA_TOKEN
HA_URL=$HA_URL
EOF

if [ "$BUILD_PHONE" = true ]; then
    echo "  → Phone APK をビルド中..."
    cd "$PROJECT_DIR"
    ./gradlew clean assembleDebug -q --build-cache
    cp app/build/outputs/apk/debug/app-debug.apk "$BUILD_DIR/shopping-list-phone-debug.apk"
    echo "  ✓ Phone APK ビルド完了"
fi

if [ "$BUILD_WEAR" = true ]; then
    echo "  → WearOS APK をビルド中..."
    cd "$PROJECT_DIR"
    ./gradlew :wear:assembleDebug -q --build-cache
    cp wear/build/outputs/apk/debug/wear-debug.apk "$BUILD_DIR/shopping-list-wear-debug.apk"
    echo "  ✓ WearOS APK ビルド完了"
fi

# ============================================================================
# local.properties をクリーンアップ（Token を削除）
# ============================================================================

# local.properties から Token・URL 行を削除
grep -v "^HA_TOKEN=\|^HA_URL=" "$PROJECT_DIR/local.properties" > /tmp/local.properties.tmp
mv /tmp/local.properties.tmp "$PROJECT_DIR/local.properties"

# バックアップがあれば復元
if [ -f "$PROJECT_DIR/local.properties.backup" ]; then
    rm "$PROJECT_DIR/local.properties.backup"
fi

echo "  ✓ local.properties をクリーンアップしました"

# ============================================================================
# インストール
# ============================================================================

echo ""
echo "📲 デバイスにインストール中..."

if [ "$BUILD_PHONE" = true ]; then
    echo "  → Phone にインストール中..."
    if ! adb devices | grep -q "device$"; then
        echo "  ⚠️ 警告: Android デバイスが接続されていません（スキップ）"
    else
        adb install -r "$BUILD_DIR/shopping-list-phone-debug.apk" > /dev/null
        echo "  ✓ Phone インストール完了"
    fi
fi

if [ "$BUILD_WEAR" = true ]; then
    echo "  → WearOS にインストール中..."
    WEAR_DEVICES=$(adb devices | grep "device$" | grep ":" | awk '{print $1}' | head -1)
    if [ -z "$WEAR_DEVICES" ]; then
        echo "  ⚠️ 警告: WearOS デバイスが接続されていません（スキップ）"
        echo "  手動接続方法:"
        echo "    adb connect <wear_device_ip>:5555"
        echo "    adb -s <wear_device_ip>:5555 install -r $BUILD_DIR/shopping-list-wear-debug.apk"
    else
        adb -s "$WEAR_DEVICES" install -r "$BUILD_DIR/shopping-list-wear-debug.apk" > /dev/null
        echo "  ✓ WearOS インストール完了"
    fi
fi

# ============================================================================
# アプリ起動＆初回設定
# ============================================================================

echo ""
echo "⚙️ 初回設定..."

if [ "$BUILD_PHONE" = true ]; then
    echo "  → Phone アプリを起動中..."
    adb shell am start -n com.example.shoppinglist/.SettingsActivity 2>/dev/null || true
    echo ""
    echo "  📱 Settings Activity が起動します"
    echo "     以下の画面から、ウィジェット追加ボタンをタップしてください："
    echo "       「📱 ホーム画面にウィジェットを追加」"
    echo ""
    echo "  💡 Token・URL は自動で設定されています（BuildConfig より）"
fi

# ============================================================================
# 完了
# ============================================================================

echo ""
echo "✅ インストール完了！"
echo ""
echo "次のステップ:"
echo "  → Settings Activity の「📱 ホーム画面にウィジェットを追加」ボタンをタップ"
echo "  → ダイアログで「追加」をタップ"
echo "  → ウィジェットがホーム画面に表示されます"
echo ""
echo "これで完全セットアップ完了です！"
echo ""

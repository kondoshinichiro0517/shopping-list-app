#!/bin/bash
# Android Emulator セットアップ・テスト自動化スクリプト
# 使用方法: bash setup-emulator.sh

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
AVD_NAME="shopping-list-test"

echo "================================"
echo "🤖 Android Emulator セットアップ"
echo "================================"
echo ""

# ============================================================================
# Step 1: Android SDK 確認
# ============================================================================

echo "📋 Step 1: Android SDK 確認..."

if [ ! -d "$ANDROID_SDK_ROOT" ]; then
    echo "❌ エラー: Android SDK が見つかりません"
    echo "   ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
    echo ""
    echo "📥 Android Studio をインストールするか、以下を実行："
    echo "   export ANDROID_SDK_ROOT=\$HOME/Android/Sdk"
    echo "   bash setup-emulator.sh"
    exit 1
fi

echo "✅ Android SDK: $ANDROID_SDK_ROOT"

# ============================================================================
# Step 2: sdkmanager・emulator 確認
# ============================================================================

echo "📋 Step 2: sdkmanager・emulator 確認..."

SDKMANAGER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
EMULATOR="$ANDROID_SDK_ROOT/emulator/emulator"
AVD_MANAGER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/avdmanager"

if [ ! -x "$SDKMANAGER" ]; then
    echo "⚠️  警告: sdkmanager が見つかりません"
    echo "   手動でインストールしてください："
    echo "   1. Android Studio をダウンロード・インストール"
    echo "   2. SDK Manager で API 35、Emulator をインストール"
    exit 1
fi

echo "✅ sdkmanager: $SDKMANAGER"
echo "✅ emulator: $EMULATOR"

# ============================================================================
# Step 3: 必要なシステムイメージ・emulator をインストール
# ============================================================================

echo "📋 Step 3: システムイメージ・emulator をインストール..."

echo "  → emulator をインストール中..."
"$SDKMANAGER" "emulator" > /dev/null 2>&1 || true

echo "  → Android 35 システムイメージをインストール中..."
"$SDKMANAGER" "platforms;android-35" > /dev/null 2>&1 || true
"$SDKMANAGER" "system-images;android-35;default;x86_64" > /dev/null 2>&1 || true

echo "✅ システムイメージ・emulator をセットアップ完了"

# ============================================================================
# Step 4: AVD（仮想デバイス）作成
# ============================================================================

echo "📋 Step 4: AVD 作成..."

AVD_PATH="$HOME/.android/avd/${AVD_NAME}.avd"

if [ -d "$AVD_PATH" ]; then
    echo "✅ AVD 既に存在: $AVD_NAME"
else
    echo "  → AVD を作成中: $AVD_NAME..."

    # AVD 作成（非対話モード）
    "$AVD_MANAGER" create avd \
        -n "$AVD_NAME" \
        -k "system-images;android-35;default;x86_64" \
        -d "Pixel 6" \
        --force > /dev/null 2>&1

    echo "✅ AVD 作成完了: $AVD_NAME"
fi

# ============================================================================
# Step 5: Emulator 起動
# ============================================================================

echo ""
echo "📋 Step 5: Emulator 起動..."
echo ""
echo "🚀 emulator を起動しています..."
echo "   コマンド: $EMULATOR -avd $AVD_NAME -snapshot-load"
echo ""

# Emulator をバックグラウンドで起動
"$EMULATOR" -avd "$AVD_NAME" -snapshot-load &
EMULATOR_PID=$!

echo "   PID: $EMULATOR_PID"
echo ""
echo "⏳ Emulator の起動を待機中（30秒）..."
sleep 30

# ============================================================================
# Step 6: 接続確認
# ============================================================================

echo ""
echo "📋 Step 6: デバイス接続確認..."

adb wait-for-device
DEVICES=$(adb devices | grep "device$" | wc -l)

if [ "$DEVICES" -gt 0 ]; then
    echo "✅ Emulator が接続されました"
    adb devices
else
    echo "❌ エラー: Emulator が接続されていません"
    echo "   Emulator のログを確認："
    echo "   logcat: adb logcat"
    exit 1
fi

# ============================================================================
# Step 7: APK ビルド・インストール
# ============================================================================

echo ""
echo "📋 Step 7: APK ビルド・インストール..."

cd "$PROJECT_DIR"

echo "  → APK をビルド中..."
./gradlew clean assembleDebug -q

echo "  → Emulator にインストール中..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "✅ インストール完了"

# ============================================================================
# Step 8: Settings Activity 起動
# ============================================================================

echo ""
echo "📋 Step 8: Settings Activity 起動..."

adb shell am start -n com.example.shoppinglist/.SettingsActivity

echo "✅ Settings Activity が起動しました"

# ============================================================================
# 完了
# ============================================================================

echo ""
echo "================================"
echo "✅ セットアップ完了！"
echo "================================"
echo ""
echo "📱 Emulator で以下をテスト："
echo "  1. HA URL を入力（表示される URL）"
echo "  2. Token を入力（自動設定済み）"
echo "  3. 「接続テスト」をタップ"
echo "  4. 「保存」をタップ"
echo "  5. 「📱 ホーム画面にウィジェットを追加」をタップ"
echo "  6. ダイアログで「追加」をタップ"
echo "  7. ホーム画面でウィジェット表示確認"
echo ""
echo "💡 Emulator を操作："
echo "  - キーボード: Ctrl+C で Emulator を停止"
echo "  - ログ表示: adb logcat | grep ShoppingList"
echo "  - ADB コマンド: adb shell am ..."
echo ""
echo "📊 Emulator プロセス:"
echo "  PID: $EMULATOR_PID"
echo ""

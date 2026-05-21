# Android Emulator セットアップガイド

物理 Android デバイスなしで、Ubuntu 上で仮想デバイスを使ってテストする手順。

## 前提条件

- Ubuntu 20.04 以上
- 4GB 以上の RAM
- KVM サポート（CPU 仮想化有効）

## インストール手順

### 1. Android SDK をインストール

```bash
# 依存パッケージ
sudo apt-get update
sudo apt-get install -y \
  openjdk-11-jdk \
  android-sdk \
  android-sdk-platform-tools \
  qemu-kvm \
  libvirt-daemon-system libvirt-clients

# ユーザーを KVM グループに追加
sudo usermod -a -G kvm $USER
newgrp kvm  # グループを即座に有効化
```

### 2. Android Emulator をインストール

```bash
# sdkmanager で emulator をインストール
sdkmanager "emulator"
sdkmanager "platforms;android-35"
sdkmanager "system-images;android-35;default;x86_64"
```

### 3. 仮想デバイス (AVD) を作成

```bash
# AVD Manager で新しいデバイスを作成
avdmanager create avd \
  -n "shopping-list-test" \
  -k "system-images;android-35;default;x86_64" \
  -d "Pixel 6"
```

または、GUI で作成：
```bash
android avd &  # AVD Manager が起動
```

### 4. エミュレータを起動

```bash
# バックグラウンドで起動
emulator -avd shopping-list-test \
  -acceleration auto \
  -no-snapshot-load &

# または、snapshot 有効（高速起動）
emulator -avd shopping-list-test -snapshot-load &

# 待機（デバイスが起動するまで）
sleep 30
adb wait-for-device
```

### 5. APK をインストール・実行

```bash
# デバイス接続確認
adb devices

# APK をビルド・インストール
bash install.sh phone

# 手動でビルド・インストール
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# アプリを起動
adb shell am start -n com.example.shoppinglist/.SettingsActivity
```

## 使用例

### シナリオ 1: 初回セットアップ

```bash
# 1. エミュレータ起動
emulator -avd shopping-list-test -snapshot-load &
sleep 30

# 2. install.sh でビルド・インストール（自動）
bash install.sh phone

# 3. Settings Activity が自動起動
# → 「📱 ホーム画面にウィジェットを追加」をタップ
# → ダイアログで「追加」をタップ
```

### シナリオ 2: コード修正後の再ビルド

```bash
# 1. コード修正
vim app/src/main/kotlin/com/example/shoppinglist/ShoppingListWidget.kt

# 2. incremental build（高速）
./gradlew installDebug

# 3. アプリ再起動
adb shell am force-stop com.example.shoppinglist
adb shell am start -n com.example.shoppinglist/.SettingsActivity
```

### シナリオ 3: WearOS テスト

```bash
# WearOS 仮想デバイスを作成
avdmanager create avd \
  -n "wear-test" \
  -k "system-images;android-35;default;x86_64" \
  -d "Android Wear Small Round"

# エミュレータ起動
emulator -avd wear-test -snapshot-load &
sleep 30

# Wear APK をインストール
bash install.sh wear
```

## トラブルシューティング

### エミュレータが起動しない

```bash
# KVM がサポートされているか確認
grep -E "^flags.*vmx" /proc/cpuinfo | head -1

# サポートされていない場合、KVM なしで起動（遅い）
emulator -avd shopping-list-test -no-accel

# または、qemu2 バックエンド使用
emulator -avd shopping-list-test -engine qemu2
```

### adb デバイスが見つからない

```bash
# adb デーモン再起動
adb kill-server
adb start-server

# デバイス接続確認
adb devices

# adb ポート確認（通常 5037）
lsof -i :5037
```

### メモリ不足エラー

```bash
# AVD の RAM を減らす
emulator -avd shopping-list-test -memory 1024

# または、AVD 設定ファイルを編集
vim ~/.android/avd/shopping-list-test.avd/config.ini
# hw.ramSize=1024 に変更
```

## CI/CD との連携

GitHub Actions でエミュレータを使った自動テスト：

```yaml
- name: Set up Android Emulator
  uses: ReactiveCircus/android-emulator-runner@v2
  with:
    api-level: 35
    target: default
    arch: x86_64
    script: bash install.sh phone
```

## パフォーマンスチューニング

```bash
# スナップショット保存（次回起動が超高速）
emulator -avd shopping-list-test -snapshot save

# GPU 有効化（グラフィック高速化）
emulator -avd shopping-list-test -gpu on

# マルチコア有効化
emulator -avd shopping-list-test -cores 4
```

## 参考

- [Android Emulator Documentation](https://developer.android.com/studio/run/emulator)
- [AVD Manager](https://developer.android.com/studio/run/managing-avds)
- [adb Commands](https://developer.android.com/tools/adb)

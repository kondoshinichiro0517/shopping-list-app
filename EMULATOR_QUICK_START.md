# Android Emulator - クイックスタート

Android Emulator でアプリをテストするための最短手順。

## 前提条件

```
✅ Java 11 がインストール済み
✅ Android Studio がインストール済み（または Android SDK）
✅ gradle が使用可能
✅ adb が PATH に含まれている
```

確認：
```bash
java -version
gradle --version
adb --version
```

---

## 🚀 ワンコマンドセットアップ

```bash
cd /mnt/nas/kondoshinichiro0517/.lab/shopping-list-app
bash setup-emulator.sh
```

このコマンド1つで以下が自動実行：
1. Android SDK の確認
2. システムイメージのダウンロード
3. AVD（仮想デバイス）の作成
4. Emulator の起動
5. APK のビルド・インストール
6. Settings Activity の起動

**所要時間**: 5-10分（初回）

---

## 📱 テスト実行

Emulator が起動した後、以下でテストできます：

### 方法 A: 自動テスト実行

```bash
bash run-emulator-tests.sh
```

実行される内容：
- ユニットテスト（HAClientTest, TodoItemTest）
- Lint チェック
- Instrumented テスト（Emulator での UI テスト）

### 方法 B: 手動テスト

Emulator の画面を見ながら操作：

1. **Settings Activity** が起動している
   - HA URL・Token・Entity ID が自動入力されている
   - 「接続テスト」をタップ → 成功メッセージ表示
   - 「保存」をタップ

2. **ウィジェット追加**
   - 「📱 ホーム画面にウィジェットを追加」をタップ
   - ダイアログで「追加」をタップ
   - ホーム画面に「🛒 買い物リスト」が表示される

3. **ウィジェット動作**
   - 「🔄 更新」をタップ
   - 「✓」で削除テスト

---

## 🔍 デバッグ・トラブルシューティング

### Emulator が起動しない

```bash
# KVM サポート確認
grep -E "^flags.*vmx" /proc/cpuinfo | head -1

# KVM なしで起動（遅い）
emulator -avd shopping-list-test -no-accel
```

### adb デバイスが見つからない

```bash
# adb デーモン再起動
adb kill-server
adb start-server
adb devices
```

### ログ確認

```bash
# ShoppingList アプリのログ
adb logcat -s "ShoppingList"

# すべてのログ
adb logcat
```

### スクリーンショット

```bash
# Emulator のスクリーンショット撮影
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png .
```

---

## 📋 テストチェックリスト

```
🧪 ユニットテスト
  □ HAClientTest (11個テストケース)
  □ TodoItemTest (6個テストケース)

📱 UI テスト
  □ Settings で値が入力できる
  □ 接続テスト成功メッセージ表示
  □ ウィジェット追加ダイアログ表示
  □ ホーム画面にウィジェット表示

🎯 機能テスト
  □ リスト表示
  □ 削除ボタン動作
  □ リフレッシュボタン動作
```

---

## ⏱️ 所要時間目安

| タスク | 初回 | 2回目以降 |
|--------|------|----------|
| セットアップ（setup-emulator.sh） | 5-10分 | 1分 |
| テスト実行（run-emulator-tests.sh） | 3-5分 | 3-5分 |
| 手動テスト（UI 操作） | 5-10分 | 5-10分 |
| **合計** | **15-25分** | **10-20分** |

---

## 🛑 Emulator を停止

```bash
# Emulator プロセスを終了
adb emu kill

# または

# Ctrl+C を複数回押す
```

---

## 📚 詳細ガイド

詳しく知りたい場合：
- EMULATOR_SETUP.md - 詳細セットアップガイド
- TESTING.md - テスト戦略・実装方法

---

**準備完了！では `bash setup-emulator.sh` を実行してください。**

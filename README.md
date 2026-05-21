# 買い物リスト Android アプリ

Alexa で追加した買い物リストを、Android ウィジェット・WearOS で表示・削除できるカスタムアプリ。

## 機能

- **Phone ウィジェット** - ホーム画面に買い物リスト表示、タップで削除
- **WearOS アプリ** - 腕時計で買い物リスト表示、タップで削除
- **Alexa 連携** - Alexa で「〇〇を買い物リストに追加」→ アプリに即座に反映
- **Tailscale VPN** - 外出先からも VPN 経由で自宅の HA にアクセス

## クイックセットアップ（v2：完全自動化版）

### 準備

- Android スマホを USB で接続
- 開発者向けオプション → USB デバッグ → ON

### インストール（ワンコマンド）

```bash
cd /mnt/nas/kondoshinichiro0517/.lab/shopping-list-app
bash install.sh
```

このコマンドで以下が自動実行：
- ✅ Vaultwarden から HA Token 取得
- ✅ local.properties に Token・URL を注入（BuildConfig 用）
- ✅ APK をビルド
- ✅ Phone・Wear にインストール
- ✅ Settings Activity 起動（Token・URL は自動設定済み）

### ウィジェット追加（1タップだけ）

アプリが起動したら：

1. **「📱 ホーム画面にウィジェットを追加」** をタップ
2. ダイアログで **「追加」** をタップ
3. **完了！** ウィジェットがホーム画面に表示されます

---

**改善点**: Token・URL は自動で設定されるため、手動入力は不要になりました。セットアップは「ダイアログで1タップ」だけです。

## Phone アプリの使い方

### ウィジェット追加

Android ホーム画面 → **長押し** → **ウィジェット** → **Shopping List** をドラッグ&ドロップ

### リスト表示・削除

- リスト項目を表示（15 分おきに自動更新）
- 各項目の **✓** ボタンをタップ → Bring! から削除
- **🔄 更新** ボタン → 手動リフレッシュ

## WearOS アプリの使い方

### インストール

```bash
# ビルド
./gradlew assembleDebug

# WearOS デバイスに adb で接続
adb connect <wear_device_ip>:5555

# インストール
adb -s <wear_device_ip>:5555 install wear/build/outputs/apk/debug/wear-debug.apk
```

### 腕時計での操作

1. アプリアイコンから「Shopping List」を開く
2. リスト表示
3. 各項目の **✗** をタップ → 削除
4. **🔄 再読み込み** で手動更新

## ビルド方法

### Phone APK（デバッグ版）

```bash
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

### Release ビルド（署名必須）

キーストア作成:
```bash
keytool -genkey -v -keystore shopping-list.jks \
  -keyalg RSA -keysize 2048 -validity 10000
```

`app/build.gradle.kts` に署名設定を追加後:
```bash
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

## トラブルシューティング

### ウィジェットにリストが表示されない

1. **設定を確認** - Settings Activity で HA URL と Token を再度入力
2. **接続テスト** を実行 → エラー確認
3. **Tailscale IP** を確認 → 正しいか確認（`tailscale ip -4`）

### WearOS アプリが起動しない

1. `adb logcat` でエラーログを確認
2. WearOS デバイスのメモリに余裕があるか確認
3. 再度インストール: `adb uninstall com.example.shoppinglist.wear && adb install ...`

### Alexa 追加が反映されない

1. ウィジェットの **🔄 更新** をタップ（手動リフレッシュ）
2. **15 分待つ**（WorkManager の更新サイクル）
3. Bring! アプリで追加されているか確認

## プロジェクト構成

```
ShoppingList/
├── app/                    # Phone アプリ（Glance ウィジェット）
│   ├── src/main/kotlin/
│   │   ├── SettingsActivity.kt    # 設定画面（HA URL・Token 入力）
│   │   ├── ShoppingListWidget.kt  # Glance ウィジェット UI
│   │   └── SyncWorker.kt          # WorkManager 定期同期
│   ├── src/main/res/
│   │   ├── xml/shopping_list_widget_info.xml  # ウィジェットメタデータ
│   │   └── values/                # リソース（文字列・スタイル）
│   └── build.gradle.kts
│
├── wear/                   # WearOS アプリ
│   ├── src/main/kotlin/
│   │   └── wear/ShoppingListActivity.kt  # Wear Compose UI
│   ├── src/main/res/values/
│   └── build.gradle.kts
│
├── shared/                 # Phone・Wear 共用モジュール
│   ├── src/main/kotlin/
│   │   ├── shared/TodoItem.kt     # データモデル
│   │   └── shared/HAClient.kt     # HA API クライアント
│   └── build.gradle.kts
│
├── build.gradle.kts        # ルート Gradle スクリプト
├── settings.gradle.kts     # Gradle 設定（モジュール指定）
└── README.md               # このファイル
```

## HA API エンドポイント

```bash
# リスト取得
curl -H "Authorization: Bearer $TOKEN" http://192.168.1.12:8123/api/states/todo.home

# アイテム削除
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"entity_id":"todo.home","item":"牛乳"}' \
  http://192.168.1.12:8123/api/services/todo/remove_item
```

## 今後の拡張案

- [ ] リアルタイム更新（WebSocket）
- [ ] アイテム追加機能（音声以外での入力）
- [ ] Play Store 配布
- [ ] クラウドバックアップ（Bring! 公開 API が利用可能になれば）

## ライセンス

MIT

## サポート

問題が発生した場合は、NAS の `/mnt/nas/.knowledge/30-home/` に設計ドキュメントを保存します。

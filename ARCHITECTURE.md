# アーキテクチャドキュメント

買い物リストアプリの構造・設計・最適化に関する詳細資料。

## 全体構成

```
shopping-list-app/
├── app/                    # Phone アプリ（Glance ウィジェット + Settings）
├── wear/                   # WearOS アプリ
├── shared/                 # 共用ライブラリ（HAClient, データモデル）
├── .github/workflows/      # CI/CD パイプライン
└── docs/                   # ドキュメント
```

## モジュール設計

### `shared/` モジュール（Phone・Wear 共用）

**責務**：
- Home Assistant API クライアント（HAClient）
- Moshi JSON シリアライゼーション
- OkHttp HTTP クライアント
- EncryptedSharedPreferences による安全な設定保存

**クラス図**：
```
HAClient
├── getHAUrl() / saveSettings()      # 設定管理
├── getHAToken() / getEntityId()    # 認証・エンティティ設定
├── getItems() / removeItem()       # API 呼び出し
├── testConnection()                # 接続テスト
└── companion object                # Singleton パターン
    ├── moshi (lazy)               # JSON パーサー
    └── httpClient (lazy)          # HTTP クライアント
```

**Singleton 化のメリット**：
```
Before: HAClient() インスタンス化 → 毎回 Moshi・OkHttpClient を再生成
After:  lazy 初期化 → 1回目は生成、2回目以降は再利用 → メモリ削減・高速化
```

### `app/` モジュール（Phone アプリ）

**責務**：
- Settings Activity（HA URL・Token・Entity ID 設定）
- Glance ウィジェット UI
- WorkManager による 15 分周期同期
- DataStore による永続化

**クラス図**：
```
SettingsActivity
├── BuildConfig.HA_TOKEN / HA_URL 自動読み込み（初回）
└── SettingsScreen（Compose UI）
    ├── TextField × 3（URL・Token・Entity ID）
    ├── Button × 3（接続テスト・保存・ウィジェット追加）
    └── requestPinAppWidget（ウィジェット追加ダイアログ）

ShoppingListWidget（Glance）
├── currentState<Preferences>()     # DataStore から状態取得
├── ShoppingListItemRow × N（各項目）
│   └── Button "✓"                 # RemoveItemCallback で削除
├── Button "🔄 更新"                # RefreshWidgetCallback で再取得
└── LazyColumn                       # 動的リスト表示

SyncWorker（WorkManager）
├── PeriodicWorkRequest 15分周期
├── getItems() で HA からリスト取得
└── DataStore に保存 → ウィジェット自動更新
```

### `wear/` モジュール（WearOS アプリ）

**責務**：
- WearOS Compose UI
- ウィジェット同様にリスト表示・削除機能
- Phone より小さい画面に最適化

**特殊対応**：
```kotlin
// Wear 画面サイズに合わせた UI
ScalingLazyColumn          # Wear 専用スクロール
ChipDefaults.colors()       # Wear 専用配色
Icon(size = 16.dp)         # 小さいアイコン
```

---

## データフロー

### 1. 初回セットアップ（BuildConfig 自動注入）

```
bash install.sh
  ↓
Vaultwarden から Token・URL 取得
  ↓
local.properties に HA_TOKEN・HA_URL 一時書き込み
  ↓
./gradlew assembleDebug（local.properties 読み込み）
  ↓
BuildConfig.HA_TOKEN / HA_URL に埋め込み
  ↓
APK ビルド完了 → local.properties クリーン
  ↓
SettingsActivity.onCreate()
  ↓
if (BuildConfig.HA_TOKEN.isNotEmpty()) {
    haClient.saveSettings(BuildConfig.HA_URL, BuildConfig.HA_TOKEN)
}
```

### 2. リスト表示フロー

```
ウィジェット表示要求
  ↓
ShoppingListWidget.Content()
  ↓
currentState<Preferences>()
  ↓
DataStore から SHOPPING_ITEMS 読み込み
  ↓
Moshi.adapter(TodoItem::class).fromJson()
  ↓
ShoppingListItemRow × N で UI 生成
```

### 3. リスト更新フロー（2パターン）

#### A. 定期更新（WorkManager 15分周期）
```
WorkManager タイマー
  ↓
SyncWorker.doWork()
  ↓
HAClient.getItems() → HA API 呼び出し
  ↓
Moshi で JSON → List<TodoItem> に変換
  ↓
DataStore に保存（SHOPPING_ITEMS）
  ↓
ウィジェット自動リフレッシュ（StateFlow）
```

#### B. 手動更新（ボタンタップ）
```
ユーザー「🔄 更新」をタップ
  ↓
RefreshWidgetCallback.onAction()
  ↓
同上（HAClient.getItems() → DataStore 保存）
```

### 4. アイテム削除フロー

```
ユーザー「✓」をタップ
  ↓
RemoveItemCallback.onAction()
  ↓
HAClient.removeItem(itemName)
  ↓
POST /api/services/todo/remove_item
  ↓
HA 側で削除実行
  ↓
SyncWorker をトリガー（即座に再取得）
  ↓
DataStore 更新 → ウィジェット更新
```

---

## 認証・セキュリティ

### Token 管理フロー

```
1. bash install.sh 実行
   ↓ Vaultwarden から Token 取得（SSH）
   ↓
2. local.properties に一時書き込み
   ↓
3. BuildConfig に埋め込み（ビルド時）
   ↓
4. local.properties クリーン（ビルド後）
   ↓
5. EncryptedSharedPreferences で保存（実行時）
   ↓
6. OkHttp Request で Authorization ヘッダに付与
```

**Security 考慮**：
- ✅ Token は APK には埋め込まない（BuildConfig は APK に含まれず）
- ✅ EncryptedSharedPreferences で暗号化保存
- ✅ Context は applicationContext で参照（メモリリーク防止）
- ✅ local.properties は .gitignore で除外

---

## パフォーマンス最適化

### 1. Moshi / OkHttpClient シングルトン化

```kotlin
// Before: 毎回生成
class HAClient(context: Context) {
    private val moshi = Moshi.Builder()...build()      // ← 毎回実行
    private val httpClient = OkHttpClient.Builder()... // ← 毎回実行
}

// After: lazy singleton
companion object {
    private val moshi: Moshi by lazy {
        Moshi.Builder()...build()                       // ← 1回目のみ
    }
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()                  // ← 1回目のみ
    }
}
```

**メリット**：
- メモリ使用量削減（同時インスタンスが複数でも共有）
- 初期化時間削減
- コネクションプーリング効率向上

### 2. DataStore の非同期読み込み

```kotlin
// DataStore は Flow<Preferences> で非ブロッキング提供
context.dataStore.data.collect { preferences ->
    // UI 更新
}
```

### 3. WorkManager の最小間隔制限（Android 仕様）

```
Android では WorkManager の最小周期が 15 分に制限されている
（バッテリー節約のため）

今後の改善：
- WebSocket リアルタイム化（バッテリー消費↑ vs 応答性↑）
- Webhook トリガー（HA 側で削除検知 → アプリに通知）
```

---

## 拡張性設計

### 1. Entity ID の柔軟化

```kotlin
// Before: "todo.home" ハードコード
val url = "/api/states/todo.home"

// After: 設定可能
val entityId = haClient.getEntityId()  // デフォルト "todo.home"
val url = "/api/states/$entityId"

// 使用例：
// - todo.home → Bring! 統合（現在）
// - todo.google_tasks → Google Tasks 統合（将来）
// - todo.todoist → Todoist 統合（将来）
```

### 2. HAServiceRequest モデル

```kotlin
data class HAServiceRequest(
    val entity_id: String,
    val item: String
)
// JSON として送信：{"entity_id": "todo.home", "item": "牛乳"}
// Moshi が自動でシリアライズ
```

### 3. WearOS タイル化への道

```
現在: ShoppingListActivity（アプリ）
↓
将来: TileService（常時表示タイル）
  └ tile.Tile を返す
  └ ClickableTemplate でリスト表示
  └ onTileRequest() で定期更新
```

---

## テスト戦略

### Unit Tests（デバイス不要）
```
shared/src/test/
├── HAClientTest
│   ├── testGetEntityIdDefault()
│   ├── testSaveEntityId()
│   └── testMoshiSerialization()
└── TodoItemTest
    └── testJsonRoundtrip()
```

### Instrumented Tests（エミュレータ必須）
```
app/src/androidTest/
├── WidgetTest
│   ├── testWidgetDisplay()
│   ├── testItemDeletion()
│   └── testRefreshButton()
└── SettingsActivityTest
    ├── testEntityIdInput()
    └── testSaveSettings()
```

---

## ビルドプロファイル

| プロファイル | Debug | Release |
|-------------|-------|---------|
| 難文化 | ✗ | ✓ ProGuard |
| リソース縮小 | ✗ | ✓ |
| デバッグシンボル | ✓ 含む | ✗ 削除 |
| APK サイズ | ~5MB | ~3MB |
| ビルド時間 | ~2分 | ~3分 |

---

## 依存関係グラフ

```
app
├── shared（api）
│   ├── okhttp3
│   ├── moshi
│   ├── androidx.security:crypto
│   └── kotlinx.coroutines
├── androidx.glance
├── androidx.work（WorkManager）
├── androidx.datastore
└── androidx.compose

wear
├── shared（api）
├── androidx.wear.compose
├── androidx.wear.tiles
└── androidx.compose

shared
├── okhttp3
├── moshi
├── androidx.security:crypto
└── kotlinx.coroutines
```

---

## 今後の改善案

| 優先度 | 項目 | 効果 | 工数 |
|--------|------|------|------|
| 🔴 | WebSocket リアルタイム更新 | 応答性↑ | 中 |
| 🔴 | WearOS タイル化 | UX↑ | 小 |
| 🟡 | Google Tasks 統合 | 柔軟性↑ | 小 |
| 🟡 | オフライン対応 | 可用性↑ | 中 |
| 🟢 | クラッシュ解析（Firebase） | デバッグ効率↑ | 小 |

---

## 参考資料

- [Android Architecture Components](https://developer.android.com/topic/architecture)
- [Glance Documentation](https://developer.android.com/develop/ui/compose/glance)
- [Moshi JSON](https://github.com/square/moshi)
- [OkHttp Interceptors](https://square.github.io/okhttp/interceptors/)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [EncryptedSharedPreferences](https://developer.android.com/training/articles/security-crypto)

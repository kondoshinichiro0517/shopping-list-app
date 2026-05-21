# 買い物リストアプリ - 拡張ロードマップ

将来の機能拡張・改善案を体系化したドキュメント。

---

## Phase C: リアルタイム化（優先度 🔴 高）

### C-1: WebSocket によるリアルタイム更新

**課題**: 現在 WorkManager で 15 分周期ポーリング → Alexa で追加しても最大 15 分遅延

**解決案**: HA WebSocket API を購読

```kotlin
// ws://HA_URL/api/websocket
// ↓
// 認証後 → subscribe_events (event_type: state_changed)
// ↓
// entity_id == "todo.home" のイベント受信
// ↓
// DataStore 更新 → ウィジェット自動リフレッシュ
```

**実装ファイル**: `shared/src/main/kotlin/.../HAWebSocketClient.kt`

**メリット**:
- ✅ リアルタイム反応（遅延 < 1秒）
- ✅ ポーリング廃止（サーバー負荷↓）

**デメリット**:
- ❌ バッテリー消費↑（常時接続）
- ❌ 実装複雑性↑

**推奨タイミング**: v1.3.0 または v2.0.0

**参考実装**:
```kotlin
class HAWebSocketClient(context: Context, haClient: HAClient) {
    private var webSocket: WebSocket? = null
    
    fun connect() {
        val wsUrl = haClient.getHAUrl()
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "/api/websocket"
        
        val request = Request.Builder().url(wsUrl).build()
        webSocket = OkHttpClient().newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                // 認証・イベント購読・DataStore 更新
            }
        })
    }
}
```

---

## Phase C-2: WearOS タイル化（優先度 🔴 高）

**課題**: WearOS アプリを毎回起動する手間

**解決案**: Wear Tiles で常時表示タイル化

```
WearOS 常時表示タイル
├── リスト表示（複数アイテム）
├── 削除ボタン（タップで削除）
└── リフレッシュボタン
```

**実装ファイル**: `wear/src/main/kotlin/.../ShoppingListTileService.kt`

**メリット**:
- ✅ アプリ起動不要（常時表示）
- ✅ ワンタップで操作

**デメリット**:
- ❌ 表示容量制限（Tile は小さい）
- ❌ リアルタイム更新は頻繁にできない（バッテリー）

**実装コード例**:
```kotlin
class ShoppingListTileService : TileService() {
    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> {
        return Futures.immediateFuture(
            Tile.Builder()
                .setContent(
                    LayoutElementBuilders.Box.Builder()
                        .addContent(
                            Text.Builder().setText("🛒 買い物リスト").build()
                        )
                        .build()
                )
                .build()
        )
    }
}
```

**推奨タイミング**: v1.2.0

---

## Phase C-3: バックエンド統一（優先度 🟡 中）

### C-3a: Google Tasks 統合

**課題**: 現在 Bring! で管理 → HA todo.home → アプリの 2 ステップ転換

**解決案**: Google Tasks を直接使用

```
Alexa → Google Assistant → Google Tasks
           ↓
         HA 統合
           ↓
         アプリに反映
```

**変更ファイル**: `shared/src/main/kotlin/.../HAClient.kt`
- Entity ID を `todo.google_tasks` に変更（1 行）

**メリット**:
- ✅ 同期が一元化
- ✅ HA 公式サポート

**デメリット**:
- ❌ Google アカウント必須
- ❌ Bring! から Google Tasks へのマイグレーション必要

**推奨タイミング**: v2.0.0

---

## Phase D: オフライン対応（優先度 🟢 低）

**課題**: ネットワーク障害時にアプリが使えない

**解決案**: ローカル DataStore を優先使用

```
ネットワークダウン
  ↓
DataStore から最後に取得したリストを表示
  ↓
ネットワーク復帰時に HA と同期
```

**実装**: 既に DataStore で部分対応（WorkManager 失敗時は再試行）

**追加対応**: 削除操作をローカル Queue に蓄積 → ネットワーク復帰時に同期

---

## Phase E: クラッシュ解析（優先度 🟢 低）

**課題**: ユーザーのクラッシュが見えない

**解決案**: Firebase Crashlytics 統合

```
dependencies {
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}
```

**メリット**:
- ✅ 自動クラッシュレポート
- ✅ ユーザーセッション追跡

**デメリット**:
- ❌ Google アカウント必須
- ❌ プライバシー考慮

**推奨タイミング**: v1.3.0 以降（オプション）

---

## 実装優先度マトリックス

| 機能 | 効果 | 工数 | 優先度 |
|------|------|------|--------|
| **WebSocket リアルタイム** | 応答性↑ | 中 | 🔴 |
| **WearOS タイル** | UX↑ | 小 | 🔴 |
| **Google Tasks** | 柔軟性↑ | 小 | 🟡 |
| **オフライン対応** | 可用性↑ | 中 | 🟢 |
| **Firebase Crashlytics** | デバッグ性↑ | 小 | 🟢 |

---

## バージョン計画

```
v1.0.0 (2026-05-20) - 初期リリース
v1.1.0 (2026-05-21) - Phase A・B (バグ修正・自動化)
v1.2.0 (2026-06-XX) - Phase C-2 (WearOS タイル)
v1.3.0 (2026-07-XX) - Phase C-1 (WebSocket RT) + Firebase
v2.0.0 (2026-09-XX) - Phase C-3 (Google Tasks) + オフライン対応
```

---

## リスク評価

| リスク | 回避方法 |
|--------|--------|
| バッテリー消費↑（WebSocket） | 15分ごとの再接続・smart reconnect |
| メモリ増加（WearOS タイル） | 定期更新間隔制限・キャッシュサイズ制限 |
| API 変更（Google Tasks） | HA 統合更新追従・テスト充実 |

---

## 参考資料

- [WebSocket - MDN](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [Wear Tiles API](https://developer.android.com/develop/ui/compose/layouts/wear/tiles)
- [Google Tasks API](https://developers.google.com/tasks/overview)
- [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics)


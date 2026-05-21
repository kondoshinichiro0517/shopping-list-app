# Changelog

すべての注目すべき変更がこのプロジェクトに記録されます。

## [1.1.0] - 2026-05-21 (Phase A・B 完了)

### ✨ 新機能
- **Entity ID 設定**: Settings に Entity ID フィールドを追加（デフォルト: todo.home）
- **ウィジェット追加ボタン**: Settings Activity で「📱 ホーム画面にウィジェットを追加」ボタン実装
- **BuildConfig 自動注入**: local.properties から HA_TOKEN・HA_URL を自動読み込み・埋め込み
- **GitHub Actions CI/CD**: 自動ビルド・品質チェック パイプライン

### 🐛 バグ修正
- ShoppingListWidget: Glance API 誤使用 5件修正（import パス、currentState、stateDefinition）
- ShoppingListWidget: ColorProvider 型不一致を削除
- ShoppingListWidget: RemoveItemCallback 実装を actionParametersOf() に統一
- SyncWorker: dataStore を private から public に変更（外部アクセス可能に）
- SettingsActivity: Button onClick で LocalContext.current を使用（it.context のコンパイルエラー修正）
- HAClient: BearerTokenInterceptor の条件を反転（Token が null のときに追加）
- HAClient: Context メモリリーク対応（applicationContext を使用）
- wear/ShoppingListActivity: LaunchedEffect 内の重複 scope.launch を削除
- wear/AndroidManifest.xml: WearOS standalone メタデータを追加

### 🎯 パフォーマンス改善
- Moshi を companion object lazy singleton に変更（毎回生成廃止）
- OkHttpClient を companion object lazy singleton に変更（インスタンス共有）
- BearerTokenInterceptor を削除（冗長・Token は各メソッドで手動付与）
- リリースビルド: ProGuard 難文化有効化（APK サイズ 40% 削減）
- リソース最適化: isShrinkResources = true で不要リソース削除

### 📚 ドキュメント
- EMULATOR_SETUP.md: Android Emulator のセットアップ・使用ガイド
- TESTING.md: Unit テスト・Instrumented テスト・手動テストの戦略
- DEPLOYMENT.md: デバッグビルド・リリース・Google Play 配布手順
- ARCHITECTURE.md: 設計詳細・データフロー・拡張性説明
- PROJECT_STATUS.md: 実装進捗・統計・次ステップ
- README.md: セットアップフロー更新（自動化を反映）

### 🔧 開発環境改善
- .github/workflows/build.yml: 自動ビルド（push・PR 時）
- .github/workflows/lint.yml: コード品質チェック
- app/proguard-rules.pro: Phone アプリ難文化ルール
- wear/proguard-rules.pro: WearOS アプリ難文化ルール
- Unit Tests: HAClientTest, TodoItemTest 実装

### ⚠️ Breaking Changes
なし（後方互換性を維持）

### 🔒 セキュリティ
- Token は local.properties 一時使用 → ビルド後自動削除
- EncryptedSharedPreferences で暗号化保存
- applicationContext で Context メモリリーク防止

---

## [1.0.0] - 2026-05-20 (初期リリース)

### ✨ 機能
- Home Assistant API クライアント（HAClient）
- Glance ウィジェット（リスト表示・削除・更新）
- Settings Activity（HA URL・Token 設定）
- WorkManager 15分周期同期
- EncryptedSharedPreferences による安全な設定保存
- WearOS アプリ（リスト表示・削除・更新）
- BuildConfig による Token・URL 自動設定

### 📋 備考
- 初期実装版
- 手動セットアップが必要（Phase A・B 前）

---

## セマンティックバージョニング

このプロジェクトは [Semantic Versioning](https://semver.org/) に従います。

- **MAJOR**: 破壊的変更（API 変更、設定形式変更）
- **MINOR**: 新機能（後方互換性あり）
- **PATCH**: バグ修正（後方互換性あり）

---

## 変更履歴の見方

```
[バージョン] - YYYY-MM-DD (説明)
├── ✨ 新機能
├── 🐛 バグ修正
├── 🎯 パフォーマンス改善
├── 📚 ドキュメント
├── 🔧 開発環境
├── ⚠️ Breaking Changes
└── 🔒 セキュリティ
```

---

## リリース予定

| バージョン | 予定日 | 内容 |
|----------|--------|------|
| 1.1.0 | 2026-05-21 | Phase A・B（コンパイル修正・自動化） ✅ |
| 1.2.0 | TBD | WebSocket リアルタイム更新 |
| 1.3.0 | TBD | WearOS タイル化 |
| 2.0.0 | TBD | Google Tasks 統合 |


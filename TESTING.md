# テスティングガイド

買い物リストアプリのテスト戦略・実装・実行方法。

## テスト構成

```
shopping-list-app/
├── app/src/test/           # Unit Tests（デバイスなし）
├── app/src/androidTest/    # Instrumented Tests（エミュレータ/実機）
├── shared/src/test/        # Shared library unit tests
└── TESTING.md              # このファイル
```

## テスト対象

### 高優先度（コア機能）
1. **HAClient API 呼び出し** - HA との通信
2. **データストア I/O** - Preferences の保存・読み込み
3. **Entity ID 設定** - 柔軟なエンティティ設定

### 中優先度（ウィジェット）
1. Widget UI レンダリング
2. Item 削除ロジック
3. リフレッシュ動作

### 低優先度（WearOS）
1. Wear UI レンダリング
2. Wear での削除操作

## 1. Unit Tests（デバイス不要・高速）

### HAClient テスト

```bash
# テスト実行
./gradlew test

# 特定テストクラスのみ
./gradlew test --tests "*.HAClientTest"

# カバレッジレポート生成
./gradlew testDebugUnitTest --tests "*.HAClientTest" --info
```

**サンプルテスト** (`shared/src/test/...`):

```kotlin
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class HAClientTest {
    private lateinit var haClient: HAClient

    @Before
    fun setUp() {
        // Mock Context と EncryptedSharedPreferences をセットアップ
        val context = ApplicationProvider.getApplicationContext<Context>()
        haClient = HAClient(context)
    }

    @Test
    fun testGetEntityIdDefault() {
        val entityId = haClient.getEntityId()
        assertEquals("todo.home", entityId)
    }

    @Test
    fun testSaveAndGetEntityId() {
        haClient.saveEntityId("todo.google_tasks")
        assertEquals("todo.google_tasks", haClient.getEntityId())
    }

    @Test
    fun testGetSettingsFromPrefs() {
        haClient.saveSettings("http://192.168.1.12:8123", "token123")
        assertEquals("http://192.168.1.12:8123", haClient.getHAUrl())
        assertEquals("token123", haClient.getHAToken())
    }
}
```

## 2. Instrumented Tests（エミュレータ/実機で実行）

エミュレータで実行される統合テスト：

```bash
# エミュレータ起動
emulator -avd shopping-list-test -snapshot-load &
adb wait-for-device

# Instrumented テスト実行
./gradlew connectedAndroidTest

# 特定テストのみ
./gradlew connectedAndroidTest --tests "*.WidgetTest"
```

## 3. 手動テストチェックリスト

### Settings Activity
- [ ] HA URL 入力可能
- [ ] Token 入力可能
- [ ] Entity ID 入力可能
- [ ] 「接続テスト」でエラーハンドリング
- [ ] 「保存」で設定が保存される
- [ ] 「📱 ホーム画面にウィジェットを追加」でダイアログ表示

### ウィジェット
- [ ] ホーム画面に表示される
- [ ] リスト項目が表示される
- [ ] 「✓」で削除できる
- [ ] 「🔄 更新」で手動リフレッシュできる
- [ ] Alexa で追加した項目が表示される（15分以内）

### WearOS
- [ ] アプリアイコンからアプリ起動可能
- [ ] リスト項目が表示される
- [ ] 「✗」で削除できる
- [ ] 「🔄 再読み込み」で手動リフレッシュできる

## 4. バグレポートテンプレート

テスト中にバグを見つけた場合：

```markdown
### バグ: [簡潔な説明]

**再現手順**:
1. ...
2. ...
3. 期待値: ...
4. 実際: ...

**デバイス**:
- Android バージョン: 
- デバイス: エミュレータ / 実機
- App バージョン: 

**ログ**:
\`\`\`
adb logcat -s "ShoppingList"
\`\`\`
```

## 5. パフォーマンステスト

### メモリ使用量
```bash
adb shell dumpsys meminfo com.example.shoppinglist
```

### バッテリー消費（WorkManager）
- 15分ポーリングの電力消費を計測
- 予想: 1日あたり 1-2% 消費

### UI レスポンス
- ウィジェット更新レイテンシ: < 2秒
- Item 削除レスポンス: < 1秒

## 6. CI/CD テスト実行

GitHub Actions で全コミットに対してテスト：

```yaml
# .github/workflows/test.yml
- name: Run Tests
  run: ./gradlew test

- name: Run Instrumented Tests
  uses: ReactiveCircus/android-emulator-runner@v2
  with:
    api-level: 35
    script: ./gradlew connectedAndroidTest
```

## テスト実行スクリプト

```bash
#!/bin/bash
# run_all_tests.sh

set -e

echo "🧪 Running Unit Tests..."
./gradlew test

echo "🧪 Running Lint..."
./gradlew lintDebug

echo "🧪 Building Debug APK..."
./gradlew assembleDebug

echo "✅ All tests passed!"
```

## 既知の制限

- EncryptedSharedPreferences は実機/エミュレータでのみテスト可能
- WebSocket リアルタイム更新テストは HA インスタンスが必要
- WearOS テストは Wear エミュレータで実行

## 参考

- [Android Testing Guide](https://developer.android.com/training/testing)
- [JUnit4](https://junit.org/junit4/)
- [Espresso](https://developer.android.com/training/testing/espresso)
- [WorkManager Testing](https://developer.android.com/training/testing/work)

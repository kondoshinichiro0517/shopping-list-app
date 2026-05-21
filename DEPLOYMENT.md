# デプロイメントガイド

開発環境からリリースまでのデプロイメント手順。

## デプロイ環境

| 環境 | 用途 | リリース手段 |
|------|------|-----------|
| **ローカル開発** | デバッグ・開発 | `bash install.sh phone` |
| **エミュレータ** | QA テスト | `adb install *.apk` |
| **実機** | ベータテスト | `adb install *.apk` |
| **Google Play** | 本番リリース | Google Play Console |
| **自社配布** | 妻・家族共有 | `.apk` ファイル直接配布 |

## 1. デバッグビルド（開発用）

```bash
# ワンステップインストール（推奨）
cd /mnt/nas/kondoshinichiro0517/.lab/shopping-list-app
bash install.sh phone

# 手動ビルド
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**特徴**：
- BuildConfig に Token・URL 自動埋め込み
- コンパイル時間: ~2分
- APK サイズ: ~5MB

---

## 2. リリースビルド（署名必須）

### ステップ 1: キーストア作成（初回のみ）

```bash
# RSA 2048bit、有効期限10000日のキーストアを生成
keytool -genkey -v -keystore shopping-list.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias shopping-list \
  -dname "CN=ShoppingList, O=Home, C=JP"

# パスフレーズを安全に保管（Vaultwarden で管理推奨）
chmod 600 shopping-list.jks
```

### ステップ 2: build.gradle.kts で署名設定を追加

```kotlin
// app/build.gradle.kts

android {
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "shopping-list.jks"
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            val keyAlias = System.getenv("KEY_ALIAS") ?: "shopping-list"
            val keyPassword = System.getenv("KEY_PASSWORD") ?: ""

            storeFile = file(keystorePath)
            storePassword = keystorePassword
            keyAlias = keyAlias
            keyPassword = keyPassword
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### ステップ 3: リリース APK をビルド

```bash
# Vaultwarden から認証情報を取得
export KEYSTORE_PATH="shopping-list.jks"
export KEYSTORE_PASSWORD=$(bw-secret.sh "shopping_list_keystore_password")
export KEY_ALIAS="shopping-list"
export KEY_PASSWORD=$(bw-secret.sh "shopping_list_key_password")

# リリースビルド
./gradlew assembleRelease

# APK を確認
ls -lh app/build/outputs/apk/release/app-release.apk
```

**リリース APK の特徴**：
- 難読化（ProGuard）有効
- リソース最適化有効
- デバッグシンボル削除
- APK サイズ: ~3MB（デバッグの 60%）

---

## 3. デバイスへの配布

### 方法 A: USB ADB 経由（開発環境）

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

### 方法 B: APK ファイル直接配布

```bash
# APK をコピー
cp app/build/outputs/apk/release/app-release.apk ~/Downloads/shopping-list.apk

# 妻のデバイスに転送
# 方法 1: USB ケーブルで接続
adb push shopping-list.apk /sdcard/Download/

# 方法 2: メール・Dropbox など経由で配布
```

**妻のデバイスでのインストール**：
```
ダウンロード → shopping-list.apk をタップ → インストール → アプリ実行
```

### 方法 C: Google Play 内部テスト（推奨・最も簡単）

```bash
# 1. Google Play Console で App を登録
# https://play.google.com/console

# 2. Android App Bundle をビルド
./gradlew bundleRelease

# 3. Google Play Console にアップロード
# App → Release → Testing → Internal testing → Upload AAB

# 4. テスター（妻）を招待
# Console → Internal testing → Testers → Add tester (メールアドレス)

# 5. テスターが Google Play Store 経由で自動インストール
# Google Play Store で「Shopping List」を検索 → インストール
```

**メリット**：
- APK 配布手間なし
- 自動更新対応
- クラッシュレポート収集

---

## 4. GitHub Actions で自動ビルド

```yaml
# .github/workflows/release.yml
name: Release Build

on:
  push:
    tags: v*

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: 11
          distribution: adopt

      - name: Build Release APK
        env:
          KEYSTORE_PATH: ${{ secrets.KEYSTORE_PATH }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew assembleRelease

      - name: Create Release
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: ${{ github.ref }}
          release_name: Release ${{ github.ref }}
          body: Automated release build
          draft: false
          prerelease: false

      - name: Upload Release APK
        uses: actions/upload-release-asset@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          upload_url: ${{ steps.create_release.outputs.upload_url }}
          asset_path: ./app/build/outputs/apk/release/app-release.apk
          asset_name: shopping-list-${{ github.ref }}.apk
          asset_content_type: application/vnd.android.package-archive
```

**使用方法**：
```bash
# タグを作成してプッシュ
git tag v1.0.0
git push origin v1.0.0

# GitHub Actions が自動で Release APK をビルド
# Releases ページに APK がアップロードされる
```

---

## 5. バージョン管理

### app/build.gradle.kts での自動バージョン管理

```kotlin
// git tag から自動バージョン取得
import java.io.File

val gitTag = "git describe --tags --always".execute().text.trim()
val versionCode = gitTag.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1

android {
    defaultConfig {
        versionCode = versionCode
        versionName = gitTag
    }
}
```

---

## 6. リリースチェックリスト

```markdown
### リリース前確認

- [ ] Unit tests 全て パス
- [ ] Lint チェック 0 エラー
- [ ] デバッグビルド で動作確認
- [ ] リリースビルド でコンパイル確認
- [ ] ProGuard 難読化後も機能正常
- [ ] ウィジェット追加・削除 動作確認
- [ ] WearOS 動作確認
- [ ] バージョン番号更新
- [ ] CHANGELOG.md 更新
- [ ] タグ作成・プッシュ

### リリース後確認

- [ ] Google Play Store で配布確認
- [ ] テスター（妻）が自動更新できることを確認
- [ ] クラッシュレポート 異常なし
- [ ] GitHub Releases ページにアップロード確認
```

---

## トラブルシューティング

### 署名エラー

```bash
# キーストアのパスフレーズ確認
keytool -list -v -keystore shopping-list.jks

# キーストアが破損している場合
rm shopping-list.jks
keytool -genkey -v -keystore shopping-list.jks ...
```

### Google Play Console の署名の不一致

```bash
# 署名を確認
jarsigner -verify -certs -verbose app/build/outputs/apk/release/app-release.apk

# 署名キーの SHA256 を取得（Console との比較用）
keytool -list -v -keystore shopping-list.jks
```

---

## 参考

- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [Google Play Console](https://play.google.com/console)
- [GitHub Actions Release](https://github.com/actions/create-release)
- [Android App Bundle](https://developer.android.com/guide/app-bundle)

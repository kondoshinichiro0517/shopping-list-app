# GitHub セットアップ・プッシュガイド

Phase B の最終ステップとして、GitHub にコードをプッシュして CI/CD 自動化を有効化します。

## 現在の状況

- ✅ コード実装完了（Phase A・B）
- ✅ ローカル git コミット完了
- ❌ GitHub 認証が未設定

## GitHub 認証方法

### 方法 1: GitHub CLI（推奨）

最も簡単です。別のマシン（Windows PC など）で実行してください：

```bash
gh auth login
```

対話的に以下を選択：
```
? What is your preferred protocol for Git operations over HTTPS? [https]: HTTPS
? Authenticate with your GitHub credentials? [Y/n]: Y
? How would you like to authenticate? [web browser/token]: web browser
```

ブラウザが開いて GitHub ログイン画面が表示されるので、ログインして認可します。

**Ubuntu 環境での実行**（ブラウザなし）:

```bash
# この ubuntu-desktop では gh auth login がインタラクティブに機能しない場合、
# Windows PC で上のコマンドを実行後、認証ファイルをコピー
scp $HOME/.config/gh/hosts.yml ubuntu-desktop:~/.config/gh/
```

### 方法 2: Personal Access Token (PAT)

GitHub で新規トークンを作成：

1. https://github.com/settings/tokens にアクセス
2. "Generate new token" → "Generate new token (classic)"
3. スコープを選択：
   - ✅ `repo` (private repository access)
   - ✅ `workflow` (GitHub Actions workflows)
4. トークンをコピー（`ghp_` で始まる）
5. **このトークンを Vaultwar に保存**

その後、ubuntu-desktop で実行：

```bash
# ubuntu-desktop で実行
gh auth login --with-token
# プロンプトで token を貼り付け
```

### 方法 3: SSH キー

```bash
# 1. キーペア生成
ssh-keygen -t ed25519 -f ~/.ssh/github_key -N ""

# 2. 公開鍵を GitHub に登録（https://github.com/settings/keys）
cat ~/.ssh/github_key.pub

# 3. SSH URL に変更
cd /mnt/nas/kondoshinichiro0517/.lab/shopping-list-app
git remote set-url origin git@github.com:kondoshinichiro0517/shopping-list-app.git

# 4. プッシュ
git push -u origin master
```

## プッシュ実行

認証設定後、ubuntu-desktop で：

```bash
cd /mnt/nas/kondoshinichiro0517/.lab/shopping-list-app
git push -u origin master
```

**期待される出力:**

```
Enumerating objects: XX, done.
Counting objects: 100% (XX/XX), done.
Delta compression using up to 8 threads.
Compressing objects: 100% (XX/XX), done.
Writing objects: 100% (XX/XX), bytes: XXXXX, done.
...
To github.com:kondoshinichiro0517/shopping-list-app.git
 * [new branch]      master -> master
Branch 'master' set to track remote branch 'master' from 'origin'.
```

## GitHub Actions ワークフロー確認

プッシュ後、自動的に build.yml と lint.yml が実行されます。

```bash
# ワークフロー実行状況確認
gh run list --repo kondoshinichiro0517/shopping-list-app

# 特定実行の詳細確認
gh run view <RUN_ID>

# APK アーティファクト確認
gh run view <RUN_ID> --log
```

### GitHub Web UI での確認

1. https://github.com/kondoshinichiro0517/shopping-list-app
2. "Actions" タブをクリック
3. 最新の run を選択して build/lint 状況を確認
4. "Artifacts" セクションで APK をダウンロード可能

## トラブルシューティング

### GitHub repository が存在しない

GitHub Web UI で新規リポジトリを作成：
- Repository name: `shopping-list-app`
- Description: `Shopping list app with Home Assistant integration`
- Visibility: `Public` (または `Private`)
- Initialize: チェック不要（既にローカルで init 済み）

### GitHub authentication failed

```bash
# 認証状況確認
gh auth status

# 再度ログイン
gh auth logout
gh auth login
```

### Push が拒否される

```bash
# ローカルブランチが remote に追跡されていない場合
git push --set-upstream origin master

# 強制プッシュ（非推奨、確認後実行）
git push -f origin master
```

## Phase B 完了条件

- ✅ GitHub に code push 完了
- ✅ GitHub Actions build workflow が成功
- ✅ GitHub Actions lint workflow が成功
- ✅ APK アーティファクトが生成・ダウンロード可能

以降は Phase C（アーキテクチャ改善）へ進みます。

---

**次のステップ**: GitHub 認証を完了後、本ドキュメントのコマンドを実行してください。


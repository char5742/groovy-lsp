# VS Code E2E Tests

このディレクトリには、Groovy Language Server VS Code拡張機能のEnd-to-Endテストが含まれています。

## 前提条件

### Linux環境での実行

VS CodeのE2Eテストを実行するには、以下のライブラリが必要です：

```bash
# Debian/Ubuntu系の場合
sudo apt-get update
sudo apt-get install -y \
    libnss3 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libxkbcommon0 \
    libasound2

# RHEL/CentOS系の場合
sudo yum install -y \
    nss \
    atk \
    gtk3 \
    alsa-lib
```

### ヘッドレス環境での実行

CI環境やヘッドレス環境で実行する場合は、仮想ディスプレイが必要です：

```bash
# xvfbのインストール
sudo apt-get install -y xvfb

# テストの実行
xvfb-run -a npm test
```

## テストの実行

### 初回セットアップ

1. システムレベルの依存関係を確認・インストール：
   必要なライブラリが不足している場合は、下記のコマンドでインストールしてください。

2. Node.js依存関係のインストール：
   ```bash
   npm install
   ```

3. VS Code拡張機能のビルド：
   ```bash
   cd ../../vscode-extension
   npm install
   npm run compile
   cd ../e2e-tests/vscode-test
   ```

### テストの実行

```bash
# 通常実行（GUI環境）
npm test

# ヘッドレス実行（CI環境やSSH接続時）
xvfb-run -a npm test

# 軽量モックテスト（依存関係不要）
npm run test:mock

# 最小限のテスト実行
npm run test:minimal
```

## テスト構成

- `test/runTest.js` - VS Codeテスト環境のセットアップ
- `test/suite/index.js` - テストスイートのエントリーポイント
- `test/suite/extension.test.js` - 基本的な拡張機能のテスト
- `test/suite/languageFeatures.test.js` - Groovy言語機能のテスト

## トラブルシューティング

### "libnss3.so: cannot open shared object file" エラー

このエラーは必要なシステムライブラリが不足している場合に発生します。上記の前提条件セクションを参照してください。

### VS Codeのダウンロードに失敗する

プロキシ環境下では、以下の環境変数を設定してください：

```bash
export HTTPS_PROXY=http://your-proxy:port
export HTTP_PROXY=http://your-proxy:port
```

### 依存関係の確認

必要なライブラリが正しくインストールされているか確認するには、以下のコマンドを実行してください：

```bash
# 必要なライブラリの確認（Debian/Ubuntu）
for lib in libnss3.so libatk-bridge-2.0.so.0 libdrm.so.2 libXcomposite.so.1 libXdamage.so.1 libXfixes.so.3 libXrandr.so.2 libgbm.so.1 libxkbcommon.so.0 libasound.so.2; do
    echo -n "Checking $lib: "
    if ldconfig -p | grep -q "$lib"; then
        echo "✓ Found"
    else
        echo "✗ Not found"
    fi
done

# xvfb-runの確認
if command -v xvfb-run &> /dev/null; then
    echo "xvfb-run: ✓ Found"
else
    echo "xvfb-run: ✗ Not found - install with: sudo apt-get install xvfb"
fi
```

### CI環境でのセットアップ

GitHub Actionsなどの CI環境では、以下のようにセットアップしてください：

```yaml
# .github/workflows/test.yml の例
- name: Install system dependencies
  run: |
    sudo apt-get update
    sudo apt-get install -y \
      libnss3 \
      libatk-bridge2.0-0 \
      libdrm2 \
      libxcomposite1 \
      libxdamage1 \
      libxfixes3 \
      libxrandr2 \
      libgbm1 \
      libxkbcommon0 \
      libasound2 \
      xvfb

- name: Build extension
  run: |
    cd packages/vscode-extension
    npm install
    npm run compile

- name: Run tests
  run: |
    cd packages/e2e-tests/vscode-test
    npm install
    xvfb-run -a npm test
```

### タイムアウト対策

テストがタイムアウトする場合は、以下の環境変数を設定してタイムアウト時間を延長できます：

```bash
# 30秒のタイムアウトを設定
timeout 30s xvfb-run -a npm test
```

### クリーンな環境での実行

既存のXvfbプロセスと競合する場合は、以下のコマンドで既存プロセスを終了してから実行してください：

```bash
# 既存のXvfbプロセスを終了
pkill Xvfb || true

# 新しいディスプレイ番号を指定して実行
Xvfb :99 -screen 0 1024x768x24 > /dev/null 2>&1 &
DISPLAY=:99 npm run test:minimal
```

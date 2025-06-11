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

1. 依存関係のインストール：
   ```bash
   npm install
   ```

2. VS Code拡張機能のビルド：
   ```bash
   cd ../../vscode-extension
   npm install
   npm run compile
   cd ../e2e-tests/vscode-test
   ```

3. テストの実行：
   ```bash
   npm test
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
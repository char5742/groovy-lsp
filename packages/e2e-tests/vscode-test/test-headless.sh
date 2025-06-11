#!/bin/bash

# ヘッドレス環境でVS Code E2Eテストを実行するスクリプト

# 環境変数の設定
export DISPLAY=:99
export ELECTRON_RUN_AS_NODE=1
export ELECTRON_DISABLE_SANDBOX=1
export VSCODE_CLI=1

# 仮想ディスプレイなしでヘッドレスモードを強制
export ELECTRON_EXTRA_LAUNCH_ARGS='--disable-gpu --no-sandbox --disable-dev-shm-usage'

# テストの実行
echo "Running E2E tests in headless mode..."
cd "$(dirname "$0")"
npm test
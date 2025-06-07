# Groovy LSP

Groovy Tier-3 Language Server Protocol implementation based on LSP4J.

## 📋 目次

- [概要](#概要)
- [機能](#機能)
- [インストール](#インストール)
- [開発環境のセットアップ](#開発環境のセットアップ)
- [アーキテクチャ](#アーキテクチャ)
- [ビルドとテスト](#ビルドとテスト)
- [貢献方法](#貢献方法)
- [ドキュメント](#ドキュメント)

## 概要

Groovy LSPは、Apache GroovyのためのフルスペックのLanguage Server Protocol実装です。LSP4Jフレームワークをベースに、IDE並みの開発体験（Tier-3）を提供します。

### 主な特徴

- ✅ **リアルタイム診断** - 構文エラー、型エラーを即座に検出
- ✅ **コード補完** - インテリジェントな補完候補の提供
- ✅ **Hover情報** - 型情報とドキュメントの表示
- ✅ **定義へのジャンプ** - シンボルの定義位置へ移動
- ✅ **参照検索** - シンボルの使用箇所を検索
- ✅ **コードフォーマット** - Google Java Formatベースの整形
- ✅ **リンティング** - CodeNarc統合による静的解析
- 🚧 **リネーム** - シンボルの一括リネーム（開発中）
- 🚧 **コードアクション** - クイックフィックス（開発中）

## 機能

### 実装済み機能

| 機能 | 説明 | 対応状況 |
|------|------|----------|
| textDocument/didOpen | ファイルオープン時の診断 | ✅ |
| textDocument/didChange | リアルタイム診断 | ✅ |
| textDocument/hover | ホバー情報表示 | ✅ |
| textDocument/completion | コード補完 | ✅ |
| textDocument/definition | 定義ジャンプ | ✅ |
| textDocument/references | 参照検索 | ✅ |
| textDocument/formatting | ドキュメント全体のフォーマット | ✅ |
| textDocument/rangeFormatting | 範囲指定フォーマット | ✅ |
| workspace/symbol | ワークスペースシンボル検索 | ✅ |

## インストール

### VS Code

1. VS Code Marketplaceから"Groovy LSP"拡張機能をインストール（準備中）
2. または、手動でサーバーを起動:

```bash
# リリース版のダウンロード
curl -L https://github.com/your-org/groovy-lsp/releases/latest/download/groovy-lsp.jar -o groovy-lsp.jar

# 実行
java -jar groovy-lsp.jar --stdio
```

### その他のエディタ

LSPをサポートする任意のエディタで使用可能です。

## 開発環境のセットアップ

### 前提条件

- Java 21以上（推奨：Java 23）
- Gradle 8.11.1以上
- Groovy 4.0.27以上

### Dev Container（推奨）

1. VS Codeでプロジェクトを開く
2. "Dev Containers"拡張機能をインストール
3. "Reopen in Container"コマンドを実行

Dev Containerには以下が含まれています：
- Eclipse Temurin JDK 23
- Gradle 8.11.1
- Groovy 4.0.27
- 必要な開発ツール一式

### 手動セットアップ

```bash
# リポジトリのクローン
git clone https://github.com/your-org/groovy-lsp.git
cd groovy-lsp

# ビルド
./gradlew clean build

# サーバーの起動（ホットリロード対応）
./gradlew :server-launcher:run --stdin
```

### デバッグ

#### VS Code
1. サーバーをデバッグモードで起動（設定済み）
2. "Debug LSP Server"構成を使用
3. ポート5005でデバッガー接続を待機

#### IntelliJ IDEA
1. 上記のgradleコマンドでサーバーを起動
2. Remote JVM Debug構成を作成（ポート5005）
3. デバッグを開始

## アーキテクチャ

### モジュール構成

```
groovy-lsp/
├── packages/
│   ├── shared/              # 共通基盤（イベントバス、DDD）
│   ├── groovy-core/         # Groovyコンパイラコア（JPMS対応）
│   ├── lsp-protocol/        # LSPプロトコル実装
│   ├── workspace-index/     # ワークスペースインデックス
│   ├── jdt-adapter/         # Eclipse JDT連携
│   ├── codenarc-lint/       # CodeNarc統合
│   ├── formatting/          # コードフォーマッター
│   └── server-launcher/     # メインエントリポイント
├── integration-tests/       # 統合テスト
├── benchmarks/             # パフォーマンスベンチマーク
└── e2e-tests/              # E2Eテスト
```

### 依存関係

```
server-launcher
    ├── shared (基盤)
    ├── groovy-core (コンパイラ)
    ├── lsp-protocol (LSP実装)
    ├── workspace-index (インデックス)
    ├── jdt-adapter (JDT連携)
    ├── codenarc-lint (静的解析)
    └── formatting (フォーマット)
```

### JPMS対応

`shared`と`groovy-core`モジュールはJPMS（Java Platform Module System）に対応しています。
詳細は[docs/JPMS-STRATEGY.md](docs/JPMS-STRATEGY.md)を参照してください。

## ビルドとテスト

### 基本的なコマンド

```bash
# クリーンビルド
./gradlew clean build

# 特定モジュールのビルド
./gradlew :server-launcher:build

# テスト実行
./gradlew test

# 統合テスト
./gradlew integrationTest

# カバレッジレポート生成
./gradlew jacocoTestReport

# コードフォーマット
./gradlew spotlessApply
```

### テストカバレッジ

全モジュールで80%以上のテストカバレッジを維持しています：
- 単体テスト（JUnit 5 + AssertJ）
- 統合テスト（実際のLSPプロトコル通信）
- E2Eテスト（VS Code環境での動作確認）
- パフォーマンスベンチマーク（JMH）

## 貢献方法

### 開発ワークフロー

1. Issueを作成または既存のIssueを選択
2. フィーチャーブランチを作成（`feat/your-feature`）
3. 変更をコミット（Conventional Commits形式）
4. テストを追加・更新
5. Pull Requestを作成

### コミットメッセージ形式

```
<type>(<scope>): <subject>

例:
feat(groovy-core): Add incremental compilation support
fix(lsp-protocol): Fix hover position calculation
docs(readme): Update installation instructions
```

### Git Hooks

lefthookによる自動チェック：
- pre-commit: Spotlessフォーマット、変更モジュールのテスト
- pre-push: ビルド、全テスト、カバレッジチェック
- commit-msg: Conventional Commits形式検証

## ドキュメント

詳細なドキュメントは[docs/](docs/)ディレクトリを参照：

📚 **[ドキュメントインデックス](docs/INDEX.md)** - すべてのドキュメントへのガイド

### 主要ドキュメント

- [アーキテクチャ概要](docs/ABSTRACT.md) - システム全体の設計
- [JPMS戦略](docs/JPMS-STRATEGY.md) - Java Platform Module System対応
- [テスト戦略](docs/TEST.md) - テストのベストプラクティス
- [静的解析](docs/STATIC_ANALYSIS.md) - Error ProneとNullAway設定
- [改善提案](docs/IMPROVEMENTS.md) - 実施済み改善内容
- [モジュラーアーキテクチャ](docs/MODULAR_MONOLITH_IMPROVEMENT.md) - 将来の拡張計画

## ライセンス

Apache License 2.0

## サポート

- バグ報告: [GitHub Issues](https://github.com/your-org/groovy-lsp/issues)
- 質問: [GitHub Discussions](https://github.com/your-org/groovy-lsp/discussions)
- コントリビューション: [CONTRIBUTING.md](CONTRIBUTING.md)

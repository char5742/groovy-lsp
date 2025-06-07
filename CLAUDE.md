# CLAUDE.md

このファイルは、このリポジトリでコードを扱う際のClaude Code (claude.ai/code)へのガイダンスを提供します。

## プロジェクト概要

Groovy LSP (Language Server Protocol) - LSP4Jベースの Groovy Tier-3 言語サーバー実装

## よく使用するコマンド

### ビルド
```bash
./gradlew clean build              # 全体のビルド
./gradlew :server-launcher:build   # 特定モジュールのビルド
```

### テスト
```bash
./gradlew test                     # 全モジュールのテスト
./gradlew :shared:test             # 特定モジュールのテスト
./gradlew jacocoTestReport         # カバレッジレポート生成
./gradlew jacocoTestCoverageVerification  # カバレッジ閾値チェック（80%）
```

### 開発サーバー起動
```bash
./gradlew :server-launcher:run --stdin    # ホットリロード対応のLSPサーバー起動
```

### コード品質
```bash
./gradlew spotlessCheck            # コードフォーマットチェック
./gradlew spotlessApply            # コードフォーマット適用
./gradlew dependencyUpdates        # 依存関係の更新確認
```

## アーキテクチャ

### モジュール構成
```
shared/              # 基盤モジュール（イベントバス、共通機能）- JPMS対応
├── groovy-core/     # Groovyコンパイラコア機能 - JPMS対応
├── jdt-adapter/     # Groovy↔JDT変換ユーティリティ
├── lsp-protocol/    # LSPプロトコルDTOとアダプター
├── workspace-index/ # ワークスペースインデックスとシンボル管理
├── codenarc-lint/   # CodeNarc統合（リント機能）
├── formatting/      # google-java-formatベースのコードフォーマット
└── server-launcher/ # メインエントリポイント、LSPサーバー実装
```

### JPMS対応状況
- `shared`と`groovy-core`モジュールのみJPMS対応（module-info.java実装済み）
- 他モジュールは外部ライブラリの自動モジュール依存のため非対応
- 詳細は[JPMS-STRATEGY.md](docs/JPMS-STRATEGY.md)参照

## 開発ワークフロー

### Git Hooks (lefthook)
- **pre-commit**: Spotlessチェック、変更モジュールのテスト
- **pre-push**: コンパイル、テスト、カバレッジチェック（80%閾値）
- **commit-msg**: Conventional Commits形式チェック

### コミットメッセージ形式
```
<type>(<scope>): <subject>
例: feat(groovy-core): Add incremental compilation support
```

タイプ: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert

## コーディング規約

- **@SuppressWarnings**: 極力使用しない
- **禁止**: `git push --no-verify`
- **フォーマット**: Google Java Format (AOSP style)
- **静的解析**: Error Prone + NullAway
- **import**: ワイルドカードインポート禁止、FQDNを短縮

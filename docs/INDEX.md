# Groovy LSP ドキュメントインデックス

このドキュメントは、Groovy LSPプロジェクトのドキュメント体系のエントリーポイントです。

## 📚 ドキュメント構成

### 🏠 プロジェクトルート

- **[README.md](../README.md)** - プロジェクト概要、クイックスタート、基本的な使い方

### 📖 技術ドキュメント

#### アーキテクチャ・設計

- **[ABSTRACT.md](ABSTRACT.md)** - システムアーキテクチャ概要
  - プロジェクトの全体像、技術スタック、モジュール構成
  - 主要な処理フロー、パフォーマンス設計
  - 開発ロードマップ

#### 実装詳細

- **[JPMS-STRATEGY.md](JPMS-STRATEGY.md)** - Java Platform Module System戦略
  - 現在のJPMS対応状況
  - 技術的課題と解決策
  - 将来の移行計画

- **[STATIC_ANALYSIS.md](STATIC_ANALYSIS.md)** - 静的解析ツール設定
  - Error ProneとNullAwayの設定
  - jSpecifyによるNull安全性
  - トラブルシューティング

- **[TEST.md](TEST.md)** - テスト戦略
  - テストレベル（単体、統合、E2E、パフォーマンス）
  - テストフレームワークとツール
  - カバレッジ目標とベストプラクティス

- **[DEFINITION_REFERENCES.md](DEFINITION_REFERENCES.md)** - 定義ジャンプ・参照検索機能
  - サポートされる要素と使用方法
  - 現在の制限事項とパフォーマンス
  - 技術的詳細とトラブルシューティング

#### 改善・拡張

- **[IMPROVEMENTS.md](IMPROVEMENTS.md)** - 実施済み改善内容
  - PR #19で実施した改善の詳細
  - ビルドエラーの修正履歴
  - CI/CDパイプラインの設定

- **[MODULAR_MONOLITH_IMPROVEMENT.md](MODULAR_MONOLITH_IMPROVEMENT.md)** - モジュラーアーキテクチャ改善提案
  - 将来のアーキテクチャ改善計画
  - イベント駆動アーキテクチャの導入
  - 段階的移行計画

## 🗺️ ドキュメントマップ

```
初めての方向け
├── README.md ────────────┐
│                         ↓
│                    ABSTRACT.md（全体像を理解）
│                         ↓
└──→ 開発者向け      実装詳細を深掘り
     ├── TEST.md（テスト方法）
     ├── STATIC_ANALYSIS.md（品質管理）
     └── JPMS-STRATEGY.md（モジュール設計）

アーキテクト向け
├── ABSTRACT.md ──────────┐
│                         ↓
├── MODULAR_MONOLITH_IMPROVEMENT.md（将来設計）
│                         ↓
└── IMPROVEMENTS.md（過去の改善履歴）
```

## 🎯 読者別ガイド

### 新規開発者

1. **[README.md](../README.md)** - プロジェクトの概要とセットアップ
2. **[ABSTRACT.md](ABSTRACT.md)** - アーキテクチャの理解
3. **[TEST.md](TEST.md)** - テストの書き方と実行方法

### コントリビューター

1. **[README.md](../README.md)** - 貢献方法とコーディング規約
2. **[STATIC_ANALYSIS.md](STATIC_ANALYSIS.md)** - コード品質基準
3. **[TEST.md](TEST.md)** - テストカバレッジ要件

### アーキテクト/技術リード

1. **[ABSTRACT.md](ABSTRACT.md)** - 現在のアーキテクチャ
2. **[MODULAR_MONOLITH_IMPROVEMENT.md](MODULAR_MONOLITH_IMPROVEMENT.md)** - 将来の改善案
3. **[JPMS-STRATEGY.md](JPMS-STRATEGY.md)** - モジュール化戦略

### 運用担当者

1. **[README.md](../README.md)** - インストールと起動方法
2. **[ABSTRACT.md](ABSTRACT.md)** - セキュリティとエラー処理
3. **[IMPROVEMENTS.md](IMPROVEMENTS.md)** - CI/CD設定

## 📝 ドキュメント管理

### 更新ポリシー

- **実装の変更時**: 関連するドキュメントを同じPRで更新
- **四半期レビュー**: 全ドキュメントの整合性確認
- **リリース前**: ロードマップとバージョン情報の更新

### ドキュメントの種類

| 種類 | 更新頻度 | 主な読者 |
|------|---------|---------|
| README.md | 頻繁 | 全員 |
| ABSTRACT.md | 中程度 | 開発者、アーキテクト |
| TEST.md | 中程度 | 開発者、QA |
| JPMS-STRATEGY.md | 低頻度 | アーキテクト |
| STATIC_ANALYSIS.md | 低頻度 | 開発者 |
| IMPROVEMENTS.md | PR毎 | 開発者、PM |
| MODULAR_MONOLITH_IMPROVEMENT.md | 低頻度 | アーキテクト |

## 🔗 外部リンク

### 公式ドキュメント

- [Language Server Protocol](https://microsoft.github.io/language-server-protocol/)
- [Apache Groovy](https://groovy-lang.org/)
- [LSP4J](https://github.com/eclipse/lsp4j)

### 関連プロジェクト

- [GitHub Repository](https://github.com/your-org/groovy-lsp)
- [Issue Tracker](https://github.com/your-org/groovy-lsp/issues)
- [Discussions](https://github.com/your-org/groovy-lsp/discussions)

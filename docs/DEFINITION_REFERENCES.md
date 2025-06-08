# 定義ジャンプ・参照検索機能

このドキュメントでは、Groovy LSPの定義ジャンプ(Go to Definition)と参照検索(Find References)機能について説明します。

## 概要

Groovy LSPは、LSPプロトコルの`textDocument/definition`と`textDocument/references`をサポートしており、以下の機能を提供します：

- **定義ジャンプ**: 変数、メソッド、クラスなどの定義元へジャンプ
- **参照検索**: 変数、メソッド、クラスなどが使用されている全ての箇所を検索

## サポートされる要素

### 定義ジャンプが可能な要素
- ローカル変数
- メソッドパラメータ
- メソッド呼び出し
- クラスの使用箇所
- フィールド/プロパティのアクセス

### 参照検索が可能な要素
- 変数（ローカル変数、パラメータ）
- メソッド（定義と呼び出し）
- クラス（定義と使用箇所）
- フィールド/プロパティ

## 使用方法

### VS Codeでの使用

1. **定義ジャンプ**
   - `F12`キーを押す
   - または、右クリックして「定義へ移動」を選択
   - または、`Ctrl/Cmd + クリック`

2. **参照検索**
   - `Shift + F12`キーを押す
   - または、右クリックして「すべての参照を検索」を選択

### その他のエディタ

LSPをサポートする他のエディタでも、エディタ固有のキーバインドで同様の機能を使用できます。

## 現在の制限事項

1. **ローカルファイル内のみ対応**
   - 現在はファイル内の定義・参照のみをサポート
   - クロスファイル検索は今後実装予定（[Issue #43](https://github.com/char5742/groovy-lsp/issues/43)）

2. **動的型の制限**
   - Groovyの動的な性質により、一部の動的メソッド呼び出しは解決できない場合があります

3. **外部ライブラリ**
   - 外部JARファイル内の定義へのジャンプは未対応

## パフォーマンス

- 目標応答時間: 50ms以内
- 大規模ファイルでも高速に動作するよう最適化されています

## 技術的詳細

### アーキテクチャ

```
LanguageClient
    ↓
textDocument/definition または textDocument/references
    ↓
GroovyTextDocumentService
    ↓
DefinitionHandler / ReferencesHandler
    ↓
ASTService (AST解析)
    ↓
結果を返す
```

### 実装の特徴

1. **Visitorパターン**: ASTを効率的に走査するためにVisitorパターンを使用
2. **非同期処理**: CompletableFutureを使用した非同期処理でUIをブロックしない
3. **エラーハンドリング**: 構文エラーや不正な位置でも適切に処理

## トラブルシューティング

### 定義ジャンプが機能しない場合

1. ファイルに構文エラーがないか確認
2. カーソルが正しい位置（識別子の上）にあるか確認
3. LSPサーバーが正常に起動しているか確認

### パフォーマンスの問題

1. ファイルサイズが極端に大きい場合は、ファイルを分割することを検討
2. LSPサーバーのログでエラーがないか確認

## 今後の改善予定

1. **クロスファイル対応**: WorkspaceIndexServiceとの統合により実現予定
2. **外部ライブラリ対応**: JARファイル内の定義へのジャンプ
3. **型推論の改善**: より正確な動的メソッド解決
4. **リネーム機能**: 定義と全ての参照を一括でリネーム

## 関連ドキュメント

- [LSP Specification - textDocument/definition](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_definition)
- [LSP Specification - textDocument/references](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_references)

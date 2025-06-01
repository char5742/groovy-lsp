# JPMS対応状況

## 概要
このドキュメントは、groovy-lspプロジェクトにおけるJava Platform Module System (JPMS)の対応状況をまとめたものです。

## 現在のJPMS対応状況

### JPMS対応モジュール
- **対象モジュール**:
  - `shared`: イベントバスなどの共通機能
  - `groovy-core`: Groovyコンパイラのコア機能
- **自動的に有効**: build.gradleで設定済み

### JPMS非対応モジュール（外部ライブラリ制約）
- `lsp-protocol`: Eclipse LSP4Jが自動モジュール
- `workspace-index`: Gradle Tooling API、LMDBが自動モジュール
- `codenarc-lint`: CodeNarcが自動モジュール
- `formatting`: Google Java Formatが自動モジュール
- `jdt-adapter`: Eclipse JDT関連のスプリットパッケージ問題
- `server-launcher`: 上記すべてのモジュールに依存

## モジュール依存関係

```
shared (基盤モジュール)
  ├── groovy-core
  ├── jdt-adapter
  ├── lsp-protocol
  ├── workspace-index
  ├── codenarc-lint
  └── formatting

server-launcher (アプリケーションモジュール)
  └── 全モジュールに依存
```

## 実装済みmodule-info.java

### 1. **shared モジュール**
   ```java
   module com.groovy.lsp.shared {
       requires transitive org.jmolecules.ddd;
       requires transitive org.jmolecules.architecture.onion.classical;
       requires com.google.common;

       exports com.groovy.lsp.shared.event;
   }
   ```

### 2. **groovy-core モジュール**
   ```java
   module com.groovy.lsp.groovy.core {
       requires transitive com.groovy.lsp.shared;
       requires org.apache.groovy;
       requires org.apache.groovy.json;
       requires org.apache.groovy.xml;
       requires org.apache.groovy.templates;
       requires org.slf4j;
       requires static org.apiguardian.api;

       exports com.groovy.lsp.groovy.core.api;
   }
   ```

## ビルドとテスト

```bash
# 通常のビルド（JPMS対応モジュールは自動的に有効）
./gradlew clean build

# 特定モジュールのテスト
./gradlew :shared:test
./gradlew :groovy-core:test

# 統合テスト
./gradlew :integration-tests:test
```

## 問題解決ガイド

### スプリットパッケージの問題
- 同じパッケージが複数のモジュールに存在する場合
- 解決策: パッケージ構造の再編成

### 循環依存の問題
- モジュール間で循環参照が発生する場合
- 解決策: 依存関係の再設計、インターフェースの抽出

### リフレクションアクセスの問題
- JPMSによりリフレクションが制限される場合
- 解決策: `opens`ディレクティブの追加

## 実装状況

- [x] コアモジュール（shared、groovy-core）のmodule-info.java作成
- [x] ビルドとテストが成功することを確認
- [x] 自動モジュール依存の調査と制限の確認
- [x] 統合テストの実行と確認

## 今後の展望

完全なJPMS対応には以下が必要:
- 外部ライブラリのJPMS対応を待つ
- または、モジュラーJARラッパーの作成を検討
- 現時点では部分的なJPMS採用が最適解

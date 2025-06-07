# Groovy LSP - JPMS (Java Platform Module System) 戦略

## 概要

Groovy LSPプロジェクトでは、Java Platform Module System (JPMS) を段階的に採用しています。このドキュメントでは、現在の対応状況、課題、および将来の戦略について説明します。

## 現在のJPMS対応状況

### ✅ JPMS対応モジュール

| モジュール | module-info.java | 理由 |
|-----------|-----------------|------|
| `shared` | ✅ 実装済み | 外部依存が少なく、明確なAPI境界を持つ |
| `groovy-core` | ✅ 実装済み | Groovyライブラリ自体がJPMS対応 |

### ❌ JPMS非対応モジュール

| モジュール | 制約理由 | 回避策 |
|-----------|---------|--------|
| `lsp-protocol` | Eclipse LSP4Jが自動モジュール | クラスパス使用 |
| `workspace-index` | Gradle Tooling API、LMDBが自動モジュール | クラスパス使用 |
| `codenarc-lint` | CodeNarcが自動モジュール | クラスパス使用 |
| `formatting` | Google Java Formatが自動モジュール | クラスパス使用 |
| `jdt-adapter` | Eclipse JDTのスプリットパッケージ問題 | クラスパス使用 |
| `server-launcher` | 非対応モジュールに依存 | クラスパス使用 |

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

### 1. shared モジュール (`packages/shared/src/main/java/module-info.java`)

```java
module com.groovy.lsp.shared {
    requires transitive org.jmolecules.ddd;
    requires transitive org.jmolecules.architecture.onion.classical;
    requires com.google.common;

    exports com.groovy.lsp.shared.event;
}
```

### 2. groovy-core モジュール (`packages/groovy-core/src/main/java/module-info.java`)

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

## ビルド設定

### build.gradle での JPMS 設定

```groovy
java {
    // JPMS有効化設定
    def enableJpms = project.name in ['shared', 'groovy-core']

    modularity.inferModulePath = enableJpms

    if (!enableJpms) {
        logger.debug("JPMS disabled for module: ${project.name} (auto-module dependencies)")
    }
}
```

### ビルドとテスト

```bash
# 通常のビルド（JPMS対応モジュールは自動的に有効）
./gradlew clean build

# 特定モジュールのテスト
./gradlew :shared:test
./gradlew :groovy-core:test

# 統合テスト
./gradlew integrationTest
```

## 技術的課題と解決策

### 1. スプリットパッケージ問題

**問題**: 同じパッケージが複数のモジュールに存在する場合、JPMSエラーが発生

**解決策**:
- パッケージ構造の再編成
- 影響を受けるモジュールをクラスパスに留める

### 2. 自動モジュール依存

**問題**: 多くの外部ライブラリがJPMS非対応（自動モジュール）

**解決策**:
- 自動モジュールに依存するモジュールはクラスパス使用を継続
- 段階的移行戦略の採用

### 3. リフレクションアクセス

**問題**: JPMSによりリフレクションが制限される

**解決策**:
```java
module com.example {
    opens com.example.internal to specific.module;  // 特定モジュールのみ
    opens com.example.public;                        // 全モジュール
}
```

### 4. テスト時のモジュールアクセス

**問題**: テストコードからプライベートAPIへのアクセスが必要

**解決策**:
- テスト時のみ `--add-opens` JVMオプションを使用
- テスト専用のmodule-info.javaを作成

## 実装チェックリスト

- [x] コアモジュール（shared、groovy-core）のmodule-info.java作成
- [x] ビルドシステムでのJPMS設定
- [x] 自動モジュール依存の調査
- [x] 統合テストでの動作確認
- [x] パフォーマンスへの影響評価

## 将来の展望

### 短期目標（6ヶ月）

1. **依存ライブラリの更新監視**
   - LSP4JのJPMS対応状況を追跡
   - CodeNarcの更新を監視

2. **モジュラーJARラッパーの検討**
   - 自動モジュールをラップする薄いJPMSモジュール作成

### 中期目標（1年）

1. **段階的JPMS移行**
   - jdt-adapterの再設計（スプリットパッケージ解消）
   - lsp-protocolの部分的JPMS対応

2. **ツールサポートの改善**
   - jlinkによるカスタムランタイム作成
   - モジュール依存関係の可視化ツール導入

### 長期目標（2年）

1. **完全JPMS対応**
   - 全モジュールのmodule-info.java実装
   - 最小限のランタイムイメージ生成

2. **パフォーマンス最適化**
   - モジュール境界での最適化
   - 起動時間の短縮

## まとめ

現時点では、外部ライブラリの制約により部分的なJPMS採用が最適解です。`shared`と`groovy-core`モジュールでJPMSの利点を活用しつつ、他のモジュールは従来のクラスパス方式を維持することで、実用性と将来性のバランスを取っています。

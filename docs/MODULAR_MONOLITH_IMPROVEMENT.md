# Groovy LSP モジュラモノリス改善提案

## 概要

本ドキュメントは、2024年のモジュラモノリスのベストプラクティスに基づいて、Groovy LSPプロジェクトのアーキテクチャ改善を提案します。

## 現状分析

### 長所
- 明確な責務分離によるモジュール設計
- 各Bounded Contextが独立して進化可能
- 外部ライブラリの適切なラッピング

### 改善点
- モジュール間の明確なAPI定義が不足
- 内部実装の隠蔽が不十分
- モジュール間通信が直接的な依存関係に依存
- JPMSが無効化されている

## 改善提案

### 1. モジュール境界の明確化

各モジュールに以下の構造を導入：

```
packages/[module-name]/
├── src/main/java/com/groovy/lsp/[module]/
│   ├── api/           # 公開API（他モジュールから参照可能）
│   │   ├── [Module]Service.java
│   │   └── dto/       # データ転送オブジェクト
│   └── internal/      # 内部実装（他モジュールから参照不可）
│       ├── impl/      # サービス実装
│       └── domain/    # ドメインモデル
```

### 2. 内部実装の隠蔽

#### Gradleでのパッケージ可視性制御

各モジュールのbuild.gradleに以下を追加：

```gradle
// API Guardianを使用してパッケージの可視性を制御
dependencies {
    compileOnly 'org.apiguardian:apiguardian-api:1.1.2'
}

// ArchUnitによるアーキテクチャテスト
testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
```

#### アーキテクチャテストの例

```java
@AnalyzeClasses(packages = "com.groovy.lsp")
public class ModularArchitectureTest {

    @ArchTest
    static final ArchRule internalPackagesShouldNotBeAccessedFromOutside =
        noClasses()
            .that().resideOutsideOfPackage("..internal..")
            .should().accessClassesThat()
            .resideInAPackage("..internal..");

    @ArchTest
    static final ArchRule modulesShouldOnlyAccessPublicAPIs =
        classes()
            .that().resideInAPackage("com.groovy.lsp.(*)..")
            .should().onlyAccessClassesThat()
            .resideInAnyPackage(
                "com.groovy.lsp.$1..",
                "com.groovy.lsp.(*).api..",
                "java..",
                "org.apache.groovy.."
            );
}
```

### 3. イベント駆動アーキテクチャの導入

#### EventBusの実装

```java
// packages/shared/src/main/java/com/groovy/lsp/shared/event/EventBus.java
public interface EventBus {
    void publish(DomainEvent event);
    <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler);
}

// packages/shared/src/main/java/com/groovy/lsp/shared/event/DomainEvent.java
public abstract class DomainEvent {
    private final Instant occurredOn;
    private final String aggregateId;

    protected DomainEvent(String aggregateId) {
        this.occurredOn = Instant.now();
        this.aggregateId = aggregateId;
    }
}
```

#### イベント例

```java
// packages/workspace-index/src/main/java/com/groovy/lsp/workspace/api/events/
public class WorkspaceIndexedEvent extends DomainEvent {
    private final String workspacePath;
    private final int symbolCount;

    public WorkspaceIndexedEvent(String workspacePath, int symbolCount) {
        super(workspacePath);
        this.workspacePath = workspacePath;
        this.symbolCount = symbolCount;
    }
}
```

### 4. 依存関係の整理

#### 新しい依存関係構造

```
shared (共通基盤)
├── event-bus
├── common-dto
└── utilities

server-launcher
├── shared
├── lsp-protocol (api only)
├── groovy-core (api only)
├── jdt-adapter (api only)
├── codenarc-lint (api only)
├── workspace-index (api only)
└── formatting (api only)

各機能モジュール
├── shared
├── groovy-core (api only)
└── lsp-protocol (api only)
```

### 5. ビルド設定の最適化

#### settings.gradle

```gradle
rootProject.name = 'groovy-lsp'

// 共通設定を定義
dependencyResolutionManagement {
    versionCatalogs {
        libs {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

// モジュール定義
include ':packages:shared'
include ':packages:groovy-core'
include ':packages:lsp-protocol'
include ':packages:jdt-adapter'
include ':packages:codenarc-lint'
include ':packages:workspace-index'
include ':packages:formatting'
include ':packages:server-launcher'
```

#### gradle/libs.versions.toml

```toml
[versions]
groovy = "4.0.27"
slf4j = "2.0.16"
logback = "1.5.16"
junit = "5.11.4"
assertj = "3.27.3"
archunit = "1.3.0"
guava = "33.4.0-jre"

[libraries]
groovy-core = { module = "org.apache.groovy:groovy", version.ref = "groovy" }
groovy-json = { module = "org.apache.groovy:groovy-json", version.ref = "groovy" }
slf4j-api = { module = "org.slf4j:slf4j-api", version.ref = "slf4j" }
logback-classic = { module = "ch.qos.logback:logback-classic", version.ref = "logback" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
assertj-core = { module = "org.assertj:assertj-core", version.ref = "assertj" }
archunit = { module = "com.tngtech.archunit:archunit-junit5", version.ref = "archunit" }
guava = { module = "com.google.guava:guava", version.ref = "guava" }

[bundles]
groovy = ["groovy-core", "groovy-json"]
testing = ["junit-jupiter", "assertj-core", "archunit"]
```

### 6. モジュールAPI定義の例

#### groovy-core モジュール

```java
// packages/groovy-core/src/main/java/com/groovy/lsp/groovy/core/api/CompilerService.java
public interface CompilerService {
    CompilationResult compile(CompilationRequest request);
    ASTNode parseFile(String filePath);
}

// packages/groovy-core/src/main/java/com/groovy/lsp/groovy/core/api/dto/CompilationRequest.java
public record CompilationRequest(
    String sourceCode,
    String fileName,
    CompilerOptions options
) {}

// packages/groovy-core/src/main/java/com/groovy/lsp/groovy/core/internal/impl/CompilerServiceImpl.java
@Singleton
class CompilerServiceImpl implements CompilerService {
    // 実装の詳細は内部パッケージに隠蔽
}
```

### 7. 段階的移行計画

#### Phase 1: 基盤整備（1-2週間）
1. sharedモジュールの作成
2. EventBusの実装
3. アーキテクチャテストの導入

#### Phase 2: API分離（2-3週間）
1. 各モジュールのapi/internalパッケージ分離
2. DTOの定義
3. インターフェースの抽出

#### Phase 3: イベント駆動化（2-3週間）
1. モジュール間の直接依存を削減
2. イベントベースの通信に移行
3. 非同期処理の導入

#### Phase 4: 最適化（1-2週間）
1. パフォーマンステスト
2. ビルド時間の最適化
3. JPMSの再有効化検討

## まとめ

この改善により、以下のメリットが期待できます：

1. **保守性の向上**: 明確なモジュール境界により、変更の影響範囲を限定
2. **拡張性の向上**: 新機能の追加が既存コードに影響を与えにくい
3. **テスタビリティの向上**: モジュール単位でのテストが容易
4. **チーム開発の効率化**: モジュール単位での並行開発が可能
5. **将来的なマイクロサービス化**: 必要に応じて特定モジュールを切り出し可能

これらの改善は段階的に実施することで、既存機能への影響を最小限に抑えながら、モダンなモジュラモノリスアーキテクチャへの移行が可能です。

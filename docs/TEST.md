# Groovy LSP テスト戦略

## 概要

Groovy LSPプロジェクトでは、堅牢で保守性の高いLanguage Serverを実現するため、包括的なテスト戦略を採用しています。

## テストレベル

### 1. 単体テスト（Unit Tests）

| フレームワーク | 用途 | カバレッジ目標 |
|--------------|------|-------------|
| JUnit 5      | 基本的なテストフレームワーク | 80%以上 |
| AssertJ      | 流暢なアサーション | - |
| Mockito      | モックとスタブ | - |
| ArchUnit     | アーキテクチャ検証 | - |

各モジュールの単体テスト実装状況：
- ✅ **shared**: イベントバス、DDDパターン検証
- ✅ **groovy-core**: コンパイラ、AST、型推論
- ✅ **lsp-protocol**: ハンドラー、プロトコル処理
- ✅ **workspace-index**: インデックス、シンボル管理
- ✅ **codenarc-lint**: ルール実行、クイックフィックス
- ✅ **formatting**: フォーマッター動作
- ✅ **server-launcher**: 起動パラメータ、DI設定

### 2. 統合テスト（Integration Tests）

`packages/integration-tests/`に配置：
- **ModuleIntegrationTest**: モジュール間の連携検証
- **ServerIntegrationTest**: LSPサーバー全体の動作確認

主なテストシナリオ：
- Groovy CoreとLint Engineの統合
- FormatterとGroovy Coreの統合
- Workspace Indexerと複数モジュールの統合
- エンドツーエンド: コード変更から診断まで

### 3. E2Eテスト（End-to-End Tests）

`packages/e2e-tests/`に配置：
- VS Code環境での実際の動作確認
- クライアント-サーバー間のプロトコル通信
- 実際のGroovyプロジェクトでの動作検証

### 4. パフォーマンステスト（Performance Tests）

`packages/benchmarks/`に配置（JMH使用）：
- **ParsingBenchmark**: Groovyファイルのパース性能
- **CompletionBenchmark**: コード補完の応答速度
- **IndexingBenchmark**: ワークスペースインデックス性能
- **FormattingBenchmark**: フォーマッティング処理速度

## テストインフラストラクチャ

### テストユーティリティ

```java
// LSPプロトコルテスト用ハーネス
packages/lsp-protocol/src/test/java/
├── AbstractProtocolTest.java      // 基底テストクラス
├── LSPTestHarness.java           // LSP通信テスト支援
├── ProtocolMockServer.java       // モックサーバー
└── TestLanguageClient.java       // テスト用クライアント
```

### テストフィクスチャ

```
packages/shared/src/test/resources/fixtures/groovy/
├── SimpleClass.groovy      // 基本的なクラス定義
├── ClosureExample.groovy   // クロージャのテスト
├── DSLExample.groovy       // DSL構文のテスト
├── TraitExample.groovy     // トレイトのテスト
└── ErrorExample.groovy     // エラー検出のテスト
```

## テスト実行

### 基本コマンド

```bash
# 全モジュールのテスト実行
./gradlew test

# 特定モジュールのテスト
./gradlew :lsp-protocol:test

# 統合テストの実行
./gradlew integrationTest

# カバレッジレポート生成
./gradlew jacocoTestReport

# カバレッジ閾値チェック
./gradlew jacocoTestCoverageVerification

# パフォーマンステスト
./gradlew :benchmarks:jmh
```

### 継続的インテグレーション

GitHub Actionsでの自動テスト：
- プルリクエスト時: 変更モジュールのテスト
- プッシュ前: 全テスト + カバレッジチェック
- マルチプラットフォーム: Ubuntu, macOS, Windows
- Javaバージョンマトリクス: Java 21, 23

## テストベストプラクティス

### 1. テスト命名規則

```java
// Given-When-Then形式
@Test
void analyzeFile_shouldReturnDiagnostics_whenSyntaxErrorExists() {
    // Given: 構文エラーを含むファイル
    // When: analyzeFileを実行
    // Then: 診断結果にエラーが含まれる
}

// 日本語での記述も可能（統合テスト）
@Test
@DisplayName("ホバー機能の基本的な動作確認")
void testHoverFunctionality() {
    // テスト実装
}
```

### 2. モック戦略

```java
// 外部依存はモック化
@Mock private WorkspaceIndexService indexService;
@Mock private CompilerConfigurationService compilerService;

// 内部実装は実際のインスタンスを使用
private final GroovyFormatter formatter = new GroovyFormatter();
```

### 3. アーキテクチャテスト

```java
@ArchTest
static final ArchRule internalPackagesShouldNotBeAccessedFromOutside =
    noClasses()
        .that().resideOutsideOfPackage("..internal..")
        .should().accessClassesThat()
        .resideInAPackage("..internal..");
```

## カバレッジ目標

### モジュール別目標

| モジュール | ライン | ブランチ | 特記事項 |
|-----------|-------|---------|------|
| shared | 80% | 80% | 基盤コード |
| groovy-core | 80% | 80% | コア機能 |
| lsp-protocol | 80% | 80% | プロトコル処理 |
| workspace-index | 80% | 80% | インデックス管理 |
| codenarc-lint | 80% | 80% | 静的解析 |
| formatting | 80% | 80% | フォーマッター |
| server-launcher | 80% | 80% | 起動処理 |

### 除外対象

- Main/Launcherクラス
- 定数定義クラス
- DIモジュール設定
- 自動生成コード

## トラブルシューティング

### よくある問題

1. **OutOfMemoryError during tests**
   ```bash
   # JVMヒープサイズを増やす
   ./gradlew test -Dorg.gradle.jvmargs="-Xmx2g -XX:+HeapDumpOnOutOfMemoryError"
   ```

2. **テストの並列実行による競合**
   ```groovy
   test {
       maxParallelForks = 1  // 並列実行を無効化
   }
   ```

3. **Windows環境でのパス問題**
   - テストでは`Path`クラスを使用してOSに依存しないパス処理を実装

4. **LMDBのロック問題**
   - 各テストで独立した一時ディレクトリを使用
   - `@TempDir`アノテーションの活用

## デバッグテクニック

### 1. LSPメッセージのトレース

```java
// テスト時のLSPメッセージをログ出力
@Test
void debugProtocolMessages() {
    var server = new ProtocolMockServer();
    server.enableTracing();  // すべてのJSON-RPCメッセージをログ出力
    // テスト実行
}
```

### 2. 診断結果の詳細確認

```java
// 診断結果を見やすく出力
assertThat(diagnostics)
    .extracting(d -> d.getMessage())
    .containsExactly(
        "Missing semicolon",
        "Undefined variable: foo"
    );
```

### 3. パフォーマンス計測

```java
@Test
@Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
void performanceTest() {
    // 200ms以内に完了することを保証
}
```

## 今後の改善計画

1. **プロパティベーステスト（Property-based Testing）**
   - QuickCheckスタイルのテスト導入
   - ランダムな入力によるエッジケース検出

2. **ミューテーションテスト（Mutation Testing）**
   - PITestの導入検討
   - テストの品質自体を検証

3. **契約テスト（Contract Testing）**
   - LSPクライアントとの互換性保証
   - プロトコルバージョン間の互換性

4. **ビジュアルリグレッションテスト**
   - VS Code拡張機能のUIテスト
   - スクリーンショット比較

## 参考資料

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html)
- [JMH Tutorial](https://github.com/openjdk/jmh)
- [LSP Specification](https://microsoft.github.io/language-server-protocol/)

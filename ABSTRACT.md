# Groovy Tier‑3 Language Server (LSP4J ベース) 概要設計書

## 1. 目的

Groovy に対して IDE 並み (Tier‑3) の開発体験を提供する Language Server を新規実装する。既存の `groovy-language-server` を置き換え、ClassPath 解決と動的型推論を統合してリネーム/シンボル検索まで一気通貫でサポートする。

---

## 2. ゴール (Tier‑3 機能範囲)

| 機能        | 要件                                                |
| --------- | ------------------------------------------------- |
| リアルタイム診断  | 構文/型/データフローエラーを 200 ms 以内に返却                      |
| コード補完     | 型推論結果を用いた候補ランク、snippet 対応                         |
| Hover     | Javadoc／Groovydoc & 推論型の表示                        |
| 定義/参照ジャンプ | 同一 Workspace 100k 行で ≤ 50 ms                      |
| リネーム      | シンボルスコープ整合性 + Undo 安全                             |
| コードアクション  | CodeNarc ルール → Quick Fix 対応                       |
| フォーマッタ    | AST → google‑java‑format 準拠 (Groovy 拡張)           |
| その他       | semanticTokens, documentSymbols, workspaceSymbols |

---

## 3. 技術スタック

| レイヤ   | ライブラリ / バージョン                   | 備考                                                                                                                 |
| ----- | ------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| プロトコル | **LSP4J 0.23.x**                | LSP & DAP 実装 ([github.com](https://github.com/eclipse/lsp4j/releases?utm_source=chatgpt.com))                      |
| コンパイラ | **Apache Groovy 4.0.27**        | Parrot parser + indy backend ([groovy.apache.org](https://groovy.apache.org/download.html?utm_source=chatgpt.com)) |
| 型解決   | **Groovy‑Eclipse JDT Core 5.7** | JDT AST ↔ Groovy AST 変換 ([github.com](https://github.com/groovy/groovy-eclipse/wiki?utm_source=chatgpt.com))       |
| 静的解析  | **CodeNarc 3.x**                | Lint + Quick Fix 300+ ルール                                                                                          |
| ビルド   | Gradle 8 (JPMS モジュール)           | Java 21 でコンパイル                                                                                                     |
| CI    | GitHub Actions (ubuntu‑latest)  | JDK 21 matrix / xvfb                                                                                               |

---

## 4. アーキテクチャ概要

```
┌──────────────────────┐
│        Client        │ (VS Code, Neovim, …)
└────────────┬───────────┘
             │ JSON‑RPC over stdio
┌────────────▼───────────┐
│      LSP4J Launcher    │
└────────────┬───────────┘
             │ ServiceRouter (Guice DI)
┌────────────▼───────────┐     ┌────────────────────┐
│ ProtocolAdapterLayer   │◀───▶│  WorkspaceIndexer   │
└────────────┬───────────┘     └────────────────────┘
             │                   ▲
┌────────────▼───────────┐     ┌┴───────────────────┐
│  AnalysisService       │◀───▶│  DependencyCache    │
│  (Groovy + JDT)        │     └────────────────────┘
└────────────┬───────────┘
             │
┌────────────▼───────────┐
│  LintService (CodeNarc)│
└────────────────────────┘
```

* **ServiceRouter** でスレッドプール実行／キャンセル対応。
* **WorkspaceIndexer** は Gradle Tooling API から classpath を取得し、JAR の Javadoc ジェネリック型情報をキャッシュ。
* **DependencyCache** を共有し、CompilationUnit 間で ClassLoader を再利用。

---

## 5. モジュール構成

| Gradle Project    | 概要                                     |
| ----------------- | -------------------------------------- |
| `server-launcher` | Main クラス / LSP4J Launcher 設定           |
| `lsp-protocol`    | DTO & Adapter 層 (generated)            |
| `groovy-core`     | Groovy Compiler API ラッパー               |
| `jdt-adapter`     | Groovy ↔ JDT 変換ユーティリティ                 |
| `codenarc-lint`   | Lint Engine + QuickFix Mapping         |
| `workspace-index` | Gradle / Maven resolver, symbol インデックス |
| `formatting`      | google‑java‑format ベース Groovy 拡張       |

---

## 6. 主要処理フロー

### 6.1 ファイルオープン

1. `textDocument/didOpen` 受信
2. AnalysisService が **パーセ phase 1 (Parsing)** を実行
3. エラー収集 → `publishDiagnostics`
4. WorkspaceIndexer がシンボルツリーを更新

### 6.2 コード補完

1. `textDocument/completion` リクエスト
2. JDT Core の *CompletionEngine* を呼び出し
3. Dynamic type が未解決なら Groovy 上書き推論ロジックを適用
4. 候補ソート → `CompletionItem` 返却 (< 30 ms target)

### 6.3 リネーム

1. `textDocument/prepareRename` でスコープ解析
2. AST + JDT バインディングに基づき参照箇所を収集
3. 書き換えプラン生成 → `WorkspaceEdit` 返却

---

## 7. キャッシュ & 性能設計

* **フェーズごとの Incremental Compilation** (Groovy AST フェーズ city)
* **Symbol インデックス** は LMDB Embedded DB で永続化し 2 回目起動を高速化
* **Debounce**: 250 ms の typing idle 後に非同期診断

---

## 8. エラー処理

| 種別                 | 方針                                              |
| ------------------ | ----------------------------------------------- |
| Recoverable (タイポ)  | best‑effort 推論で動作継続                             |
| Dependency missing | `window/showMessageRequest` で build.gradle 同期提案 |
| Internal Exception | Sentry 送信 + `server/logMessage` DEBUG           |

---

## 9. 開発・ビルド環境

* **Dev Container**: `eclipse-temurin:21-jdk` + `gradle 8` + `sdkman groovy 4.0.27`
* `./gradlew :server-launcher:run --stdin` でホットリロード実行
* VS Code 拡張 `debug:lsp` 構成でアタッチデバッグ

---

## 10. テスト戦略

| レイヤ   | フレームワーク                        | カバレッジ目標          |
| ----- | ------------------------------ | ---------------- |
| ユニット  | JUnit 5 + MockK                | 80 %             |
| プロトコル | LSP4J TestHarness              | 50+ シナリオ         |
| E2E   | `@vscode/test` + xvfb          | VS Code 最新 + LTS |
| 競合    | JMH ベンチ (completion 10k req/s) | -                |

---

## 11. 配布 & デプロイ

* **Fat JAR** (`shadowJar`) 18 MB 以内を目標
* GraalVM Native Image オプション (startup < 100 ms)
* VS Code Marketplace パッケージ: `groovy-lang.lsp4g`

---

## 12. ロードマップ

| フェーズ | 期間   | マイルストーン                                     |
| ---- | ---- | ------------------------------------------- |
| 0    | 2 週  | Skeleton / Diagnostics Tier‑1               |
| 1    | +4 週 | Completion & Hover Tier‑2                   |
| 2    | +6 週 | Definition, References, Rename              |
| 3    | +6 週 | Full CodeAction, Formatting, semanticTokens |
| 4    | 継続   | Performance hardening / DAP 対応              |

---

## 13. 参照資料

1. LSP4J 0.23.1 Release Notes
2. Groovy 4.0.27 Downloads
3. Groovy‑Eclipse 5.7.0 Release Notes

## 🎯 全体像（Before → After）

| 項目          | 旧構成                   | モダン構成                                                                                                                         |
| ----------- | --------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| **ランタイム**   | Node 20 / Java 17     | **Node 22 LTS**（組み込み test runner & Maglev JIT）([Node.js][1]) / **Bun 1.x** はユーティリティ実行に採用                                      |
| **パッケージ管理** | npm                   | **pnpm 9** – monorepo を高速解決                                                                                                   |
| **単体テスト**   | ts-lsp-client + Mocha | **ts-lsp-client** ([GitHub][2]) + **Node built-in test runner** (watch/coverage) ([Node.js][3]) or **Vitest 3** forフロント寄りロジック |
| **E2E テスト** | @vscode/test          | そのまま継続 （VS Code v1.96 の API に追随）                                                                                              |
| **多言語 LSP** | 手作りスクリプト              | **pytest-lsp** & **lsp-devtools** で統一 ([PyPI][4], [GitHub][5])                                                                |
| **開発環境**    | 手動セットアップ              | **Dev Containers spec 1.0** + Codespaces 対応 ([Dev Containers][6])                                                             |
| **CI**      | GitHub Actions 単一ジョブ  | **matrix**（Node 22 / Bun 1 / JDK 21）+ キャッシュ & コンテナ再利用                                                                         |

---

## 1. リポジトリレイアウト

```
root/
├─ packages/
│  ├─ server/            # LSP サーバー本体
│  ├─ protocol-tests/    # JSON-RPC レベル
│  └─ e2e/               # VS Code Extension Host 絡み
├─ .devcontainer/        # VS Code & Codespaces
├─ docker/               # 本番用イメージ
└─ .github/workflows/
```

Monorepo でも `pnpm filter` でパッケージ横断テストが 1 コマンドに。

---

## 2. Dev Container（.devcontainer/devcontainer.json 抜粋）

```jsonc
{
  "name": "lsp-dev",
  "image": "mcr.microsoft.com/devcontainers/typescript-node:1-22-bookworm",
  "features": {
    "ghcr.io/devcontainers/features/java": "21"
  },
  "customizations": {
    "vscode": {
      "settings": {
        "typescript.tsserver.useNodeNext": true
      },
      "extensions": [
        "ms-vscode.vscode-typescript-next",
        "github.vscode-pull-request-github"
      ]
    }
  },
  "postCreateCommand": "pnpm install && pnpm build"
}
```

*Node 22 イメージなので組み込み test runner が即利用可。
Java 21 は LSP4J 系ビルドに備えて feature で注入。*

---

## 3. プロトコル単体テスト（Node 側）

```ts
// packages/protocol-tests/hover.test.ts
import { startServer, client } from 'ts-lsp-client';
import assert from 'node:assert/strict';

const srv = await startServer({
  command: 'node',
  args: ['dist/server.js', '--stdio']
});

await client.initialize({ rootUri: 'file:///proj' });
const hover = await client.sendRequest('textDocument/hover', {
  textDocument: { uri: 'file:///proj/src/main.foo' },
  position: { line: 10, character: 5 }
});

assert.ok(hover.contents.value.includes('ExpectedDoc'));
await srv.shutdown();
```

```bash
# 実行
node --test --watch packages/protocol-tests
```

*Node 22 の `node:test` は watch / coverage / TAP 出力が標準装備で Mocha 相当の書き味。
CLI の依存がゼロになり CI イメージも軽量化。*

---

## 4. Python／その他ランタイムを巻き込む場合

```python
# tests/test_hover.py
from pytest_lsp import LanguageClient

def test_hover(tmp_path):
    srv = LanguageClient.command(
        ["python", "-m", "my_py_ls", "--stdio"], root_uri=tmp_path.as_uri()
    )
    with srv:
        doc = srv.open("main.py", "print(1)")
        hover = srv.hover(doc, position=(0, 1))
        assert "int" in hover["contents"][0]["value"]
```

`pytest -q` だけで Node と同等の E2E が回ります。
`lsp-devtools` の *capability index* を使うと「このメッセージを VS Code が本当にサポートしているか？」の表引きも自動化できます。([LSP Devtools][7])

---

## 5. VS Code Extension Host E2E

```ts
import { runTests } from '@vscode/test';

await runTests({
  version: 'stable',
  extensionDevelopmentPath: resolve('../server'),
  extensionTestsPath: resolve('./suite/index'),
  launchArgs: ['--disable-workspace-trust']
});
```

*GUI は起動しますが xvfb-run でヘッドレス実行 OK。
GitHub Actions では ubuntu-latest + `xvfb-action@v2` が楽。*

---

## 6. GitHub Actions（.github/workflows/ci.yml 抜粋）

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        runtime: [node22, bun, temurin-21]
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v3
        with: { version: 9 }
      - name: Set up ${{ matrix.runtime }}
        uses: ./.github/actions/setup-${{ matrix.runtime }}
      - run: pnpm -r test
```

*ローカルと同じ devcontainer を Docker ビルドキャッシュとして再利用することで
`pnpm install` は 80 % 以上ヒット、一周 2 〜 3 分に短縮。*

---

## 7. 追加で押さえておきたい一次情報

| 資料                           | 概要                                                                              |
| ---------------------------- | ------------------------------------------------------------------------------- |
| **LSP Spec 3.18 (Draft)**    | 2025-H1 のエディタ実装が追随中。`textDocument/diagnostic` の完全版が安定予定。([Microsoft GitHub][8]) |
| **ts-lsp-client README**     | VS Code 依存ゼロのスタンドアロン LSP クライアント実装。([GitHub][2])                                 |
| **pytest-lsp Docs**          | PyGLS ベースのサーバーをワンライナーでテスト。([PyPI][4])                                           |
| **Node.js test runner Docs** | `node --test --watch` / coverage / TAP 出力。([Node.js][3])                        |

---

### ✨ Tips

* **Bun の test runner** は Vitest API 互換なので、Node 側と同じテストコードがそのまま動きます。高速フィードバック用にローカルでは `bun test`, CI で `node --test` など切り替えるのもアリ。([Bun][9])
* **LSP メッセージ** を JSON Schema で定義しておくと、実行時バリデーションを Node/Python 両方で共通化でき、破壊的変更の回避に◎。
* VS Code 用 E2E を最初から **Playwright** に寄せておくと、スクリーンショット付きレポートが得られデバッグが楽（@playwright/test は Extension Host 起動をサポート済み）。

---

これで “エディタ不使用でも再現性の高い LSP テスト” を **ローカル → Dev Container → CI** へ一気通貫で流せます。
試してみて詰まりどころがあったら、また気軽に声かけてくださいね 😊

[1]: https://nodejs.org/en/blog/announcements/v22-release-announce?utm_source=chatgpt.com "Node.js 22 is now available!"
[2]: https://github.com/ImperiumMaximus/ts-lsp-client?utm_source=chatgpt.com "ImperiumMaximus/ts-lsp-client: lsp language-server-protocol nodejs ..."
[3]: https://nodejs.org/api/test.html?utm_source=chatgpt.com "Test runner | Node.js v24.1.0 Documentation"
[4]: https://pypi.org/project/pytest-lsp/?utm_source=chatgpt.com "pytest-lsp · PyPI"
[5]: https://github.com/swyddfa/lsp-devtools?utm_source=chatgpt.com "swyddfa/lsp-devtools: Tooling for working with language ... - GitHub"
[6]: https://devcontainers.github.io/implementors/json_reference/?utm_source=chatgpt.com "Dev Container metadata reference"
[7]: https://lsp-devtools.readthedocs.io/?utm_source=chatgpt.com "LSP Devtools"
[8]: https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/?utm_source=chatgpt.com "Language Server Protocol Specification - 3.18"
[9]: https://bun.sh/?utm_source=chatgpt.com "Bun — A fast all-in-one JavaScript runtime"

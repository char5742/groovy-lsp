#!/bin/bash

echo "Testing Language Server directly..."
echo "Workspace: /home/shuma/workspace/programing/java/groovy-lsp/packages/e2e-tests/test/fixtures/cross-file-project"

# Create a simple initialize request
cat << 'EOF' | /usr/lib/jvm/java-23-amazon-corretto/bin/java -jar /home/shuma/workspace/programing/java/groovy-lsp/packages/vscode-extension/server/groovy-language-server.jar --workspace /home/shuma/workspace/programing/java/groovy-lsp/packages/e2e-tests/test/fixtures/cross-file-project 2>&1
Content-Length: 259

{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"processId":null,"rootUri":"file:///home/shuma/workspace/programing/java/groovy-lsp/packages/e2e-tests/test/fixtures/cross-file-project","capabilities":{"textDocument":{"completion":{"completionItem":{"snippetSupport":true}}}}}}
EOF
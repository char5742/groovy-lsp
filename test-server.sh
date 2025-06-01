#!/bin/bash

# Test script to interact with the Groovy Language Server
# This script sends LSP initialize request to the server via stdio

# Create a test request
cat << 'EOF' | java -jar packages/server-launcher/build/libs/groovy-language-server-0.1.0-SNAPSHOT.jar
Content-Length: 252

{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"processId":null,"clientInfo":{"name":"Test Client"},"rootPath":null,"rootUri":"file:///home/shuma/workspace/programing/java/groovy-lsp","initializationOptions":{},"capabilities":{}}}
EOF
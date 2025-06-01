#!/bin/bash
# Conventional Commits format checker

commit_regex='^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\(.+\))?: .{1,}$'
commit_msg_file="$1"

if [ -z "$commit_msg_file" ]; then
    echo "❌ コミットメッセージファイルが指定されていません!"
    exit 1
fi

if [ ! -f "$commit_msg_file" ]; then
    echo "❌ コミットメッセージファイルが見つかりません: $commit_msg_file"
    exit 1
fi

first_line=$(head -n 1 "$commit_msg_file")

if ! echo "$first_line" | grep -qE "$commit_regex"; then
    echo "❌ コミットメッセージがConventional Commits形式に従っていません!"
    echo "形式: <type>(<scope>): <subject>"
    echo "例: feat(groovy-core): Add incremental compilation support"
    echo "実際のメッセージ: $first_line"
    exit 1
fi

echo "✅ コミットメッセージが正しい形式です: $first_line"
# 静的解析ツール設定ガイド

本プロジェクトでは、コード品質とNull安全性を向上させるため、以下の静的解析ツールを使用しています：

- **Error Prone** - Googleが開発した静的解析ツール
- **NullAway** - Uberが開発したNull安全性チェックツール（Error Proneプラグイン）
- **jSpecify** - Null安全性のための標準アノテーション

## 必要条件

### Java バージョン

- **推奨**: Java 21（LTS）
- **サポート**: Java 17-21
- **注意**: Java 22以降ではError Proneが互換性の問題により自動的に無効化されます

### 現在の設定

```groovy
// gradle/libs.versions.toml
errorprone-core = "2.26.1"
nullaway = "0.11.3"
jspecify = "1.0.0"
```

## 設定内容

### 1. Error Prone

Error Proneは以下の設定で有効化されています：

```groovy
tasks.withType(JavaCompile) {
    options.errorprone {
        enabled = true
        disableWarningsInGeneratedCode = true
        
        // Groovyとの相性を考慮して無効化
        disable("UnusedVariable")
        disable("StringSplitter")
    }
}
```

### 2. NullAway

NullAwayはjSpecifyの`@NullMarked`アノテーションと連携して動作します：

```groovy
options.errorprone {
    check("NullAway", CheckSeverity.ERROR)
    option("NullAway:AnnotatedPackages", "com.groovy.lsp")
    option("NullAway:OnlyNullMarked", "true")  // @NullMarkedパッケージのみチェック
    option("NullAway:TreatGeneratedAsUnannotated", "true")
    option("NullAway:ExcludedFieldAnnotations", "org.mockito.Mock,org.mockito.Spy")
}
```

### 3. jSpecify統合

各パッケージのpackage-info.javaファイルに`@NullMarked`を適用：

```java
@org.jspecify.annotations.NullMarked
package com.groovy.lsp.shared;
```

これにより、パッケージ内の全ての型がデフォルトで非nullとなります。

## 使用方法

### 1. Null許容の明示

nullを返す可能性のあるメソッドや、nullを受け入れるパラメータには`@Nullable`を使用：

```java
@Nullable
public String findValue(String key) {
    return map.get(key);  // nullを返す可能性がある
}

public void process(@Nullable String input) {
    if (input != null) {
        // null チェックが必要
    }
}
```

### 2. 修飾型への適用

修飾型（例：ネストされた型）には特別な構文を使用：

```java
// 正しい使用法
private java.lang.reflect.@Nullable Method findSetter(Class<?> clazz, String name) {
    // ...
}

// 間違った使用法（コンパイルエラー）
@Nullable
private java.lang.reflect.Method findSetter(Class<?> clazz, String name) {
    // ...
}
```

## ビルドとテスト

### Java 21でのビルド（推奨）

```bash
# Java 21環境でError ProneとNullAwayが有効
./gradlew clean build
```

### Java 23でのビルド

```bash
# Error Proneは自動的に無効化されますが、ビルドは成功します
./gradlew clean build
```

警告メッセージが表示されます：
```
Error Prone is disabled for Java 23 due to compatibility issues. 
Consider using Java 21 for full static analysis support.
```

## トラブルシューティング

### 1. Error Prone関連のエラー

Java 22以降で以下のようなエラーが発生する場合：
```
java.lang.NoSuchFieldError: Class com.sun.tools.javac.parser.Tokens$Comment$CommentStyle
```

**解決策**: Java 21を使用するか、Error Proneが無効化されていることを確認してください。

### 2. NullAwayのfalse positive

動的に生成されるコードや、モックオブジェクトでfalse positiveが発生する場合：

**解決策**: 
- `@SuppressWarnings("NullAway")`を使用
- 設定で除外：`option("NullAway:ExcludedClasses", "...")`

### 3. Groovyコードでの問題

GroovyコードではError Proneが自動的に無効化されるため、静的解析は行われません。

## ベストプラクティス

1. **新規コードは必ず@NullMarkedパッケージに配置**
2. **publicメソッドには適切な@Nullableアノテーションを付与**
3. **テストコードでは必要に応じて@SuppressWarningsを使用**
4. **CI/CDではJava 21を使用して完全な静的解析を実行**

## 参考資料

- [Error Prone公式ドキュメント](https://errorprone.info/)
- [NullAway GitHub](https://github.com/uber/NullAway)
- [jSpecify公式サイト](https://jspecify.dev/)
- [gradle-errorprone-plugin](https://github.com/tbroyer/gradle-errorprone-plugin)
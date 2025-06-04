package com.example

class ErrorExample {
    // Syntax error: missing closing brace
    def brokenMethod() {
        if (true) {
            println "This won't compile"
        // Missing closing brace
    }

    // Type error: wrong return type
    String returnNumber() {
        return 42
    }

    // Reference error: undefined variable
    def useUndefinedVariable() {
        println undefinedVar
    }

    // Method not found
    def callNonExistentMethod() {
        "string".nonExistentMethod()
    }
}

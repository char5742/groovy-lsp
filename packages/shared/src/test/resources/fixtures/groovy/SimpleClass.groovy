package com.example

class SimpleClass {
    String name
    int age

    SimpleClass(String name, int age) {
        this.name = name
        this.age = age
    }

    String greet() {
        return "Hello, my name is $name and I am $age years old"
    }

    void birthday() {
        age++
    }
}

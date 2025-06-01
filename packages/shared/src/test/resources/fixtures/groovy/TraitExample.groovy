package com.example

trait Flyable {
    abstract String getName()

    String fly() {
        "I'm ${getName()} and I can fly!"
    }

    int altitude = 1000
}

trait Swimmable {
    String swim() {
        "I can swim!"
    }

    int depth = 10
}

class Duck implements Flyable, Swimmable {
    String name = "Donald"

    String getName() {
        return name
    }

    void quack() {
        println "Quack!"
    }
}

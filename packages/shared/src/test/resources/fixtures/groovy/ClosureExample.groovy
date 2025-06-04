package com.example

class ClosureExample {
    def numbers = [1, 2, 3, 4, 5]

    def doubleNumbers() {
        numbers.collect { it * 2 }
    }

    def filterEven() {
        numbers.findAll { it % 2 == 0 }
    }

    def sumWithReduce() {
        numbers.inject(0) { sum, num -> sum + num }
    }

    def withCustomClosure(Closure operation) {
        numbers.collect(operation)
    }

    def methodWithDefaultParams(String name = "World", int times = 1) {
        times.times {
            println "Hello, $name!"
        }
    }
}

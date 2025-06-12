package com.example

import spock.lang.Specification
import spock.lang.Unroll

class CalculatorSpec extends Specification {

    def calculator = new Calculator()

    def "should add two numbers correctly"() {
        given:
        int a = 5
        int b = 3

        when:
        int result = calculator.add(a, b)

        then:
        result == 8
    }

    @Unroll
    def "should calculate #a + #b = #expected"() {
        expect:
        calculator.add(a, b) == expected

        where:
        a | b | expected
        1 | 1 | 2
        2 | 3 | 5
        0 | 0 | 0
        -1| 1 | 0
    }

    def "should throw exception when dividing by zero"() {
        when:
        calculator.divide(10, 0)

        then:
        thrown(IllegalArgumentException)
    }
}

package com.example

interface Calculator {
    int add(int a, int b)
    int subtract(int a, int b)

    default int multiply(int a, int b) {
        return a * b
    }
}

interface ScientificCalculator extends Calculator {
    double sqrt(double value)
    double pow(double base, double exponent)
}

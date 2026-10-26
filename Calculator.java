/**
 * Task 09: Calculator Implementation (Class Under Test)
 * Provides fundamental and advanced mathematical operations with robust validation.
 */
public class Calculator {

    private double memory = 0.0;

    // Basic Arithmetic Operations
    public double add(double a, double b) {
        return a + b;
    }

    public double subtract(double a, double b) {
        return a - b;
    }

    public double multiply(double a, double b) {
        return a * b;
    }

    public double divide(double a, double b) {
        if (Math.abs(b) < 1e-9) {
            throw new ArithmeticException("Division by zero is undefined.");
        }
        return a / b;
    }

    // Advanced Mathematical Functions
    public double power(double base, double exponent) {
        return Math.pow(base, exponent);
    }

    public double squareRoot(double number) {
        if (number < 0) {
            throw new IllegalArgumentException("Cannot compute square root of a negative number: " + number);
        }
        return Math.sqrt(number);
    }

    public long factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Factorial is undefined for negative integers: " + n);
        }
        if (n > 20) {
            throw new ArithmeticException("Factorial overflow for input > 20");
        }
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    public double modulus(double a, double b) {
        if (Math.abs(b) < 1e-9) {
            throw new ArithmeticException("Modulo by zero is undefined.");
        }
        return a % b;
    }

    // Memory Register Operations
    public void memoryAdd(double value) {
        this.memory += value;
    }

    public void memoryClear() {
        this.memory = 0.0;
    }

    public double memoryRecall() {
        return this.memory;
    }
}

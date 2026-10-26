import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Task 09: Calculator Unit Test Suite (JUnit Architecture)
 * Contains comprehensive assertions, lifecycle hooks (@BeforeEach),
 * boundary tests, and exception handling tests.
 */
public class CalculatorTest {

    // Custom Annotation declarations mirroring JUnit 5
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Test {
        String description() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface BeforeEach {}

    // Assertion Framework
    public static class Assertions {
        public static void assertEquals(double expected, double actual, double delta, String message) {
            if (Math.abs(expected - actual) > delta) {
                throw new AssertionError(String.format("%s [Expected: %.4f, but found: %.4f]", message, expected, actual));
            }
        }

        public static void assertEquals(long expected, long actual, String message) {
            if (expected != actual) {
                throw new AssertionError(String.format("%s [Expected: %d, but found: %d]", message, expected, actual));
            }
        }

        public static void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError("Assertion Failed: " + message);
            }
        }

        public static <T extends Throwable> void assertThrows(Class<T> expectedType, Executable executable, String message) {
            try {
                executable.execute();
            } catch (Throwable actualThrown) {
                if (expectedType.isInstance(actualThrown)) {
                    return; // Test Passed
                }
                throw new AssertionError(String.format("%s: Expected %s but caught %s",
                        message, expectedType.getSimpleName(), actualThrown.getClass().getSimpleName()));
            }
            throw new AssertionError(message + ": Expected " + expectedType.getSimpleName() + " was not thrown.");
        }
    }

    @FunctionalInterface
    public interface Executable {
        void execute() throws Throwable;
    }

    // Fixture Under Test
    private Calculator calc;

    @BeforeEach
    public void setUp() {
        calc = new Calculator();
    }

    // --- Test Cases ---

    @Test(description = "Verify addition with positive and negative floating point values")
    public void testAddition() {
        Assertions.assertEquals(15.5, calc.add(10.2, 5.3), 0.0001, "Addition of two positive numbers failed");
        Assertions.assertEquals(-2.0, calc.add(-5.5, 3.5), 0.0001, "Addition with negative operand failed");
        Assertions.assertEquals(0.0, calc.add(0.0, 0.0), 0.0001, "Addition of zeros failed");
    }

    @Test(description = "Verify subtraction logic including sign inversions")
    public void testSubtraction() {
        Assertions.assertEquals(25.0, calc.subtract(50.0, 25.0), 0.0001, "Standard subtraction failed");
        Assertions.assertEquals(-15.0, calc.subtract(10.0, 25.0), 0.0001, "Subtraction yielding negative failed");
    }

    @Test(description = "Verify multiplication across zero and negative scalars")
    public void testMultiplication() {
        Assertions.assertEquals(42.0, calc.multiply(6.0, 7.0), 0.0001, "Multiplication failed");
        Assertions.assertEquals(0.0, calc.multiply(55.0, 0.0), 0.0001, "Multiplication with zero failed");
        Assertions.assertEquals(-30.0, calc.multiply(5.0, -6.0), 0.0001, "Signed multiplication failed");
    }

    @Test(description = "Verify precision division")
    public void testDivisionValid() {
        Assertions.assertEquals(2.5, calc.divide(5.0, 2.0), 0.0001, "Floating division failed");
        Assertions.assertEquals(0.3333, calc.divide(1.0, 3.0), 0.001, "Fractional division failed");
    }

    @Test(description = "Verify division by zero throws ArithmeticException")
    public void testDivisionByZeroThrowsException() {
        Assertions.assertThrows(ArithmeticException.class, () -> calc.divide(10.0, 0.0),
                "Division by zero must throw ArithmeticException");
    }

    @Test(description = "Verify exponentiation including zero and fractional powers")
    public void testPower() {
        Assertions.assertEquals(8.0, calc.power(2.0, 3.0), 0.0001, "Integer power calculation failed");
        Assertions.assertEquals(1.0, calc.power(5.0, 0.0), 0.0001, "Power to exponent zero failed");
        Assertions.assertEquals(0.25, calc.power(2.0, -2.0), 0.0001, "Negative exponent power failed");
    }

    @Test(description = "Verify square root calculation for valid positive values")
    public void testSquareRootValid() {
        Assertions.assertEquals(5.0, calc.squareRoot(25.0), 0.0001, "Square root failed");
        Assertions.assertEquals(0.0, calc.squareRoot(0.0), 0.0001, "Square root of zero failed");
    }

    @Test(description = "Verify square root of negative input throws IllegalArgumentException")
    public void testSquareRootNegativeThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> calc.squareRoot(-9.0),
                "Square root of negative number must throw IllegalArgumentException");
    }

    @Test(description = "Verify factorial boundary conditions (0!, 1!, 5!, 10!)")
    public void testFactorial() {
        Assertions.assertEquals(1, calc.factorial(0), "0! must equal 1");
        Assertions.assertEquals(1, calc.factorial(1), "1! must equal 1");
        Assertions.assertEquals(120, calc.factorial(5), "5! must equal 120");
        Assertions.assertEquals(3628800, calc.factorial(10), "10! must equal 3,628,800");
    }

    @Test(description = "Verify factorial with negative input throws IllegalArgumentException")
    public void testFactorialNegativeThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> calc.factorial(-3),
                "Factorial of negative input must throw IllegalArgumentException");
    }

    @Test(description = "Verify memory accumulator operations (add, recall, clear)")
    public void testMemoryRegister() {
        calc.memoryAdd(100.50);
        calc.memoryAdd(49.50);
        Assertions.assertEquals(150.0, calc.memoryRecall(), 0.0001, "Memory recall mismatch after additions");

        calc.memoryClear();
        Assertions.assertEquals(0.0, calc.memoryRecall(), 0.0001, "Memory clear did not reset to zero");
    }
}

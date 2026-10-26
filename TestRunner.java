import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Task 09: TestRunner
 * Standalone JUnit Execution Engine that discovers and runs annotated test methods.
 */
public class TestRunner {

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("            JUNIT UNIT TESTING ENGINE - CALCULATOR TEST SUITE                            ");
        System.out.println("=========================================================================================");

        Class<?> testClass = CalculatorTest.class;
        Method beforeEachMethod = null;
        List<Method> testMethods = new ArrayList<>();

        for (Method method : testClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(CalculatorTest.BeforeEach.class)) {
                beforeEachMethod = method;
            }
            if (method.isAnnotationPresent(CalculatorTest.Test.class)) {
                testMethods.add(method);
            }
        }

        System.out.printf("Test Suite: %s | Discovered %d Test Cases%n%n", testClass.getSimpleName(), testMethods.size());

        int passed = 0;
        int failed = 0;
        long suiteStartTime = System.nanoTime();

        for (int i = 0; i < testMethods.size(); i++) {
            Method testMethod = testMethods.get(i);
            CalculatorTest.Test testAnnotation = testMethod.getAnnotation(CalculatorTest.Test.class);
            String desc = testAnnotation.description().isEmpty() ? testMethod.getName() : testAnnotation.description();

            long start = System.nanoTime();
            boolean success = false;
            String failureReason = "";

            try {
                Object instance = testClass.getDeclaredConstructor().newInstance();
                if (beforeEachMethod != null) {
                    beforeEachMethod.invoke(instance);
                }
                testMethod.invoke(instance);
                success = true;
                passed++;
            } catch (Throwable t) {
                failed++;
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                failureReason = cause.getMessage();
            }

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            String status = success ? "[PASS]" : "[FAIL]";

            System.out.printf("  %-7s #%02d: %-38s (%2d ms)%n", status, (i + 1), testMethod.getName(), elapsedMs);
            System.out.printf("          Description: %s%n", desc);
            if (!success) {
                System.out.printf("          FAILURE REASON: %s%n", failureReason);
            }
            System.out.println();
        }

        long totalDurationMs = (System.nanoTime() - suiteStartTime) / 1_000_000;

        System.out.println("=========================================================================================");
        System.out.println("                               TEST EXECUTION SUMMARY                                    ");
        System.out.println("=========================================================================================");
        System.out.printf("  Total Tests Executed : %d%n", (passed + failed));
        System.out.printf("  Passed Tests         : %d%n", passed);
        System.out.printf("  Failed Tests         : %d%n", failed);
        System.out.printf("  Execution Time       : %d ms%n", totalDurationMs);
        System.out.printf("  Final Test Verdict   : %s%n", failed == 0 ? "ALL TESTS PASSED (100% SUCCESS)" : "FAILURES DETECTED");
        System.out.println("=========================================================================================");
    }
}

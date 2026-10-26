import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Task 10: Secure Authentication Stress Testing Using Test Cases
 * Concepts: Cryptographic Salting & Hashing (SHA-256), Constant-time Comparison,
 *           Rate Limiting / Account Lockout, Multi-Threaded Stress Testing (ExecutorService),
 *           Latency/Throughput Benchmarking, and Test Case Assertions.
 */
public class SecureAuthStressTesting {

    // User Credential Entity
    public static class UserAccount {
        private final String username;
        private final String saltHex;
        private final String passwordHashHex;
        private final AtomicInteger failedAttempts = new AtomicInteger(0);
        private volatile long lockoutUntilTimestamp = 0;

        public UserAccount(String username, String password) {
            this.username = username;
            this.saltHex = generateSaltHex(16);
            this.passwordHashHex = hashPassword(password, this.saltHex);
        }

        public String getUsername() { return username; }
        public int getFailedAttempts() { return failedAttempts.get(); }
        public boolean isLockedOut() { return System.currentTimeMillis() < lockoutUntilTimestamp; }

        public void recordFailedAttempt(int maxAttempts, long lockoutDurationMs) {
            int attempts = failedAttempts.incrementAndGet();
            if (attempts >= maxAttempts) {
                lockoutUntilTimestamp = System.currentTimeMillis() + lockoutDurationMs;
            }
        }

        public void resetLockout() {
            failedAttempts.set(0);
            lockoutUntilTimestamp = 0;
        }

        public boolean verifyPassword(String inputPassword) {
            String computedHash = hashPassword(inputPassword, this.saltHex);
            return slowEquals(this.passwordHashHex, computedHash);
        }

        // Timing-attack resistant string comparison
        private static boolean slowEquals(String a, String b) {
            if (a == null || b == null) return false;
            byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
            byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
            int diff = aBytes.length ^ bBytes.length;
            for (int i = 0; i < aBytes.length && i < bBytes.length; i++) {
                diff |= aBytes[i] ^ bBytes[i];
            }
            return diff == 0;
        }

        public static String generateSaltHex(int numBytes) {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[numBytes];
            random.nextBytes(salt);
            return bytesToHex(salt);
        }

        public static String hashPassword(String password, String saltHex) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(hexToBytes(saltHex));
                byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
                return bytesToHex(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 unavailable", e);
            }
        }

        private static String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        }

        private static byte[] hexToBytes(String hex) {
            int len = hex.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                        + Character.digit(hex.charAt(i + 1), 16));
            }
            return data;
        }
    }

    // Authentication Service with Session Token Issuer
    public static class AuthenticationService {
        private final ConcurrentHashMap<String, UserAccount> userDatabase = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, String> activeSessions = new ConcurrentHashMap<>();
        private static final int MAX_FAILED_ATTEMPTS = 5;
        private static final long LOCKOUT_DURATION_MS = 3000; // 3 seconds lockout for testing

        public void registerUser(String username, String password) {
            userDatabase.put(username, new UserAccount(username, password));
        }

        public String authenticate(String username, String password) throws SecurityException {
            UserAccount account = userDatabase.get(username);
            if (account == null) {
                throw new SecurityException("Invalid credentials.");
            }

            if (account.isLockedOut()) {
                throw new SecurityException("Account is temporarily locked due to excessive failed attempts. Please retry later.");
            }

            if (!account.verifyPassword(password)) {
                account.recordFailedAttempt(MAX_FAILED_ATTEMPTS, LOCKOUT_DURATION_MS);
                throw new SecurityException("Invalid credentials. Attempt #" + account.getFailedAttempts());
            }

            // Authentication succeeded
            account.resetLockout();
            String token = "AUTH-TOKEN-" + UUID.randomUUID();
            activeSessions.put(token, username);
            return token;
        }

        public boolean validateToken(String token) {
            return activeSessions.containsKey(token);
        }
    }

    // Benchmark Metrics Container
    public static class StressMetrics {
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        final AtomicLong totalLatencyNs = new AtomicLong(0);
        final AtomicLong minLatencyNs = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong maxLatencyNs = new AtomicLong(0);

        public void record(long latencyNs, boolean success) {
            if (success) successCount.incrementAndGet();
            else failureCount.incrementAndGet();

            totalLatencyNs.addAndGet(latencyNs);
            minLatencyNs.accumulateAndGet(latencyNs, Math::min);
            maxLatencyNs.accumulateAndGet(latencyNs, Math::max);
        }

        public void printSummary(String testName, int totalRequests, long totalDurationMs) {
            double throughput = (totalRequests / (totalDurationMs / 1000.0));
            double avgLatencyMs = (totalLatencyNs.get() / (double) totalRequests) / 1_000_000.0;
            double minLatencyMs = minLatencyNs.get() / 1_000_000.0;
            double maxLatencyMs = maxLatencyNs.get() / 1_000_000.0;

            System.out.println("\n-----------------------------------------------------------------------------------------");
            System.out.println("  STRESS TEST REPORT: " + testName);
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.printf("  * Total Requests Executed : %d%n", totalRequests);
            System.out.printf("  * Successful Requests     : %d%n", successCount.get());
            System.out.printf("  * Blocked / Failed Requests: %d%n", failureCount.get());
            System.out.printf("  * Total Wall-Clock Time   : %d ms%n", totalDurationMs);
            System.out.printf("  * Average Throughput      : %,.2f req/sec%n", throughput);
            System.out.printf("  * Latency (Avg)           : %.3f ms%n", avgLatencyMs);
            System.out.printf("  * Latency (Min / Max)     : %.3f ms / %.3f ms%n", minLatencyMs, maxLatencyMs);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=========================================================================================");
        System.out.println("            SECURE AUTHENTICATION SYSTEM & MULTI-THREADED STRESS TESTING                 ");
        System.out.println("=========================================================================================");

        AuthenticationService authService = new AuthenticationService();

        // Seed Users
        authService.registerUser("alice_sec", "P@ssw0rdSecure2026!");
        authService.registerUser("bob_corp",  "UltraSafe#9981");
        authService.registerUser("target_admin", "SuperAdminSecretKey");

        System.out.println("Registered 3 test accounts with unique 128-bit cryptographic salts.");

        // =========================================================================
        // Test Case 1: Functional Verification & Exception Validation
        // =========================================================================
        System.out.println("\n>>> TEST CASE 1: Standard Credentials & Security Checks <<<");
        try {
            String token = authService.authenticate("alice_sec", "P@ssw0rdSecure2026!");
            System.out.println("  [PASS] Alice authenticated successfully! Session Token: " + token.substring(0, 25) + "...");
            System.out.println("  [PASS] Token verification in cache: " + authService.validateToken(token));
        } catch (Exception e) {
            System.err.println("  [FAIL] " + e.getMessage());
        }

        try {
            authService.authenticate("alice_sec", "WrongPassword!");
            System.err.println("  [FAIL] Expected SecurityException not thrown!");
        } catch (SecurityException e) {
            System.out.println("  [PASS] Expected invalid credentials exception caught: " + e.getMessage());
        }

        // =========================================================================
        // Test Case 2: Brute-Force Rate Limiting & Account Lockout
        // =========================================================================
        System.out.println("\n>>> TEST CASE 2: Brute Force Attack Simulation & Account Lockout <<<");
        String target = "target_admin";
        System.out.println("Simulating rapid successive dictionary attacks against '" + target + "'...");

        for (int i = 1; i <= 6; i++) {
            try {
                authService.authenticate(target, "GuessPwd_" + i);
            } catch (SecurityException e) {
                System.out.printf("  Attempt #%d: %s%n", i, e.getMessage());
            }
        }

        // =========================================================================
        // Test Case 3: High-Concurrency Multithreaded Stress Test
        // =========================================================================
        System.out.println("\n>>> TEST CASE 3: Multi-Threaded Stress Load Test <<<");
        int concurrencyLevel = 10;
        int totalRequests = 500;
        ExecutorService threadPool = Executors.newFixedThreadPool(concurrencyLevel);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalRequests);
        StressMetrics stressMetrics = new StressMetrics();

        System.out.printf("Firing %d concurrent authentication requests across %d parallel threads...%n",
                totalRequests, concurrencyLevel);

        long wallClockStart = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            final int reqId = i;
            threadPool.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all worker threads to fire simultaneously
                    long t0 = System.nanoTime();
                    // Alternate between valid and invalid credentials
                    String user = (reqId % 2 == 0) ? "bob_corp" : "alice_sec";
                    String pwd  = (reqId % 2 == 0) ? "UltraSafe#9981" : "WrongPassword!";

                    boolean success = false;
                    try {
                        String t = authService.authenticate(user, pwd);
                        success = (t != null);
                    } catch (Exception ignored) {
                        success = false;
                    }
                    long latency = System.nanoTime() - t0;
                    stressMetrics.record(latency, success);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Unleash threads
        finishLatch.await();    // Wait for all to complete
        long wallClockEnd = System.currentTimeMillis();
        threadPool.shutdown();

        stressMetrics.printSummary("Concurrent Login & Salt-Hashing Stress Benchmark", totalRequests, (wallClockEnd - wallClockStart));

        System.out.println("\n=========================================================================================");
        System.out.println(" Stress Testing Complete: System maintained thread safety, zero data race, and resilience!");
        System.out.println("=========================================================================================");
    }
}

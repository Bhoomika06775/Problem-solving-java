import java.util.Arrays;

/**
 * Task 03: Maximum Profit Analyzer Using Kadane's Algorithm
 * Concepts: Dynamic Programming, Kadane's Algorithm, Contiguous Subarray Analysis,
 *           Optimal Investment Window Detection, O(N) Time Optimization.
 */
public class MaxProfitAnalyzer {

    // Result container for maximum subarray analysis
    public static class ProfitAnalysisResult {
        private final double maxProfit;
        private final int startIndex;
        private final int endIndex;
        private final double[] subArray;

        public ProfitAnalysisResult(double maxProfit, int startIndex, int endIndex, double[] fullArray) {
            this.maxProfit = maxProfit;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            if (startIndex >= 0 && endIndex >= startIndex && endIndex < fullArray.length) {
                this.subArray = Arrays.copyOfRange(fullArray, startIndex, endIndex + 1);
            } else {
                this.subArray = new double[0];
            }
        }

        public double getMaxProfit() { return maxProfit; }
        public int getStartIndex() { return startIndex; }
        public int getEndIndex() { return endIndex; }
        public double[] getSubArray() { return subArray; }

        public void printSummary(String label) {
            System.out.println("\n------------------------------------------------------------");
            System.out.println("  " + label);
            System.out.println("------------------------------------------------------------");
            System.out.printf("  * Maximum Cumulative Profit : $%,.2f%n", maxProfit);
            System.out.printf("  * Optimal Trade Window      : Day %d to Day %d (Length: %d days)%n",
                    startIndex + 1, endIndex + 1, (endIndex - startIndex + 1));
            System.out.print("  * Daily Profit Sequence     : [ ");
            for (double v : subArray) {
                System.out.printf("%s$%.2f ", v >= 0 ? "+" : "", v);
            }
            System.out.println("]");
        }
    }

    /**
     * Kadane's Algorithm with start/end indices tracking.
     * Operates in O(N) time and O(1) extra space.
     */
    public static ProfitAnalysisResult findMaxProfitWindow(double[] dailyChanges, boolean verboseTrace) {
        if (dailyChanges == null || dailyChanges.length == 0) {
            return new ProfitAnalysisResult(0, -1, -1, new double[0]);
        }

        double maxSoFar = dailyChanges[0];
        double currentMax = dailyChanges[0];

        int bestStart = 0;
        int bestEnd = 0;
        int tempStart = 0;

        if (verboseTrace) {
            System.out.printf("%-6s | %-12s | %-15s | %-15s | %-15s%n",
                    "Day", "Change ($)", "Current Max ($)", "Max So Far ($)", "Action Taken");
            System.out.println("-----------------------------------------------------------------------------");
            System.out.printf("Day %-2d | %-+10.2f | %-15.2f | %-15.2f | Initialize%n",
                    1, dailyChanges[0], currentMax, maxSoFar);
        }

        for (int i = 1; i < dailyChanges.length; i++) {
            double change = dailyChanges[i];
            String action;

            if (change > currentMax + change) {
                // Starting a new window is better than extending
                currentMax = change;
                tempStart = i;
                action = "Reset Window (Start @ Day " + (i + 1) + ")";
            } else {
                currentMax = currentMax + change;
                action = "Extend Window";
            }

            if (currentMax > maxSoFar) {
                maxSoFar = currentMax;
                bestStart = tempStart;
                bestEnd = i;
                action += " -> New Peak!";
            }

            if (verboseTrace) {
                System.out.printf("Day %-2d | %-+10.2f | %-15.2f | %-15.2f | %s%n",
                        (i + 1), change, currentMax, maxSoFar, action);
            }
        }

        return new ProfitAnalysisResult(maxSoFar, bestStart, bestEnd, dailyChanges);
    }

    /**
     * Convert stock price series to daily differences and apply Kadane's
     */
    public static ProfitAnalysisResult analyzeStockPrices(double[] stockPrices) {
        if (stockPrices == null || stockPrices.length < 2) {
            return new ProfitAnalysisResult(0, -1, -1, new double[0]);
        }

        double[] dailyDifferences = new double[stockPrices.length - 1];
        for (int i = 0; i < stockPrices.length - 1; i++) {
            dailyDifferences[i] = stockPrices[i + 1] - stockPrices[i];
        }

        ProfitAnalysisResult diffResult = findMaxProfitWindow(dailyDifferences, false);
        return diffResult;
    }

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("                     MAXIMUM PROFIT ANALYZER (KADANE'S ALGORITHM)                        ");
        System.out.println("=========================================================================================");

        // Test Scenario 1: Mixed Market Fluctuations (Volatile Trading Week)
        System.out.println("\n>>> Scenario 1: Daily Profit/Loss Analysis with Step-by-Step Kadane Trace <<<");
        double[] dailyPnL = { 250.0, -320.0, 410.0, 180.0, -90.0, 520.0, 310.0, -600.0, 150.0 };

        System.out.println("Input Daily PnL: " + Arrays.toString(dailyPnL));
        System.out.println();
        ProfitAnalysisResult res1 = findMaxProfitWindow(dailyPnL, true);
        res1.printSummary("Optimal Profit Period (Scenario 1)");

        // Test Scenario 2: Severe Bear Market (All Negative Returns)
        System.out.println("\n>>> Scenario 2: Severe Bear Market (Handling All Negative Days) <<<");
        double[] bearMarket = { -450.0, -120.0, -890.0, -60.0, -310.0 };
        System.out.println("Input Bear Market: " + Arrays.toString(bearMarket));
        ProfitAnalysisResult res2 = findMaxProfitWindow(bearMarket, false);
        res2.printSummary("Minimum Loss Single-Day Period (Scenario 2)");

        // Test Scenario 3: Stock Price Daily Ticker Analysis
        System.out.println("\n>>> Scenario 3: Raw Stock Price Feed to Maximum Profit Streak <<<");
        double[] stockPrices = { 100.0, 105.0, 102.0, 115.0, 125.0, 110.0, 130.0, 128.0 };
        System.out.println("Stock Prices over 8 consecutive days: " + Arrays.toString(stockPrices));
        ProfitAnalysisResult res3 = analyzeStockPrices(stockPrices);
        res3.printSummary("Stock Price Difference Optimal Streak (Scenario 3)");

        System.out.println("\n=========================================================================================");
        System.out.println(" Kadane's Algorithm Time Complexity: O(N) | Space Complexity: O(1)");
        System.out.println("=========================================================================================");
    }
}

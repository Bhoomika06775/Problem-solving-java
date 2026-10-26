import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Task 04: Intelligent DNA Pattern Search Using KMP (Knuth-Morris-Pratt) Algorithm
 * Concepts: String Matching, Longest Prefix Suffix (LPS) Array, Linear Time Search O(N+M),
 *           Genomic Nucleotide Analysis (A, C, G, T), Motif Discovery.
 */
public class DnaPatternSearchKMP {

    /**
     * Computes the LPS (Longest Proper Prefix which is also Suffix) array for the given pattern.
     * Time Complexity: O(M) where M is the pattern length.
     */
    public static int[] computeLPSArray(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        int length = 0; // Length of previous longest prefix suffix
        int i = 1;

        lps[0] = 0; // LPS[0] is always 0

        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(length)) {
                length++;
                lps[i] = length;
                i++;
            } else {
                if (length != 0) {
                    length = lps[length - 1]; // Fallback to previous prefix
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        return lps;
    }

    /**
     * KMP Search Algorithm to find all occurrences of pattern in DNA text.
     * Time Complexity: O(N) where N is text length.
     * Total Time: O(N + M)
     */
    public static List<Integer> searchDNA(String text, String pattern) {
        List<Integer> matchIndices = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();

        if (m == 0 || n == 0 || m > n) {
            return matchIndices;
        }

        int[] lps = computeLPSArray(pattern);
        int i = 0; // Index for text
        int j = 0; // Index for pattern

        while (i < n) {
            if (pattern.charAt(j) == text.charAt(i)) {
                i++;
                j++;
            }

            if (j == m) {
                // Pattern match found at index (i - j)
                matchIndices.add(i - j);
                j = lps[j - 1]; // Use LPS to avoid backtracking in text
            } else if (i < n && pattern.charAt(j) != text.charAt(i)) {
                // Mismatch after j matches
                if (j != 0) {
                    j = lps[j - 1]; // Shift pattern using LPS
                } else {
                    i++;
                }
            }
        }
        return matchIndices;
    }

    /**
     * Formatted display of the LPS table
     */
    public static void printLPSTable(String pattern, int[] lps) {
        System.out.println("LPS (Longest Prefix Suffix) Preprocessing Table:");
        System.out.print("  Index:   ");
        for (int i = 0; i < pattern.length(); i++) {
            System.out.printf("%3d ", i);
        }
        System.out.println();

        System.out.print("  Pattern: ");
        for (int i = 0; i < pattern.length(); i++) {
            System.out.printf("%3c ", pattern.charAt(i));
        }
        System.out.println();

        System.out.print("  LPS:     ");
        for (int val : lps) {
            System.out.printf("%3d ", val);
        }
        System.out.println("\n");
    }

    /**
     * Visual alignment of matches in the chromosome strand
     */
    public static void printVisualAlignment(String text, String pattern, List<Integer> matches) {
        System.out.println("Genomic Sequence Alignment:");
        System.out.println("Text:    " + text);

        for (int matchIndex : matches) {
            StringBuilder sb = new StringBuilder();
            sb.append("Match:   ");
            for (int k = 0; k < matchIndex; k++) {
                sb.append(" ");
            }
            sb.append(pattern);
            sb.append(String.format("  (Offset @ Index %d)", matchIndex));
            System.out.println(sb.toString());
        }
    }

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("            INTELLIGENT DNA PATTERN SEARCH USING KMP ALGORITHM                           ");
        System.out.println("=========================================================================================");

        // Test Case 1: Chromosome Strand Search
        String dnaStrand = "AGCTACGCGACAGCTAGCTAGCTAAGCTACGATCGAGCTAGCTA";
        String targetMotif = "AGCTAGCTA";

        System.out.println("\n>>> Genomic Query 1: Overlapping Target Motif Detection <<<");
        System.out.println("DNA Chromosome Text : " + dnaStrand);
        System.out.println("Length of DNA Text  : " + dnaStrand.length() + " base pairs");
        System.out.println("Target Motif Pattern: " + targetMotif);
        System.out.println("Pattern Length (M)  : " + targetMotif.length() + " base pairs\n");

        int[] lps1 = computeLPSArray(targetMotif);
        printLPSTable(targetMotif, lps1);

        List<Integer> matches1 = searchDNA(dnaStrand, targetMotif);

        System.out.printf("Total Occurrences Found: %d%n", matches1.size());
        System.out.println("Matching Start Indices (0-based): " + matches1);
        System.out.println();
        printVisualAlignment(dnaStrand, targetMotif, matches1);

        // Test Case 2: Repetitive Regulatory DNA Sequence
        System.out.println("\n>>> Genomic Query 2: TATA Box & Poly-A Regulatory Repeat Search <<<");
        String promoterRegion = "TATAAAGGGTATATAAGCTTATAAAATATAAAG";
        String tataBox = "TATAAA";

        System.out.println("Promoter Region     : " + promoterRegion);
        System.out.println("Target TATA Motif   : " + tataBox);
        int[] lps2 = computeLPSArray(tataBox);
        printLPSTable(tataBox, lps2);

        List<Integer> matches2 = searchDNA(promoterRegion, tataBox);
        System.out.printf("Total Occurrences Found: %d%n", matches2.size());
        System.out.println("Matching Start Indices: " + matches2);
        printVisualAlignment(promoterRegion, tataBox, matches2);

        // Efficiency Analysis
        System.out.println("\n-----------------------------------------------------------------------------------------");
        System.out.println("Algorithmic Complexity Summary:");
        System.out.println("  * Preprocessing Time (LPS) : O(M)");
        System.out.println("  * Search Phase Time        : O(N)");
        System.out.println("  * Total Time Complexity    : O(N + M) (Optimal linear time, zero text backtracking)");
        System.out.println("  * Auxiliary Space          : O(M) for LPS array");
        System.out.println("=========================================================================================");
    }
}

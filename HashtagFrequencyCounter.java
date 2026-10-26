import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Task 07: Hashtag Frequencies Counter Using Maps
 * Concepts: Java Map Framework (HashMap, LinkedHashMap, TreeMap), Regex Pattern Matching,
 *           Frequency Distribution, Top-K Sorting, and Category Partitioning.
 */
public class HashtagFrequencyCounter {

    // Representation of a Social Media Post
    public static class Post {
        private final String postId;
        private final String author;
        private final String category; // e.g., TECH, ENTERTAINMENT, SPORTS, SCIENCE
        private final String content;

        public Post(String postId, String author, String category, String content) {
            this.postId = postId;
            this.author = author;
            this.category = category;
            this.content = content;
        }

        public String getPostId() { return postId; }
        public String getAuthor() { return author; }
        public String getCategory() { return category; }
        public String getContent() { return content; }
    }

    // Engine for counting and analyzing hashtags
    public static class HashtagAnalyticsEngine {
        // Regex pattern to capture hashtags: starts with # followed by alphanumeric characters or underscores
        private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");

        private final Map<String, Integer> globalFrequencyMap = new HashMap<>();
        private final Map<String, Map<String, Integer>> categoryFrequencyMap = new HashMap<>();
        private int totalPostsProcessed = 0;
        private int totalHashtagsFound = 0;

        /**
         * Extract hashtags from text and normalize to lowercase
         */
        public List<String> extractHashtags(String text) {
            List<String> tags = new ArrayList<>();
            if (text == null) return tags;

            Matcher matcher = HASHTAG_PATTERN.matcher(text);
            while (matcher.find()) {
                tags.add("#" + matcher.group(1).toLowerCase());
            }
            return tags;
        }

        /**
         * Ingest a batch of posts
         */
        public void ingestPosts(List<Post> posts) {
            for (Post post : posts) {
                totalPostsProcessed++;
                List<String> tags = extractHashtags(post.getContent());
                totalHashtagsFound += tags.size();

                for (String tag : tags) {
                    // Update global count using Map.merge
                    globalFrequencyMap.merge(tag, 1, Integer::sum);

                    // Update category-wise count using computeIfAbsent & merge
                    categoryFrequencyMap
                            .computeIfAbsent(post.getCategory(), k -> new HashMap<>())
                            .merge(tag, 1, Integer::sum);
                }
            }
        }

        /**
         * Retrieve top-K trending hashtags sorted by frequency (descending) and name (ascending)
         */
        public List<Map.Entry<String, Integer>> getTopTrending(int k) {
            return globalFrequencyMap.entrySet().stream()
                    .sorted((e1, e2) -> {
                        int cmp = e2.getValue().compareTo(e1.getValue());
                        return cmp != 0 ? cmp : e1.getKey().compareTo(e2.getKey());
                    })
                    .limit(k)
                    .collect(Collectors.toList());
        }

        /**
         * Returns an alphabetically sorted TreeMap view of all hashtags
         */
        public Map<String, Integer> getAlphabeticalView() {
            return new TreeMap<>(globalFrequencyMap);
        }

        /**
         * ASCII Visual Bar Chart of Top Hashtags
         */
        public void printVisualBarChart(int topK) {
            List<Map.Entry<String, Integer>> top = getTopTrending(topK);
            if (top.isEmpty()) return;

            int maxCount = top.get(0).getValue();
            System.out.println("Top Trending Hashtag Distribution (Bar Chart):");
            for (Map.Entry<String, Integer> entry : top) {
                int barLength = (int) Math.round(((double) entry.getValue() / maxCount) * 30);
                StringBuilder bar = new StringBuilder();
                for (int i = 0; i < barLength; i++) bar.append("█");
                System.out.printf("  %-18s | %-30s (%d mentions)%n", entry.getKey(), bar.toString(), entry.getValue());
            }
        }

        public void printDetailedReport() {
            System.out.println("\n=========================================================================================");
            System.out.println("                     HASHTAG FREQUENCY ANALYSIS REPORT                                  ");
            System.out.println("=========================================================================================");
            System.out.printf("Total Posts Processed       : %d%n", totalPostsProcessed);
            System.out.printf("Total Hashtag Mentions      : %d%n", totalHashtagsFound);
            System.out.printf("Unique Hashtags Identified  : %d%n", globalFrequencyMap.size());
            System.out.println("-----------------------------------------------------------------------------------------");

            System.out.println("\n>>> Top 5 Global Trending Hashtags <<<");
            List<Map.Entry<String, Integer>> top5 = getTopTrending(5);
            int rank = 1;
            for (Map.Entry<String, Integer> e : top5) {
                System.out.printf("  #%d. %-16s -> %d occurrences%n", rank++, e.getKey(), e.getValue());
            }

            System.out.println();
            printVisualBarChart(5);

            System.out.println("\n>>> Category-Wise Top Hashtags (Nested Maps) <<<");
            categoryFrequencyMap.forEach((category, tagMap) -> {
                System.out.println("Category: [" + category + "]");
                tagMap.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .forEach(e -> System.out.printf("    * %-16s : %d mentions%n", e.getKey(), e.getValue()));
            });

            System.out.println("\n>>> Lexicographical Alphabetical Index (TreeMap View) <<<");
            Map<String, Integer> alphaMap = getAlphabeticalView();
            alphaMap.forEach((tag, freq) -> System.out.printf("  %-18s : %d%n", tag, freq));
            System.out.println("=========================================================================================");
        }
    }

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("            HASHTAG FREQUENCIES COUNTER (JAVA COLLECTIONS & MAPS)                        ");
        System.out.println("=========================================================================================");

        List<Post> feed = Arrays.asList(
                new Post("P-01", "dev_alex", "TECH",
                        "Learning modern backend development with #Java and #SpringBoot! Loving #Coding."),
                new Post("P-02", "sarah_ai", "TECH",
                        "Deep learning advancements in 2026! #AI #MachineLearning #Java #Innovation"),
                new Post("P-03", "sports_center", "SPORTS",
                        "What an incredible final match! #Champions #Football #Victory #Football"),
                new Post("P-04", "tech_insider", "TECH",
                        "The new Java release has amazing performance updates! #Java #Coding #TechNews"),
                new Post("P-05", "movie_buff", "ENTERTAINMENT",
                        "Weekend blockbuster movie marathon! #Cinema #Entertainment #WeekendVibes"),
                new Post("P-06", "code_ninja", "TECH",
                        "Mastering algorithms and problem solving! #Coding #Java #DataStructures"),
                new Post("P-07", "astro_dan", "SCIENCE",
                        "Stunning James Webb telescope images released today! #Space #Science #Innovation"),
                new Post("P-08", "fitness_pro", "SPORTS",
                        "Morning marathon prep completed! #Fitness #HealthyLiving #Victory"),
                new Post("P-09", "daily_tech", "TECH",
                        "Microservices architecture with Spring Cloud and #Java #SpringBoot #TechNews"),
                new Post("P-10", "pop_culture", "ENTERTAINMENT",
                        "Red carpet fashion awards night! #Cinema #Celebrity #Entertainment")
        );

        System.out.println("\n--- Ingesting Feed of " + feed.size() + " Social Media Posts ---");
        HashtagAnalyticsEngine engine = new HashtagAnalyticsEngine();
        engine.ingestPosts(feed);

        engine.printDetailedReport();
    }
}

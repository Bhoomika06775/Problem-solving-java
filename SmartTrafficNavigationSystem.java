import java.util.*;

/**
 * Task 08: Smart Traffic Navigation System Using Graph Representation
 * Concepts: Graph Data Structure (Adjacency List), Dijkstra's Shortest Path Algorithm,
 *           PriorityQueue (Min-Heap), Dynamic Weighting (Traffic Congestion Multipliers),
 *           and Real-Time Rerouting.
 */
public class SmartTrafficNavigationSystem {

    // Congestion Level Multipliers
    public enum CongestionLevel {
        CLEAR(1.0, "Smooth"),
        MODERATE(1.6, "Slow Moving"),
        HEAVY(2.8, "Heavy Jam"),
        BLOCKED(Double.POSITIVE_INFINITY, "Road Closed");

        private final double delayMultiplier;
        private final String description;

        CongestionLevel(double delayMultiplier, String description) {
            this.delayMultiplier = delayMultiplier;
            this.description = description;
        }

        public double getDelayMultiplier() { return delayMultiplier; }
        public String getDescription() { return description; }
    }

    // Directed Road Segment (Graph Edge)
    public static class RoadSegment {
        private final String source;
        private final String destination;
        private final double distanceKm;
        private final double speedLimitKmh;
        private CongestionLevel congestion;

        public RoadSegment(String source, String destination, double distanceKm, double speedLimitKmh) {
            this.source = source;
            this.destination = destination;
            this.distanceKm = distanceKm;
            this.speedLimitKmh = speedLimitKmh;
            this.congestion = CongestionLevel.CLEAR;
        }

        public String getSource() { return source; }
        public String getDestination() { return destination; }
        public double getDistanceKm() { return distanceKm; }
        public double getSpeedLimitKmh() { return speedLimitKmh; }
        public CongestionLevel getCongestion() { return congestion; }
        public void setCongestion(CongestionLevel congestion) { this.congestion = congestion; }

        // Travel time in minutes
        public double getTravelTimeMinutes() {
            if (congestion == CongestionLevel.BLOCKED) return Double.POSITIVE_INFINITY;
            double effectiveSpeed = speedLimitKmh / congestion.getDelayMultiplier();
            return (distanceKm / effectiveSpeed) * 60.0;
        }

        @Override
        public String toString() {
            return String.format("%s -> %s (%.1f km, Time: %.1f min [%s])",
                    source, destination, distanceKm, getTravelTimeMinutes(), congestion.getDescription());
        }
    }

    // Navigation Route Result
    public static class NavigationRoute {
        private final List<String> path;
        private final double totalDistanceKm;
        private final double totalTimeMinutes;

        public NavigationRoute(List<String> path, double totalDistanceKm, double totalTimeMinutes) {
            this.path = path;
            this.totalDistanceKm = totalDistanceKm;
            this.totalTimeMinutes = totalTimeMinutes;
        }

        public List<String> getPath() { return path; }
        public double getTotalDistanceKm() { return totalDistanceKm; }
        public double getTotalTimeMinutes() { return totalTimeMinutes; }

        public void printRoute(String title) {
            System.out.println("  " + title);
            if (path.isEmpty() || totalTimeMinutes == Double.POSITIVE_INFINITY) {
                System.out.println("  [NO ROUTE AVAILABLE] Destination unreachable due to road closures.");
                return;
            }
            System.out.printf("  Path            : %s%n", String.join("  ──▶  ", path));
            System.out.printf("  Total Distance  : %.2f km%n", totalDistanceKm);
            System.out.printf("  Estimated Time  : %.1f minutes (%.2f hours)%n", totalTimeMinutes, totalTimeMinutes / 60.0);
        }
    }

    // Node wrapper for PriorityQueue
    private static class NodeRecord implements Comparable<NodeRecord> {
        final String nodeId;
        final double cost;

        NodeRecord(String nodeId, double cost) {
            this.nodeId = nodeId;
            this.cost = cost;
        }

        @Override
        public int compareTo(NodeRecord other) {
            return Double.compare(this.cost, other.cost);
        }
    }

    // City Road Network (Graph Representation via Adjacency List)
    public static class CityRoadNetwork {
        private final Map<String, List<RoadSegment>> adjacencyList = new HashMap<>();

        public void addIntersection(String name) {
            adjacencyList.putIfAbsent(name, new ArrayList<>());
        }

        public void addBidirectionalRoad(String u, String v, double distanceKm, double speedLimitKmh) {
            addIntersection(u);
            addIntersection(v);
            adjacencyList.get(u).add(new RoadSegment(u, v, distanceKm, speedLimitKmh));
            adjacencyList.get(v).add(new RoadSegment(v, u, distanceKm, speedLimitKmh));
        }

        public void updateTraffic(String from, String to, CongestionLevel level) {
            boolean updated = false;
            if (adjacencyList.containsKey(from)) {
                for (RoadSegment edge : adjacencyList.get(from)) {
                    if (edge.getDestination().equals(to)) {
                        edge.setCongestion(level);
                        updated = true;
                    }
                }
            }
            if (adjacencyList.containsKey(to)) {
                for (RoadSegment edge : adjacencyList.get(to)) {
                    if (edge.getDestination().equals(from)) {
                        edge.setCongestion(level);
                        updated = true;
                    }
                }
            }
            if (updated) {
                System.out.printf("  [TRAFFIC EVENT] Road [%s <--> %s] updated to: %s (%s)%n",
                        from, to, level.name(), level.getDescription());
            }
        }

        /**
         * Dijkstra's Algorithm: Optimize for Minimum Travel Time (Factoring Live Congestion)
         */
        public NavigationRoute findFastestRoute(String start, String destination) {
            return dijkstra(start, destination, true);
        }

        /**
         * Dijkstra's Algorithm: Optimize for Minimum Physical Distance (Ignoring Congestion)
         */
        public NavigationRoute findShortestDistanceRoute(String start, String destination) {
            return dijkstra(start, destination, false);
        }

        private NavigationRoute dijkstra(String start, String destination, boolean optimizeForTime) {
            Map<String, Double> minCost = new HashMap<>();
            Map<String, String> previousNode = new HashMap<>();
            Map<String, RoadSegment> edgeUsed = new HashMap<>();

            for (String node : adjacencyList.keySet()) {
                minCost.put(node, Double.POSITIVE_INFINITY);
            }
            minCost.put(start, 0.0);

            PriorityQueue<NodeRecord> pq = new PriorityQueue<>();
            pq.add(new NodeRecord(start, 0.0));

            while (!pq.isEmpty()) {
                NodeRecord current = pq.poll();
                String u = current.nodeId;

                if (current.cost > minCost.get(u)) continue;
                if (u.equals(destination)) break;

                for (RoadSegment edge : adjacencyList.getOrDefault(u, Collections.emptyList())) {
                    if (edge.getCongestion() == CongestionLevel.BLOCKED) continue;

                    double weight = optimizeForTime ? edge.getTravelTimeMinutes() : edge.getDistanceKm();
                    double newCost = minCost.get(u) + weight;

                    if (newCost < minCost.get(edge.getDestination())) {
                        minCost.put(edge.getDestination(), newCost);
                        previousNode.put(edge.getDestination(), u);
                        edgeUsed.put(edge.getDestination(), edge);
                        pq.add(new NodeRecord(edge.getDestination(), newCost));
                    }
                }
            }

            // Reconstruct path
            if (minCost.get(destination) == Double.POSITIVE_INFINITY) {
                return new NavigationRoute(Collections.emptyList(), 0, Double.POSITIVE_INFINITY);
            }

            LinkedList<String> path = new LinkedList<>();
            String step = destination;
            double totalDist = 0.0;
            double totalTime = 0.0;

            while (step != null) {
                path.addFirst(step);
                RoadSegment edge = edgeUsed.get(step);
                if (edge != null) {
                    totalDist += edge.getDistanceKm();
                    totalTime += edge.getTravelTimeMinutes();
                }
                step = previousNode.get(step);
            }

            return new NavigationRoute(path, totalDist, totalTime);
        }

        public void printNetworkTopology() {
            System.out.println("City Road Network Topology (Adjacency List):");
            adjacencyList.forEach((node, edges) -> {
                System.out.printf("  [%-16s] connects to %d junctions:%n", node, edges.size());
                for (RoadSegment e : edges) {
                    System.out.printf("     -> %-16s | Dist: %4.1f km | Speed: %2.0f km/h | Status: %s%n",
                            e.getDestination(), e.getDistanceKm(), e.getSpeedLimitKmh(), e.getCongestion().getDescription());
                }
            });
        }
    }

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("            SMART TRAFFIC NAVIGATION SYSTEM (GRAPH & DIJKSTRA)                           ");
        System.out.println("=========================================================================================");

        CityRoadNetwork network = new CityRoadNetwork();

        // 1. Build City Intersections & Arterial Highways
        network.addBidirectionalRoad("Downtown",       "North-Hub",       6.0,  50.0);
        network.addBidirectionalRoad("Downtown",       "Midtown-Square",  4.0,  40.0);
        network.addBidirectionalRoad("North-Hub",      "East-Junction",   8.0,  60.0);
        network.addBidirectionalRoad("North-Hub",      "Airport-Terminal",14.0, 80.0);
        network.addBidirectionalRoad("Midtown-Square", "East-Junction",   5.0,  45.0);
        network.addBidirectionalRoad("Midtown-Square", "South-Gate",      7.0,  50.0);
        network.addBidirectionalRoad("East-Junction",  "Airport-Terminal", 6.0, 70.0);
        network.addBidirectionalRoad("South-Gate",     "Tech-Corridor",   9.0,  60.0);
        network.addBidirectionalRoad("Tech-Corridor",  "Airport-Terminal", 8.0, 75.0);

        System.out.println("\n--- Step 1: Baseline City Network Architecture ---");
        network.printNetworkTopology();

        // 2. Query 1: Initial Navigation from Downtown to Airport
        String origin = "Downtown";
        String destination = "Airport-Terminal";

        System.out.printf("\n--- Step 2: Route Planning from '%s' to '%s' (Clear Roads) ---%n", origin, destination);
        NavigationRoute shortestDist = network.findShortestDistanceRoute(origin, destination);
        shortestDist.printRoute("Option A: Shortest Physical Distance Route");

        System.out.println();
        NavigationRoute fastestRoute = network.findFastestRoute(origin, destination);
        fastestRoute.printRoute("Option B: Fastest Dynamic Route (Real-Time Traffic)");

        // 3. Step 3: Simulate Real-Time Traffic Incidents
        System.out.println("\n--- Step 3: Real-Time Traffic Congestion & Roadblock Incident ---");
        // Heavy traffic jam on Midtown <-> East-Junction
        network.updateTraffic("Midtown-Square", "East-Junction", CongestionLevel.HEAVY);
        // North-Hub to Airport road closed due to construction
        network.updateTraffic("North-Hub", "Airport-Terminal", CongestionLevel.BLOCKED);

        // 4. Query 2: Dynamic Rerouting after Incident
        System.out.printf("\n--- Step 4: Autonomous Rerouting for '%s' to '%s' ---%n", origin, destination);
        NavigationRoute rerouted = network.findFastestRoute(origin, destination);
        rerouted.printRoute("Optimal Real-Time Rerouted Path (Avoiding Delays & Blockages)");

        System.out.println("\n=========================================================================================");
        System.out.println(" Navigation Analysis Complete: Dynamic Dijkstra guarantees optimal minimal commute!");
        System.out.println("=========================================================================================");
    }
}

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Task 02: Real-Time Stream Engine Using Stream Processing Concepts
 * Concepts: Event Streams, Windowed Aggregations, Sliding/Tumbling Windows,
 *           Filtering, Anomaly Alerting, and Reactive Pipeline Processing.
 */
public class RealTimeStreamEngine {

    // Representation of a telemetry / metric event in the stream
    public static class TelemetryEvent {
        private final String eventId;
        private final String deviceId;
        private final String sensorType; // e.g., TEMPERATURE, PRESSURE, VIBRATION
        private final double value;
        private final long timestamp;

        public TelemetryEvent(String eventId, String deviceId, String sensorType, double value, long timestamp) {
            this.eventId = eventId;
            this.deviceId = deviceId;
            this.sensorType = sensorType;
            this.value = value;
            this.timestamp = timestamp;
        }

        public String getEventId() { return eventId; }
        public String getDeviceId() { return deviceId; }
        public String getSensorType() { return sensorType; }
        public double getValue() { return value; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            String timeStr = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(timestamp));
            return String.format("[%s] Event=%s | Device=%s | Type=%-11s | Value=%6.2f",
                    timeStr, eventId, deviceId, sensorType, value);
        }
    }

    // Windowed Aggregated Metrics
    public static class WindowMetrics {
        private final long windowStart;
        private final long windowEnd;
        private final String sensorType;
        private final long eventCount;
        private final double average;
        private final double min;
        private final double max;

        public WindowMetrics(long windowStart, long windowEnd, String sensorType,
                             long eventCount, double average, double min, double max) {
            this.windowStart = windowStart;
            this.windowEnd = windowEnd;
            this.sensorType = sensorType;
            this.eventCount = eventCount;
            this.average = average;
            this.min = min;
            this.max = max;
        }

        public void display() {
            System.out.printf("  [Window %d - %d ms] %-11s | Count: %3d | Avg: %6.2f | Min: %6.2f | Max: %6.2f%n",
                    windowStart % 100000, windowEnd % 100000, sensorType, eventCount, average, min, max);
        }
    }

    // Stream Processor Engine
    public static class StreamPipeline {
        private final List<TelemetryEvent> buffer = new ArrayList<>();
        private final List<Predicate<TelemetryEvent>> filters = new ArrayList<>();
        private final List<Consumer<TelemetryEvent>> alertHandlers = new ArrayList<>();

        public StreamPipeline filter(Predicate<TelemetryEvent> predicate) {
            filters.add(predicate);
            return this;
        }

        public StreamPipeline onAlert(Consumer<TelemetryEvent> handler) {
            alertHandlers.add(handler);
            return this;
        }

        public void ingest(TelemetryEvent event) {
            // Check filters
            boolean passesAll = filters.stream().allMatch(f -> f.test(event));
            if (passesAll) {
                buffer.add(event);
                // Check alerts
                alertHandlers.forEach(h -> h.accept(event));
            }
        }

        public List<TelemetryEvent> getIngestedEvents() {
            return Collections.unmodifiableList(buffer);
        }

        // Tumbling Window Aggregation by duration
        public List<WindowMetrics> computeTumblingWindows(long windowDurationMs) {
            if (buffer.isEmpty()) return Collections.emptyList();

            long minTime = buffer.stream().mapToLong(TelemetryEvent::getTimestamp).min().orElse(0);
            long maxTime = buffer.stream().mapToLong(TelemetryEvent::getTimestamp).max().orElse(0);

            List<WindowMetrics> results = new ArrayList<>();

            for (long start = minTime; start <= maxTime; start += windowDurationMs) {
                long end = start + windowDurationMs;
                long finalStart = start;

                List<TelemetryEvent> windowEvents = buffer.stream()
                        .filter(e -> e.getTimestamp() >= finalStart && e.getTimestamp() < end)
                        .collect(Collectors.toList());

                if (!windowEvents.isEmpty()) {
                    // Group by sensor type in this window
                    Map<String, List<TelemetryEvent>> byType = windowEvents.stream()
                            .collect(Collectors.groupingBy(TelemetryEvent::getSensorType));

                    for (Map.Entry<String, List<TelemetryEvent>> entry : byType.entrySet()) {
                        DoubleSummaryStatistics stats = entry.getValue().stream()
                                .mapToDouble(TelemetryEvent::getValue)
                                .summaryStatistics();

                        results.add(new WindowMetrics(
                                start, end, entry.getKey(),
                                stats.getCount(), stats.getAverage(), stats.getMin(), stats.getMax()
                        ));
                    }
                }
            }
            return results;
        }
    }

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("                     REAL-TIME STREAM ENGINE SIMULATOR                                   ");
        System.out.println("=========================================================================================");

        StreamPipeline pipeline = new StreamPipeline();

        // 1. Data Cleaning Filter: Drop faulty sensor readings (e.g., negative values or NaN)
        pipeline.filter(event -> event.getValue() >= 0.0);

        // 2. Real-time Anomaly Alert Handler
        pipeline.onAlert(event -> {
            if ("TEMPERATURE".equals(event.getSensorType()) && event.getValue() > 80.0) {
                System.out.printf("  [CRITICAL ALERT] Sensor: %s | Device: %s | High Temp: %.2f °C!%n",
                        event.getSensorType(), event.getDeviceId(), event.getValue());
            } else if ("PRESSURE".equals(event.getSensorType()) && event.getValue() > 140.0) {
                System.out.printf("  [WARNING ALERT] Sensor: %s | Device: %s | High Pressure: %.2f PSI!%n",
                        event.getSensorType(), event.getDeviceId(), event.getValue());
            }
        });

        System.out.println("\n--- Ingesting Live Telemetry Stream Events ---");

        long baseTime = System.currentTimeMillis();
        List<TelemetryEvent> liveEvents = Arrays.asList(
                new TelemetryEvent("EVT-1001", "DEV-ALPHA", "TEMPERATURE", 65.5, baseTime + 100),
                new TelemetryEvent("EVT-1002", "DEV-BETA",  "PRESSURE",    120.0, baseTime + 250),
                new TelemetryEvent("EVT-1003", "DEV-GAMMA", "TEMPERATURE", 88.4, baseTime + 450), // triggers alert!
                new TelemetryEvent("EVT-1004", "DEV-ALPHA", "VIBRATION",   12.3, baseTime + 600),
                new TelemetryEvent("EVT-1005", "DEV-DELTA", "PRESSURE",    -5.0, baseTime + 700),  // filtered out (corrupt)
                new TelemetryEvent("EVT-1006", "DEV-BETA",  "TEMPERATURE", 72.1, baseTime + 1100),
                new TelemetryEvent("EVT-1007", "DEV-GAMMA", "PRESSURE",    148.2, baseTime + 1300), // triggers alert!
                new TelemetryEvent("EVT-1008", "DEV-ALPHA", "TEMPERATURE", 94.0, baseTime + 1500), // triggers alert!
                new TelemetryEvent("EVT-1009", "DEV-BETA",  "VIBRATION",   15.8, baseTime + 1800),
                new TelemetryEvent("EVT-1010", "DEV-GAMMA", "TEMPERATURE", 68.0, baseTime + 2100),
                new TelemetryEvent("EVT-1011", "DEV-ALPHA", "PRESSURE",    115.4, baseTime + 2350),
                new TelemetryEvent("EVT-1012", "DEV-DELTA", "TEMPERATURE", 74.3, baseTime + 2600)
        );

        for (TelemetryEvent evt : liveEvents) {
            System.out.println("Processing: " + evt);
            pipeline.ingest(evt);
        }

        System.out.println("\n--- Ingestion Completed ---");
        System.out.println("Total Events Ingested (Valid): " + pipeline.getIngestedEvents().size());

        // 3. Window Aggregation: 1000ms Tumbling Window Processing
        System.out.println("\n>>> Windowed Aggregations (1000ms Tumbling Window) <<<");
        List<WindowMetrics> windowMetrics = pipeline.computeTumblingWindows(1000);
        windowMetrics.forEach(WindowMetrics::display);

        // 4. Stream Analysis: Per-Device Summary using Java Streams
        System.out.println("\n>>> Per-Device Event Ingestion Metrics <<<");
        Map<String, Long> deviceEventCounts = pipeline.getIngestedEvents().stream()
                .collect(Collectors.groupingBy(TelemetryEvent::getDeviceId, Collectors.counting()));

        deviceEventCounts.forEach((dev, count) ->
                System.out.printf("  * Device ID: %-10s | Total Valid Packets: %d%n", dev, count));

        // 5. Overall Sensor Statistics
        System.out.println("\n>>> Sensor Value Distribution <<<");
        Map<String, DoubleSummaryStatistics> sensorStats = pipeline.getIngestedEvents().stream()
                .collect(Collectors.groupingBy(
                        TelemetryEvent::getSensorType,
                        Collectors.summarizingDouble(TelemetryEvent::getValue)
                ));

        sensorStats.forEach((sensor, stat) -> {
            System.out.printf("  * Sensor: %-11s | Samples: %2d | Mean: %6.2f | Range: [%.2f - %.2f]%n",
                    sensor, stat.getCount(), stat.getAverage(), stat.getMin(), stat.getMax());
        });

        System.out.println("=========================================================================================");
    }
}

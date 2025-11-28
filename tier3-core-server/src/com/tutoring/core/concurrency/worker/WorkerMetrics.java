package com.tutoring.core.concurrency.worker;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized metrics for Worker Pattern performance monitoring
 * Tracks latency improvements and resource utilization
 */
public class WorkerMetrics {
    // Broadcast metrics
    private final AtomicLong totalBroadcasts = new AtomicLong(0);
    private final AtomicLong totalBroadcastTimeMs = new AtomicLong(0);
    private final AtomicLong minBroadcastTimeMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxBroadcastTimeMs = new AtomicLong(0);
    
    // Client handler metrics
    private final AtomicLong totalConnectionsAccepted = new AtomicLong(0);
    private final AtomicLong totalConnectionsRejected = new AtomicLong(0);
    
    // Thread pool utilization
    private final AtomicLong peakActiveThreads = new AtomicLong(0);
    
    private static WorkerMetrics instance;
    
    private WorkerMetrics() {}
    
    public static synchronized WorkerMetrics getInstance() {
        if (instance == null) {
            instance = new WorkerMetrics();
        }
        return instance;
    }
    
    /**
     * Record a broadcast operation
     */
    public void recordBroadcast(long durationMs, int recipientCount) {
        totalBroadcasts.incrementAndGet();
        totalBroadcastTimeMs.addAndGet(durationMs);
        
        // Update min/max
        updateMin(minBroadcastTimeMs, durationMs);
        updateMax(maxBroadcastTimeMs, durationMs);
    }
    
    /**
     * Record connection acceptance
     */
    public void recordConnectionAccepted() {
        totalConnectionsAccepted.incrementAndGet();
    }
    
    /**
     * Record connection rejection
     */
    public void recordConnectionRejected() {
        totalConnectionsRejected.incrementAndGet();
    }
    
    /**
     * Update peak active threads
     */
    public void updatePeakThreads(int activeThreads) {
        updateMax(peakActiveThreads, activeThreads);
    }
    
    private void updateMin(AtomicLong current, long newValue) {
        long oldValue;
        do {
            oldValue = current.get();
            if (newValue >= oldValue) return;
        } while (!current.compareAndSet(oldValue, newValue));
    }
    
    private void updateMax(AtomicLong current, long newValue) {
        long oldValue;
        do {
            oldValue = current.get();
            if (newValue <= oldValue) return;
        } while (!current.compareAndSet(oldValue, newValue));
    }
    
    /**
     * Print comprehensive metrics report
     */
    public void printReport() {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║          WORKER PATTERN PERFORMANCE METRICS              ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        
        // Broadcast performance
        long broadcasts = totalBroadcasts.get();
        if (broadcasts > 0) {
            long totalTime = totalBroadcastTimeMs.get();
            double avgTime = (double) totalTime / broadcasts;
            
            System.out.println("║ BROADCAST PERFORMANCE                                    ║");
            System.out.printf("║   Total Broadcasts:           %,10d                  ║%n", broadcasts);
            System.out.printf("║   Average Latency:            %,10.2f ms              ║%n", avgTime);
            System.out.printf("║   Min Latency:                %,10d ms              ║%n", 
                minBroadcastTimeMs.get() == Long.MAX_VALUE ? 0 : minBroadcastTimeMs.get());
            System.out.printf("║   Max Latency:                %,10d ms              ║%n", maxBroadcastTimeMs.get());
            
            // Calculate improvement vs sequential (estimate)
            // Sequential: ~1ms per recipient, Parallel: ~constant time
            System.out.printf("║   Estimated Speedup:          %,10.1fx               ║%n", 
                calculateEstimatedSpeedup(avgTime));
        }
        
        System.out.println("║                                                          ║");
        
        // Connection management
        long accepted = totalConnectionsAccepted.get();
        long rejected = totalConnectionsRejected.get();
        long total = accepted + rejected;
        
        System.out.println("║ CONNECTION MANAGEMENT                                    ║");
        System.out.printf("║   Connections Accepted:       %,10d                  ║%n", accepted);
        System.out.printf("║   Connections Rejected:       %,10d                  ║%n", rejected);
        
        if (total > 0) {
            double acceptRate = (double) accepted / total * 100;
            System.out.printf("║   Accept Rate:                %,10.1f%%              ║%n", acceptRate);
        }
        
        System.out.println("║                                                          ║");
        
        // Thread utilization
        System.out.println("║ THREAD POOL UTILIZATION                                  ║");
        System.out.printf("║   Peak Active Threads:        %,10d                  ║%n", peakActiveThreads.get());
        System.out.printf("║   Available CPU Cores:        %,10d                  ║%n", 
            Runtime.getRuntime().availableProcessors());
        
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }
    
    /**
     * Calculate estimated speedup vs sequential broadcasting
     */
    private double calculateEstimatedSpeedup(double avgParallelTimeMs) {
        // Assume sequential would take ~1ms per recipient (conservative estimate)
        // Typical room has ~10 recipients
        double estimatedSequentialTime = 10.0; // ms for 10 recipients
        
        if (avgParallelTimeMs < 0.1) return 100.0; // Cap at 100x
        
        double speedup = estimatedSequentialTime / avgParallelTimeMs;
        return Math.min(speedup, 100.0); // Cap at 100x
    }
    
    /**
     * Get average broadcast latency
     */
    public double getAverageBroadcastLatency() {
        long broadcasts = totalBroadcasts.get();
        if (broadcasts == 0) return 0.0;
        
        return (double) totalBroadcastTimeMs.get() / broadcasts;
    }
    
    /**
     * Get connection acceptance rate
     */
    public double getAcceptanceRate() {
        long accepted = totalConnectionsAccepted.get();
        long rejected = totalConnectionsRejected.get();
        long total = accepted + rejected;
        
        if (total == 0) return 100.0;
        
        return (double) accepted / total * 100.0;
    }
    
    /**
     * Reset all metrics
     */
    public void reset() {
        totalBroadcasts.set(0);
        totalBroadcastTimeMs.set(0);
        minBroadcastTimeMs.set(Long.MAX_VALUE);
        maxBroadcastTimeMs.set(0);
        totalConnectionsAccepted.set(0);
        totalConnectionsRejected.set(0);
        peakActiveThreads.set(0);
    }
    
    /**
     * Get snapshot of current metrics
     */
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            totalBroadcasts.get(),
            getAverageBroadcastLatency(),
            minBroadcastTimeMs.get() == Long.MAX_VALUE ? 0 : minBroadcastTimeMs.get(),
            maxBroadcastTimeMs.get(),
            totalConnectionsAccepted.get(),
            totalConnectionsRejected.get(),
            getAcceptanceRate(),
            peakActiveThreads.get()
        );
    }
    
    public static class MetricsSnapshot {
        public final long totalBroadcasts;
        public final double avgBroadcastLatency;
        public final long minBroadcastLatency;
        public final long maxBroadcastLatency;
        public final long connectionsAccepted;
        public final long connectionsRejected;
        public final double acceptanceRate;
        public final long peakActiveThreads;
        
        MetricsSnapshot(long totalBroadcasts, double avgBroadcastLatency,
                       long minBroadcastLatency, long maxBroadcastLatency,
                       long connectionsAccepted, long connectionsRejected,
                       double acceptanceRate, long peakActiveThreads) {
            this.totalBroadcasts = totalBroadcasts;
            this.avgBroadcastLatency = avgBroadcastLatency;
            this.minBroadcastLatency = minBroadcastLatency;
            this.maxBroadcastLatency = maxBroadcastLatency;
            this.connectionsAccepted = connectionsAccepted;
            this.connectionsRejected = connectionsRejected;
            this.acceptanceRate = acceptanceRate;
            this.peakActiveThreads = peakActiveThreads;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Broadcasts: %d, AvgLatency: %.2fms, Connections: %d accepted / %d rejected (%.1f%%), PeakThreads: %d",
                totalBroadcasts, avgBroadcastLatency, connectionsAccepted, connectionsRejected, 
                acceptanceRate, peakActiveThreads
            );
        }
    }
}


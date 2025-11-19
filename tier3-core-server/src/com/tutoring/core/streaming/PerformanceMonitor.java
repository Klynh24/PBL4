package com.tutoring.core.streaming;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors performance metrics for the streaming system
 * Tracks processing times, throughput, and frame statistics
 * 
 * From IMPLEMENTATION_ROADMAP.md Phase 5
 */
public class PerformanceMonitor {
    private final Map<String, Metric> metrics = new ConcurrentHashMap<>();
    
    // Global statistics
    private final AtomicLong totalFramesProcessed = new AtomicLong(0);
    private final AtomicLong totalBytesProcessed = new AtomicLong(0);
    private final AtomicLong totalKeyframes = new AtomicLong(0);
    private final AtomicLong totalDeltaFrames = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();
    
    /**
     * Record a timing measurement for a specific stage
     * 
     * @param stage Name of the processing stage (e.g., "dirty_detection", "encoding")
     * @param durationMs Duration in milliseconds
     */
    public void recordFrameProcessing(String stage, long durationMs) {
        metrics.computeIfAbsent(stage, k -> new Metric()).record(durationMs);
    }
    
    /**
     * Record a completed frame
     * 
     * @param frameSize Size in bytes
     * @param isKeyframe True if keyframe, false if delta
     * @param processingTimeMs Total processing time
     */
    public void recordFrame(int frameSize, boolean isKeyframe, long processingTimeMs) {
        totalFramesProcessed.incrementAndGet();
        totalBytesProcessed.addAndGet(frameSize);
        
        if (isKeyframe) {
            totalKeyframes.incrementAndGet();
        } else {
            totalDeltaFrames.incrementAndGet();
        }
        
        recordFrameProcessing("total", processingTimeMs);
    }
    
    /**
     * Print comprehensive statistics
     */
    public void printStats() {
        long frames = totalFramesProcessed.get();
        long bytes = totalBytesProcessed.get();
        long keyframes = totalKeyframes.get();
        long deltaFrames = totalDeltaFrames.get();
        long runtimeSeconds = (System.currentTimeMillis() - startTime) / 1000;
        
        System.out.println("\n=== Performance Statistics ===");
        System.out.println("Runtime: " + runtimeSeconds + " seconds");
        System.out.println();
        
        // Frame statistics
        System.out.println("Frames:");
        System.out.println("  Total: " + frames);
        System.out.println("  Keyframes: " + keyframes + " (" + 
            (frames > 0 ? (keyframes * 100.0 / frames) : 0) + "%)");
        System.out.println("  Delta frames: " + deltaFrames + " (" + 
            (frames > 0 ? (deltaFrames * 100.0 / frames) : 0) + "%)");
        System.out.println("  Frame rate: " + 
            (runtimeSeconds > 0 ? (frames * 1.0 / runtimeSeconds) : 0) + " FPS");
        System.out.println();
        
        // Bandwidth statistics
        double mbProcessed = bytes / 1024.0 / 1024.0;
        double avgFrameSize = frames > 0 ? (bytes * 1.0 / frames / 1024.0) : 0;
        double throughputMBps = runtimeSeconds > 0 ? (mbProcessed / runtimeSeconds) : 0;
        
        System.out.println("Bandwidth:");
        System.out.println("  Total processed: " + String.format("%.2f MB", mbProcessed));
        System.out.println("  Avg frame size: " + String.format("%.2f KB", avgFrameSize));
        System.out.println("  Throughput: " + String.format("%.2f MB/s", throughputMBps));
        System.out.println();
        
        // Processing time statistics
        System.out.println("Processing Times:");
        metrics.forEach((stage, metric) -> {
            System.out.printf("  %s: avg=%.2fms, max=%dms, min=%dms, count=%d%n",
                stage, 
                metric.getAverage(), 
                metric.getMax(), 
                metric.getMin(),
                metric.getCount());
        });
        
        System.out.println("==============================\n");
    }
    
    /**
     * Print brief statistics (single line)
     */
    public void printBrief() {
        long frames = totalFramesProcessed.get();
        long bytes = totalBytesProcessed.get();
        double avgFrameSize = frames > 0 ? (bytes * 1.0 / frames / 1024.0) : 0;
        
        Metric totalMetric = metrics.get("total");
        double avgTime = totalMetric != null ? totalMetric.getAverage() : 0;
        
        System.out.printf("[Performance] Frames: %d, Avg size: %.2f KB, Avg time: %.2f ms%n",
            frames, avgFrameSize, avgTime);
    }
    
    /**
     * Get metric for a specific stage
     */
    public Metric getMetric(String stage) {
        return metrics.get(stage);
    }
    
    /**
     * Get total frames processed
     */
    public long getTotalFrames() {
        return totalFramesProcessed.get();
    }
    
    /**
     * Get total bytes processed
     */
    public long getTotalBytes() {
        return totalBytesProcessed.get();
    }
    
    /**
     * Get average frame size in bytes
     */
    public double getAverageFrameSize() {
        long frames = totalFramesProcessed.get();
        return frames > 0 ? (totalBytesProcessed.get() * 1.0 / frames) : 0;
    }
    
    /**
     * Check if performance targets are met
     * 
     * @return true if all targets met
     */
    public boolean checkTargets() {
        boolean targetsOk = true;
        
        Metric totalMetric = metrics.get("total");
        if (totalMetric != null) {
            // Target: < 100ms total processing
            if (totalMetric.getAverage() > 100) {
                System.out.println("[WARNING] Total processing time exceeds target: " + 
                    totalMetric.getAverage() + "ms (target: <100ms)");
                targetsOk = false;
            }
        }
        
        Metric detectionMetric = metrics.get("dirty_detection");
        if (detectionMetric != null) {
            // Target: < 30ms dirty detection
            if (detectionMetric.getAverage() > 30) {
                System.out.println("[WARNING] Dirty detection time exceeds target: " + 
                    detectionMetric.getAverage() + "ms (target: <30ms)");
                targetsOk = false;
            }
        }
        
        // Target: Avg delta frame < 50 KB
        long deltaFrames = totalDeltaFrames.get();
        if (deltaFrames > 0) {
            double avgDeltaSize = getAverageFrameSize() / 1024.0; // KB
            if (avgDeltaSize > 50) {
                System.out.println("[WARNING] Average delta frame size exceeds target: " + 
                    String.format("%.2f KB", avgDeltaSize) + " (target: <50KB)");
                targetsOk = false;
            }
        }
        
        return targetsOk;
    }
    
    /**
     * Reset all statistics
     */
    public void reset() {
        metrics.clear();
        totalFramesProcessed.set(0);
        totalBytesProcessed.set(0);
        totalKeyframes.set(0);
        totalDeltaFrames.set(0);
    }
    
    /**
     * Inner class to track metrics for a specific stage
     * 
     * ✅ OPTIMIZATION: Lock-free implementation using atomic operations
     */
    public static class Metric {
        private final AtomicLong totalMs = new AtomicLong(0);
        private final AtomicLong maxMs = new AtomicLong(0);
        private final AtomicLong minMs = new AtomicLong(Long.MAX_VALUE);
        private final AtomicInteger count = new AtomicInteger(0);
        
        // ✅ OPTIMIZATION: Lock-free record using CAS operations
        public void record(long ms) {
            totalMs.addAndGet(ms);
            count.incrementAndGet();
            
            // ✅ OPTIMIZATION: Lock-free max update using compare-and-swap
            long currentMax;
            do {
                currentMax = maxMs.get();
                if (ms <= currentMax) break;
            } while (!maxMs.compareAndSet(currentMax, ms));
            
            // ✅ OPTIMIZATION: Lock-free min update using compare-and-swap
            long currentMin;
            do {
                currentMin = minMs.get();
                if (ms >= currentMin) break;
            } while (!minMs.compareAndSet(currentMin, ms));
        }
        
        public double getAverage() {
            int cnt = count.get();
            return cnt > 0 ? (totalMs.get() * 1.0 / cnt) : 0;
        }
        
        public long getMax() {
            return maxMs.get();
        }
        
        public long getMin() {
            long min = minMs.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        
        public int getCount() {
            return count.get();
        }
        
        public long getTotal() {
            return totalMs.get();
        }
    }
}


package streaming.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Tracks network statistics for a single client
 * Uses a sliding window to calculate packet loss rate
 * Thread-safe implementation using ConcurrentLinkedQueue
 */
public class ClientNetworkStats {
    private final int windowSeconds;
    private final Queue<NetworkSample> samples = new ConcurrentLinkedQueue<>();
    
    public ClientNetworkStats(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
    
    /**
     * Record a packet sent to this client
     */
    public void incrementPacketsSent() {
        addSample(1, 0);
    }
    
    /**
     * Record NACK requests from this client
     * @param count Number of packets lost (NACK count)
     */
    public void recordNACKs(int count) {
        addSample(0, count);
    }
    
    /**
     * Add a network sample with timestamp
     */
    private void addSample(int sent, int lost) {
        long now = System.currentTimeMillis();
        samples.offer(new NetworkSample(now, sent, lost));
        
        // Remove old samples outside the window
        long cutoff = now - (windowSeconds * 1000L);
        samples.removeIf(s -> s.timestamp < cutoff);
    }
    
    /**
     * Calculate packet loss rate over the window
     * @return Loss rate (0.0 to 1.0)
     */
    public double calculatePacketLossRate() {
        int totalSent = 0;
        int totalLost = 0;
        
        for (NetworkSample sample : samples) {
            totalSent += sample.packetsSent;
            totalLost += sample.packetsLost;
        }
        
        if (totalSent == 0) return 0.0;
        return (double) totalLost / totalSent;
    }
    
    /**
     * Get total packets sent in window
     */
    public int getTotalPacketsSent() {
        return samples.stream().mapToInt(s -> s.packetsSent).sum();
    }
    
    /**
     * Get total packets lost in window
     */
    public int getTotalPacketsLost() {
        return samples.stream().mapToInt(s -> s.packetsLost).sum();
    }
    
    /**
     * Get number of samples in window
     */
    public int getSampleCount() {
        return samples.size();
    }
    
    /**
     * Clear all samples (for cleanup)
     */
    public void clear() {
        samples.clear();
    }
    
    /**
     * Internal class representing a network measurement sample
     */
    private static class NetworkSample {
        final long timestamp;
        final int packetsSent;
        final int packetsLost;
        
        NetworkSample(long timestamp, int sent, int lost) {
            this.timestamp = timestamp;
            this.packetsSent = sent;
            this.packetsLost = lost;
        }
    }
}

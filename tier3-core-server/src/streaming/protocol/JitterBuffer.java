package streaming.protocol;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ✅ CRITICAL FIX: Jitter Buffer for Voice Packets
 * 
 * Problem: Voice packets arrive out of order, causing audio glitches
 * Solution: Buffer packets and play them in order with adaptive delay
 * 
 * Benefits:
 * - Smooth voice playback
 * - Handles network jitter (variable latency)
 * - Adaptive buffer size based on jitter
 * - Prevents audio gaps and stuttering
 * 
 * Usage: Server-side buffering before sending to clients
 */
public class JitterBuffer {
    private final String clientId;
    private final ConcurrentSkipListMap<Long, VoicePacket> buffer;

    // Configuration
    private static final int MIN_BUFFER_MS = 20; // Minimum 20ms buffer
    private static final int MAX_BUFFER_MS = 200; // Maximum 200ms buffer
    private static final int TARGET_BUFFER_MS = 60; // Target 60ms buffer

    // State
    private long nextExpectedSeq = 0;
    private long bufferDelayMs = TARGET_BUFFER_MS;
    private long lastPlayoutTime = 0;

    // Metrics
    private final AtomicInteger packetsReceived = new AtomicInteger(0);
    private final AtomicInteger packetsPlayed = new AtomicInteger(0);
    private final AtomicInteger packetsLate = new AtomicInteger(0);
    private final AtomicInteger packetsDropped = new AtomicInteger(0);
    private final AtomicLong totalJitter = new AtomicLong(0);

    // Jitter calculation
    private long lastArrivalTime = 0;
    private long lastPacketTimestamp = 0;

    public JitterBuffer(String clientId) {
        this.clientId = clientId;
        this.buffer = new ConcurrentSkipListMap<>();
        this.lastPlayoutTime = System.currentTimeMillis();
    }

    /**
     * Add packet to jitter buffer
     * 
     * @param seq       Sequence number
     * @param timestamp Packet timestamp (milliseconds)
     * @param data      Audio data
     */
    public void addPacket(long seq, long timestamp, byte[] data) {
        packetsReceived.incrementAndGet();

        // Calculate jitter
        long currentTime = System.currentTimeMillis();
        if (lastArrivalTime > 0 && lastPacketTimestamp > 0) {
            long arrivalDelta = currentTime - lastArrivalTime;
            long timestampDelta = timestamp - lastPacketTimestamp;
            long jitter = Math.abs(arrivalDelta - timestampDelta);
            totalJitter.addAndGet(jitter);

            // Adapt buffer size based on jitter
            adaptBufferSize(jitter);
        }
        lastArrivalTime = currentTime;
        lastPacketTimestamp = timestamp;

        // Add to buffer
        VoicePacket packet = new VoicePacket(seq, timestamp, data, currentTime);
        buffer.put(seq, packet);

        // Cleanup old packets (older than 1 second)
        buffer.headMap(seq - 50).clear(); // Keep last 50 packets max
    }

    /**
     * Get next packet ready for playout
     * Returns null if no packet is ready yet (waiting for jitter buffer)
     */
    public byte[] getNextPacket() {
        if (buffer.isEmpty()) {
            return null;
        }

        long currentTime = System.currentTimeMillis();

        // Check if enough time has passed since last playout
        long timeSinceLastPlayout = currentTime - lastPlayoutTime;
        if (timeSinceLastPlayout < 20) { // Minimum 20ms between packets
            return null;
        }

        // Get oldest packet in buffer
        Long firstSeq = buffer.firstKey();
        if (firstSeq == null) {
            return null;
        }

        VoicePacket packet = buffer.get(firstSeq);

        // Check if packet has been buffered long enough
        long timeInBuffer = currentTime - packet.arrivalTime;
        if (timeInBuffer < bufferDelayMs) {
            // Not ready yet, wait for jitter buffer to fill
            return null;
        }

        // Remove and return packet
        buffer.remove(firstSeq);
        packetsPlayed.incrementAndGet();
        lastPlayoutTime = currentTime;
        nextExpectedSeq = firstSeq + 1;

        return packet.data;
    }

    /**
     * Adapt buffer size based on observed jitter
     */
    private void adaptBufferSize(long jitter) {
        if (jitter > bufferDelayMs * 0.8) {
            // High jitter, increase buffer
            bufferDelayMs = Math.min(MAX_BUFFER_MS, bufferDelayMs + 10);
        } else if (jitter < bufferDelayMs * 0.3) {
            // Low jitter, decrease buffer for lower latency
            bufferDelayMs = Math.max(MIN_BUFFER_MS, bufferDelayMs - 5);
        }
    }

    /**
     * Check if buffer has packets ready
     */
    public boolean hasReadyPackets() {
        if (buffer.isEmpty()) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        Long firstSeq = buffer.firstKey();
        if (firstSeq == null) {
            return false;
        }

        VoicePacket packet = buffer.get(firstSeq);
        long timeInBuffer = currentTime - packet.arrivalTime;

        return timeInBuffer >= bufferDelayMs;
    }

    /**
     * Get buffer size (number of packets)
     */
    public int getBufferSize() {
        return buffer.size();
    }

    /**
     * Get current buffer delay in milliseconds
     */
    public long getBufferDelayMs() {
        return bufferDelayMs;
    }

    /**
     * Get average jitter
     */
    public double getAverageJitter() {
        int received = packetsReceived.get();
        if (received <= 1) {
            return 0.0;
        }
        return (double) totalJitter.get() / (received - 1);
    }

    /**
     * Get metrics
     */
    public JitterMetrics getMetrics() {
        return new JitterMetrics(
                packetsReceived.get(),
                packetsPlayed.get(),
                packetsLate.get(),
                packetsDropped.get(),
                buffer.size(),
                bufferDelayMs,
                getAverageJitter());
    }

    /**
     * Print statistics
     */
    public void printStats() {
        System.out.println("\n=== Jitter Buffer Stats [" + clientId + "] ===");
        System.out.println("Packets Received: " + packetsReceived.get());
        System.out.println("Packets Played: " + packetsPlayed.get());
        System.out.println("Packets Late: " + packetsLate.get());
        System.out.println("Packets Dropped: " + packetsDropped.get());
        System.out.println("Current Buffer Size: " + buffer.size());
        System.out.println("Buffer Delay: " + bufferDelayMs + " ms");
        System.out.printf("Average Jitter: %.2f ms%n", getAverageJitter());
        System.out.println("===========================================\n");
    }

    /**
     * Clear buffer
     */
    public void clear() {
        buffer.clear();
        nextExpectedSeq = 0;
        lastPlayoutTime = System.currentTimeMillis();
    }

    /**
     * Voice packet wrapper
     */
    private static class VoicePacket {
        final long seq;
        final long timestamp;
        final byte[] data;
        final long arrivalTime;

        VoicePacket(long seq, long timestamp, byte[] data, long arrivalTime) {
            this.seq = seq;
            this.timestamp = timestamp;
            this.data = data;
            this.arrivalTime = arrivalTime;
        }
    }

    /**
     * Metrics data class
     */
    public static class JitterMetrics {
        public final int packetsReceived;
        public final int packetsPlayed;
        public final int packetsLate;
        public final int packetsDropped;
        public final int currentBufferSize;
        public final long bufferDelayMs;
        public final double averageJitter;

        JitterMetrics(int packetsReceived, int packetsPlayed, int packetsLate,
                int packetsDropped, int currentBufferSize, long bufferDelayMs,
                double averageJitter) {
            this.packetsReceived = packetsReceived;
            this.packetsPlayed = packetsPlayed;
            this.packetsLate = packetsLate;
            this.packetsDropped = packetsDropped;
            this.currentBufferSize = currentBufferSize;
            this.bufferDelayMs = bufferDelayMs;
            this.averageJitter = averageJitter;
        }
    }
}

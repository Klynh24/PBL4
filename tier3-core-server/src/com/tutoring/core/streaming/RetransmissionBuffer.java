package com.tutoring.core.streaming;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ RESTORED: Retransmission Buffer for NACK Support
 * 
 * Stores recent UDP packets for potential retransmission
 * Used when client sends NACK (Negative Acknowledgement) for missing packets
 * 
 * USAGE:
 * - Server stores each packet after sending
 * - Client detects missing packet → sends NACK via TCP
 * - Server retrieves packet from buffer → resends via UDP
 * 
 * CONFIGURATION:
 * - Buffer size: 500 packets (~700 KB)
 * - Retention: Packets automatically removed when buffer full
 * - Strategy: Remove oldest first (FIFO)
 */
public class RetransmissionBuffer {
    private final Map<Integer, byte[]> recentPackets = new ConcurrentHashMap<>();
    private static final int MAX_BUFFER_SIZE = 500; // ~700KB (1400 bytes × 500)
    
    // Statistics
    private long packetsStored = 0;
    private long packetsRetrieved = 0;
    private long packetsExpired = 0;
    
    /**
     * Store a packet for potential retransmission
     * 
     * @param seqNum Sequence number (unique packet identifier)
     * @param packet Packet data (including 28-byte header)
     */
    public void storePacket(int seqNum, byte[] packet) {
        // Store packet
        recentPackets.put(seqNum, packet.clone()); // Clone to prevent external modification
        packetsStored++;
        
        // Trim oldest packets if buffer too large
        if (recentPackets.size() > MAX_BUFFER_SIZE) {
            int minSeq = recentPackets.keySet().stream()
                .min(Integer::compare)
                .orElse(seqNum);
            
            recentPackets.remove(minSeq);
            packetsExpired++;
        }
    }
    
    /**
     * Retrieve a packet by sequence number (for retransmission)
     * 
     * @param seqNum Sequence number to retrieve
     * @return Packet data, or null if not in buffer
     */
    public byte[] getPacket(int seqNum) {
        byte[] packet = recentPackets.get(seqNum);
        
        if (packet != null) {
            packetsRetrieved++;
            return packet.clone(); // Clone to prevent external modification
        }
        
        return null;
    }
    
    /**
     * Clear all packets from buffer
     */
    public void clear() {
        recentPackets.clear();
    }
    
    /**
     * Get buffer statistics
     */
    public BufferStats getStats() {
        return new BufferStats(
            recentPackets.size(),
            packetsStored,
            packetsRetrieved,
            packetsExpired
        );
    }
    
    /**
     * Print buffer statistics
     */
    public void printStats() {
        BufferStats stats = getStats();
        System.out.println(String.format(
            "[RetransmissionBuffer] Size: %d, Stored: %d, Retrieved: %d, Expired: %d, Hit Rate: %.1f%%",
            stats.currentSize, stats.totalStored, stats.totalRetrieved, stats.totalExpired,
            stats.getHitRate() * 100
        ));
    }
    
    /**
     * Statistics container
     */
    public static class BufferStats {
        public final int currentSize;
        public final long totalStored;
        public final long totalRetrieved;
        public final long totalExpired;
        
        public BufferStats(int currentSize, long totalStored, long totalRetrieved, long totalExpired) {
            this.currentSize = currentSize;
            this.totalStored = totalStored;
            this.totalRetrieved = totalRetrieved;
            this.totalExpired = totalExpired;
        }
        
        public double getHitRate() {
            if (totalRetrieved == 0) {
                return 0.0;
            }
            return (double) totalRetrieved / (totalRetrieved + totalExpired);
        }
    }
}


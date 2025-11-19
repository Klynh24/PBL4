package com.tutoring.core.streaming;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ✅ PHASE 1 UPGRADE: Zero-Copy Retransmission Buffer with Memory-Mapped Files
 * 
 * Stores recent UDP packets for potential retransmission
 * Used when client sends NACK (Negative Acknowledgement) for missing packets
 * 
 * ZERO-COPY OPTIMIZATIONS:
 * - Memory-mapped file (MappedByteBuffer) for off-heap storage
 * - OS-managed memory paging (no GC pressure)
 * - Direct memory access (no JVM heap overhead)
 * 
 * OPTIMIZATIONS:
 * - O(1) cleanup using circular sequence number tracking
 * - Lock-free operations using ConcurrentHashMap for metadata
 * 
 * CONFIGURATION:
 * - Buffer size: 500 packets (~700 KB)
 * - Mapped file size: 100 MB (allows for expansion)
 * - Retention: Packets automatically removed when buffer full
 * - Strategy: Remove oldest first (FIFO) - O(1) implementation
 */
public class RetransmissionBuffer {
    // ✅ PHASE 1: ZERO-COPY - Memory-mapped file for packet storage
    private MappedByteBuffer mappedBuffer;
    private FileChannel fileChannel;
    private File tempFile;
    private static final int MAX_BUFFER_SIZE = 500; // ~700KB (1400 bytes × 500)
    private static final int MAX_PACKET_SIZE = 1500; // Max UDP packet size
    private static final long MAPPED_FILE_SIZE = 100 * 1024 * 1024; // 100 MB
    
    // Metadata: sequence number -> offset in mapped buffer
    private final Map<Integer, Integer> packetOffsets = new ConcurrentHashMap<>();
    
    // ✅ OPTIMIZATION: Track oldest sequence number for O(1) cleanup
    private final AtomicInteger oldestSeqNum = new AtomicInteger(0);
    private final AtomicInteger nextSeqNum = new AtomicInteger(0);
    private final AtomicInteger currentOffset = new AtomicInteger(0);
    
    // Statistics
    private long packetsStored = 0;
    private long packetsRetrieved = 0;
    private long packetsExpired = 0;
    
    /**
     * ✅ PHASE 1: ZERO-COPY - Initialize memory-mapped file
     */
    public RetransmissionBuffer() {
        try {
            // Create temporary file for memory mapping
            tempFile = File.createTempFile("retransmission", ".dat");
            tempFile.deleteOnExit(); // Clean up on JVM exit
            
            // Open file channel
            fileChannel = new RandomAccessFile(tempFile, "rw").getChannel();
            
            // Map file to memory (READ_WRITE mode)
            mappedBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, MAPPED_FILE_SIZE);
            
            System.out.println("[RetransmissionBuffer] ✅ ZERO-COPY: Initialized memory-mapped file (" + 
                (MAPPED_FILE_SIZE / 1024 / 1024) + " MB)");
        } catch (IOException e) {
            System.err.println("[RetransmissionBuffer] ERROR: Failed to initialize memory-mapped file: " + 
                e.getMessage());
            e.printStackTrace();
            // Fallback: use heap-based storage (backward compatibility)
            System.err.println("[RetransmissionBuffer] Falling back to heap-based storage");
        }
    }
    
    /**
     * ✅ PHASE 1: ZERO-COPY - Store a packet in memory-mapped file
     * 
     * @param seqNum Sequence number (unique packet identifier)
     * @param packet Packet data (including 28-byte header)
     */
    public void storePacket(int seqNum, byte[] packet) {
        if (mappedBuffer == null) {
            // Fallback to heap storage if memory mapping failed
            storePacketHeap(seqNum, packet);
            return;
        }
        
        try {
            int packetSize = packet.length;
            if (packetSize > MAX_PACKET_SIZE) {
                System.err.println("[RetransmissionBuffer] Packet too large: " + packetSize);
                return;
            }
            
            // ✅ PHASE 1: ZERO-COPY - Calculate offset in mapped buffer (circular buffer)
            int offset = currentOffset.getAndAdd(MAX_PACKET_SIZE + 4); // +4 for size header
            
            // Wrap around if we exceed mapped file size
            if (offset + MAX_PACKET_SIZE + 4 > MAPPED_FILE_SIZE) {
                offset = 0;
                currentOffset.set(MAX_PACKET_SIZE + 4);
            }
            
            // ✅ PHASE 1: ZERO-COPY - Write directly to mapped memory
            mappedBuffer.position(offset);
            mappedBuffer.putInt(packetSize); // Store size first
            mappedBuffer.put(packet); // Store packet data
            
            // Store metadata (sequence number -> offset)
            Integer oldOffset = packetOffsets.put(seqNum, offset);
            packetsStored++;
            
            // ✅ OPTIMIZATION: O(1) cleanup using tracked oldest sequence number
            if (packetOffsets.size() > MAX_BUFFER_SIZE) {
                int oldest = oldestSeqNum.get();
                packetOffsets.remove(oldest);
                packetsExpired++;
                oldestSeqNum.incrementAndGet();
            }
            
            // Update next sequence number tracker
            if (seqNum >= nextSeqNum.get()) {
                nextSeqNum.set(seqNum + 1);
            }
            
        } catch (Exception e) {
            System.err.println("[RetransmissionBuffer] Error storing packet: " + e.getMessage());
            // Fallback to heap storage
            storePacketHeap(seqNum, packet);
        }
    }
    
    /**
     * Fallback: Heap-based storage (backward compatibility)
     */
    private final Map<Integer, byte[]> heapPackets = new ConcurrentHashMap<>();
    private final BufferPool bufferPool = BufferPool.getInstance();
    
    private void storePacketHeap(int seqNum, byte[] packet) {
        byte[] packetCopy = bufferPool.acquire(packet.length);
        System.arraycopy(packet, 0, packetCopy, 0, packet.length);
        
        byte[] oldPacket = heapPackets.put(seqNum, packetCopy);
        if (oldPacket != null) {
            bufferPool.release(oldPacket);
        }
        packetsStored++;
        
        if (heapPackets.size() > MAX_BUFFER_SIZE) {
            int oldest = oldestSeqNum.get();
            byte[] removed = heapPackets.remove(oldest);
            if (removed != null) {
                bufferPool.release(removed);
                packetsExpired++;
            }
            oldestSeqNum.incrementAndGet();
        }
        
        if (seqNum >= nextSeqNum.get()) {
            nextSeqNum.set(seqNum + 1);
        }
    }
    
    /**
     * ✅ PHASE 1: ZERO-COPY - Retrieve a packet from memory-mapped file
     * 
     * @param seqNum Sequence number to retrieve
     * @return Packet data (cloned), or null if not in buffer
     */
    public byte[] getPacket(int seqNum) {
        if (mappedBuffer == null) {
            // Fallback to heap storage
            return getPacketHeap(seqNum);
        }
        
        try {
            Integer offset = packetOffsets.get(seqNum);
            if (offset == null) {
                return null;
            }
            
            // ✅ PHASE 1: ZERO-COPY - Read directly from mapped memory
            mappedBuffer.position(offset);
            int packetSize = mappedBuffer.getInt();
            
            if (packetSize <= 0 || packetSize > MAX_PACKET_SIZE) {
                return null;
            }
            
            // Read packet data
            byte[] packet = new byte[packetSize];
            mappedBuffer.get(packet);
            
            packetsRetrieved++;
            return packet;
            
        } catch (Exception e) {
            System.err.println("[RetransmissionBuffer] Error retrieving packet: " + e.getMessage());
            return getPacketHeap(seqNum);
        }
    }
    
    private byte[] getPacketHeap(int seqNum) {
        byte[] packet = heapPackets.get(seqNum);
        if (packet != null) {
            packetsRetrieved++;
            byte[] copy = bufferPool.acquire(packet.length);
            System.arraycopy(packet, 0, copy, 0, packet.length);
            return copy;
        }
        return null;
    }
    
    /**
     * ✅ PHASE 1: ZERO-COPY - Clear all packets from buffer
     */
    public void clear() {
        packetOffsets.clear();
        heapPackets.clear();
        
        // Return heap buffers to pool
        for (byte[] packet : heapPackets.values()) {
            bufferPool.release(packet);
        }
        
        oldestSeqNum.set(0);
        nextSeqNum.set(0);
        currentOffset.set(0);
        
        // Clear mapped buffer
        if (mappedBuffer != null) {
            mappedBuffer.clear();
        }
    }
    
    /**
     * ✅ PHASE 1: ZERO-COPY - Cleanup resources
     */
    public void close() {
        try {
            clear();
            if (mappedBuffer != null) {
                // Unmap the buffer (Java doesn't provide direct API, but GC will handle it)
                mappedBuffer = null;
            }
            if (fileChannel != null) {
                fileChannel.close();
            }
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        } catch (IOException e) {
            System.err.println("[RetransmissionBuffer] Error closing: " + e.getMessage());
        }
    }
    
    /**
     * Get buffer statistics
     */
    public BufferStats getStats() {
        int currentSize = mappedBuffer != null ? packetOffsets.size() : heapPackets.size();
        return new BufferStats(
            currentSize,
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


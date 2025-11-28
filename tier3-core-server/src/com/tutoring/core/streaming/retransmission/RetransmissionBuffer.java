package com.tutoring.core.streaming.retransmission;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RetransmissionBuffer {
    private MappedByteBuffer mappedBuffer;
    private FileChannel fileChannel;
    private File tempFile;
    private static final int MAX_BUFFER_SIZE = 500; // ~700KB (1400 bytes × 500)
    private static final int MAX_PACKET_SIZE = 1500; // Max UDP packet size
    private static final long MAPPED_FILE_SIZE = 100 * 1024 * 1024; // 100 MB

    private final Map<Integer, Integer> packetOffsets = new ConcurrentHashMap<>();

    private final AtomicInteger oldestSeqNum = new AtomicInteger(0);
    private final AtomicInteger nextSeqNum = new AtomicInteger(0);
    private final AtomicInteger currentOffset = new AtomicInteger(0);

    private long packetsStored = 0;
    private long packetsRetrieved = 0;
    private long packetsExpired = 0;

    public RetransmissionBuffer() {
        try {
            tempFile = File.createTempFile("retransmission", ".dat");
            tempFile.deleteOnExit();

            fileChannel = new RandomAccessFile(tempFile, "rw").getChannel();

            mappedBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, MAPPED_FILE_SIZE);

            System.out.println("[RetransmissionBuffer] ✅ ZERO-COPY: Initialized memory-mapped file (" +
                    (MAPPED_FILE_SIZE / 1024 / 1024) + " MB)");
        } catch (IOException e) {
            System.err.println("[RetransmissionBuffer] ERROR: Failed to initialize memory-mapped file: " +
                    e.getMessage());
            e.printStackTrace();
            System.err.println("[RetransmissionBuffer] Falling back to heap-based storage");
        }
    }

    public void storePacket(int seqNum, byte[] packet) {
        if (mappedBuffer == null) {
            storePacketHeap(seqNum, packet);
            return;
        }

        try {
            int packetSize = packet.length;
            if (packetSize > MAX_PACKET_SIZE) {
                System.err.println("[RetransmissionBuffer] Packet too large: " + packetSize);
                return;
            }

            int offset = currentOffset.getAndAdd(MAX_PACKET_SIZE + 4); // +4 for size header

            if (offset + MAX_PACKET_SIZE + 4 > MAPPED_FILE_SIZE) {
                offset = 0;
                currentOffset.set(MAX_PACKET_SIZE + 4);
            }

            mappedBuffer.position(offset);
            mappedBuffer.putInt(packetSize); // Store size first
            mappedBuffer.put(packet); // Store packet data

            Integer oldOffset = packetOffsets.put(seqNum, offset);
            packetsStored++;

            if (packetOffsets.size() > MAX_BUFFER_SIZE) {
                int oldest = oldestSeqNum.get();
                packetOffsets.remove(oldest);
                packetsExpired++;
                oldestSeqNum.incrementAndGet();
            }

            if (seqNum >= nextSeqNum.get()) {
                nextSeqNum.set(seqNum + 1);
            }

        } catch (Exception e) {
            System.err.println("[RetransmissionBuffer] Error storing packet: " + e.getMessage());
            storePacketHeap(seqNum, packet);
        }
    }

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

    public byte[] getPacket(int seqNum) {
        if (mappedBuffer == null) {
            return getPacketHeap(seqNum);
        }

        try {
            Integer offset = packetOffsets.get(seqNum);
            if (offset == null) {
                return null;
            }

            mappedBuffer.position(offset);
            int packetSize = mappedBuffer.getInt();

            if (packetSize <= 0 || packetSize > MAX_PACKET_SIZE) {
                return null;
            }

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

    public void clear() {
        packetOffsets.clear();
        heapPackets.clear();

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

    public void close() {
        try {
            clear();
            if (mappedBuffer != null) {
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

    public BufferStats getStats() {
        int currentSize = mappedBuffer != null ? packetOffsets.size() : heapPackets.size();
        return new BufferStats(
                currentSize,
                packetsStored,
                packetsRetrieved,
                packetsExpired);
    }

    public void printStats() {
        BufferStats stats = getStats();
        System.out.println(String.format(
                "[RetransmissionBuffer] Size: %d, Stored: %d, Retrieved: %d, Expired: %d, Hit Rate: %.1f%%",
                stats.currentSize, stats.totalStored, stats.totalRetrieved, stats.totalExpired,
                stats.getHitRate() * 100));
    }

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

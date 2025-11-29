package com.tutoring.proxy;

import java.util.ArrayList;
import java.util.List;

/**
 * ✅ FEC XOR ENCODER - Forward Error Correction
 * 
 * Proxy Server version - Tạo FEC packets từ data packets
 * Giống với Core Server version nhưng trong package riêng
 */
public class FecXorEncoder {
    private static final int DEFAULT_FEC_COUNT = 2;
    private static final int DEFAULT_GROUP_SIZE = 10;
    
    private final int fecCount;
    private final int groupSize;
    
    public FecXorEncoder() {
        this(DEFAULT_GROUP_SIZE, DEFAULT_FEC_COUNT);
    }
    
    public FecXorEncoder(int groupSize, int fecCount) {
        if (groupSize < 1 || fecCount < 1 || fecCount >= groupSize) {
            throw new IllegalArgumentException("Invalid FEC parameters: groupSize=" + 
                groupSize + ", fecCount=" + fecCount);
        }
        this.groupSize = groupSize;
        this.fecCount = fecCount;
    }
    
    /**
     * Tạo FEC packets từ một group data packets
     */
    public List<byte[]> encode(List<byte[]> dataPackets) {
        if (dataPackets == null || dataPackets.isEmpty()) {
            return new ArrayList<>();
        }
        
        int maxSize = findMaxPacketSize(dataPackets);
        List<byte[]> fecPackets = new ArrayList<>(fecCount);
        
        for (int fecIdx = 0; fecIdx < fecCount; fecIdx++) {
            byte[] fec = new byte[maxSize];
            
            for (int dataIdx = 0; dataIdx < dataPackets.size(); dataIdx++) {
                if (shouldInclude(dataIdx, fecIdx)) {
                    byte[] dataPacket = dataPackets.get(dataIdx);
                    xorBytes(fec, dataPacket);
                }
            }
            
            fecPackets.add(fec);
        }
        
        return fecPackets;
    }
    
    private boolean shouldInclude(int dataIdx, int fecIdx) {
        return (dataIdx % fecCount) == fecIdx;
    }
    
    private void xorBytes(byte[] target, byte[] source) {
        int minLen = Math.min(target.length, source.length);
        for (int i = 0; i < minLen; i++) {
            target[i] ^= source[i];
        }
    }
    
    private int findMaxPacketSize(List<byte[]> packets) {
        int maxSize = 0;
        for (byte[] packet : packets) {
            if (packet != null && packet.length > maxSize) {
                maxSize = packet.length;
            }
        }
        return maxSize;
    }
    
    public int getFecCount() {
        return fecCount;
    }
    
    public int getGroupSize() {
        return groupSize;
    }
}


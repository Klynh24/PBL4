package streaming.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ✅ FEC XOR DECODER - Recover lost packets using FEC
 * 
 * Sử dụng FEC packets để recover packets bị mất mà không cần NACK
 */
public class FecXorDecoder {
    private final int fecCount; // Số FEC packets
    private final int groupSize; // Số data packets trong 1 group
    
    public FecXorDecoder(int groupSize, int fecCount) {
        if (groupSize < 1 || fecCount < 1 || fecCount >= groupSize) {
            throw new IllegalArgumentException("Invalid FEC parameters: groupSize=" + 
                groupSize + ", fecCount=" + fecCount);
        }
        this.groupSize = groupSize;
        this.fecCount = fecCount;
    }
    
    /**
     * Recover một packet bị mất
     * 
     * @param receivedPackets List of received packets (null = lost packet)
     * @param fecPackets List of FEC packets
     * @param lostIndex Index của packet bị mất
     * @return Recovered packet, hoặc null nếu không thể recover
     */
    public byte[] recoverPacket(List<byte[]> receivedPackets, 
                               List<byte[]> fecPackets,
                               int lostIndex) {
        if (receivedPackets == null || fecPackets == null) {
            return null;
        }
        
        if (lostIndex < 0 || lostIndex >= receivedPackets.size()) {
            return null;
        }
        
        // Tìm FEC packet phù hợp (cùng pattern với lost packet)
        int fecIdx = lostIndex % fecCount;
        
        if (fecIdx >= fecPackets.size() || fecPackets.get(fecIdx) == null) {
            return null; // Không có FEC packet tương ứng
        }
        
        byte[] fecPacket = fecPackets.get(fecIdx);
        
        // Bắt đầu với FEC packet
        int maxSize = findMaxSize(receivedPackets, fecPacket);
        byte[] recovered = new byte[maxSize];
        System.arraycopy(fecPacket, 0, recovered, 0, Math.min(fecPacket.length, maxSize));
        
        // XOR với tất cả received packets (trừ lost packet)
        for (int i = 0; i < receivedPackets.size(); i++) {
            if (i == lostIndex) {
                continue; // Skip lost packet
            }
            
            byte[] packet = receivedPackets.get(i);
            if (packet != null && shouldInclude(i, fecIdx)) {
                xorBytes(recovered, packet);
            }
        }
        
        return recovered;
    }
    
    /**
     * Recover nhiều packets bị mất (nếu có đủ FEC packets)
     * 
     * @param receivedPackets List of received packets (null = lost)
     * @param fecPackets List of FEC packets
     * @param lostIndices Set of lost packet indices
     * @return Map of recovered packets (index -> packet), empty nếu không thể recover hết
     */
    public java.util.Map<Integer, byte[]> recoverMultiplePackets(
            List<byte[]> receivedPackets,
            List<byte[]> fecPackets,
            Set<Integer> lostIndices) {
        
        java.util.Map<Integer, byte[]> recovered = new java.util.HashMap<>();
        
        if (lostIndices.size() > fecPackets.size()) {
            return recovered; // Không đủ FEC để recover
        }
        
        // Recover từng packet một
        for (Integer lostIndex : lostIndices) {
            byte[] recoveredPacket = recoverPacket(receivedPackets, fecPackets, lostIndex);
            if (recoveredPacket != null) {
                recovered.put(lostIndex, recoveredPacket);
            }
        }
        
        return recovered;
    }
    
    /**
     * Kiểm tra xem có thể recover packet này không
     */
    private boolean shouldInclude(int dataIdx, int fecIdx) {
        return (dataIdx % fecCount) == fecIdx;
    }
    
    /**
     * XOR byte array vào target (in-place)
     */
    private void xorBytes(byte[] target, byte[] source) {
        int minLen = Math.min(target.length, source.length);
        for (int i = 0; i < minLen; i++) {
            target[i] ^= source[i];
        }
    }
    
    /**
     * Tìm kích thước lớn nhất
     */
    private int findMaxSize(List<byte[]> packets, byte[] fecPacket) {
        int maxSize = fecPacket != null ? fecPacket.length : 0;
        for (byte[] packet : packets) {
            if (packet != null && packet.length > maxSize) {
                maxSize = packet.length;
            }
        }
        return maxSize;
    }
}


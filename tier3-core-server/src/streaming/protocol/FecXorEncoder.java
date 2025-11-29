package streaming.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * ✅ FEC XOR ENCODER - Forward Error Correction
 * 
 * Tạo FEC packets (redundancy) từ data packets để recover packets bị mất mà không cần NACK
 * 
 * Algorithm:
 * - Mỗi group N data packets → tạo M FEC packets
 * - FEC packets = XOR của các data packets theo pattern
 * - Pattern: FEC1 XOR odd indices, FEC2 XOR even indices
 * - Có thể recover M packets bị mất trong 1 group
 * 
 * Ví dụ với 10 packets, 2 FEC:
 * - FEC1 = D1 XOR D3 XOR D5 XOR D7 XOR D9 (odd indices)
 * - FEC2 = D2 XOR D4 XOR D6 XOR D8 XOR D10 (even indices)
 * - Nếu mất D5 → recover = FEC1 XOR D1 XOR D3 XOR D7 XOR D9
 */
public class FecXorEncoder {
    private static final int DEFAULT_FEC_COUNT = 2; // 2 FEC packets per group
    private static final int DEFAULT_GROUP_SIZE = 10; // 10 data packets per group
    
    private final int fecCount; // Số FEC packets tạo ra
    private final int groupSize; // Số data packets trong 1 group
    
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
     * 
     * @param dataPackets List of data packets (must have same size or smaller)
     * @return List of FEC packets (size = fecCount)
     */
    public List<byte[]> encode(List<byte[]> dataPackets) {
        if (dataPackets == null || dataPackets.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Find maximum packet size
        int maxSize = findMaxPacketSize(dataPackets);
        
        // Create FEC packets
        List<byte[]> fecPackets = new ArrayList<>(fecCount);
        
        for (int fecIdx = 0; fecIdx < fecCount; fecIdx++) {
            byte[] fec = new byte[maxSize];
            
            // XOR các data packets theo pattern
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
    
    /**
     * Kiểm tra xem data packet index có nên được XOR vào FEC packet này không
     * Pattern: Round-robin distribution
     * - FEC0: indices 0, fecCount, 2*fecCount, ...
     * - FEC1: indices 1, 1+fecCount, 1+2*fecCount, ...
     * - FEC2: indices 2, 2+fecCount, 2+2*fecCount, ...
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
     * Tìm kích thước packet lớn nhất
     */
    private int findMaxPacketSize(List<byte[]> packets) {
        int maxSize = 0;
        for (byte[] packet : packets) {
            if (packet != null && packet.length > maxSize) {
                maxSize = packet.length;
            }
        }
        return maxSize;
    }
    
    /**
     * Get FEC overhead percentage
     */
    public double getOverheadPercentage() {
        return ((double) fecCount / groupSize) * 100.0;
    }
    
    public int getFecCount() {
        return fecCount;
    }
    
    public int getGroupSize() {
        return groupSize;
    }
    
    /**
     * Calculate optimal FEC count based on expected packet loss
     * 
     * @param expectedPacketLoss Expected packet loss percentage (0.0 - 1.0)
     * @param groupSize Size of packet group
     * @return Recommended FEC count
     */
    public static int calculateOptimalFecCount(double expectedPacketLoss, int groupSize) {
        // Rule of thumb: FEC count = expected_loss * groupSize + 1
        // But cap at reasonable limits
        int fecCount = (int) Math.ceil(expectedPacketLoss * groupSize) + 1;
        
        // Cap at 25% overhead (reasonable limit)
        int maxFec = Math.max(1, groupSize / 4);
        fecCount = Math.min(fecCount, maxFec);
        
        // Minimum 1, maximum groupSize-1
        return Math.max(1, Math.min(fecCount, groupSize - 1));
    }
}


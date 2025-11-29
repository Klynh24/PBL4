package streaming.protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ✅ HIGH PRIORITY: Forward Error Correction (FEC)
 * 
 * Problem: Only NACK-based retransmission, requires round-trip time
 * Solution: XOR-based FEC - can recover lost packets without retransmission
 * 
 * Benefits:
 * - Lower latency (no round-trip for recovery)
 * - Better for real-time voice/video
 * - Reduces retransmission overhead
 * - Particularly effective for 1-2 packet losses
 * 
 * Algorithm: Simple XOR FEC
 * - For every N packets, generate 1 FEC packet
 * - FEC packet = XOR of all N packets
 * - Can recover 1 lost packet per group
 */
public class ForwardErrorCorrection {
    private static final int FEC_GROUP_SIZE = 8; // 8 packets + 1 FEC = 12.5% overhead
    private static final int MAX_PACKET_SIZE = 65536;

    /**
     * Generate FEC packets for a group of data packets
     * 
     * @param packets List of data packets
     * @return FEC packet (XOR of all input packets)
     */
    public static byte[] generateFEC(List<byte[]> packets) {
        if (packets == null || packets.isEmpty()) {
            return new byte[0];
        }

        // Find maximum packet size
        int maxSize = 0;
        for (byte[] packet : packets) {
            if (packet.length > maxSize) {
                maxSize = packet.length;
            }
        }

        // Create FEC packet (XOR of all packets)
        byte[] fecPacket = new byte[maxSize];

        for (byte[] packet : packets) {
            for (int i = 0; i < packet.length; i++) {
                fecPacket[i] ^= packet[i];
            }
        }

        return fecPacket;
    }

    /**
     * Generate FEC packet from array of packets
     */
    public static byte[] generateFEC(byte[]... packets) {
        return generateFEC(Arrays.asList(packets));
    }

    /**
     * Recover lost packet using FEC
     * 
     * @param receivedPackets List of received packets (can contain nulls for lost
     *                        packets)
     * @param fecPacket       FEC packet
     * @param lostIndex       Index of lost packet to recover
     * @return Recovered packet, or null if cannot recover
     */
    public static byte[] recoverPacket(List<byte[]> receivedPackets, byte[] fecPacket, int lostIndex) {
        if (receivedPackets == null || fecPacket == null) {
            return null;
        }

        if (lostIndex < 0 || lostIndex >= receivedPackets.size()) {
            return null;
        }

        // Start with FEC packet
        byte[] recovered = Arrays.copyOf(fecPacket, fecPacket.length);

        // XOR with all received packets (except the lost one)
        for (int i = 0; i < receivedPackets.size(); i++) {
            if (i == lostIndex) {
                continue; // Skip lost packet
            }

            byte[] packet = receivedPackets.get(i);
            if (packet != null) {
                for (int j = 0; j < Math.min(packet.length, recovered.length); j++) {
                    recovered[j] ^= packet[j];
                }
            }
        }

        return recovered;
    }

    /**
     * FEC Group Manager - manages FEC groups for a stream
     */
    public static class FECGroupManager {
        private final List<byte[]> currentGroup = new ArrayList<>();
        private int groupCount = 0;

        /**
         * Add packet to current group
         * Returns FEC packet if group is complete, null otherwise
         */
        public byte[] addPacket(byte[] packet) {
            if (packet == null) {
                return null;
            }

            currentGroup.add(packet);

            if (currentGroup.size() >= FEC_GROUP_SIZE) {
                // Group complete, generate FEC
                byte[] fecPacket = generateFEC(currentGroup);

                // Reset for next group
                currentGroup.clear();
                groupCount++;

                return fecPacket;
            }

            return null; // Group not complete yet
        }

        /**
         * Force generate FEC for current group (even if incomplete)
         */
        public byte[] flush() {
            if (currentGroup.isEmpty()) {
                return null;
            }

            byte[] fecPacket = generateFEC(currentGroup);
            currentGroup.clear();
            groupCount++;

            return fecPacket;
        }

        /**
         * Get current group size
         */
        public int getCurrentGroupSize() {
            return currentGroup.size();
        }

        /**
         * Get total FEC groups generated
         */
        public int getGroupCount() {
            return groupCount;
        }

        /**
         * Reset manager
         */
        public void reset() {
            currentGroup.clear();
            groupCount = 0;
        }
    }

    /**
     * FEC Recovery Manager - manages packet recovery for a stream
     */
    public static class FECRecoveryManager {
        private final List<byte[]> currentGroup = new ArrayList<>();
        private byte[] currentFEC = null;
        private int recoveredPackets = 0;

        /**
         * Add received packet to current group
         */
        public void addPacket(byte[] packet) {
            currentGroup.add(packet);
        }

        /**
         * Set FEC packet for current group
         */
        public void setFEC(byte[] fecPacket) {
            currentFEC = fecPacket;
        }

        /**
         * Try to recover lost packet
         * 
         * @param lostIndex Index of lost packet in current group
         * @return Recovered packet, or null if cannot recover
         */
        public byte[] tryRecover(int lostIndex) {
            if (currentFEC == null) {
                return null; // No FEC available
            }

            byte[] recovered = recoverPacket(currentGroup, currentFEC, lostIndex);

            if (recovered != null) {
                recoveredPackets++;
            }

            return recovered;
        }

        /**
         * Complete current group and start new one
         */
        public void nextGroup() {
            currentGroup.clear();
            currentFEC = null;
        }

        /**
         * Get total recovered packets
         */
        public int getRecoveredCount() {
            return recoveredPackets;
        }

        /**
         * Reset manager
         */
        public void reset() {
            currentGroup.clear();
            currentFEC = null;
            recoveredPackets = 0;
        }
    }

    /**
     * Calculate FEC overhead percentage
     */
    public static double calculateOverhead(int groupSize) {
        return (1.0 / groupSize) * 100.0;
    }

    /**
     * Get default FEC group size
     */
    public static int getDefaultGroupSize() {
        return FEC_GROUP_SIZE;
    }
}

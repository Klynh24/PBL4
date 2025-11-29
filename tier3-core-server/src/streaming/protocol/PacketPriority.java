package streaming.protocol;

/**
 * ✅ PACKET PRIORITIZATION
 * 
 * Defines priority levels for different packet types to reduce latency for critical data
 * 
 * Priority Levels:
 * - CRITICAL (0): Voice packets, NACK responses, control messages - lowest latency required
 * - HIGH (1): Keyframes - needed for video sync, but less time-sensitive than voice
 * - MEDIUM (2): Delta frames - can tolerate slight delay
 * - LOW (3): FEC packets - redundant data, lowest priority
 */
public enum PacketPriority {
    CRITICAL(0),    // Voice (mediaType=1), NACK responses, control
    HIGH(1),        // Keyframes (frameType=0x01)
    MEDIUM(2),      // Delta frames (frameType=0x02)
    LOW(3);         // FEC packets (frameType=0xF0-0xFF)
    
    private final int level;
    
    PacketPriority(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
    
    /**
     * Determine priority from packet header
     * 
     * @param packetData Raw packet data (must have at least 28 bytes header)
     * @return PacketPriority based on packet type
     */
    public static PacketPriority fromPacket(byte[] packetData) {
        if (packetData == null || packetData.length < 28) {
            return MEDIUM; // Default fallback
        }
        
        // Check if FEC packet (byte 27, bit 0)
        boolean isFec = (packetData[27] & 0x01) != 0;
        if (isFec) {
            return LOW;
        }
        
        // Check frame type (byte 16)
        byte frameType = packetData[16];
        
        // Check if keyframe
        if (frameType == 0x01) {
            return HIGH;
        }
        
        // Check if delta frame
        if (frameType == 0x02) {
            return MEDIUM;
        }
        
        // Check media type (byte 28, if packet is advanced format)
        if (packetData.length > 28) {
            byte mediaType = packetData[28];
            if (mediaType == 1) { // VOICE
                return CRITICAL;
            }
        }
        
        return MEDIUM; // Default
    }
    
    /**
     * Determine priority from media type and frame type
     * 
     * @param mediaType Media type (1=VOICE, 2=SCREEN)
     * @param frameType Frame type (0x01=keyframe, 0x02=delta, 0xF0-0xFF=FEC)
     * @param isFecPacket Whether this is an FEC packet
     * @return PacketPriority
     */
    public static PacketPriority fromTypes(byte mediaType, byte frameType, boolean isFecPacket) {
        if (isFecPacket) {
            return LOW;
        }
        
        if (mediaType == 1) { // VOICE
            return CRITICAL;
        }
        
        if (frameType == 0x01) { // KEYFRAME
            return HIGH;
        }
        
        if (frameType == 0x02) { // DELTA
            return MEDIUM;
        }
        
        return MEDIUM; // Default
    }
}


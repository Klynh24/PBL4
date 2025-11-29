package com.tutoring.proxy;

/**
 * ✅ PACKET PRIORITIZATION
 * 
 * Proxy Server version - Same priority logic as Core Server
 */
public enum PacketPriority {
    CRITICAL(0),    // Voice (mediaType=1)
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
     */
    public static PacketPriority fromPacket(byte[] packetData) {
        if (packetData == null || packetData.length < 28) {
            return MEDIUM;
        }
        
        // Check if FEC packet (byte 27, bit 0)
        boolean isFec = (packetData[27] & 0x01) != 0;
        if (isFec) {
            return LOW;
        }
        
        // Check frame type (byte 16)
        byte frameType = packetData[16];
        
        if (frameType == 0x01) { // Keyframe
            return HIGH;
        }
        
        if (frameType == 0x02) { // Delta
            return MEDIUM;
        }
        
        // Check media type (byte 28, if packet is advanced format)
        if (packetData.length > 28) {
            byte mediaType = packetData[28];
            if (mediaType == 1) { // VOICE
                return CRITICAL;
            }
        }
        
        return MEDIUM;
    }
    
    /**
     * Determine priority from media type and frame type
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
        
        return MEDIUM;
    }
}


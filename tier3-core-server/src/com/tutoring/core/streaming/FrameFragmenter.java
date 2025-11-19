package com.tutoring.core.streaming;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ✅ RESTORED: Frame Fragmenter for Core Server
 * 
 * Fragments large frames into MTU-safe UDP packets
 * Implements 28-byte header structure for reliable transmission
 * 
 * Header Structure (28 bytes):
 * 0-3:   Magic Number (0x53435245 - 'SCRE')
 * 4-7:   Sequence Number (global)
 * 8-11:  Frame ID
 * 12-13: Fragment Index
 * 14-15: Total Fragments
 * 16:    Frame Type (0x01 = keyframe, 0x02 = delta)
 * 17-24: Timestamp (milliseconds)
 * 25-26: Payload Length
 * 27:    Reserved
 * 
 * IDENTICAL to Proxy Server's FrameFragmenter for compatibility
 */
public class FrameFragmenter {
    private static final int MAX_PAYLOAD_SIZE = 1372; // 1400 MTU - 28 header
    private static final int HEADER_SIZE = 28;
    private static final int MAGIC_NUMBER = 0x53435245; // 'SCRE'
    
    private final AtomicInteger globalSequenceNumber = new AtomicInteger(0);
    
    /**
     * Fragment a frame into multiple UDP packets
     * 
     * @param frameId Unique frame identifier
     * @param frameType 0x01 = keyframe, 0x02 = delta frame
     * @param frameData Complete frame data to fragment
     * @return List of UDP packet byte arrays ready to send
     */
    public List<byte[]> fragmentFrame(int frameId, byte frameType, byte[] frameData) {
        List<byte[]> fragments = new ArrayList<>();
        
        if (frameData == null || frameData.length == 0) {
            return fragments;
        }
        
        // Calculate total fragments needed
        int totalFragments = (int) Math.ceil((double) frameData.length / MAX_PAYLOAD_SIZE);
        
        // Fragment the data
        for (int i = 0; i < totalFragments; i++) {
            int offset = i * MAX_PAYLOAD_SIZE;
            int length = Math.min(MAX_PAYLOAD_SIZE, frameData.length - offset);
            
            byte[] packet = createPacket(frameId, i, totalFragments, frameType,
                                        frameData, offset, length);
            fragments.add(packet);
        }
        
        return fragments;
    }
    
    /**
     * Create a UDP packet with 28-byte header + payload
     */
    private byte[] createPacket(int frameId, int fragmentIndex, 
                                int totalFragments, byte frameType,
                                byte[] data, int offset, int length) {
        byte[] packet = new byte[HEADER_SIZE + length];
        ByteBuffer buffer = ByteBuffer.wrap(packet);
        
        // Write header (28 bytes)
        buffer.putInt(MAGIC_NUMBER);                           // 0-3: Magic
        buffer.putInt(globalSequenceNumber.getAndIncrement()); // 4-7: Sequence
        buffer.putInt(frameId);                                // 8-11: Frame ID
        buffer.putShort((short) fragmentIndex);                // 12-13: Fragment index
        buffer.putShort((short) totalFragments);               // 14-15: Total fragments
        buffer.put(frameType);                                 // 16: Frame type
        buffer.putLong(System.currentTimeMillis());            // 17-24: Timestamp
        buffer.putShort((short) length);                       // 25-26: Payload length
        buffer.put((byte) 0);                                  // 27: Reserved
        
        // Write payload (starting at byte 28)
        buffer.put(data, offset, length);
        
        return packet;
    }
    
    /**
     * Get current sequence number (for monitoring)
     */
    public int getSequenceNumber() {
        return globalSequenceNumber.get();
    }
    
    /**
     * Reset sequence number (for testing)
     */
    public void resetSequence() {
        globalSequenceNumber.set(0);
    }
}


package com.tutoring.proxy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fragments large WebSocket binary messages into MTU-safe UDP packets
 * Implements the 28-byte header structure compatible with Core Server
 */
public class FrameFragmenter {
    private static final int MAX_PAYLOAD_SIZE = 1372; // 1400 MTU - 28 header
    private static final int HEADER_SIZE = 28;
    private static final int MAGIC_NUMBER = 0x53435245; // 'SCRE'
    
    private final AtomicInteger globalSequenceNumber = new AtomicInteger(0);
    private final AtomicInteger frameIdCounter = new AtomicInteger(0);
    
    /**
     * Fragment a binary WebSocket message into multiple UDP packets
     * 
     * @param messageData The complete binary data from WebSocket
     * @return List of UDP packet byte arrays ready to send
     */
    public List<byte[]> fragmentMessage(byte[] messageData) {
        List<byte[]> fragments = new ArrayList<>();
        
        if (messageData == null || messageData.length == 0) {
            return fragments;
        }
        
        // Generate unique frame ID for this message
        int frameId = frameIdCounter.getAndIncrement();
        
        // Calculate total fragments needed
        int totalFragments = (int) Math.ceil((double) messageData.length / MAX_PAYLOAD_SIZE);
        
        // Fragment the data
        for (int i = 0; i < totalFragments; i++) {
            int offset = i * MAX_PAYLOAD_SIZE;
            int length = Math.min(MAX_PAYLOAD_SIZE, messageData.length - offset);
            
            byte[] packet = createPacket(frameId, i, totalFragments, 
                                        messageData, offset, length);
            fragments.add(packet);
        }
        
        return fragments;
    }
    
    /**
     * Create a UDP packet with 28-byte header + payload
     * 
     * Header Structure (28 bytes):
     * 0-3:   Magic Number (0x53435245)
     * 4-7:   Sequence Number (global)
     * 8-11:  Frame ID
     * 12-13: Fragment Index
     * 14-15: Total Fragments
     * 16:    Frame Type (0x01 = keyframe)
     * 17-24: Timestamp (milliseconds)
     * 25-26: Payload Length
     * 27:    Reserved (0x00)
     */
    private byte[] createPacket(int frameId, int fragmentIndex, 
                                int totalFragments, byte[] data, 
                                int offset, int length) {
        byte[] packet = new byte[HEADER_SIZE + length];
        ByteBuffer buffer = ByteBuffer.wrap(packet);
        
        // Write header (28 bytes)
        buffer.putInt(MAGIC_NUMBER);                           // 0-3: Magic
        buffer.putInt(globalSequenceNumber.getAndIncrement()); // 4-7: Sequence
        buffer.putInt(frameId);                                // 8-11: Frame ID
        buffer.putShort((short) fragmentIndex);                // 12-13: Fragment index
        buffer.putShort((short) totalFragments);               // 14-15: Total fragments
        buffer.put((byte) 0x01);                               // 16: Frame type (keyframe)
        buffer.putLong(System.currentTimeMillis());            // 17-24: Timestamp
        buffer.putShort((short) length);                       // 25-26: Payload length
        buffer.put((byte) 0);                                  // 27: Reserved
        
        // Write payload (starting at byte 28)
        buffer.put(data, offset, length);
        
        return packet;
    }
    
    /**
     * Get statistics for monitoring
     */
    public int getSequenceNumber() {
        return globalSequenceNumber.get();
    }
    
    public int getFrameIdCounter() {
        return frameIdCounter.get();
    }
}


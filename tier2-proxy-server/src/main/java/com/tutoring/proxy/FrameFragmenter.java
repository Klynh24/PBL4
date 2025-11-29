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
    
    // ✅ NEW: FEC flags
    private static final byte FLAG_IS_FEC = (byte) 0x01;
    private static final byte FRAME_TYPE_FEC = (byte) 0xF0;
    
    private final AtomicInteger globalSequenceNumber = new AtomicInteger(0);
    private final AtomicInteger frameIdCounter = new AtomicInteger(0);
    
    // ✅ NEW: FEC encoder
    private FecXorEncoder fecEncoder;
    private final boolean enableFec;
    
    public FrameFragmenter() {
        this(false);
    }
    
    public FrameFragmenter(boolean enableFec) {
        this.enableFec = enableFec;
        if (enableFec) {
            this.fecEncoder = new FecXorEncoder(10, 2); // 10 packets, 2 FEC
        }
    }
    
    public FrameFragmenter(boolean enableFec, int groupSize, int fecCount) {
        this.enableFec = enableFec;
        if (enableFec) {
            this.fecEncoder = new FecXorEncoder(groupSize, fecCount);
        }
    }
    
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
        
        // ✅ NEW: Generate FEC packets if enabled
        if (enableFec && fecEncoder != null && fragments.size() > 1) {
            List<byte[]> fecPackets = generateFecPackets(fragments, frameId);
            fragments.addAll(fecPackets);
        }
        
        return fragments;
    }
    
    /**
     * ✅ NEW: Generate FEC packets from data packets
     */
    private List<byte[]> generateFecPackets(List<byte[]> dataPackets, int frameId) {
        if (fecEncoder == null || dataPackets.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Extract payloads (skip 28-byte header)
        List<byte[]> payloads = new ArrayList<>(dataPackets.size());
        for (byte[] packet : dataPackets) {
            if (packet.length > HEADER_SIZE) {
                byte[] payload = new byte[packet.length - HEADER_SIZE];
                System.arraycopy(packet, HEADER_SIZE, payload, 0, payload.length);
                payloads.add(payload);
            } else {
                payloads.add(new byte[0]);
            }
        }
        
        // Generate FEC packets
        List<byte[]> fecPayloads = fecEncoder.encode(payloads);
        
        // Wrap FEC packets with header
        List<byte[]> fecPacketsWithHeader = new ArrayList<>(fecPayloads.size());
        long timestamp = System.currentTimeMillis();
        byte originalFrameType = 0x01; // Keyframe (default for proxy server)
        
        for (int i = 0; i < fecPayloads.size(); i++) {
            byte[] fecPayload = fecPayloads.get(i);
            byte[] fecPacket = createFecPacket(frameId, i, fecPayloads.size(), 
                                             originalFrameType, fecPayload, timestamp);
            fecPacketsWithHeader.add(fecPacket);
        }
        
        return fecPacketsWithHeader;
    }
    
    /**
     * ✅ NEW: Create FEC packet with header
     */
    private byte[] createFecPacket(int frameId, int fecIndex, int totalFec, 
                                   byte originalFrameType, byte[] fecData, long timestamp) {
        byte[] packet = new byte[HEADER_SIZE + fecData.length];
        ByteBuffer buffer = ByteBuffer.wrap(packet);
        
        buffer.putInt(MAGIC_NUMBER);
        buffer.putInt(globalSequenceNumber.getAndIncrement());
        buffer.putInt(frameId);
        buffer.putShort((short) fecIndex);
        buffer.putShort((short) totalFec);
        buffer.put((byte)(FRAME_TYPE_FEC | originalFrameType));
        buffer.putLong(timestamp);
        buffer.putShort((short) fecData.length);
        buffer.put(FLAG_IS_FEC);
        
        buffer.put(fecData);
        
        return packet;
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
     * 27:    Flags (bit 0: isFEC, bit 1-7: reserved)
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
        buffer.put((byte) 0);                                  // 27: Flags (0 = data packet)
        
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


package com.tutoring.core.streaming;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

/**
 * ✅ RESTORED: Streaming Broadcaster
 * 
 * Handles broadcasting fragmented frames to multiple clients
 * Integrates with:
 * - FrameFragmenter: Creates packets
 * - RetransmissionBuffer: Stores for NACK
 * - ClientStreamState: Tracks per-client needs
 * 
 * FEATURES:
 * - Selective broadcasting (keyframe to new clients, delta to existing)
 * - Packet buffering for retransmission
 * - Per-client state management
 */
public class StreamingBroadcaster {
    private final DatagramSocket udpSocket;
    private final RetransmissionBuffer retransmissionBuffer;
    private final FrameFragmenter fragmenter;
    
    public StreamingBroadcaster(DatagramSocket udpSocket, 
                               RetransmissionBuffer retransmissionBuffer) {
        this.udpSocket = udpSocket;
        this.retransmissionBuffer = retransmissionBuffer;
        this.fragmenter = new FrameFragmenter();
    }
    
    /**
     * Broadcast frame to multiple clients
     * 
     * @param frameId Frame identifier
     * @param frameType 0x01 = keyframe, 0x02 = delta
     * @param frameData Encoded frame data
     * @param recipients Set of client UDP addresses
     */
    public void broadcastFrame(int frameId, byte frameType, byte[] frameData,
                              Set<InetSocketAddress> recipients) {
        
        // Fragment frame into packets
        List<byte[]> fragments = fragmenter.fragmentFrame(frameId, frameType, frameData);
        
        // Send each fragment to all recipients
        for (byte[] packet : fragments) {
            // Store in retransmission buffer
            // Extract sequence number from packet (bytes 4-7)
            int seqNum = ((packet[4] & 0xFF) << 24) |
                        ((packet[5] & 0xFF) << 16) |
                        ((packet[6] & 0xFF) << 8) |
                        (packet[7] & 0xFF);
            
            retransmissionBuffer.storePacket(seqNum, packet);
            
            // Broadcast to all recipients
            for (InetSocketAddress recipient : recipients) {
                try {
                    DatagramPacket udpPacket = new DatagramPacket(
                        packet,
                        packet.length,
                        recipient
                    );
                    udpSocket.send(udpPacket);
                } catch (IOException e) {
                    System.err.println("[Broadcaster] Error sending to " + recipient + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Broadcast keyframe to specific client (for new joiners)
     */
    public void sendKeyframeToClient(int frameId, byte[] keyframeData, 
                                    InetSocketAddress recipient) {
        List<byte[]> fragments = fragmenter.fragmentFrame(frameId, (byte)0x01, keyframeData);
        
        for (byte[] packet : fragments) {
            try {
                DatagramPacket udpPacket = new DatagramPacket(
                    packet,
                    packet.length,
                    recipient
                );
                udpSocket.send(udpPacket);
            } catch (IOException e) {
                System.err.println("[Broadcaster] Error sending keyframe: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get fragmenter (for statistics)
     */
    public FrameFragmenter getFragmenter() {
        return fragmenter;
    }
}


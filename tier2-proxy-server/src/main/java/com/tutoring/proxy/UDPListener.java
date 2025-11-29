package com.tutoring.proxy;

import javax.websocket.Session;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Listens for UDP media packets from Core Server and forwards to WebSocket client
 */
public class UDPListener implements Runnable {
    private final ConnectionContext context;
    private static final int BUFFER_SIZE = 65536;
    
    public UDPListener(ConnectionContext context) {
        this.context = context;
    }
    
    @Override
    public void run() {
        DatagramSocket socket = context.getUdpSocket();
        Session session = context.getWsSession();
        System.out.println("[UDP Listener] Started for session: " + session.getId());
        
        byte[] buffer = new byte[BUFFER_SIZE];
        
        try {
            while (!socket.isClosed() && session.isOpen()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                // Forward binary data to WebSocket client with priority
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                
                // ✅ NEW: Validate packet format before forwarding
                if (data.length >= 4) {
                    // Check magic number (0x53435245 = 'SCRE')
                    int magic = ((data[0] & 0xFF) << 24) |
                               ((data[1] & 0xFF) << 16) |
                               ((data[2] & 0xFF) << 8) |
                               (data[3] & 0xFF);
                    
                    if (magic != 0x53435245) {
                        // ✅ NEW: Log invalid magic number (but don't spam)
                        if (packet.getLength() % 1000 == 0 || packet.getLength() < 500) {
                            System.err.println("[UDP Listener] ⚠️ Invalid magic number: 0x" + 
                                Integer.toHexString(magic) + " (expected 0x53435245), packet size: " + 
                                packet.getLength() + ", first 8 bytes: " + 
                                bytesToHex(data, Math.min(8, data.length)));
                        }
                        // Still forward the packet (might be audio or other format)
                    }
                }
                
                // ✅ NEW: Determine priority from packet
                PacketPriority priority = PacketPriority.fromPacket(data);
                
                // ✅ NEW: Send via priority sender
                if (context.sendBinary(data, priority)) {
                    // Log periodically (first few packets for debugging)
                    if (packet.getLength() > 0 && packet.getLength() < 500) {
                        System.out.println("[UDP Listener " + session.getId() + "] Forwarded " + 
                            packet.getLength() + " bytes to client (priority: " + priority + ")");
                    }
                }
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("[UDP Listener] Error: " + e.getMessage());
            }
        } finally {
            System.out.println("[UDP Listener] Stopped for session: " + session.getId());
        }
    }
    
    /**
     * ✅ NEW: Helper method to convert bytes to hex string for debugging
     */
    private static String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length && i < bytes.length; i++) {
            sb.append(String.format("0x%02x ", bytes[i] & 0xFF));
        }
        return sb.toString().trim();
    }
}


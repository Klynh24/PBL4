package com.tutoring.proxy;

import javax.websocket.Session;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

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
                
                // Forward binary data to WebSocket client
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                
                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                session.getBasicRemote().sendBinary(byteBuffer);
                
                System.out.println("[UDP Listener " + session.getId() + "] Forwarded " + 
                    packet.getLength() + " bytes to client");
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("[UDP Listener] Error: " + e.getMessage());
            }
        } finally {
            System.out.println("[UDP Listener] Stopped for session: " + session.getId());
        }
    }
}


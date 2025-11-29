package com.tutoring.proxy;

import javax.websocket.Session;
import java.io.*;
import java.net.DatagramSocket;
import java.net.Socket;

/**
 * Maintains connection state for a single WebSocket session
 * Includes TCP and UDP connections to Core Server
 */
public class ConnectionContext {
    private final Session wsSession;
    private final Socket tcpSocket;
    private final DatagramSocket udpSocket;
    private final PrintWriter tcpOut;
    private final BufferedReader tcpIn;
    
    // Frame fragmenter for UDP (one per connection to maintain sequence numbers)
    private final FrameFragmenter fragmenter;
    
    // ✅ NEW: Priority sender for outgoing WebSocket binary messages
    private PrioritySender prioritySender;
    private Thread senderThread;
    
    public ConnectionContext(Session wsSession, Socket tcpSocket, DatagramSocket udpSocket) throws IOException {
        this.wsSession = wsSession;
        this.tcpSocket = tcpSocket;
        this.udpSocket = udpSocket;
        this.tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);
        this.tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
        // ✅ FEC ENABLED: Enable Forward Error Correction for better packet loss recovery
        // Configuration: 10 packets per group, 2 FEC packets (20% overhead)
        this.fragmenter = new FrameFragmenter(true, 10, 2);
        
        // ✅ NEW: Initialize priority sender
        this.prioritySender = new PrioritySender(wsSession);
        this.senderThread = new Thread(prioritySender, "PrioritySender-" + wsSession.getId());
        this.senderThread.setDaemon(true);
        this.senderThread.start();
    }
    
    public Session getWsSession() {
        return wsSession;
    }
    
    public Socket getTcpSocket() {
        return tcpSocket;
    }
    
    public DatagramSocket getUdpSocket() {
        return udpSocket;
    }
    
    public BufferedReader getTcpIn() {
        return tcpIn;
    }
    
    public FrameFragmenter getFragmenter() {
        return fragmenter;
    }
    
    /**
     * ✅ NEW: Send binary data with priority
     */
    public boolean sendBinary(byte[] data, PacketPriority priority) {
        if (prioritySender != null && prioritySender.send(data, priority)) {
            return true;
        }
        // Fallback: direct send if priority sender not available
        try {
            if (wsSession.isOpen()) {
                wsSession.getBasicRemote().sendBinary(java.nio.ByteBuffer.wrap(data));
                return true;
            }
        } catch (Exception e) {
            System.err.println("[Context] Error sending binary: " + e.getMessage());
        }
        return false;
    }
    
    public synchronized void sendTCP(String message) {
        tcpOut.println(message);
    }
    
    public void close() {
        try {
            // ✅ NEW: Shutdown priority sender
            if (prioritySender != null) {
                prioritySender.shutdown();
            }
            
            sendTCP("DISCONNECT");
            if (tcpSocket != null && !tcpSocket.isClosed()) {
                tcpSocket.close();
            }
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
            System.out.println("[Context] Closed connections for session: " + wsSession.getId());
        } catch (IOException e) {
            System.err.println("[Context] Error closing connections: " + e.getMessage());
        }
    }
}


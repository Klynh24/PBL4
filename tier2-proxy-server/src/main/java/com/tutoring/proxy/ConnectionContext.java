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
    
    public ConnectionContext(Session wsSession, Socket tcpSocket, DatagramSocket udpSocket) throws IOException {
        this.wsSession = wsSession;
        this.tcpSocket = tcpSocket;
        this.udpSocket = udpSocket;
        this.tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);
        this.tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
        this.fragmenter = new FrameFragmenter();
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
    
    public synchronized void sendTCP(String message) {
        tcpOut.println(message);
    }
    
    public void close() {
        try {
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


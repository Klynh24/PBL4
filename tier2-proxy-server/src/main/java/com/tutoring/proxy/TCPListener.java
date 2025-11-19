package com.tutoring.proxy;

import org.json.JSONObject;
import javax.websocket.Session;
import java.io.IOException;

/**
 * Listens for TCP messages from Core Server and forwards to WebSocket client
 */
public class TCPListener implements Runnable {
    private final ConnectionContext context;
    
    public TCPListener(ConnectionContext context) {
        this.context = context;
    }
    
    @Override
    public void run() {
        Session session = context.getWsSession();
        System.out.println("[TCP Listener] Started for session: " + session.getId());
        
        try {
            String message;
            while ((message = context.getTcpIn().readLine()) != null) {
                System.out.println("[TCP Listener " + session.getId() + "] Received from Core: " + message);
                
                // Translate Core Server message to JSON
                JSONObject json = WebSocketEndpoint.translateToClient(message);
                
                // Send to WebSocket client
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(json.toString());
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("[TCP Listener] Connection closed for session: " + session.getId());
        } finally {
            System.out.println("[TCP Listener] Stopped for session: " + session.getId());
        }
    }
}


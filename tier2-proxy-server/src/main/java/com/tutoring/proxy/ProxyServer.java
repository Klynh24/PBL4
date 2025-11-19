package com.tutoring.proxy;

import org.glassfish.tyrus.server.Server;
import javax.websocket.DeploymentException;

/**
 * Tier 2: WebSocket Proxy Server
 * Translates between WebSocket (from browser) and TCP/UDP (to Core Server)
 */
public class ProxyServer {
    private static final String HOST = "0.0.0.0";
    private static final int PORT = 8089;
    private static final String CONTEXT_PATH = "/";
    
    public static void main(String[] args) {
        Server server = new Server(HOST, PORT, CONTEXT_PATH, null, WebSocketEndpoint.class);
        
        try {
            System.out.println("====================================");
            System.out.println("  Tier 2: WebSocket Proxy Server   ");
            System.out.println("====================================");
            System.out.println("Starting WebSocket server on ws://" + HOST + ":" + PORT + "/connect");
            System.out.println("Waiting for connections...\n");
            
            server.start();
            
            // Keep server running
            Thread.currentThread().join();
            
        } catch (DeploymentException e) {
            System.err.println("Failed to deploy WebSocket endpoint: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("Server interrupted");
        } finally {
            server.stop();
        }
    }
}


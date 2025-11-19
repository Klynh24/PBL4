package com.tutoring.core.streaming;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitors network quality for all clients using NACK frequency
 * Provides per-client quality assessment based on packet loss rates
 * Thread-safe implementation for concurrent access
 */
public class NetworkQualityMonitor {
    // Monitoring configuration
    private static final int NACK_WINDOW_SECONDS = 10;  // 10-second sliding window
    private static final double POOR_THRESHOLD = 0.05;    // 5% packet loss = POOR
    private static final double CRITICAL_THRESHOLD = 0.15; // 15% packet loss = CRITICAL
    
    // Per-client network statistics
    private final Map<String, ClientNetworkStats> clientStats = new ConcurrentHashMap<>();
    
    /**
     * Record that a packet was sent to a specific client
     * Call this from UDPMediaHandler for every packet sent
     */
    public void recordPacketSent(String clientId) {
        ClientNetworkStats stats = getOrCreateStats(clientId);
        stats.incrementPacketsSent();
    }
    
    /**
     * Record NACK requests from a client
     * Call this from ClientHandler when NACK is received
     * 
     * @param clientId Client who sent the NACK
     * @param missedPacketCount Number of packets in NACK request
     */
    public void recordNACK(String clientId, int missedPacketCount) {
        ClientNetworkStats stats = getOrCreateStats(clientId);
        stats.recordNACKs(missedPacketCount);
        
        // Log significant packet loss
        if (missedPacketCount > 10) {
            System.out.println("[NetworkMonitor] High NACK from client " + clientId + 
                ": " + missedPacketCount + " packets");
        }
    }
    
    /**
     * Assess network quality for a specific client
     * 
     * @param clientId Client to assess
     * @return NetworkQuality classification
     */
    public NetworkQuality assessQuality(String clientId) {
        ClientNetworkStats stats = clientStats.get(clientId);
        if (stats == null) return NetworkQuality.UNKNOWN;
        
        double lossRate = stats.calculatePacketLossRate();
        
        // Classify based on thresholds
        if (lossRate >= CRITICAL_THRESHOLD) {
            return NetworkQuality.CRITICAL;  // > 15% loss
        } else if (lossRate >= POOR_THRESHOLD) {
            return NetworkQuality.POOR;      // 5-15% loss
        } else {
            return NetworkQuality.GOOD;      // < 5% loss
        }
    }
    
    /**
     * Get statistics for a client
     */
    public ClientNetworkStats getStats(String clientId) {
        return clientStats.get(clientId);
    }
    
    /**
     * Get all monitored client IDs
     */
    public Set<String> getAllClients() {
        return clientStats.keySet();
    }
    
    /**
     * Remove client from monitoring (on disconnect)
     */
    public void removeClient(String clientId) {
        ClientNetworkStats stats = clientStats.remove(clientId);
        if (stats != null) {
            stats.clear();
        }
    }
    
    /**
     * Get or create statistics object for a client
     */
    private ClientNetworkStats getOrCreateStats(String clientId) {
        return clientStats.computeIfAbsent(
            clientId, 
            k -> new ClientNetworkStats(NACK_WINDOW_SECONDS)
        );
    }
    
    /**
     * Print monitoring statistics (for debugging)
     */
    public void printStats() {
        System.out.println("\n=== Network Quality Statistics ===");
        System.out.println("Clients monitored: " + clientStats.size());
        
        for (Map.Entry<String, ClientNetworkStats> entry : clientStats.entrySet()) {
            String clientId = entry.getKey();
            ClientNetworkStats stats = entry.getValue();
            NetworkQuality quality = assessQuality(clientId);
            
            double lossRate = stats.calculatePacketLossRate() * 100;
            
            System.out.printf("  Client %s: %s (Loss: %.2f%%, Sent: %d, Lost: %d, Samples: %d)%n",
                clientId.substring(0, Math.min(8, clientId.length())),
                quality,
                lossRate,
                stats.getTotalPacketsSent(),
                stats.getTotalPacketsLost(),
                stats.getSampleCount()
            );
        }
        System.out.println("==================================\n");
    }
    
    /**
     * Clear all statistics (for testing)
     */
    public void clearAll() {
        clientStats.values().forEach(ClientNetworkStats::clear);
        clientStats.clear();
    }
}

package com.tutoring.core.streaming;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages quality levels for rooms based on network conditions
 * Implements "Worst Client Wins" policy: room quality matches poorest client
 * Thread-safe for concurrent access from evaluation thread
 */
public class QualityLevelManager {
    private final NetworkQualityMonitor networkMonitor;
    private final Map<String, QualityLevel> roomQualityLevels = new ConcurrentHashMap<>();
    
    public QualityLevelManager(NetworkQualityMonitor networkMonitor) {
        this.networkMonitor = networkMonitor;
    }
    
    /**
     * Evaluate and determine optimal quality level for a room
     * 
     * @param roomId Room to evaluate
     * @param clientIds All clients in the room
     * @return New quality level if changed, null if no change needed
     */
    public QualityLevel evaluateRoomQuality(String roomId, Set<String> clientIds) {
        if (clientIds == null || clientIds.isEmpty()) {
            return QualityLevel.HIGH;
        }
        
        // Assess each client's network quality
        NetworkQuality worstQuality = NetworkQuality.GOOD;
        String worstClientId = null;
        
        for (String clientId : clientIds) {
            NetworkQuality quality = networkMonitor.assessQuality(clientId);
            
            // Find worst quality (highest ordinal value)
            if (quality.ordinal() > worstQuality.ordinal()) {
                worstQuality = quality;
                worstClientId = clientId;
            }
        }
        
        // Map network quality to streaming quality level
        QualityLevel recommendedLevel = mapNetworkToQuality(worstQuality);
        
        // Get current quality level for room
        QualityLevel currentLevel = roomQualityLevels.getOrDefault(
            roomId, QualityLevel.HIGH
        );
        
        // Check if quality needs to change
        if (recommendedLevel != currentLevel) {
            System.out.println("[QualityManager] Room " + roomId + 
                " quality change: " + currentLevel + " -> " + recommendedLevel +
                (worstClientId != null ? " (worst client: " + 
                worstClientId.substring(0, Math.min(8, worstClientId.length())) + ")" : ""));
            
            roomQualityLevels.put(roomId, recommendedLevel);
            return recommendedLevel;
        }
        
        return null; // No change needed
    }
    
    /**
     * Map network quality to streaming quality level
     * Policy: Conservative mapping to ensure stability
     */
    private QualityLevel mapNetworkToQuality(NetworkQuality networkQuality) {
        switch (networkQuality) {
            case GOOD:
                return QualityLevel.HIGH;      // < 5% loss: Full HD
            case POOR:
                return QualityLevel.MEDIUM;    // 5-15% loss: HD
            case CRITICAL:
                return QualityLevel.LOW;       // > 15% loss: SD
            default:
                return QualityLevel.HIGH;      // Unknown: Assume good
        }
    }
    
    /**
     * Get current quality level for a room
     */
    public QualityLevel getCurrentQuality(String roomId) {
        return roomQualityLevels.getOrDefault(roomId, QualityLevel.HIGH);
    }
    
    /**
     * Set quality level for a room (manual override)
     */
    public void setQuality(String roomId, QualityLevel quality) {
        roomQualityLevels.put(roomId, quality);
    }
    
    /**
     * Remove room from tracking (on room close)
     */
    public void removeRoom(String roomId) {
        roomQualityLevels.remove(roomId);
    }
    
    /**
     * Get all tracked rooms
     */
    public Set<String> getAllRooms() {
        return roomQualityLevels.keySet();
    }
    
    /**
     * Print current quality levels for all rooms
     */
    public void printStats() {
        System.out.println("\n=== Quality Level Statistics ===");
        System.out.println("Rooms tracked: " + roomQualityLevels.size());
        
        for (Map.Entry<String, QualityLevel> entry : roomQualityLevels.entrySet()) {
            String roomId = entry.getKey();
            QualityLevel level = entry.getValue();
            
            System.out.printf("  Room %s: %s (Est. BW: %d KB/s)%n",
                roomId, level, level.getEstimatedBandwidth());
        }
        System.out.println("=================================\n");
    }
    
    /**
     * Clear all room quality levels (for testing)
     */
    public void clearAll() {
        roomQualityLevels.clear();
    }
}

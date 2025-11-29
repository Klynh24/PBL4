package streaming.quality;

import server.ClientHandler;
import management.RoomManager;

import java.util.Map;
import java.util.Set;

/**
 * Periodic task that evaluates network quality for all rooms
 * and triggers quality level changes when needed
 * ✅ WORKER PATTERN: Scheduled by ScheduledExecutorService (no internal loop)
 */
public class QualityEvaluationTask implements Runnable {
    private final QualityLevelManager qualityManager;
    private final RoomManager roomManager;
    private final Map<String, ClientHandler> clientHandlers;
    
    public QualityEvaluationTask(QualityLevelManager qualityManager,
                                RoomManager roomManager,
                                Map<String, ClientHandler> clientHandlers) {
        this.qualityManager = qualityManager;
        this.roomManager = roomManager;
        this.clientHandlers = clientHandlers;
    }
    
    @Override
    public void run() {
        // ✅ WORKER PATTERN: Single execution per schedule (no while loop)
        try {
            evaluateAllRooms();
        } catch (Exception e) {
            System.err.println("[QualityEvaluation] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Evaluate quality for all active rooms
     */
    private void evaluateAllRooms() {
        Set<String> rooms = roomManager.listRooms();
        
        if (rooms.isEmpty()) {
            return; // No rooms to evaluate
        }
        
        for (String roomId : rooms) {
            evaluateRoom(roomId);
        }
    }
    
    /**
     * Evaluate quality for a specific room
     */
    private void evaluateRoom(String roomId) {
        Set<String> members = roomManager.getRoomMembers(roomId);
        
        if (members == null || members.isEmpty()) {
            return; // Room is empty
        }
        
        // Evaluate quality for this room
        QualityLevel newQuality = qualityManager.evaluateRoomQuality(roomId, members);
        
        // If quality changed, notify all members
        if (newQuality != null) {
            notifyRoomMembers(roomId, newQuality);
        }
    }
    
    /**
     * Notify all members of a room about quality change
     * Sends SET_QUALITY command to all clients
     */
    private void notifyRoomMembers(String roomId, QualityLevel quality) {
        Set<String> members = roomManager.getRoomMembers(roomId);
        if (members == null || members.isEmpty()) return;
        
        String qualityMessage = "SET_QUALITY:" + quality.toProtocol();
        
        int notifiedCount = 0;
        for (String clientId : members) {
            ClientHandler handler = clientHandlers.get(clientId);
            if (handler != null) {
                handler.sendMessage(qualityMessage);
                notifiedCount++;
            }
        }
        
        System.out.println("[QualityEvaluation] Notified room " + roomId + 
            " to change quality to " + quality.name() + 
            " (" + notifiedCount + " clients)");
    }
    
    // ✅ WORKER PATTERN: No stop/isRunning methods needed
    // Task lifecycle is managed by ScheduledExecutorService
}

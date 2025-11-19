package com.tutoring.core.streaming;

import com.tutoring.core.ClientHandler;
import com.tutoring.core.RoomManager;

import java.util.Map;
import java.util.Set;

/**
 * Periodic task that evaluates network quality for all rooms
 * and triggers quality level changes when needed
 * Runs in a dedicated daemon thread
 */
public class QualityEvaluationTask implements Runnable {
    private final QualityLevelManager qualityManager;
    private final RoomManager roomManager;
    private final Map<String, ClientHandler> clientHandlers;
    
    private static final long EVALUATION_INTERVAL_MS = 5000; // Evaluate every 5 seconds
    private volatile boolean running = true;
    
    public QualityEvaluationTask(QualityLevelManager qualityManager,
                                RoomManager roomManager,
                                Map<String, ClientHandler> clientHandlers) {
        this.qualityManager = qualityManager;
        this.roomManager = roomManager;
        this.clientHandlers = clientHandlers;
    }
    
    @Override
    public void run() {
        System.out.println("[QualityEvaluation] Started (interval: " + 
            EVALUATION_INTERVAL_MS + "ms)");
        
        while (running) {
            try {
                evaluateAllRooms();
                Thread.sleep(EVALUATION_INTERVAL_MS);
            } catch (InterruptedException e) {
                System.out.println("[QualityEvaluation] Interrupted, stopping");
                break;
            } catch (Exception e) {
                System.err.println("[QualityEvaluation] Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("[QualityEvaluation] Stopped");
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
    
    /**
     * Stop the evaluation task
     */
    public void stop() {
        running = false;
    }
    
    /**
     * Check if task is running
     */
    public boolean isRunning() {
        return running;
    }
}

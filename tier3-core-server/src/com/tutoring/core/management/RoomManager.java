package com.tutoring.core.management;

import com.tutoring.core.model.ChatMessage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe room management
 * Maintains room state and member lists
 */
public class RoomManager {
    // Map: RoomName -> Set of ClientIDs
    private final ConcurrentHashMap<String, Set<String>> rooms;
    
    // Map: RoomName -> List of ChatMessages (chat history)
    private final ConcurrentHashMap<String, List<ChatMessage>> chatHistory;
    
    // Maximum number of messages to store per room
    private static final int MAX_HISTORY_SIZE = 50;
    
    public RoomManager() {
        this.rooms = new ConcurrentHashMap<>();
        this.chatHistory = new ConcurrentHashMap<>();
    }
    
    // ✅ OPTIMIZATION: Remove redundant synchronized (ConcurrentHashMap is already thread-safe)
    // Only synchronize when modifying the Set/List contents
    public boolean createRoom(String roomName, String creatorId) {
        // Use putIfAbsent for atomic operation
        Set<String> members = Collections.synchronizedSet(new HashSet<>());
        members.add(creatorId);
        
        Set<String> existing = rooms.putIfAbsent(roomName, members);
        if (existing != null) {
            return false; // Room already exists
        }
        
        // Initialize empty chat history for new room
        chatHistory.putIfAbsent(roomName, Collections.synchronizedList(new ArrayList<>()));
        
        return true;
    }
    
    public boolean joinRoom(String roomName, String clientId) {
        Set<String> members = rooms.computeIfAbsent(roomName, k -> {
            // Auto-create room if it doesn't exist
            chatHistory.putIfAbsent(k, Collections.synchronizedList(new ArrayList<>()));
            return Collections.synchronizedSet(new HashSet<>());
        });
        
        members.add(clientId);
        return true;
    }
    
    public void leaveRoom(String roomName, String clientId) {
        Set<String> members = rooms.get(roomName);
        if (members != null) {
            members.remove(clientId);
            
            // Remove empty rooms (check size atomically)
            if (members.isEmpty()) {
                // Double-check pattern: verify still empty after acquiring lock
                synchronized (members) {
                    if (members.isEmpty()) {
                        rooms.remove(roomName);
                        chatHistory.remove(roomName);
                        System.out.println("[RoomManager] Room " + roomName + " removed (empty)");
                    }
                }
            }
        }
    }
    
    public Set<String> getRoomMembers(String roomName) {
        return rooms.get(roomName);
    }
    
    public Set<String> listRooms() {
        return new HashSet<>(rooms.keySet());
    }
    
    public int getRoomSize(String roomName) {
        Set<String> members = rooms.get(roomName);
        return members != null ? members.size() : 0;
    }
    
    public boolean roomExists(String roomName) {
        return rooms.containsKey(roomName);
    }
    
    /**
     * Add a chat message to room history
     */
    public void addChatMessage(String roomName, String username, String message) {
        List<ChatMessage> history = chatHistory.get(roomName);
        if (history != null) {
            synchronized (history) {
                // Add new message
                history.add(new ChatMessage(username, message));
                
                // Trim history if it exceeds max size
                if (history.size() > MAX_HISTORY_SIZE) {
                    history.remove(0); // Remove oldest message
                }
            }
            
            System.out.println("[RoomManager] Stored message in " + roomName + 
                " (history size: " + history.size() + ")");
        }
    }
    
    /**
     * Get chat history for a room
     * Returns a copy to prevent concurrent modification
     */
    public List<ChatMessage> getChatHistory(String roomName) {
        List<ChatMessage> history = chatHistory.get(roomName);
        if (history != null) {
            synchronized (history) {
                return new ArrayList<>(history);
            }
        }
        return new ArrayList<>();
    }
}


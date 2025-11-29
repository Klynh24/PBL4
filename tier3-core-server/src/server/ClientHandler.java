package server;

import management.RoomManager;
import management.UserManager;
import model.ChatMessage;
import model.ClientStreamState;
import streaming.retransmission.RetransmissionBuffer;
import streaming.network.NetworkQualityMonitor;
import concurrency.pool.DirectBufferPool;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles individual client connections via TCP
 * Manages signaling, authentication, and room operations
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final RoomManager roomManager;
    private final UserManager userManager;
    private final ConcurrentHashMap<String, ClientHandler> clientHandlers;
    private final RetransmissionBuffer retransmissionBuffer;
    private final java.nio.channels.DatagramChannel udpChannel; // ✅ PHASE 1: ZERO-COPY - Use DatagramChannel
    private final NetworkQualityMonitor networkMonitor;
    private final DirectBufferPool bufferPool; // ✅ NEW: DirectByteBuffer pool

    private BufferedReader in;
    private PrintWriter out;

    private String clientId;
    private String username;
    private String currentRoom;
    private InetSocketAddress udpAddress;
    private ClientStreamState streamState;

    public ClientHandler(Socket socket, RoomManager roomManager, UserManager userManager,
            ConcurrentHashMap<String, ClientHandler> clientHandlers,
            RetransmissionBuffer retransmissionBuffer,
            java.nio.channels.DatagramChannel udpChannel,
            NetworkQualityMonitor networkMonitor) {
        this.socket = socket;
        this.roomManager = roomManager;
        this.userManager = userManager;
        this.clientHandlers = clientHandlers;
        this.retransmissionBuffer = retransmissionBuffer;
        this.udpChannel = udpChannel;
        this.networkMonitor = networkMonitor;
        this.bufferPool = DirectBufferPool.getInstance(); // ✅ NEW: Use buffer pool
        this.clientId = UUID.randomUUID().toString();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Add to client handlers
            clientHandlers.put(clientId, this);

            String message;
            while ((message = in.readLine()) != null) {
                handleMessage(message);
            }
        } catch (IOException e) {
            System.out.println("[ClientHandler] Connection closed: " + clientId);
        } finally {
            cleanup();
        }
    }

    // ✅ FIX: Ensure streams are properly closed
    private void closeStreams() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            // Ignore
        }
        if (out != null) {
            out.close();
        }
    }

    private void handleMessage(String message) {
        System.out.println("[ClientHandler " + clientId + "] Received: " + message);

        String[] parts = message.split(":", 2);
        if (parts.length < 1)
            return;

        String command = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "REGISTER_UDP":
                handleRegisterUDP(data);
                break;
            case "REGISTER":
                handleRegister(data);
                break;
            case "LOGIN":
                handleLogin(data);
                break;
            case "CREATE_ROOM":
                handleCreateRoom(data);
                break;
            case "JOIN_ROOM":
                handleJoinRoom(data);
                break;
            case "LEAVE_ROOM":
                handleLeaveRoom();
                break;
            case "LIST_ROOMS":
                handleListRooms();
                break;
            case "CHAT":
                handleChat(data);
                break;
            case "NACK":
                handleNACK(data);
                break;
            case "QUALITY_ACK":
                handleQualityAck(data);
                break;
            case "DISCONNECT":
                cleanup();
                break;
            default:
                sendMessage("ERROR:Unknown command");
        }
    }

    private void handleRegisterUDP(String portStr) {
        try {
            int port = Integer.parseInt(portStr);
            InetAddress address = socket.getInetAddress();
            udpAddress = new InetSocketAddress(address, port);
            System.out.println("[ClientHandler " + clientId + "] Registered UDP: " + udpAddress);
            sendMessage("UDP_REGISTERED:OK");
        } catch (NumberFormatException e) {
            sendMessage("ERROR:Invalid UDP port");
        }
    }

    private void handleRegister(String data) {
        String[] parts = data.split(":");
        if (parts.length < 2) {
            sendMessage("ERROR:Invalid register format");
            return;
        }

        String username = parts[0];
        String password = parts[1];

        if (userManager.registerUser(username, password)) {
            sendMessage("REGISTER_SUCCESS:" + username);
            System.out.println("[ClientHandler " + clientId + "] User registered: " + username);
        } else {
            sendMessage("ERROR:Username already exists");
        }
    }

    private void handleLogin(String data) {
        String[] parts = data.split(":");
        if (parts.length < 2) {
            sendMessage("ERROR:Invalid login format");
            return;
        }

        String username = parts[0];
        String password = parts[1];

        if (userManager.authenticateUser(username, password)) {
            this.username = username;
            sendMessage("LOGIN_SUCCESS:" + username);
            System.out.println("[ClientHandler " + clientId + "] User logged in: " + username);
        } else {
            sendMessage("ERROR:Invalid credentials");
        }
    }

    private void handleCreateRoom(String roomName) {
        if (username == null) {
            sendMessage("ERROR:Not logged in");
            return;
        }

        if (roomManager.createRoom(roomName, clientId)) {
            currentRoom = roomName;
            sendMessage("ROOM_CREATED:" + roomName);
            System.out.println("[ClientHandler " + clientId + "] Room created: " + roomName);
        } else {
            sendMessage("ERROR:Room already exists");
        }
    }

    private void handleJoinRoom(String roomName) {
        if (username == null) {
            sendMessage("ERROR:Not logged in");
            return;
        }

        if (roomManager.joinRoom(roomName, clientId)) {
            currentRoom = roomName;

            // Initialize stream state for advanced screen sharing
            if (streamState == null) {
                initializeStreamState();
            }

            sendMessage("ROOM_JOINED:" + roomName);
            System.out.println("[ClientHandler " + clientId + "] User joined room: " + roomName);

            // Send chat history to the new user
            sendChatHistory(roomName);

            // Notify others in the room
            broadcastToRoom(currentRoom, "USER_JOINED:" + username, true);
        } else {
            sendMessage("ERROR:Room does not exist");
        }
    }

    private void handleLeaveRoom() {
        if (currentRoom != null) {
            roomManager.leaveRoom(currentRoom, clientId);
            broadcastToRoom(currentRoom, "USER_LEFT:" + username, true);
            sendMessage("ROOM_LEFT:" + currentRoom);
            System.out.println("[ClientHandler " + clientId + "] User left room: " + currentRoom);
            currentRoom = null;
        }
    }

    private void handleListRooms() {
        Set<String> rooms = roomManager.listRooms();
        StringBuilder sb = new StringBuilder("ROOM_LIST:");
        for (String room : rooms) {
            int count = roomManager.getRoomSize(room);
            sb.append(room).append("(").append(count).append("),");
        }
        sendMessage(sb.toString());
    }

    private void handleChat(String data) {
        if (currentRoom == null) {
            sendMessage("ERROR:Not in a room");
            return;
        }

        // Store message in room history
        roomManager.addChatMessage(currentRoom, username, data);

        // Broadcast to all users in the room (excluding sender)
        String chatMessage = "CHAT:" + username + ":" + data;
        broadcastToRoom(currentRoom, chatMessage, true);
    }

    private void broadcastToRoom(String roomName, String message, boolean excludeSelf) {
        Set<String> members = roomManager.getRoomMembers(roomName);
        if (members == null)
            return;

        for (String memberId : members) {
            if (excludeSelf && memberId.equals(clientId))
                continue;

            ClientHandler handler = clientHandlers.get(memberId);
            if (handler != null) {
                handler.sendMessage(message);
            }
        }
    }

    /**
     * Send chat history to this client
     */
    private void sendChatHistory(String roomName) {
        List<ChatMessage> history = roomManager.getChatHistory(roomName);

        if (history.isEmpty()) {
            System.out.println("[ClientHandler " + clientId + "] No chat history for room: " + roomName);
            return;
        }

        System.out.println("[ClientHandler " + clientId + "] Sending " + history.size() +
                " chat messages to new user");

        // Send a special message to indicate chat history start
        sendMessage("CHAT_HISTORY_START:" + history.size());

        // Send each historical message
        for (ChatMessage msg : history) {
            sendMessage(msg.toProtocolString());
        }

        // Send message to indicate chat history end
        sendMessage("CHAT_HISTORY_END");
    }

    public synchronized void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public InetSocketAddress getUdpAddress() {
        return udpAddress;
    }

    public String getClientId() {
        return clientId;
    }

    public String getUsername() {
        return username;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    /**
     * Initialize stream state for advanced screen sharing
     */
    public void initializeStreamState() {
        this.streamState = new ClientStreamState(clientId);
    }

    /**
     * Get stream state for this client
     */
    public ClientStreamState getStreamState() {
        return streamState;
    }

    /**
     * Handle NACK (Negative Acknowledgement) for packet retransmission
     * Format: "frameId:seqNum1,seqNum2,seqNum3"
     */
    private void handleNACK(String data) {
        try {
            String[] parts = data.split(":", 2);
            if (parts.length < 2) {
                System.err.println("[NACK] Invalid format: " + data);
                return;
            }

            int frameId = Integer.parseInt(parts[0]);
            String[] seqNumStrs = parts[1].split(",");

            // ABR: Record NACK for quality monitoring
            networkMonitor.recordNACK(clientId, seqNumStrs.length);

            System.out.println("[NACK] Client " + clientId + " requesting " +
                    seqNumStrs.length + " packets for frame " + frameId);

            int retransmitted = 0;
            for (String seqStr : seqNumStrs) {
                try {
                    int seqNum = Integer.parseInt(seqStr.trim());

                    // Retrieve packet from buffer
                    byte[] packet = retransmissionBuffer.getPacket(seqNum);

                    if (packet != null) {
                        // Resend via UDP
                        sendUDPPacket(packet);
                        retransmitted++;
                    } else {
                        System.out.println("[NACK] Packet " + seqNum +
                                " not in buffer (too old)");
                    }
                } catch (NumberFormatException e) {
                    System.err.println("[NACK] Invalid sequence number: " + seqStr);
                }
            }

            System.out.println("[NACK] Retransmitted " + retransmitted + "/" +
                    seqNumStrs.length + " packets");

            // Track packet loss for client state
            if (streamState != null) {
                streamState.reportPacketLoss(seqNumStrs.length);
            }

        } catch (Exception e) {
            System.err.println("[NACK] Error handling NACK: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle QUALITY_ACK from client
     * Format: "LEVEL"
     */
    private void handleQualityAck(String data) {
        System.out.println("[QualityACK] Client " + clientId + " acknowledged quality change to " + data);
        // Could track acknowledgments here if needed
    }

    /**
     * ✅ REFACTORED: Send UDP packet to this client via DatagramChannel
     * Now uses DirectByteBuffer pool for zero-copy I/O
     */
    public void sendUDPPacket(byte[] packetData) {
        if (udpAddress == null) {
            System.err.println("[UDP] Cannot send, no UDP address for client " + clientId);
            return;
        }

        try {
            // ✅ NEW: Use DirectByteBuffer from pool (true zero-copy)
            java.nio.ByteBuffer buffer = bufferPool.acquireAndFill(packetData);
            udpChannel.send(buffer, udpAddress);
            bufferPool.release(buffer); // Return to pool

        } catch (Exception e) {
            System.err.println("[UDP] Error sending packet: " + e.getMessage());
        }
    }

    private void cleanup() {
        try {
            if (currentRoom != null) {
                handleLeaveRoom();
            }
            clientHandlers.remove(clientId);

            // ABR: Remove from network monitoring
            networkMonitor.removeClient(clientId);

            // ✅ FIX: Close streams before socket
            closeStreams();

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("[ClientHandler " + clientId + "] Cleaned up");
        } catch (IOException e) {
            System.err.println("[ClientHandler] Error during cleanup: " + e.getMessage());
        }
    }
}

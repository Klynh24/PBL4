package com.tutoring.core;

import com.tutoring.core.streaming.NetworkQualityMonitor;

import java.io.IOException;
import java.net.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles UDP media packets (voice and screen sharing)
 * Broadcasts received media to all room members except sender
 */
public class UDPMediaHandler implements Runnable {
    private final DatagramSocket socket;
    private final RoomManager roomManager;
    private final ConcurrentHashMap<String, ClientHandler> clientHandlers;
    private final NetworkQualityMonitor qualityMonitor;
    
    // Track media type per frame ID (frameId -> mediaType)
    private final ConcurrentHashMap<Integer, Byte> frameMediaTypes = new ConcurrentHashMap<>();

    private static final int BUFFER_SIZE = 65536; // 64KB buffer for media packets

    public UDPMediaHandler(DatagramSocket socket, RoomManager roomManager,
            ConcurrentHashMap<String, ClientHandler> clientHandlers,
            NetworkQualityMonitor qualityMonitor) {
        this.socket = socket;
        this.roomManager = roomManager;
        this.clientHandlers = clientHandlers;
        this.qualityMonitor = qualityMonitor;
    }

    @Override
    public void run() {
        System.out.println("[UDP Handler] Started listening on port " + socket.getLocalPort());
        System.out.println("[UDP Handler] Ready to receive UDP packets from Proxy Server");
        byte[] buffer = new byte[BUFFER_SIZE];
        
        long packetsReceived = 0;

        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                packetsReceived++;

                // ✅ PERFORMANCE: Reduce debug logging spam (only log first 5 packets + every 1000th)
                // At 10 FPS with 38 fragments/frame = 380 packets/sec → too many logs
                if (packetsReceived <= 5 || packetsReceived % 1000 == 0) {
                    System.out.println(String.format("[UDP Handler] Packet #%d received: %d bytes from %s:%d",
                        packetsReceived, packet.getLength(), 
                        packet.getAddress().getHostAddress(), packet.getPort()));
                }

                // Process the received packet
                handleMediaPacket(packet);

            } catch (IOException e) {
                System.err.println("[UDP Handler] Error receiving packet: " + e.getMessage());
            }
        }
    }

    private void handleMediaPacket(DatagramPacket receivedPacket) {
        try {
            byte[] data = receivedPacket.getData();
            int length = receivedPacket.getLength();
            InetSocketAddress sourceAddress = new InetSocketAddress(
                    receivedPacket.getAddress(),
                    receivedPacket.getPort());

            // Check if this is an advanced screen sharing packet (magic number 0x53435245)
            if (isValidPacket(data, length)) {
                // Advanced screen sharing packet - identify sender by source address
                String senderId = findClientByUdpAddress(sourceAddress);
                if (senderId == null) {
                    // Unknown sender - packet might be from unregistered client
                    System.out.println("[UDP Handler] Received advanced packet from unknown address: " +
                            sourceAddress + " (packet length: " + length + ")");
                    return;
                }

                // ✅ PERFORMANCE: Only log first few packets per client to reduce spam
                // Logging is handled in broadcastAdvancedPacketToRoom for completed frames

                // Get sender's current room
                ClientHandler senderHandler = clientHandlers.get(senderId);
                if (senderHandler == null)
                    return;

                String roomId = senderHandler.getCurrentRoom();
                if (roomId == null) {
                    // Sender not in a room
                    return;
                }

                // ✅ CRITICAL FIX: Extract fragment index and frame ID from header
                // Header: bytes 8-11 = Frame ID, bytes 12-13 = Fragment Index
                int frameId = ((data[8] & 0xFF) << 24) |
                             ((data[9] & 0xFF) << 16) |
                             ((data[10] & 0xFF) << 8) |
                             (data[11] & 0xFF);
                int fragmentIndex = ((data[12] & 0xFF) << 8) | (data[13] & 0xFF);
                
                // Extract media type ONLY from fragment 0 (first fragment)
                // Fragment 0 payload: [MediaType(1 byte)][MediaData...]
                byte mediaType = 2; // Default to screen (2)
                if (fragmentIndex == 0 && length > 28) {
                    mediaType = data[28]; // First byte of payload = media type
                    frameMediaTypes.put(frameId, mediaType); // Store for other fragments
                } else {
                    // For fragments > 0, get media type from stored map
                    Byte storedType = frameMediaTypes.get(frameId);
                    if (storedType != null) {
                        mediaType = storedType;
                    }
                }
                
                // Cleanup old frame IDs (prevent memory leak)
                if (frameMediaTypes.size() > 1000) {
                    frameMediaTypes.clear(); // Simple cleanup - can be improved
                }

                // Broadcast entire packet (with 28-byte header) to room members
                broadcastAdvancedPacketToRoom(roomId, senderId, mediaType, data, length);
                return;
            }

            // Legacy packet format:
            // [ClientID_Length(4bytes)][ClientID][RoomID_Length(4bytes)][RoomID][MediaType(1byte)][Data]
            if (length < 9)
                return; // Minimum packet size

            int offset = 0;

            // Extract ClientID length
            int clientIdLength = ((data[offset] & 0xFF) << 24) |
                    ((data[offset + 1] & 0xFF) << 16) |
                    ((data[offset + 2] & 0xFF) << 8) |
                    (data[offset + 3] & 0xFF);
            offset += 4;

            if (clientIdLength <= 0 || clientIdLength > 100 || offset + clientIdLength > length)
                return;

            // Extract ClientID
            String senderId = new String(data, offset, clientIdLength);
            offset += clientIdLength;

            // Extract RoomID length
            int roomIdLength = ((data[offset] & 0xFF) << 24) |
                    ((data[offset + 1] & 0xFF) << 16) |
                    ((data[offset + 2] & 0xFF) << 8) |
                    (data[offset + 3] & 0xFF);
            offset += 4;

            if (roomIdLength <= 0 || roomIdLength > 100 || offset + roomIdLength > length)
                return;

            // Extract RoomID
            String roomId = new String(data, offset, roomIdLength);
            offset += roomIdLength;

            // Extract Media Type
            if (offset >= length)
                return;
            byte mediaType = data[offset];
            offset++;

            // Extract actual media data
            int mediaDataLength = length - offset;
            if (mediaDataLength <= 0)
                return;

            // Broadcast to room members
            broadcastMediaToRoom(roomId, senderId, mediaType, data, offset, mediaDataLength);

        } catch (Exception e) {
            System.err.println("[UDP Handler] Error processing packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Find client ID by matching UDP source address to registered UDP addresses
     * 
     * FIXED: Enhanced matching logic to handle both:
     * 1. Web clients (via Proxy) - match by IP only
     * 2. Java clients (direct) - match by IP + port OR just IP if only one client from that IP
     */
    private String findClientByUdpAddress(InetSocketAddress sourceAddress) {
        // First try: exact match (IP + port)
        for (String clientId : clientHandlers.keySet()) {
            ClientHandler handler = clientHandlers.get(clientId);
            if (handler == null)
                continue;

            InetSocketAddress registeredAddress = handler.getUdpAddress();
            if (registeredAddress == null)
                continue;

            // Exact match (IP + port)
            if (registeredAddress.equals(sourceAddress)) {
                return clientId;
            }
        }
        
        // Second try: match by IP only (client might use different source port for sending)
        String matchedClientId = null;
        int matchCount = 0;
        
        for (String clientId : clientHandlers.keySet()) {
            ClientHandler handler = clientHandlers.get(clientId);
            if (handler == null)
                continue;

            InetSocketAddress registeredAddress = handler.getUdpAddress();
            if (registeredAddress == null)
                continue;

            // Match IP address only
            if (registeredAddress.getAddress().equals(sourceAddress.getAddress())) {
                matchedClientId = clientId;
                matchCount++;
            }
        }
        
        // Only return if there's exactly one match (avoid ambiguity)
        if (matchCount == 1) {
            System.out.println("[UDP Handler] Matched client by IP only: " + matchedClientId + 
                " (source: " + sourceAddress + " != registered port)");
            return matchedClientId;
        } else if (matchCount > 1) {
            System.err.println("[UDP Handler] WARNING: Multiple clients from same IP: " + 
                sourceAddress.getAddress() + " (count: " + matchCount + ")");
            return null;
        }
        
        return null;
    }

    /**
     * Broadcast advanced screen sharing packet (with 28-byte header) to room
     * members
     */
    private void broadcastAdvancedPacketToRoom(String roomId, String senderId, byte mediaType,
            byte[] packetData, int packetLength) {
        Set<String> members = roomManager.getRoomMembers(roomId);
        if (members == null)
            return;

        int broadcastCount = 0;
        for (String memberId : members) {
            // Skip the sender
            if (memberId.equals(senderId))
                continue;

            ClientHandler handler = clientHandlers.get(memberId);
            if (handler == null)
                continue;

            InetSocketAddress udpAddress = handler.getUdpAddress();
            if (udpAddress == null)
                continue;

            try {
                // ✅ PERFORMANCE: Reuse packet buffer instead of copying for each recipient
                // Create DatagramPacket with original data (no copy needed for single recipient)
                // For multiple recipients, we still need to copy to avoid race conditions
                DatagramPacket outPacket;
                
                if (broadcastCount == 0) {
                    // First recipient: reuse original buffer (safe if only one recipient)
                    outPacket = new DatagramPacket(
                            packetData,
                            packetLength,
                            udpAddress);
                } else {
                    // Multiple recipients: need copy to avoid concurrent modification
                    byte[] packetCopy = new byte[packetLength];
                    System.arraycopy(packetData, 0, packetCopy, 0, packetLength);
                    outPacket = new DatagramPacket(
                            packetCopy,
                            packetLength,
                            udpAddress);
                }

                socket.send(outPacket);
                broadcastCount++;

                // ABR: Track packet sent for quality monitoring
                qualityMonitor.recordPacketSent(memberId);

            } catch (IOException e) {
                System.err.println("[UDP Handler] Error sending to " + memberId + ": " + e.getMessage());
            }
        }

        if (broadcastCount > 0) {
            String mediaTypeName = (mediaType == 1) ? "VOICE" : (mediaType == 2) ? "SCREEN" : "UNKNOWN";
            System.out.println("[UDP Handler] Broadcast " + mediaTypeName + " (advanced) from " +
                    senderId + " in room " + roomId + " to " + broadcastCount + " recipients (" + packetLength
                    + " bytes)");
        }
    }

    private void broadcastMediaToRoom(String roomId, String senderId, byte mediaType,
            byte[] data, int offset, int length) {
        Set<String> members = roomManager.getRoomMembers(roomId);
        if (members == null)
            return;

        int broadcastCount = 0;
        for (String memberId : members) {
            // Skip the sender
            if (memberId.equals(senderId))
                continue;

            ClientHandler handler = clientHandlers.get(memberId);
            if (handler == null)
                continue;

            InetSocketAddress udpAddress = handler.getUdpAddress();
            if (udpAddress == null)
                continue;

            try {
                // Create new packet with the media data
                byte[] mediaData = new byte[length];
                System.arraycopy(data, offset, mediaData, 0, length);

                DatagramPacket outPacket = new DatagramPacket(
                        mediaData,
                        length,
                        udpAddress);

                socket.send(outPacket);
                broadcastCount++;

                // ABR: Track packet sent for quality monitoring
                qualityMonitor.recordPacketSent(memberId);

            } catch (IOException e) {
                System.err.println("[UDP Handler] Error sending to " + memberId + ": " + e.getMessage());
            }
        }

        if (broadcastCount > 0) {
            String mediaTypeName = (mediaType == 1) ? "VOICE" : (mediaType == 2) ? "SCREEN" : "UNKNOWN";
            System.out.println("[UDP Handler] Broadcast " + mediaTypeName + " from " +
                    senderId + " in room " + roomId + " to " + broadcastCount + " recipients (" + length + " bytes)");
        }
    }

    /**
     * Validate packet header for advanced screen sharing
     * Checks for magic number 0x53435245 ('SCRE')
     */
    private boolean isValidPacket(byte[] data, int length) {
        // Minimum header size for advanced packets
        if (length < 28) {
            return false;
        }

        // Check magic number (0x53435245 = 'SCRE')
        int magic = ((data[0] & 0xFF) << 24) |
                ((data[1] & 0xFF) << 16) |
                ((data[2] & 0xFF) << 8) |
                (data[3] & 0xFF);

        return magic == 0x53435245;
    }
}


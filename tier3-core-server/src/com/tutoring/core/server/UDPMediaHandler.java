package com.tutoring.core.server;

import com.tutoring.core.management.RoomManager;
import com.tutoring.core.server.ClientHandler;
import com.tutoring.core.server.BroadcastWorker;
import com.tutoring.core.streaming.network.NetworkQualityMonitor;
import com.tutoring.core.concurrency.pool.BufferPool;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UDPMediaHandler implements Runnable {
    private final DatagramChannel channel;
    private final RoomManager roomManager;
    private final ConcurrentHashMap<String, ClientHandler> clientHandlers;
    private final NetworkQualityMonitor qualityMonitor;
    private final BroadcastWorker broadcastWorker; // ✅ WORKER PATTERN: Parallel broadcaster

    private final ConcurrentHashMap<Integer, Byte> frameMediaTypes = new ConcurrentHashMap<>();

    private static final int BUFFER_SIZE = 65536; // 64KB buffer for media packets

    private final ByteBuffer directReceiveBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

    private final BufferPool bufferPool = BufferPool.getInstance();

    public UDPMediaHandler(DatagramChannel channel, RoomManager roomManager,
            ConcurrentHashMap<String, ClientHandler> clientHandlers,
            NetworkQualityMonitor qualityMonitor,
            BroadcastWorker broadcastWorker) {
        this.channel = channel;
        this.roomManager = roomManager;
        this.clientHandlers = clientHandlers;
        this.qualityMonitor = qualityMonitor;
        this.broadcastWorker = broadcastWorker;
    }

    @Override
    public void run() {
        try {
            int port = channel.socket().getLocalPort();
            System.out.println("[UDP Handler] Started listening on port " + port);
            System.out.println("[UDP Handler] ✅ ZERO-COPY: Using DirectByteBuffer for off-heap memory");
            System.out.println("[UDP Handler] Ready to receive UDP packets from Proxy Server");
        } catch (Exception e) {
            System.err.println("[UDP Handler] Error getting port: " + e.getMessage());
        }

        long packetsReceived = 0;

        while (true) {
            try {
                directReceiveBuffer.clear();
                SocketAddress senderAddress = channel.receive(directReceiveBuffer);

                if (senderAddress == null) {
                    continue;
                }

                directReceiveBuffer.flip();
                int packetLength = directReceiveBuffer.remaining();

                packetsReceived++;

                if (packetsReceived <= 5 || packetsReceived % 1000 == 0) {
                    InetSocketAddress inetAddr = (InetSocketAddress) senderAddress;
                    System.out.println(String.format("[UDP Handler] Packet #%d received: %d bytes from %s:%d",
                            packetsReceived, packetLength,
                            inetAddr.getAddress().getHostAddress(), inetAddr.getPort()));
                }

                byte[] packetData = new byte[packetLength];
                directReceiveBuffer.get(packetData);
                handleMediaPacket(packetData, packetLength, (InetSocketAddress) senderAddress);

            } catch (IOException e) {
                System.err.println("[UDP Handler] Error receiving packet: " + e.getMessage());
            }
        }
    }

    private void handleMediaPacket(byte[] data, int length, InetSocketAddress sourceAddress) {
        try {

            if (isValidPacket(data, length)) {
                String senderId = findClientByUdpAddress(sourceAddress);
                if (senderId == null) {
                    System.out.println("[UDP Handler] Received advanced packet from unknown address: " +
                            sourceAddress + " (packet length: " + length + ")");
                    return;
                }
                ClientHandler senderHandler = clientHandlers.get(senderId);
                if (senderHandler == null)
                    return;

                String roomId = senderHandler.getCurrentRoom();
                if (roomId == null) {
                    return;
                }

                int frameId = ((data[8] & 0xFF) << 24) |
                        ((data[9] & 0xFF) << 16) |
                        ((data[10] & 0xFF) << 8) |
                        (data[11] & 0xFF);
                int fragmentIndex = ((data[12] & 0xFF) << 8) | (data[13] & 0xFF);

                byte mediaType = 2;
                if (fragmentIndex == 0 && length > 28) {
                    mediaType = data[28];
                    frameMediaTypes.put(frameId, mediaType);
                } else {
                    Byte storedType = frameMediaTypes.get(frameId);
                    if (storedType != null) {
                        mediaType = storedType;
                    }
                }

                if (frameMediaTypes.size() > 1000) {

                    int currentFrameId = frameId;
                    frameMediaTypes.entrySet().removeIf(entry -> entry.getKey() < currentFrameId - 100);
                }

                broadcastAdvancedPacketToRoom(roomId, senderId, mediaType, data, length);
                return;
            }

            if (length < 9)
                return;

            int offset = 0;

            int clientIdLength = ((data[offset] & 0xFF) << 24) |
                    ((data[offset + 1] & 0xFF) << 16) |
                    ((data[offset + 2] & 0xFF) << 8) |
                    (data[offset + 3] & 0xFF);
            offset += 4;

            if (clientIdLength <= 0 || clientIdLength > 100 || offset + clientIdLength > length)
                return;

            String senderId = new String(data, offset, clientIdLength);
            offset += clientIdLength;

            int roomIdLength = ((data[offset] & 0xFF) << 24) |
                    ((data[offset + 1] & 0xFF) << 16) |
                    ((data[offset + 2] & 0xFF) << 8) |
                    (data[offset + 3] & 0xFF);
            offset += 4;

            if (roomIdLength <= 0 || roomIdLength > 100 || offset + roomIdLength > length)
                return;

            String roomId = new String(data, offset, roomIdLength);
            offset += roomIdLength;

            if (offset >= length)
                return;
            byte mediaType = data[offset];
            offset++;

            int mediaDataLength = length - offset;
            if (mediaDataLength <= 0)
                return;

            broadcastMediaToRoom(roomId, senderId, mediaType, data, offset, mediaDataLength);

        } catch (Exception e) {
            System.err.println("[UDP Handler] Error processing packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String findClientByUdpAddress(InetSocketAddress sourceAddress) {
        for (String clientId : clientHandlers.keySet()) {
            ClientHandler handler = clientHandlers.get(clientId);
            if (handler == null)
                continue;

            InetSocketAddress registeredAddress = handler.getUdpAddress();
            if (registeredAddress == null)
                continue;

            if (registeredAddress.equals(sourceAddress)) {
                return clientId;
            }
        }

        String matchedClientId = null;
        int matchCount = 0;

        for (String clientId : clientHandlers.keySet()) {
            ClientHandler handler = clientHandlers.get(clientId);
            if (handler == null)
                continue;

            InetSocketAddress registeredAddress = handler.getUdpAddress();
            if (registeredAddress == null)
                continue;

            if (registeredAddress.getAddress().equals(sourceAddress.getAddress())) {
                matchedClientId = clientId;
                matchCount++;
            }
        }

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

    private void broadcastAdvancedPacketToRoom(String roomId, String senderId, byte mediaType,
            byte[] packetData, int packetLength) {
        Set<String> members = roomManager.getRoomMembers(roomId);
        if (members == null)
            return;

        // ✅ WORKER PATTERN: Use parallel broadcast worker for high performance
        int broadcastCount = broadcastWorker.broadcastSmart(
            members, 
            senderId, 
            packetData, 
            packetLength, 
            mediaType
        );

        if (broadcastCount > 0) {
            String mediaTypeName = (mediaType == 1) ? "VOICE" : (mediaType == 2) ? "SCREEN" : "UNKNOWN";
            System.out.println("[UDP Handler] ✅ PARALLEL Broadcast " + mediaTypeName + " (advanced) from " +
                    senderId + " in room " + roomId + " to " + broadcastCount + " recipients (" + packetLength
                    + " bytes)");
        }
    }

    private void broadcastMediaToRoom(String roomId, String senderId, byte mediaType,
            byte[] data, int offset, int length) {
        Set<String> members = roomManager.getRoomMembers(roomId);
        if (members == null)
            return;

        // Extract media data from original packet
        byte[] mediaData = bufferPool.acquire(length);
        System.arraycopy(data, offset, mediaData, 0, length);

        // ✅ WORKER PATTERN: Use parallel broadcast worker for high performance
        int broadcastCount = broadcastWorker.broadcastSmart(
            members, 
            senderId, 
            mediaData, 
            length, 
            mediaType
        );

        if (broadcastCount > 0) {
            String mediaTypeName = (mediaType == 1) ? "VOICE" : (mediaType == 2) ? "SCREEN" : "UNKNOWN";
            System.out.println("[UDP Handler] ✅ PARALLEL Broadcast " + mediaTypeName + " from " +
                    senderId + " in room " + roomId + " to " + broadcastCount + " recipients (" + length + " bytes)");
        }
    }

    private boolean isValidPacket(byte[] data, int length) {
        if (length < 28) {
            return false;
        }

        int magic = ((data[0] & 0xFF) << 24) |
                ((data[1] & 0xFF) << 16) |
                ((data[2] & 0xFF) << 8) |
                (data[3] & 0xFF);

        return magic == 0x53435245;
    }
}

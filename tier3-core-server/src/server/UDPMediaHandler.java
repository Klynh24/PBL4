package server;

import management.RoomManager;
import server.ClientHandler;
import server.BroadcastWorker;
import streaming.network.NetworkQualityMonitor;
import concurrency.pool.BufferPool;
import concurrency.worker.VideoWorker; // ✅ NEW: Worker 1
import concurrency.worker.AudioMixerWorker; // ✅ NEW: Worker 2
import concurrency.worker.ClientAudioWorker; // ✅ NEW: Workers 3-7

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
    
    // ✅ NEW: Specialized workers for packet routing
    private final VideoWorker videoWorker; // Worker 1
    private final AudioMixerWorker audioMixerWorker; // Worker 2
    private final ConcurrentHashMap<String, ClientAudioWorker> clientAudioWorkers; // Workers 3-7

    // ✅ FIX: Flag to control receive loop
    private volatile boolean running = true;

    private final ConcurrentHashMap<Integer, Byte> frameMediaTypes = new ConcurrentHashMap<>();

    private static final int BUFFER_SIZE = 65536; // 64KB buffer for media packets

    private final ByteBuffer directReceiveBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

    private final BufferPool bufferPool = BufferPool.getInstance();

    public UDPMediaHandler(DatagramChannel channel, RoomManager roomManager,
            ConcurrentHashMap<String, ClientHandler> clientHandlers,
            NetworkQualityMonitor qualityMonitor,
            BroadcastWorker broadcastWorker,
            VideoWorker videoWorker,
            AudioMixerWorker audioMixerWorker,
            ConcurrentHashMap<String, ClientAudioWorker> clientAudioWorkers) {
        this.channel = channel;
        this.roomManager = roomManager;
        this.clientHandlers = clientHandlers;
        this.qualityMonitor = qualityMonitor;
        this.broadcastWorker = broadcastWorker;
        this.videoWorker = videoWorker;
        this.audioMixerWorker = audioMixerWorker;
        this.clientAudioWorkers = clientAudioWorkers;
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

        while (running) {
            try {
                // ✅ FIX: Check if channel is still open before receiving
                if (channel == null || !channel.isOpen()) {
                    break;
                }
                
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
                // ✅ FIX: Only log error if handler is still running
                if (running && (channel == null || !channel.isOpen())) {
                    // Channel was closed, stop the loop
                    break;
                } else if (running) {
                    // Real error occurred
                    System.err.println("[UDP Handler] Error receiving packet: " + e.getMessage());
                }
                // If channel is closed, break the loop
                if (channel == null || !channel.isOpen()) {
                    break;
                }
            }
        }
        System.out.println("[UDP Handler] Receive loop stopped");
    }
    
    /**
     * ✅ FIX: Stop the UDP handler gracefully
     */
    public void shutdown() {
        running = false;
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

                // ✅ NEW: Route packets to appropriate workers
                if (mediaType == 2) {
                    // Video packet → Worker 1 (VideoWorker) for optimized processing
                    routeVideoPacket(roomId, senderId, data, length, frameId);
                } else if (mediaType == 1) {
                    // Audio packet → Worker 2 (AudioMixerWorker) and Workers 3-7 (ClientAudioWorker)
                    routeAudioPacket(roomId, senderId, data, length);
                } else {
                    // Unknown media type, use old broadcast method
                    broadcastAdvancedPacketToRoom(roomId, senderId, mediaType, data, length);
                }
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

    /**
     * ✅ NEW: Route video packet to VideoWorker (Worker 1)
     */
    private void routeVideoPacket(String roomId, String senderId, byte[] packetData, int length, int frameId) {
        try {
            Set<String> roomMembers = roomManager.getRoomMembers(roomId);
            if (roomMembers == null || roomMembers.isEmpty()) {
                return;
            }
            
            // Create video frame task
            // Note: VideoWorker expects full frame data, but we receive fragments
            // For now, we'll submit fragments directly and let VideoWorker handle reassembly
            // Or we can submit the full packet as-is for broadcasting
            VideoWorker.VideoFrameTask task = new VideoWorker.VideoFrameTask(
                senderId,
                roomId,
                roomMembers,
                packetData, // Submit fragment as-is for now
                frameId
            );
            
            boolean submitted = videoWorker.submitFrame(task);
            if (!submitted) {
                // Fallback to old broadcast method if worker queue is full
                broadcastAdvancedPacketToRoom(roomId, senderId, (byte) 2, packetData, length);
            }
            
        } catch (Exception e) {
            System.err.println("[UDP Handler] Error routing video packet: " + e.getMessage());
            // Fallback to old broadcast method
            broadcastAdvancedPacketToRoom(roomId, senderId, (byte) 2, packetData, length);
        }
    }
    
    /**
     * ✅ NEW: Route audio packet to AudioMixerWorker (Worker 2) and ClientAudioWorkers (Workers 3-7)
     */
    private void routeAudioPacket(String roomId, String senderId, byte[] packetData, int length) {
        try {
            Set<String> roomMembers = roomManager.getRoomMembers(roomId);
            if (roomMembers == null || roomMembers.isEmpty()) {
                return;
            }
            
            // Extract audio data (skip packet header if present)
            // For advanced packets, audio data starts at byte 28
            int audioDataOffset = 28;
            if (length <= audioDataOffset) {
                return;
            }
            
            byte[] audioData = new byte[length - audioDataOffset];
            System.arraycopy(packetData, audioDataOffset, audioData, 0, audioData.length);
            
            long timestamp = System.currentTimeMillis();
            
            // Route to AudioMixerWorker (Worker 2) for mixing
            AudioMixerWorker.AudioPacketTask mixerTask = new AudioMixerWorker.AudioPacketTask(
                senderId,
                roomId,
                audioData,
                timestamp
            );
            audioMixerWorker.submitAudio(mixerTask);
            
            // Route to each client's dedicated ClientAudioWorker (Workers 3-7)
            // This allows per-client audio processing (volume, echo cancellation, etc.)
            for (String clientId : roomMembers) {
                if (clientId.equals(senderId)) {
                    continue; // Don't send audio back to sender
                }
                
                // Get or create ClientAudioWorker for this client
                ClientAudioWorker worker = clientAudioWorkers.computeIfAbsent(clientId, id -> {
                    ClientHandler handler = clientHandlers.get(id);
                    if (handler == null) {
                        return null;
                    }
                    
                    ClientAudioWorker newWorker = new ClientAudioWorker(
                        id,
                        handler,
                        broadcastWorker,
                        qualityMonitor
                    );
                    
                    Thread workerThread = new Thread(newWorker, "ClientAudioWorker-" + id.substring(0, 8));
                    workerThread.setDaemon(false);
                    workerThread.start();
                    
                    System.out.println("[UDP Handler] ✅ Created ClientAudioWorker for client: " + id.substring(0, 8));
                    return newWorker;
                });
                
                if (worker != null) {
                    ClientAudioWorker.ClientAudioTask clientTask = new ClientAudioWorker.ClientAudioTask(
                        audioData,
                        senderId,
                        timestamp
                    );
                    worker.submitAudio(clientTask);
                }
            }
            
        } catch (Exception e) {
            System.err.println("[UDP Handler] Error routing audio packet: " + e.getMessage());
            // Fallback to old broadcast method
            broadcastAdvancedPacketToRoom(roomId, senderId, (byte) 1, packetData, length);
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

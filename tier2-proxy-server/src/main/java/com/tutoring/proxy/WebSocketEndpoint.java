package com.tutoring.proxy;

import org.json.JSONObject;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint that handles client connections
 * Each WebSocket session maintains its own TCP and UDP connections to Core
 * Server
 */
@ServerEndpoint("/connect")
public class WebSocketEndpoint {
    private static final String CORE_SERVER_HOST = "localhost";
    private static final int CORE_SERVER_TCP_PORT = 9000;
    private static final int CORE_SERVER_UDP_PORT = 9001;

    // Session storage
    private static final ConcurrentHashMap<Session, ConnectionContext> contexts = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("[Proxy] New WebSocket connection: " + session.getId());

        try {
            // Create TCP connection to Core Server
            Socket tcpSocket = new Socket(CORE_SERVER_HOST, CORE_SERVER_TCP_PORT);
            System.out.println("[Proxy " + session.getId() + "] TCP connected to Core Server");

            // Create UDP socket with ephemeral port
            DatagramSocket udpSocket = new DatagramSocket();
            int udpPort = udpSocket.getLocalPort();
            System.out.println("[Proxy " + session.getId() + "] UDP socket bound to port " + udpPort);

            // Create connection context
            ConnectionContext context = new ConnectionContext(session, tcpSocket, udpSocket);
            contexts.put(session, context);

            // Register UDP port with Core Server
            PrintWriter tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);
            tcpOut.println("REGISTER_UDP:" + udpPort);

            // Start TCP listener thread
            Thread tcpListener = new Thread(new TCPListener(context));
            tcpListener.setDaemon(true);
            tcpListener.start();

            // Start UDP listener thread
            Thread udpListener = new Thread(new UDPListener(context));
            udpListener.setDaemon(true);
            udpListener.start();

            System.out.println("[Proxy " + session.getId() + "] Connection context initialized");

        } catch (IOException e) {
            System.err.println("[Proxy] Failed to connect to Core Server: " + e.getMessage());
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                        "Failed to connect to Core Server"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @OnMessage
    public void onTextMessage(Session session, String message) {
        ConnectionContext context = contexts.get(session);
        if (context == null)
            return;

        try {
            // ✅ CRITICAL FIX: Handle NACK messages (text format: "NACK:frameId:index1,index2")
            if (message.startsWith("NACK:")) {
                // Forward NACK directly to Core Server (TCP)
                context.sendTCP(message);
                System.out.println("[Proxy " + session.getId().substring(0, 8) + "] Forwarded NACK to Core: " + message);
                return;
            }
            
            // Parse JSON message from client
            JSONObject json = new JSONObject(message);
            String type = json.getString("type");

            // Translate to Core Server protocol
            String coreMessage = translateToCore(json);

            // Send via TCP to Core Server
            context.sendTCP(coreMessage);
            System.out.println("[Proxy " + session.getId() + "] Sent to Core: " + coreMessage);

        } catch (Exception e) {
            System.err.println("[Proxy] Error processing text message: " + e.getMessage());
        }
    }

    @OnMessage
    public void onBinaryMessage(Session session, ByteBuffer buffer) {
        ConnectionContext context = contexts.get(session);
        if (context == null)
            return;

        try {
            // Extract binary data from WebSocket message
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            // CRITICAL FIX: Parse the legacy packet format from Web client
            // Format:
            // [ClientID_Length(4)][ClientID][RoomID_Length(4)][RoomID][MediaType(1)][Data]
            // Then fragment ONLY the media data (not the header) into UDP packets

            if (data.length < 9) {
                System.err.println("[Proxy] Invalid binary packet: too small (" + data.length + " bytes)");
                return;
            }

            int offset = 0;

            // Extract ClientID length
            int clientIdLength = ((data[offset] & 0xFF) << 24) |
                    ((data[offset + 1] & 0xFF) << 16) |
                    ((data[offset + 2] & 0xFF) << 8) |
                    (data[offset + 3] & 0xFF);
            offset += 4;

            if (clientIdLength <= 0 || clientIdLength > 100 || offset + clientIdLength > data.length) {
                System.err.println("[Proxy] Invalid clientId length: " + clientIdLength);
                return;
            }

            // Extract ClientID
            String clientId = new String(data, offset, clientIdLength);
            offset += clientIdLength;

            // Extract RoomID length
            int roomIdLength = ((data[offset] & 0xFF) << 24) |
                    ((data[offset + 1] & 0xFF) << 16) |
                    ((data[offset + 2] & 0xFF) << 8) |
                    (data[offset + 3] & 0xFF);
            offset += 4;

            if (roomIdLength <= 0 || roomIdLength > 100 || offset + roomIdLength > data.length) {
                System.err.println("[Proxy] Invalid roomId length: " + roomIdLength);
                return;
            }

            // Extract RoomID
            String roomId = new String(data, offset, roomIdLength);
            offset += roomIdLength;

            // Extract Media Type
            if (offset >= data.length) {
                System.err.println("[Proxy] Missing media type");
                return;
            }
            byte mediaType = data[offset];
            offset++;

            // Extract actual media data
            int mediaDataLength = data.length - offset;
            if (mediaDataLength <= 0) {
                System.err.println("[Proxy] No media data");
                return;
            }

            byte[] mediaData = new byte[mediaDataLength];
            System.arraycopy(data, offset, mediaData, 0, mediaDataLength);

            // ✅ ADVANCED PIPELINE: Always use fragmentation for reliability and scalability
            // Create media packet: [MediaType(1 byte)][Media Data]
            byte[] mediaPacket = new byte[1 + mediaDataLength];
            mediaPacket[0] = mediaType;
            System.arraycopy(mediaData, 0, mediaPacket, 1, mediaDataLength);

            // Fragment using FrameFragmenter (supports any size)
            FrameFragmenter fragmenter = context.getFragmenter();
            List<byte[]> fragments = fragmenter.fragmentMessage(mediaPacket);

            // Get frame ID BEFORE loop (for logging)
            int frameId = fragmenter.getFrameIdCounter() - 1;
            String mediaTypeName = (mediaType == 1) ? "VOICE" : (mediaType == 2) ? "SCREEN" : "UNKNOWN";

            // Send each fragment to Core Server
            InetAddress coreAddress = InetAddress.getByName(CORE_SERVER_HOST);
            DatagramSocket udpSocket = context.getUdpSocket();

            for (int i = 0; i < fragments.size(); i++) {
                byte[] fragment = fragments.get(i);
                DatagramPacket packet = new DatagramPacket(
                        fragment,
                        fragment.length,
                        coreAddress,
                        CORE_SERVER_UDP_PORT);
                udpSocket.send(packet);

                // ✅ DEBUG: Log first fragment send details
                if (i == 0 && frameId < 3) {
                    System.out.println(String.format(
                            "[Proxy UDP DEBUG] Sent fragment 0/%d: %d bytes to %s:%d from local port %d",
                            fragments.size(), fragment.length, coreAddress.getHostAddress(),
                            CORE_SERVER_UDP_PORT, udpSocket.getLocalPort()));
                }
            }

            // Log periodically (not every frame)
            if (frameId < 5 || frameId % 50 == 0) {
                System.out.println(String.format(
                        "[Proxy %s] %s from client=%s, room=%s: %d bytes → %d fragments (ADVANCED FORMAT, frame #%d)",
                        session.getId().substring(0, 8), mediaTypeName, clientId, roomId,
                        mediaPacket.length, fragments.size(), frameId));
            }

        } catch (IOException e) {
            System.err.println("[Proxy] Error forwarding binary data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("[Proxy] WebSocket closed: " + session.getId() + " - " + reason);

        ConnectionContext context = contexts.remove(session);
        if (context != null) {
            context.close();
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("[Proxy] WebSocket error: " + session.getId());
        error.printStackTrace();
    }

    /**
     * Translates JSON messages from client to Core Server protocol
     */
    private String translateToCore(JSONObject json) {
        String type = json.getString("type");

        switch (type) {
            case "register":
                return "REGISTER:" + json.getString("username") + ":" + json.getString("password");

            case "login":
                return "LOGIN:" + json.getString("username") + ":" + json.getString("password");

            case "create_room":
                return "CREATE_ROOM:" + json.getString("room");

            case "join_room":
                return "JOIN_ROOM:" + json.getString("room");

            case "leave_room":
                return "LEAVE_ROOM";

            case "list_rooms":
                return "LIST_ROOMS";

            case "chat":
                return "CHAT:" + json.getString("message");

            case "quality_ack":
                return "QUALITY_ACK:" + json.getString("level");

            case "disconnect":
                return "DISCONNECT";

            default:
                return "UNKNOWN:" + type;
        }
    }

    /**
     * Translates Core Server messages to JSON for client
     */
    static JSONObject translateToClient(String coreMessage) {
        JSONObject json = new JSONObject();

        String[] parts = coreMessage.split(":", 2);
        String type = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        json.put("type", type.toLowerCase());

        switch (type) {
            case "LOGIN_SUCCESS":
            case "REGISTER_SUCCESS":
                json.put("username", data);
                break;

            case "ROOM_CREATED":
            case "ROOM_JOINED":
            case "ROOM_LEFT":
                json.put("room", data);
                break;

            case "ROOM_LIST":
                json.put("rooms", data);
                break;

            case "CHAT":
                String[] chatParts = data.split(":", 2);
                if (chatParts.length == 2) {
                    json.put("username", chatParts[0]);
                    json.put("message", chatParts[1]);
                }
                break;

            case "SET_QUALITY":
                // Format: SET_QUALITY:LEVEL:WIDTH:HEIGHT:FPS:QUALITY
                String[] qualityParts = data.split(":");
                if (qualityParts.length >= 5) {
                    json.put("level", qualityParts[0]);
                    json.put("width", Integer.parseInt(qualityParts[1]));
                    json.put("height", Integer.parseInt(qualityParts[2]));
                    json.put("fps", Integer.parseInt(qualityParts[3]));
                    json.put("jpegQuality", Float.parseFloat(qualityParts[4]));
                }
                break;

            case "QUALITY_ACK":
                json.put("level", data);
                break;

            case "USER_JOINED":
            case "USER_LEFT":
                json.put("username", data);
                break;

            case "CHAT_HISTORY_START":
                json.put("count", data);
                break;

            case "CHAT_HISTORY_END":
                // No additional data needed
                break;

            case "ERROR":
                json.put("error", data);
                break;

            default:
                json.put("data", data);
        }

        return json;
    }
}

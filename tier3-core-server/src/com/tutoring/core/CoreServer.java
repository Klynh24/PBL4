package com.tutoring.core;

import com.tutoring.core.streaming.RetransmissionBuffer;
import com.tutoring.core.streaming.PerformanceMonitor;
import com.tutoring.core.streaming.NetworkQualityMonitor;
import com.tutoring.core.streaming.QualityLevelManager;
import com.tutoring.core.streaming.QualityEvaluationTask;

import java.io.*;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.util.*;
import java.util.concurrent.*;

/**
 * Tier 3: Core Logic Server
 * Uses ONLY pure Java TCP/UDP sockets (java.net package)
 * Handles all application logic, state management, and media broadcasting
 */
public class CoreServer {
    private static final int TCP_PORT = 9000;
    private static final int UDP_PORT = 9001;

    private ServerSocket tcpServerSocket;
    private DatagramChannel udpChannel; // ✅ PHASE 1: ZERO-COPY - Use DatagramChannel

    // Thread-safe state management
    private final RoomManager roomManager;
    private final UserManager userManager;

    // Map to store client handlers by their ID
    private final ConcurrentHashMap<String, ClientHandler> clientHandlers;

    // Advanced screen sharing components
    private RetransmissionBuffer retransmissionBuffer;
    private PerformanceMonitor performanceMonitor;

    // Adaptive bitrate components
    private NetworkQualityMonitor networkMonitor;
    private QualityLevelManager qualityManager;
    private QualityEvaluationTask qualityEvaluationTask;

    public CoreServer() {
        this.roomManager = new RoomManager();
        this.userManager = new UserManager();
        this.clientHandlers = new ConcurrentHashMap<>();
    }

    public void start() throws IOException {
        // Initialize TCP ServerSocket for signaling
        tcpServerSocket = new ServerSocket(TCP_PORT);
        System.out.println("[Core Server] TCP Server started on port " + TCP_PORT);

        // ✅ PHASE 1: ZERO-COPY - Initialize UDP Channel for media (instead of DatagramSocket)
        udpChannel = DatagramChannel.open();
        udpChannel.bind(new InetSocketAddress(UDP_PORT));
        udpChannel.configureBlocking(true); // Blocking mode for simplicity (can be non-blocking for better performance)
        System.out.println("[Core Server] UDP Channel started on port " + UDP_PORT);
        System.out.println("[Core Server] ✅ ZERO-COPY: Using DatagramChannel with DirectByteBuffer");
        
        // ✅ CRITICAL FIX: Increase receive buffer to handle burst traffic from fragmented frames
        try {
            int targetBuffer = 8 * 1024 * 1024; // 8 MB (can hold ~5,700 packets)
            udpChannel.socket().setReceiveBufferSize(targetBuffer);
            int actualBuffer = udpChannel.socket().getReceiveBufferSize();
            
            System.out.println(String.format("[Core Server] UDP receive buffer: requested=%d KB, actual=%d KB",
                targetBuffer / 1024, actualBuffer / 1024));
            
            if (actualBuffer < targetBuffer / 2) {
                System.err.println("[Core Server] WARNING: OS limited buffer size - may experience packet loss!");
                System.err.println("[Core Server] On Linux: sudo sysctl -w net.core.rmem_max=8388608");
            }
        } catch (SocketException e) {
            System.err.println("[Core Server] WARNING: Could not set receive buffer size: " + e.getMessage());
        }

        // Initialize advanced screen sharing components
        retransmissionBuffer = new RetransmissionBuffer();
        performanceMonitor = new PerformanceMonitor();
        System.out.println("[Core Server] Advanced screen sharing components initialized");

        // Initialize adaptive bitrate components
        networkMonitor = new NetworkQualityMonitor();
        qualityManager = new QualityLevelManager(networkMonitor);
        System.out.println("[Core Server] Adaptive bitrate (ABR) components initialized");

        // Start performance monitoring thread
        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // Print stats every 30 seconds
                    performanceMonitor.printStats();
                    performanceMonitor.checkTargets();

                    // ABR: Print network quality stats
                    networkMonitor.printStats();
                    qualityManager.printStats();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
        System.out.println("[Core Server] Performance monitoring started");

        // Start quality evaluation thread (ABR)
        qualityEvaluationTask = new QualityEvaluationTask(qualityManager, roomManager, clientHandlers);
        Thread qualityThread = new Thread(qualityEvaluationTask);
        qualityThread.setDaemon(true);
        qualityThread.start();
        System.out.println("[Core Server] Quality evaluation thread started (ABR)");

        // ✅ PHASE 1: ZERO-COPY - Start UDP listener thread with DatagramChannel
        Thread udpThread = new Thread(new UDPMediaHandler(udpChannel, roomManager, clientHandlers, networkMonitor));
        udpThread.setDaemon(true);
        udpThread.start();

        // Main loop: Accept TCP connections
        System.out.println("[Core Server] Ready to accept connections...\n");
        while (true) {
            try {
                Socket clientSocket = tcpServerSocket.accept();
                System.out.println("[Core Server] New TCP connection from: " +
                        clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());

                // ✅ PHASE 1: ZERO-COPY - Create client handler with DatagramChannel
                ClientHandler handler = new ClientHandler(clientSocket, roomManager, userManager,
                        clientHandlers, retransmissionBuffer, udpChannel, networkMonitor);
                Thread handlerThread = new Thread(handler);
                handlerThread.start();
            } catch (IOException e) {
                System.err.println("[Core Server] Error accepting connection: " + e.getMessage());
            }
        }
    }

    /**
     * Get network quality monitor (for testing/monitoring)
     */
    public NetworkQualityMonitor getNetworkMonitor() {
        return networkMonitor;
    }

    /**
     * Get quality level manager (for testing/monitoring)
     */
    public QualityLevelManager getQualityManager() {
        return qualityManager;
    }

    public static void main(String[] args) {
        try {
            CoreServer server = new CoreServer();
            server.start();
        } catch (IOException e) {
            System.err.println("[Core Server] Failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

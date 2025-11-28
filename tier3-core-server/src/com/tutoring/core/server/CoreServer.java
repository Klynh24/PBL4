package com.tutoring.core.server;

import com.tutoring.core.management.RoomManager;
import com.tutoring.core.management.UserManager;
import com.tutoring.core.management.ThreadPoolManager;
import com.tutoring.core.server.ClientHandler;
import com.tutoring.core.server.UDPMediaHandler;
import com.tutoring.core.server.BroadcastWorker;
import com.tutoring.core.streaming.retransmission.RetransmissionBuffer;
import com.tutoring.core.monitoring.PerformanceMonitor;
import com.tutoring.core.streaming.network.NetworkQualityMonitor;
import com.tutoring.core.streaming.quality.QualityLevelManager;
import com.tutoring.core.streaming.quality.QualityEvaluationTask;
import com.tutoring.core.concurrency.worker.WorkerMetrics;

import java.io.*;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.util.*;
import java.util.concurrent.*;

public class CoreServer {
    private static final int TCP_PORT = 9000;
    private static final int UDP_PORT = 9001;

    private ServerSocket tcpServerSocket;
    private DatagramChannel udpChannel;

    private final RoomManager roomManager;
    private final UserManager userManager;

    private final ConcurrentHashMap<String, ClientHandler> clientHandlers;

    private RetransmissionBuffer retransmissionBuffer;
    private PerformanceMonitor performanceMonitor;

    // Adaptive bitrate components
    private NetworkQualityMonitor networkMonitor;
    private QualityLevelManager qualityManager;
    private QualityEvaluationTask qualityEvaluationTask;
    
    // ✅ WORKER PATTERN: Central thread pool management
    private ThreadPoolManager threadPoolManager;
    private BroadcastWorker broadcastWorker;

    public CoreServer() {
        this.roomManager = new RoomManager();
        this.userManager = new UserManager();
        this.clientHandlers = new ConcurrentHashMap<>();
        this.threadPoolManager = new ThreadPoolManager();
        
        System.out.println("[Core Server] ✅ WORKER PATTERN: ThreadPoolManager initialized");
    }

    public void start() throws IOException {
        tcpServerSocket = new ServerSocket(TCP_PORT);
        System.out.println("[Core Server] TCP Server started on port " + TCP_PORT);

        udpChannel = DatagramChannel.open();
        udpChannel.bind(new InetSocketAddress(UDP_PORT));
        udpChannel.configureBlocking(true); // Blocking mode for simplicity (can be non-blocking for better performance)
        System.out.println("[Core Server] UDP Channel started on port " + UDP_PORT);
        System.out.println("[Core Server] ✅ ZERO-COPY: Using DatagramChannel with DirectByteBuffer");

        try {
            int targetBuffer = 8 * 1024 * 1024;
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

        retransmissionBuffer = new RetransmissionBuffer();
        performanceMonitor = new PerformanceMonitor();
        System.out.println("[Core Server] Advanced screen sharing components initialized");

        networkMonitor = new NetworkQualityMonitor();
        qualityManager = new QualityLevelManager(networkMonitor);
        System.out.println("[Core Server] Adaptive bitrate (ABR) components initialized");
        
        // ✅ WORKER PATTERN: Initialize broadcast worker
        broadcastWorker = new BroadcastWorker(
            udpChannel,
            clientHandlers,
            networkMonitor,
            threadPoolManager.getBroadcastExecutor()
        );
        System.out.println("[Core Server] ✅ WORKER PATTERN: BroadcastWorker initialized for parallel broadcasting");

        // ✅ WORKER PATTERN: Use scheduled executor for monitoring
        threadPoolManager.scheduleMonitoring(() -> {
            performanceMonitor.printStats();
            performanceMonitor.checkTargets();
            networkMonitor.printStats();
            qualityManager.printStats();
            threadPoolManager.printStats();
            broadcastWorker.printStats();
            WorkerMetrics.getInstance().printReport();
        }, 30, 30, TimeUnit.SECONDS);
        System.out.println("[Core Server] ✅ WORKER PATTERN: Performance monitoring scheduled (30s interval)");

        // ✅ WORKER PATTERN: Use scheduled executor for quality evaluation
        qualityEvaluationTask = new QualityEvaluationTask(qualityManager, roomManager, clientHandlers);
        threadPoolManager.scheduleMonitoring(qualityEvaluationTask, 5, 5, TimeUnit.SECONDS);
        System.out.println("[Core Server] ✅ WORKER PATTERN: Quality evaluation scheduled (5s interval)");

        // ✅ WORKER PATTERN: UDP handler still runs in dedicated thread (I/O bound)
        Thread udpThread = new Thread(new UDPMediaHandler(
            udpChannel, 
            roomManager, 
            clientHandlers, 
            networkMonitor,
            broadcastWorker
        ));
        udpThread.setDaemon(true);
        udpThread.setName("UDP-Receiver");
        udpThread.start();

        System.out.println("[Core Server] Ready to accept connections...\n");
        
        // ✅ WORKER PATTERN: Use thread pool for client handlers
        while (true) {
            try {
                Socket clientSocket = tcpServerSocket.accept();
                System.out.println("[Core Server] New TCP connection from: " +
                        clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());

                ClientHandler handler = new ClientHandler(clientSocket, roomManager, userManager,
                        clientHandlers, retransmissionBuffer, udpChannel, networkMonitor);
                
                // ✅ WORKER PATTERN: Submit to thread pool instead of creating new thread
                boolean accepted = threadPoolManager.submitClientHandler(handler);
                
                if (!accepted) {
                    // Server overloaded - reject connection gracefully
                    System.err.println("[Core Server] Server overloaded - rejecting connection from " +
                            clientSocket.getInetAddress().getHostAddress());
                    try {
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        out.println("ERROR:Server overloaded, please try again later");
                        clientSocket.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                }
            } catch (IOException e) {
                System.err.println("[Core Server] Error accepting connection: " + e.getMessage());
            }
        }
    }

    public NetworkQualityMonitor getNetworkMonitor() {
        return networkMonitor;
    }

    public QualityLevelManager getQualityManager() {
        return qualityManager;
    }
    
    public BroadcastWorker getBroadcastWorker() {
        return broadcastWorker;
    }
    
    public ThreadPoolManager getThreadPoolManager() {
        return threadPoolManager;
    }
    
    /**
     * ✅ WORKER PATTERN: Graceful shutdown
     */
    public void shutdown() {
        System.out.println("\n[Core Server] Initiating graceful shutdown...");
        
        try {
            // Stop accepting new connections
            if (tcpServerSocket != null && !tcpServerSocket.isClosed()) {
                tcpServerSocket.close();
                System.out.println("[Core Server] TCP server socket closed");
            }
            
            // Stop UDP channel
            if (udpChannel != null && udpChannel.isOpen()) {
                udpChannel.close();
                System.out.println("[Core Server] UDP channel closed");
            }
            
            // Shutdown all thread pools
            if (threadPoolManager != null) {
                threadPoolManager.shutdown();
            }
            
            System.out.println("[Core Server] ✅ Shutdown completed successfully\n");
            
        } catch (IOException e) {
            System.err.println("[Core Server] Error during shutdown: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        CoreServer server = null;
        
        try {
            server = new CoreServer();
            
            // ✅ WORKER PATTERN: Add shutdown hook for graceful termination
            final CoreServer finalServer = server;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[Core Server] Shutdown signal received (Ctrl+C)");
                finalServer.shutdown();
            }));
            
            server.start();
            
        } catch (IOException e) {
            System.err.println("[Core Server] Failed to start: " + e.getMessage());
            e.printStackTrace();
            
            // Attempt cleanup
            if (server != null) {
                server.shutdown();
            }
            System.exit(1);
        }
    }
}

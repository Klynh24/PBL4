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

    public CoreServer() {
        this.roomManager = new RoomManager();
        this.userManager = new UserManager();
        this.clientHandlers = new ConcurrentHashMap<>();
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

        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000);
                    performanceMonitor.printStats();
                    performanceMonitor.checkTargets();

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

        qualityEvaluationTask = new QualityEvaluationTask(qualityManager, roomManager, clientHandlers);
        Thread qualityThread = new Thread(qualityEvaluationTask);
        qualityThread.setDaemon(true);
        qualityThread.start();
        System.out.println("[Core Server] Quality evaluation thread started (ABR)");

        Thread udpThread = new Thread(new UDPMediaHandler(udpChannel, roomManager, clientHandlers, networkMonitor));
        udpThread.setDaemon(true);
        udpThread.start();

        System.out.println("[Core Server] Ready to accept connections...\n");
        while (true) {
            try {
                Socket clientSocket = tcpServerSocket.accept();
                System.out.println("[Core Server] New TCP connection from: " +
                        clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());

                ClientHandler handler = new ClientHandler(clientSocket, roomManager, userManager,
                        clientHandlers, retransmissionBuffer, udpChannel, networkMonitor);
                Thread handlerThread = new Thread(handler);
                handlerThread.start();
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

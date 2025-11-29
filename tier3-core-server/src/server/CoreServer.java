package server;

import management.RoomManager;
import management.UserManager;
import management.ThreadPoolManager;
import server.ClientHandler;
import server.UDPMediaHandler;
import server.BroadcastWorker;
import streaming.retransmission.RetransmissionBuffer;
import monitoring.PerformanceMonitor;
import streaming.network.NetworkQualityMonitor;
import streaming.quality.QualityLevelManager;
import streaming.quality.QualityEvaluationTask;
import streaming.quality.PerClientQualityManager;
import streaming.encoding.FrameEncoder;
import streaming.protocol.FrameFragmenter;
import concurrency.worker.WorkerMetrics;
import concurrency.worker.VideoWorker;
import concurrency.worker.AudioMixerWorker;
import concurrency.worker.ClientAudioWorker;

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

    private volatile boolean running = false;

    private final RoomManager roomManager;
    private final UserManager userManager;

    private final ConcurrentHashMap<String, ClientHandler> clientHandlers;

    private RetransmissionBuffer retransmissionBuffer;
    private PerformanceMonitor performanceMonitor;

    private NetworkQualityMonitor networkMonitor;
    private QualityLevelManager qualityManager;
    private QualityEvaluationTask qualityEvaluationTask;
    private PerClientQualityManager perClientQualityManager;

    private ThreadPoolManager threadPoolManager;
    private BroadcastWorker broadcastWorker;
    private UDPMediaHandler udpMediaHandler;

    private VideoWorker videoWorker;
    private AudioMixerWorker audioMixerWorker;
    private ConcurrentHashMap<String, ClientAudioWorker> clientAudioWorkers;
    private FrameEncoder frameEncoder;
    private FrameFragmenter frameFragmenter;

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
        udpChannel.configureBlocking(true);
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

        try {
            frameEncoder = new FrameEncoder();
            System.out.println("[Core Server] ✅ Hardware encoder detection completed");
        } catch (Exception e) {
            System.err.println("[Core Server] Warning: Failed to initialize encoder: " + e.getMessage());
            frameEncoder = new FrameEncoder(); // Create anyway
        }

        frameFragmenter = new streaming.protocol.FrameFragmenter(true, 10, 2); // FEC enabled

        System.out.println("[Core Server] Advanced screen sharing components initialized");

        networkMonitor = new NetworkQualityMonitor();
        qualityManager = new QualityLevelManager(networkMonitor);
        perClientQualityManager = new PerClientQualityManager(networkMonitor, clientHandlers); // ✅ NEW
        System.out.println("[Core Server] ✅ Adaptive bitrate (ABR) components initialized");
        System.out.println("[Core Server] ✅ Per-Client Adaptive Quality ENABLED (no more 'Worst Client Wins')");

        broadcastWorker = new BroadcastWorker(
                udpChannel,
                clientHandlers,
                networkMonitor,
                threadPoolManager.getBroadcastExecutor());
        System.out.println("[Core Server] ✅ WORKER PATTERN: BroadcastWorker initialized for parallel broadcasting");

        videoWorker = new VideoWorker(
                broadcastWorker,
                frameEncoder,
                frameFragmenter,
                retransmissionBuffer,
                networkMonitor);
        Thread videoWorkerThread = new Thread(videoWorker, "VideoWorker");
        videoWorkerThread.setDaemon(false);
        videoWorkerThread.start();
        System.out.println("[Core Server] ✅ Worker 1: VideoWorker started (60fps, <100ms latency)");

        audioMixerWorker = new AudioMixerWorker(
                broadcastWorker,
                networkMonitor,
                roomManager);
        Thread audioMixerThread = new Thread(audioMixerWorker, "AudioMixerWorker");
        audioMixerThread.setDaemon(false);
        audioMixerThread.start();
        System.out.println("[Core Server] ✅ Worker 2: AudioMixerWorker started");

        clientAudioWorkers = new ConcurrentHashMap<>();
        System.out
                .println("[Core Server] ✅ Workers 3-7: ClientAudioWorker pool initialized (<150ms latency per client)");

        threadPoolManager.scheduleMonitoring(() -> {
            performanceMonitor.printStats();
            performanceMonitor.checkTargets();
            networkMonitor.printStats();
            qualityManager.printStats();
            perClientQualityManager.printStats();
            threadPoolManager.printStats();
            broadcastWorker.printStats();
            WorkerMetrics.getInstance().printReport();
        }, 30, 30, TimeUnit.SECONDS);
        System.out.println("[Core Server] ✅ WORKER PATTERN: Performance monitoring scheduled (30s interval)");

        qualityEvaluationTask = new QualityEvaluationTask(qualityManager, roomManager, clientHandlers);
        threadPoolManager.scheduleMonitoring(qualityEvaluationTask, 5, 5, TimeUnit.SECONDS);
        System.out.println("[Core Server] ✅ WORKER PATTERN: Quality evaluation scheduled (5s interval)");

        threadPoolManager.scheduleMonitoring(() -> {
            perClientQualityManager.evaluateAllClients();
        }, 5, 5, TimeUnit.SECONDS);
        System.out.println("[Core Server] ✅ Per-Client Quality evaluation scheduled (5s interval)");

        udpMediaHandler = new UDPMediaHandler(
                udpChannel,
                roomManager,
                clientHandlers,
                networkMonitor,
                broadcastWorker,
                videoWorker,
                audioMixerWorker,
                clientAudioWorkers);
        Thread udpThread = new Thread(udpMediaHandler);
        udpThread.setDaemon(true);
        udpThread.setName("UDP-Receiver");
        udpThread.start();

        System.out.println("[Core Server] Ready to accept connections...\n");

        running = true;
        while (running) {
            try {
                if (tcpServerSocket == null || tcpServerSocket.isClosed()) {
                    break;
                }
                Socket clientSocket = tcpServerSocket.accept();
                System.out.println("[Core Server] New TCP connection from: " +
                        clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());

                ClientHandler handler = new ClientHandler(clientSocket, roomManager, userManager,
                        clientHandlers, retransmissionBuffer, udpChannel, networkMonitor);

                boolean accepted = threadPoolManager.submitClientHandler(handler);

                if (!accepted) {
                    System.err.println("[Core Server] Server overloaded - rejecting connection from " +
                            clientSocket.getInetAddress().getHostAddress());
                    try {
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        out.println("ERROR:Server overloaded, please try again later");
                        clientSocket.close();
                    } catch (IOException ex) {
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("[Core Server] Error accepting connection: " + e.getMessage());
                }
                if (tcpServerSocket == null || tcpServerSocket.isClosed()) {
                    break;
                }
            }
        }
        System.out.println("[Core Server] Accept loop stopped");
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

    public void shutdown() {
        System.out.println("\n[Core Server] Initiating graceful shutdown...");

        running = false;

        try {
            if (tcpServerSocket != null && !tcpServerSocket.isClosed()) {
                tcpServerSocket.close();
                System.out.println("[Core Server] TCP server socket closed");
            }

            if (udpMediaHandler != null) {
                udpMediaHandler.shutdown();
                System.out.println("[Core Server] UDP handler shutdown initiated");
            }

            if (udpChannel != null && udpChannel.isOpen()) {
                udpChannel.close();
                System.out.println("[Core Server] UDP channel closed");
            }

            if (videoWorker != null) {
                videoWorker.shutdown();
            }

            if (audioMixerWorker != null) {
                audioMixerWorker.shutdown();
            }

            if (clientAudioWorkers != null) {
                for (ClientAudioWorker worker : clientAudioWorkers.values()) {
                    worker.shutdown();
                }
                clientAudioWorkers.clear();
            }

            if (broadcastWorker != null) {
                broadcastWorker.shutdown();
            }

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

            final CoreServer finalServer = server;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[Core Server] Shutdown signal received (Ctrl+C)");
                finalServer.shutdown();
            }));

            server.start();

        } catch (IOException e) {
            System.err.println("[Core Server] Failed to start: " + e.getMessage());
            e.printStackTrace();

            if (server != null) {
                server.shutdown();
            }
            System.exit(1);
        }
    }
}

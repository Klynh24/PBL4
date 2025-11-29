package concurrency.worker;

import server.BroadcastWorker;
import server.ClientHandler;
import streaming.encoding.FrameEncoder;
import streaming.protocol.FrameFragmenter;
import streaming.retransmission.RetransmissionBuffer;
import streaming.network.NetworkQualityMonitor;

import java.nio.channels.DatagramChannel;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class VideoWorker implements Runnable {
    private final BlockingQueue<VideoFrameTask> frameQueue;
    private final BroadcastWorker broadcastWorker;
    private final FrameEncoder frameEncoder;
    private final FrameFragmenter fragmenter;
    private final RetransmissionBuffer retransmissionBuffer;
    private final NetworkQualityMonitor networkMonitor;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong framesProcessed = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong framesDropped = new AtomicLong(0);

    private static final int TARGET_FPS = 60;
    private static final long TARGET_FRAME_INTERVAL_MS = 1000 / TARGET_FPS; // ~16ms
    private static final long MAX_LATENCY_MS = 50; // ✅ OPTIMIZED: Reduced from 100ms to 50ms for lower latency
    private static final int MAX_QUEUE_SIZE = 5; // ✅ OPTIMIZED: Reduced from 10 to 5 to prevent buffering delay

    public VideoWorker(BroadcastWorker broadcastWorker,
            FrameEncoder frameEncoder,
            FrameFragmenter fragmenter,
            RetransmissionBuffer retransmissionBuffer,
            NetworkQualityMonitor networkMonitor) {
        this.broadcastWorker = broadcastWorker;
        this.frameEncoder = frameEncoder;
        this.fragmenter = fragmenter;
        this.retransmissionBuffer = retransmissionBuffer;
        this.networkMonitor = networkMonitor;

        this.frameQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

        System.out.println("[VideoWorker] ✅ Initialized:");
        System.out.println("  - Target FPS: " + TARGET_FPS);
        System.out.println("  - Target latency: <" + MAX_LATENCY_MS + "ms");
        System.out.println("  - Queue size: " + MAX_QUEUE_SIZE);
    }

    public boolean submitFrame(VideoFrameTask task) {
        if (!running.get()) {
            return false;
        }

        if (frameQueue.size() >= MAX_QUEUE_SIZE) {
            framesDropped.incrementAndGet();
            System.err.println("[VideoWorker] Queue full, dropping frame from client: " + task.senderId);
            return false;
        }

        task.receiveTimestamp = System.currentTimeMillis();

        return frameQueue.offer(task);
    }

    @Override
    public void run() {
        System.out.println("[VideoWorker] 🎥 Started video processing worker");
        long lastFrameTime = 0;

        while (running.get() || !frameQueue.isEmpty()) {
            try {
                VideoFrameTask task = frameQueue.poll(100, TimeUnit.MILLISECONDS);

                if (task == null) {
                    continue;
                }

                long processingStart = System.currentTimeMillis();

                // ✅ OPTIMIZED: Remove frame rate limiting sleep to minimize latency
                // Queue size already limits throughput, no need for additional delay
                // Target latency < 50ms, so process frames immediately
                lastFrameTime = processingStart;

                processFrame(task);

                long processingTime = System.currentTimeMillis() - processingStart;
                totalProcessingTime.addAndGet(processingTime);
                framesProcessed.incrementAndGet();

                long latency = System.currentTimeMillis() - task.receiveTimestamp;
                if (latency > MAX_LATENCY_MS) {
                    System.err.println(
                            "[VideoWorker] ⚠️ High latency: " + latency + "ms (target: <" + MAX_LATENCY_MS + "ms)");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[VideoWorker] Error processing frame: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("[VideoWorker] 🛑 Stopped video processing worker");
    }

    private void processFrame(VideoFrameTask task) {
        try {
            // ✅ FIX: task.frameData is already a fragmented UDP packet (not full frame)
            // Just broadcast it directly without re-fragmenting
            byte[] packet = task.frameData;

            // Store in retransmission buffer for NACK
            int seqNum = extractSequenceNumber(packet);
            if (seqNum > 0) {
                retransmissionBuffer.storePacket(seqNum, packet);
            }

            // Broadcast packet to all room members
            broadcastWorker.broadcastToRoom(
                    task.roomMembers,
                    task.senderId,
                    packet,
                    packet.length,
                    (byte) 2); // Media type: SCREEN

        } catch (Exception e) {
            System.err.println("[VideoWorker] Error in processFrame: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ FIX: Extract sequence number from packet header (bytes 4-7, after magic
     * number)
     * Header format: [Magic(0-3)][SeqNum(4-7)][FrameID(8-11)]...
     */
    private int extractSequenceNumber(byte[] packet) {
        if (packet == null || packet.length < 8) {
            return -1;
        }
        // Sequence number is at bytes 4-7 (after magic number 0-3)
        return ((packet[4] & 0xFF) << 24) |
                ((packet[5] & 0xFF) << 16) |
                ((packet[6] & 0xFF) << 8) |
                (packet[7] & 0xFF);
    }

    public void shutdown() {
        System.out.println("[VideoWorker] Shutting down...");
        running.set(false);

        try {
            long startWait = System.currentTimeMillis();
            while (!frameQueue.isEmpty() && (System.currentTimeMillis() - startWait) < 5000) {
                Thread.sleep(100);
            }
            if (!frameQueue.isEmpty()) {
                System.err.println("[VideoWorker] ⚠️ Shutdown timeout - " + frameQueue.size() + " frames dropped");
                frameQueue.clear();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        printStats();
    }

    public void printStats() {
        long processed = framesProcessed.get();
        long dropped = framesDropped.get();
        long totalTime = totalProcessingTime.get();

        System.out.println("\n=== Video Worker Statistics ===");
        System.out.println("Frames Processed: " + processed);
        System.out.println("Frames Dropped: " + dropped);

        if (processed > 0) {
            double avgProcessingTime = (double) totalTime / processed;
            double currentFps = processed > 0 ? 1000.0 / avgProcessingTime : 0;
            double dropRate = (double) dropped / (dropped + processed) * 100;

            System.out.printf("Avg Processing Time: %.2f ms%n", avgProcessingTime);
            System.out.printf("Current FPS: %.1f (target: %d)%n", currentFps, TARGET_FPS);
            System.out.printf("Drop Rate: %.2f%%%n", dropRate);
        }

        System.out.println("Queue Size: " + frameQueue.size());
        System.out.println("===============================\n");
    }

    public static class VideoFrameTask {
        public final String senderId;
        public final String roomId;
        public final java.util.Set<String> roomMembers;
        public final byte[] frameData;
        public final int frameId;
        public long receiveTimestamp;

        public VideoFrameTask(String senderId, String roomId,
                java.util.Set<String> roomMembers,
                byte[] frameData, int frameId) {
            this.senderId = senderId;
            this.roomId = roomId;
            this.roomMembers = roomMembers;
            this.frameData = frameData;
            this.frameId = frameId;
        }
    }
}

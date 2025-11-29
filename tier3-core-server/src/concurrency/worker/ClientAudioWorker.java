package concurrency.worker;

import server.BroadcastWorker;
import server.ClientHandler;
import streaming.network.NetworkQualityMonitor;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ClientAudioWorker implements Runnable {
    private final String clientId;
    private final ClientHandler clientHandler;
    private final BroadcastWorker broadcastWorker;
    private final NetworkQualityMonitor networkMonitor;

    private final BlockingQueue<ClientAudioTask> audioQueue;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong packetsProcessed = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong packetsDropped = new AtomicLong(0);

    // Performance targets
    private static final long MAX_LATENCY_MS = 150;
    private static final int MAX_QUEUE_SIZE = 50;

    private float volume = 1.0f;
    private boolean echoCancellation = false;

    public ClientAudioWorker(String clientId, ClientHandler clientHandler, BroadcastWorker broadcastWorker,
            NetworkQualityMonitor networkMonitor) {
        this.clientId = clientId;
        this.clientHandler = clientHandler;
        this.broadcastWorker = broadcastWorker;
        this.networkMonitor = networkMonitor;

        this.audioQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

        System.out.println("[ClientAudioWorker-" + clientId.substring(0, 8) + "] ✅ Initialized:");
        System.out.println("  - Target latency: <" + MAX_LATENCY_MS + "ms");
        System.out.println("  - Queue size: " + MAX_QUEUE_SIZE);
    }

    public boolean submitAudio(ClientAudioTask task) {
        if (!running.get()) {
            return false;
        }

        if (audioQueue.size() >= MAX_QUEUE_SIZE) {
            packetsDropped.incrementAndGet();
            System.err
                    .println("[ClientAudioWorker-" + clientId.substring(0, 8) + "] Queue full, dropping audio packet");
            return false;
        }

        task.receiveTimestamp = System.currentTimeMillis();
        return audioQueue.offer(task);
    }

    @Override
    public void run() {
        System.out.println("[ClientAudioWorker-" + clientId.substring(0, 8) + "] 🎤 Started audio worker");

        while (running.get() || !audioQueue.isEmpty()) {
            try {
                ClientAudioTask task = audioQueue.poll(100, TimeUnit.MILLISECONDS);

                if (task == null) {
                    continue;
                }

                long processingStart = System.currentTimeMillis();

                processAudio(task);

                long processingTime = System.currentTimeMillis() - processingStart;
                totalProcessingTime.addAndGet(processingTime);
                packetsProcessed.incrementAndGet();

                long latency = System.currentTimeMillis() - task.receiveTimestamp;
                if (latency > MAX_LATENCY_MS) {
                    System.err.println("[ClientAudioWorker-" + clientId.substring(0, 8) +
                            "] ⚠️ High latency: " + latency + "ms (target: <" + MAX_LATENCY_MS + "ms)");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[ClientAudioWorker-" + clientId.substring(0, 8) +
                        "] Error processing audio: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("[ClientAudioWorker-" + clientId.substring(0, 8) + "] 🛑 Stopped audio worker");
    }

    private void processAudio(ClientAudioTask task) {
        try {
            byte[] audioData = task.audioData;

            if (volume != 1.0f) {
                audioData = applyVolume(audioData, volume);
            }

            if (echoCancellation) {
            }

            Set<String> singleClientSet = Collections.singleton(clientId);

            broadcastWorker.broadcastToRoom(
                    singleClientSet,
                    null,
                    audioData,
                    audioData.length,
                    (byte) 1);

            networkMonitor.recordPacketSent(clientId);

        } catch (Exception e) {
            System.err.println("[ClientAudioWorker-" + clientId.substring(0, 8) +
                    "] Error in processAudio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private byte[] applyVolume(byte[] audioData, float volume) {
        if (volume == 1.0f || audioData == null || audioData.length < 2) {
            return audioData;
        }

        byte[] adjusted = new byte[audioData.length];
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(audioData)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        java.nio.ByteBuffer output = java.nio.ByteBuffer.wrap(adjusted)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);

        while (buffer.remaining() >= 2) {
            short sample = buffer.getShort();
            short adjustedSample = (short) (sample * volume);

            if (adjustedSample > Short.MAX_VALUE) {
                adjustedSample = Short.MAX_VALUE;
            } else if (adjustedSample < Short.MIN_VALUE) {
                adjustedSample = Short.MIN_VALUE;
            }

            output.putShort(adjustedSample);
        }

        return adjusted;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(2.0f, volume));
    }

    public void setEchoCancellation(boolean enabled) {
        this.echoCancellation = enabled;
    }

    public void shutdown() {
        System.out.println("[ClientAudioWorker-" + clientId.substring(0, 8) + "] Shutting down...");
        running.set(false);

        try {
            long startWait = System.currentTimeMillis();
            while (!audioQueue.isEmpty() && (System.currentTimeMillis() - startWait) < 2000) {
                Thread.sleep(100);
            }
            if (!audioQueue.isEmpty()) {
                System.err.println("[ClientAudioWorker-" + clientId.substring(0, 8) +
                        "] ⚠️ Shutdown timeout - " + audioQueue.size() + " packets dropped");
                audioQueue.clear();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        printStats();
    }

    public void printStats() {
        long processed = packetsProcessed.get();
        long dropped = packetsDropped.get();
        long totalTime = totalProcessingTime.get();

        System.out.println("\n=== Client Audio Worker Statistics (Client: " + clientId.substring(0, 8) + ") ===");
        System.out.println("Packets Processed: " + processed);
        System.out.println("Packets Dropped: " + dropped);

        if (processed > 0) {
            double avgProcessingTime = (double) totalTime / processed;
            double dropRate = (double) dropped / (dropped + processed) * 100;

            System.out.printf("Avg Processing Time: %.2f ms%n", avgProcessingTime);
            System.out.printf("Drop Rate: %.2f%%%n", dropRate);
        }

        System.out.println("Queue Size: " + audioQueue.size());
        System.out.println("Volume: " + volume);
        System.out.println("Echo Cancellation: " + (echoCancellation ? "ON" : "OFF"));
        System.out.println("==========================================================\n");
    }

    public static class ClientAudioTask {
        public final byte[] audioData;
        public final String sourceClientId;
        public final long timestamp;
        public long receiveTimestamp;

        public ClientAudioTask(byte[] audioData, String sourceClientId, long timestamp) {
            this.audioData = audioData;
            this.sourceClientId = sourceClientId;
            this.timestamp = timestamp;
        }
    }
}

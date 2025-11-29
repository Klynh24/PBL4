package concurrency.worker;

import server.BroadcastWorker;
import streaming.network.NetworkQualityMonitor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class AudioMixerWorker implements Runnable {
    private final BlockingQueue<AudioPacketTask> audioQueue;
    private final BroadcastWorker broadcastWorker;
    private final NetworkQualityMonitor networkMonitor;
    private final management.RoomManager roomManager;

    private final ConcurrentHashMap<String, List<AudioSample>> roomAudioBuffers;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong packetsMixed = new AtomicLong(0);
    private final AtomicLong totalMixingTime = new AtomicLong(0);
    private final AtomicLong packetsDropped = new AtomicLong(0);

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNELS = 1;
    private static final int BYTES_PER_SAMPLE = 2;
    private static final int MAX_BUFFER_MS = 50;
    private static final int MAX_QUEUE_SIZE = 100;

    public AudioMixerWorker(BroadcastWorker broadcastWorker,
            NetworkQualityMonitor networkMonitor,
            management.RoomManager roomManager) {
        this.broadcastWorker = broadcastWorker;
        this.networkMonitor = networkMonitor;
        this.roomManager = roomManager;
        this.roomAudioBuffers = new ConcurrentHashMap<>();
        this.audioQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

        System.out.println("[AudioMixerWorker] ✅ Initialized:");
        System.out.println("  - Sample Rate: " + SAMPLE_RATE + " Hz");
        System.out.println("  - Channels: " + CHANNELS + " (Mono)");
        System.out.println("  - Mix Interval: " + MAX_BUFFER_MS + "ms");
    }

    public boolean submitAudio(AudioPacketTask task) {
        if (!running.get()) {
            return false;
        }

        if (audioQueue.size() >= MAX_QUEUE_SIZE) {
            packetsDropped.incrementAndGet();
            return false;
        }

        task.receiveTimestamp = System.currentTimeMillis();
        return audioQueue.offer(task);
    }

    @Override
    public void run() {
        System.out.println("[AudioMixerWorker] 🎵 Started audio mixing worker");

        ScheduledExecutorService mixerScheduler = Executors.newScheduledThreadPool(1);
        mixerScheduler.scheduleAtFixedRate(
                this::mixAndBroadcastAllRooms,
                0,
                MAX_BUFFER_MS,
                TimeUnit.MILLISECONDS);

        while (running.get() || !audioQueue.isEmpty()) {
            try {
                AudioPacketTask task = audioQueue.poll(100, TimeUnit.MILLISECONDS);

                if (task == null) {
                    continue;
                }

                addToRoomBuffer(task);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[AudioMixerWorker] Error processing audio: " + e.getMessage());
                e.printStackTrace();
            }
        }

        mixerScheduler.shutdown();
        try {
            if (!mixerScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                mixerScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            mixerScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("[AudioMixerWorker] 🛑 Stopped audio mixing worker");
    }

    private void addToRoomBuffer(AudioPacketTask task) {
        String roomId = task.roomId;

        roomAudioBuffers.computeIfAbsent(roomId, k -> new ArrayList<>()).add(
                new AudioSample(task.senderId, task.audioData, task.timestamp));
    }

    private void mixAndBroadcastAllRooms() {
        long mixStart = System.currentTimeMillis();

        for (Map.Entry<String, List<AudioSample>> entry : roomAudioBuffers.entrySet()) {
            String roomId = entry.getKey();
            List<AudioSample> samples = entry.getValue();

            if (samples.isEmpty()) {
                continue;
            }

            try {
                byte[] mixedAudio = mixAudioSamples(samples);

                if (mixedAudio != null && mixedAudio.length > 0) {
                    Set<String> roomMembers = getRoomMembers(roomId);

                    if (roomMembers != null && !roomMembers.isEmpty()) {
                        broadcastWorker.broadcastToRoom(
                                roomMembers,
                                null,
                                mixedAudio,
                                mixedAudio.length,
                                (byte) 1);

                        packetsMixed.incrementAndGet();
                    }
                }

                samples.clear();

            } catch (Exception e) {
                System.err.println("[AudioMixerWorker] Error mixing audio for room " + roomId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        long mixTime = System.currentTimeMillis() - mixStart;
        totalMixingTime.addAndGet(mixTime);
    }

    private byte[] mixAudioSamples(List<AudioSample> samples) {
        if (samples.isEmpty()) {
            return null;
        }

        if (samples.size() == 1) {
            return samples.get(0).data;
        }

        int maxLength = 0;
        for (AudioSample sample : samples) {
            if (sample.data.length > maxLength) {
                maxLength = sample.data.length;
            }
        }

        if (maxLength == 0 || maxLength % BYTES_PER_SAMPLE != 0) {
            return null;
        }

        int sampleCount = maxLength / BYTES_PER_SAMPLE;
        short[] mixedSamples = new short[sampleCount];

        for (AudioSample sample : samples) {
            ByteBuffer buffer = ByteBuffer.wrap(sample.data)
                    .order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < sampleCount && buffer.remaining() >= BYTES_PER_SAMPLE; i++) {
                short sampleValue = buffer.getShort();
                mixedSamples[i] += sampleValue;
            }
        }

        short maxAbs = 0;
        for (short s : mixedSamples) {
            short abs = (short) Math.abs(s);
            if (abs > maxAbs) {
                maxAbs = abs;
            }
        }

        if (maxAbs > Short.MAX_VALUE) {
            float scale = (float) Short.MAX_VALUE / maxAbs;
            for (int i = 0; i < mixedSamples.length; i++) {
                mixedSamples[i] = (short) (mixedSamples[i] * scale);
            }
        }

        ByteBuffer output = ByteBuffer.allocate(mixedSamples.length * BYTES_PER_SAMPLE)
                .order(ByteOrder.LITTLE_ENDIAN);

        for (short s : mixedSamples) {
            output.putShort(s);
        }

        return output.array();
    }

    private Set<String> getRoomMembers(String roomId) {
        if (roomManager != null) {
            Set<String> members = roomManager.getRoomMembers(roomId);
            return members != null ? members : new HashSet<>();
        }
        return new HashSet<>();
    }

    public void shutdown() {
        System.out.println("[AudioMixerWorker] Shutting down...");
        running.set(false);

        try {
            long startWait = System.currentTimeMillis();
            while (!audioQueue.isEmpty() && (System.currentTimeMillis() - startWait) < 2000) {
                Thread.sleep(100);
            }
            if (!audioQueue.isEmpty()) {
                System.err
                        .println("[AudioMixerWorker] ⚠️ Shutdown timeout - " + audioQueue.size() + " packets dropped");
                audioQueue.clear();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        printStats();
    }

    public void printStats() {
        long mixed = packetsMixed.get();
        long dropped = packetsDropped.get();
        long totalTime = totalMixingTime.get();

        System.out.println("\n=== Audio Mixer Worker Statistics ===");
        System.out.println("Packets Mixed: " + mixed);
        System.out.println("Packets Dropped: " + dropped);

        if (mixed > 0) {
            double avgMixingTime = (double) totalTime / mixed;
            System.out.printf("Avg Mixing Time: %.2f ms%n", avgMixingTime);
        }

        System.out.println("Queue Size: " + audioQueue.size());
        System.out.println("Active Rooms: " + roomAudioBuffers.size());
        System.out.println("====================================\n");
    }

    public static class AudioPacketTask {
        public final String senderId;
        public final String roomId;
        public final byte[] audioData;
        public final long timestamp;
        public long receiveTimestamp;

        public AudioPacketTask(String senderId, String roomId, byte[] audioData, long timestamp) {
            this.senderId = senderId;
            this.roomId = roomId;
            this.audioData = audioData;
            this.timestamp = timestamp;
        }
    }

    private static class AudioSample {
        final String clientId;
        final byte[] data;
        final long timestamp;

        AudioSample(String clientId, byte[] data, long timestamp) {
            this.clientId = clientId;
            this.data = data;
            this.timestamp = timestamp;
        }
    }
}

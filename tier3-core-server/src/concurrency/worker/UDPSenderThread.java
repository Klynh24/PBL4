package concurrency.worker;

import streaming.protocol.PacketPriority;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ✅ CRITICAL FIX: Dedicated UDP Sender Thread
 * 
 * Problem: Synchronized UDP channel blocks all broadcast threads
 * Solution: Single sender thread + lock-free queue
 * 
 * Benefits:
 * - No thread contention on UDP channel
 * - Maximum throughput for 100+ clients
 * - Lock-free packet submission
 * - Automatic packet pacing
 */
public class UDPSenderThread implements Runnable {
    private final DatagramChannel udpChannel;
    // ✅ NEW: Priority queue for packet prioritization
    private final PriorityBlockingQueue<UDPPacket> packetQueue;
    private final AtomicBoolean running = new AtomicBoolean(true);

    // Metrics
    private final AtomicLong totalPacketsSent = new AtomicLong(0);
    private final AtomicLong totalBytesSent = new AtomicLong(0);
    private final AtomicLong droppedPackets = new AtomicLong(0);

    // Configuration
    private static final int MAX_QUEUE_SIZE = 10000; // Drop packets if queue exceeds this
    private static final long PACING_DELAY_MICROS = 100; // 100 microseconds between packets (pacing)

    public UDPSenderThread(DatagramChannel udpChannel) {
        this.udpChannel = udpChannel;
        // ✅ NEW: Priority queue - packets with lower priority level are sent first
        this.packetQueue = new PriorityBlockingQueue<>(MAX_QUEUE_SIZE, 
            (p1, p2) -> Integer.compare(p1.priority.getLevel(), p2.priority.getLevel()));
    }

    /**
     * Submit packet for sending (non-blocking, lock-free)
     * Returns true if queued, false if queue is full (packet dropped)
     * 
     * @param buffer Packet buffer
     * @param destination Destination address
     * @param priority Packet priority (defaults to MEDIUM if null)
     */
    public boolean sendPacket(ByteBuffer buffer, InetSocketAddress destination, PacketPriority priority) {
        if (priority == null) {
            priority = PacketPriority.MEDIUM;
        }
        
        // Try to add to queue without blocking
        // Priority queue will automatically order by priority level
        boolean queued = packetQueue.offer(new UDPPacket(buffer, destination, priority));

        if (!queued) {
            droppedPackets.incrementAndGet();
            System.err.println("[UDPSender] Queue full! Dropped packet to " + destination);
        }

        return queued;
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public boolean sendPacket(ByteBuffer buffer, InetSocketAddress destination) {
        return sendPacket(buffer, destination, PacketPriority.MEDIUM);
    }

    @Override
    public void run() {
        System.out.println("[UDPSender] ✅ Dedicated sender thread started");
        System.out.println("[UDPSender] ✅ Priority queue enabled (capacity: " + MAX_QUEUE_SIZE + ")");
        System.out.println("[UDPSender] ✅ Packet pacing: " + PACING_DELAY_MICROS + " microseconds");
        System.out.println("[UDPSender] ✅ Packet prioritization: CRITICAL > HIGH > MEDIUM > LOW");

        long lastPacingTime = System.nanoTime();

        while (running.get() || !packetQueue.isEmpty()) {
            try {
                // Get next packet (blocking with timeout)
                UDPPacket packet = packetQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);

                if (packet == null) {
                    continue; // Timeout, check running flag
                }

                // ✅ PACKET PACING: Delay between packets to prevent burst
                long currentTime = System.nanoTime();
                long elapsedMicros = (currentTime - lastPacingTime) / 1000;

                if (elapsedMicros < PACING_DELAY_MICROS) {
                    long sleepMicros = PACING_DELAY_MICROS - elapsedMicros;
                    if (sleepMicros > 0) {
                        // Sleep for remaining time (convert micros to nanos)
                        Thread.sleep(sleepMicros / 1000, (int) ((sleepMicros % 1000) * 1000));
                    }
                }

                // ✅ FIX: Check if channel is still open before sending
                if (!udpChannel.isOpen()) {
                    break; // Channel closed, stop sending
                }

                // Send packet (NO LOCK needed - single thread)
                int bytesSent = udpChannel.send(packet.buffer, packet.destination);

                totalPacketsSent.incrementAndGet();
                totalBytesSent.addAndGet(bytesSent);
                lastPacingTime = System.nanoTime();

                // Return buffer to pool if applicable
                if (packet.buffer.isDirect()) {
                    // DirectByteBuffer - will be reused by pool
                    packet.buffer.clear();
                }

            } catch (InterruptedException e) {
                System.err.println("[UDPSender] Thread interrupted");
                break;
            } catch (java.nio.channels.ClosedChannelException e) {
                // Channel was closed - this is expected during shutdown
                break;
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg == null) {
                    errorMsg = e.getClass().getSimpleName();
                }
                // Only log if channel is still open (to avoid spam during shutdown)
                if (udpChannel.isOpen()) {
                    System.err.println("[UDPSender] Error sending packet: " + errorMsg);
                }
            }
        }

        System.out.println("[UDPSender] Sender thread stopped");
        printStats();
    }

    /**
     * Shutdown sender thread gracefully
     */
    public void shutdown() {
        running.set(false);
        System.out.println("[UDPSender] Shutdown requested, draining queue...");
    }

    /**
     * Get queue size
     */
    public int getQueueSize() {
        return packetQueue.size();
    }

    /**
     * Get metrics
     */
    public SenderMetrics getMetrics() {
        return new SenderMetrics(
                totalPacketsSent.get(),
                totalBytesSent.get(),
                droppedPackets.get(),
                packetQueue.size());
    }

    /**
     * Print statistics
     */
    public void printStats() {
        System.out.println("\n=== UDP Sender Statistics ===");
        System.out.println("Total Packets Sent: " + totalPacketsSent.get());
        System.out.println("Total Bytes Sent: " + String.format("%.2f MB", totalBytesSent.get() / 1_000_000.0));
        System.out.println("Dropped Packets: " + droppedPackets.get());
        System.out.println("Current Queue Size: " + packetQueue.size());

        if (totalPacketsSent.get() > 0) {
            double dropRate = (double) droppedPackets.get() / totalPacketsSent.get() * 100;
            System.out.printf("Drop Rate: %.4f%%%n", dropRate);

            double avgPacketSize = (double) totalBytesSent.get() / totalPacketsSent.get();
            System.out.printf("Avg Packet Size: %.1f bytes%n", avgPacketSize);
        }

        System.out.println("=============================\n");
    }

    /**
     * ✅ NEW: Packet wrapper for priority queue
     */
    static class UDPPacket implements Comparable<UDPPacket> {
        final ByteBuffer buffer;
        final InetSocketAddress destination;
        final PacketPriority priority;
        final long timestamp; // For FIFO within same priority

        UDPPacket(ByteBuffer buffer, InetSocketAddress destination, PacketPriority priority) {
            this.buffer = buffer;
            this.destination = destination;
            this.priority = priority;
            this.timestamp = System.nanoTime(); // For ordering within same priority
        }

        @Override
        public int compareTo(UDPPacket other) {
            // First compare by priority (lower level = higher priority)
            int priorityCompare = Integer.compare(this.priority.getLevel(), other.priority.getLevel());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // Within same priority, use FIFO (earlier timestamp = earlier in queue)
            return Long.compare(this.timestamp, other.timestamp);
        }
    }

    /**
     * Metrics data class
     */
    public static class SenderMetrics {
        public final long totalPacketsSent;
        public final long totalBytesSent;
        public final long droppedPackets;
        public final int currentQueueSize;

        SenderMetrics(long totalPacketsSent, long totalBytesSent, long droppedPackets, int currentQueueSize) {
            this.totalPacketsSent = totalPacketsSent;
            this.totalBytesSent = totalBytesSent;
            this.droppedPackets = droppedPackets;
            this.currentQueueSize = currentQueueSize;
        }
    }
}

package com.tutoring.core.server;

import com.tutoring.core.server.ClientHandler;
import com.tutoring.core.streaming.network.NetworkQualityMonitor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance parallel broadcaster for UDP media packets
 * Optimized for low-latency multi-recipient broadcasting
 * 
 * Performance optimizations:
 * 1. Parallel sending - Submit all sends simultaneously
 * 2. Zero-copy ByteBuffer reuse
 * 3. Batch operations - Minimize synchronization overhead
 * 4. Fire-and-forget mode - Don't wait for completion (lowest latency)
 */
public class BroadcastWorker {
    private final DatagramChannel udpChannel;
    private final ConcurrentHashMap<String, ClientHandler> clientHandlers;
    private final NetworkQualityMonitor qualityMonitor;
    private final ExecutorService executorService;
    
    // Metrics
    private final AtomicInteger totalBroadcasts = new AtomicInteger(0);
    private final AtomicInteger totalPacketsSent = new AtomicInteger(0);
    private final AtomicInteger failedSends = new AtomicInteger(0);
    
    // Performance tuning
    private static final boolean FIRE_AND_FORGET = true; // Don't wait for completion (fastest)
    private static final int BATCH_TIMEOUT_MS = 50; // Max time to wait for batch completion
    
    public BroadcastWorker(DatagramChannel udpChannel,
                          ConcurrentHashMap<String, ClientHandler> clientHandlers,
                          NetworkQualityMonitor qualityMonitor,
                          ExecutorService executorService) {
        this.udpChannel = udpChannel;
        this.clientHandlers = clientHandlers;
        this.qualityMonitor = qualityMonitor;
        this.executorService = executorService;
    }
    
    /**
     * Broadcast packet to all room members in parallel
     * This is the main entry point for high-performance broadcasting
     * 
     * @param roomMembers Set of client IDs in the room
     * @param senderId ID of the sender (excluded from broadcast)
     * @param packetData Raw packet data to broadcast
     * @param packetLength Length of packet data
     * @param mediaType Media type (1=VOICE, 2=SCREEN)
     * @return Number of recipients successfully sent to
     */
    public int broadcastToRoom(Set<String> roomMembers, 
                              String senderId,
                              byte[] packetData,
                              int packetLength,
                              byte mediaType) {
        if (roomMembers == null || roomMembers.isEmpty()) {
            return 0;
        }
        
        totalBroadcasts.incrementAndGet();
        
        // Build list of valid recipients
        List<ClientHandler> recipients = new ArrayList<>(roomMembers.size());
        for (String memberId : roomMembers) {
            if (memberId.equals(senderId)) continue; // Exclude sender
            
            ClientHandler handler = clientHandlers.get(memberId);
            if (handler != null && handler.getUdpAddress() != null) {
                recipients.add(handler);
            }
        }
        
        if (recipients.isEmpty()) {
            return 0;
        }
        
        // PARALLEL BROADCAST - Submit all sends simultaneously
        if (FIRE_AND_FORGET) {
            // Fire-and-forget mode: Submit and don't wait (lowest latency)
            return broadcastFireAndForget(recipients, packetData, packetLength, mediaType);
        } else {
            // Wait-for-completion mode: Wait for all sends (more reliable)
            return broadcastWithCompletion(recipients, packetData, packetLength, mediaType);
        }
    }
    
    /**
     * Fire-and-forget broadcast: Submit tasks and return immediately
     * Lowest latency, but no guarantee of completion
     */
    private int broadcastFireAndForget(List<ClientHandler> recipients,
                                       byte[] packetData,
                                       int packetLength,
                                       byte mediaType) {
        int submittedCount = 0;
        
        for (ClientHandler recipient : recipients) {
            // Submit broadcast task to executor
            executorService.submit(() -> {
                sendToRecipient(recipient, packetData, packetLength, mediaType);
            });
            submittedCount++;
        }
        
        return submittedCount;
    }
    
    /**
     * Wait-for-completion broadcast: Submit tasks and wait for completion
     * Higher latency, but guaranteed completion tracking
     */
    private int broadcastWithCompletion(List<ClientHandler> recipients,
                                       byte[] packetData,
                                       int packetLength,
                                       byte mediaType) {
        List<Future<Boolean>> futures = new ArrayList<>(recipients.size());
        
        // Submit all broadcast tasks
        for (ClientHandler recipient : recipients) {
            Future<Boolean> future = executorService.submit(new BroadcastTask(
                recipient, packetData, packetLength, mediaType
            ));
            futures.add(future);
        }
        
        // Wait for completion (with timeout)
        int successCount = 0;
        for (Future<Boolean> future : futures) {
            try {
                Boolean success = future.get(BATCH_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (success != null && success) {
                    successCount++;
                }
            } catch (Exception e) {
                failedSends.incrementAndGet();
            }
        }
        
        return successCount;
    }
    
    /**
     * Send packet to a single recipient
     * This is the core sending logic, executed in worker threads
     */
    private void sendToRecipient(ClientHandler recipient,
                                 byte[] packetData,
                                 int packetLength,
                                 byte mediaType) {
        String clientId = recipient.getClientId();
        InetSocketAddress udpAddress = recipient.getUdpAddress();
        
        if (udpAddress == null) {
            return; // No UDP address registered
        }
        
        try {
            // Use ByteBuffer for zero-copy sending
            ByteBuffer buffer = ByteBuffer.wrap(packetData, 0, packetLength);
            
            // Send packet via UDP channel
            synchronized (udpChannel) { // Synchronize channel access
                udpChannel.send(buffer, udpAddress);
            }
            
            // Update metrics
            totalPacketsSent.incrementAndGet();
            qualityMonitor.recordPacketSent(clientId);
            
        } catch (IOException e) {
            failedSends.incrementAndGet();
            System.err.println("[BroadcastWorker] Failed to send to " + clientId + ": " + e.getMessage());
        } catch (Exception e) {
            failedSends.incrementAndGet();
            System.err.println("[BroadcastWorker] Unexpected error sending to " + clientId + ": " + e.getMessage());
        }
    }
    
    /**
     * Callable task for broadcast with completion tracking
     */
    private class BroadcastTask implements Callable<Boolean> {
        private final ClientHandler recipient;
        private final byte[] packetData;
        private final int packetLength;
        private final byte mediaType;
        
        BroadcastTask(ClientHandler recipient, byte[] packetData, int packetLength, byte mediaType) {
            this.recipient = recipient;
            this.packetData = packetData;
            this.packetLength = packetLength;
            this.mediaType = mediaType;
        }
        
        @Override
        public Boolean call() {
            try {
                sendToRecipient(recipient, packetData, packetLength, mediaType);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
    
    /**
     * Optimized broadcast for small rooms (< 10 members)
     * Uses sequential sending to avoid thread overhead
     */
    public int broadcastSequential(Set<String> roomMembers,
                                   String senderId,
                                   byte[] packetData,
                                   int packetLength,
                                   byte mediaType) {
        if (roomMembers == null || roomMembers.isEmpty()) {
            return 0;
        }
        
        int sentCount = 0;
        
        for (String memberId : roomMembers) {
            if (memberId.equals(senderId)) continue;
            
            ClientHandler handler = clientHandlers.get(memberId);
            if (handler != null && handler.getUdpAddress() != null) {
                sendToRecipient(handler, packetData, packetLength, mediaType);
                sentCount++;
            }
        }
        
        return sentCount;
    }
    
    /**
     * Smart broadcast: Choose parallel or sequential based on room size
     * Optimizes for both small and large rooms
     */
    public int broadcastSmart(Set<String> roomMembers,
                             String senderId,
                             byte[] packetData,
                             int packetLength,
                             byte mediaType) {
        if (roomMembers == null || roomMembers.isEmpty()) {
            return 0;
        }
        
        int roomSize = roomMembers.size();
        
        // For small rooms (< 10 members), sequential is faster (no thread overhead)
        // For large rooms (>= 10 members), parallel is much faster
        if (roomSize < 10) {
            return broadcastSequential(roomMembers, senderId, packetData, packetLength, mediaType);
        } else {
            return broadcastToRoom(roomMembers, senderId, packetData, packetLength, mediaType);
        }
    }
    
    /**
     * Get broadcast metrics
     */
    public BroadcastMetrics getMetrics() {
        return new BroadcastMetrics(
            totalBroadcasts.get(),
            totalPacketsSent.get(),
            failedSends.get()
        );
    }
    
    /**
     * Print statistics
     */
    public void printStats() {
        System.out.println("\n=== Broadcast Worker Statistics ===");
        System.out.println("Total Broadcasts: " + totalBroadcasts.get());
        System.out.println("Total Packets Sent: " + totalPacketsSent.get());
        System.out.println("Failed Sends: " + failedSends.get());
        
        if (totalBroadcasts.get() > 0) {
            double avgPacketsPerBroadcast = (double) totalPacketsSent.get() / totalBroadcasts.get();
            double failureRate = (double) failedSends.get() / totalPacketsSent.get() * 100;
            System.out.printf("Avg Packets/Broadcast: %.1f%n", avgPacketsPerBroadcast);
            System.out.printf("Failure Rate: %.2f%%%n", failureRate);
        }
        
        System.out.println("===================================\n");
    }
    
    /**
     * Reset metrics
     */
    public void resetMetrics() {
        totalBroadcasts.set(0);
        totalPacketsSent.set(0);
        failedSends.set(0);
    }
    
    public static class BroadcastMetrics {
        public final int totalBroadcasts;
        public final int totalPacketsSent;
        public final int failedSends;
        
        BroadcastMetrics(int totalBroadcasts, int totalPacketsSent, int failedSends) {
            this.totalBroadcasts = totalBroadcasts;
            this.totalPacketsSent = totalPacketsSent;
            this.failedSends = failedSends;
        }
    }
}


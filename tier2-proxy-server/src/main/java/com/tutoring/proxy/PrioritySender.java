package com.tutoring.proxy;

import javax.websocket.Session;
import java.nio.ByteBuffer;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ✅ PACKET PRIORITIZATION: Priority sender for WebSocket binary messages
 * 
 * Sends packets to WebSocket client in priority order
 */
public class PrioritySender implements Runnable {
    private final Session session;
    private final PriorityBlockingQueue<PrioritizedPacket> queue;
    private final AtomicBoolean running = new AtomicBoolean(true);
    
    private static final int MAX_QUEUE_SIZE = 5000;
    
    public PrioritySender(Session session) {
        this.session = session;
        this.queue = new PriorityBlockingQueue<>(MAX_QUEUE_SIZE,
            (p1, p2) -> {
                int priorityCompare = Integer.compare(p1.priority.getLevel(), p2.priority.getLevel());
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                return Long.compare(p1.timestamp, p2.timestamp);
            });
    }
    
    /**
     * Enqueue packet for sending
     */
    public boolean send(byte[] data, PacketPriority priority) {
        if (!session.isOpen() || !running.get()) {
            return false;
        }
        
        if (priority == null) {
            priority = PacketPriority.MEDIUM;
        }
        
        return queue.offer(new PrioritizedPacket(data, priority, System.nanoTime()));
    }
    
    @Override
    public void run() {
        while (running.get() && session.isOpen()) {
            try {
                PrioritizedPacket packet = queue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (packet == null) {
                    continue;
                }
                
                if (!session.isOpen()) {
                    break;
                }
                
                ByteBuffer buffer = ByteBuffer.wrap(packet.data);
                session.getBasicRemote().sendBinary(buffer);
                
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                if (session.isOpen()) {
                    System.err.println("[PrioritySender] Error sending packet: " + e.getMessage());
                }
            }
        }
    }
    
    public void shutdown() {
        running.set(false);
    }
    
    private static class PrioritizedPacket {
        final byte[] data;
        final PacketPriority priority;
        final long timestamp;
        
        PrioritizedPacket(byte[] data, PacketPriority priority, long timestamp) {
            this.data = data;
            this.priority = priority;
            this.timestamp = timestamp;
        }
    }
}


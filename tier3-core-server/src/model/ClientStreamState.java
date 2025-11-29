package model;

/**
 * ✅ RESTORED: Per-Client Stream State
 * 
 * Tracks streaming state for each connected client
 * Used for:
 * - Keyframe management (new clients need keyframe)
 * - Packet loss tracking
 * - Adaptive bitrate decisions
 * - Quality adjustment per client
 */
public class ClientStreamState {
    private final String clientId;
    private boolean needsKeyframe;
    private long lastKeyframeTime;
    private int lastReceivedFrameId;
    
    // For adaptive bitrate
    private double estimatedBandwidth;
    private int lostPacketCount;
    private int totalPacketCount;
    
    public ClientStreamState(String clientId) {
        this.clientId = clientId;
        this.needsKeyframe = true; // New clients always need keyframe
        this.lastKeyframeTime = 0;
        this.lastReceivedFrameId = -1;
        this.estimatedBandwidth = 0.0;
        this.lostPacketCount = 0;
        this.totalPacketCount = 0;
    }
    
    public synchronized boolean needsKeyframe() {
        return needsKeyframe;
    }
    
    public synchronized void keyframeSent() {
        needsKeyframe = false;
        lastKeyframeTime = System.currentTimeMillis();
    }
    
    public synchronized void requestKeyframe() {
        needsKeyframe = true;
    }
    
    /**
     * Check if periodic keyframe should be sent (every 5 seconds)
     */
    public boolean shouldSendPeriodicKeyframe() {
        return System.currentTimeMillis() - lastKeyframeTime > 5000;
    }
    
    /**
     * Report packet loss for this client
     */
    public synchronized void reportPacketLoss(int lostPackets) {
        lostPacketCount += lostPackets;
        totalPacketCount += lostPackets; // Assume we sent them
    }
    
    /**
     * Report successful packet delivery
     */
    public synchronized void reportPacketSuccess(int packets) {
        totalPacketCount += packets;
    }
    
    /**
     * Get packet loss rate (0.0 to 1.0)
     */
    public synchronized double getPacketLossRate() {
        if (totalPacketCount == 0) {
            return 0.0;
        }
        return (double) lostPacketCount / totalPacketCount;
    }
    
    /**
     * Update estimated bandwidth (in Mbps)
     */
    public synchronized void updateBandwidth(double mbps) {
        // Exponential moving average
        if (estimatedBandwidth == 0) {
            estimatedBandwidth = mbps;
        } else {
            estimatedBandwidth = 0.7 * estimatedBandwidth + 0.3 * mbps;
        }
    }
    
    public synchronized double getEstimatedBandwidth() {
        return estimatedBandwidth;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public synchronized int getLastReceivedFrameId() {
        return lastReceivedFrameId;
    }
    
    public synchronized void setLastReceivedFrameId(int frameId) {
        lastReceivedFrameId = frameId;
    }
    
    /**
     * Reset state (e.g., when client reconnects)
     */
    public synchronized void reset() {
        needsKeyframe = true;
        lastKeyframeTime = 0;
        lastReceivedFrameId = -1;
        lostPacketCount = 0;
        totalPacketCount = 0;
    }
}


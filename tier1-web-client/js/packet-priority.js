/**
 * ✅ PACKET PRIORITIZATION
 * 
 * Client-side packet priority system for WebSocket binary messages
 * 
 * Priority Levels:
 * - CRITICAL (0): Voice packets - lowest latency required
 * - HIGH (1): Keyframes - needed for video sync
 * - MEDIUM (2): Delta frames - can tolerate slight delay
 * - LOW (3): FEC packets - redundant data, lowest priority
 */
class PacketPriority {
    static CRITICAL = { level: 0, name: 'CRITICAL' };
    static HIGH = { level: 1, name: 'HIGH' };
    static MEDIUM = { level: 2, name: 'MEDIUM' };
    static LOW = { level: 3, name: 'LOW' };

    /**
     * Determine priority from packet header
     * @param {ArrayBuffer|Uint8Array} packetData - Raw packet data (must have at least 28 bytes header)
     * @returns {Object} PacketPriority object
     */
    static fromPacket(packetData) {
        if (!packetData || packetData.byteLength < 28) {
            return PacketPriority.MEDIUM;
        }

        const view = new DataView(packetData.buffer || packetData);
        
        // Check if FEC packet (byte 27, bit 0)
        const flags = view.getUint8(27);
        const isFec = (flags & 0x01) !== 0;
        if (isFec) {
            return PacketPriority.LOW;
        }

        // Check frame type (byte 16)
        const frameType = view.getUint8(16);
        
        if (frameType === 0x01) { // Keyframe
            return PacketPriority.HIGH;
        }
        
        if (frameType === 0x02) { // Delta
            return PacketPriority.MEDIUM;
        }

        // Check media type (byte 28, if packet is advanced format)
        if (packetData.byteLength > 28) {
            const mediaType = view.getUint8(28);
            if (mediaType === 1) { // VOICE
                return PacketPriority.CRITICAL;
            }
        }

        return PacketPriority.MEDIUM;
    }

    /**
     * Determine priority from media type and frame type
     * @param {number} mediaType - Media type (1=VOICE, 2=SCREEN)
     * @param {number} frameType - Frame type (0x01=keyframe, 0x02=delta)
     * @param {boolean} isFecPacket - Whether this is an FEC packet
     * @returns {Object} PacketPriority object
     */
    static fromTypes(mediaType, frameType, isFecPacket) {
        if (isFecPacket) {
            return PacketPriority.LOW;
        }
        
        if (mediaType === 1) { // VOICE
            return PacketPriority.CRITICAL;
        }
        
        if (frameType === 0x01) { // KEYFRAME
            return PacketPriority.HIGH;
        }
        
        if (frameType === 0x02) { // DELTA
            return PacketPriority.MEDIUM;
        }
        
        return PacketPriority.MEDIUM;
    }
}

/**
 * ✅ PRIORITY QUEUE: Priority sender for WebSocket binary messages
 * 
 * Sends packets to WebSocket server in priority order
 */
class PrioritySender {
    constructor(websocket, maxQueueSize = 5000) {
        this.ws = websocket;
        this.queue = [];
        this.maxQueueSize = maxQueueSize;
        this.sending = false;
        this.stats = {
            packetsQueued: 0,
            packetsSent: 0,
            packetsDropped: 0
        };
    }

    /**
     * Enqueue packet for sending
     * @param {ArrayBuffer|Uint8Array} data - Packet data
     * @param {Object} priority - PacketPriority object
     * @returns {boolean} True if queued successfully
     */
    send(data, priority) {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            return false;
        }

        if (!priority) {
            priority = PacketPriority.fromPacket(data);
        }

        // Drop packet if queue is full (drop lowest priority packets first)
        if (this.queue.length >= this.maxQueueSize) {
            // Find and remove lowest priority packet
            let lowestIdx = -1;
            let lowestLevel = -1;
            for (let i = 0; i < this.queue.length; i++) {
                if (lowestIdx === -1 || this.queue[i].priority.level > lowestLevel) {
                    lowestIdx = i;
                    lowestLevel = this.queue[i].priority.level;
                }
            }
            if (lowestIdx >= 0 && this.queue[lowestIdx].priority.level > priority.level) {
                // Remove lowest priority packet and add new one
                this.queue.splice(lowestIdx, 1);
                this.stats.packetsDropped++;
            } else {
                // New packet has lower priority, drop it
                this.stats.packetsDropped++;
                return false;
            }
        }

        this.queue.push({
            data: data,
            priority: priority,
            timestamp: performance.now()
        });

        this.stats.packetsQueued++;

        // Trigger send if not already sending
        if (!this.sending) {
            this.processQueue();
        }

        return true;
    }

    /**
     * Process queue and send packets in priority order
     */
    async processQueue() {
        if (this.sending || !this.ws || this.ws.readyState !== WebSocket.OPEN) {
            return;
        }

        this.sending = true;

        while (this.queue.length > 0 && this.ws.readyState === WebSocket.OPEN) {
            // Sort by priority (lower level = higher priority), then by timestamp
            this.queue.sort((a, b) => {
                const priorityDiff = a.priority.level - b.priority.level;
                if (priorityDiff !== 0) {
                    return priorityDiff;
                }
                return a.timestamp - b.timestamp;
            });

            const packet = this.queue.shift();
            
            try {
                this.ws.send(packet.data);
                this.stats.packetsSent++;
            } catch (error) {
                console.error('[PrioritySender] Error sending packet:', error);
                break;
            }

            // Small delay to prevent overwhelming the WebSocket
            if (this.queue.length > 0) {
                await new Promise(resolve => setTimeout(resolve, 0));
            }
        }

        this.sending = false;
    }

    /**
     * Get statistics
     */
    getStats() {
        return { ...this.stats, queueSize: this.queue.length };
    }

    /**
     * Clear queue
     */
    clear() {
        this.queue = [];
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = { PacketPriority, PrioritySender };
}


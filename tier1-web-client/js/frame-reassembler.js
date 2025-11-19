/**
 * Client-side Frame Reassembler for Phase 2: Reliability Layer
 * Handles packet reassembly, NACK requests, and jitter buffer
 * 
 * From IMPLEMENTATION_ROADMAP.md Phase 2, Task 2.2
 */
class FrameReassembler {
    constructor(websocket) {
        this.ws = websocket;
        
        // Frame buffer: frameId -> { fragments: Map, totalFragments: int, timestamp: long }
        this.frameBuffer = new Map();
        
        // Jitter buffer: stores completed frames for ordered playback
        this.jitterBuffer = [];
        this.maxJitterBufferSize = 5;
        
        // Last rendered frame ID (for ordering)
        this.lastRenderedFrameId = -1;
        
        // Statistics
        this.stats = {
            packetsReceived: 0,
            framesCompleted: 0,
            framesDropped: 0,
            nacksSent: 0,
            packetsLost: 0
        };
        
        // NACK configuration
        this.nackTimeoutMs = 100; // Wait 100ms before sending NACK
        this.maxNackAttempts = 3;
        this.nackTimers = new Map(); // frameId -> timeoutId
        
        // Cleanup old frames every 5 seconds
        setInterval(() => this.cleanupOldFrames(), 5000);
        
        // ✅ CRITICAL: Cleanup corrupted frames every 2 seconds
        setInterval(() => {
            const cleaned = this.cleanupCorruptedFrames();
            if (cleaned > 0) {
                console.warn('[Reassembler] Cleaned up', cleaned, 'corrupted frames');
            }
        }, 2000);
    }
    
    /**
     * Process incoming UDP packet (binary data)
     * 
     * @param {ArrayBuffer} packetData Raw UDP packet data
     */
    processPacket(packetData) {
        this.stats.packetsReceived++;
        
        // Parse header (28 bytes)
        const header = this.parseHeader(packetData);
        
        if (!header) {
            console.error('[Reassembler] Invalid packet header, packet size:', packetData.byteLength);
            return null;
        }
        
        // ✅ DEBUG: Log first few packets to verify parsing
        if (this.stats.packetsReceived <= 5) {
            console.log('[Reassembler] Packet #' + this.stats.packetsReceived + ':', 
                'frameId=' + header.frameId, 
                'fragment=' + header.fragmentIndex + '/' + header.totalFragments,
                'size=' + packetData.byteLength);
        }
        
        // Fast path: single fragment frame
        if (header.totalFragments === 1) {
            const payload = new Uint8Array(packetData, 28);
            return this.onFrameComplete(header.frameId, payload, header.isKeyframe);
        }
        
        // Multi-fragment frame: store fragment
        this.storeFragment(header, packetData);
        
        // Check if frame is complete
        const frame = this.frameBuffer.get(header.frameId);
        if (frame && frame.fragments.size === header.totalFragments) {
            console.log('[Reassembler] Frame complete:', header.frameId, 
                '(' + frame.fragments.size + '/' + header.totalFragments + ' fragments)');
            return this.assembleFrame(header.frameId);
        }
        
        // Schedule NACK check if not already scheduled
        if (!this.nackTimers.has(header.frameId)) {
            const timerId = setTimeout(() => {
                this.checkForMissingFragments(header.frameId);
            }, this.nackTimeoutMs);
            
            this.nackTimers.set(header.frameId, timerId);
        }
        
        return null; // Frame not yet complete
    }
    
    /**
     * Parse 28-byte packet header
     * 
     * Header Format:
     * - Magic Number (4 bytes): 0x53435245 ('SCRE')
     * - Sequence Number (4 bytes)
     * - Frame ID (4 bytes)
     * - Fragment Index (4 bytes)
     * - Total Fragments (4 bytes)
     * - Frame Type (4 bytes): 1=Keyframe, 2=Delta
     * - Reserved (4 bytes)
     */
    parseHeader(packetData) {
        if (packetData.byteLength < 28) {
            return null;
        }
        
        const view = new DataView(packetData);
        
        // Check magic number
        const magic = view.getUint32(0, false); // Big-endian
        if (magic !== 0x53435245) {
            console.error('[Reassembler] Invalid magic number:', magic.toString(16));
            return null;
        }
        
        // ✅ CRITICAL FIX: fragmentIndex and totalFragments are 2 bytes (short), not 4 bytes!
        // Java: putShort() → JavaScript: getUint16()
        // ByteBuffer in Java uses BIG-ENDIAN by default
        const fragmentIndex = view.getUint16(12, false); // Big-endian
        const totalFragments = view.getUint16(14, false); // Big-endian
        
        // ✅ CRITICAL: Validate parsed values to prevent infinite loops
        if (totalFragments > 10000 || fragmentIndex >= 10000) {
            console.error('[Reassembler] Invalid fragment values:', {
                fragmentIndex: fragmentIndex,
                totalFragments: totalFragments,
                'raw bytes 12-15': Array.from(new Uint8Array(packetData.slice(12, 16)))
                    .map(b => '0x' + b.toString(16).padStart(2, '0')).join(' ')
            });
            return null; // Reject invalid packet
        }
        
        return {
            magic: magic,
            sequenceNumber: view.getUint32(4, false),
            frameId: view.getUint32(8, false),
            fragmentIndex: fragmentIndex,
            totalFragments: totalFragments,
            frameType: view.getUint8(16), // ✅ FIX: 1 byte at offset 16
            isKeyframe: view.getUint8(16) === 1
        };
    }
    
    /**
     * Store a fragment in the frame buffer
     */
    storeFragment(header, packetData) {
        // ✅ CRITICAL: Validate header before storing
        if (!header || header.totalFragments > 10000 || header.fragmentIndex >= 10000) {
            console.error('[Reassembler] Invalid header, cannot store fragment:', header);
            return;
        }
        
        let frame = this.frameBuffer.get(header.frameId);
        
        if (!frame) {
            frame = {
                fragments: new Map(),
                totalFragments: header.totalFragments,
                isKeyframe: header.isKeyframe,
                timestamp: Date.now(),
                nackAttempts: 0
            };
            this.frameBuffer.set(header.frameId, frame);
        } else {
            // ✅ CRITICAL: If totalFragments mismatch, use the larger value (shouldn't happen, but safety check)
            if (frame.totalFragments !== header.totalFragments) {
                console.warn('[Reassembler] totalFragments mismatch for frame', header.frameId,
                    ': stored=' + frame.totalFragments + ', header=' + header.totalFragments);
                // Use the larger value (should be consistent, but handle edge case)
                frame.totalFragments = Math.max(frame.totalFragments, header.totalFragments);
            }
        }
        
        // Store payload (skip 28-byte header)
        const payload = new Uint8Array(packetData, 28);
        frame.fragments.set(header.fragmentIndex, payload);
    }
    
    /**
     * Assemble complete frame from fragments
     */
    assembleFrame(frameId) {
        const frame = this.frameBuffer.get(frameId);
        if (!frame) return null;
        
        // Cancel NACK timer
        if (this.nackTimers.has(frameId)) {
            clearTimeout(this.nackTimers.get(frameId));
            this.nackTimers.delete(frameId);
        }
        
        // Calculate total size
        let totalSize = 0;
        for (let fragment of frame.fragments.values()) {
            totalSize += fragment.length;
        }
        
        // Combine fragments in order
        const assembled = new Uint8Array(totalSize);
        let offset = 0;
        
        for (let i = 0; i < frame.totalFragments; i++) {
            const fragment = frame.fragments.get(i);
            
            if (!fragment) {
                console.error('[Reassembler] Missing fragment', i, 'for frame', frameId);
                this.frameBuffer.delete(frameId);
                this.stats.framesDropped++;
                return null;
            }
            
            assembled.set(fragment, offset);
            offset += fragment.length;
        }
        
        // Remove from buffer
        this.frameBuffer.delete(frameId);
        this.stats.framesCompleted++;
        
        return this.onFrameComplete(frameId, assembled, frame.isKeyframe);
    }
    
    /**
     * Check for missing fragments and send NACK
     */
    checkForMissingFragments(frameId) {
        const frame = this.frameBuffer.get(frameId);
        
        if (!frame) {
            // Frame was completed or dropped
            return;
        }
        
        // Check if frame is now complete
        if (frame.fragments.size === frame.totalFragments) {
            this.assembleFrame(frameId);
            return;
        }
        
        // Check if too many NACK attempts
        if (frame.nackAttempts >= this.maxNackAttempts) {
            console.warn('[Reassembler] Too many NACKs for frame', frameId, ', dropping');
            this.frameBuffer.delete(frameId);
            this.nackTimers.delete(frameId);
            this.stats.framesDropped++;
            return;
        }
        
        // Find missing fragments
        const missing = [];
        // ✅ CRITICAL: Validate totalFragments before looping
        if (frame.totalFragments > 10000) {
            console.error('[Reassembler] Invalid totalFragments:', frame.totalFragments, 
                'for frame', frameId, '- dropping frame');
            this.frameBuffer.delete(frameId);
            this.nackTimers.delete(frameId);
            this.stats.framesDropped++;
            return;
        }
        
        for (let i = 0; i < frame.totalFragments; i++) {
            if (!frame.fragments.has(i)) {
                missing.push(i);
            }
        }
        
        if (missing.length > 0) {
            this.sendNACK(frameId, missing);
            frame.nackAttempts++;
            this.stats.nacksSent++;
            this.stats.packetsLost += missing.length;
            
            // Schedule another check
            const timerId = setTimeout(() => {
                this.checkForMissingFragments(frameId);
            }, this.nackTimeoutMs * 2); // Exponential backoff
            
            this.nackTimers.set(frameId, timerId);
        }
    }
    
    /**
     * Send NACK request to server via WebSocket (TCP)
     * 
     * @param {number} frameId Frame ID with missing packets
     * @param {number[]} missingIndices Array of missing fragment indices
     */
    sendNACK(frameId, missingIndices) {
        console.log('[Reassembler] Sending NACK for frame', frameId, 
            ', missing fragments:', missingIndices);
        
        // Format: "NACK:frameId:index1,index2,index3"
        const message = `NACK:${frameId}:${missingIndices.join(',')}`;
        
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(message);
        } else {
            console.error('[Reassembler] Cannot send NACK, WebSocket not open');
        }
    }
    
    /**
     * Handle completed frame
     * Adds to jitter buffer for ordered playback
     */
    onFrameComplete(frameId, frameData, isKeyframe) {
        // console.log('[Reassembler] Frame complete:', frameId, frameData.length, 'bytes',
        //     isKeyframe ? '(keyframe)' : '(delta)');
        
        // Add to jitter buffer
        this.jitterBuffer.push({
            frameId: frameId,
            data: frameData,
            isKeyframe: isKeyframe,
            timestamp: Date.now()
        });
        
        // Sort by frame ID
        this.jitterBuffer.sort((a, b) => a.frameId - b.frameId);
        
        // Limit buffer size
        if (this.jitterBuffer.length > this.maxJitterBufferSize) {
            this.jitterBuffer.shift();
        }
        
        // Try to get next frame in sequence
        return this.getNextFrame();
    }
    
    /**
     * Get next frame in sequence from jitter buffer
     * Ensures frames are rendered in order
     */
    getNextFrame() {
        if (this.jitterBuffer.length === 0) {
            return null;
        }
        
        const nextFrame = this.jitterBuffer[0];
        
        // ✅ CRITICAL FIX: Always render first frame or keyframes immediately
        // Don't wait for sequential frames - this causes black screen if first frame is delayed
        if (this.lastRenderedFrameId === -1) {
            // First frame ever - render immediately
            this.jitterBuffer.shift();
            this.lastRenderedFrameId = nextFrame.frameId;
            console.log('[Reassembler] Rendering first frame:', nextFrame.frameId);
            return nextFrame;
        }
        
        // Check if this is the next frame in sequence
        if (nextFrame.frameId === this.lastRenderedFrameId + 1 || nextFrame.isKeyframe) {
            // Sequential frame or keyframe - render immediately
            this.jitterBuffer.shift();
            this.lastRenderedFrameId = nextFrame.frameId;
            return nextFrame;
        }
        
        // Wait for missing frames (but not too long)
        const age = Date.now() - nextFrame.timestamp;
        if (age > 200) { // ✅ FIX: Reduced timeout from 500ms to 200ms for faster recovery
            console.warn('[Reassembler] Skipping to frame', nextFrame.frameId, 
                'after waiting', age, 'ms (last rendered:', this.lastRenderedFrameId + ')');
            this.jitterBuffer.shift();
            this.lastRenderedFrameId = nextFrame.frameId;
            return nextFrame;
        }
        
        return null; // Wait for next frame
    }
    
    /**
     * Cleanup old incomplete frames (prevent memory leak)
     */
    cleanupOldFrames() {
        const now = Date.now();
        const maxAge = 5000; // 5 seconds
        
        for (let [frameId, frame] of this.frameBuffer.entries()) {
            if (now - frame.timestamp > maxAge) {
                console.warn('[Reassembler] Dropping old incomplete frame:', frameId);
                this.frameBuffer.delete(frameId);
                
                if (this.nackTimers.has(frameId)) {
                    clearTimeout(this.nackTimers.get(frameId));
                    this.nackTimers.delete(frameId);
                }
                
                this.stats.framesDropped++;
            }
        }
    }
    
    /**
     * Get statistics
     */
    getStats() {
        const lossRate = this.stats.packetsReceived > 0 
            ? (this.stats.packetsLost / this.stats.packetsReceived * 100).toFixed(2)
            : 0;
        
        return {
            ...this.stats,
            lossRate: lossRate + '%',
            bufferSize: this.frameBuffer.size,
            jitterBufferSize: this.jitterBuffer.length
        };
    }
    
    /**
     * Print statistics
     */
    printStats() {
        const stats = this.getStats();
        console.log('[Reassembler Stats]', stats);
    }
    
    /**
     * Reset statistics
     */
    reset() {
        this.frameBuffer.clear();
        this.jitterBuffer = [];
        this.lastRenderedFrameId = -1;
        
        for (let timerId of this.nackTimers.values()) {
            clearTimeout(timerId);
        }
        this.nackTimers.clear();
        
        this.stats = {
            packetsReceived: 0,
            framesCompleted: 0,
            framesDropped: 0,
            nacksSent: 0,
            packetsLost: 0
        };
    }
    
    /**
     * Cleanup corrupted frames (frames with invalid totalFragments)
     */
    cleanupCorruptedFrames() {
        const corruptedFrames = [];
        for (let [frameId, frame] of this.frameBuffer.entries()) {
            if (frame.totalFragments > 10000) {
                corruptedFrames.push(frameId);
            }
        }
        
        for (let frameId of corruptedFrames) {
            console.warn('[Reassembler] Cleaning up corrupted frame:', frameId);
            this.frameBuffer.delete(frameId);
            if (this.nackTimers.has(frameId)) {
                clearTimeout(this.nackTimers.get(frameId));
                this.nackTimers.delete(frameId);
            }
            this.stats.framesDropped++;
        }
        
        return corruptedFrames.length;
    }
}

// Export for use in other files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = FrameReassembler;
}


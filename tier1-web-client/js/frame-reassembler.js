/**
 * Client-side Frame Reassembler for Phase 2: Reliability Layer
 * Handles packet reassembly, NACK requests, and jitter buffer
 * 
 * From IMPLEMENTATION_ROADMAP.md Phase 2, Task 2.2
 */
class FrameReassembler {
    constructor(websocket) {
        this.ws = websocket;
        
        // Frame buffer: frameId -> { fragments: Map, fecPackets: Map, totalFragments: int, timestamp: long }
        this.frameBuffer = new Map();
        
        // ✅ NEW: FEC decoder for packet recovery
        this.fecDecoder = new FecXorDecoder(10, 2); // groupSize=10, fecCount=2 (matches server)
        
        // Jitter buffer: stores completed frames for ordered playback
        this.jitterBuffer = [];
        this.maxJitterBufferSize = 5;
        
        // Last rendered frame ID (for ordering)
        this.lastRenderedFrameId = -1;
        
        // ✅ NEW: Frame rate limiting to prevent rapid frame changes
        this.lastRenderTime = 0;
        this.minFrameInterval = 16; // ✅ OPTIMIZED: 16ms = ~60 FPS for smooth playback
        this.pendingRender = false; // Flag to prevent concurrent renders
        this.onFrameReady = null; // Callback for when frame is ready to render
        
        // Statistics
        this.stats = {
            packetsReceived: 0,
            framesCompleted: 0,
            framesDropped: 0,
            nacksSent: 0,
            packetsLost: 0,
            fecRecovered: 0 // ✅ NEW: Count of packets recovered via FEC
        };
        
        // NACK configuration
        this.nackTimeoutMs = 200; // ✅ FIX: Increased from 100ms to 200ms to reduce NACK spam and allow packets to arrive naturally
        this.maxNackAttempts = 5; // ✅ FIX: Increased from 3 to 5 attempts to handle temporary network issues
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
            // ✅ FIX: Only log first few errors to avoid console spam
            if (this.stats.packetsReceived <= 10 || (this.stats.packetsReceived % 100 === 0)) {
                console.error('[Reassembler] Invalid packet header, packet size:', packetData.byteLength);
            }
            return null;
        }
        
        // ✅ DEBUG: Log first few packets to verify parsing
        if (this.stats.packetsReceived <= 5) {
            console.log('[Reassembler] Packet #' + this.stats.packetsReceived + ':', 
                'frameId=' + header.frameId, 
                'fragment=' + header.fragmentIndex + '/' + header.totalFragments,
                'size=' + packetData.byteLength);
        }
        
        // ✅ NEW: Handle FEC packets separately
        if (header.isFecPacket) {
            this.storeFecPacket(header, packetData);
            // After storing FEC, try to recover missing fragments
            this.tryFecRecovery(header.frameId);
            return null;
        }
        
        // Fast path: single fragment frame
        if (header.totalFragments === 1) {
            const payload = new Uint8Array(packetData, 28);
            this.onFrameComplete(header.frameId, payload, header.isKeyframe);
            return null; // Don't return frame - it will be rendered via callback
        }
        
        // Multi-fragment frame: store fragment
        this.storeFragment(header, packetData);
        
        // ✅ NEW: After storing fragment, try FEC recovery (FEC packets might have arrived first)
        this.tryFecRecovery(header.frameId);
        
        // Check if frame is complete
        const frame = this.frameBuffer.get(header.frameId);
        if (frame && frame.fragments.size === header.totalFragments) {
            console.log('[Reassembler] Frame complete:', header.frameId, 
                '(' + frame.fragments.size + '/' + header.totalFragments + ' fragments)');
            this.assembleFrame(header.frameId);
            // Frame will be rendered via callback with rate limiting
            return null;
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
            // ✅ FIX: Only log first few errors to avoid console spam
            if (this.stats.packetsReceived <= 10 || (this.stats.packetsReceived % 100 === 0)) {
                console.error('[Reassembler] Invalid magic number:', magic.toString(16), 
                    'packet size:', packetData.byteLength,
                    'first 8 bytes:', Array.from(new Uint8Array(packetData.slice(0, 8)))
                        .map(b => '0x' + b.toString(16).padStart(2, '0')).join(' '));
            }
            return null;
        }
        
        // ✅ CRITICAL FIX: fragmentIndex and totalFragments are 2 bytes (short), not 4 bytes!
        // Java: putShort() → JavaScript: getUint16()
        // ByteBuffer in Java uses BIG-ENDIAN by default
        const fragmentIndex = view.getUint16(12, false); // Big-endian
        const totalFragments = view.getUint16(14, false); // Big-endian
        
        // ✅ NEW: Check if this is a FEC packet
        const flags = view.getUint8(27);
        const isFecPacket = (flags & 0x01) === 0x01;
        const frameType = view.getUint8(16);
        
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
            frameType: frameType,
            isKeyframe: !isFecPacket && (frameType === 1 || (frameType & 0x0F) === 1),
            isFecPacket: isFecPacket, // ✅ NEW: Flag to identify FEC packets
            fecIndex: isFecPacket ? fragmentIndex : null, // ✅ NEW: FEC index if FEC packet
            totalFecPackets: isFecPacket ? totalFragments : null // ✅ NEW: Total FEC packets if FEC packet
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
                fecPackets: new Map(), // ✅ NEW: Store FEC packets separately
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
     * ✅ NEW: Store FEC packet
     */
    storeFecPacket(header, packetData) {
        let frame = this.frameBuffer.get(header.frameId);
        
        if (!frame) {
            // Create frame entry for FEC packet (frame might not have started yet)
            // Note: We need to estimate totalFragments from FEC info
            // FEC packets have totalFecPackets in header, but we need data packet count
            // For now, we'll initialize with a placeholder that will be updated
            frame = {
                fragments: new Map(),
                fecPackets: new Map(),
                totalFragments: 0, // Will be updated when first data fragment arrives
                isKeyframe: false,
                timestamp: Date.now(),
                nackAttempts: 0,
                totalFecPackets: header.totalFecPackets // Store for reference
            };
            this.frameBuffer.set(header.frameId, frame);
        }
        
        // Store FEC payload (skip 28-byte header)
        const fecPayload = new Uint8Array(packetData, 28);
        frame.fecPackets.set(header.fecIndex, fecPayload);
    }
    
    /**
     * ✅ NEW: Try to recover missing fragments using FEC
     */
    tryFecRecovery(frameId) {
        const frame = this.frameBuffer.get(frameId);
        if (!frame || !frame.fecPackets || frame.fecPackets.size === 0) {
            return; // No FEC packets available
        }
        
        // Skip if totalFragments not yet known (FEC packets might arrive before data)
        if (frame.totalFragments === 0) {
            return;
        }
        
        // Find missing fragments
        const missing = [];
        for (let i = 0; i < frame.totalFragments; i++) {
            if (!frame.fragments.has(i)) {
                missing.push(i);
            }
        }
        
        if (missing.length === 0) {
            return; // No missing fragments
        }
        
        // Don't try to recover if too many packets are missing (FEC has limits)
        // FEC can only recover up to fecCount packets
        const maxFecCount = frame.fecPackets.size;
        if (missing.length > maxFecCount) {
            return; // Too many missing, FEC can't help
        }
        
        // Convert fragments and FEC packets to arrays for decoder
        const receivedPackets = [];
        for (let i = 0; i < frame.totalFragments; i++) {
            receivedPackets.push(frame.fragments.get(i) || null);
        }
        
        const fecPackets = [];
        const maxFecIndex = Math.max(...frame.fecPackets.keys());
        for (let i = 0; i <= maxFecIndex; i++) {
            fecPackets.push(frame.fecPackets.get(i) || null);
        }
        
        // Try to recover each missing packet
        let recoveredCount = 0;
        for (const lostIndex of missing) {
            const recovered = this.fecDecoder.recoverPacket(receivedPackets, fecPackets, lostIndex);
            if (recovered) {
                // Successfully recovered!
                frame.fragments.set(lostIndex, recovered);
                receivedPackets[lostIndex] = recovered; // Update for next recovery attempt
                recoveredCount++;
                this.stats.fecRecovered++;
                
                console.log('[Reassembler] ✅ FEC recovered fragment', lostIndex, 'for frame', frameId);
            }
        }
        
        if (recoveredCount > 0) {
            // Check if frame is now complete
            if (frame.fragments.size === frame.totalFragments) {
                console.log('[Reassembler] Frame complete after FEC recovery:', frameId);
                this.assembleFrame(frameId);
            }
        }
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
        
        // ✅ FIX: Use callback pattern instead of returning frame directly
        this.onFrameComplete(frameId, assembled, frame.isKeyframe);
        return null; // Frame will be rendered via callback with rate limiting
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
        
        // ✅ NEW: Try FEC recovery first before sending NACK
        this.tryFecRecovery(frameId);
        
        // Re-check frame after FEC recovery attempt
        if (frame.fragments.size === frame.totalFragments) {
            this.assembleFrame(frameId);
            return; // Frame complete, no need for NACK
        }
        
        // Find missing fragments (after FEC recovery attempt)
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
            // Only send NACK if FEC recovery failed
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
            // Remove oldest frame (not next in sequence)
            const oldest = this.jitterBuffer.shift();
            if (oldest.frameId <= this.lastRenderedFrameId) {
                // Already rendered, safe to remove
            } else {
                // This frame hasn't been rendered yet - keep it, remove newest instead
                // But we already removed it, so add back and remove from end
                this.jitterBuffer.unshift(oldest);
                this.jitterBuffer.pop();
            }
        }
        
        // ✅ NEW: Schedule render with rate limiting
        this.scheduleRender();
        
        return null; // Don't return frame directly - let scheduleRender handle it
    }
    
    /**
     * ✅ NEW: Schedule frame render with rate limiting
     * Uses requestAnimationFrame for smooth browser-synced rendering
     */
    scheduleRender() {
        if (this.pendingRender) {
            return; // Already scheduled
        }
        
        this.pendingRender = true;
        
        // ✅ OPTIMIZED: Use requestAnimationFrame for smooth, browser-synced rendering
        // This ensures frames are rendered at the optimal time for the browser's display refresh
        if (typeof requestAnimationFrame !== 'undefined') {
            requestAnimationFrame(() => {
                this.processRender();
            });
        } else {
            // Fallback to setTimeout if requestAnimationFrame not available
            const now = Date.now();
            const timeSinceLastRender = now - this.lastRenderTime;
            const delay = Math.max(0, this.minFrameInterval - timeSinceLastRender);
            setTimeout(() => {
                this.processRender();
            }, delay);
        }
    }
    
    /**
     * ✅ NEW: Process one frame render from jitter buffer
     */
    processRender() {
        this.pendingRender = false;
        
        const nextFrame = this.getNextFrame();
        if (nextFrame) {
            this.lastRenderTime = Date.now();
            // Trigger callback to display frame
            if (this.onFrameReady) {
                this.onFrameReady(nextFrame);
            }
            
            // ✅ NEW: Schedule next render if more frames are ready
            if (this.jitterBuffer.length > 0) {
                // Check if next frame in buffer is ready to render
                const nextInBuffer = this.jitterBuffer[0];
                if (nextInBuffer.frameId === this.lastRenderedFrameId + 1 || nextInBuffer.isKeyframe) {
                    // Schedule next render immediately (requestAnimationFrame will handle timing)
                    this.scheduleRender();
                }
            }
        }
    }
    
    /**
     * Get next frame in sequence from jitter buffer
     * Ensures frames are rendered in order - returns ONLY the next sequential frame
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
        
        // ✅ FIX: Only render the exact next frame in sequence
        // Don't render multiple sequential frames at once
        if (nextFrame.frameId === this.lastRenderedFrameId + 1) {
            // Exact next frame - render it
            this.jitterBuffer.shift();
            this.lastRenderedFrameId = nextFrame.frameId;
            return nextFrame;
        }
        
        // ✅ NEW: Keyframes can break sequence (for recovery)
        if (nextFrame.isKeyframe && nextFrame.frameId > this.lastRenderedFrameId) {
            // Keyframe after current position - render it (breaks sequence for recovery)
            this.jitterBuffer.shift();
            this.lastRenderedFrameId = nextFrame.frameId;
            console.log('[Reassembler] Rendering keyframe', nextFrame.frameId, '(skipped from', this.lastRenderedFrameId + ')');
            return nextFrame;
        }
        
        // Wait for missing frames (but not too long)
        const age = Date.now() - nextFrame.timestamp;
        if (age > 200) { // ✅ OPTIMIZED: Reduced to 200ms for lower latency - skip old frames faster
            // Skip this frame if it's too old
            this.jitterBuffer.shift();
            this.lastRenderedFrameId = nextFrame.frameId;
            // Try next frame immediately
            return this.getNextFrame();
        }
        
        return null; // Wait for next frame in sequence
    }
    
    /**
     * ✅ NEW: Set callback for when frame is ready to render
     */
    setOnFrameReady(callback) {
        this.onFrameReady = callback;
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
        
        const fecStats = this.fecDecoder.getStats();
        
        return {
            ...this.stats,
            lossRate: lossRate + '%',
            bufferSize: this.frameBuffer.size,
            jitterBufferSize: this.jitterBuffer.length,
            fecStats: fecStats // ✅ NEW: Include FEC statistics
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


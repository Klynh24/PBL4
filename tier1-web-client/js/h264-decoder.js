/**
 * ✅ NEW: H.264 Hardware Decoder using WebCodecs API
 * 
 * Decodes H.264 video streams from hardware encoder for smooth, low-latency playback
 * 
 * Browser Support:
 * - Chrome 94+ ✅
 * - Edge 94+ ✅
 * - Firefox 121+ ✅ (experimental)
 * 
 * Fallback: JPEG decoding if WebCodecs not available
 */
class H264Decoder {
    constructor(canvas, onFrameReady) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.onFrameReady = onFrameReady || (() => {});
        
        this.decoder = null;
        this.decoderConfig = null;
        this.isSupported = false;
        this.initialized = false;
        this.pendingConfig = null;
        this.frameCount = 0;
        
        // Check WebCodecs support
        this.isSupported = this.checkSupport();
        
        if (this.isSupported) {
            console.log('[H264Decoder] ✅ WebCodecs API supported - using hardware H.264 decoding');
            this.init();
        } else {
            console.warn('[H264Decoder] ⚠️ WebCodecs API not supported - falling back to JPEG');
        }
    }
    
    checkSupport() {
        return typeof window !== 'undefined' && 
               'VideoDecoder' in window && 
               'VideoFrame' in window;
    }
    
    async init() {
        if (!this.isSupported || this.initialized) {
            return;
        }
        
        try {
            // Configure decoder for H.264
            this.decoderConfig = {
                codec: 'avc1.42E01E', // H.264 Baseline Profile Level 3.0
                optimizeForLatency: true, // ✅ CRITICAL: Low latency mode
                hardwareAcceleration: 'prefer-hardware' // ✅ Use hardware decoder if available
            };
            
            // Create VideoDecoder
            this.decoder = new VideoDecoder({
                output: (frame) => {
                    this.handleFrame(frame);
                },
                error: (error) => {
                    console.error('[H264Decoder] Decode error:', error);
                    this.onFrameReady(null, error);
                }
            });
            
            this.initialized = true;
            console.log('[H264Decoder] ✅ Decoder initialized (hardware-accelerated, low-latency)');
            
            // Apply pending config if any
            if (this.pendingConfig) {
                this.configure(this.pendingConfig);
            }
            
        } catch (error) {
            console.error('[H264Decoder] Failed to initialize:', error);
            this.isSupported = false;
            this.initialized = false;
        }
    }
    
    /**
     * Configure decoder with SPS/PPS (from H.264 stream)
     * Should be called when keyframe is received
     */
    configure(config) {
        if (!this.isSupported) {
            return false;
        }
        
        if (!this.initialized) {
            this.pendingConfig = config;
            return false;
        }
        
        try {
            // Extract SPS and PPS from config
            const decoderConfig = {
                ...this.decoderConfig,
                description: config.description, // SPS/PPS data
                codedWidth: config.width || this.canvas.width,
                codedHeight: config.height || this.canvas.height
            };
            
            this.decoder.configure(decoderConfig);
            console.log('[H264Decoder] ✅ Decoder configured:', decoderConfig);
            return true;
        } catch (error) {
            console.error('[H264Decoder] Configuration error:', error);
            return false;
        }
    }
    
    /**
     * Decode H.264 NAL unit (frame data)
     * 
     * @param {ArrayBuffer|Uint8Array} data - H.264 NAL unit data
     * @param {boolean} isKeyframe - Whether this is a keyframe
     * @param {number} timestamp - Presentation timestamp (microseconds)
     */
    decode(data, isKeyframe = false, timestamp = null) {
        if (!this.isSupported || !this.decoder || this.decoder.state !== 'configured') {
            return false;
        }
        
        try {
            // Convert to EncodedVideoChunk
            const chunk = new EncodedVideoChunk({
                type: isKeyframe ? 'key' : 'delta',
                timestamp: timestamp || (this.frameCount * 33333), // ~30 FPS default
                duration: null, // Unknown duration
                data: data
            });
            
            this.decoder.decode(chunk);
            this.frameCount++;
            return true;
        } catch (error) {
            console.error('[H264Decoder] Decode error:', error);
            return false;
        }
    }
    
    /**
     * Handle decoded video frame
     */
    handleFrame(frame) {
        try {
            // Draw frame to canvas
            this.ctx.drawImage(frame, 0, 0, this.canvas.width, this.canvas.height);
            
            // Notify callback
            this.onFrameReady(frame, null);
            
            // Close frame to free memory (important!)
            frame.close();
            
        } catch (error) {
            console.error('[H264Decoder] Frame rendering error:', error);
            this.onFrameReady(null, error);
        }
    }
    
    /**
     * Extract SPS/PPS from H.264 NAL units
     * NAL unit types: 7 = SPS, 8 = PPS
     */
    static extractConfig(data) {
        const view = new DataView(data.buffer || data, data.byteOffset || 0);
        const config = {
            description: null,
            width: 0,
            height: 0
        };
        
        let sps = null;
        let pps = null;
        
        // Parse NAL units (simplified - assumes Annex-B format)
        let offset = 0;
        while (offset < data.length - 4) {
            // Look for start code: 0x00 0x00 0x00 0x01 or 0x00 0x00 0x01
            if (view.getUint8(offset) === 0x00 && 
                view.getUint8(offset + 1) === 0x00) {
                
                let nalStart = offset;
                if (view.getUint8(offset + 2) === 0x00 && 
                    view.getUint8(offset + 3) === 0x01) {
                    offset += 4;
                } else if (view.getUint8(offset + 2) === 0x01) {
                    offset += 3;
                } else {
                    offset++;
                    continue;
                }
                
                if (offset >= data.length) break;
                
                // Get NAL unit type (low 5 bits of first byte)
                const nalType = view.getUint8(offset) & 0x1F;
                
                // Find next start code
                let nalEnd = offset + 1;
                while (nalEnd < data.length - 3) {
                    if (view.getUint8(nalEnd) === 0x00 &&
                        view.getUint8(nalEnd + 1) === 0x00 &&
                        (view.getUint8(nalEnd + 2) === 0x01 ||
                         (view.getUint8(nalEnd + 2) === 0x00 && 
                          view.getUint8(nalEnd + 3) === 0x01))) {
                        break;
                    }
                    nalEnd++;
                }
                
                const nalData = data.slice(offset, nalEnd);
                
                if (nalType === 7) { // SPS
                    sps = nalData;
                    // TODO: Parse SPS to extract width/height
                } else if (nalType === 8) { // PPS
                    pps = nalData;
                }
                
                offset = nalEnd;
            } else {
                offset++;
            }
        }
        
        if (sps && pps) {
            // Create description from SPS and PPS (AVC format)
            const descLength = 4 + sps.length + 1 + 2 + pps.length;
            const description = new Uint8Array(descLength);
            const descView = new DataView(description.buffer);
            
            let pos = 0;
            descView.setUint8(pos++, 1); // configurationVersion
            descView.setUint8(pos++, sps[1]); // AVCProfileIndication
            descView.setUint8(pos++, sps[2]); // profile_compatibility
            descView.setUint8(pos++, sps[3]); // AVCLevelIndication
            descView.setUint8(pos++, 0xFF); // lengthSizeMinusOne (4 bytes)
            descView.setUint8(pos++, 0xE1); // numOfSequenceParameterSets (1)
            descView.setUint16(pos, sps.length, false); // SPS length
            pos += 2;
            description.set(sps, pos); // SPS data
            pos += sps.length;
            descView.setUint8(pos++, 1); // numOfPictureParameterSets (1)
            descView.setUint16(pos, pps.length, false); // PPS length
            pos += 2;
            description.set(pps, pos); // PPS data
            
            config.description = description;
        }
        
        return config;
    }
    
    /**
     * Check if data is H.264 format
     */
    static isH264(data) {
        if (!data || data.length < 4) return false;
        
        const view = new DataView(data.buffer || data, data.byteOffset || 0);
        
        // Check for H.264 start codes or AVC format
        // Start code: 0x00 0x00 0x00 0x01 or 0x00 0x00 0x01
        if ((view.getUint8(0) === 0x00 && view.getUint8(1) === 0x00 && 
             (view.getUint8(2) === 0x00 || view.getUint8(2) === 0x01) &&
             view.getUint8(2) === 0x01)) {
            return true;
        }
        
        // Check for AVC format (length-prefixed)
        // First 4 bytes are length, followed by NAL unit
        if (data.length >= 5) {
            const nalStart = view.getUint32(0, false); // Big-endian length
            if (nalStart > 0 && nalStart < data.length - 4) {
                const nalType = view.getUint8(4) & 0x1F;
                // Valid NAL unit types
                if (nalType >= 1 && nalType <= 23) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Cleanup and close decoder
     */
    close() {
        if (this.decoder) {
            this.decoder.close();
            this.decoder = null;
        }
        this.initialized = false;
    }
}


package com.tutoring.core.streaming;

/**
 * Quality levels for adaptive bitrate streaming
 * Each level defines resolution, frame rate, and JPEG compression quality
 */
public enum QualityLevel {
    HIGH(1920, 1080, 15, 0.85f),      // Full HD, highest quality
    MEDIUM(1280, 720, 10, 0.75f),     // HD, balanced
    LOW(854, 480, 5, 0.65f),          // SD, low bandwidth
    MINIMAL(640, 360, 3, 0.50f);      // Minimal, emergency fallback
    
    public final int width;
    public final int height;
    public final int fps;
    public final float jpegQuality;
    
    QualityLevel(int width, int height, int fps, float jpegQuality) {
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.jpegQuality = jpegQuality;
    }
    
    /**
     * Convert to protocol string for transmission
     * Format: LEVEL:WIDTH:HEIGHT:FPS:QUALITY
     */
    public String toProtocol() {
        return name() + ":" + width + ":" + height + ":" + fps + ":" + jpegQuality;
    }
    
    /**
     * Get estimated bandwidth usage (KB/s)
     */
    public int getEstimatedBandwidth() {
        switch (this) {
            case HIGH:    return 500;  // 400-600 KB/s
            case MEDIUM:  return 250;  // 200-300 KB/s
            case LOW:     return 125;  // 100-150 KB/s
            case MINIMAL: return 65;   // 50-80 KB/s
            default:      return 500;
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s (%dx%d @ %d FPS, Q:%.2f)", 
            name(), width, height, fps, jpegQuality);
    }
}


package com.tutoring.core.streaming.quality;

public enum QualityLevel {
    HIGH(1280, 720, 12, 0.6f), // 720p @ 12 FPS, Q:0.6
    MEDIUM(1024, 576, 10, 0.5f), // 576p @ 10 FPS, Q:0.5
    LOW(854, 480, 8, 0.4f), // 480p @ 8 FPS, Q:0.4
    MINIMAL(640, 360, 5, 0.35f);

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

    public String toProtocol() {
        return name() + ":" + width + ":" + height + ":" + fps + ":" + jpegQuality;
    }

    public int getEstimatedBandwidth() {
        switch (this) {
            case HIGH:
                return 500; // 400-600 KB/s
            case MEDIUM:
                return 250; // 200-300 KB/s
            case LOW:
                return 125; // 100-150 KB/s
            case MINIMAL:
                return 65; // 50-80 KB/s
            default:
                return 500;
        }
    }

    @Override
    public String toString() {
        return String.format("%s (%dx%d @ %d FPS, Q:%.2f)",
                name(), width, height, fps, jpegQuality);
    }
}

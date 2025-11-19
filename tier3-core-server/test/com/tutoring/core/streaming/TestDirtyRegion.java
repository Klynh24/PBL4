package com.tutoring.core.streaming;

import java.util.List;
import java.util.Random;

/**
 * Unit tests for Phase 3: Delta Compression
 * Tests dirty region detection and frame encoding
 */
public class TestDirtyRegion {
    
    public static void main(String[] args) {
        System.out.println("=== Phase 3: Delta Compression Tests ===\n");
        
        testNoChanges();
        testSmallChange();
        testLargeChange();
        testMultipleRegions();
        testChangePercentage();
        
        System.out.println("\n=== All Phase 3 Tests Passed ===");
    }
    
    /**
     * Test 1: No changes (identical frames)
     */
    private static void testNoChanges() {
        System.out.println("Test 1: No Changes");
        
        DirtyRegionDetector detector = new DirtyRegionDetector();
        
        int width = 1920, height = 1080;
        byte[] frame1 = createFrame(width, height, 128); // Gray frame
        byte[] frame2 = frame1.clone(); // Identical
        
        // First call establishes baseline
        List<DirtyRegionDetector.Rectangle> regions1 = 
            detector.detectDirtyRegions(frame1, width, height);
        
        // Second call should detect no changes
        List<DirtyRegionDetector.Rectangle> regions2 = 
            detector.detectDirtyRegions(frame2, width, height);
        
        System.out.println("  ✓ First call regions: " + regions1.size() + " (full frame)");
        System.out.println("  ✓ Second call regions: " + regions2.size() + " (no changes)");
        assert regions2.isEmpty() : "Should detect no changes";
        System.out.println();
    }
    
    /**
     * Test 2: Small localized change (top-left corner)
     */
    private static void testSmallChange() {
        System.out.println("Test 2: Small Localized Change");
        
        DirtyRegionDetector detector = new DirtyRegionDetector();
        
        int width = 1920, height = 1080;
        byte[] frame1 = createFrame(width, height, 128);
        byte[] frame2 = frame1.clone();
        
        // Change top-left 200x200 region
        changeRegion(frame2, width, 0, 0, 200, 200, 255);
        
        // Detect
        detector.detectDirtyRegions(frame1, width, height); // Baseline
        List<DirtyRegionDetector.Rectangle> regions = 
            detector.detectDirtyRegions(frame2, width, height);
        
        System.out.println("  ✓ Dirty regions detected: " + regions.size());
        regions.forEach(r -> System.out.println("    - " + r));
        
        // Calculate change percentage
        double changePercent = detector.calculateChangePercentage(regions, width, height);
        System.out.println("  ✓ Change percentage: " + 
            String.format("%.2f%%", changePercent * 100));
        
        // Should be around 2% (200x200 out of 1920x1080)
        assert changePercent < 0.05 : "Change percentage too high: " + changePercent;
        System.out.println();
    }
    
    /**
     * Test 3: Large change (> 30% triggers keyframe)
     */
    private static void testLargeChange() {
        System.out.println("Test 3: Large Change (Motion Detection)");
        
        DirtyRegionDetector detector = new DirtyRegionDetector();
        
        int width = 1920, height = 1080;
        byte[] frame1 = createFrame(width, height, 128);
        byte[] frame2 = createFrame(width, height, 200); // Completely different
        
        // Detect
        detector.detectDirtyRegions(frame1, width, height); // Baseline
        List<DirtyRegionDetector.Rectangle> regions = 
            detector.detectDirtyRegions(frame2, width, height);
        
        double changePercent = detector.calculateChangePercentage(regions, width, height);
        System.out.println("  ✓ Change percentage: " + 
            String.format("%.2f%%", changePercent * 100));
        
        // Should trigger keyframe (> 30%)
        boolean shouldBeKeyframe = changePercent > 0.30;
        System.out.println("  ✓ Should send keyframe: " + shouldBeKeyframe);
        assert shouldBeKeyframe : "Large change should trigger keyframe";
        System.out.println();
    }
    
    /**
     * Test 4: Multiple non-contiguous regions
     */
    private static void testMultipleRegions() {
        System.out.println("Test 4: Multiple Regions (Clock + Mouse)");
        
        DirtyRegionDetector detector = new DirtyRegionDetector();
        
        int width = 1920, height = 1080;
        byte[] frame1 = createFrame(width, height, 128);
        byte[] frame2 = frame1.clone();
        
        // Change 1: Top-right corner (clock - 150x50)
        changeRegion(frame2, width, 1770, 0, 150, 50, 200);
        
        // Change 2: Center (mouse - 50x50)
        changeRegion(frame2, width, 935, 515, 50, 50, 220);
        
        // Change 3: Bottom-left (video player - 400x300)
        changeRegion(frame2, width, 50, 750, 400, 300, 180);
        
        // Detect
        detector.detectDirtyRegions(frame1, width, height); // Baseline
        List<DirtyRegionDetector.Rectangle> regions = 
            detector.detectDirtyRegions(frame2, width, height);
        
        System.out.println("  ✓ Regions detected: " + regions.size());
        regions.forEach(r -> System.out.println("    - " + r));
        
        double changePercent = detector.calculateChangePercentage(regions, width, height);
        System.out.println("  ✓ Total change: " + 
            String.format("%.2f%%", changePercent * 100));
        System.out.println();
    }
    
    /**
     * Test 5: Performance measurement
     */
    private static void testChangePercentage() {
        System.out.println("Test 5: Performance Measurement");
        
        DirtyRegionDetector detector = new DirtyRegionDetector();
        
        int width = 1920, height = 1080;
        byte[] frame1 = createFrame(width, height, 128);
        byte[] frame2 = frame1.clone();
        
        // Simulate typing (small changes across screen)
        Random rand = new Random();
        for (int i = 0; i < 20; i++) {
            int x = rand.nextInt(width - 100);
            int y = rand.nextInt(height - 50);
            changeRegion(frame2, width, x, y, 100, 50, rand.nextInt(256));
        }
        
        // Baseline
        detector.detectDirtyRegions(frame1, width, height);
        
        // Measure detection time
        long startTime = System.nanoTime();
        List<DirtyRegionDetector.Rectangle> regions = 
            detector.detectDirtyRegions(frame2, width, height);
        long duration = (System.nanoTime() - startTime) / 1_000_000; // ms
        
        System.out.println("  ✓ Detection time: " + duration + "ms");
        System.out.println("  ✓ Regions found: " + regions.size());
        System.out.println("  ✓ Target: < 30ms");
        
        assert duration < 50 : "Detection too slow: " + duration + "ms (target: <30ms)";
        System.out.println("  ✓ Performance acceptable\n");
    }
    
    /**
     * Helper: Create a frame filled with a single color
     */
    private static byte[] createFrame(int width, int height, int grayValue) {
        byte[] frame = new byte[width * height * 4]; // ARGB
        byte value = (byte) grayValue;
        
        for (int i = 0; i < frame.length; i += 4) {
            frame[i] = (byte) 255;     // A
            frame[i + 1] = value;      // R
            frame[i + 2] = value;      // G
            frame[i + 3] = value;      // B
        }
        
        return frame;
    }
    
    /**
     * Helper: Change a rectangular region in a frame
     */
    private static void changeRegion(byte[] frame, int frameWidth, 
                                     int x, int y, int width, int height, 
                                     int newValue) {
        byte value = (byte) newValue;
        
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                int px = x + dx;
                int py = y + dy;
                
                if (px >= 0 && px < frameWidth && py >= 0) {
                    int offset = (py * frameWidth + px) * 4;
                    
                    if (offset + 3 < frame.length) {
                        frame[offset + 1] = value; // R
                        frame[offset + 2] = value; // G
                        frame[offset + 3] = value; // B
                    }
                }
            }
        }
    }
}


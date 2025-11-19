package com.tutoring.core.streaming;

import java.util.List;
import java.util.Random;

/**
 * Unit tests for Phase 1: Foundation
 * Tests packet fragmentation and reassembly
 */
public class TestFragmentation {
    
    public static void main(String[] args) {
        System.out.println("=== Phase 1: Fragmentation Tests ===\n");
        
        testSingleFragment();
        testMultipleFragments();
        testLargeFrame();
        testPacketExtraction();
        
        System.out.println("\n=== All Phase 1 Tests Passed ===");
    }
    
    /**
     * Test 1: Single fragment (small frame)
     */
    private static void testSingleFragment() {
        System.out.println("Test 1: Single Fragment");
        
        FrameFragmenter fragmenter = new FrameFragmenter();
        
        // Create small frame (500 bytes)
        byte[] testFrame = new byte[500];
        new Random().nextBytes(testFrame);
        
        // Fragment it
        List<byte[]> fragments = fragmenter.fragmentFrame(1, (byte)0x01, testFrame);
        
        // Verify
        assert fragments.size() == 1 : "Expected 1 fragment, got " + fragments.size();
        assert fragments.get(0).length <= 1400 : "Packet too large: " + fragments.get(0).length;
        
        System.out.println("  ✓ Small frame creates 1 fragment");
        System.out.println("  ✓ Packet size: " + fragments.get(0).length + " bytes (≤ 1400)\n");
    }
    
    /**
     * Test 2: Multiple fragments (10KB frame)
     */
    private static void testMultipleFragments() {
        System.out.println("Test 2: Multiple Fragments");
        
        FrameFragmenter fragmenter = new FrameFragmenter();
        
        // Create 10KB frame
        byte[] testFrame = new byte[10240];
        new Random().nextBytes(testFrame);
        
        // Fragment it
        List<byte[]> fragments = fragmenter.fragmentFrame(2, (byte)0x01, testFrame);
        
        // Verify
        int expectedFragments = (int) Math.ceil(10240.0 / 1372.0); // 1372 = payload size
        assert fragments.size() == expectedFragments : 
            "Expected ~" + expectedFragments + " fragments, got " + fragments.size();
        
        // Check all packets are within MTU
        for (int i = 0; i < fragments.size(); i++) {
            assert fragments.get(i).length <= 1400 : 
                "Fragment " + i + " too large: " + fragments.get(i).length;
        }
        
        System.out.println("  ✓ 10KB frame creates " + fragments.size() + " fragments");
        System.out.println("  ✓ All packets ≤ 1400 bytes");
        
        // Calculate total size
        int totalSize = fragments.stream().mapToInt(p -> p.length).sum();
        System.out.println("  ✓ Total size: " + totalSize + " bytes\n");
    }
    
    /**
     * Test 3: Large frame (100KB)
     */
    private static void testLargeFrame() {
        System.out.println("Test 3: Large Frame (100KB)");
        
        FrameFragmenter fragmenter = new FrameFragmenter();
        
        // Create 100KB frame
        byte[] testFrame = new byte[102400];
        new Random().nextBytes(testFrame);
        
        long startTime = System.currentTimeMillis();
        List<byte[]> fragments = fragmenter.fragmentFrame(3, (byte)0x02, testFrame);
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("  ✓ 100KB frame creates " + fragments.size() + " fragments");
        System.out.println("  ✓ Fragmentation time: " + duration + "ms");
        
        // Verify header consistency
        int firstSeq = FrameFragmenter.extractSequenceNumber(fragments.get(0));
        int lastSeq = FrameFragmenter.extractSequenceNumber(fragments.get(fragments.size() - 1));
        
        System.out.println("  ✓ Sequence numbers: " + firstSeq + " to " + lastSeq);
        assert lastSeq == firstSeq + fragments.size() - 1 : "Sequence numbers not sequential";
        System.out.println("  ✓ Sequence numbers are sequential\n");
    }
    
    /**
     * Test 4: Packet header extraction
     */
    private static void testPacketExtraction() {
        System.out.println("Test 4: Packet Header Extraction");
        
        FrameFragmenter fragmenter = new FrameFragmenter();
        
        byte[] testFrame = new byte[5000];
        List<byte[]> fragments = fragmenter.fragmentFrame(42, (byte)0x01, testFrame);
        
        // Test extraction methods
        for (int i = 0; i < fragments.size(); i++) {
            byte[] packet = fragments.get(i);
            
            int seqNum = FrameFragmenter.extractSequenceNumber(packet);
            int frameId = FrameFragmenter.extractFrameId(packet);
            boolean isKeyframe = FrameFragmenter.isKeyframe(packet);
            
            assert frameId == 42 : "Frame ID mismatch: expected 42, got " + frameId;
            assert isKeyframe : "Should be keyframe";
            
            if (i == 0) {
                System.out.println("  ✓ Sequence number: " + seqNum);
                System.out.println("  ✓ Frame ID: " + frameId);
                System.out.println("  ✓ Is keyframe: " + isKeyframe);
            }
        }
        
        System.out.println("  ✓ All header extractions successful\n");
    }
}


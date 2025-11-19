package com.tutoring.core.streaming;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ✅ RESTORED: Frame Encoder
 * 
 * Encodes frames into compressed format:
 * - Keyframes: Full JPEG compression
 * - Delta frames: Only changed regions compressed
 * 
 * BANDWIDTH SAVINGS:
 * - Keyframe: 100-200 KB (full frame)
 * - Delta frame: 10-50 KB (only changed regions)
 * - Typical: 80-95% bandwidth reduction
 */
public class FrameEncoder {
    
    /**
     * Encode full frame as keyframe (JPEG)
     */
    public byte[] encodeKeyframe(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Get JPEG writer
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer found");
        }
        
        ImageWriter writer = writers.next();
        
        // Set compression quality
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        
        // Write image
        writer.setOutput(ImageIO.createImageOutputStream(baos));
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        
        return baos.toByteArray();
    }
    
    /**
     * Encode delta frame (only changed regions)
     * 
     * Format:
     * [Number of Regions (2 bytes)]
     * For each region:
     *   [X (2 bytes)][Y (2 bytes)][Width (2 bytes)][Height (2 bytes)]
     *   [Compressed Size (4 bytes)][JPEG Data (variable)]
     */
    public byte[] encodeDeltaFrame(BufferedImage image, 
                                  List<DirtyRegionDetector.Rectangle> dirtyRegions, 
                                  float quality) throws IOException {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // Write number of regions
        dos.writeShort(dirtyRegions.size());
        
        // Write each region
        for (DirtyRegionDetector.Rectangle rect : dirtyRegions) {
            dos.writeShort(rect.x);
            dos.writeShort(rect.y);
            dos.writeShort(rect.width);
            dos.writeShort(rect.height);
            
            // Extract region from full image
            BufferedImage regionImage = image.getSubimage(
                rect.x, rect.y, rect.width, rect.height
            );
            
            // Compress region with JPEG
            byte[] compressedRegion = compressToJPEG(regionImage, quality);
            
            dos.writeInt(compressedRegion.length);
            dos.write(compressedRegion);
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Compress BufferedImage to JPEG with specified quality
     */
    private byte[] compressToJPEG(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer found");
        }
        
        ImageWriter writer = writers.next();
        
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        
        writer.setOutput(ImageIO.createImageOutputStream(baos));
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        
        return baos.toByteArray();
    }
    
    /**
     * Decode delta frame (for testing/verification)
     */
    public static class DecodedRegion {
        public final int x;
        public final int y;
        public final BufferedImage image;
        
        public DecodedRegion(int x, int y, BufferedImage image) {
            this.x = x;
            this.y = y;
            this.image = image;
        }
    }
    
    public List<DecodedRegion> decodeDeltaFrame(byte[] deltaData) throws IOException {
        List<DecodedRegion> regions = new ArrayList<>();
        
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(deltaData));
        
        // Read number of regions
        int numRegions = dis.readShort();
        
        // Read each region
        for (int i = 0; i < numRegions; i++) {
            int x = dis.readShort();
            int y = dis.readShort();
            int width = dis.readShort();
            int height = dis.readShort();
            int compressedSize = dis.readInt();
            
            // Read JPEG data
            byte[] jpegData = new byte[compressedSize];
            dis.readFully(jpegData);
            
            // Decompress JPEG
            BufferedImage regionImage = ImageIO.read(new ByteArrayInputStream(jpegData));
            
            regions.add(new DecodedRegion(x, y, regionImage));
        }
        
        return regions;
    }
}


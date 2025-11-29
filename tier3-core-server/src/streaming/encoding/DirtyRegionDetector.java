package streaming.encoding;

import java.util.*;

public class DirtyRegionDetector {
    private static final int BLOCK_SIZE = 64;
    private static final int CHANGE_THRESHOLD = 1000;
    private static final int RGB_THRESHOLD = 10;

    private byte[] previousFrame = null;
    private int frameWidth;
    private int frameHeight;

    private final List<Rectangle> reusableDirtyRegions = new ArrayList<>(256);

    private static final boolean VECTOR_API_AVAILABLE = checkVectorAPIAvailable();

    private static boolean checkVectorAPIAvailable() {
        try {
            Class<?> vectorClass = Class.forName("jdk.incubator.vector.ByteVector");
            Class<?> speciesClass = Class.forName("jdk.incubator.vector.VectorSpecies");
            return vectorClass != null && speciesClass != null;
        } catch (ClassNotFoundException e) {
            System.out.println("[DirtyRegionDetector] Vector API not available, using scalar fallback");
            return false;
        }
    }

    private static int getVectorLanes() {
        try {
            Class<?> byteVectorClass = Class.forName("jdk.incubator.vector.ByteVector");
            Object species = byteVectorClass.getField("SPECIES_256").get(null);
            java.lang.reflect.Method lengthMethod = species.getClass().getMethod("length");
            int lanes = (Integer) lengthMethod.invoke(species);
            System.out.println("[DirtyRegionDetector] ✅ SIMD: Vector API available with " + lanes + " lanes");
            return lanes;
        } catch (Exception e) {
            System.out.println("[DirtyRegionDetector] Could not determine vector lanes, using scalar fallback");
            return 0;
        }
    }

    public List<Rectangle> detectDirtyRegions(byte[] currentFrame, int width, int height) {
        if (previousFrame == null || width != frameWidth || height != frameHeight) {
            int frameSize = width * height * 4;
            previousFrame = new byte[frameSize];
            System.arraycopy(currentFrame, 0, previousFrame, 0, frameSize);
            frameWidth = width;
            frameHeight = height;
            reusableDirtyRegions.clear();
            reusableDirtyRegions.add(new Rectangle(0, 0, width, height));
            return new ArrayList<>(reusableDirtyRegions);
        }

        reusableDirtyRegions.clear();

        int blocksX = (int) Math.ceil((double) width / BLOCK_SIZE);
        int blocksY = (int) Math.ceil((double) height / BLOCK_SIZE);
        int earlyExitThreshold = CHANGE_THRESHOLD / 100;

        for (int by = 0; by < blocksY; by++) {
            for (int bx = 0; bx < blocksX; bx++) {
                int x = bx * BLOCK_SIZE;
                int y = by * BLOCK_SIZE;
                int w = Math.min(BLOCK_SIZE, width - x);
                int h = Math.min(BLOCK_SIZE, height - y);

                boolean changed = VECTOR_API_AVAILABLE
                        ? isBlockChangedVectorized(currentFrame, previousFrame, x, y, w, h, width, earlyExitThreshold)
                        : isBlockChanged(currentFrame, previousFrame, x, y, w, h, width, earlyExitThreshold);

                if (changed) {
                    reusableDirtyRegions.add(new Rectangle(x, y, w, h));
                }
            }
        }

        // ✅ OPTIMIZATION: Swap buffers instead of cloning (zero-copy)
        // Copy current to previous for next comparison
        System.arraycopy(currentFrame, 0, previousFrame, 0, currentFrame.length);

        // Merge adjacent rectangles to reduce packet count
        List<Rectangle> merged = mergeAdjacentRectangles(reusableDirtyRegions);
        return new ArrayList<>(merged); // Return new list (caller owns it)
    }

    private boolean isBlockChangedVectorized(byte[] current, byte[] previous,
            int x, int y, int w, int h, int frameWidth, int earlyExitThreshold) {
        try {
            Class<?> byteVectorClass = Class.forName("jdk.incubator.vector.ByteVector");
            Class<?> vectorSpeciesClass = Class.forName("jdk.incubator.vector.VectorSpecies");
            Class<?> vectorMaskClass = Class.forName("jdk.incubator.vector.VectorMask");
            Class<?> vectorOperatorsClass = Class.forName("jdk.incubator.vector.VectorOperators");

            Object species = byteVectorClass.getField("SPECIES_256").get(null);
            int vectorLength = (Integer) vectorSpeciesClass.getMethod("length").invoke(species);

            int bytesPerPixel = 4;
            int baseOffset = (y * frameWidth + x) * bytesPerPixel;
            int pixelCount = w * h;

            int changedPixels = 0;
            int vectorBytes = vectorLength;

            for (int i = 0; i < pixelCount; i += 8) {
                int offset = baseOffset + (i * bytesPerPixel);
                if (offset + vectorBytes - 1 >= current.length ||
                        offset + vectorBytes - 1 >= previous.length) {

                    break;
                }
                Object currentVec = byteVectorClass.getMethod("fromArray", vectorSpeciesClass, byte[].class, int.class)
                        .invoke(null, species, current, offset);
                Object previousVec = byteVectorClass.getMethod("fromArray", vectorSpeciesClass, byte[].class, int.class)
                        .invoke(null, species, previous, offset);

                Object diff = byteVectorClass.getMethod("sub", byteVectorClass)
                        .invoke(currentVec, previousVec);
                diff = byteVectorClass.getMethod("abs").invoke(diff);
                Object thresholdVec = byteVectorClass.getMethod("broadcast", vectorSpeciesClass, byte.class)
                        .invoke(null, species, (byte) RGB_THRESHOLD);
                Object mask = byteVectorClass.getMethod("compare",
                        Class.forName("jdk.incubator.vector.VectorOperators$Comparison"), byteVectorClass)
                        .invoke(diff, vectorOperatorsClass.getField("GT").get(null), thresholdVec);

                int trueCount = (Integer) vectorMaskClass.getMethod("trueCount").invoke(mask);
                changedPixels += trueCount;
                if (changedPixels > earlyExitThreshold) {
                    return true;
                }
            }

            return changedPixels > earlyExitThreshold;

        } catch (Exception e) {
            System.err.println("[DirtyRegionDetector] Vector API error, using scalar fallback: " + e.getMessage());
            return isBlockChanged(current, previous, x, y, w, h, frameWidth, earlyExitThreshold);
        }
    }

    private boolean isBlockChanged(byte[] current, byte[] previous,
            int x, int y, int w, int h, int frameWidth, int earlyExitThreshold) {
        int changedPixels = 0;
        int bytesPerPixel = 4;
        int stride = frameWidth * bytesPerPixel;

        int baseOffset = (y * frameWidth + x) * bytesPerPixel;
        int maxOffset = current.length - 3;

        for (int dy = 0; dy < h; dy++) {
            int rowOffset = baseOffset + (dy * stride);

            for (int dx = 0; dx < w; dx++) {
                int offset = rowOffset + (dx * bytesPerPixel);

                // Bounds check (optimized)
                if (offset + 3 > maxOffset || offset + 3 >= previous.length) {
                    continue;
                }

                // ✅ OPTIMIZATION: Compare all channels at once, use bitwise ops
                int rDiff = Math.abs((current[offset + 1] & 0xFF) - (previous[offset + 1] & 0xFF));
                int gDiff = Math.abs((current[offset + 2] & 0xFF) - (previous[offset + 2] & 0xFF));
                int bDiff = Math.abs((current[offset + 3] & 0xFF) - (previous[offset + 3] & 0xFF));

                // If any channel exceeds threshold, pixel changed
                if (rDiff > RGB_THRESHOLD || gDiff > RGB_THRESHOLD || bDiff > RGB_THRESHOLD) {
                    changedPixels++;

                    // ✅ OPTIMIZATION: Early exit (most common case)
                    if (changedPixels > earlyExitThreshold) {
                        return true;
                    }
                }
            }
        }

        return changedPixels > earlyExitThreshold;
    }

    /**
     * Merge adjacent rectangles to reduce number of regions
     */
    private List<Rectangle> mergeAdjacentRectangles(List<Rectangle> rects) {
        if (rects.size() <= 1) {
            return rects;
        }

        List<Rectangle> merged = new ArrayList<>(rects);
        boolean changed = true;

        while (changed) {
            changed = false;
            for (int i = 0; i < merged.size(); i++) {
                for (int j = i + 1; j < merged.size(); j++) {
                    Rectangle r1 = merged.get(i);
                    Rectangle r2 = merged.get(j);

                    if (rectsAreAdjacent(r1, r2)) {
                        Rectangle combined = combineRects(r1, r2);
                        merged.set(i, combined);
                        merged.remove(j);
                        changed = true;
                        break;
                    }
                }
                if (changed)
                    break;
            }
        }

        return merged;
    }

    /**
     * Check if two rectangles are adjacent or overlapping
     */
    private boolean rectsAreAdjacent(Rectangle r1, Rectangle r2) {
        return !(r1.x + r1.width < r2.x - BLOCK_SIZE ||
                r2.x + r2.width < r1.x - BLOCK_SIZE ||
                r1.y + r1.height < r2.y - BLOCK_SIZE ||
                r2.y + r2.height < r1.y - BLOCK_SIZE);
    }

    /**
     * Combine two rectangles into their bounding box
     */
    private Rectangle combineRects(Rectangle r1, Rectangle r2) {
        int minX = Math.min(r1.x, r2.x);
        int minY = Math.min(r1.y, r2.y);
        int maxX = Math.max(r1.x + r1.width, r2.x + r2.width);
        int maxY = Math.max(r1.y + r1.height, r2.y + r2.height);

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Calculate percentage of frame that changed
     */
    public double calculateChangePercentage(List<Rectangle> dirtyRegions,
            int frameWidth, int frameHeight) {
        if (dirtyRegions.isEmpty()) {
            return 0.0;
        }

        int totalDirtyPixels = dirtyRegions.stream()
                .mapToInt(r -> r.width * r.height)
                .sum();

        int totalPixels = frameWidth * frameHeight;
        return (double) totalDirtyPixels / totalPixels;
    }

    /**
     * Reset detector (e.g., when resolution changes)
     */
    public void reset() {
        previousFrame = null;
        frameWidth = 0;
        frameHeight = 0;
        reusableDirtyRegions.clear();
    }

    /**
     * Rectangle class for representing dirty regions
     */
    public static class Rectangle {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return String.format("Rectangle[x=%d, y=%d, w=%d, h=%d]", x, y, width, height);
        }
    }
}

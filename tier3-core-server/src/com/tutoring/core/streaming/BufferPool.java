package com.tutoring.core.streaming;

import java.util.Arrays;

/**
 * ✅ NEW: Specialized Byte Array Pool
 * 
 * Optimized pool for byte[] buffers used in packet processing
 * Automatically resets buffer contents when returning to pool
 */
public class BufferPool {
    private static final int DEFAULT_MAX_POOL_SIZE = 50;
    private static final int[] COMMON_SIZES = {
        1400,      // Single UDP packet
        65536,     // 64KB buffer
        262144,    // 256KB buffer
        1048576    // 1MB buffer
    };
    
    private final ObjectPool<byte[]>[] pools;
    
    @SuppressWarnings("unchecked")
    public BufferPool() {
        pools = new ObjectPool[COMMON_SIZES.length];
        for (int i = 0; i < COMMON_SIZES.length; i++) {
            final int size = COMMON_SIZES[i];
            pools[i] = new ObjectPool<>(
                "byte[" + size + "]",
                () -> new byte[size],
                DEFAULT_MAX_POOL_SIZE
            );
        }
    }
    
    /**
     * Acquire a buffer of at least the requested size
     * Returns pooled buffer if available, or creates new one
     */
    public byte[] acquire(int minSize) {
        // Find smallest pool that fits
        for (int i = 0; i < COMMON_SIZES.length; i++) {
            if (COMMON_SIZES[i] >= minSize) {
                return pools[i].acquire();
            }
        }
        // No pool large enough - allocate new
        return new byte[minSize];
    }
    
    /**
     * Release a buffer back to the pool
     * Only pools buffers of known sizes
     */
    public void release(byte[] buffer) {
        if (buffer == null) return;
        
        int size = buffer.length;
        for (int i = 0; i < COMMON_SIZES.length; i++) {
            if (COMMON_SIZES[i] == size) {
                // Clear buffer before returning (security + reset state)
                Arrays.fill(buffer, (byte) 0);
                pools[i].release(buffer);
                return;
            }
        }
        // Unknown size - don't pool (let GC handle it)
    }
    
    /**
     * Get statistics for all pools
     */
    public void printStats() {
        System.out.println("\n=== Buffer Pool Statistics ===");
        for (ObjectPool<byte[]> pool : pools) {
            ObjectPool.PoolStats stats = pool.getStats();
            System.out.printf("  %s: size=%d, allocations=%d, reuses=%d, reuse_rate=%.1f%%%n",
                stats.name, stats.currentSize, stats.totalAllocations, stats.totalReuses,
                stats.getReuseRate() * 100);
        }
        System.out.println("==============================\n");
    }
    
    /**
     * Singleton instance (shared across all components)
     */
    private static BufferPool instance;
    
    public static synchronized BufferPool getInstance() {
        if (instance == null) {
            instance = new BufferPool();
        }
        return instance;
    }
}


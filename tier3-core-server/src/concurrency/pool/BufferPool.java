package concurrency.pool;

import java.util.Arrays;

public class BufferPool {
    private static final int DEFAULT_MAX_POOL_SIZE = 50;
    private static final int[] COMMON_SIZES = {
            1400, // Single UDP packet
            65536, // 64KB buffer
            262144, // 256KB buffer
            1048576 // 1MB buffer
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
                    DEFAULT_MAX_POOL_SIZE);
        }
    }

    public byte[] acquire(int minSize) {
        for (int i = 0; i < COMMON_SIZES.length; i++) {
            if (COMMON_SIZES[i] >= minSize) {
                return pools[i].acquire();
            }
        }
        return new byte[minSize];
    }

    public void release(byte[] buffer) {
        if (buffer == null)
            return;

        int size = buffer.length;
        for (int i = 0; i < COMMON_SIZES.length; i++) {
            if (COMMON_SIZES[i] == size) {
                Arrays.fill(buffer, (byte) 0);
                pools[i].release(buffer);
                return;
            }
        }
    }

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

    private static BufferPool instance;

    public static synchronized BufferPool getInstance() {
        if (instance == null) {
            instance = new BufferPool();
        }
        return instance;
    }
}

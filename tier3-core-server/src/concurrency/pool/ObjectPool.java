package concurrency.pool;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class ObjectPool<T> {
    private final Queue<T> pool = new ConcurrentLinkedQueue<>();
    private final Supplier<T> factory;
    private final int maxSize;
    private final String name;

    private long allocations = 0;
    private long reuses = 0;

    public ObjectPool(String name, Supplier<T> factory, int maxSize) {
        this.name = name;
        this.factory = factory;
        this.maxSize = maxSize;
    }

    public T acquire() {
        T obj = pool.poll();
        if (obj != null) {
            reuses++;
            return obj;
        }
        allocations++;
        return factory.get();
    }

    public void release(T obj) {
        if (obj != null && pool.size() < maxSize) {
            pool.offer(obj);
        }
    }

    public PoolStats getStats() {
        return new PoolStats(name, pool.size(), allocations, reuses);
    }

    public void clear() {
        pool.clear();
    }

    public static class PoolStats {
        public final String name;
        public final int currentSize;
        public final long totalAllocations;
        public final long totalReuses;

        public PoolStats(String name, int currentSize, long allocations, long reuses) {
            this.name = name;
            this.currentSize = currentSize;
            this.totalAllocations = allocations;
            this.totalReuses = reuses;
        }

        public double getReuseRate() {
            long total = totalAllocations + totalReuses;
            return total > 0 ? (double) totalReuses / total : 0.0;
        }
    }
}

package management;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central manager for all thread pools in the Core Server
 * Optimized for high-performance, low-latency networking
 * 
 * Thread Pools:
 * 1. Client Handler Pool - Fixed pool for TCP connections
 * 2. Broadcast Worker Pool - Work-stealing pool for parallel UDP broadcasting
 * 3. Frame Processing Pool - CPU-bound encoding/decoding tasks
 * 4. Monitoring Pool - Scheduled tasks for stats and quality evaluation
 */
public class ThreadPoolManager {
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    // Pool sizes optimized for networking workload
    private static final int CLIENT_HANDLER_POOL_SIZE = 200; // Max concurrent clients
    private static final int BROADCAST_WORKER_POOL_SIZE = CPU_CORES * 4; // High parallelism for I/O
    private static final int FRAME_PROCESSOR_POOL_SIZE = CPU_CORES; // CPU-bound tasks
    private static final int MONITORING_POOL_SIZE = 2; // Lightweight scheduled tasks

    // Thread pools
    private ExecutorService clientHandlerExecutor;
    private ExecutorService broadcastWorkerExecutor;
    private ExecutorService frameProcessorExecutor;
    private ScheduledExecutorService monitoringExecutor;

    // Metrics
    private final AtomicInteger activeClientHandlers = new AtomicInteger(0);
    private final AtomicInteger activeBroadcastTasks = new AtomicInteger(0);
    private final AtomicInteger rejectedConnections = new AtomicInteger(0);

    private volatile boolean isShutdown = false;

    public ThreadPoolManager() {
        initializeThreadPools();
        System.out.println("[ThreadPoolManager] Initialized with " + CPU_CORES + " CPU cores");
        System.out.println("[ThreadPoolManager] Client Handler Pool: " + CLIENT_HANDLER_POOL_SIZE);
        System.out.println("[ThreadPoolManager] Broadcast Worker Pool: " + BROADCAST_WORKER_POOL_SIZE);
        System.out.println("[ThreadPoolManager] Frame Processor Pool: " + FRAME_PROCESSOR_POOL_SIZE);
    }

    private void initializeThreadPools() {
        // 1. Client Handler Pool - Fixed pool with queue
        clientHandlerExecutor = new ThreadPoolExecutor(
                CLIENT_HANDLER_POOL_SIZE / 2, // Core pool size (50% for burst capacity)
                CLIENT_HANDLER_POOL_SIZE, // Max pool size
                60L, TimeUnit.SECONDS, // Keep-alive time
                new LinkedBlockingQueue<>(100), // Bounded queue to prevent memory issues
                new NamedThreadFactory("ClientHandler"),
                new ThreadPoolExecutor.AbortPolicy() // Reject if queue full
        );

        // 2. Broadcast Worker Pool - Work-stealing for load balancing
        // ForkJoinPool is optimized for parallel tasks with minimal contention
        broadcastWorkerExecutor = new ForkJoinPool(
                BROADCAST_WORKER_POOL_SIZE,
                new ForkJoinPool.ForkJoinWorkerThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(0);

                    @Override
                    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                        ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                        thread.setName("BroadcastWorker-" + counter.incrementAndGet());
                        return thread;
                    }
                },
                null, // No uncaught exception handler
                true // Async mode for better I/O performance
        );

        // 3. Frame Processor Pool - Fixed pool for CPU-bound tasks
        frameProcessorExecutor = new ThreadPoolExecutor(
                FRAME_PROCESSOR_POOL_SIZE,
                FRAME_PROCESSOR_POOL_SIZE,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(500),
                new NamedThreadFactory("FrameProcessor"),
                new ThreadPoolExecutor.CallerRunsPolicy() // Backpressure: caller runs if queue full
        );

        // 4. Monitoring Pool - Scheduled executor for periodic tasks
        monitoringExecutor = Executors.newScheduledThreadPool(
                MONITORING_POOL_SIZE,
                new NamedThreadFactory("Monitor"));
    }

    /**
     * Submit a client handler task
     * Returns false if rejected (server overloaded)
     */
    public boolean submitClientHandler(Runnable task) {
        if (isShutdown)
            return false;

        try {
            activeClientHandlers.incrementAndGet();
            clientHandlerExecutor.submit(() -> {
                try {
                    task.run();
                } finally {
                    activeClientHandlers.decrementAndGet();
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            rejectedConnections.incrementAndGet();
            activeClientHandlers.decrementAndGet();
            System.err.println("[ThreadPoolManager] Rejected client connection (overloaded)");
            return false;
        }
    }

    /**
     * Submit a broadcast task (returns Future for tracking)
     */
    public Future<?> submitBroadcastTask(Runnable task) {
        if (isShutdown)
            return CompletableFuture.completedFuture(null);

        activeBroadcastTasks.incrementAndGet();
        return broadcastWorkerExecutor.submit(() -> {
            try {
                task.run();
            } finally {
                activeBroadcastTasks.decrementAndGet();
            }
        });
    }

    /**
     * Submit multiple broadcast tasks in parallel
     * Returns when all tasks complete (with timeout)
     */
    public void submitBroadcastBatch(Runnable[] tasks, long timeoutMs) {
        if (tasks == null || tasks.length == 0)
            return;

        CountDownLatch latch = new CountDownLatch(tasks.length);

        for (Runnable task : tasks) {
            submitBroadcastTask(() -> {
                try {
                    task.run();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            // Wait for all broadcasts to complete (or timeout)
            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Submit frame processing task
     */
    public Future<?> submitFrameProcessing(Callable<?> task) {
        if (isShutdown)
            return CompletableFuture.completedFuture(null);
        return frameProcessorExecutor.submit(task);
    }

    /**
     * Schedule a monitoring task at fixed rate
     */
    public ScheduledFuture<?> scheduleMonitoring(Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (isShutdown)
            return null;
        return monitoringExecutor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    /**
     * Get executor for direct access (use with caution)
     */
    public ExecutorService getBroadcastExecutor() {
        return broadcastWorkerExecutor;
    }

    /**
     * Print pool statistics
     */
    public void printStats() {
        System.out.println("\n=== Thread Pool Statistics ===");
        System.out.println("Active Client Handlers: " + activeClientHandlers.get());
        System.out.println("Active Broadcast Tasks: " + activeBroadcastTasks.get());
        System.out.println("Rejected Connections: " + rejectedConnections.get());

        if (clientHandlerExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) clientHandlerExecutor;
            System.out.println("Client Handler Pool:");
            System.out.println("  Active Threads: " + tpe.getActiveCount());
            System.out.println("  Pool Size: " + tpe.getPoolSize());
            System.out.println("  Queue Size: " + tpe.getQueue().size());
            System.out.println("  Completed Tasks: " + tpe.getCompletedTaskCount());
        }

        if (frameProcessorExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) frameProcessorExecutor;
            System.out.println("Frame Processor Pool:");
            System.out.println("  Active Threads: " + tpe.getActiveCount());
            System.out.println("  Queue Size: " + tpe.getQueue().size());
        }

        System.out.println("==============================\n");
    }

    /**
     * Graceful shutdown of all thread pools
     */
    public void shutdown() {
        if (isShutdown)
            return;
        isShutdown = true;

        System.out.println("[ThreadPoolManager] Initiating graceful shutdown...");

        // Shutdown in reverse order of dependency
        shutdownExecutor(monitoringExecutor, "Monitoring", 5);
        shutdownExecutor(frameProcessorExecutor, "FrameProcessor", 10);
        shutdownExecutor(broadcastWorkerExecutor, "BroadcastWorker", 10);
        shutdownExecutor(clientHandlerExecutor, "ClientHandler", 30);

        System.out.println("[ThreadPoolManager] All thread pools shut down");
    }

    private void shutdownExecutor(ExecutorService executor, String name, int timeoutSeconds) {
        if (executor == null)
            return;

        System.out.println("[ThreadPoolManager] Shutting down " + name + " executor...");
        executor.shutdown();

        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                System.err.println("[ThreadPoolManager] " + name + " did not terminate in time, forcing shutdown");
                executor.shutdownNow();

                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("[ThreadPoolManager] " + name + " did not terminate after force shutdown");
                }
            } else {
                System.out.println("[ThreadPoolManager] " + name + " executor shut down cleanly");
            }
        } catch (InterruptedException e) {
            System.err.println("[ThreadPoolManager] Interrupted while shutting down " + name);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Custom thread factory for named threads
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final String prefix;

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(false); // Non-daemon for proper shutdown
            return thread;
        }
    }

    /**
     * Get metrics for monitoring
     */
    public ThreadPoolMetrics getMetrics() {
        return new ThreadPoolMetrics(
                activeClientHandlers.get(),
                activeBroadcastTasks.get(),
                rejectedConnections.get());
    }

    public static class ThreadPoolMetrics {
        public final int activeClientHandlers;
        public final int activeBroadcastTasks;
        public final int rejectedConnections;

        ThreadPoolMetrics(int activeClientHandlers, int activeBroadcastTasks, int rejectedConnections) {
            this.activeClientHandlers = activeClientHandlers;
            this.activeBroadcastTasks = activeBroadcastTasks;
            this.rejectedConnections = rejectedConnections;
        }
    }
}

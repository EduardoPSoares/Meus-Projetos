package me.ray.midgard.bot.core.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

public class TaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TaskScheduler.class);

    private final ScheduledExecutorService scheduler;
    private final ExecutorService asyncPool;
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public TaskScheduler(int poolSize) {
        this.scheduler = Executors.newScheduledThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "MidgardBot-Scheduler");
            t.setDaemon(true);
            return t;
        });
        this.asyncPool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "MidgardBot-Async");
            t.setDaemon(true);
            return t;
        });
    }

    public TaskScheduler() {
        this(2);
    }

    // ==================== Async Execution ====================

    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, asyncPool);
    }

    public <T> CompletableFuture<T> supplyAsync(java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncPool);
    }

    // ==================== Delayed Tasks ====================

    public ScheduledFuture<?> delay(Runnable task, long delay, TimeUnit unit) {
        return scheduler.schedule(wrapTask(task), delay, unit);
    }

    public ScheduledFuture<?> delaySeconds(Runnable task, long seconds) {
        return delay(task, seconds, TimeUnit.SECONDS);
    }

    public ScheduledFuture<?> delayMillis(Runnable task, long millis) {
        return delay(task, millis, TimeUnit.MILLISECONDS);
    }

    // ==================== Repeating Tasks ====================

    public ScheduledFuture<?> repeat(String name, Runnable task, long initialDelay, long period, TimeUnit unit) {
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(wrapTask(task), initialDelay, period, unit);
        tasks.put(name, future);
        logger.debug("Registered repeating task: {} ({}{})", name, period, unit.toString().toLowerCase().charAt(0));
        return future;
    }

    public ScheduledFuture<?> repeatSeconds(String name, Runnable task, long initialDelay, long periodSeconds) {
        return repeat(name, task, initialDelay, periodSeconds, TimeUnit.SECONDS);
    }

    public ScheduledFuture<?> repeatMinutes(String name, Runnable task, long initialDelay, long periodMinutes) {
        return repeat(name, task, initialDelay, periodMinutes, TimeUnit.MINUTES);
    }

    // ==================== Task Management ====================

    public boolean cancelTask(String name) {
        ScheduledFuture<?> future = tasks.remove(name);
        if (future != null) {
            future.cancel(false);
            logger.debug("Cancelled task: {}", name);
            return true;
        }
        return false;
    }

    public boolean isTaskRunning(String name) {
        ScheduledFuture<?> future = tasks.get(name);
        return future != null && !future.isDone() && !future.isCancelled();
    }

    public void cancelAllTasks() {
        for (var entry : tasks.entrySet()) {
            entry.getValue().cancel(false);
        }
        tasks.clear();
        logger.info("Cancelled all scheduled tasks");
    }

    public void shutdown() {
        cancelAllTasks();
        scheduler.shutdown();
        asyncPool.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!asyncPool.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            asyncPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Task scheduler shut down");
    }

    private Runnable wrapTask(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Error in scheduled task", e);
            }
        };
    }

    public int getActiveTaskCount() {
        return (int) tasks.values().stream()
                .filter(f -> !f.isDone() && !f.isCancelled())
                .count();
    }
}

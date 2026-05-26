package com.midgard.core.task;

import com.midgard.core.MidgardCore;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized task scheduler with named task tracking.
 */
public class TaskManager {

    private final MidgardCore plugin;
    private final Map<String, BukkitTask> tasks = new ConcurrentHashMap<>();

    public TaskManager(MidgardCore plugin) {
        this.plugin = plugin;
    }

    // --- Sync tasks ---

    public BukkitTask run(Runnable task) {
        return plugin.getServer().getScheduler().runTask(plugin, task);
    }

    public BukkitTask runLater(Runnable task, long delayTicks) {
        return plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    public BukkitTask runTimer(String name, Runnable task, long delayTicks, long periodTicks) {
        cancel(name);
        BukkitTask bukkit = plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        tasks.put(name, bukkit);
        return bukkit;
    }

    // --- Async tasks ---

    public BukkitTask runAsync(Runnable task) {
        return plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    public BukkitTask runAsyncLater(Runnable task, long delayTicks) {
        return plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
    }

    public BukkitTask runAsyncTimer(String name, Runnable task, long delayTicks, long periodTicks) {
        cancel(name);
        BukkitTask bukkit = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        tasks.put(name, bukkit);
        return bukkit;
    }

    // --- Management ---

    public void cancel(String name) {
        BukkitTask task = tasks.remove(name);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public boolean isRunning(String name) {
        BukkitTask task = tasks.get(name);
        return task != null && !task.isCancelled();
    }

    public void cancelAll() {
        tasks.values().forEach(t -> {
            if (!t.isCancelled()) t.cancel();
        });
        tasks.clear();
    }
}

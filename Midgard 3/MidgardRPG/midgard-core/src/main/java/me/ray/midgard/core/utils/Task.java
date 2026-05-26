package me.ray.midgard.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.concurrent.TimeUnit;

/**
 * Modernized task scheduler with Folia compatibility
 * Uses native Folia API when available, falls back to Bukkit scheduler
 */
public class Task {

    private static volatile JavaPlugin plugin;
    private static final boolean IS_FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        IS_FOLIA = folia;
    }

    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    // Region-aware scheduling - Entity
    public static void sync(Entity entity, Runnable runnable) {
        checkInit();
        if (IS_FOLIA) {
            entity.getScheduler().run(plugin, scheduledTask -> runnable.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    // Region-aware scheduling - Location
    public static void sync(Location location, Runnable runnable) {
        checkInit();
        if (IS_FOLIA) {
            Bukkit.getRegionScheduler().run(plugin, location, scheduledTask -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    // Global scheduling
    public static BukkitTask sync(Runnable runnable) {
        checkInit();
        if (IS_FOLIA) {
            return new FoliaTask(Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> runnable.run()));
        }
        return Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static BukkitTask async(Runnable runnable) {
        checkInit();
        if (IS_FOLIA) {
            return new FoliaTask(Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run()));
        }
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static BukkitTask syncLater(Entity entity, Runnable runnable, long delayTicks) {
        checkInit();
        if (IS_FOLIA) {
            var scheduled = entity.getScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), null, delayTicks);
            return scheduled != null ? new FoliaTask(scheduled) : null;
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public static BukkitTask syncLater(Location location, Runnable runnable, long delayTicks) {
        checkInit();
        if (IS_FOLIA) {
            return new FoliaTask(Bukkit.getRegionScheduler().runDelayed(plugin, location, scheduledTask -> runnable.run(), delayTicks));
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public static BukkitTask syncLater(Runnable runnable, long delayTicks) {
        checkInit();
        if (IS_FOLIA) {
            return new FoliaTask(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), delayTicks));
        }
        return Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
    }

    public static BukkitTask asyncLater(Runnable runnable, long delayTicks) {
        checkInit();
        if (IS_FOLIA) {
            return new FoliaTask(Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), delayTicks * 50, TimeUnit.MILLISECONDS));
        }
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
    }

    public static BukkitTask syncTimer(Entity entity, Runnable runnable, long delayTicks, long periodTicks) {
        checkInit();
        if (IS_FOLIA) {
            var scheduled = entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), null, delayTicks, periodTicks);
            return scheduled != null ? new FoliaTask(scheduled) : null;
        } else {
            // Paper doesn't have entity-specific timer, falls back to global (unsafe if entity moves across regions without care)
            // But Paper is single-threaded, so it's fine.
            return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        }
    }

    public static BukkitTask syncTimer(Location location, Runnable runnable, long delayTicks, long periodTicks) {
        checkInit();
        if (IS_FOLIA) {
            return new FoliaTask(Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, scheduledTask -> runnable.run(), delayTicks, periodTicks));
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        }
    }

    public static BukkitTask syncTimer(Runnable runnable, long delayTicks, long periodTicks) {
        checkInit();
        if (IS_FOLIA) {
            return new FoliaTask(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), delayTicks, periodTicks));
        }
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
    }

    public static BukkitTask asyncTimer(Runnable runnable, long delayTicks, long periodTicks) {
        checkInit();
        if (IS_FOLIA) {
            return new FoliaTask(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS));
        }
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
    }

    private static void checkInit() {
        if (plugin == null) {
            throw new IllegalStateException("Task utility not initialized! Call Task.init(plugin) first.");
        }
    }

    private static class FoliaTask implements BukkitTask {
        private final io.papermc.paper.threadedregions.scheduler.ScheduledTask task;
        private final JavaPlugin plugin;

        public FoliaTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
            this.task = task;
            this.plugin = Task.plugin;
        }

        @Override
        public int getTaskId() {
            return -1; // Not supported in Folia
        }

        @Override
        public org.bukkit.plugin.Plugin getOwner() {
            return plugin;
        }

        @Override
        public boolean isSync() {
            // Difficult to determine exactly without context, but usually true for region/global tasks
            return true; 
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}

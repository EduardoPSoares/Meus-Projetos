package me.ray.midgard.modules.performance.monitor;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.performance.PerformanceModule;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * Tarefa que monitora a saúde do servidor em background.
 * Gera alertas quando detecta problemas de performance.
 */
public class HealthMonitor implements Runnable {

    private final PerformanceModule module;
    private final TPSMonitor tpsMonitor;
    
    // Thresholds configuráveis
    private double tpsAlertThreshold = 15.0;
    private double memoryAlertThreshold = 85.0;
    private int entityAlertThreshold = 1000;
    private int chunkAlertThreshold = 500;
    
    // Estado de alertas (evita spam)
    private boolean tpsAlertSent = false;
    private boolean memoryAlertSent = false;
    private boolean entityAlertSent = false;
    private boolean chunkAlertSent = false;
    
    // Estatísticas
    private long checksPerformed = 0;
    private long alertsTriggered = 0;
    private long lastCheckTime = 0;
    
    private BukkitTask taskId = null;

    public HealthMonitor(PerformanceModule module, TPSMonitor tpsMonitor) {
        this.module = module;
        this.tpsMonitor = tpsMonitor;
    }

    /**
     * Inicia o monitoramento de saúde (a cada 5 segundos).
     */
    public void start() {
        if (taskId != null) {
            return;
        }
        // Run on main/global thread to safely access Bukkit API
        taskId = Task.syncTimer(this, 100L, 100L); // 5 seconds
    }

    /**
     * Para o monitoramento.
     */
    public void stop() {
        if (taskId != null) {
            taskId.cancel();
            taskId = null;
        }
    }

    @Override
    public void run() {
        checksPerformed++;
        lastCheckTime = System.currentTimeMillis();
        
        checkTPS();
        checkMemory();
        checkEntities();
        checkChunks();
    }

    private void checkTPS() {
        double tps = tpsMonitor.getAverageTPS();
        
        if (tps < tpsAlertThreshold) {
            if (!tpsAlertSent) {
                tpsAlertSent = true;
                alertsTriggered++;
                MidgardLogger.warn(String.format("[Performance] ⚠ TPS baixo detectado: %.1f (threshold: %.1f)", tps, tpsAlertThreshold));
                broadcastToAdmins(module.getMessage("watcher.alert_tps_low"), tps);
            }
        } else {
            if (tpsAlertSent) {
                tpsAlertSent = false;
                MidgardLogger.info(String.format("[Performance] ✔ TPS normalizado: %.1f", tps));
            }
        }
    }

    private void checkMemory() {
        var memInfo = ServerMonitor.getMemoryInfo();
        double usedPercent = memInfo.usedPercent();
        
        if (usedPercent > memoryAlertThreshold) {
            if (!memoryAlertSent) {
                memoryAlertSent = true;
                alertsTriggered++;
                MidgardLogger.warn(String.format("[Performance] ⚠ Memória alta: %.1f%% (threshold: %.1f%%)", usedPercent, memoryAlertThreshold));
                broadcastToAdmins(module.getMessage("watcher.alert_memory_high"), usedPercent);
            }
        } else {
            if (memoryAlertSent) {
                memoryAlertSent = false;
                MidgardLogger.info(String.format("[Performance] ✔ Memória normalizada: %.1f%%", usedPercent));
            }
        }
    }

    private void checkEntities() {
        int totalEntities = ServerMonitor.getEntityStats().values().stream()
            .mapToInt(e -> e.total())
            .sum();
        
        if (totalEntities > entityAlertThreshold) {
            if (!entityAlertSent) {
                entityAlertSent = true;
                alertsTriggered++;
                MidgardLogger.warn(String.format("[Performance] ⚠ Muitas entidades: %d (threshold: %d)", totalEntities, entityAlertThreshold));
            }
        } else {
            entityAlertSent = false;
        }
    }

    private void checkChunks() {
        int totalChunks = ServerMonitor.getChunkStats().values().stream()
            .mapToInt(c -> c.loaded())
            .sum();
        
        if (totalChunks > chunkAlertThreshold) {
            if (!chunkAlertSent) {
                chunkAlertSent = true;
                alertsTriggered++;
                MidgardLogger.warn(String.format("[Performance] ⚠ Muitos chunks: %d (threshold: %d)", totalChunks, chunkAlertThreshold));
            }
        } else {
            chunkAlertSent = false;
        }
    }

    private void broadcastToAdmins(String message, Object... args) {
        String formatted = String.format(message, args);
        Task.sync(() -> {
            Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("midgard.admin"))
                .forEach(p -> me.ray.midgard.core.text.MessageUtils.send(p, formatted));
        });
    }

    // ========== Getters e Setters ==========

    public void setTpsAlertThreshold(double threshold) {
        this.tpsAlertThreshold = threshold;
    }

    public void setMemoryAlertThreshold(double threshold) {
        this.memoryAlertThreshold = threshold;
    }

    public void setEntityAlertThreshold(int threshold) {
        this.entityAlertThreshold = threshold;
    }

    public void setChunkAlertThreshold(int threshold) {
        this.chunkAlertThreshold = threshold;
    }

    public long getChecksPerformed() {
        return checksPerformed;
    }

    public long getAlertsTriggered() {
        return alertsTriggered;
    }

    public long getLastCheckTime() {
        return lastCheckTime;
    }

    public boolean isRunning() {
        return taskId != null;
    }

    public HealthStatus getStatus() {
        return new HealthStatus(
            tpsAlertSent, memoryAlertSent, entityAlertSent, chunkAlertSent,
            tpsAlertThreshold, memoryAlertThreshold, entityAlertThreshold, chunkAlertThreshold
        );
    }

    public record HealthStatus(
        boolean tpsAlert, boolean memoryAlert, boolean entityAlert, boolean chunkAlert,
        double tpsThreshold, double memoryThreshold, int entityThreshold, int chunkThreshold
    ) {
        public int activeAlerts() {
            int count = 0;
            if (tpsAlert) {
                count++;
            }
            if (memoryAlert) {
                count++;
            }
            if (entityAlert) {
                count++;
            }
            if (chunkAlert) {
                count++;
            }
            return count;
        }
        
        public String getOverallStatus() {
            int alerts = activeAlerts();
            if (alerts == 0) {
                return "HEALTHY";
            }
            if (alerts <= 2) {
                return "WARNING";
            }
            return "CRITICAL";
        }
    }
}

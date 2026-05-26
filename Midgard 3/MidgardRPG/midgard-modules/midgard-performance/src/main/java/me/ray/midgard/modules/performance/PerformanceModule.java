package me.ray.midgard.modules.performance;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.performance.spark.MidgardAnalyzer;
import me.ray.midgard.modules.performance.spark.SparkPerformanceManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

/**
 * Módulo de Performance do MidgardRPG.
 * 
 * Utiliza Spark como fonte primária de métricas e fornece:
 * - Monitoramento em tempo real (TPS, MSPT, CPU, Memória, GC)
 * - Análise profunda do projeto MidgardRPG
 * - Diagnóstico automático de problemas
 * - Relatórios detalhados com recomendações
 * 
 * @requires Spark Profiler (https://spark.lucko.me/)
 */
public class PerformanceModule extends RPGModule {

    private static volatile PerformanceModule instance;
    private FileConfiguration messagesConfig;
    
    // Monitor de saúde contínuo
    private HealthWatcher healthWatcher;
    private BukkitTask watcherTaskId = null;

    public PerformanceModule() {
        super("Performance");
    }

    @Override
    public void onEnable() {
        instance = this;
        loadMessages();
        
        // Inicializa gerenciador Spark com delay via scheduler para resolver race-condition
        Task.sync(() -> {
            SparkPerformanceManager.init();
            MidgardAnalyzer.init();
            
            // Log de status após init tardio
            if (SparkPerformanceManager.isAvailable()) {
                MidgardLogger.info("[Performance] ✔ Módulo ativado com integração Spark completa");
            } else {
                MidgardLogger.warn("[Performance] ⚠ Módulo funcionando em modo limitado (sem Spark)");
            }
        });
        
        // Inicia monitoramento contínuo de saúde
        startHealthWatcher();
        
        // Registra comando apenas no AdminCommand para /rpg admin performance
        PerformanceCommand perfCmd = new PerformanceCommand(this);
        if (MidgardCore.getAdminCommand() != null) {
            MidgardCore.getAdminCommand().registerSubcommand(perfCmd);
        }
        

        
        // MidgardLogger.info("Módulo Performance inicializado (processo em background)");
        // ConsoleUtils.success("Módulo Performance ativado!");
    }

    @Override
    public void onDisable() {
        stopHealthWatcher();
        
        MidgardLogger.info("[Performance] Módulo desativado");
        instance = null;
    }

    /**
     * Inicia o monitoramento contínuo de saúde do servidor.
     * Verifica a cada 10 segundos e alerta sobre problemas críticos.
     */
    private void startHealthWatcher() {
        if (watcherTaskId != null) {
            return;
        }
        
        healthWatcher = new HealthWatcher(this);
        // Task assíncrona global para monitoramento de performance
        watcherTaskId = Task.asyncTimer(healthWatcher, 200L, 200L); // 10 segundos
    }

    private void stopHealthWatcher() {
        if (watcherTaskId != null) {
            watcherTaskId.cancel();
            watcherTaskId = null;
        }
    }

    public static PerformanceModule getInstance() {
        return instance;
    }

    private void loadMessages() {
        try {
            File messagesFile = new File(getDataFolder(), "lang/messages.yml");
            if (!messagesFile.exists()) {
                plugin.saveResource("modules/performance/lang/messages.yml", false);
            }
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        } catch (Exception e) {
            MidgardLogger.warn("Erro ao carregar mensagens do Performance: " + e.getMessage());
        }
    }

    public String getMessage(String path) {
        if (messagesConfig == null) {
            return path;
        }
        String message = messagesConfig.getString(path, path);
        return message.replace("&", "§");
    }

    public File getDataFolder() {
        return new File(plugin.getDataFolder(), "modules/performance");
    }

    public HealthWatcher getHealthWatcher() {
        return healthWatcher;
    }

    // ========== HEALTH WATCHER ==========

    /**
     * Monitora saúde do servidor em background e dispara alertas.
     */
    public static class HealthWatcher implements Runnable {
        private final PerformanceModule module;
        
        // Estado de alertas (evita spam)
        private volatile boolean tpsAlertActive = false;
        private volatile boolean memoryAlertActive = false;
        private volatile boolean cpuAlertActive = false;
        
        // Thresholds
        private static final double TPS_CRITICAL = 15.0;
        private static final double TPS_WARNING = 17.0;
        private static final double MEMORY_CRITICAL = 90.0;
        private static final double CPU_CRITICAL = 85.0;
        
        // Estatísticas
        private volatile long checksPerformed = 0;
        private volatile long alertsTriggered = 0;

        public HealthWatcher(PerformanceModule module) {
            this.module = module;
        }

        @Override
        public void run() {
            if (!SparkPerformanceManager.isAvailable()) {
                return;
            }
            
            checksPerformed++;
            var manager = SparkPerformanceManager.getInstance();
            var metrics = manager.getMetrics();
            
            checkTPS(metrics.tps());
            checkMemory(metrics.memory());
            checkCPU(metrics.cpu());
        }

        private void checkTPS(SparkPerformanceManager.TPSMetrics tps) {
            if (!tps.available()) {
                return;
            }
            
            double current = tps.last5s();
            
            if (current < TPS_CRITICAL) {
                if (!tpsAlertActive) {
                    tpsAlertActive = true;
                    alertsTriggered++;
                    MidgardLogger.warn(String.format("[Performance] ⚠ TPS CRÍTICO: %.1f - Servidor com lag severo!", current));
                    broadcastToAdmins(module.getMessage("watcher.alert_tps"), current);
                }
            } else if (current < TPS_WARNING) {
                if (!tpsAlertActive) {
                    tpsAlertActive = true;
                    alertsTriggered++;
                    MidgardLogger.warn(String.format("[Performance] ⚠ TPS baixo: %.1f", current));
                }
            } else {
                if (tpsAlertActive) {
                    tpsAlertActive = false;
                    MidgardLogger.info(String.format("[Performance] ✔ TPS normalizado: %.1f", current));
                }
            }
        }

        private void checkMemory(SparkPerformanceManager.MemoryMetrics mem) {
            if (!mem.available()) {
                return;
            }
            
            double percent = mem.usedPercent();
            
            if (percent > MEMORY_CRITICAL) {
                if (!memoryAlertActive) {
                    memoryAlertActive = true;
                    alertsTriggered++;
                    MidgardLogger.warn(String.format("[Performance] ⚠ MEMÓRIA CRÍTICA: %.1f%% - OOM iminente!", percent));
                    broadcastToAdmins(module.getMessage("watcher.alert_memory"), percent);
                }
            } else {
                if (memoryAlertActive) {
                    memoryAlertActive = false;
                    MidgardLogger.info(String.format("[Performance] ✔ Memória normalizada: %.1f%%", percent));
                }
            }
        }

        private void checkCPU(SparkPerformanceManager.CPUMetrics cpu) {
            if (!cpu.available()) {
                return;
            }
            
            double percent = cpu.process().seconds10() * 100;
            
            if (percent > CPU_CRITICAL) {
                if (!cpuAlertActive) {
                    cpuAlertActive = true;
                    alertsTriggered++;
                    MidgardLogger.warn(String.format("[Performance] ⚠ CPU alta: %.1f%%", percent));
                }
            } else {
                if (cpuAlertActive) {
                    cpuAlertActive = false;
                }
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

        public long getChecksPerformed() { return checksPerformed; }
        public long getAlertsTriggered() { return alertsTriggered; }
        public boolean isTpsAlertActive() { return tpsAlertActive; }
        public boolean isMemoryAlertActive() { return memoryAlertActive; }
        public boolean isCpuAlertActive() { return cpuAlertActive; }
    }
}


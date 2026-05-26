package me.ray.midgard.modules.performance.monitor;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.debug.MidgardProfiler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitora estatísticas internas do MidgardRPG.
 * Coleta dados de módulos, comandos, GUIs e operações do plugin.
 */
public class MidgardMonitor {

    // Contadores de eventos internos
    private static final Map<String, Long> eventCounts = new ConcurrentHashMap<>();
    private static final Map<String, Long> guiOpens = new ConcurrentHashMap<>();
    private static final Map<String, Long> commandExecutions = new ConcurrentHashMap<>();
    private static final Map<String, Long> databaseQueries = new ConcurrentHashMap<>();

    /**
     * Obtém estatísticas dos módulos registrados.
     */
    public static Map<String, ModuleStats> getModuleStats() {
        Map<String, ModuleStats> stats = new HashMap<>();
        
        var moduleManager = MidgardCore.getModuleManager();
        if (moduleManager == null) {
            return stats;
        }
        
        // Obtém tempos do profiler para cada módulo
        Map<String, Long> maxTimes = MidgardProfiler.getMaxExecutionTimes();
        Map<String, Integer> counts = MidgardProfiler.getInvocationCounts();
        
        // Itera sobre todos os módulos registrados
        for (var entry : moduleManager.getModules().entrySet()) {
            String name = entry.getKey();
            RPGModule module = entry.getValue();
            
            if (module != null) {
                String enableKey = "module_enable:" + name;
                long enableTime = maxTimes.getOrDefault(enableKey, 0L);
                
                // Coleta tempos de operações relacionadas ao módulo
                long totalProfiledTime = 0;
                int totalOperations = 0;
                
                for (var profEntry : maxTimes.entrySet()) {
                    if (profEntry.getKey().toLowerCase().contains(name.toLowerCase())) {
                        totalProfiledTime += profEntry.getValue();
                        totalOperations += counts.getOrDefault(profEntry.getKey(), 0);
                    }
                }
                
                stats.put(name, new ModuleStats(
                    module.isEnabled(),
                    enableTime,
                    totalProfiledTime,
                    totalOperations
                ));
            }
        }
        
        return stats;
    }

    /**
     * Obtém todas as operações monitoradas pelo profiler.
     */
    public static Map<String, OperationStats> getProfilerStats() {
        Map<String, OperationStats> stats = new HashMap<>();
        
        Map<String, Long> maxTimes = MidgardProfiler.getMaxExecutionTimes();
        Map<String, Long> lastTimes = MidgardProfiler.getLastExecutionTimes();
        Map<String, Integer> counts = MidgardProfiler.getInvocationCounts();
        
        for (String key : maxTimes.keySet()) {
            stats.put(key, new OperationStats(
                maxTimes.getOrDefault(key, 0L),
                lastTimes.getOrDefault(key, 0L),
                counts.getOrDefault(key, 0)
            ));
        }
        
        return stats;
    }

    /**
     * Obtém operações mais lentas.
     */
    public static Map<String, Long> getSlowestOperations(int limit) {
        return MidgardProfiler.getMaxExecutionTimes().entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));
    }

    /**
     * Obtém operações mais frequentes.
     */
    public static Map<String, Integer> getMostFrequentOperations(int limit) {
        return MidgardProfiler.getInvocationCounts().entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));
    }

    // ========== Tracking Methods ==========

    public static void trackEvent(String eventName) {
        eventCounts.merge(eventName, 1L, (a, b) -> a + b);
    }

    public static void trackGuiOpen(String guiName) {
        guiOpens.merge(guiName, 1L, (a, b) -> a + b);
    }

    public static void trackCommand(String command) {
        commandExecutions.merge(command, 1L, (a, b) -> a + b);
    }

    public static void trackDatabaseQuery(String queryType) {
        databaseQueries.merge(queryType, 1L, (a, b) -> a + b);
    }

    public static Map<String, Long> getEventCounts() {
        return new HashMap<>(eventCounts);
    }

    public static Map<String, Long> getGuiOpens() {
        return new HashMap<>(guiOpens);
    }

    public static Map<String, Long> getCommandExecutions() {
        return new HashMap<>(commandExecutions);
    }

    public static Map<String, Long> getDatabaseQueries() {
        return new HashMap<>(databaseQueries);
    }

    public static void clearTracking() {
        eventCounts.clear();
        guiOpens.clear();
        commandExecutions.clear();
        databaseQueries.clear();
    }

    // ========== Data Records ==========

    public record ModuleStats(boolean enabled, long enableTime, long totalProfiledTime, int totalOperations) {
        public String getStatus() {
            return enabled ? "<green>✔" : "<red>✘";
        }
        
        public String getTimeColor() {
            if (enableTime < 100) {
                return "<green>";
            }
            if (enableTime < 500) {
                return "<yellow>";
            }
            return "<red>";
        }
    }

    public record OperationStats(long maxTime, long lastTime, int count) {
        public String getTimeColor() {
            if (maxTime < 10) {
                return "<green>";
            }
            if (maxTime < 50) {
                return "<yellow>";
            }
            if (maxTime < 100) {
                return "<gold>";
            }
            return "<red>";
        }
    }
}

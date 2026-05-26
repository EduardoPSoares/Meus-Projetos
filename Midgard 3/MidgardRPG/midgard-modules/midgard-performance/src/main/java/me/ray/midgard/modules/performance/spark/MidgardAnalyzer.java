package me.ray.midgard.modules.performance.spark;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.debug.MidgardProfiler;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Analisador profundo do projeto MidgardRPG.
 * Monitora e analisa todos os componentes internos do plugin.
 */
public class MidgardAnalyzer {

    private static volatile MidgardAnalyzer instance;
    
    // Tracking de execuções
    private final Map<String, OperationTracker> operationTrackers = new ConcurrentHashMap<>();
    private final Map<String, Long> eventExecutionTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> eventExecutionCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> commandExecutionTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> commandExecutionCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> guiOpenTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> guiOpenCounts = new ConcurrentHashMap<>();
    
    // Snapshot de análise
    private volatile AnalysisSnapshot lastSnapshot;
    private volatile long lastSnapshotTime = 0;
    private static final long SNAPSHOT_CACHE_MS = 5000;

    private MidgardAnalyzer() {}

    public static void init() {
        instance = new MidgardAnalyzer();
    }

    public static MidgardAnalyzer getInstance() {
        return instance;
    }

    // ========== TRACKING METHODS ==========

    /**
     * Rastreia execução de um evento.
     */
    public void trackEvent(String eventName, long executionTimeNanos) {
        long millis = executionTimeNanos / 1_000_000;
        eventExecutionTimes.merge(eventName, millis, (a, b) -> Math.max(a, b));
        eventExecutionCounts.merge(eventName, 1, (a, b) -> a + b);
        
        getOrCreateTracker("event:" + eventName).record(millis);
    }

    /**
     * Rastreia execução de um comando.
     */
    public void trackCommand(String commandName, long executionTimeNanos) {
        long millis = executionTimeNanos / 1_000_000;
        commandExecutionTimes.merge(commandName, millis, (a, b) -> Math.max(a, b));
        commandExecutionCounts.merge(commandName, 1, (a, b) -> a + b);
        
        getOrCreateTracker("command:" + commandName).record(millis);
    }

    /**
     * Rastreia abertura de GUI.
     */
    public void trackGUI(String guiName, long executionTimeNanos) {
        long millis = executionTimeNanos / 1_000_000;
        guiOpenTimes.merge(guiName, millis, (a, b) -> Math.max(a, b));
        guiOpenCounts.merge(guiName, 1, (a, b) -> a + b);
        
        getOrCreateTracker("gui:" + guiName).record(millis);
    }

    /**
     * Rastreia operação genérica.
     */
    public void trackOperation(String operationName, long executionTimeNanos) {
        long millis = executionTimeNanos / 1_000_000;
        getOrCreateTracker(operationName).record(millis);
    }

    private OperationTracker getOrCreateTracker(String name) {
        return operationTrackers.computeIfAbsent(name, k -> new OperationTracker());
    }

    // ========== ANÁLISE PROFUNDA ==========

    /**
     * Executa análise completa do projeto.
     */
    public AnalysisSnapshot analyze() {
        long now = System.currentTimeMillis();
        if (lastSnapshot != null && (now - lastSnapshotTime) < SNAPSHOT_CACHE_MS) {
            return lastSnapshot;
        }

        lastSnapshot = new AnalysisSnapshot(
            analyzeModules(),
            analyzeEvents(),
            analyzeCommands(),
            analyzeProfiler(),
            analyzeMemoryUsage(),
            now
        );
        lastSnapshotTime = now;
        return lastSnapshot;
    }

    /**
     * Analisa todos os módulos do MidgardRPG.
     */
    private List<ModuleAnalysis> analyzeModules() {
        List<ModuleAnalysis> analyses = new ArrayList<>();
        
        var moduleManager = MidgardCore.getModuleManager();
        if (moduleManager == null) {
            return analyses;
        }

        Map<String, Long> profilerTimes = MidgardProfiler.getMaxExecutionTimes();
        Map<String, Integer> profilerCounts = MidgardProfiler.getInvocationCounts();

        for (var entry : moduleManager.getModules().entrySet()) {
            String name = entry.getKey();
            RPGModule module = entry.getValue();
            
            if (module == null) {
                continue;
            }

            // Tempo de inicialização
            String enableKey = "module_enable:" + name;
            long enableTime = profilerTimes.getOrDefault(enableKey, 0L);

            // Operações relacionadas ao módulo
            List<OperationAnalysis> operations = new ArrayList<>();
            long totalTime = 0;
            int totalOps = 0;

            for (var profEntry : profilerTimes.entrySet()) {
                String key = profEntry.getKey();
                if (key.toLowerCase().contains(name.toLowerCase()) && !key.equals(enableKey)) {
                    long time = profEntry.getValue();
                    int count = profilerCounts.getOrDefault(key, 0);
                    operations.add(new OperationAnalysis(key, time, count, calculateSeverity(time)));
                    totalTime += time;
                    totalOps += count;
                }
            }

            // Ordena operações por tempo
            operations.sort((a, b) -> Long.compare(b.maxTime(), a.maxTime()));

            // Listeners registrados pelo módulo
            int listenerCount = countModuleListeners(module);

            analyses.add(new ModuleAnalysis(
                name,
                module.isEnabled(),
                enableTime,
                totalTime,
                totalOps,
                listenerCount,
                operations,
                calculateModuleHealth(enableTime, totalTime, totalOps, operations)
            ));
        }

        // Ordena por tempo total
        analyses.sort((a, b) -> Long.compare(b.totalTime(), a.totalTime()));
        return analyses;
    }

    /**
     * Analisa eventos do Bukkit registrados pelo plugin.
     */
    private EventAnalysis analyzeEvents() {
        List<RegisteredListenerInfo> listeners = new ArrayList<>();
        Plugin midgardPlugin = MidgardCore.getInstance();
        
        if (midgardPlugin == null) {
            return new EventAnalysis(0, 0, List.of(), List.of());
        }

        int totalListeners = 0;
        Set<String> eventTypes = new HashSet<>();

        // Itera sobre todos os HandlerLists do Bukkit
        for (HandlerList handlerList : HandlerList.getHandlerLists()) {
            for (RegisteredListener listener : handlerList.getRegisteredListeners()) {
                if (listener.getPlugin().equals(midgardPlugin)) {
                    totalListeners++;
                    
                    String eventName = getEventName(handlerList);
                    eventTypes.add(eventName);
                    
                    long maxTime = eventExecutionTimes.getOrDefault(eventName, 0L);
                    int count = eventExecutionCounts.getOrDefault(eventName, 0);
                    
                    listeners.add(new RegisteredListenerInfo(
                        eventName,
                        listener.getListener().getClass().getSimpleName(),
                        listener.getPriority().name(),
                        maxTime,
                        count,
                        calculateSeverity(maxTime)
                    ));
                }
            }
        }

        // Ordena por tempo de execução
        listeners.sort((a, b) -> Long.compare(b.maxTime(), a.maxTime()));

        // Top eventos mais lentos
        List<RegisteredListenerInfo> slowest = listeners.stream()
            .filter(l -> l.maxTime() > 0)
            .limit(10)
            .collect(Collectors.toList());

        return new EventAnalysis(totalListeners, eventTypes.size(), listeners, slowest);
    }

    /**
     * Analisa comandos registrados.
     */
    private CommandAnalysis analyzeCommands() {
        List<CommandInfo> commands = new ArrayList<>();
        
        Map<String, Long> profilerTimes = MidgardProfiler.getMaxExecutionTimes();
        Map<String, Integer> profilerCounts = MidgardProfiler.getInvocationCounts();

        for (var entry : profilerTimes.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("command:")) {
                String cmdName = key.substring(8);
                long time = entry.getValue();
                int count = profilerCounts.getOrDefault(key, 0);
                
                commands.add(new CommandInfo(
                    cmdName,
                    time,
                    count,
                    count > 0 ? time : 0, // avg approximation
                    calculateSeverity(time)
                ));
            }
        }

        commands.sort((a, b) -> Long.compare(b.maxTime(), a.maxTime()));

        int totalExecutions = commands.stream().mapToInt(CommandInfo::executions).sum();
        List<CommandInfo> slowest = commands.stream()
            .filter(c -> c.maxTime() > 10)
            .limit(10)
            .collect(Collectors.toList());

        return new CommandAnalysis(commands.size(), totalExecutions, commands, slowest);
    }

    /**
     * Analisa dados do profiler interno.
     */
    private ProfilerAnalysis analyzeProfiler() {
        Map<String, Long> maxTimes = MidgardProfiler.getMaxExecutionTimes();
        Map<String, Long> lastTimes = MidgardProfiler.getLastExecutionTimes();
        Map<String, Integer> counts = MidgardProfiler.getInvocationCounts();

        List<ProfiledOperation> operations = new ArrayList<>();
        
        for (String key : maxTimes.keySet()) {
            operations.add(new ProfiledOperation(
                key,
                maxTimes.getOrDefault(key, 0L),
                lastTimes.getOrDefault(key, 0L),
                counts.getOrDefault(key, 0),
                calculateSeverity(maxTimes.getOrDefault(key, 0L))
            ));
        }

        operations.sort((a, b) -> Long.compare(b.maxTime(), a.maxTime()));

        // Top 15 mais lentas
        List<ProfiledOperation> slowest = operations.stream()
            .limit(15)
            .collect(Collectors.toList());

        // Top 15 mais frequentes
        List<ProfiledOperation> mostFrequent = operations.stream()
            .sorted((a, b) -> Integer.compare(b.count(), a.count()))
            .limit(15)
            .collect(Collectors.toList());

        long totalOperations = counts.values().stream().mapToInt(Integer::intValue).sum();
        long totalTime = maxTimes.values().stream().mapToLong(Long::longValue).sum();

        return new ProfilerAnalysis(operations.size(), totalOperations, totalTime, slowest, mostFrequent);
    }

    /**
     * Analisa uso de memória por componente (estimativa).
     */
    private MemoryAnalysis analyzeMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long heapUsed = runtime.totalMemory() - runtime.freeMemory();
        long heapMax = runtime.maxMemory();
        
        // Estimativas baseadas em heurísticas
        var moduleManager = MidgardCore.getModuleManager();
        int moduleCount = moduleManager != null ? moduleManager.getModules().size() : 0;
        int commandCount = (int) MidgardProfiler.getMaxExecutionTimes().keySet().stream()
            .filter(k -> k.startsWith("command:")).count();
        
        // Estimativa grosseira de memória por componente
        long estimatedModuleMemory = moduleCount * 50 * 1024L; // ~50KB por módulo
        long estimatedCommandMemory = commandCount * 10 * 1024L; // ~10KB por comando
        long estimatedCacheMemory = operationTrackers.size() * 5 * 1024L; // ~5KB por tracker
        
        return new MemoryAnalysis(
            heapUsed,
            heapMax,
            estimatedModuleMemory,
            estimatedCommandMemory,
            estimatedCacheMemory,
            moduleCount,
            commandCount,
            operationTrackers.size()
        );
    }

    // ========== HELPERS ==========

    private int countModuleListeners(RPGModule module) {
        // Conta listeners que pertencem ao pacote do módulo
        String modulePackage = module.getClass().getPackageName();
        int count = 0;
        
        for (HandlerList handlerList : HandlerList.getHandlerLists()) {
            for (RegisteredListener listener : handlerList.getRegisteredListeners()) {
                if (listener.getListener().getClass().getPackageName().startsWith(modulePackage)) {
                    count++;
                }
            }
        }
        return count;
    }

    private String getEventName(HandlerList handlerList) {
        try {
            // Tenta obter o nome do evento via reflection
            Class<?> enclosing = handlerList.getClass().getEnclosingClass();
            if (enclosing != null) {
                return enclosing.getSimpleName();
            }
        } catch (Exception ignored) { /* Reflection may fail for anonymous classes */ }
        return "Unknown";
    }

    private Severity calculateSeverity(long timeMs) {
        if (timeMs <= 5) {
            return Severity.EXCELLENT;
        }
        if (timeMs <= 15) {
            return Severity.GOOD;
        }
        if (timeMs <= 30) {
            return Severity.MODERATE;
        }
        if (timeMs <= 50) {
            return Severity.WARNING;
        }
        if (timeMs <= 100) {
            return Severity.CRITICAL;
        }
        return Severity.SEVERE;
    }

    private Severity calculateModuleHealth(long enableTime, long totalTime, int totalOps, List<OperationAnalysis> ops) {
        // Inicialização lenta
        if (enableTime > 1000) {
            return Severity.CRITICAL;
        }
        if (enableTime > 500) {
            return Severity.WARNING;
        }
        
        // Operações críticas
        long criticalOps = ops.stream().filter(o -> o.severity() == Severity.CRITICAL || o.severity() == Severity.SEVERE).count();
        if (criticalOps > 3) {
            return Severity.CRITICAL;
        }
        if (criticalOps > 0) {
            return Severity.WARNING;
        }
        
        // Tempo médio por operação
        if (totalOps > 0) {
            double avgTime = (double) totalTime / totalOps;
            if (avgTime > 50) {
                return Severity.WARNING;
            }
        }
        
        if (enableTime < 100) {
            return Severity.EXCELLENT;
        }
        return Severity.GOOD;
    }

    public void clearTracking() {
        operationTrackers.clear();
        eventExecutionTimes.clear();
        eventExecutionCounts.clear();
        commandExecutionTimes.clear();
        commandExecutionCounts.clear();
        guiOpenTimes.clear();
        guiOpenCounts.clear();
        lastSnapshot = null;
    }

    // ========== INNER CLASSES ==========

    private static class OperationTracker {
        private long maxTime = 0;
        // private long lastTime = 0;
        // private long totalTime = 0;
        // private int count = 0;

        void record(long timeMs) {
            maxTime = Math.max(maxTime, timeMs);
            // lastTime = timeMs;
            // totalTime += timeMs;
            // count++;
        }

        /*
        long getMaxTime() { return maxTime; }
        long getLastTime() { return lastTime; }
        long getTotalTime() { return totalTime; }
        int getCount() { return count; }
        double getAvgTime() { return count > 0 ? (double) totalTime / count : 0; }
        */
    }

    // ========== RECORDS ==========

    public record AnalysisSnapshot(
        List<ModuleAnalysis> modules,
        EventAnalysis events,
        CommandAnalysis commands,
        ProfilerAnalysis profiler,
        MemoryAnalysis memory,
        long timestamp
    ) {}

    public record ModuleAnalysis(
        String name,
        boolean enabled,
        long enableTime,
        long totalTime,
        int totalOperations,
        int listenerCount,
        List<OperationAnalysis> operations,
        Severity health
    ) {}

    public record OperationAnalysis(String name, long maxTime, int count, Severity severity) {}

    public record EventAnalysis(
        int totalListeners,
        int uniqueEvents,
        List<RegisteredListenerInfo> allListeners,
        List<RegisteredListenerInfo> slowest
    ) {}

    public record RegisteredListenerInfo(
        String eventName,
        String listenerClass,
        String priority,
        long maxTime,
        int executions,
        Severity severity
    ) {}

    public record CommandAnalysis(
        int totalCommands,
        int totalExecutions,
        List<CommandInfo> allCommands,
        List<CommandInfo> slowest
    ) {}

    public record CommandInfo(
        String name,
        long maxTime,
        int executions,
        long avgTime,
        Severity severity
    ) {}

    public record ProfilerAnalysis(
        int trackedOperations,
        long totalExecutions,
        long totalTime,
        List<ProfiledOperation> slowest,
        List<ProfiledOperation> mostFrequent
    ) {}

    public record ProfiledOperation(
        String name,
        long maxTime,
        long lastTime,
        int count,
        Severity severity
    ) {}

    public record MemoryAnalysis(
        long heapUsed,
        long heapMax,
        long estimatedModuleMemory,
        long estimatedCommandMemory,
        long estimatedCacheMemory,
        int moduleCount,
        int commandCount,
        int trackerCount
    ) {
        public double heapUsedPercent() {
            return heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;
        }
        
        public long heapUsedMB() { return heapUsed / 1024 / 1024; }
        public long heapMaxMB() { return heapMax / 1024 / 1024; }
    }

    public enum Severity {
        EXCELLENT("<green>", "✔"),
        GOOD("<yellow>", "●"),
        MODERATE("<gold>", "◐"),
        WARNING("<gold>", "⚠"),
        CRITICAL("<red>", "✘"),
        SEVERE("<dark_red>", "☠");

        private final String color;
        private final String icon;

        Severity(String color, String icon) {
            this.color = color;
            this.icon = icon;
        }

        public String getColor() { return color; }
        public String getIcon() { return icon; }
    }
}

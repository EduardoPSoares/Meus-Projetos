package me.ray.midgard.modules.performance.spark;

import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.gc.GarbageCollector;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.performance.PerformanceModule;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Map;

/**
 * Gerenciador central de Performance baseado no Spark.
 * Fornece acesso unificado a todas as métricas do servidor.
 * 
 * O Spark é OBRIGATÓRIO para funcionalidade completa.
 */
public class SparkPerformanceManager {

    private static volatile SparkPerformanceManager instance;
    private static volatile Spark spark;
    private static volatile boolean available = false;

    // Cache de métricas
    private volatile ServerMetrics lastMetrics;
    private volatile long lastMetricsUpdate = 0;
    private static final long CACHE_DURATION_MS = 1000; // 1 segundo

    private SparkPerformanceManager() {}

    /**
     * Inicializa o gerenciador de performance.
     */
    public static void init() {
        instance = new SparkPerformanceManager();
        
        try {
            // 1. Tenta obter via ServicesManager (Padrão Bukkit/Paper)
            // Isso deve funcionar para o Spark bundled no Paper 1.21
            RegisteredServiceProvider<Spark> provider = Bukkit.getServicesManager().getRegistration(Spark.class);
            if (provider != null) {
                spark = provider.getProvider();
                available = true;
                MidgardLogger.info("[SparkPerformance] ✔ Spark API integrada via ServicesManager!");
                return;
            }

            // 2. Tenta obter via SparkProvider (Static wrapper)
            try {
                spark = SparkProvider.get();
                available = true;
                MidgardLogger.info("[SparkPerformance] ✔ Spark API integrada via SparkProvider!");
                return;
            } catch (Throwable e) {
                // Apenas loga debug pois pode ser normal se não estiver carregado ainda
                MidgardLogger.info("[SparkPerformance] (Debug) SparkProvider direto falhou: " + e.getMessage());
            }

            // 3. Fallback: PluginManager
            if (Bukkit.getPluginManager().isPluginEnabled("spark")) {
                try {
                    spark = SparkProvider.get();
                    available = true;
                    MidgardLogger.info("[SparkPerformance] ✔ Spark API integrada (Plugin)!");
                } catch (Throwable e) {
                    MidgardLogger.warn("[SparkPerformance] Plugin detectado mas falha na API: " + e.getMessage());
                }
            } else {
                MidgardLogger.warn("[SparkPerformance] ✘ Spark não detectado nesta fase de inicialização.");
                MidgardLogger.warn("[SparkPerformance] Instale: https://spark.lucko.me/ se não estiver usando Paper.");
            }
        } catch (Throwable e) {
            MidgardLogger.warn(String.format("[SparkPerformance] Falha ao integrar Spark: %s", e.getMessage()));
            available = false;
        }
    }

    public static SparkPerformanceManager getInstance() {
        return instance;
    }

    public static boolean isAvailable() {
        return available && spark != null;
    }

    public static Spark getSpark() {
        return spark;
    }

    // ========== MÉTRICAS PRINCIPAIS ==========

    /**
     * Obtém todas as métricas do servidor em um único snapshot.
     * Utiliza cache para evitar chamadas excessivas.
     */
    public ServerMetrics getMetrics() {
        long now = System.currentTimeMillis();
        if (lastMetrics != null && (now - lastMetricsUpdate) < CACHE_DURATION_MS) {
            return lastMetrics;
        }
        
        lastMetrics = collectMetrics();
        lastMetricsUpdate = now;
        return lastMetrics;
    }

    /**
     * Força coleta de métricas ignorando cache.
     */
    public ServerMetrics collectMetrics() {
        if (!isAvailable()) {
            return ServerMetrics.unavailable();
        }

        try {
            return new ServerMetrics(
                collectTPSMetrics(),
                collectMSPTMetrics(),
                collectCPUMetrics(),
                collectGCMetrics(),
                collectMemoryMetrics(),
                System.currentTimeMillis()
            );
        } catch (Exception e) {
            MidgardLogger.warn(String.format("[SparkPerformance] Erro ao coletar métricas: %s", e.getMessage()));
            return ServerMetrics.unavailable();
        }
    }

    // ========== COLETORES INDIVIDUAIS ==========

    private TPSMetrics collectTPSMetrics() {
        DoubleStatistic<StatisticWindow.TicksPerSecond> tps = spark.tps();
        if (tps == null) {
            return TPSMetrics.unavailable();
        }

        return new TPSMetrics(
            tps.poll(StatisticWindow.TicksPerSecond.SECONDS_5),
            tps.poll(StatisticWindow.TicksPerSecond.SECONDS_10),
            tps.poll(StatisticWindow.TicksPerSecond.MINUTES_1),
            tps.poll(StatisticWindow.TicksPerSecond.MINUTES_5),
            tps.poll(StatisticWindow.TicksPerSecond.MINUTES_15),
            true
        );
    }

    private MSPTMetrics collectMSPTMetrics() {
        GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> mspt = spark.mspt();
        if (mspt == null) {
            return MSPTMetrics.unavailable();
        }

        DoubleAverageInfo last10s = mspt.poll(StatisticWindow.MillisPerTick.SECONDS_10);
        DoubleAverageInfo last1m = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_1);

        if (last10s == null || last1m == null) {
            return MSPTMetrics.unavailable();
        }

        return new MSPTMetrics(
            new MSPTWindow(last10s.min(), last10s.median(), last10s.percentile95th(), last10s.max()),
            new MSPTWindow(last1m.min(), last1m.median(), last1m.percentile95th(), last1m.max()),
            true
        );
    }

    private CPUMetrics collectCPUMetrics() {
        DoubleStatistic<StatisticWindow.CpuUsage> cpuProcess = spark.cpuProcess();
        DoubleStatistic<StatisticWindow.CpuUsage> cpuSystem = spark.cpuSystem();

        if (cpuProcess == null || cpuSystem == null) {
            return CPUMetrics.unavailable();
        }

        return new CPUMetrics(
            new CPUWindow(
                cpuProcess.poll(StatisticWindow.CpuUsage.SECONDS_10),
                cpuProcess.poll(StatisticWindow.CpuUsage.MINUTES_1),
                cpuProcess.poll(StatisticWindow.CpuUsage.MINUTES_15)
            ),
            new CPUWindow(
                cpuSystem.poll(StatisticWindow.CpuUsage.SECONDS_10),
                cpuSystem.poll(StatisticWindow.CpuUsage.MINUTES_1),
                cpuSystem.poll(StatisticWindow.CpuUsage.MINUTES_15)
            ),
            true
        );
    }

    private GCMetrics collectGCMetrics() {
        Map<String, GarbageCollector> gcMap = spark.gc();
        if (gcMap == null || gcMap.isEmpty()) {
            return GCMetrics.unavailable();
        }

        long totalCollections = 0;
        long totalTime = 0;
        double avgTime = 0;
        long avgFrequency = 0;
        int count = 0;

        for (GarbageCollector gc : gcMap.values()) {
            totalCollections += gc.totalCollections();
            totalTime += gc.totalTime();
            avgTime += gc.avgTime();
            avgFrequency += gc.avgFrequency();
            count++;
        }

        if (count > 0) {
            avgTime /= count;
            avgFrequency /= count;
        }

        return new GCMetrics(totalCollections, totalTime, avgTime, avgFrequency, gcMap, true);
    }

    private MemoryMetrics collectMemoryMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;

        return new MemoryMetrics(max, total, used, free, true);
    }

    // ========== ANÁLISE DE SAÚDE ==========

    /**
     * Realiza diagnóstico completo do servidor.
     */
    public HealthDiagnosis diagnose() {
        ServerMetrics metrics = getMetrics();
        
        return new HealthDiagnosis(
            diagnoseTPS(metrics.tps()),
            diagnoseMSPT(metrics.mspt()),
            diagnoseCPU(metrics.cpu()),
            diagnoseGC(metrics.gc()),
            diagnoseMemory(metrics.memory()),
            calculateOverallHealth(metrics)
        );
    }

    private static String msg(String key, String fallback) {
        PerformanceModule mod = PerformanceModule.getInstance();
        if (mod != null) {
            String val = mod.getMessage(key);
            if (val != null && !val.isEmpty() && !val.equals(key)) {
                return val;
            }
        }
        return fallback;
    }

    private HealthIssue diagnoseTPS(TPSMetrics tps) {
        if (!tps.available()) {
            return HealthIssue.unknown("TPS");
        }
        
        double current = tps.last5s();
        if (current >= 19.5) {
            return HealthIssue.healthy("TPS", String.format(msg("diagnosis.tps.excellent", "%.1f TPS - Excelente"), current));
        }
        if (current >= 18.0) {
            return HealthIssue.good("TPS", String.format(msg("diagnosis.tps.good", "%.1f TPS - Bom"), current));
        }
        if (current >= 15.0) {
            return HealthIssue.warning("TPS", String.format(msg("diagnosis.tps.warning", "%.1f TPS - Atenção necessária"), current));
        }
        if (current >= 10.0) {
            return HealthIssue.critical("TPS", String.format(msg("diagnosis.tps.critical", "%.1f TPS - CRÍTICO! Lag severo"), current));
        }
        return HealthIssue.severe("TPS", String.format(msg("diagnosis.tps.severe", "%.1f TPS - SEVERO! Servidor sob stress extremo"), current));
    }

    private HealthIssue diagnoseMSPT(MSPTMetrics mspt) {
        if (!mspt.available()) {
            return HealthIssue.unknown("MSPT");
        }
        
        double median = mspt.last10s().median();
        if (median <= 30) {
            return HealthIssue.healthy("MSPT", String.format(msg("diagnosis.mspt.excellent", "%.1fms mediana - Excelente"), median));
        }
        if (median <= 40) {
            return HealthIssue.good("MSPT", String.format(msg("diagnosis.mspt.good", "%.1fms mediana - Bom"), median));
        }
        if (median <= 50) {
            return HealthIssue.warning("MSPT", String.format(msg("diagnosis.mspt.warning", "%.1fms mediana - Limite do tick"), median));
        }
        return HealthIssue.critical("MSPT", String.format(msg("diagnosis.mspt.critical", "%.1fms mediana - Ticks atrasados!"), median));
    }

    private HealthIssue diagnoseCPU(CPUMetrics cpu) {
        if (!cpu.available()) {
            return HealthIssue.unknown("CPU");
        }
        
        double process = cpu.process().seconds10() * 100;
        if (process <= 50) {
            return HealthIssue.healthy("CPU", String.format(msg("diagnosis.cpu.excellent", "%.1f%% uso - Excelente"), process));
        }
        if (process <= 70) {
            return HealthIssue.good("CPU", String.format(msg("diagnosis.cpu.good", "%.1f%% uso - Bom"), process));
        }
        if (process <= 85) {
            return HealthIssue.warning("CPU", String.format(msg("diagnosis.cpu.warning", "%.1f%% uso - Alto"), process));
        }
        return HealthIssue.critical("CPU", String.format(msg("diagnosis.cpu.critical", "%.1f%% uso - CRÍTICO!"), process));
    }

    private HealthIssue diagnoseGC(GCMetrics gc) {
        if (!gc.available()) {
            return HealthIssue.unknown("GC");
        }
        
        // GC frequente (< 30s entre coletas) pode indicar problemas
        if (gc.avgFrequency() > 60000) {
            return HealthIssue.healthy("GC", msg("diagnosis.gc.excellent", "Frequência baixa - Normal"));
        }
        if (gc.avgFrequency() > 30000) {
            return HealthIssue.good("GC", msg("diagnosis.gc.good", "Frequência moderada"));
        }
        if (gc.avgFrequency() > 10000) {
            return HealthIssue.warning("GC", msg("diagnosis.gc.warning", "GC frequente - Possível memory pressure"));
        }
        return HealthIssue.critical("GC", msg("diagnosis.gc.critical", "GC muito frequente - Memory leak provável!"));
    }

    private HealthIssue diagnoseMemory(MemoryMetrics mem) {
        if (!mem.available()) {
            return HealthIssue.unknown("Memory");
        }
        
        double percent = mem.usedPercent();
        if (percent <= 60) {
            return HealthIssue.healthy("Memory", String.format(msg("diagnosis.memory.excellent", "%.1f%% usado - Excelente"), percent));
        }
        if (percent <= 75) {
            return HealthIssue.good("Memory", String.format(msg("diagnosis.memory.good", "%.1f%% usado - Bom"), percent));
        }
        if (percent <= 85) {
            return HealthIssue.warning("Memory", String.format(msg("diagnosis.memory.warning", "%.1f%% usado - Alto"), percent));
        }
        if (percent <= 95) {
            return HealthIssue.critical("Memory", String.format(msg("diagnosis.memory.critical", "%.1f%% usado - CRÍTICO!"), percent));
        }
        return HealthIssue.severe("Memory", String.format(msg("diagnosis.memory.severe", "%.1f%% usado - EMERGÊNCIA! OOM iminente"), percent));
    }

    private HealthLevel calculateOverallHealth(ServerMetrics metrics) {
        if (!metrics.available()) {
            return HealthLevel.UNKNOWN;
        }
        
        int score = 0;
        int total = 0;
        
        // TPS (peso 3)
        if (metrics.tps().available()) {
            double tps = metrics.tps().last5s();
            if (tps >= 19) {
                score += 3;
            } else if (tps >= 17) {
                score += 2;
            } else if (tps >= 15) {
                score += 1;
            }
            total += 3;
        }
        
        // MSPT (peso 2)
        if (metrics.mspt().available()) {
            double mspt = metrics.mspt().last10s().median();
            if (mspt <= 35) {
                score += 2;
            } else if (mspt <= 45) {
                score += 1;
            }
            total += 2;
        }
        
        // Memory (peso 2)
        if (metrics.memory().available()) {
            double mem = metrics.memory().usedPercent();
            if (mem <= 70) {
                score += 2;
            } else if (mem <= 85) {
                score += 1;
            }
            total += 2;
        }
        
        // CPU (peso 1)
        if (metrics.cpu().available()) {
            double cpu = metrics.cpu().process().seconds10() * 100;
            if (cpu <= 60) {
                score += 1;
            }
            total += 1;
        }
        
        if (total == 0) {
            return HealthLevel.UNKNOWN;
        }
        
        double percentage = (double) score / total * 100;
        if (percentage >= 90) {
            return HealthLevel.EXCELLENT;
        }
        if (percentage >= 75) {
            return HealthLevel.GOOD;
        }
        if (percentage >= 50) {
            return HealthLevel.WARNING;
        }
        if (percentage >= 25) {
            return HealthLevel.CRITICAL;
        }
        return HealthLevel.SEVERE;
    }

    // ========== RECORDS E ENUMS ==========

    public record ServerMetrics(
        TPSMetrics tps,
        MSPTMetrics mspt,
        CPUMetrics cpu,
        GCMetrics gc,
        MemoryMetrics memory,
        long timestamp
    ) {
        public boolean available() {
            return timestamp > 0;
        }

        public static ServerMetrics unavailable() {
            return new ServerMetrics(
                TPSMetrics.unavailable(),
                MSPTMetrics.unavailable(),
                CPUMetrics.unavailable(),
                GCMetrics.unavailable(),
                MemoryMetrics.unavailable(),
                0
            );
        }
    }

    public record TPSMetrics(
        double last5s, double last10s, double last1m, double last5m, double last15m,
        boolean available
    ) {
        public static TPSMetrics unavailable() {
            return new TPSMetrics(0, 0, 0, 0, 0, false);
        }

        public String getColor(double tps) {
            if (tps >= 19.0) {
                return "<green>";
            }
            if (tps >= 17.0) {
                return "<yellow>";
            }
            if (tps >= 15.0) {
                return "<gold>";
            }
            return "<red>";
        }

        public double average() {
            return (last5s + last10s + last1m + last5m + last15m) / 5.0;
        }
    }

    public record MSPTMetrics(MSPTWindow last10s, MSPTWindow last1m, boolean available) {
        public static MSPTMetrics unavailable() {
            return new MSPTMetrics(MSPTWindow.empty(), MSPTWindow.empty(), false);
        }
    }

    public record MSPTWindow(double min, double median, double p95, double max) {
        public static MSPTWindow empty() {
            return new MSPTWindow(0, 0, 0, 0);
        }

        public String getColor(double mspt) {
            if (mspt <= 30) {
                return "<green>";
            }
            if (mspt <= 40) {
                return "<yellow>";
            }
            if (mspt <= 50) {
                return "<gold>";
            }
            return "<red>";
        }
    }

    public record CPUMetrics(CPUWindow process, CPUWindow system, boolean available) {
        public static CPUMetrics unavailable() {
            return new CPUMetrics(CPUWindow.empty(), CPUWindow.empty(), false);
        }
    }

    public record CPUWindow(double seconds10, double minutes1, double minutes15) {
        public static CPUWindow empty() {
            return new CPUWindow(0, 0, 0);
        }

        public String getColor(double cpu) {
            if (cpu <= 0.50) {
                return "<green>";
            }
            if (cpu <= 0.70) {
                return "<yellow>";
            }
            if (cpu <= 0.85) {
                return "<gold>";
            }
            return "<red>";
        }

        public String formatPercent(double value) {
            return String.format("%.1f%%", value * 100);
        }
    }

    public record GCMetrics(
        long totalCollections, long totalTime, double avgTime, long avgFrequency,
        Map<String, GarbageCollector> collectors, boolean available
    ) {
        public static GCMetrics unavailable() {
            return new GCMetrics(0, 0, 0, 0, Map.of(), false);
        }

        public String formatTime() {
            if (totalTime < 1000) {
                return totalTime + "ms";
            }
            return String.format("%.1fs", totalTime / 1000.0);
        }
    }

    public record MemoryMetrics(long max, long total, long used, long free, boolean available) {
        public static MemoryMetrics unavailable() {
            return new MemoryMetrics(0, 0, 0, 0, false);
        }

        public long usedMB() { return used / 1024 / 1024; }
        public long freeMB() { return free / 1024 / 1024; }
        public long maxMB() { return max / 1024 / 1024; }
        public long totalMB() { return total / 1024 / 1024; }

        public double usedPercent() {
            return max > 0 ? (double) used / max * 100 : 0;
        }

        public String getColor() {
            double percent = usedPercent();
            if (percent <= 60) {
                return "<green>";
            }
            if (percent <= 75) {
                return "<yellow>";
            }
            if (percent <= 85) {
                return "<gold>";
            }
            return "<red>";
        }
    }

    public record HealthDiagnosis(
        HealthIssue tps, HealthIssue mspt, HealthIssue cpu, HealthIssue gc, HealthIssue memory,
        HealthLevel overallHealth
    ) {}

    public record HealthIssue(String category, HealthLevel level, String message) {
        public static HealthIssue healthy(String category, String message) {
            return new HealthIssue(category, HealthLevel.EXCELLENT, message);
        }
        public static HealthIssue good(String category, String message) {
            return new HealthIssue(category, HealthLevel.GOOD, message);
        }
        public static HealthIssue warning(String category, String message) {
            return new HealthIssue(category, HealthLevel.WARNING, message);
        }
        public static HealthIssue critical(String category, String message) {
            return new HealthIssue(category, HealthLevel.CRITICAL, message);
        }
        public static HealthIssue severe(String category, String message) {
            return new HealthIssue(category, HealthLevel.SEVERE, message);
        }
        public static HealthIssue unknown(String category) {
            return new HealthIssue(category, HealthLevel.UNKNOWN, msg("diagnosis.unavailable", "Dados indisponíveis"));
        }

        public String getIcon() {
            return level.getIcon();
        }

        public String getColor() {
            return level.getColor();
        }
    }

    public enum HealthLevel {
        EXCELLENT("<green>", "★★★★★", "Excelente"),
        GOOD("<yellow>", "★★★★☆", "Bom"),
        WARNING("<gold>", "★★★☆☆", "Atenção"),
        CRITICAL("<red>", "★★☆☆☆", "Crítico"),
        SEVERE("<dark_red>", "★☆☆☆☆", "Severo"),
        UNKNOWN("<gray>", "?????", "Desconhecido");

        private final String color;
        private final String icon;
        private final String label;

        HealthLevel(String color, String icon, String label) {
            this.color = color;
            this.icon = icon;
            this.label = label;
        }

        public String getColor() { return color; }
        public String getIcon() { return icon; }
        public String getLabel() { return msg("diagnosis.health." + name().toLowerCase(), label); }
    }
}

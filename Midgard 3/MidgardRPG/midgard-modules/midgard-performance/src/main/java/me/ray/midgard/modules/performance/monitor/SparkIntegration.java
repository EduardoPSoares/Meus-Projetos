package me.ray.midgard.modules.performance.monitor;

import me.lucko.spark.api.Spark;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.Bukkit;

/**
 * Integração direta com o Spark Profiler.
 * Usa a API do Spark para obter métricas precisas do servidor.
 */
public class SparkIntegration {

    private static Spark spark;
    private static boolean available = false;

    /**
     * Inicializa a integração com o Spark.
     */
    public static void init() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("spark")) {
                spark = SparkProvider.get();
                available = true;
                MidgardLogger.info("[Performance] Spark API integrada com sucesso!");
            } else {
                MidgardLogger.info("[Performance] Spark não encontrado - usando monitor interno");
            }
        } catch (Throwable e) {
            MidgardLogger.warn("[Performance] Falha ao integrar Spark: " + e.getMessage());
            available = false;
        }
    }

    /**
     * Verifica se o Spark está disponível.
     */
    public static boolean isAvailable() {
        return available && spark != null;
    }

    /**
     * Obtém o TPS do Spark (mais preciso que nosso monitor).
     */
    public static SparkTPS getTPS() {
        if (!isAvailable()) {
            return null;
        }
        
        try {
            DoubleStatistic<StatisticWindow.TicksPerSecond> tps = spark.tps();
            if (tps == null) {
                return null;
            }
            
            double last5s = tps.poll(StatisticWindow.TicksPerSecond.SECONDS_5);
            double last10s = tps.poll(StatisticWindow.TicksPerSecond.SECONDS_10);
            double last1m = tps.poll(StatisticWindow.TicksPerSecond.MINUTES_1);
            double last5m = tps.poll(StatisticWindow.TicksPerSecond.MINUTES_5);
            double last15m = tps.poll(StatisticWindow.TicksPerSecond.MINUTES_15);
            
            return new SparkTPS(last5s, last10s, last1m, last5m, last15m);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtém o MSPT (Milliseconds Per Tick) do Spark.
     */
    public static SparkMSPT getMSPT() {
        if (!isAvailable()) {
            return null;
        }
        
        try {
            GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick> mspt = spark.mspt();
            if (mspt == null) {
                return null;
            }
            
            DoubleAverageInfo last10s = mspt.poll(StatisticWindow.MillisPerTick.SECONDS_10);
            DoubleAverageInfo last1m = mspt.poll(StatisticWindow.MillisPerTick.MINUTES_1);
            
            if (last10s == null || last1m == null) {
                return null;
            }
            
            return new SparkMSPT(
                last10s.min(), last10s.median(), last10s.percentile95th(), last10s.max(),
                last1m.min(), last1m.median(), last1m.percentile95th(), last1m.max()
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtém uso de CPU do Spark.
     */
    public static SparkCPU getCPU() {
        if (!isAvailable()) {
            return null;
        }
        
        try {
            DoubleStatistic<StatisticWindow.CpuUsage> cpuProcess = spark.cpuProcess();
            DoubleStatistic<StatisticWindow.CpuUsage> cpuSystem = spark.cpuSystem();
            
            if (cpuProcess == null || cpuSystem == null) {
                return null;
            }
            
            return new SparkCPU(
                cpuProcess.poll(StatisticWindow.CpuUsage.SECONDS_10),
                cpuProcess.poll(StatisticWindow.CpuUsage.MINUTES_1),
                cpuProcess.poll(StatisticWindow.CpuUsage.MINUTES_15),
                cpuSystem.poll(StatisticWindow.CpuUsage.SECONDS_10),
                cpuSystem.poll(StatisticWindow.CpuUsage.MINUTES_1),
                cpuSystem.poll(StatisticWindow.CpuUsage.MINUTES_15)
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtém informações de GC do Spark.
     */
    public static SparkGC getGCStats() {
        if (!isAvailable()) {
            return null;
        }
        
        try {
            var gcStats = spark.gc();
            if (gcStats == null) {
                return null;
            }
            
            long totalCollections = 0;
            long totalTime = 0;
            double avgFrequency = 0;
            
            for (var entry : gcStats.entrySet()) {
                var info = entry.getValue();
                totalCollections += info.totalCollections();
                totalTime += info.totalTime();
                avgFrequency += info.avgFrequency();
            }
            
            return new SparkGC(totalCollections, totalTime, avgFrequency / Math.max(1, gcStats.size()));
        } catch (Exception e) {
            return null;
        }
    }

    // ========== Data Records ==========

    public record SparkTPS(double last5s, double last10s, double last1m, double last5m, double last15m) {
        public String getColor(double tps) {
            if (tps >= 19.0) {
                return "<green>";
            }
            if (tps >= 15.0) {
                return "<yellow>";
            }
            if (tps >= 10.0) {
                return "<gold>";
            }
            return "<red>";
        }
        
        public String format(double tps) {
            return String.format("%.2f", tps);
        }
    }

    public record SparkMSPT(
        double min10s, double median10s, double p95_10s, double max10s,
        double min1m, double median1m, double p95_1m, double max1m
    ) {
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
        
        public String format(double mspt) {
            return String.format("%.1f", mspt);
        }
    }

    public record SparkCPU(
        double process10s, double process1m, double process15m,
        double system10s, double system1m, double system15m
    ) {
        public String getColor(double percent) {
            if (percent <= 50) {
                return "<green>";
            }
            if (percent <= 70) {
                return "<yellow>";
            }
            if (percent <= 90) {
                return "<gold>";
            }
            return "<red>";
        }
        
        public String formatPercent(double value) {
            return String.format("%.1f%%", value * 100);
        }
    }

    public record SparkGC(long totalCollections, long totalTimeMs, double avgFrequency) {
        public String formatTime() {
            if (totalTimeMs < 1000) {
                return totalTimeMs + "ms";
            }
            return String.format("%.1fs", totalTimeMs / 1000.0);
        }
    }
}

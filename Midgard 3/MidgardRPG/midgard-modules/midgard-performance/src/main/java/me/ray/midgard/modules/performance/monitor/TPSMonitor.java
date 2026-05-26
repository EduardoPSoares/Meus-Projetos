package me.ray.midgard.modules.performance.monitor;

import me.ray.midgard.core.utils.Task;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;

/**
 * Monitora o TPS (Ticks Per Second) do servidor.
 * Usa um sistema de média móvel para calcular TPS preciso.
 */
public class TPSMonitor implements Runnable {

    private static final int SAMPLE_SIZE = 100;
    private static final long EXPECTED_TICK_TIME = 50_000_000L; // 50ms em nanosegundos
    
    private final LinkedList<Double> tpsHistory = new LinkedList<>();
    private long lastTick = System.nanoTime();
    private double currentTPS = 20.0;
    private double averageTPS = 20.0;
    private double minTPS = 20.0;
    private double maxTPS = 20.0;
    
    private BukkitTask task = null;

    /**
     * Inicia o monitoramento de TPS.
     */
    public void start(JavaPlugin plugin) {
        if (task != null) {
            return;
        }
        
        lastTick = System.nanoTime();
        task = Task.syncTimer(this, 0L, 1L);
    }

    /**
     * Para o monitoramento.
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void run() {
        long now = System.nanoTime();
        long elapsed = now - lastTick;
        lastTick = now;
        
        // Calcula TPS baseado no tempo decorrido
        double tps = EXPECTED_TICK_TIME / (double) elapsed * 20.0;
        tps = Math.min(20.0, Math.max(0.0, tps)); // Clamp entre 0-20
        
        currentTPS = tps;
        
        // Adiciona à história
        tpsHistory.addLast(tps);
        if (tpsHistory.size() > SAMPLE_SIZE) {
            tpsHistory.removeFirst();
        }
        
        // Recalcula estatísticas
        recalculateStats();
    }

    private void recalculateStats() {
        if (tpsHistory.isEmpty()) {
            return;
        }
        
        double sum = 0;
        double min = 20.0;
        double max = 0.0;
        
        for (double tps : tpsHistory) {
            sum += tps;
            if (tps < min) {
                min = tps;
            }
            if (tps > max) {
                max = tps;
            }
        }
        
        averageTPS = sum / tpsHistory.size();
        minTPS = min;
        maxTPS = max;
    }

    /**
     * Retorna o TPS atual (último tick).
     */
    public double getCurrentTPS() {
        return currentTPS;
    }

    /**
     * Retorna o TPS médio baseado nos últimos N ticks.
     */
    public double getAverageTPS() {
        return averageTPS;
    }

    /**
     * Retorna o TPS mínimo registrado.
     */
    public double getMinTPS() {
        return minTPS;
    }

    /**
     * Retorna o TPS máximo registrado.
     */
    public double getMaxTPS() {
        return maxTPS;
    }

    /**
     * Retorna a cor MiniMessage baseada no TPS.
     */
    public String getTPSColor(double tps) {
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

    /**
     * Reseta as estatísticas.
     */
    public void reset() {
        tpsHistory.clear();
        currentTPS = 20.0;
        averageTPS = 20.0;
        minTPS = 20.0;
        maxTPS = 20.0;
    }
}

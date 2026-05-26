package me.ray.midgardspigot.chaos;

import java.util.logging.Logger;
import java.util.Random;

/**
 * ChaosEngine - Ferramenta para injeção de falhas controlada.
 * Use ChaosEngine.check("contexto") no início de métodos para simular erros.
 */
public class ChaosEngine {
    private static Logger logger = Logger.getLogger("ChaosEngine");
    private static boolean enabled = false;
    private static double failureRate = 0.1; // 10% padrão quando ativado
    private static final Random random = new Random();

    public static void setLogger(Logger log) {
        logger = log;
    }

    /**
     * Ativa ou desativa o Chaos Engine.
     * @param value true para ativar, false para desativar.
     */
    public static void setEnabled(boolean value) {
        enabled = value;
        if (logger != null) logger.info("Chaos Engine " + (enabled ? "ENABLED" : "DISABLED"));
    }

    /**
     * Define a taxa de falha (0.0 a 1.0).
     * @param rate Probabilidade de falha (ex: 0.1 para 10%).
     */
    public static void setFailureRate(double rate) {
        failureRate = Math.max(0.0, Math.min(1.0, rate));
        if (logger != null) logger.info("Chaos Engine failure rate set to " + (failureRate * 100) + "%");
    }

    /**
     * Verifica se deve ocorrer uma falha neste ponto.
     * @param context Descrição do local onde a verificação está sendo feita.
     * @throws RuntimeException se o Chaos Engine decidir falhar.
     */
    public static void check(String context) {
        if (!enabled) return;
        
        if (random.nextDouble() < failureRate) {
            String message = "ChaosEngine: Forced error in " + context;
            if (logger != null) logger.warning("ChaosEngine triggered in " + context);
            throw new RuntimeException(message);
        }
    }
    
    /**
     * Força uma falha imediatamente se o engine estiver ativado.
     * @param context Descrição do local.
     */
    public static void forceFail(String context) {
        if (!enabled) return;
        String message = "ChaosEngine: FORCED FAIL in " + context;
        if (logger != null) logger.severe(message);
        throw new RuntimeException(message);
    }
}

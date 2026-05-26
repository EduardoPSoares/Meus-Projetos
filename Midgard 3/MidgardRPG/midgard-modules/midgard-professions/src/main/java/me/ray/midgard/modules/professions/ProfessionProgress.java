package me.ray.midgard.modules.professions;

/**
 * Progresso de um jogador em uma profissão específica.
 * Armazena nível, XP e dados básicos de progressão.
 */
public class ProfessionProgress {

    private final ProfessionType type;
    private volatile int level;
    private volatile double xp;
    private volatile double xpToNextLevel;

    public ProfessionProgress(ProfessionType type) {
        this.type = type;
        this.level = 0;
        this.xp = 0;
        this.xpToNextLevel = calculateXpNeeded(1);
    }

    public ProfessionProgress(ProfessionType type, int level, double xp) {
        this.type = type;
        this.level = level;
        this.xp = xp;
        this.xpToNextLevel = calculateXpNeeded(level + 1);
    }

    public ProfessionType getType() { return type; }
    public int getLevel() { return level; }
    public double getXp() { return xp; }
    public double getXpToNextLevel() { return xpToNextLevel; }

    public synchronized void setLevel(int level) {
        this.level = Math.max(0, Math.min(level, 100));
        this.xpToNextLevel = calculateXpNeeded(this.level + 1);
    }

    public synchronized void setXp(double xp) {
        this.xp = xp;
    }

    /**
     * Adiciona XP e processa level-ups. Retorna quantos níveis ganhou.
     * Sincronizado pois BrewEvent pode disparar em region thread diferente do jogador.
     */
    public synchronized int addXp(double amount) {
        if (amount <= 0 || level >= 100) { return 0; }

        xp += amount;
        int levelsGained = 0;

        while (xp >= xpToNextLevel && level < 100) {
            xp -= xpToNextLevel;
            level++;
            levelsGained++;
            xpToNextLevel = calculateXpNeeded(level + 1);
        }

        if (level >= 100) {
            xp = 0;
        }

        return levelsGained;
    }

    /**
     * Curva quadrática de XP: Level 2 = 300, Level 10 = 5500, Level 50 = 127,500, Level 100 = 505,000
     */
    public static double calculateXpNeeded(int targetLevel) {
        return 50.0 * targetLevel * targetLevel + 50.0 * targetLevel;
    }

    public synchronized double getProgressPercent() {
        return xpToNextLevel > 0 ? (xp / xpToNextLevel) * 100.0 : 0;
    }
}

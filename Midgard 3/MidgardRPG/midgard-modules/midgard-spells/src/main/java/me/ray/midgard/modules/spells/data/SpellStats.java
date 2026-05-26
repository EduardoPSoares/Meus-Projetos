package me.ray.midgard.modules.spells.data;

public class SpellStats {

    private int casts = 0;
    private int killsWithSpell = 0;
    private double totalDamage = 0;
    private long lastCastTime = 0;

    public SpellStats() {
    }

    public void incrementCasts() {
        casts++;
        lastCastTime = System.currentTimeMillis();
    }

    public void addDamage(double damage) {
        totalDamage += damage;
    }

    public void incrementKills() {
        killsWithSpell++;
    }

    public int getCasts() {
        return casts;
    }

    public int getKillsWithSpell() {
        return killsWithSpell;
    }

    public double getTotalDamage() {
        return totalDamage;
    }

    public long getLastCastTime() {
        return lastCastTime;
    }
}

package me.ray.midgard.modules.combat.mechanics;

public class DamageResult {
    private double damage;
    private boolean isCritical;
    private String damageKey;

    public DamageResult(double damage, boolean isCritical, String damageKey) {
        this.damage = damage;
        this.isCritical = isCritical;
        this.damageKey = damageKey;
    }

    public double getDamage() {
        return damage;
    }

    public boolean isCritical() {
        return isCritical;
    }

    public String getDamageKey() {
        return damageKey;
    }
}

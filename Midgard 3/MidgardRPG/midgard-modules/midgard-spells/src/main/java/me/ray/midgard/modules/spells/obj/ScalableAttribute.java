package me.ray.midgard.modules.spells.obj;

public record ScalableAttribute(double base, double perLevel) {
    
    public double calculate(int level) {
        // Level 1 uses base value. Level 2 adds perLevel once.
        return Math.max(0, base + (perLevel * Math.max(0, level - 1)));
    }

    public static ScalableAttribute of(double value) {
        return new ScalableAttribute(value, 0.0);
    }
}

package me.ray.midgard.modules.item.utils;

import java.util.concurrent.ThreadLocalRandom;

public class StatRange {
    private final double min;
    private final double max;

    public StatRange(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getRandom() {
        if (min >= max) { return min; }
        double val = ThreadLocalRandom.current().nextDouble(min, max);
        return Math.round(val * 100.0) / 100.0;
    }

    public String toString() {
        if (min == max) {
            return String.valueOf(min);
        }
        return min + " > " + max;
    }

    public static StatRange parse(String str) {
        try {
            // Support for "min -> max" and "min > max" format
            String normalized = str.replace("->", ">");
            if (normalized.contains(">")) {
                String[] parts = normalized.split(">");
                if (parts.length == 2) {
                    double val1 = Double.parseDouble(parts[0].trim());
                    double val2 = Double.parseDouble(parts[1].trim());
                    return new StatRange(Math.min(val1, val2), Math.max(val1, val2));
                }
            }

            // Support for "min-max" format (handles negative numbers correctly)
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("^(-?\\d+\\.?\\d*)\\s*-\\s*(-?\\d+\\.?\\d*)$")
                    .matcher(str.trim());
            if (matcher.matches()) {
                double val1 = Double.parseDouble(matcher.group(1));
                double val2 = Double.parseDouble(matcher.group(2));
                return new StatRange(Math.min(val1, val2), Math.max(val1, val2));
            }

            double val = Double.parseDouble(str.trim());
            return new StatRange(val, val);
        } catch (NumberFormatException e) {
            return new StatRange(0, 0);
        }
    }
}

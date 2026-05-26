package com.midgard.core.utils;

import java.text.DecimalFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Math and number utilities.
 */
public final class MathUtils {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    private MathUtils() {
    }

    public static int random(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static double random(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static boolean chance(double percent) {
        return ThreadLocalRandom.current().nextDouble(100) < percent;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static String format(double value) {
        return DECIMAL_FORMAT.format(value);
    }

    public static String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) {
            return "%02d:%02d:%02d".formatted(hours, minutes, seconds);
        }
        return "%02d:%02d".formatted(minutes, seconds);
    }

    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    public static double distance2D(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }
}

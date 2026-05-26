package me.ray.midgard.core.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class MathUtils {

    private static final Random RANDOM = new Random();
    private static final NavigableMap<Long, String> SUFFIXES = new TreeMap<>();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));

    static {
        SUFFIXES.put(1_000L, "k");
        SUFFIXES.put(1_000_000L, "M");
        SUFFIXES.put(1_000_000_000L, "B");
        SUFFIXES.put(1_000_000_000_000L, "T");
    }

    /**
     * Formats a number into RPG style (e.g., 1500 -> 1.5k).
     */
    public static String format(double value) {
        if (value < 0) { return "-" + format(-value); }
        if (value < 1000) { return DECIMAL_FORMAT.format(value); }

        java.util.Map.Entry<Long, String> e = SUFFIXES.floorEntry((long) value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        double truncated = value / (divideBy / 10);
        boolean hasDecimal = truncated < 100 && ((long) truncated % 10 != 0);
        return hasDecimal ? (truncated / 10d) + suffix : (long) (truncated / 10) + suffix;
    }

    /**
     * Returns true if the random check passes the given percentage.
     * @param chance Percentage (0-100).
     */
    public static boolean chance(double chance) {
        return RANDOM.nextDouble() * 100 < chance;
    }

    /**
     * Returns a random integer between min and max (inclusive).
     */
    public static int randomRange(int min, int max) {
        return RANDOM.nextInt((max - min) + 1) + min;
    }
    
    public static double round(double value, int places) {
        if (places < 0) { throw new IllegalArgumentException(); }
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}

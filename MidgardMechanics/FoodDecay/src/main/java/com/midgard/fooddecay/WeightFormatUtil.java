package com.midgard.fooddecay;

import static com.midgard.core.utils.MessageUtils.sc;

import java.util.Locale;

public final class WeightFormatUtil {

    private WeightFormatUtil() {
    }

    public static String formatKgDisplay(double value) {
        String formatted = String.format(Locale.US, "%.3f", value);
        while (formatted.contains(".") && (formatted.endsWith("0") || formatted.endsWith("."))) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    public static String buildLoreLine(String marker, String sizeColor, double weightKg, String sizeDisplay, int maxStack) {
        return marker + " " + sizeColor + "\u2696 " + formatKgDisplay(weightKg) + " kg"
                + " &8| " + sizeColor + sc(sizeDisplay)
                + " &8| &7max " + maxStack;
    }
}

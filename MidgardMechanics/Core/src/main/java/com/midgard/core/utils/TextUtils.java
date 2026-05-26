package com.midgard.core.utils;

import java.util.List;
import java.util.StringJoiner;

/**
 * Text and string utilities.
 */
public final class TextUtils {

    private TextUtils() {
    }

    /**
     * Create a progress bar string.
     */
    public static String progressBar(double current, double max, int bars, String filledColor, String emptyColor, String symbol) {
        if (max <= 0) return emptyColor + symbol.repeat(bars);
        int filled = (int) ((current / max) * bars);
        StringBuilder sb = new StringBuilder();
        sb.append(filledColor);
        for (int i = 0; i < bars; i++) {
            if (i == filled) sb.append(emptyColor);
            sb.append(symbol);
        }
        return MessageUtils.colorize(sb.toString());
    }

    public static String progressBar(double current, double max, int bars) {
        return progressBar(current, max, bars, "&a", "&7", "▮");
    }

    /**
     * Center a message for chat (approximate based on 80 char width).
     */
    public static String center(String text) {
        String stripped = MessageUtils.stripColor(text);
        int spaces = (80 - stripped.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spaces; i++) sb.append(' ');
        sb.append(text);
        return sb.toString();
    }

    /**
     * Join a list of strings with a separator.
     */
    public static String join(List<String> list, String separator) {
        StringJoiner joiner = new StringJoiner(separator);
        list.forEach(joiner::add);
        return joiner.toString();
    }

    /**
     * Capitalize the first letter of each word.
     */
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        String[] words = text.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1));
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    /**
     * Format an enum name to a readable title: DIAMOND_SWORD -> Diamond Sword
     */
    public static String formatEnum(Enum<?> value) {
        return capitalize(value.name().replace('_', ' '));
    }

    /**
     * Truncate a string to a max length with ellipsis.
     */
    public static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Repeat a string n times.
     */
    public static String repeat(String str, int times) {
        return str.repeat(Math.max(0, times));
    }
}

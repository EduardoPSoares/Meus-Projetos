package me.ray.midgard.bot.core.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class FormatUtil {

    private static final DecimalFormat DECIMAL = new DecimalFormat("#,##0.##");
    private static final NumberFormat NUMBER = NumberFormat.getInstance(Locale.forLanguageTag("pt-BR"));

    private FormatUtil() {}

    public static String number(long value) {
        return NUMBER.format(value);
    }

    public static String number(double value) {
        return DECIMAL.format(value);
    }

    public static String abbreviate(long value) {
        if (value < 1_000) return String.valueOf(value);
        if (value < 1_000_000) return DECIMAL.format(value / 1_000.0) + "K";
        if (value < 1_000_000_000) return DECIMAL.format(value / 1_000_000.0) + "M";
        return DECIMAL.format(value / 1_000_000_000.0) + "B";
    }

    public static String progressBar(double current, double max, int length) {
        int filled = (int) Math.round((current / max) * length);
        filled = Math.max(0, Math.min(length, filled));
        return "█".repeat(filled) + "░".repeat(length - filled);
    }

    public static String progressBarWithPercent(double current, double max, int length) {
        double percent = (current / max) * 100;
        return progressBar(current, max, length) + " " + DECIMAL.format(percent) + "%";
    }

    public static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase();
    }

    public static String pad(String text, int length) {
        if (text.length() >= length) return text;
        return text + " ".repeat(length - text.length());
    }

    public static String padLeft(String text, int length) {
        if (text.length() >= length) return text;
        return " ".repeat(length - text.length()) + text;
    }

    public static String codeBlock(String content, String language) {
        return "```" + language + "\n" + content + "\n```";
    }

    public static String codeBlock(String content) {
        return codeBlock(content, "");
    }

    public static String inlineCode(String content) {
        return "`" + content + "`";
    }

    public static String bold(String content) {
        return "**" + content + "**";
    }

    public static String italic(String content) {
        return "*" + content + "*";
    }

    public static String underline(String content) {
        return "__" + content + "__";
    }

    public static String strikethrough(String content) {
        return "~~" + content + "~~";
    }

    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}

package me.ray.midgard.bot.core.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private TimeUtil() {}

    public static String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    public static String formatDurationShort(long millis) {
        return formatDuration(Duration.ofMillis(millis));
    }

    public static String formatTimestamp(Instant instant) {
        return DATE_FORMAT.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    public static String formatDate(Instant instant) {
        return SHORT_DATE.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    /**
     * Discord timestamp format: <t:EPOCH:STYLE>
     * R = relative, F = full, f = short, D = date, T = time
     */
    public static String discordTimestamp(Instant instant, char style) {
        return "<t:" + instant.getEpochSecond() + ":" + style + ">";
    }

    public static String discordRelative(Instant instant) {
        return discordTimestamp(instant, 'R');
    }

    public static String discordFull(Instant instant) {
        return discordTimestamp(instant, 'F');
    }

    public static Duration parseDuration(String input) {
        long totalSeconds = 0;
        StringBuilder current = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                current.append(c);
            } else {
                if (current.length() == 0) continue;
                long value = Long.parseLong(current.toString());
                current.setLength(0);
                switch (Character.toLowerCase(c)) {
                    case 's' -> totalSeconds += value;
                    case 'm' -> totalSeconds += value * 60;
                    case 'h' -> totalSeconds += value * 3600;
                    case 'd' -> totalSeconds += value * 86400;
                    case 'w' -> totalSeconds += value * 604800;
                }
            }
        }
        return Duration.ofSeconds(totalSeconds);
    }
}

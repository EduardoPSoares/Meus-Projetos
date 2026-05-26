package de.maxhenkel.voicechat.util;

import de.maxhenkel.voicechat.Voicechat;

public final class MessageFormatUtil {

    private MessageFormatUtil() {
    }

    public static String blocks(long value) {
        return Voicechat.MESSAGES.format("formats.blocks", "%s blocos", value);
    }

    public static String seconds(long value) {
        return Voicechat.MESSAGES.format("formats.seconds", "%ss", value);
    }

    public static String cooldownPair(long talkSeconds, long cooldownSeconds) {
        return Voicechat.MESSAGES.format(
                "formats.cooldown_pair",
                "%s fala / %s espera",
                seconds(talkSeconds),
                seconds(cooldownSeconds)
        );
    }

    public static String cooldownCompact(long talkSeconds, long cooldownSeconds) {
        return Voicechat.MESSAGES.format(
                "formats.cooldown_compact",
                "%s/%s",
                seconds(talkSeconds),
                seconds(cooldownSeconds)
        );
    }

    public static String duration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;

        if (days > 0L) {
            return Voicechat.MESSAGES.format("formats.duration_days_hours", "%sd %sh", days, hours % 24L);
        }
        if (hours > 0L) {
            return Voicechat.MESSAGES.format("formats.duration_hours_minutes", "%sh %sm", hours, minutes % 60L);
        }
        if (minutes > 0L) {
            return Voicechat.MESSAGES.format("formats.duration_minutes_seconds", "%sm %ss", minutes, totalSeconds % 60L);
        }
        return Voicechat.MESSAGES.format("formats.duration_seconds", "%ss", totalSeconds);
    }
}

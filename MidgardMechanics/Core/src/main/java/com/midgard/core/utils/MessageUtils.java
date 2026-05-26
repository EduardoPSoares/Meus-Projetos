package com.midgard.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Messaging utilities with color code support (including hex) using Adventure API.
 */
public final class MessageUtils {

    private static String prefix = "";
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('§')
                    .hexColors()
                    .build();
    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexColors()
                    .build();

    private MessageUtils() {
    }

    public static void init(String prefix) {
        MessageUtils.prefix = colorize(prefix);
    }

    /**
     * Translate color codes (&amp; and hex &#RRGGBB) and return as legacy string.
     */
    public static String colorize(String text) {
        if (text == null) return "";
        // Hex color support: &#RRGGBB → §x§R§R§G§G§B§B
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            StringBuilder hex = new StringBuilder("§x");
            for (char c : matcher.group(1).toCharArray()) {
                hex.append("§").append(c);
            }
            matcher.appendReplacement(buffer, hex.toString());
        }
        matcher.appendTail(buffer);
        // Translate & color codes to § codes
        String raw = buffer.toString();
        return LEGACY_SERIALIZER.serialize(AMPERSAND_SERIALIZER.deserialize(raw));
    }

    /**
     * Convert text with color codes to an Adventure Component.
     */
    public static Component toComponent(String text) {
        if (text == null) return Component.empty();
        return LEGACY_SERIALIZER.deserialize(colorize(text));
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(toComponent(prefix + colorize(message)));
    }

    public static void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(toComponent(message));
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(toComponent(message));
    }

    public static void sendTitle(Player player, String title, String subtitle,
                                  int fadeIn, int stay, int fadeOut) {
        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L));
        player.showTitle(Title.title(toComponent(title), toComponent(subtitle), times));
    }

    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 70, 20);
    }

    public static void broadcast(String message) {
        org.bukkit.Bukkit.broadcast(toComponent(prefix + colorize(message)));
    }

    public static String getPrefix() {
        return prefix;
    }

    public static String stripColor(String text) {
        return PlainTextComponentSerializer.plainText().serialize(toComponent(text));
    }

    // ── Small Caps ──

    private static final String SMALL_CAPS_MAP = "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ";

    /**
     * Converts lowercase letters to Unicode small-caps, preserving color codes,
     * symbols, numbers and uppercase letters.
     */
    public static String sc(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                sb.append(c).append(text.charAt(++i));
            } else if (c >= 'a' && c <= 'z') {
                sb.append(SMALL_CAPS_MAP.charAt(c - 'a'));
            } else {
                switch (c) {
                    case 'á', 'à', 'â', 'ã' -> sb.append('ᴀ');
                    case 'é', 'ê' -> sb.append('ᴇ');
                    case 'í' -> sb.append('ɪ');
                    case 'ó', 'ô', 'õ' -> sb.append('ᴏ');
                    case 'ú' -> sb.append('ᴜ');
                    case 'ç' -> sb.append('ᴄ');
                    default -> sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}

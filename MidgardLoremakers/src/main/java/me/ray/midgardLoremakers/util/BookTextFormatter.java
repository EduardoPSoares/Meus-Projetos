package me.ray.midgardLoremakers.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class BookTextFormatter {

    private BookTextFormatter() {
    }

    public static String convertMarkdownToSectionCodes(String text) {
        return text
                .replaceAll("\\*\\*(.+?)\\*\\*", "\u00A7l$1\u00A7r")
                .replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "\u00A7o$1\u00A7r")
                .replaceAll("~~(.+?)~~", "\u00A7m$1\u00A7r")
                .replaceAll("__(.+?)__", "\u00A7n$1\u00A7r");
    }

    public static Component parseFormattedText(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }

        String converted = convertMarkdownToSectionCodes(raw);

        TextComponent.Builder builder = Component.text();
        TextColor currentColor = null;
        boolean bold = false;
        boolean italic = false;
        boolean underlined = false;
        boolean strikethrough = false;

        int i = 0;
        StringBuilder segment = new StringBuilder();

        while (i < converted.length()) {
            if (converted.charAt(i) == '\u00A7' && i + 1 < converted.length()) {
                if (!segment.isEmpty()) {
                    builder.append(Component.text(segment.toString(), buildStyle(currentColor, bold, italic, underlined, strikethrough)));
                    segment.setLength(0);
                }

                char code = Character.toLowerCase(converted.charAt(i + 1));
                TextColor mapped = mapColorCode(code);
                if (mapped != null) {
                    currentColor = mapped;
                    bold = false;
                    italic = false;
                    underlined = false;
                    strikethrough = false;
                } else {
                    switch (code) {
                        case 'l' -> bold = true;
                        case 'o' -> italic = true;
                        case 'n' -> underlined = true;
                        case 'm' -> strikethrough = true;
                        case 'r' -> {
                            currentColor = null;
                            bold = false;
                            italic = false;
                            underlined = false;
                            strikethrough = false;
                        }
                    }
                }
                i += 2;
            } else {
                segment.append(converted.charAt(i));
                i++;
            }
        }

        if (!segment.isEmpty()) {
            builder.append(Component.text(segment.toString(), buildStyle(currentColor, bold, italic, underlined, strikethrough)));
        }

        return builder.build();
    }

    private static Style buildStyle(TextColor color, boolean bold, boolean italic, boolean underlined, boolean strikethrough) {
        Style.Builder style = Style.style();
        if (color != null) style.color(color);
        if (bold) style.decoration(TextDecoration.BOLD, TextDecoration.State.TRUE);
        if (italic) style.decoration(TextDecoration.ITALIC, TextDecoration.State.TRUE);
        if (underlined) style.decoration(TextDecoration.UNDERLINED, TextDecoration.State.TRUE);
        if (strikethrough) style.decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.TRUE);
        return style.build();
    }

    private static TextColor mapColorCode(char code) {
        return switch (code) {
            case '0' -> NamedTextColor.BLACK;
            case '1' -> NamedTextColor.DARK_BLUE;
            case '2' -> NamedTextColor.DARK_GREEN;
            case '3' -> NamedTextColor.DARK_AQUA;
            case '4' -> NamedTextColor.DARK_RED;
            case '5' -> NamedTextColor.DARK_PURPLE;
            case '6' -> NamedTextColor.GOLD;
            case '7' -> NamedTextColor.GRAY;
            case '8' -> NamedTextColor.DARK_GRAY;
            case '9' -> NamedTextColor.BLUE;
            case 'a' -> NamedTextColor.GREEN;
            case 'b' -> NamedTextColor.AQUA;
            case 'c' -> NamedTextColor.RED;
            case 'd' -> NamedTextColor.LIGHT_PURPLE;
            case 'e' -> NamedTextColor.YELLOW;
            case 'f' -> NamedTextColor.WHITE;
            default -> null;
        };
    }
}

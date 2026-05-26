package me.ray.midgard.core.text;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.i18n.MessageKey;
import me.ray.midgard.core.i18n.Placeholder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utilitários para formatação e envio de mensagens.
 * <p>
 * Suporta:
 * <ul>
 *     <li>MiniMessage format</li>
 *     <li>Conversão de códigos legados</li>
 *     <li>PlaceholderAPI</li>
 *     <li>MessageKey tipado</li>
 *     <li>Placeholder tipado</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class MessageUtils {

    private static final MiniMessage MM = createMiniMessage();

    private static MiniMessage createMiniMessage() {
        try {
            if (hasNexoSupport()) {
                return MiniMessage.builder()
                        .tags(net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.builder()
                                .resolver(net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.standard())
                                // .resolver(com.nexomc.nexo.api.NexoAdventure.TAG_RESOLVER)
                                .build())
                        .build();
            }
        } catch (Throwable e) {
            // Fallback para MiniMessage padrão se Nexo não estiver disponível
        }
        return MiniMessage.miniMessage();
    }

    private static boolean hasNexoSupport() {
        try {
            return org.bukkit.Bukkit.getPluginManager().isPluginEnabled("Nexo");
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Parses a MiniMessage string into a Component.
     * @param message The string with MiniMessage tags.
     * @return The parsed Component.
     */
    public static Component parse(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        
        // Ensure standard MiniMessage format for hex colors if using legacy format &#RRGGBB
        String processed = convertLegacyColors(message);
        
        try {
            return MM.deserialize(processed);
        } catch (Exception e) {
            // Fallback to plain text if MiniMessage fails
            return Component.text(processed);
        }
    }

    public static Component parse(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        
        if (player != null) {
            message = applyPlaceholderAPI(player, message);
        }
        
        String processed = convertLegacyColors(message);
        
        try {
            return MM.deserialize(processed);
        } catch (Exception e) {
            return Component.text(processed);
        }
    }

    /**
     * Aplica PlaceholderAPI se o plugin estiver presente.
     * PlaceholderAPI é compileOnly — acesso direto à classe causa NoClassDefFoundError
     * se o plugin não estiver instalado no servidor.
     */
    private static volatile boolean papiChecked = false;
    private static volatile boolean papiAvailable = false;

    private static String applyPlaceholderAPI(Player player, String message) {
        if (!papiChecked) {
            papiChecked = true;
            papiAvailable = org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        }
        if (!papiAvailable) {
            return message;
        }
        try {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message);
        } catch (Throwable t) {
            // Fallback if PlaceholderAPI throws or class not found
            return message;
        }
    }

    public static String serialize(Component component) {
        return MM.serialize(component);
    }

    public static String center(String message) {
        return center(message, 154);
    }

    public static String center(String message, int centerPx) {
        if (message == null || message.equals("")) {
            return "";
        }
        message = convertLegacyColors(message); // Ensure colors are handled (though MM handles them too)
        // But for length calc we need to strip colors/tags.
        // Simplified legacy strip for length calc:
        // Actually, we should use a method that strips both legacy and MiniMessage tags for length calc?
        // Or just assume legacy & codes are main style here as per usage in CharacterMenu.
        
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;
        
        // Quick & Dirty legacy color strip and bold detection
        // Note: CharacterMenu uses "&" codes.
        for (char c : message.toCharArray()) {
            if (c == '§' || c == '&') {
                previousCode = true;
                continue;
            } else if (previousCode) {
                previousCode = false;
                if (c == 'l' || c == 'L') {
                   isBold = true;
                   continue;
                } else {
                   // Reset bold if color code (0-9, a-f, r)
                   // But simplified: assume any color resets format
                   isBold = false;
                }
            } else {
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = centerPx - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        sb.append(message);
        return sb.toString();
    }

    private static final java.util.Map<Character, String> LEGACY_COLOR_MAP = new java.util.HashMap<>();
    
    static {
        LEGACY_COLOR_MAP.put('0', "<black>");
        LEGACY_COLOR_MAP.put('1', "<dark_blue>");
        LEGACY_COLOR_MAP.put('2', "<dark_green>");
        LEGACY_COLOR_MAP.put('3', "<dark_aqua>");
        LEGACY_COLOR_MAP.put('4', "<dark_red>");
        LEGACY_COLOR_MAP.put('5', "<dark_purple>");
        LEGACY_COLOR_MAP.put('6', "<gold>");
        LEGACY_COLOR_MAP.put('7', "<gray>");
        LEGACY_COLOR_MAP.put('8', "<dark_gray>");
        LEGACY_COLOR_MAP.put('9', "<blue>");
        LEGACY_COLOR_MAP.put('a', "<green>");
        LEGACY_COLOR_MAP.put('b', "<aqua>");
        LEGACY_COLOR_MAP.put('c', "<red>");
        LEGACY_COLOR_MAP.put('d', "<light_purple>");
        LEGACY_COLOR_MAP.put('e', "<yellow>");
        LEGACY_COLOR_MAP.put('f', "<white>");
        LEGACY_COLOR_MAP.put('k', "<obfuscated>");
        LEGACY_COLOR_MAP.put('l', "<bold>");
        LEGACY_COLOR_MAP.put('m', "<strikethrough>");
        LEGACY_COLOR_MAP.put('n', "<underlined>");
        LEGACY_COLOR_MAP.put('o', "<italic>");
        LEGACY_COLOR_MAP.put('r', "<reset>");
    }

    private static final java.util.regex.Pattern HEX_PATTERN = java.util.regex.Pattern.compile("&#([0-9a-fA-F]{6})");

    private static String convertLegacyColors(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // Fast path: no legacy codes present
        if (message.indexOf('&') == -1 && message.indexOf('§') == -1) {
            return message;
        }
        
        // Hex support &#RRGGBB -> <#RRGGBB>
        message = HEX_PATTERN.matcher(message).replaceAll("<#$1>");
        
        // Single-pass replacement for &X and §X codes
        StringBuilder sb = new StringBuilder(message.length());
        char[] chars = message.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if ((c == '&' || c == '§') && i + 1 < chars.length) {
                char code = Character.toLowerCase(chars[i + 1]);
                String replacement = LEGACY_COLOR_MAP.get(code);
                if (replacement != null) {
                    sb.append(replacement);
                    i++; // Skip the code character
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Sends a parsed message to a CommandSender.
     * @param sender The recipient.
     * @param message The raw MiniMessage string.
     */
    public static void send(CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }

    public static void send(Player player, String message) {
        player.sendMessage(parse(player, message));
    }

    /**
     * Sends a component directly to a CommandSender.
     * @param sender The recipient.
     * @param message The component to send.
     */
    public static void send(CommandSender sender, Component message) {
        sender.sendMessage(message);
    }

    /**
     * Sends a parsed message with a prefix.
     * @param sender The recipient.
     * @param message The raw MiniMessage string.
     */
    public static void sendPrefixed(CommandSender sender, String message) {
        String prefix = MidgardCore.getLanguageManager().getRawMessage("core.prefix");
        if (prefix.startsWith("<red>Chave não encontrada")) {
             prefix = "<gradient:#5e4fa2:#f79459><bold>Midgard</bold></gradient> <dark_gray>»</dark_gray> <gray>";
        }
        if (sender instanceof Player player) {
            sender.sendMessage(parse(player, prefix + message));
        } else {
            sender.sendMessage(parse(prefix + message));
        }
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(parse(player, message));
    }
    
    // ============================================
    // MESSAGEKEY SUPPORT
    // ============================================
    
    /**
     * Envia uma mensagem por MessageKey para um jogador.
     *
     * @param player O jogador destinatário
     * @param messageKey A chave da mensagem
     */
    public static void send(Player player, MessageKey messageKey) {
        if (player == null || messageKey == null) {
            return;
        }
        player.sendMessage(MidgardCore.getLanguageManager().getMessage(messageKey));
    }
    
    /**
     * Envia uma mensagem por MessageKey com placeholders.
     *
     * @param player O jogador destinatário
     * @param messageKey A chave da mensagem
     * @param placeholders Os placeholders a substituir
     */
    public static void send(Player player, MessageKey messageKey, Placeholder... placeholders) {
        if (player == null || messageKey == null) {
            return;
        }
        player.sendMessage(MidgardCore.getLanguageManager().getMessage(messageKey, placeholders));
    }
    
    /**
     * Envia uma action bar por MessageKey.
     *
     * @param player O jogador destinatário
     * @param messageKey A chave da mensagem
     */
    public static void sendActionBar(Player player, MessageKey messageKey) {
        if (player == null || messageKey == null) {
            return;
        }
        player.sendActionBar(MidgardCore.getLanguageManager().getMessage(messageKey));
    }
    
    /**
     * Envia uma action bar por MessageKey com placeholders.
     *
     * @param player O jogador destinatário
     * @param messageKey A chave da mensagem
     * @param placeholders Os placeholders a substituir
     */
    public static void sendActionBar(Player player, MessageKey messageKey, Placeholder... placeholders) {
        if (player == null || messageKey == null) {
            return;
        }
        player.sendActionBar(MidgardCore.getLanguageManager().getMessage(messageKey, placeholders));
    }
    
    // ============================================
    // QUICK SEND METHODS
    // ============================================
    
    /**
     * Envia uma mensagem de sucesso.
     *
     * @param player O jogador destinatário
     * @param message A mensagem
     */
    public static void sendSuccess(Player player, String message) {
        String format = MidgardCore.getLanguageManager().getRawMessage("core.formats.success");
        if (format.startsWith("<red>Chave não encontrada")) {
            format = "<green>✔ <white>%message%";
        }
        player.sendMessage(parse(player, format.replace("%message%", message)));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }
    
    /**
     * Envia uma mensagem de erro.
     *
     * @param player O jogador destinatário
     * @param message A mensagem
     */
    public static void sendError(Player player, String message) {
        String format = MidgardCore.getLanguageManager().getRawMessage("core.formats.error");
        if (format.startsWith("<red>Chave não encontrada")) {
            format = "<red>✖ <gray>%message%";
        }
        player.sendMessage(parse(player, format.replace("%message%", message)));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
    }
    
    /**
     * Envia uma mensagem de aviso.
     *
     * @param player O jogador destinatário
     * @param message A mensagem
     */
    public static void sendWarning(Player player, String message) {
        String format = MidgardCore.getLanguageManager().getRawMessage("core.formats.warning");
        if (format.startsWith("<red>Chave não encontrada")) {
            format = "<gold>⚠ <gray>%message%";
        }
        player.sendMessage(parse(player, format.replace("%message%", message)));
    }
    
    /**
     * Envia uma mensagem informativa.
     *
     * @param player O jogador destinatário
     * @param message A mensagem
     */
    public static void sendInfo(Player player, String message) {
        String format = MidgardCore.getLanguageManager().getRawMessage("core.formats.info");
        if (format.startsWith("<red>Chave não encontrada")) {
            format = "<aqua>ℹ <gray>%message%";
        }
        player.sendMessage(parse(player, format.replace("%message%", message)));
    }
}

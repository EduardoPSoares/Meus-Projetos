package me.ray.midgard.modules.item.listener;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputListener implements Listener {

    private static final int MAX_INPUT_LENGTH = 2048;
    private static final Map<UUID, Consumer<String>> inputs = new ConcurrentHashMap<>();

    public ChatInputListener() {
    }

    public static void requestInput(Player player, Consumer<String> callback) {
        if (player == null || callback == null) { return; }
        inputs.put(player.getUniqueId(), callback);
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        inputs.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = inputs.remove(player.getUniqueId());
        if (callback != null) {
            String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
            String normalized = normalizeInput(rawMessage);
            
            if (normalized.equalsIgnoreCase("cancel")) {
                event.setCancelled(true);
                MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.chat.cancelled"));
                return;
            }

            event.setCancelled(true);
            if (normalized.length() > MAX_INPUT_LENGTH) {
                MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.chat.error_processing"));
                return;
            }
            String message = normalized;
            // Ensure it runs on player region thread
            me.ray.midgard.core.utils.Task.sync(player, () -> {
                try {
                    callback.accept(message);
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao processar entrada de chat para o jogador " + player.getName(), e);
                    MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.chat.error_processing"));
                }
            });
        }
    }

    private String normalizeInput(String message) {
        if (message == null) { return ""; }
        String trimmed = message.trim();
        StringBuilder sb = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c >= 32 && c != 127) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

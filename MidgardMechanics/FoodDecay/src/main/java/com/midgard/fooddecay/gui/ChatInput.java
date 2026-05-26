package com.midgard.fooddecay.gui;

import com.midgard.core.MidgardCore;
import com.midgard.core.utils.MessageUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Utility for capturing text input from players via chat.
 * Closes the player's inventory, prompts them, and listens for their next chat message.
 */
public final class ChatInput implements Listener {

    private static final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();
    private static final Map<UUID, Runnable> cancelCallbacks = new ConcurrentHashMap<>();
    private static ChatInput instance;

    private ChatInput() {}

    /**
     * Registers the chat input listener. Call once on plugin enable.
     */
    public static void register() {
        if (instance != null) return;
        instance = new ChatInput();
        Bukkit.getPluginManager().registerEvents(instance, MidgardCore.getInstance());
    }

    /**
     * Unregisters the chat input listener. Call on plugin disable.
     */
    public static void unregister() {
        if (instance == null) return;
        HandlerList.unregisterAll(instance);
        pendingInputs.clear();
        cancelCallbacks.clear();
        instance = null;
    }

    /**
     * Requests text input from a player. Closes their inventory and shows a prompt.
     * The callback runs on the main thread with the player's response.
     *
     * @param player   the player to prompt
     * @param prompt   the prompt message (supports color codes)
     * @param callback called with the player's typed message
     */
    public static void request(Player player, String prompt, Consumer<String> callback) {
        request(player, prompt, callback, null);
    }

    /**
     * Requests text input with an optional cancel callback.
     * When the player types "cancel", the onCancel runnable is executed
     * (e.g. to reopen the previous GUI).
     */
    public static void request(Player player, String prompt, Consumer<String> callback, Runnable onCancel) {
        register();
        player.closeInventory();
        player.sendMessage(MessageUtils.toComponent(prompt));
        player.sendMessage(MessageUtils.toComponent("&7Digite &ccancel &7para cancelar."));
        pendingInputs.put(player.getUniqueId(), callback);
        if (onCancel != null) {
            cancelCallbacks.put(player.getUniqueId(), onCancel);
        } else {
            cancelCallbacks.remove(player.getUniqueId());
        }
    }

    /**
     * Returns true if the player has a pending chat input request.
     */
    public static boolean hasPending(Player player) {
        return pendingInputs.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Consumer<String> callback = pendingInputs.remove(uuid);
        if (callback == null) return;

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Run callback on main thread
        Bukkit.getScheduler().runTask(MidgardCore.getInstance(), () -> {
            if ("cancel".equalsIgnoreCase(message.trim())) {
                Runnable onCancel = cancelCallbacks.remove(uuid);
                event.getPlayer().sendMessage(MessageUtils.toComponent("&cCancelado."));
                if (onCancel != null) {
                    onCancel.run();
                }
                return;
            }
            cancelCallbacks.remove(uuid);
            callback.accept(message);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pendingInputs.remove(uuid);
        cancelCallbacks.remove(uuid);
    }
}

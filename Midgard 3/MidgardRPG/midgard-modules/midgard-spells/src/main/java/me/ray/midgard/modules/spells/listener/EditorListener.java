package me.ray.midgard.modules.spells.listener;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;


public class EditorListener implements Listener {

    private final Map<UUID, Consumer<String>> pendingInputs = new java.util.concurrent.ConcurrentHashMap<>();

    public EditorListener() {
    }

    public void requestInput(Player player, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), callback);
    }
    
    public void cancelInput(Player player) {
        pendingInputs.remove(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (pendingInputs.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            Consumer<String> callback = pendingInputs.remove(player.getUniqueId());
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            
            // Run on player region thread
            me.ray.midgard.core.utils.Task.sync(player, () -> callback.accept(message));
        }
    }
}

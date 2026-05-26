package midgardvanish.listener;

import com.nickuc.chat.api.events.PublicMessageEvent;
import midgardvanish.manager.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class VanishChatListener implements Listener {

    private final VanishManager vanishManager;

    public VanishChatListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @EventHandler
    public void onPublicMessage(PublicMessageEvent event) {
        if (event.isCancelled()) return;

        Player sender = event.getSender();
        if (!vanishManager.isVanished(sender)) return;

        // Cancel the nChat message so it doesn't go to regular chat
        event.setCancelled(true);

        String message = event.getMessage();
        String formatted = "§7[§cᴠᴀɴɪsʜ§7] §f" + sender.getName() + "§7: §f" + message;

        // Send to all vanished players (no distance limit)
        for (UUID uuid : vanishManager.getVanishedPlayers()) {
            Player vanished = Bukkit.getPlayer(uuid);
            if (vanished != null && vanished.isOnline()) {
                vanished.sendMessage(formatted);
            }
        }

        // Log to console
        Bukkit.getLogger().info("[VANISH CHAT] " + sender.getName() + ": " + message);
    }
}

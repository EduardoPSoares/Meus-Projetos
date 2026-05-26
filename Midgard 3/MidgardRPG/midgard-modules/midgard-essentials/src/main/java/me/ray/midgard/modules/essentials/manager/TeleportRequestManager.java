package me.ray.midgard.modules.essentials.manager;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.core.utils.TeleportUtils;
import me.ray.midgard.modules.essentials.config.EssentialsConfig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportRequestManager {

    private final JavaPlugin plugin;
    // private final EssentialsConfig config;
    private final EssentialsManager essentialsManager;
    // Target UUID -> Sender UUID
    private final Map<UUID, UUID> requests = new ConcurrentHashMap<>();

    public TeleportRequestManager(JavaPlugin plugin, EssentialsConfig config, EssentialsManager essentialsManager) {
        this.plugin = plugin;
        // this.config = config;
        this.essentialsManager = essentialsManager;
    }

    public void sendRequest(Player sender, Player target) {
        if (requests.containsKey(target.getUniqueId()) && requests.get(target.getUniqueId()).equals(sender.getUniqueId())) {
            MessageUtils.send(sender, essentialsManager.getMessage("tpa.already_sent"));
            return;
        }

        requests.put(target.getUniqueId(), sender.getUniqueId());
        
        // Expire after 60 seconds
        Task.syncLater(sender, () -> {
            if (requests.get(target.getUniqueId()) != null && requests.get(target.getUniqueId()).equals(sender.getUniqueId())) {
                requests.remove(target.getUniqueId());
                if (sender.isOnline()) {
                    MessageUtils.send(sender, essentialsManager.getMessage("tpa.expired"));
                }
            }
        }, 20L * 60);

        MessageUtils.send(sender, essentialsManager.getMessage("tpa.sent").replace("%player%", target.getName()));
        MessageUtils.send(target, essentialsManager.getMessage("tpa.received").replace("%player%", sender.getName()));
    }

    public void acceptRequest(Player target) {
        UUID senderId = requests.remove(target.getUniqueId());
        if (senderId == null) {
            MessageUtils.send(target, essentialsManager.getMessage("tpa.no_request"));
            return;
        }

        Player sender = plugin.getServer().getPlayer(senderId);
        if (sender != null && sender.isOnline()) {
            if (!TeleportUtils.isSafeLocation(target.getLocation())) {
                MessageUtils.send(target, essentialsManager.getMessage("tpa.unsafe_location_target"));
                MessageUtils.send(sender, essentialsManager.getMessage("tpa.unsafe_location_sender"));
                return;
            }

            TeleportUtils.teleport(sender, target.getLocation());
            MessageUtils.send(sender, essentialsManager.getMessage("tpa.teleporting"));
            MessageUtils.send(target, essentialsManager.getMessage("tpa.accepted"));
        } else {
            MessageUtils.send(target, essentialsManager.getMessage("tpa.sender_offline"));
        }
    }

    public void denyRequest(Player target) {
        UUID senderId = requests.remove(target.getUniqueId());
        if (senderId == null) {
            MessageUtils.send(target, essentialsManager.getMessage("tpa.no_request"));
            return;
        }

        Player sender = plugin.getServer().getPlayer(senderId);
        if (sender != null && sender.isOnline()) {
            MessageUtils.send(sender, essentialsManager.getMessage("tpa.denied"));
        }
        MessageUtils.send(target, essentialsManager.getMessage("tpa.denied"));
    }

    /**
     * Limpa dados de um jogador ao sair do servidor.
     *
     * @param uuid UUID do jogador.
     */
    public void cleanupPlayer(UUID uuid) {
        requests.remove(uuid);
        // Also remove any requests where this player was the sender
        requests.values().removeIf(senderId -> senderId.equals(uuid));
    }
}

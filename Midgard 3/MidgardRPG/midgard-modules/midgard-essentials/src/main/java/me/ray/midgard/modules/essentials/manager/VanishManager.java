package me.ray.midgard.modules.essentials.manager;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.essentials.config.EssentialsConfig;
import me.ray.midgard.modules.essentials.data.EssentialsData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishManager {

    private final JavaPlugin plugin;
    private final EssentialsManager essentialsManager;
    private final Set<UUID> vanishedPlayers;

    public VanishManager(JavaPlugin plugin, EssentialsConfig config, EssentialsManager essentialsManager) {
        this.plugin = plugin;
        this.essentialsManager = essentialsManager;
        this.vanishedPlayers = ConcurrentHashMap.newKeySet();
    }

    public void toggleVanish(Player player) {
        if (isVanished(player)) {
            unvanish(player);
        } else {
            vanish(player);
        }
    }

    public void vanish(Player player) {
        vanish(player, false);
    }

    public void vanish(Player player, boolean silent) {
        vanishedPlayers.add(player.getUniqueId());
        
        // Persistência
        updatePersistence(player, true);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.hasPermission("midgard.vanish.see")) {
                onlinePlayer.hidePlayer(plugin, player);
            }
        }
        if (!silent) {
            MessageUtils.send(player, essentialsManager.getMessage("vanish.enabled"));
        }
    }

    public void unvanish(Player player) {
        vanishedPlayers.remove(player.getUniqueId());
        
        // Persistência
        updatePersistence(player, false);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.showPlayer(plugin, player);
        }
        MessageUtils.send(player, essentialsManager.getMessage("vanish.disabled"));
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }
    
    public void updateFor(Player player) {
        if (!player.hasPermission("midgard.vanish.see")) {
            for (UUID uuid : vanishedPlayers) {
                Player vanishedPlayer = Bukkit.getPlayer(uuid);
                if (vanishedPlayer != null) {
                    player.hidePlayer(plugin, vanishedPlayer);
                }
            }
        }
    }
    
    public void removePlayer(Player player) {
        vanishedPlayers.remove(player.getUniqueId());
    }

    public void loadVanishState(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile != null) {
            EssentialsData data = profile.getOrCreateData(EssentialsData.class);
            if (data.isVanished()) {
                vanishedPlayers.add(player.getUniqueId());
                // Aplicar vanish visualmente (sem mensagem)
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!onlinePlayer.hasPermission("midgard.vanish.see")) {
                        onlinePlayer.hidePlayer(plugin, player);
                    }
                }
                MessageUtils.send(player, essentialsManager.getMessage("vanish.enabled"));
            }
        }
    }

    private void updatePersistence(Player player, boolean state) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile != null) {
            EssentialsData data = profile.getOrCreateData(EssentialsData.class);
            data.setVanished(state);
            // ProfileManager auto-saves periodically and on quit, so we don't need to force save here
            // unless we want instant safety against crashes. 
            // Given it's an admin feature, relying on auto-save/quit save is usually fine.
        }
    }
}

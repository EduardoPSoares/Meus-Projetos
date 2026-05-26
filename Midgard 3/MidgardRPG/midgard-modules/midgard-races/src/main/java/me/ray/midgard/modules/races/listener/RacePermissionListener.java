package me.ray.midgard.modules.races.listener;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.event.PlayerChangeRaceEvent;
import me.ray.midgard.modules.races.model.Race;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;

import java.util.Map;
import java.util.UUID;

public class RacePermissionListener implements Listener {

    private final RacesModule module;
    private final Map<UUID, PermissionAttachment> attachments = new java.util.concurrent.ConcurrentHashMap<>();

    public RacePermissionListener(RacesModule module) {
        this.module = module;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            updatePermissions(event.getPlayer());
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao aplicar permissões de raça para %s", event.getPlayer().getName(), e);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            UUID uuid = event.getPlayer().getUniqueId();
            PermissionAttachment attachment = attachments.remove(uuid);
            if (attachment != null) {
                try {
                    event.getPlayer().removeAttachment(attachment);
                } catch (IllegalArgumentException ignored) {
                    // Attachment already removed or invalid
                }
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao remover permissões de raça para %s", event.getPlayer().getName(), e);
        }
    }

    @EventHandler
    public void onChangeRace(PlayerChangeRaceEvent event) {
        try {
            updatePermissions(event.getPlayer(), event.getNewRace());
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao atualizar permissões na mudança de raça para %s", event.getPlayer().getName(), e);
        }
    }

    private void updatePermissions(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return; }

        RaceData data = profile.getData(RaceData.class);
        if (data != null && data.hasRace()) {
            Race race = module.getRaceManager().getRace(data.getRaceId());
            if (race != null) {
                updatePermissions(player, race);
            }
        }
    }

    private void updatePermissions(Player player, Race race) {
        UUID uuid = player.getUniqueId();
        
        // Remove old attachment
        PermissionAttachment oldAttachment = attachments.remove(uuid);
        if (oldAttachment != null) {
            try {
                player.removeAttachment(oldAttachment);
            } catch (IllegalArgumentException ignored) { /* Attachment pode já ter sido removido */ }
        }

        // If no permissions to add, stop here
        if (race == null || race.getPermissions() == null || race.getPermissions().isEmpty()) {
            return;
        }

        // Add new attachment
        PermissionAttachment newAttachment = player.addAttachment(module.getPlugin());
        for (String perm : race.getPermissions()) {
            newAttachment.setPermission(perm, true);
        }
        attachments.put(uuid, newAttachment);
        
        MidgardLogger.debug("Permissões de raça atualizadas para %s: %d permissões aplicadas.", player.getName(), race.getPermissions().size());
    }
}

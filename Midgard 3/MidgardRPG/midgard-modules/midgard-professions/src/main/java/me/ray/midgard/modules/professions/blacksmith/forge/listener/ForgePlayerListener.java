package me.ray.midgard.modules.professions.blacksmith.forge.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeManager;
import me.ray.midgard.modules.professions.blacksmith.forge.ghost.GhostBlockManager;
import me.ray.midgard.modules.professions.blacksmith.forge.session.ForgeSessionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player disconnection during forge sessions and recipe search chat input.
 * Cleans up active forging sessions, build sessions, and HUD elements.
 */
public class ForgePlayerListener implements Listener {

    private final ForgeSessionManager sessionManager;
    private final GhostBlockManager ghostBlockManager;
    private final ForgeManager forgeManager;

    public ForgePlayerListener(ForgeSessionManager sessionManager, GhostBlockManager ghostBlockManager) {
        this(sessionManager, ghostBlockManager, null);
    }

    public ForgePlayerListener(ForgeSessionManager sessionManager, GhostBlockManager ghostBlockManager, ForgeManager forgeManager) {
        this.sessionManager = sessionManager;
        this.ghostBlockManager = ghostBlockManager;
        this.forgeManager = forgeManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var playerId = event.getPlayer().getUniqueId();

        // Cancel any active forge session (forging mini-games)
        if (sessionManager.hasActiveSession(playerId)) {
            sessionManager.cancelSession(playerId);
        }

        // Cancel any build session (cleans up ghost blocks + BossBar HUD)
        if (ghostBlockManager.hasSession(playerId)) {
            ghostBlockManager.cancelSession(playerId);
        }

        // Unload player data cache and save to DB
        if (forgeManager != null) {
            forgeManager.unloadPlayerData(playerId);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (forgeManager != null) {
            forgeManager.onChatForRecipeSearch(event);
        }
    }
}

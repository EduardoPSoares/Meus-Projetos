package me.ray.midgard.modules.professions.blacksmith.forge.minigame;

import me.ray.midgard.modules.professions.blacksmith.forge.session.ForgeSession;
import org.bukkit.entity.Player;

/**
 * Base interface for all forge mini-games.
 */
public interface ForgeMinigame {

    /**
     * Starts the mini-game for the given player and session.
     */
    void start(Player player, ForgeSession session);

    /**
     * Called when the player performs an action (click, etc.).
     */
    void onAction(Player player, ForgeSession session, int slot);

    /**
     * Called every tick to update the mini-game state.
     */
    void tick(Player player, ForgeSession session);

    /**
     * Stops the mini-game and returns the score (0.0 - 1.0).
     */
    double stop(Player player, ForgeSession session);

    /**
     * Whether the mini-game is currently active.
     */
    boolean isActive();

    /**
     * Gets the mini-game type identifier.
     */
    String getType();
}

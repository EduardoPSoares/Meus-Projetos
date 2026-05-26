package me.ray.midgard.modules.professions.blacksmith.forge.event;

import me.ray.midgard.modules.professions.blacksmith.forge.recipe.ForgeRecipe;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Evento disparado quando um jogador inicia o processo de forjamento.
 * Cancelável — se cancelado, materiais não são consumidos e a sessão não começa.
 */
public class ForgeStartEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final ForgeStructure forge;
    private final ForgeRecipe recipe;
    private boolean cancelled;

    public ForgeStartEvent(Player player, ForgeStructure forge, ForgeRecipe recipe) {
        this.player = player;
        this.forge = forge;
        this.recipe = recipe;
    }

    public Player getPlayer() { return player; }
    public ForgeStructure getForge() { return forge; }
    public ForgeRecipe getRecipe() { return recipe; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }
}

package me.ray.midgard.modules.professions.blacksmith.forge.event;

import me.ray.midgard.modules.professions.blacksmith.forge.quality.QualityTier;
import me.ray.midgard.modules.professions.blacksmith.forge.recipe.ForgeRecipe;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Evento disparado quando um jogador completa o processo de forjamento.
 */
public class ForgeCompleteEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final ForgeStructure forge;
    private final ForgeRecipe recipe;
    private final ItemStack result;
    private final QualityTier qualityTier;
    private final double qualityScore;
    private final double xpGained;

    public ForgeCompleteEvent(Player player, ForgeStructure forge, ForgeRecipe recipe,
                              ItemStack result, QualityTier qualityTier, double qualityScore, double xpGained) {
        this.player = player;
        this.forge = forge;
        this.recipe = recipe;
        this.result = result;
        this.qualityTier = qualityTier;
        this.qualityScore = qualityScore;
        this.xpGained = xpGained;
    }

    public Player getPlayer() { return player; }
    public ForgeStructure getForge() { return forge; }
    public ForgeRecipe getRecipe() { return recipe; }
    public ItemStack getResult() { return result; }
    public QualityTier getQualityTier() { return qualityTier; }
    public double getQualityScore() { return qualityScore; }
    public double getXpGained() { return xpGained; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }
}

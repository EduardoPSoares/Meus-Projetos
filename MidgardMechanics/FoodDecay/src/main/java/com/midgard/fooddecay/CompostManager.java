package com.midgard.fooddecay;

import com.midgard.core.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Manages composting of spoiled food in vanilla composter blocks.
 * Right-click a composter with expired food to fill it.
 * When full, collect fertilizer (bone meal by default).
 */
public class CompostManager {

    private final FoodDecayConfig config;
    private final FoodDecayManager manager;

    public CompostManager(FoodDecayConfig config, FoodDecayManager manager) {
        this.config = config;
        this.manager = manager;
    }

    /**
     * Handles a player right-clicking a composter block.
     * Returns true if the interaction was consumed.
     */
    public boolean onComposterInteract(Player player, Block block, ItemStack handItem) {
        if (!config.isCompostingEnabled()) return false;
        if (block.getType() != Material.COMPOSTER) return false;
        if (!(block.getBlockData() instanceof Levelled levelled)) return false;

        int level = levelled.getLevel();
        int maxLevel = levelled.getMaximumLevel(); // 8 for composter

        // Level 8 = ready to collect
        if (level >= maxLevel) {
            return collectCompost(player, block, levelled);
        }

        // Try to add spoiled food
        if (handItem == null || handItem.getType().isAir()) return false;
        if (!handItem.getType().isEdible()) return false;
        if (!manager.isExpired(handItem)) {
            // Only allow spoiled food — optionally also partially decayed food
            if (!config.isCompostPartialDecay()) return false;
            double freshness = manager.getFreshness(handItem);
            if (freshness > config.getCompostMinDecay()) return false;
        }

        // Consume 1 item from hand
        handItem.setAmount(handItem.getAmount() - 1);

        // Increment composter level
        levelled.setLevel(level + 1);
        block.setBlockData(levelled);

        // Effects
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_COMPOSTER_FILL_SUCCESS, 1f, 1f);
        block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                block.getLocation().add(0.5, 1.0, 0.5), 6, 0.25, 0.1, 0.25, 0);

        String msg = config.msg("compost-added");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(MessageUtils.toComponent(msg));
        }

        // If this fill made it full (new level reached maxLevel)
        if (level + 1 >= maxLevel) {
            levelled.setLevel(maxLevel);
            block.setBlockData(levelled);
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_COMPOSTER_READY, 1f, 1f);
        }

        return true;
    }

    /**
     * Collects compost result from a full composter.
     */
    private boolean collectCompost(Player player, Block block, Levelled levelled) {
        Material resultMat;
        try {
            resultMat = Material.valueOf(config.getCompostResultMaterial());
        } catch (IllegalArgumentException e) {
            resultMat = Material.BONE_MEAL;
        }

        ItemStack result = new ItemStack(resultMat, config.getCompostResultAmount());

        // Drop at player or add to inventory
        var leftover = player.getInventory().addItem(result);
        for (ItemStack overflow : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }

        // Reset composter
        levelled.setLevel(0);
        block.setBlockData(levelled);

        // Effects
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_COMPOSTER_EMPTY, 1f, 1f);
        block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                block.getLocation().add(0.5, 1.0, 0.5), 10, 0.3, 0.2, 0.3, 0);

        String msg = config.msg("compost-collected");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(MessageUtils.toComponent(msg));
        }

        return true;
    }
}

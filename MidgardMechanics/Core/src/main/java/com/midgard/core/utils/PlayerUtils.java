package com.midgard.core.utils;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Player-related utilities.
 */
public final class PlayerUtils {

    private PlayerUtils() {
    }

    /**
     * Reset a player to default state (health, food, inventory, effects, gamemode).
     */
    public static void reset(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) player.setHealth(maxHealth.getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExp(0f);
        player.setLevel(0);
        player.setFireTicks(0);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setGameMode(GameMode.SURVIVAL);
        clearEffects(player);
    }

    /**
     * Clear all potion effects from a player.
     */
    public static void clearEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    /**
     * Apply a potion effect to a player.
     */
    public static void addEffect(Player player, PotionEffectType type, int durationTicks, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, false));
    }

    /**
     * Check if a player's inventory is full.
     */
    public static boolean isInventoryFull(Player player) {
        return player.getInventory().firstEmpty() == -1;
    }

    /**
     * Give an item to a player, dropping on ground if inventory is full.
     */
    public static void giveItem(Player player, ItemStack item) {
        if (isInventoryFull(player)) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        } else {
            player.getInventory().addItem(item);
        }
    }

    /**
     * Heal a player by an amount.
     */
    public static void heal(Player player, double amount) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        double max = attr.getValue();
        double newHealth = Math.min(player.getHealth() + amount, max);
        player.setHealth(newHealth);
    }

    /**
     * Feed a player by an amount.
     */
    public static void feed(Player player, int amount) {
        player.setFoodLevel(Math.min(player.getFoodLevel() + amount, 20));
    }
}

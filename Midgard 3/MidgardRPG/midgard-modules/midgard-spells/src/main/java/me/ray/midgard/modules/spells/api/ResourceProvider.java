package me.ray.midgard.modules.spells.api;

import org.bukkit.entity.Player;

public interface ResourceProvider {
    double getMana(Player player);
    double getStamina(Player player);
    boolean consumeMana(Player player, double amount);
    boolean consumeStamina(Player player, double amount);
}

package me.ray.midgard.modules.spells.requirement;

import org.bukkit.entity.Player;

public interface SpellRequirement {
    boolean check(Player player);
    String getFailureMessage();
}

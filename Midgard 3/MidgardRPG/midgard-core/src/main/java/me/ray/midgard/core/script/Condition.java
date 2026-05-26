package me.ray.midgard.core.script;

import org.bukkit.entity.Player;

public interface Condition {
    boolean check(Player player);
}

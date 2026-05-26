package me.ray.midgard.core.script.conditions;

import me.ray.midgard.core.script.Condition;
import org.bukkit.entity.Player;

public class PermissionCondition implements Condition {

    private final String permission;

    public PermissionCondition(String permission) {
        this.permission = permission;
    }

    @Override
    public boolean check(Player player) {
        return player.hasPermission(permission);
    }
}

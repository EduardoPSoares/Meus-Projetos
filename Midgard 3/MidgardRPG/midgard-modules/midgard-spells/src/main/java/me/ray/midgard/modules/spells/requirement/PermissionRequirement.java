package me.ray.midgard.modules.spells.requirement;

import org.bukkit.entity.Player;

public class PermissionRequirement implements SpellRequirement {

    private final String permission;

    public PermissionRequirement(String permission) {
        this.permission = permission;
    }

    @Override
    public boolean check(Player player) {
        return player.hasPermission(permission);
    }

    @Override
    public String getFailureMessage() {
        String raw = me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("spells.requirements.permission_needed");
        return raw.replace("%permission%", permission);
    }
}

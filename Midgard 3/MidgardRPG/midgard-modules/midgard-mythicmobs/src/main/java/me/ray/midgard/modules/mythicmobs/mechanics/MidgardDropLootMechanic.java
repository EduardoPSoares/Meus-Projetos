package me.ray.midgard.modules.mythicmobs.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.loot.LootContext;
import me.ray.midgard.core.loot.LootTable;
import me.ray.midgard.core.profile.MidgardProfile;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MidgardDropLootMechanic implements ITargetedEntitySkill {

    private final String tableId;

    public MidgardDropLootMechanic(MythicLineConfig config) {
        this.tableId = config.getString(new String[]{"id", "table", "t"}, "");
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        LootTable table = MidgardCore.getLootManager().getTable(tableId);
        if (table == null) {
            return SkillResult.CONDITION_FAILED;
        }
        
        Player killer = null;
        if (data.getCaster().getEntity().isPlayer()) {
            killer = (Player) BukkitAdapter.adapt(data.getCaster().getEntity());
        }
        
        // If caster is the mob (onDeath), we need the killer (trigger).
        if (data.getTrigger().isPlayer()) {
            killer = (Player) BukkitAdapter.adapt(data.getTrigger());
        }
        
        MidgardProfile profile = null;
        if (killer != null) {
            profile = MidgardCore.getProfileManager().getProfile(killer.getUniqueId());
        }
        
        Location loc = BukkitAdapter.adapt(target.getLocation());
        LootContext context = new LootContext(profile, loc, 1.0);
        List<ItemStack> items = MidgardCore.getLootManager().rollLoot(table, context);
        
        me.ray.midgard.core.utils.Task.sync(loc, () -> {
            for (ItemStack item : items) {
                loc.getWorld().dropItemNaturally(loc, item);
            }
        });
        
        return SkillResult.SUCCESS;
    }
}

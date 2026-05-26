package me.ray.midgard.modules.mythicmobs.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MidgardLevelScaleMechanic implements ITargetedEntitySkill {

    private final double radius;
    private final String mode; // AVERAGE, HIGHEST, LOWEST, NEAREST
    private final int add;
    private final double multiplier;

    public MidgardLevelScaleMechanic(MythicLineConfig config) {
        this.radius = config.getDouble(new String[]{"radius", "r"}, 30.0);
        this.mode = config.getString(new String[]{"mode", "m"}, "AVERAGE").toUpperCase();
        this.add = config.getInteger(new String[]{"add", "a"}, 0);
        this.multiplier = config.getDouble(new String[]{"multiplier", "mult"}, 1.0);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        if (!MythicBukkit.inst().getMobManager().isActiveMob(target)) {
            return SkillResult.INVALID_TARGET;
        }
        
        // Run on main thread because getNearbyEntities requires it
        Entity bukkitEntity = BukkitAdapter.adapt(target);
        me.ray.midgard.core.utils.Task.sync(bukkitEntity, () -> {
            ActiveMob mob = MythicBukkit.inst().getMobManager().getActiveMob(target.getUniqueId()).orElse(null);
            if (mob == null) {
                return;
            }
            
            List<Player> players = bukkitEntity.getNearbyEntities(radius, radius, radius).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .collect(Collectors.toList());
            
            if (players.isEmpty()) {
                return;
            }
            
            int targetLevel = 1;
            
            switch (mode) {
                case "AVERAGE":
                    int sum = 0;
                    int count = 0;
                    for (Player p : players) {
                        int lvl = getLevel(p);
                        if (lvl > 0) {
                            sum += lvl;
                            count++;
                        }
                    }
                    if (count > 0) {
                        targetLevel = sum / count;
                    }
                    break;
                    
                case "HIGHEST":
                    targetLevel = players.stream()
                            .mapToInt(this::getLevel)
                            .max()
                            .orElse(1);
                    break;
                    
                case "LOWEST":
                    targetLevel = players.stream()
                            .mapToInt(this::getLevel)
                            .min()
                            .orElse(1);
                    break;
                    
                case "NEAREST":
                    Player nearest = players.stream()
                            .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(bukkitEntity.getLocation())))
                            .orElse(null);
                    if (nearest != null) {
                        targetLevel = getLevel(nearest);
                    }
                    break;
            }
            
            // Apply modifiers
            int finalLevel = (int) ((targetLevel * multiplier) + add);
            if (finalLevel < 1) {
                finalLevel = 1;
            }
            
            mob.setLevel(finalLevel);
        });
        
        return SkillResult.SUCCESS;
    }
    
    private int getLevel(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return 1;
        }
        CombatData data = profile.getOrCreateData(CombatData.class);
        return data.getLevel();
    }
}

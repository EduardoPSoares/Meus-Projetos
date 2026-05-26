package me.ray.midgard.core.utils;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class RayTraceUtils {

    /**
     * Raytraces for a living entity in the player's line of sight.
     */
    @Nullable
    public static LivingEntity rayTraceEntity(Player player, double range) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        RayTraceResult result = player.getWorld().rayTrace(
                eyeLoc,
                direction,
                range,
                FluidCollisionMode.NEVER,
                true,
                0.5, // Ray size
                entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            return (LivingEntity) result.getHitEntity();
        }
        return null;
    }

    /**
     * Raytraces for a block or entity.
     */
    @Nullable
    public static RayTraceResult rayTrace(Location start, Vector direction, double range, Predicate<Entity> filter) {
        return start.getWorld().rayTrace(
                start,
                direction,
                range,
                FluidCollisionMode.NEVER,
                true,
                0.1,
                filter
        );
    }
}

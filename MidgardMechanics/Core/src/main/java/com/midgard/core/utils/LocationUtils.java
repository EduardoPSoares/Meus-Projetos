package com.midgard.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Location and spatial utilities.
 */
public final class LocationUtils {

    private LocationUtils() {
    }

    /**
     * Serialize location to string: world,x,y,z,yaw,pitch
     */
    public static String serialize(Location loc) {
        if (loc == null || loc.getWorld() == null) return "null";
        return loc.getWorld().getName() + "," +
                loc.getX() + "," +
                loc.getY() + "," +
                loc.getZ() + "," +
                loc.getYaw() + "," +
                loc.getPitch();
    }

    /**
     * Deserialize location from string.
     */
    public static Location deserialize(String str) {
        if (str == null || str.equals("null")) return null;
        String[] parts = str.split(",");
        if (parts.length < 4) return null;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Center a location on the block (0.5, 0, 0.5 offset).
     */
    public static Location center(Location loc) {
        return new Location(loc.getWorld(),
                loc.getBlockX() + 0.5,
                loc.getY(),
                loc.getBlockZ() + 0.5,
                loc.getYaw(),
                loc.getPitch());
    }

    /**
     * Get all blocks within a radius from a center location.
     */
    public static List<Block> getBlocksInRadius(Location center, int radius) {
        List<Block> blocks = new ArrayList<>();
        if (center.getWorld() == null) return blocks;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location loc = center.clone().add(x, y, z);
                    if (loc.distanceSquared(center) <= radius * radius) {
                        blocks.add(loc.getBlock());
                    }
                }
            }
        }
        return blocks;
    }

    /**
     * Get all entities within a radius from a location.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Entity> List<T> getNearbyEntities(Location loc, double radius, Class<T> type) {
        List<T> entities = new ArrayList<>();
        if (loc.getWorld() == null) return entities;

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (type.isInstance(entity)) {
                entities.add((T) entity);
            }
        }
        return entities;
    }

    /**
     * Get a direction vector between two locations.
     */
    public static Vector getDirection(Location from, Location to) {
        return to.toVector().subtract(from.toVector()).normalize();
    }

    /**
     * Check if two locations are in the same block.
     */
    public static boolean isSameBlock(Location a, Location b) {
        return a.getBlockX() == b.getBlockX() &&
                a.getBlockY() == b.getBlockY() &&
                a.getBlockZ() == b.getBlockZ() &&
                a.getWorld() == b.getWorld();
    }
}

package me.ray.midgard.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Cuboid implements Iterable<Block> {

    private final String worldName;
    private final int xMin, xMax;
    private final int yMin, yMax;
    private final int zMin, zMax;

    public Cuboid(Location point1, Location point2) {
        if (!point1.getWorld().equals(point2.getWorld())) {
            throw new IllegalArgumentException("Locations must be in the same world");
        }
        this.worldName = point1.getWorld().getName();
        this.xMin = Math.min(point1.getBlockX(), point2.getBlockX());
        this.xMax = Math.max(point1.getBlockX(), point2.getBlockX());
        this.yMin = Math.min(point1.getBlockY(), point2.getBlockY());
        this.yMax = Math.max(point1.getBlockY(), point2.getBlockY());
        this.zMin = Math.min(point1.getBlockZ(), point2.getBlockZ());
        this.zMax = Math.max(point1.getBlockZ(), point2.getBlockZ());
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(worldName)) {
            return false;
        }
        return loc.getBlockX() >= xMin && loc.getBlockX() <= xMax &&
               loc.getBlockY() >= yMin && loc.getBlockY() <= yMax &&
               loc.getBlockZ() >= zMin && loc.getBlockZ() <= zMax;
    }

    public Location getCenter() {
        World world = Bukkit.getWorld(worldName);
        return new Location(world, 
            (xMin + xMax) / 2.0 + 0.5, 
            (yMin + yMax) / 2.0 + 0.5, 
            (zMin + zMax) / 2.0 + 0.5
        );
    }

    public List<Block> getBlocks() {
        List<Block> blocks = new ArrayList<>();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return blocks;
        }

        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = zMin; z <= zMax; z++) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

    @Override
    public Iterator<Block> iterator() {
        return getBlocks().iterator();
    }
}

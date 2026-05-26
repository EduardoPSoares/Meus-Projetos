package me.ray.midgard.modules.professions.blacksmith.forge.structure;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeRotation;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a constructed and activated forge in the world.
 * Tracks the forge's location, owner, tier, and usage statistics.
 */
public class ForgeStructure {

    private final UUID forgeId;
    private final UUID ownerUuid;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final ForgeTier tier;
    private final ForgeRotation rotation;
    private final long createdAt;

    private String name;
    private long lastUsed;
    private int totalItemsForged;
    private boolean active;

    // Cache of interactive block world locations
    private transient Map<ForgeBlock.ForgeBlockType, Location> interactiveLocations;

    // Fuel zone world locations (invisible area for fuel item drops)
    private transient List<Location> fuelZoneLocations;

    public ForgeStructure(UUID forgeId, UUID ownerUuid, String worldName,
                          int x, int y, int z, ForgeTier tier, ForgeRotation rotation) {
        this.forgeId = forgeId;
        this.ownerUuid = ownerUuid;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tier = tier;
        this.rotation = rotation;
        this.createdAt = System.currentTimeMillis();
        this.lastUsed = createdAt;
        this.totalItemsForged = 0;
        this.active = true;
        this.name = tier.getName();
    }

    // Full constructor for loading from DB
    public ForgeStructure(UUID forgeId, UUID ownerUuid, String worldName,
                          int x, int y, int z, ForgeTier tier, ForgeRotation rotation,
                          long createdAt, long lastUsed, int totalItemsForged, boolean active,
                          String name) {
        this.forgeId = forgeId;
        this.ownerUuid = ownerUuid;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tier = tier;
        this.rotation = rotation;
        this.createdAt = createdAt;
        this.lastUsed = lastUsed;
        this.totalItemsForged = totalItemsForged;
        this.active = active;
        this.name = name != null ? name : tier.getName();
    }

    public UUID getForgeId() { return forgeId; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getWorldName() { return worldName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public ForgeTier getTier() { return tier; }
    public ForgeRotation getRotation() { return rotation; }
    public long getCreatedAt() { return createdAt; }
    public long getLastUsed() { return lastUsed; }
    public int getTotalItemsForged() { return totalItemsForged; }
    public boolean isActive() { return active; }
    public String getName() { return name; }

    public void setLastUsed(long lastUsed) { this.lastUsed = lastUsed; }
    public void setActive(boolean active) { this.active = active; }
    public void incrementItemsForged() { this.totalItemsForged++; }
    public void setName(String name) { this.name = name; }

    /**
     * Gets the anchor location in the world.
     */
    public Location getAnchorLocation(World world) {
        return new Location(world, x, y, z);
    }

    /**
     * Gets the anchor location using the world name.
     */
    public Location getAnchorLocation() {
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) { return null; }
        return new Location(world, x, y, z);
    }

    /**
     * Gets cached interactive block locations. Must be initialized first.
     */
    public Map<ForgeBlock.ForgeBlockType, Location> getInteractiveLocations() {
        return interactiveLocations;
    }

    /**
     * Gets the fuel zone world locations (where players drop fuel items).
     */
    public List<Location> getFuelZoneLocations() {
        return fuelZoneLocations != null ? fuelZoneLocations : List.of();
    }

    /**
     * Checks if a location is part of this forge's fuel zone.
     */
    public boolean isFuelZoneAt(Location location) {
        if (fuelZoneLocations == null || location == null) { return false; }
        for (Location fuelLoc : fuelZoneLocations) {
            if (fuelLoc.getBlockX() == location.getBlockX()
                    && fuelLoc.getBlockY() == location.getBlockY()
                    && fuelLoc.getBlockZ() == location.getBlockZ()
                    && Objects.equals(fuelLoc.getWorld(), location.getWorld())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Initializes the interactive block location cache and fuel zone from a schematic.
     */
    public void initializeInteractiveLocations(ForgeSchematic schematic) {
        this.interactiveLocations = new HashMap<>();
        this.fuelZoneLocations = new ArrayList<>();
        Location anchor = getAnchorLocation();
        if (anchor == null) { return; }

        for (var entry : schematic.getInteractiveBlocks().entrySet()) {
            Location worldLoc = schematic.toWorldLocation(anchor, rotation, entry.getValue());
            interactiveLocations.put(entry.getKey(), worldLoc);
        }

        for (ForgeBlock fuelBlock : schematic.getFuelZoneBlocks()) {
            Location worldLoc = schematic.toWorldLocation(anchor, rotation, fuelBlock);
            fuelZoneLocations.add(worldLoc);
        }
    }

    /**
     * Checks if a world location is part of this forge's interactive blocks.
     */
    public ForgeBlock.ForgeBlockType getInteractiveTypeAt(Location location) {
        if (interactiveLocations == null) { return null; }
        for (var entry : interactiveLocations.entrySet()) {
            Location interLoc = entry.getValue();
            if (interLoc.getBlockX() == location.getBlockX()
                    && interLoc.getBlockY() == location.getBlockY()
                    && interLoc.getBlockZ() == location.getBlockZ()
                    && Objects.equals(interLoc.getWorld(), location.getWorld())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Checks if the given location is within the bounds of this forge structure.
     */
    public boolean isWithinBounds(Location location, ForgeSchematic schematic) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) { return false; }
        int dx = location.getBlockX() - x;
        int dy = location.getBlockY() - y;
        int dz = location.getBlockZ() - z;
        return dx >= -(schematic.getWidth() / 2) && dx <= (schematic.getWidth() / 2)
                && dy >= 0 && dy < schematic.getHeight()
                && dz >= -(schematic.getDepth() / 2) && dz <= (schematic.getDepth() / 2);
    }

    /**
     * Distance from this forge to a location (in blocks).
     */
    public double distanceTo(Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) { return Double.MAX_VALUE; }
        double dx = location.getX() - x;
        double dy = location.getY() - y;
        double dz = location.getZ() - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ForgeStructure that = (ForgeStructure) o;
        return forgeId.equals(that.forgeId);
    }

    @Override
    public int hashCode() {
        return forgeId.hashCode();
    }
}

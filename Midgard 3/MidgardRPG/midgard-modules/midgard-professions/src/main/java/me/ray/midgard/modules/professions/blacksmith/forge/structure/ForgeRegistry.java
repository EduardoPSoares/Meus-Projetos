package me.ray.midgard.modules.professions.blacksmith.forge.structure;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of all active forge structures in the world.
 * Provides fast lookup by location, owner, and chunk.
 */
public class ForgeRegistry {

    // Primary storage: forgeId → ForgeStructure
    private final Map<UUID, ForgeStructure> forgesById = new ConcurrentHashMap<>();

    // Index: ownerUuid → set of forgeIds
    private final Map<UUID, Set<UUID>> forgesByOwner = new ConcurrentHashMap<>();

    // Index: "world:chunkX:chunkZ" → set of forgeIds (for location-based lookup)
    private final Map<String, Set<UUID>> forgesByChunk = new ConcurrentHashMap<>();

    /**
     * Registers a forge structure.
     */
    public void register(ForgeStructure forge) {
        forgesById.put(forge.getForgeId(), forge);

        forgesByOwner.computeIfAbsent(forge.getOwnerUuid(), k -> ConcurrentHashMap.newKeySet())
                .add(forge.getForgeId());

        String chunkKey = getChunkKey(forge.getWorldName(), forge.getX() >> 4, forge.getZ() >> 4);
        forgesByChunk.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet())
                .add(forge.getForgeId());
    }

    /**
     * Unregisters a forge structure.
     */
    public void unregister(UUID forgeId) {
        ForgeStructure forge = forgesById.remove(forgeId);
        if (forge == null) { return; }

        Set<UUID> ownerForges = forgesByOwner.get(forge.getOwnerUuid());
        if (ownerForges != null) {
            ownerForges.remove(forgeId);
            if (ownerForges.isEmpty()) { forgesByOwner.remove(forge.getOwnerUuid()); }
        }

        String chunkKey = getChunkKey(forge.getWorldName(), forge.getX() >> 4, forge.getZ() >> 4);
        Set<UUID> chunkForges = forgesByChunk.get(chunkKey);
        if (chunkForges != null) {
            chunkForges.remove(forgeId);
            if (chunkForges.isEmpty()) { forgesByChunk.remove(chunkKey); }
        }
    }

    /**
     * Gets a forge by its ID.
     */
    public ForgeStructure getById(UUID forgeId) {
        return forgesById.get(forgeId);
    }

    /**
     * Gets all forges owned by a player.
     */
    public List<ForgeStructure> getByOwner(UUID ownerUuid) {
        Set<UUID> ids = forgesByOwner.get(ownerUuid);
        if (ids == null) { return Collections.emptyList(); }
        List<ForgeStructure> result = new ArrayList<>();
        for (UUID id : ids) {
            ForgeStructure forge = forgesById.get(id);
            if (forge != null) { result.add(forge); }
        }
        return result;
    }

    /**
     * Gets the number of forges a player owns of a specific tier.
     */
    public int countByOwnerAndTier(UUID ownerUuid, ForgeTier tier) {
        return (int) getByOwner(ownerUuid).stream()
                .filter(f -> f.getTier() == tier)
                .count();
    }

    /**
     * Gets the total number of forges a player owns.
     */
    public int countByOwner(UUID ownerUuid) {
        Set<UUID> ids = forgesByOwner.get(ownerUuid);
        return ids != null ? ids.size() : 0;
    }

    /**
     * Gets all forges near a location (within a radius in chunks).
     */
    public List<ForgeStructure> getNearby(org.bukkit.Location location, int chunkRadius) {
        List<ForgeStructure> result = new ArrayList<>();
        int centerChunkX = location.getBlockX() >> 4;
        int centerChunkZ = location.getBlockZ() >> 4;
        String worldName = location.getWorld().getName();

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                String key = getChunkKey(worldName, centerChunkX + dx, centerChunkZ + dz);
                Set<UUID> ids = forgesByChunk.get(key);
                if (ids != null) {
                    for (UUID id : ids) {
                        ForgeStructure forge = forgesById.get(id);
                        if (forge != null) { result.add(forge); }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Finds the nearest forge to a location.
     */
    public ForgeStructure findNearest(org.bukkit.Location location, int maxChunkRadius) {
        List<ForgeStructure> nearby = getNearby(location, maxChunkRadius);
        ForgeStructure nearest = null;
        double minDist = Double.MAX_VALUE;
        for (ForgeStructure forge : nearby) {
            double dist = forge.distanceTo(location);
            if (dist < minDist) {
                minDist = dist;
                nearest = forge;
            }
        }
        return nearest;
    }

    /**
     * Gets all registered forges.
     */
    public Collection<ForgeStructure> getAll() {
        return Collections.unmodifiableCollection(forgesById.values());
    }

    /**
     * Clears all registered forges. Used during reload.
     */
    public void clear() {
        forgesById.clear();
        forgesByOwner.clear();
        forgesByChunk.clear();
    }

    public int size() {
        return forgesById.size();
    }

    private String getChunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }
}

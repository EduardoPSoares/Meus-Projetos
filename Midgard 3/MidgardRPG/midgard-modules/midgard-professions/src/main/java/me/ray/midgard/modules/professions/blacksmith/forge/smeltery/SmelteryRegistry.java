package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de todas as Smelteries ativas no servidor.
 * Usa índices por chunk e owner para lookups O(1) em vez de O(n).
 */
public class SmelteryRegistry {

    // Primary storage: smelteryId → SmelteryStructure
    private final Map<UUID, SmelteryStructure> smelteriesById = new ConcurrentHashMap<>();

    // Index: ownerUuid → set of smelteryIds
    private final Map<UUID, Set<UUID>> smelteriesByOwner = new ConcurrentHashMap<>();

    // Index: "world:chunkX:chunkZ" → set of smelteryIds (para lookup espacial)
    private final Map<String, Set<UUID>> smelteriesByChunk = new ConcurrentHashMap<>();

    public void register(SmelteryStructure smeltery) {
        smelteriesById.put(smeltery.getSmelteryId(), smeltery);

        smelteriesByOwner.computeIfAbsent(smeltery.getOwnerUuid(), k -> ConcurrentHashMap.newKeySet())
                .add(smeltery.getSmelteryId());

        // Indexar todos os chunks cobertos pela estrutura
        for (String chunkKey : getOccupiedChunkKeys(smeltery)) {
            smelteriesByChunk.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet())
                    .add(smeltery.getSmelteryId());
        }
    }

    public void unregister(UUID smelteryId) {
        SmelteryStructure smeltery = smelteriesById.remove(smelteryId);
        if (smeltery == null) { return; }

        Set<UUID> ownerSet = smelteriesByOwner.get(smeltery.getOwnerUuid());
        if (ownerSet != null) {
            ownerSet.remove(smelteryId);
            if (ownerSet.isEmpty()) { smelteriesByOwner.remove(smeltery.getOwnerUuid()); }
        }

        for (String chunkKey : getOccupiedChunkKeys(smeltery)) {
            Set<UUID> chunkSet = smelteriesByChunk.get(chunkKey);
            if (chunkSet != null) {
                chunkSet.remove(smelteryId);
                if (chunkSet.isEmpty()) { smelteriesByChunk.remove(chunkKey); }
            }
        }
    }

    public SmelteryStructure getById(UUID smelteryId) {
        return smelteriesById.get(smelteryId);
    }

    public Collection<SmelteryStructure> getAll() {
        return Collections.unmodifiableCollection(smelteriesById.values());
    }

    public int size() {
        return smelteriesById.size();
    }

    public void clear() {
        smelteriesById.clear();
        smelteriesByOwner.clear();
        smelteriesByChunk.clear();
    }

    /**
     * Encontra a smeltery que contém a localização dada.
     * Usa índice de chunk para lookup O(1) em vez de iterar todas.
     */
    public SmelteryStructure getAtLocation(Location location) {
        if (location.getWorld() == null) { return null; }
        String chunkKey = getChunkKey(location.getWorld().getName(),
                location.getBlockX() >> 4, location.getBlockZ() >> 4);
        Set<UUID> ids = smelteriesByChunk.get(chunkKey);
        if (ids == null) { return null; }
        for (UUID id : ids) {
            SmelteryStructure smeltery = smelteriesById.get(id);
            if (smeltery != null && smeltery.isActive() && smeltery.containsLocation(location)) {
                return smeltery;
            }
        }
        return null;
    }

    /**
     * Encontra a smeltery mais próxima de uma localização (busca por chunks no raio).
     */
    public SmelteryStructure getNearby(Location location, double maxDistance) {
        if (location.getWorld() == null) { return null; }
        String worldName = location.getWorld().getName();
        int chunkRadius = (int) Math.ceil(maxDistance / 16.0);
        int centerChunkX = location.getBlockX() >> 4;
        int centerChunkZ = location.getBlockZ() >> 4;

        SmelteryStructure closest = null;
        double closestDist = maxDistance;

        Set<UUID> checked = new HashSet<>();
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                String key = getChunkKey(worldName, centerChunkX + dx, centerChunkZ + dz);
                Set<UUID> ids = smelteriesByChunk.get(key);
                if (ids == null) { continue; }
                for (UUID id : ids) {
                    if (!checked.add(id)) { continue; }
                    SmelteryStructure smeltery = smelteriesById.get(id);
                    if (smeltery == null || !smeltery.isActive()) { continue; }
                    if (!smeltery.getWorldName().equals(worldName)) { continue; }
                    Location anchor = smeltery.getAnchorLocation();
                    if (anchor == null) { continue; }
                    double dist = anchor.distance(location);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = smeltery;
                    }
                }
            }
        }
        return closest;
    }

    /**
     * Conta smelteries por dono. O(1) lookup no índice.
     */
    public int countByOwner(UUID ownerUuid) {
        Set<UUID> ids = smelteriesByOwner.get(ownerUuid);
        return ids != null ? ids.size() : 0;
    }

    /**
     * Lista smelteries de um dono. Usa índice de owner.
     */
    public List<SmelteryStructure> getByOwner(UUID ownerUuid) {
        Set<UUID> ids = smelteriesByOwner.get(ownerUuid);
        if (ids == null) { return Collections.emptyList(); }
        List<SmelteryStructure> result = new ArrayList<>();
        for (UUID id : ids) {
            SmelteryStructure smeltery = smelteriesById.get(id);
            if (smeltery != null) { result.add(smeltery); }
        }
        return result;
    }

    // ── Utilitários de chunk ──

    private String getChunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }

    /**
     * Retorna todas as chunk keys ocupadas por uma smeltery (pode cobrir múltiplos chunks).
     */
    private List<String> getOccupiedChunkKeys(SmelteryStructure smeltery) {
        List<String> keys = new ArrayList<>();
        int tw = smeltery.getTier().getTotalWidth();
        int td = smeltery.getTier().getTotalDepth();
        int minChunkX = smeltery.getX() >> 4;
        int maxChunkX = (smeltery.getX() + tw - 1) >> 4;
        int minChunkZ = smeltery.getZ() >> 4;
        int maxChunkZ = (smeltery.getZ() + td - 1) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                keys.add(getChunkKey(smeltery.getWorldName(), cx, cz));
            }
        }
        return keys;
    }
}

package de.maxhenkel.voicechat.zone;

import de.maxhenkel.voicechat.Voicechat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZoneParticleVisualizer {

    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> viewingZones = new ConcurrentHashMap<>();

    private static final double PARTICLE_SPACING = 1.0;
    private static final int TICK_INTERVAL = 10;

    public boolean isViewing(UUID playerUuid) {
        return activeTasks.containsKey(playerUuid);
    }

    public boolean isViewingZone(UUID playerUuid, String zoneName) {
        Set<String> zones = viewingZones.get(playerUuid);
        return zones != null && zones.contains(zoneName);
    }

    public void toggleAll(Player player) {
        if (activeTasks.containsKey(player.getUniqueId())) {
            stopViewing(player);
        } else {
            startViewingAll(player);
        }
    }

    public void toggleZone(Player player, String zoneName) {
        Set<String> zones = viewingZones.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        if (zones.contains(zoneName)) {
            zones.remove(zoneName);
            if (zones.isEmpty()) {
                stopViewing(player);
            }
        } else {
            zones.add(zoneName);
            if (!activeTasks.containsKey(player.getUniqueId())) {
                startTask(player);
            }
        }
    }

    public void startViewingAll(Player player) {
        Set<String> zones = ConcurrentHashMap.newKeySet();
        for (RestrictedZone zone : Voicechat.restrictedZoneManager.getZones()) {
            zones.add(zone.getName());
        }
        viewingZones.put(player.getUniqueId(), zones);
        startTask(player);
    }

    public void startViewingZone(Player player, String zoneName) {
        Set<String> zones = viewingZones.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        zones.add(zoneName);
        if (!activeTasks.containsKey(player.getUniqueId())) {
            startTask(player);
        }
    }

    public void stopViewing(Player player) {
        BukkitTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        viewingZones.remove(player.getUniqueId());
    }

    public void showSelection(Player player, Location pos1, Location pos2) {
        if (pos1 == null || pos2 == null) return;
        if (pos1.getWorld() == null || pos2.getWorld() == null) return;
        if (!pos1.getWorld().equals(pos2.getWorld())) return;

        String selectionKey = "__selection__";
        Set<String> zones = viewingZones.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        zones.add(selectionKey);

        BukkitTask existing = activeTasks.get(player.getUniqueId());
        if (existing != null) {
            existing.cancel();
        }

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(Voicechat.INSTANCE, () -> {
            Player p = Bukkit.getPlayer(player.getUniqueId());
            if (p == null || !p.isOnline()) {
                stopViewing(player);
                return;
            }
            drawBoxEdges(p, pos1.getWorld(), minX, minY, minZ, maxX, maxY, maxZ, Particle.COMPOSTER);
        }, 0L, TICK_INTERVAL);

        activeTasks.put(player.getUniqueId(), task);
    }

    private void startTask(Player player) {
        BukkitTask existing = activeTasks.get(player.getUniqueId());
        if (existing != null) {
            existing.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(Voicechat.INSTANCE, () -> {
            Player p = Bukkit.getPlayer(player.getUniqueId());
            if (p == null || !p.isOnline()) {
                stopViewing(player);
                return;
            }

            Set<String> zoneNames = viewingZones.get(player.getUniqueId());
            if (zoneNames == null || zoneNames.isEmpty()) {
                stopViewing(player);
                return;
            }

            for (String zoneName : zoneNames) {
                if (zoneName.equals("__selection__")) continue;
                RestrictedZone zone = Voicechat.restrictedZoneManager.getZone(zoneName);
                if (zone == null) continue;

                World world = Bukkit.getWorld(zone.getWorld());
                if (world == null || !p.getWorld().equals(world)) continue;

                double dist = p.getLocation().distance(new Location(world,
                        (zone.getMinX() + zone.getMaxX()) / 2.0,
                        (zone.getMinY() + zone.getMaxY()) / 2.0,
                        (zone.getMinZ() + zone.getMaxZ()) / 2.0));
                if (dist > 200) continue;

                Particle particle = zone.isVoiceEnabled() ? Particle.HAPPY_VILLAGER : Particle.FLAME;
                drawBoxEdges(p, world, zone.getMinX(), zone.getMinY(), zone.getMinZ(),
                        zone.getMaxX(), zone.getMaxY(), zone.getMaxZ(), particle);
            }
        }, 0L, TICK_INTERVAL);

        activeTasks.put(player.getUniqueId(), task);
    }

    private void drawBoxEdges(Player player, World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Particle particle) {
        double x1 = minX;
        double y1 = minY;
        double z1 = minZ;
        double x2 = maxX + 1.0;
        double y2 = maxY + 1.0;
        double z2 = maxZ + 1.0;

        // 4 edges along X axis
        for (double x = x1; x <= x2; x += PARTICLE_SPACING) {
            spawnParticle(player, world, x, y1, z1, particle);
            spawnParticle(player, world, x, y1, z2, particle);
            spawnParticle(player, world, x, y2, z1, particle);
            spawnParticle(player, world, x, y2, z2, particle);
        }

        // 4 edges along Y axis
        for (double y = y1; y <= y2; y += PARTICLE_SPACING) {
            spawnParticle(player, world, x1, y, z1, particle);
            spawnParticle(player, world, x1, y, z2, particle);
            spawnParticle(player, world, x2, y, z1, particle);
            spawnParticle(player, world, x2, y, z2, particle);
        }

        // 4 edges along Z axis
        for (double z = z1; z <= z2; z += PARTICLE_SPACING) {
            spawnParticle(player, world, x1, y1, z, particle);
            spawnParticle(player, world, x1, y2, z, particle);
            spawnParticle(player, world, x2, y1, z, particle);
            spawnParticle(player, world, x2, y2, z, particle);
        }
    }

    private void spawnParticle(Player player, World world, double x, double y, double z, Particle particle) {
        Location loc = new Location(world, x, y, z);
        if (player.getLocation().distance(loc) < 100) {
            player.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
        }
    }

    public void cleanup() {
        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        viewingZones.clear();
    }
}

package me.ray.midgard.modules.performance.monitor;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitora estatísticas do servidor em tempo real.
 * Inclui entidades, chunks, jogadores e mundos.
 */
public class ServerMonitor {

    /**
     * Obtém informações de memória do servidor.
     */
    public static MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;
        
        return new MemoryInfo(max, total, used, free);
    }

    /**
     * Obtém contagem de entidades por mundo.
     */
    public static Map<String, EntityStats> getEntityStats() {
        Map<String, EntityStats> stats = new HashMap<>();
        
        for (World world : Bukkit.getWorlds()) {
            Map<EntityType, Integer> typeCounts = new EnumMap<>(EntityType.class);
            int total = 0;
            int hostile = 0;
            int passive = 0;
            int players = 0;
            int other = 0;
            
            for (Entity entity : world.getEntities()) {
                EntityType type = entity.getType();
                typeCounts.merge(type, 1, (a, b) -> a + b);
                total++;
                
                if (entity instanceof Player) {
                    players++;
                } else if (isHostile(type)) {
                    hostile++;
                } else if (isPassive(type)) {
                    passive++;
                } else {
                    other++;
                }
            }
            
            stats.put(world.getName(), new EntityStats(total, hostile, passive, players, other, typeCounts));
        }
        
        return stats;
    }

    /**
     * Obtém contagem de chunks carregados por mundo.
     */
    public static Map<String, ChunkStats> getChunkStats() {
        Map<String, ChunkStats> stats = new HashMap<>();
        
        for (World world : Bukkit.getWorlds()) {
            if (me.ray.midgard.core.utils.Task.isFolia()) {
                 // Folia safe implementation (skip chunk counting)
                 stats.put(world.getName(), new ChunkStats(0, 0, 0));
                 continue;
            }

            Chunk[] chunks = world.getLoadedChunks();
            int loaded = chunks.length;
            int forceLoaded = 0;
            int ticketing = 0;
            
            for (Chunk chunk : chunks) {
                if (chunk.isForceLoaded()) {
                    forceLoaded++;
                }
                // Check if chunk is entity ticking (ordinal comparison)
                if (chunk.getLoadLevel().ordinal() >= Chunk.LoadLevel.ENTITY_TICKING.ordinal()) {
                    ticketing++;
                }
            }
            
            stats.put(world.getName(), new ChunkStats(loaded, forceLoaded, ticketing));
        }
        
        return stats;
    }

    /**
     * Obtém estatísticas de jogadores online.
     */
    public static PlayerStats getPlayerStats() {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        
        double avgPing = 0;
        int minPing = Integer.MAX_VALUE;
        int maxPing = 0;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            int ping = player.getPing();
            avgPing += ping;
            if (ping < minPing) {
                minPing = ping;
            }
            if (ping > maxPing) {
                maxPing = ping;
            }
        }
        
        if (online > 0) {
            avgPing /= online;
        } else {
            minPing = 0;
        }
        
        return new PlayerStats(online, max, (int) avgPing, minPing, maxPing);
    }

    /**
     * Força garbage collection e retorna tempo gasto.
     */
    public static long forceGC() {
        long before = System.currentTimeMillis();
        System.gc();
        return System.currentTimeMillis() - before;
    }

    // ========== Helper Methods ==========

    private static boolean isHostile(EntityType type) {
        return switch (type) {
            case ZOMBIE, SKELETON, CREEPER, SPIDER, CAVE_SPIDER, ENDERMAN,
                 WITCH, SLIME, MAGMA_CUBE, BLAZE, GHAST, WITHER_SKELETON,
                 GUARDIAN, ELDER_GUARDIAN, SHULKER, EVOKER, VINDICATOR,
                 VEX, PILLAGER, RAVAGER, HOGLIN, PIGLIN_BRUTE, ZOGLIN,
                 WARDEN, PHANTOM, DROWNED, HUSK, STRAY, ZOMBIFIED_PIGLIN,
                 ENDERMITE, SILVERFISH -> true;
            default -> false;
        };
    }

    private static boolean isPassive(EntityType type) {
        return switch (type) {
            case PIG, COW, SHEEP, CHICKEN, HORSE, DONKEY, MULE, LLAMA,
                 RABBIT, FOX, BEE, CAT, WOLF, PARROT, TURTLE, DOLPHIN,
                 COD, SALMON, TROPICAL_FISH, PUFFERFISH, SQUID, GLOW_SQUID,
                 AXOLOTL, GOAT, FROG, TADPOLE, ALLAY, SNIFFER, CAMEL,
                 VILLAGER, WANDERING_TRADER, IRON_GOLEM, SNOW_GOLEM -> true;
            default -> false;
        };
    }

    // ========== Data Records ==========

    public record MemoryInfo(long max, long total, long used, long free) {
        public double usedPercent() {
            return (double) used / max * 100;
        }
        
        public String usedMB() {
            return String.format("%.1f", used / 1024.0 / 1024.0);
        }
        
        public String maxMB() {
            return String.format("%.1f", max / 1024.0 / 1024.0);
        }
        
        public String freeMB() {
            return String.format("%.1f", free / 1024.0 / 1024.0);
        }
        
        public String getColor() {
            double percent = usedPercent();
            if (percent < 60) {
                return "<green>";
            }
            if (percent < 80) {
                return "<yellow>";
            }
            if (percent < 90) {
                return "<gold>";
            }
            return "<red>";
        }
    }

    public record EntityStats(int total, int hostile, int passive, int players, int other,
                               Map<EntityType, Integer> byType) {
        
        public String getTopEntities(int count) {
            return byType.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(count)
                .map(e -> e.getKey().name().toLowerCase() + ":" + e.getValue())
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
        }
    }

    public record ChunkStats(int loaded, int forceLoaded, int ticketing) {}

    public record PlayerStats(int online, int max, int avgPing, int minPing, int maxPing) {
        public String getPingColor(int ping) {
            if (ping < 50) {
                return "<green>";
            }
            if (ping < 100) {
                return "<yellow>";
            }
            if (ping < 200) {
                return "<gold>";
            }
            return "<red>";
        }
    }
}

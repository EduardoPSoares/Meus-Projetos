package me.ray.midgard.modules.professions.blacksmith.forge.shared;

import me.ray.midgard.core.utils.Task;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Base abstrata para sessões de criação de templates (Forge e Smeltery).
 * Gerencia seleção de posições, escaneamento de área, visualização wireframe e dimensões.
 *
 * @param <B> tipo do bloco escaneado (ex: ForgeBlock.ForgeBlockType ou SmelteryBlockType)
 */
public abstract class AbstractCreationSession<B extends Enum<B>> {

    private final UUID adminId;
    private Location pos1;
    private Location pos2;
    private String name;
    private int requiredLevel;
    private boolean scanned;

    private List<ScannedBlock<B>> scannedBlocks;
    private final Map<String, B> assignedRoles = new LinkedHashMap<>();
    private BukkitTask visualTask;

    protected AbstractCreationSession(UUID adminId, String defaultName) {
        this.adminId = adminId;
        this.name = defaultName;
        this.requiredLevel = 1;
    }

    // ── Subclass hooks ──

    /** Cor das partículas de wireframe. */
    protected abstract Color getWireframeColor();

    /** Tipo padrão para blocos sem função especial (ex: STRUCTURE ou WALL). */
    protected abstract B getDefaultBlockType();

    /** Detecta automaticamente o tipo de bloco pelo material. */
    protected abstract B autoDetectType(Material material);

    /** Tipos de bloco obrigatórios que precisam existir no template. */
    protected abstract B[] getRequiredBlockTypes();

    // ── Getters / Setters ──

    public UUID getAdminId() { return adminId; }
    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public String getName() { return name; }
    public int getRequiredLevel() { return requiredLevel; }
    public List<ScannedBlock<B>> getScannedBlocks() { return scannedBlocks; }
    public Map<String, B> getAssignedRoles() { return assignedRoles; }
    public boolean isScanned() { return scanned; }
    public boolean hasBothPositions() { return pos1 != null && pos2 != null; }

    public void setName(String name) { this.name = name; }
    public void setRequiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; }

    public void setPos1(Location loc) {
        this.pos1 = loc.clone();
        this.scanned = false;
        this.scannedBlocks = null;
    }

    public void setPos2(Location loc) {
        this.pos2 = loc.clone();
        this.scanned = false;
        this.scannedBlocks = null;
    }

    // ── Scanning ──

    public int scanArea() {
        if (pos1 == null || pos2 == null) { return -1; }

        scannedBlocks = new ArrayList<>();
        assignedRoles.clear();

        World world = pos1.getWorld();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material mat = block.getType();
                    if (mat.isAir()) { continue; }

                    B autoType = autoDetectType(mat);
                    ScannedBlock<B> sb = new ScannedBlock<>(x, y, z, mat, autoType);
                    scannedBlocks.add(sb);

                    if (autoType != getDefaultBlockType()) {
                        assignedRoles.put(sb.locationKey(), autoType);
                    }
                }
            }
        }
        this.scanned = true;
        return scannedBlocks.size();
    }

    // ── Dimensions ──

    public Location getMinCorner() {
        if (pos1 == null || pos2 == null) { return null; }
        return new Location(pos1.getWorld(),
                Math.min(pos1.getBlockX(), pos2.getBlockX()),
                Math.min(pos1.getBlockY(), pos2.getBlockY()),
                Math.min(pos1.getBlockZ(), pos2.getBlockZ()));
    }

    public Location getMaxCorner() {
        if (pos1 == null || pos2 == null) { return null; }
        return new Location(pos1.getWorld(),
                Math.max(pos1.getBlockX(), pos2.getBlockX()),
                Math.max(pos1.getBlockY(), pos2.getBlockY()),
                Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
    }

    public int getWidth() {
        Location min = getMinCorner(), max = getMaxCorner();
        return max.getBlockX() - min.getBlockX() + 1;
    }

    public int getHeight() {
        Location min = getMinCorner(), max = getMaxCorner();
        return max.getBlockY() - min.getBlockY() + 1;
    }

    public int getDepth() {
        Location min = getMinCorner(), max = getMaxCorner();
        return max.getBlockZ() - min.getBlockZ() + 1;
    }

    // ── Role Assignment ──

    public void assignRole(String locationKey, B type) {
        if (type == getDefaultBlockType()) {
            assignedRoles.remove(locationKey);
        } else {
            assignedRoles.put(locationKey, type);
        }
    }

    public B getAssignedRole(String locationKey) {
        return assignedRoles.getOrDefault(locationKey, getDefaultBlockType());
    }

    public Map<B, Integer> getRoleCounts() {
        Map<B, Integer> counts = new EnumMap<>(getDefaultBlockType().getDeclaringClass());
        for (B type : assignedRoles.values()) {
            counts.merge(type, 1, Integer::sum);
        }
        return counts;
    }

    public boolean hasRequiredRoles() {
        Map<B, Integer> counts = getRoleCounts();
        for (B type : getRequiredBlockTypes()) {
            if (!counts.containsKey(type)) { return false; }
        }
        return true;
    }

    public List<B> getMissingRoles() {
        Map<B, Integer> counts = getRoleCounts();
        List<B> missing = new ArrayList<>();
        for (B type : getRequiredBlockTypes()) {
            if (!counts.containsKey(type)) { missing.add(type); }
        }
        return missing;
    }

    // ── Visualization ──

    public void startVisualization(Player player) {
        stopVisualization();

        Location anchor = pos1 != null ? pos1 : pos2;
        if (anchor == null || anchor.getWorld() == null) { return; }

        visualTask = Task.syncTimer(anchor, () -> {
            Player p = Bukkit.getPlayer(adminId);
            if (p == null || !p.isOnline()) {
                stopVisualization();
                return;
            }

            if (pos1 != null && pos2 == null) {
                drawMarker(p, pos1);
            } else if (pos1 == null && pos2 != null) {
                drawMarker(p, pos2);
            } else if (pos1 != null) {
                drawWireframe(p);
            }
        }, 5L, 8L);
    }

    public void stopVisualization() {
        if (visualTask != null) {
            visualTask.cancel();
            visualTask = null;
        }
    }

    private void drawMarker(Player player, Location loc) {
        Particle.DustOptions dust = new Particle.DustOptions(getWireframeColor(), 0.8f);
        for (double dy = 0; dy <= 1.0; dy += 0.25) {
            player.spawnParticle(Particle.DUST, loc.getBlockX() + 0.5, loc.getBlockY() + dy,
                    loc.getBlockZ() + 0.5, 1, 0, 0, 0, 0, dust);
        }
    }

    private void drawWireframe(Player player) {
        double x1 = Math.min(pos1.getBlockX(), pos2.getBlockX());
        double y1 = Math.min(pos1.getBlockY(), pos2.getBlockY());
        double z1 = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        double x2 = Math.max(pos1.getBlockX(), pos2.getBlockX()) + 1.0;
        double y2 = Math.max(pos1.getBlockY(), pos2.getBlockY()) + 1.0;
        double z2 = Math.max(pos1.getBlockZ(), pos2.getBlockZ()) + 1.0;

        Particle.DustOptions dust = new Particle.DustOptions(getWireframeColor(), 0.7f);

        drawLine(player, x1, y1, z1, x2, y1, z1, dust);
        drawLine(player, x1, y1, z2, x2, y1, z2, dust);
        drawLine(player, x1, y1, z1, x1, y1, z2, dust);
        drawLine(player, x2, y1, z1, x2, y1, z2, dust);
        drawLine(player, x1, y2, z1, x2, y2, z1, dust);
        drawLine(player, x1, y2, z2, x2, y2, z2, dust);
        drawLine(player, x1, y2, z1, x1, y2, z2, dust);
        drawLine(player, x2, y2, z1, x2, y2, z2, dust);
        drawLine(player, x1, y1, z1, x1, y2, z1, dust);
        drawLine(player, x2, y1, z1, x2, y2, z1, dust);
        drawLine(player, x1, y1, z2, x1, y2, z2, dust);
        drawLine(player, x2, y1, z2, x2, y2, z2, dust);
    }

    private void drawLine(Player player, double x1, double y1, double z1,
                          double x2, double y2, double z2, Particle.DustOptions dust) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.01) { return; }

        double step = 0.4;
        int points = (int) (length / step) + 1;
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            player.spawnParticle(Particle.DUST,
                    x1 + dx * t, y1 + dy * t, z1 + dz * t,
                    1, 0, 0, 0, 0, dust);
        }
    }

    // ── Record ──

    public record ScannedBlock<B>(int worldX, int worldY, int worldZ, Material material,
                                  B autoDetectedType) {
        public String locationKey() {
            return worldX + "," + worldY + "," + worldZ;
        }
    }
}

package midgardvanish.nms;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages virtual TextDisplay entities above player heads,
 * visible only to vanished staff members.
 */
public class NameTagManager {

    private final JavaPlugin plugin;
    // targetUUID -> virtual entity ID
    private final Map<UUID, Integer> entityIds = new ConcurrentHashMap<>();
    // viewerUUID -> set of target UUIDs they can see nametags for
    private final Map<UUID, Set<UUID>> activeViewers = new ConcurrentHashMap<>();
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(Integer.MAX_VALUE / 2);

    // TextDisplay data accessor indices (1.21.4)
    // Entity base: 0=flags, 1=air, 2=customName, 3=customNameVisible, 4=silent, 5=noGravity, 6=pose, 7=ticksFrozen
    // Display base: 8=interpStartDelta, 9=interpDuration, 10=posRotInterpDuration, 11=translation(V3),
    //   12=scale(V3), 13=leftRot(Q), 14=rightRot(Q), 15=billboard(BYTE), 16=brightness(INT),
    //   17=viewRange(FLOAT), 18=shadowRadius, 19=shadowStrength, 20=width, 21=height, 22=glowColor(INT)
    // TextDisplay: 23=text, 24=lineWidth, 25=backgroundColor, 26=textOpacity, 27=styleFlags

    public NameTagManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private int getOrCreateEntityId(UUID targetUUID) {
        return entityIds.computeIfAbsent(targetUUID, k -> ENTITY_ID_COUNTER.getAndDecrement());
    }

    private ServerGamePacketListenerImpl getConnection(Player player) {
        return ((CraftPlayer) player).getHandle().connection;
    }

    /**
     * Show a target player's nametag to a viewer (vanished staff).
     */
    public void showNameTag(Player viewer, Player target) {
        UUID viewerUUID = viewer.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        Set<UUID> targets = activeViewers.computeIfAbsent(viewerUUID, k -> ConcurrentHashMap.newKeySet());
        if (!targets.add(targetUUID)) {
            // Already showing, just update text
            updateNameTagText(viewer, target);
            return;
        }

        try {
            int entityId = getOrCreateEntityId(targetUUID);
            var nmsTarget = ((CraftPlayer) target).getHandle();
            ServerGamePacketListenerImpl conn = getConnection(viewer);

            // Spawn the TextDisplay entity at target's location
            conn.send(new ClientboundAddEntityPacket(
                    entityId,
                    UUID.randomUUID(),
                    target.getLocation().getX(),
                    target.getLocation().getY() + getNameTagHeight(target),
                    target.getLocation().getZ(),
                    0f, // xRot
                    0f, // yRot
                    EntityType.TEXT_DISPLAY,
                    0,  // data
                    Vec3.ZERO,
                    0.0 // yHeadRot
            ));

            // Send metadata
            conn.send(new ClientboundSetEntityDataPacket(entityId, buildNameTagMetadata(target)));

            // Make the TextDisplay ride the target player
            sendPassengersPacket(conn, nmsTarget.getId(), entityId);
        } catch (Exception e) {
            plugin.getLogger().warning("[NameTag] Failed to show nametag for " + target.getName() + " to " + viewer.getName() + ": " + e.getMessage());
            targets.remove(targetUUID);
        }
    }

    /**
     * Hide a target player's nametag from a viewer.
     */
    public void hideNameTag(Player viewer, UUID targetUUID) {
        Set<UUID> targets = activeViewers.get(viewer.getUniqueId());
        if (targets == null || !targets.remove(targetUUID)) return;

        Integer entityId = entityIds.get(targetUUID);
        if (entityId == null) return;

        getConnection(viewer).send(new ClientboundRemoveEntitiesPacket(entityId));
    }

    /**
     * Hide all nametags from a viewer (when they leave vanish).
     */
    public void hideAllNameTags(Player viewer) {
        Set<UUID> targets = activeViewers.remove(viewer.getUniqueId());
        if (targets == null || targets.isEmpty()) return;

        int[] ids = targets.stream()
                .map(entityIds::get)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .toArray();

        if (ids.length > 0) {
            getConnection(viewer).send(new ClientboundRemoveEntitiesPacket(ids));
        }
    }

    /**
     * Show all online players' nametags to a viewer (when they enter vanish).
     */
    public void showAllNameTags(Player viewer) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(viewer)) continue;
            showNameTag(viewer, online);
        }
    }

    /**
     * Update the display text of a nametag for all viewers seeing it.
     */
    public void updateNameTagText(Player viewer, Player target) {
        Integer entityId = entityIds.get(target.getUniqueId());
        if (entityId == null) return;

        Set<UUID> targets = activeViewers.get(viewer.getUniqueId());
        if (targets == null || !targets.contains(target.getUniqueId())) return;

        getConnection(viewer).send(new ClientboundSetEntityDataPacket(entityId, buildNameTagMetadata(target)));
    }

    /**
     * Remove a target from the system entirely (when they quit).
     */
    public void removeTarget(Player target) {
        UUID targetUUID = target.getUniqueId();
        Integer entityId = entityIds.remove(targetUUID);
        if (entityId == null) return;

        // Remove from all active viewers
        for (Map.Entry<UUID, Set<UUID>> entry : activeViewers.entrySet()) {
            if (entry.getValue().remove(targetUUID)) {
                Player viewer = Bukkit.getPlayer(entry.getKey());
                if (viewer != null && viewer.isOnline()) {
                    getConnection(viewer).send(new ClientboundRemoveEntitiesPacket(entityId));
                }
            }
        }
    }

    /**
     * When a new player joins, show their nametag to all current vanished viewers.
     */
    public void handleTargetJoin(Player target, Set<UUID> vanishedPlayers) {
        for (UUID vanishedUUID : vanishedPlayers) {
            Player viewer = Bukkit.getPlayer(vanishedUUID);
            if (viewer == null || viewer.equals(target)) continue;
            if (viewer.isOnline()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (viewer.isOnline() && target.isOnline()) {
                        showNameTag(viewer, target);
                    }
                }, 5L);
            }
        }
    }

    /**
     * Check if a viewer is currently tracking a target's nametag.
     */
    public boolean isTracking(UUID viewerUUID, UUID targetUUID) {
        Set<UUID> targets = activeViewers.get(viewerUUID);
        return targets != null && targets.contains(targetUUID);
    }

    /**
     * Re-send the TextDisplay entity for an already tracked viewer-target pair.
     * Called when the target player's entity is re-spawned on the client (chunk load, TP, etc).
     */
    public void resendNameTag(Player viewer, Player target) {
        UUID viewerUUID = viewer.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        Set<UUID> targets = activeViewers.get(viewerUUID);
        if (targets == null || !targets.contains(targetUUID)) return;

        Integer entityId = entityIds.get(targetUUID);
        if (entityId == null) return;

        try {
            var nmsTarget = ((CraftPlayer) target).getHandle();
            ServerGamePacketListenerImpl conn = getConnection(viewer);

            // Re-spawn the TextDisplay entity
            conn.send(new ClientboundAddEntityPacket(
                    entityId,
                    UUID.randomUUID(),
                    target.getLocation().getX(),
                    target.getLocation().getY() + getNameTagHeight(target),
                    target.getLocation().getZ(),
                    0f, 0f,
                    EntityType.TEXT_DISPLAY,
                    0, Vec3.ZERO, 0.0
            ));

            // Re-send metadata
            conn.send(new ClientboundSetEntityDataPacket(entityId, buildNameTagMetadata(target)));

            // Re-attach as passenger
            sendPassengersPacket(conn, nmsTarget.getId(), entityId);
        } catch (Exception e) {
            plugin.getLogger().warning("[NameTag] Failed to resend nametag: " + e.getMessage());
        }
    }

    /**
     * Cleanup everything on disable.
     */
    public void cleanup() {
        for (Map.Entry<UUID, Set<UUID>> entry : activeViewers.entrySet()) {
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer != null && viewer.isOnline()) {
                hideAllNameTags(viewer);
            }
        }
        activeViewers.clear();
        entityIds.clear();
    }

    // === Private helpers ===

    private List<SynchedEntityData.DataValue<?>> buildNameTagMetadata(Player target) {
        // Get display name - use Bukkit displayName (TAB modifies this)
        String displayName = target.getName();

        return List.of(
                // Entity flags: invisible (0x20) is NOT set - we want visible text
                // Index 0: Entity flags byte
                SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(0, EntityDataSerializers.BYTE), (byte) 0
                ),
                // Index 5: No gravity
                SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(5, EntityDataSerializers.BOOLEAN), true
                ),
                // Index 15: Billboard constraint (2 = CENTER - always faces player)
                SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(15, EntityDataSerializers.BYTE), (byte) 3
                ),
                // Index 17: View range (multiplier, default 1.0)
                SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(17, EntityDataSerializers.FLOAT), 1.0f
                ),
                // Index 23: Text (Component)
                SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(23, EntityDataSerializers.COMPONENT), Component.literal(displayName)
                ),
                // Index 24: Line width
                SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(24, EntityDataSerializers.INT), 200
                ),
                // Index 25: Background color (ARGB - fully transparent)
                SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(25, EntityDataSerializers.INT), 0
                ),
                // Index 26: Text opacity (byte, -1 = fully opaque)
                SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(26, EntityDataSerializers.BYTE), (byte) -1
                ),
                // Index 27: Style flags (bit 3 = SEE_THROUGH, bit 0 = HAS_SHADOW)
                SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(27, EntityDataSerializers.BYTE), (byte) 0x01
                ),
                // Display translation Y offset (to position above head)
                SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(11, EntityDataSerializers.VECTOR3), new org.joml.Vector3f(0f, 0.3f, 0f)
                ),
                // Display scale
                SynchedEntityData.DataValue.create(
                        new EntityDataAccessor<>(12, EntityDataSerializers.VECTOR3), new org.joml.Vector3f(1f, 1f, 1f)
                )
        );
    }

    private double getNameTagHeight(Player target) {
        // Place above the player's head
        return target.getHeight() + 0.3;
    }

    private void sendPassengersPacket(ServerGamePacketListenerImpl conn, int vehicleId, int passengerId) {
        // We need to create a passengers packet manually
        // ClientboundSetPassengersPacket requires an Entity, so we use reflection
        try {
            var nmsEntity = findNmsEntity(vehicleId);
            if (nmsEntity != null) {
                // Manually construct the packet using Unsafe
                var packet = createPassengersPacketDirect(vehicleId, passengerId);
                conn.send(packet);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[NameTag] Failed to send passengers packet: " + e.getMessage());
        }
    }

    private net.minecraft.world.entity.Entity findNmsEntity(int entityId) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            var nmsPlayer = ((CraftPlayer) p).getHandle();
            if (nmsPlayer.getId() == entityId) return nmsPlayer;
        }
        return null;
    }

    private ClientboundSetPassengersPacket createPassengersPacketDirect(int vehicleId, int passengerId) throws Exception {
        // Use Unsafe to create packet without entity
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) unsafeField.get(null);

        var packet = (ClientboundSetPassengersPacket) unsafe.allocateInstance(ClientboundSetPassengersPacket.class);

        // Set vehicle field
        var vehicleField = ClientboundSetPassengersPacket.class.getDeclaredField("vehicle");
        vehicleField.setAccessible(true);
        long vehicleOffset = unsafe.objectFieldOffset(vehicleField);
        unsafe.putInt(packet, vehicleOffset, vehicleId);

        // Set passengers field
        var passengersField = ClientboundSetPassengersPacket.class.getDeclaredField("passengers");
        passengersField.setAccessible(true);
        long passengersOffset = unsafe.objectFieldOffset(passengersField);
        unsafe.putObject(packet, passengersOffset, new int[]{passengerId});

        return packet;
    }
}

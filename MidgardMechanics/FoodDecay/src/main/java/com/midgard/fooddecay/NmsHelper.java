package com.midgard.fooddecay;

import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * NMS helper for Paper 1.21.4 (Mojang mappings).
 * <p>
 * Forces immediate per-slot inventory sync instead of waiting for the next server tick.
 * Uses {@code AbstractContainerMenu.broadcastChanges()} which internally:
 * <ul>
 *   <li>Detects which slots changed by comparing current items vs. remoteSlots</li>
 *   <li>Sends {@code ClientboundContainerSetSlotPacket} for each changed slot only</li>
 *   <li>Updates remoteSlots to prevent duplicate sync on the next tick</li>
 * </ul>
 * Result: zero-delay visual lore updates with minimal bandwidth.
 */
public final class NmsHelper {

    private static final Logger LOGGER = Logger.getLogger("FoodDecay");
    private static boolean available = false;

    private static Method getHandleMethod;
    private static Field containerMenuField;
    private static Method broadcastChangesMethod;

    static {
        try {
            // CraftPlayer.getHandle() → ServerPlayer
            Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            getHandleMethod = craftPlayer.getMethod("getHandle");

            // ServerPlayer.containerMenu → AbstractContainerMenu (active menu)
            Class<?> serverPlayer = Class.forName("net.minecraft.server.level.ServerPlayer");
            containerMenuField = serverPlayer.getField("containerMenu");

            // AbstractContainerMenu.broadcastChanges() — syncs changed slots to client
            Class<?> abstractMenu = Class.forName("net.minecraft.world.inventory.AbstractContainerMenu");
            broadcastChangesMethod = abstractMenu.getMethod("broadcastChanges");

            available = true;
            LOGGER.info("[FoodDecay] NMS immediate sync enabled (Paper 1.21.4)");
        } catch (Exception e) {
            available = false;
            LOGGER.warning("[FoodDecay] NMS immediate sync unavailable: " + e.getMessage());
        }
    }

    private NmsHelper() {}

    public static boolean isAvailable() {
        return available;
    }

    /**
     * Forces the server to immediately sync all pending inventory/container changes
     * to the player. Only changed slots are sent as individual packets.
     * <p>
     * Without this call, visual updates are delayed until the next server tick (~50ms).
     * With this call, the player sees lore changes instantly.
     */
    public static void syncInventory(Player player) {
        if (!available) return;
        try {
            Object serverPlayer = getHandleMethod.invoke(player);
            Object menu = containerMenuField.get(serverPlayer);
            broadcastChangesMethod.invoke(menu);
        } catch (Exception e) {
            LOGGER.fine("NMS sync fallback for " + player.getName() + ": " + e.getMessage());
        }
    }
}

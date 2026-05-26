package com.midgard.core.nms;

import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * High-level packet utilities built on top of ReflectionUtils.
 */
public final class PacketUtils {

    private static final Logger LOGGER = Logger.getLogger("MidgardCore");

    private PacketUtils() {
    }

    public static void sendPacket(Player player, Object packet) {
        try {
            Object entityPlayer = ReflectionUtils.getHandle(player);
            Object connection = ReflectionUtils.getPlayerConnection(entityPlayer);
            ReflectionUtils.sendPacket(connection, packet);
        } catch (ReflectiveOperationException e) {
            LOGGER.fine("Failed to send packet to " + player.getName() + ": " + e.getMessage());
        }
    }

    public static void sendPacket(Iterable<? extends Player> players, Object packet) {
        for (Player player : players) {
            sendPacket(player, packet);
        }
    }

    public static Object createPacket(String className, Class<?>[] paramTypes, Object... args) {
        try {
            Class<?> packetClass = ReflectionUtils.getNMSClass(className);
            return ReflectionUtils.newInstance(packetClass, paramTypes, args);
        } catch (ReflectiveOperationException e) {
            LOGGER.fine("Failed to create packet: " + className + ": " + e.getMessage());
            return null;
        }
    }
}

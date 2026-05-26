package com.midgard.core.gui;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active GUI menus for players.
 */
public final class GuiManager {

    private static final Map<UUID, GuiMenu> activeMenus = new ConcurrentHashMap<>();

    private GuiManager() {
    }

    public static void registerMenu(Player player, GuiMenu menu) {
        activeMenus.put(player.getUniqueId(), menu);
    }

    public static void unregisterMenu(Player player) {
        activeMenus.remove(player.getUniqueId());
    }

    public static GuiMenu getMenu(Player player) {
        return activeMenus.get(player.getUniqueId());
    }

    public static boolean hasMenu(Player player) {
        return activeMenus.containsKey(player.getUniqueId());
    }
}

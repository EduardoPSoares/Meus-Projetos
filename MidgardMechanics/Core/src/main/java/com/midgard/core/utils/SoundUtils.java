package com.midgard.core.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Sound utilities for common game sounds.
 */
public final class SoundUtils {

    private SoundUtils() {
    }

    public static void play(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void play(Player player, Sound sound) {
        play(player, sound, 1.0f, 1.0f);
    }

    public static void success(Player player) {
        play(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }

    public static void error(Player player) {
        play(player, Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
    }

    public static void click(Player player) {
        play(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    public static void ding(Player player) {
        play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
    }

    public static void pop(Player player) {
        play(player, Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
    }
}

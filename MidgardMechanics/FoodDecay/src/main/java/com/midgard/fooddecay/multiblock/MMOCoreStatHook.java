package com.midgard.fooddecay.multiblock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;

/**
 * Reflection-based bridge for MythicLib/MMOCore stat modifiers.
 * Safe to call even when the integration is unavailable.
 */
public final class MMOCoreStatHook {

    private static Boolean available;
    private static Method getPlayerDataMethod;
    private static Method getStatMapMethod;
    private static Method statMapGetInstanceMethod;
    private static Method statInstanceRemoveIfMethod;
    private static Constructor<?> statModifierConstructor;
    private static Method statModifierRegisterMethod;
    private static Object flatModifierType;

    private MMOCoreStatHook() {
    }

    public static boolean isAvailable() {
        if (available == null) {
            available = init();
        }
        return available;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean init() {
        try {
            Plugin mmocore = Bukkit.getPluginManager().getPlugin("MMOCore");
            Plugin mythicLib = Bukkit.getPluginManager().getPlugin("MythicLib");
            boolean hasRuntime = (mmocore != null && mmocore.isEnabled())
                    || (mythicLib != null && mythicLib.isEnabled());
            if (!hasRuntime) {
                return false;
            }

            Class<?> playerDataClass = Class.forName("io.lumine.mythic.lib.api.player.MMOPlayerData");
            getPlayerDataMethod = playerDataClass.getMethod("get", Player.class);
            getStatMapMethod = playerDataClass.getMethod("getStatMap");

            Class<?> statMapClass = getStatMapMethod.getReturnType();
            statMapGetInstanceMethod = statMapClass.getMethod("getInstance", String.class);

            Class<?> statInstanceClass = statMapGetInstanceMethod.getReturnType();
            statInstanceRemoveIfMethod = statInstanceClass.getMethod("removeIf", Predicate.class);

            Class<?> modifierTypeClass = Class.forName("io.lumine.mythic.lib.api.stat.ModifierType");
            Class<?> statModifierClass = Class.forName("io.lumine.mythic.lib.api.stat.StatModifier");
            statModifierConstructor = statModifierClass.getConstructor(
                    String.class, String.class, double.class, modifierTypeClass
            );
            statModifierRegisterMethod = statModifierClass.getMethod("register", playerDataClass);
            flatModifierType = Enum.valueOf((Class<? extends Enum>) modifierTypeClass, "FLAT");
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO,
                    "[FoodDecay] MMOCore/MythicLib stats not found or incompatible, nutrition stat bonuses disabled.");
            return false;
        }
    }

    public static void clearBonuses(Player player, Collection<String> stats, String keyPrefix) {
        if (!isAvailable() || player == null || stats == null || stats.isEmpty()) {
            return;
        }

        try {
            Object playerData = getPlayerDataMethod.invoke(null, player);
            Object statMap = getStatMapMethod.invoke(playerData);
            Predicate<String> predicate = key -> key != null && key.startsWith(keyPrefix);

            for (String statId : stats) {
                String normalized = normalizeStat(statId);
                if (normalized == null) {
                    continue;
                }

                Object statInstance = statMapGetInstanceMethod.invoke(statMap, normalized);
                if (statInstance != null) {
                    statInstanceRemoveIfMethod.invoke(statInstance, predicate);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING,
                    "[FoodDecay] Failed to clear nutrition MMOCore stats", e);
        }
    }

    public static void applyBonuses(Player player, Map<String, Double> stats, String keyPrefix) {
        if (!isAvailable() || player == null || stats == null || stats.isEmpty()) {
            return;
        }

        try {
            Object playerData = getPlayerDataMethod.invoke(null, player);
            for (Map.Entry<String, Double> entry : stats.entrySet()) {
                String statId = normalizeStat(entry.getKey());
                double value = entry.getValue() != null ? entry.getValue() : 0D;
                if (statId == null || Math.abs(value) < 0.000001D) {
                    continue;
                }

                Object modifier = statModifierConstructor.newInstance(
                        keyPrefix + statId,
                        statId,
                        value,
                        flatModifierType
                );
                statModifierRegisterMethod.invoke(modifier, playerData);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING,
                    "[FoodDecay] Failed to apply nutrition MMOCore stats", e);
        }
    }

    private static String normalizeStat(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }
}

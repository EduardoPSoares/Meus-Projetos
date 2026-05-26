package com.midgard.fooddecay.multiblock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Reflection-based hook for MMOCore integration.
 * All calls are safe even if MMOCore is not installed.
 */
public final class MMOCoreHook {

    private static Boolean available;
    private static Method getPlayerDataMethod;
    private static Method getExperienceMethod;
    private static Method getProfessionLevelMethod;
    private static Method giveExperienceMethod;
    private static Method getProfessionManagerMethod;
    private static Method getAllProfessionsMethod;
    private static Method getProfessionIdMethod;
    private static Object mmoCorePlugin;

    private MMOCoreHook() {}

    public static boolean isAvailable() {
        if (available == null) {
            available = init();
        }
        return available;
    }

    private static boolean init() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("MMOCore");
            if (plugin == null || !plugin.isEnabled()) return false;

            mmoCorePlugin = plugin;

            // PlayerData.get(Player)
            Class<?> playerDataClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            getPlayerDataMethod = playerDataClass.getMethod("get", Player.class);

            // playerData.getCollectionSkills().getLevel(profession)
            // OR playerData.getProfession().getLevel(professionId)
            // MMOCore API: PlayerData.getExperience() / PlayerData.getLevel()
            // For professions: playerData.getCollectionSkills().getLevel(String professionId)
            Class<?> collClass = playerDataClass.getMethod("getCollectionSkills").getReturnType();
            getExperienceMethod = playerDataClass.getMethod("getCollectionSkills");
            getProfessionLevelMethod = collClass.getMethod("getLevel", String.class);
            giveExperienceMethod = collClass.getMethod("giveExperience",
                    String.class, double.class,
                    Class.forName("net.Indyuce.mmocore.api.quest.trigger.api.ExperienceTableCause"));

            // Profession manager for listing professions
            Class<?> mmoCoreClass = plugin.getClass();
            getProfessionManagerMethod = mmoCoreClass.getMethod("getProfessionManager");
            Object profMgr = getProfessionManagerMethod.invoke(plugin);
            getAllProfessionsMethod = profMgr.getClass().getMethod("getAll");

            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO,
                    "[FoodDecay] MMOCore not found or incompatible, MMOCore features disabled.");
            return false;
        }
    }

    /**
     * Gets the player's level in a specific profession.
     * Returns 0 if MMOCore is unavailable or the profession doesn't exist.
     */
    public static int getProfessionLevel(Player player, String professionId) {
        if (!isAvailable()) return 0;
        try {
            Object playerData = getPlayerDataMethod.invoke(null, player);
            Object collSkills = getExperienceMethod.invoke(playerData);
            return (int) getProfessionLevelMethod.invoke(collSkills, professionId);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Gives profession experience to a player.
     */
    public static void giveExperience(Player player, String professionId, double amount) {
        if (!isAvailable() || amount <= 0) return;
        try {
            Object playerData = getPlayerDataMethod.invoke(null, player);
            Object collSkills = getExperienceMethod.invoke(playerData);
            // null cause = generic source
            giveExperienceMethod.invoke(collSkills, professionId, amount, null);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[FoodDecay] Failed to give MMOCore experience", e);
        }
    }

    /**
     * Returns all registered profession IDs.
     */
    @SuppressWarnings("unchecked")
    public static List<String> getProfessionIds() {
        if (!isAvailable()) return List.of();
        try {
            Object profMgr = getProfessionManagerMethod.invoke(mmoCorePlugin);
            Collection<?> professions = (Collection<?>) getAllProfessionsMethod.invoke(profMgr);
            List<String> ids = new ArrayList<>();
            for (Object prof : professions) {
                Method getIdMethod = prof.getClass().getMethod("getId");
                ids.add((String) getIdMethod.invoke(prof));
            }
            return ids;
        } catch (Exception e) {
            return List.of();
        }
    }
}

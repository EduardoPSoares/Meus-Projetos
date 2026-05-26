package me.ray.midgard.modules.spells.manager;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.data.SpellProfile;
import me.ray.midgard.modules.spells.obj.Spell;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class SpellXPManager {

    private final SpellsModule module;

    private double baseXpPerCast = 10.0;
    private double mobKillBonus = 25.0;
    private double pvpBonus = 50.0;
    private double retentionRate = 0.5;
    private boolean enabled = true;
    private int[] xpPerLevelTable;

    public SpellXPManager(SpellsModule module) {
        this.module = module;
        loadConfig();
    }

    public void loadConfig() {
        ConfigurationSection xpSection = module.getConfig() != null
                ? module.getConfig().getConfigurationSection("spell-xp")
                : null;

        if (xpSection != null) {
            enabled = xpSection.getBoolean("enabled", true);
            baseXpPerCast = xpSection.getDouble("base-per-cast", 10.0);
            mobKillBonus = xpSection.getDouble("mob-kill-bonus", 25.0);
            pvpBonus = xpSection.getDouble("pvp-bonus", 50.0);
            retentionRate = xpSection.getDouble("retention-rate", 0.5);

            if (xpSection.isList("xp-per-level")) {
                List<Integer> list = xpSection.getIntegerList("xp-per-level");
                xpPerLevelTable = new int[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    xpPerLevelTable[i] = list.get(i);
                }
            }
        }

        if (xpPerLevelTable == null || xpPerLevelTable.length == 0) {
            // Default: 100 * level^1.5
            xpPerLevelTable = new int[31];
            for (int i = 0; i <= 30; i++) {
                xpPerLevelTable[i] = (int) (100 * Math.pow(Math.max(1, i), 1.5));
            }
        }
    }

    public void grantCastXP(Player player, Spell spell, boolean mobDied, boolean isPvP) {
        if (!enabled) { return; }

        SpellProfile profile = module.getSpellManager().getProfile(player);
        if (profile == null) { return; }

        String spellId = spell.getId();
        int currentLevel = profile.getSpellLevel(spellId);
        if (currentLevel >= spell.getMaxLevel()) { return; }

        double xpGained = baseXpPerCast;
        if (mobDied) { xpGained += mobKillBonus; }
        if (isPvP) { xpGained += pvpBonus; }

        profile.addSpellXP(spellId, xpGained);
        checkLevelUp(player, profile, spell);
    }

    /**
     * Grant only the kill/PvP bonus XP (no base cast XP) to avoid double-granting.
     * Called from SpellDamageListener when a mob dies after a spell cast.
     */
    public void grantKillBonusXP(Player player, Spell spell, boolean isPvP) {
        if (!enabled) { return; }

        SpellProfile profile = module.getSpellManager().getProfile(player);
        if (profile == null) { return; }

        String spellId = spell.getId();
        int currentLevel = profile.getSpellLevel(spellId);
        if (currentLevel >= spell.getMaxLevel()) { return; }

        double xpGained = mobKillBonus;
        if (isPvP) { xpGained += pvpBonus; }

        profile.addSpellXP(spellId, xpGained);
        checkLevelUp(player, profile, spell);
    }

    private void checkLevelUp(Player player, SpellProfile profile, Spell spell) {
        String spellId = spell.getId();
        int currentLevel = profile.getSpellLevel(spellId);
        double currentXP = profile.getSpellXP(spellId);

        int xpNeeded = getXPForLevel(currentLevel + 1);

        while (currentXP >= xpNeeded && currentLevel < spell.getMaxLevel()) {
            currentXP -= xpNeeded;
            currentLevel++;
            profile.setSpellLevel(spellId, currentLevel);
            profile.setSpellXP(spellId, currentXP);

            onLevelUp(player, spell, currentLevel);

            if (currentLevel >= spell.getMaxLevel()) { break; }
            xpNeeded = getXPForLevel(currentLevel + 1);
        }
    }

    private void onLevelUp(Player player, Spell spell, int newLevel) {
        String msg = module.getMessage("xp.level_up")
                .replace("%spell%", spell.getDisplayName())
                .replace("%level%", String.valueOf(newLevel));
        MessageUtils.send(player, msg);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

        // Check milestones
        module.getSpellManager().checkMilestone(player, spell, newLevel);

        // Check mastery
        if (newLevel >= spell.getMaxLevel()) {
            module.getSpellManager().grantMastery(player, spell);
        }
    }

    public int getXPForLevel(int level) {
        if (level <= 0 || level > 30) { return Integer.MAX_VALUE; }
        if (level < xpPerLevelTable.length) {
            return Math.max(1, xpPerLevelTable[level]);
        }
        return Math.max(1, (int) (100 * Math.pow(level, 1.5)));
    }

    public int getStartingLevelFromMemory(int rememberedLevel) {
        return Math.max(1, (int) Math.floor(rememberedLevel * retentionRate));
    }

    public double getRetentionRate() {
        return retentionRate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}

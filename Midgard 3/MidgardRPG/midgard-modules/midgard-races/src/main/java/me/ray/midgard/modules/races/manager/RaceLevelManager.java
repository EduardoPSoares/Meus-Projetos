package me.ray.midgard.modules.races.manager;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.event.PlayerRaceLevelUpEvent;
import me.ray.midgard.modules.races.model.Race;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

public class RaceLevelManager {

    private final RacesModule module;
    private double baseXp;
    private double xpMultiplier;
    private int maxLevel;

    public RaceLevelManager(RacesModule module) {
        this.module = module;
        loadConfig();
    }

    public void loadConfig() {
        this.maxLevel = module.getConfig().getInt("leveling.max-level", 50);
        this.baseXp = module.getConfig().getDouble("leveling.base-xp", 100);
        this.xpMultiplier = module.getConfig().getDouble("leveling.multiplier", 1.25);
    }

    public void addExperience(Player player, double amount) {
        addExperience(player, amount, null);
    }

    public void addExperience(Player player, double amount, me.ray.midgard.modules.races.api.RaceXpSource source) {
        if (!module.getConfig().getBoolean("leveling.enabled", true)) { return; }

        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return; }

        RaceData data = profile.getData(RaceData.class);
        if (data == null || !data.hasRace()) { return; }

        // Apply XP source multiplier from race
        double finalAmount = amount;
        Race race = module.getRaceManager().getRace(data.getRaceId());
        if (race != null && source != null) {
            finalAmount *= race.getXpMultiplier(source);
        }

        // Apply Exp Boost Traits
        if (race != null && race.getTraits() != null) {
            for (me.ray.midgard.modules.races.model.ConfiguredTrait ct : race.getTraits()) {
                boolean isExpTrait = ct.getTrigger() == me.ray.midgard.modules.races.api.TraitTrigger.ON_EXP_GAIN;
                boolean isKillTrait = ct.getTrigger() == me.ray.midgard.modules.races.api.TraitTrigger.ON_KILL
                        && source == me.ray.midgard.modules.races.api.RaceXpSource.COMBAT;
                if (!isExpTrait && !isKillTrait) { continue; }
                if (data.getLevel() < ct.getMinLevel()) { continue; }
                if (ct.isSelectable() && !data.hasMutation(ct.getId())) { continue; }
                if (!ct.getCondition().isMet(player)) { continue; }

                Map<String, Object> config = ct.getConfig();
                double multiplier = 1.0;
                if (config.get("multiplier") instanceof Number n) {
                    multiplier = n.doubleValue();
                } else if (config.get("value") instanceof Number n) {
                    multiplier = 1.0 + (n.doubleValue() / 100.0);
                }

                double chance = 100.0;
                if (config.get("chance") instanceof Number n) {
                    chance = n.doubleValue();
                }

                if (Math.random() * 100 <= chance) {
                    finalAmount *= multiplier;
                }
            }
        }

        double newExp = data.getExperience() + finalAmount;
        if (Double.isNaN(newExp) || Double.isInfinite(newExp)) { return; }
        data.setExperience(newExp);
        checkLevelUp(player, data);
    }

    public void checkLevelUp(Player player, RaceData data) {
        if (data.getLevel() >= maxLevel) { return; }

        double required = getRequiredExperience(data.getLevel());
        if (required <= 0) { return; } // Guard against infinite recursion with zero/negative XP requirement
        if (data.getExperience() >= required) {
            // Level Up
            int oldLevel = data.getLevel();
            int newLevel = oldLevel + 1;
            
            data.setExperience(data.getExperience() - required);
            data.setLevel(newLevel);
            
            // Fire event
            Race race = module.getRaceManager().getRace(data.getRaceId());
            PlayerRaceLevelUpEvent event = new PlayerRaceLevelUpEvent(player, race, oldLevel, newLevel);
            Bukkit.getPluginManager().callEvent(event);
            
            MessageUtils.send(player, module.getMessage("event.level_up")
                .replace("%old%", String.valueOf(oldLevel))
                .replace("%new%", String.valueOf(newLevel)));
            
            // Recursively check if we gained enough for multiple levels (rare but possible)
            checkLevelUp(player, data);
        }
    }

    public double getRequiredExperience(int currentLevel) {
        return calculateRequiredXp(currentLevel, baseXp, xpMultiplier);
    }

    public static double calculateRequiredXp(int level, double base, double multiplier) {
        return base * Math.pow(multiplier, level - 1);
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setLevel(Player player, int newLevel) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return; }

        RaceData data = profile.getData(RaceData.class);
        if (data == null || !data.hasRace()) { return; }

        int clamped = Math.max(1, Math.min(newLevel, maxLevel));
        int oldLevel = data.getLevel();
        data.setLevel(clamped);
        data.setExperience(0);

        if (clamped > oldLevel) {
            Race race = module.getRaceManager().getRace(data.getRaceId());
            PlayerRaceLevelUpEvent event = new PlayerRaceLevelUpEvent(player, race, oldLevel, clamped);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    public void setExperience(Player player, double amount) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return; }

        RaceData data = profile.getData(RaceData.class);
        if (data == null || !data.hasRace()) { return; }

        double safe = Math.max(0, amount);
        if (Double.isNaN(safe) || Double.isInfinite(safe)) { return; }
        data.setExperience(safe);
        checkLevelUp(player, data);
    }
}

package me.ray.midgard.modules.races.listener;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.AttributeModifier;
import me.ray.midgard.core.attribute.AttributeOperation;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.event.PlayerChangeRaceEvent;
import me.ray.midgard.modules.races.event.PlayerRaceLevelUpEvent;
import me.ray.midgard.modules.races.model.Race;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;

public class RaceAttributeListener implements Listener {

    private final RacesModule module;
    private static final String MODIFIER_SOURCE_PREFIX = "RaceBonus:";

    public RaceAttributeListener(RacesModule module) {
        this.module = module;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            Player player = event.getPlayer();
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
            if (profile == null) { return; }

            RaceData data = profile.getData(RaceData.class);
            if (data != null && data.hasRace()) {
                Race race = module.getRaceManager().getRace(data.getRaceId());
                if (race != null) {
                    applyRaceAttributes(profile, race, data.getLevel());
                }
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao aplicar atributos de raça para %s", event.getPlayer().getName(), e);
        }
    }

    @EventHandler
    public void onChangeRace(PlayerChangeRaceEvent event) {
        try {
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(event.getPlayer());
            if (profile == null) { return; }

            if (event.getOldRace() != null) {
                removeRaceAttributes(profile, event.getOldRace());
            }

            if (event.getNewRace() != null) {
                RaceData data = profile.getData(RaceData.class);
                int level = (data != null) ? data.getLevel() : 1;
                applyRaceAttributes(profile, event.getNewRace(), level);
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao tratar mudança de raça para %s", event.getPlayer().getName(), e);
        }
    }

    @EventHandler
    public void onRaceLevelUp(PlayerRaceLevelUpEvent event) {
        try {
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(event.getPlayer());
            if (profile == null) { return; }

            applyRaceAttributes(profile, event.getRace(), event.getNewLevel());
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao aplicar atributos no level up para %s", event.getPlayer().getName(), e);
        }
    }

    private void applyRaceAttributes(MidgardProfile profile, Race race, int level) {
        CoreAttributeData attrData = profile.getOrCreateData(CoreAttributeData.class);
        Player player = profile.getPlayer();
        if (player == null) { return; }

        boolean isDay = RacesModule.isDayTime(player.getWorld().getTime());

        Map<String, Double> attrs = race.getAttributes();
        Map<String, Double> perLevel = race.getPerLevelAttributes();
        if (attrs == null) { attrs = Map.of(); }
        if (perLevel == null) { perLevel = Map.of(); }

        // Atributos condicionais ao horário
        Map<String, Double> timeAttrs = isDay ? race.getDayAttributes() : race.getNightAttributes();
        Map<String, Double> timePerLevel = isDay ? race.getDayPerLevelAttributes() : race.getNightPerLevelAttributes();
        if (timeAttrs == null) { timeAttrs = Map.of(); }
        if (timePerLevel == null) { timePerLevel = Map.of(); }

        // Coletar todas as chaves de atributo envolvidas (ambos os períodos para remover modifiers obsoletos)
        java.util.Set<String> allKeys = new java.util.HashSet<>();
        allKeys.addAll(attrs.keySet());
        allKeys.addAll(perLevel.keySet());
        allKeys.addAll(timeAttrs.keySet());
        allKeys.addAll(timePerLevel.keySet());
        // Incluir chaves do período oposto para garantir remoção de modifiers antigos
        Map<String, Double> oppositeAttrs = isDay ? race.getNightAttributes() : race.getDayAttributes();
        Map<String, Double> oppositePerLevel = isDay ? race.getNightPerLevelAttributes() : race.getDayPerLevelAttributes();
        if (oppositeAttrs != null) { allKeys.addAll(oppositeAttrs.keySet()); }
        if (oppositePerLevel != null) { allKeys.addAll(oppositePerLevel.keySet()); }

        for (String key : allKeys) {
            double base = attrs.getOrDefault(key, 0.0) + timeAttrs.getOrDefault(key, 0.0);
            Double perLevelVal = perLevel.get(key);
            Double timePerLevelVal = timePerLevel.get(key);
            double combinedPerLevel = (perLevelVal != null ? perLevelVal : 0.0)
                    + (timePerLevelVal != null ? timePerLevelVal : 0.0);
            Double finalPerLevel = combinedPerLevel != 0 ? combinedPerLevel : null;
            applyAttribute(player, attrData, race.getId(), key, base, finalPerLevel, level);
        }
    }

    /**
     * Reaplica atributos da raça para um jogador. Chamado na transição dia/noite.
     */
    public void refreshAttributes(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return; }

        RaceData data = profile.getData(RaceData.class);
        if (data == null || !data.hasRace()) { return; }

        Race race = module.getRaceManager().getRace(data.getRaceId());
        if (race == null || !race.hasTimeAttributes()) { return; }

        applyRaceAttributes(profile, race, data.getLevel());
    }

    private void applyAttribute(Player player, CoreAttributeData attrData, String raceId, String attrKey, double baseValue, Double perLevelValue, int level) {
        String attrId = attrKey.toLowerCase();
        double totalValue = baseValue;
        
        if (perLevelValue != null) {
            totalValue += perLevelValue * (level - 1);
        }

        // 1. Try Custom RPG Attribute first
        AttributeInstance instance = attrData.getInstance(attrId);
        if (instance != null) {
            String modName = MODIFIER_SOURCE_PREFIX + raceId;
            instance.removeModifier(modName);
            if (totalValue != 0) {
                instance.addModifier(new AttributeModifier(
                        modName,
                        totalValue,
                        AttributeOperation.ADD_NUMBER
                ));
            }
            return;
        }

        // 2. Try Vanilla Attribute
        try {
            String mappedName = mapAttributeName(attrKey);
            org.bukkit.attribute.Attribute bukkitAttr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(mappedName.toLowerCase()));
            if (bukkitAttr == null) { return; }
            org.bukkit.attribute.AttributeInstance vanillaInstance = player.getAttribute(bukkitAttr);
            
            if (vanillaInstance != null) {
                NamespacedKey modifierKey = new NamespacedKey("midgard", "race_bonus_" + raceId.toLowerCase() + "_" + mappedName.toLowerCase());
                
                for (org.bukkit.attribute.AttributeModifier mod : new java.util.ArrayList<>(vanillaInstance.getModifiers())) {
                    if (mod.getKey().equals(modifierKey)) {
                        vanillaInstance.removeModifier(mod);
                    }
                }
                
                if (totalValue == 0) { return; }

                vanillaInstance.addModifier(new org.bukkit.attribute.AttributeModifier(
                        modifierKey,
                        totalValue,
                        org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
                ));
            }
        } catch (IllegalArgumentException ignored) { /* Atributo pode não existir na versão atual */ }
    }

    private void removeRaceAttributes(MidgardProfile profile, Race race) {
        CoreAttributeData attrData = profile.getOrCreateData(CoreAttributeData.class);
        Player player = profile.getPlayer();
        if (player == null) { return; }
        
        // 1. Remove all Custom RPG Modifiers that start with our prefix
        for (AttributeInstance instance : attrData.getInstances().values()) {
            instance.removeModifier(MODIFIER_SOURCE_PREFIX + race.getId());
        }

        // 2. Remove all Vanilla Modifiers that belong to this race
        for (org.bukkit.attribute.Attribute bukkitAttr : Registry.ATTRIBUTE) {
            try {
                org.bukkit.attribute.AttributeInstance vanillaInstance = player.getAttribute(bukkitAttr);
                if (vanillaInstance != null) {
                    String raceIdPrefix = "race_bonus_" + race.getId().toLowerCase() + "_";
                    for (org.bukkit.attribute.AttributeModifier mod : new java.util.ArrayList<>(vanillaInstance.getModifiers())) {
                        if (mod.getKey().getNamespace().equals("midgard") && mod.getKey().getKey().startsWith(raceIdPrefix)) {
                            vanillaInstance.removeModifier(mod);
                        }
                    }
                }
            } catch (Exception ignored) { /* Remoção de modifier pode falhar se atributo inválido */ }
        }
    }

    private String mapAttributeName(String name) {
        name = name.toUpperCase();
        if (name.startsWith("GENERIC_") || name.startsWith("PLAYER_")) { return name; }

        switch (name) {
            case "MAX_HEALTH": return "GENERIC_MAX_HEALTH";
            case "FOLLOW_RANGE": return "GENERIC_FOLLOW_RANGE";
            case "KNOCKBACK_RESISTANCE": return "GENERIC_KNOCKBACK_RESISTANCE";
            case "MOVEMENT_SPEED": return "GENERIC_MOVEMENT_SPEED";
            case "FLYING_SPEED": return "GENERIC_FLYING_SPEED";
            case "ATTACK_DAMAGE": return "GENERIC_ATTACK_DAMAGE";
            case "ATTACK_KNOCKBACK": return "GENERIC_ATTACK_KNOCKBACK";
            case "ATTACK_SPEED": return "GENERIC_ATTACK_SPEED";
            case "ARMOR": return "GENERIC_ARMOR";
            case "ARMOR_TOUGHNESS": return "GENERIC_ARMOR_TOUGHNESS";
            case "FALL_DAMAGE_MULTIPLIER": return "GENERIC_FALL_DAMAGE_MULTIPLIER";
            case "LUCK": return "GENERIC_LUCK";
            case "MAX_ABSORPTION": return "GENERIC_MAX_ABSORPTION";
            case "SAFE_FALL_DISTANCE": return "GENERIC_SAFE_FALL_DISTANCE";
            case "SCALE": return "GENERIC_SCALE";
            case "STEP_HEIGHT": return "GENERIC_STEP_HEIGHT";
            case "GRAVITY": return "GENERIC_GRAVITY";
            case "JUMP_STRENGTH": return "GENERIC_JUMP_STRENGTH";
            case "BURNING_TIME": return "GENERIC_BURNING_TIME";
            case "EXPLOSION_KNOCKBACK_RESISTANCE": return "GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE";
            case "WATER_MOVEMENT_EFFICIENCY": return "GENERIC_WATER_MOVEMENT_EFFICIENCY";
            case "MOVEMENT_EFFICIENCY": return "GENERIC_MOVEMENT_EFFICIENCY";
            case "OXYGEN_BONUS": return "GENERIC_OXYGEN_BONUS";
            case "SUBMERGED_MINING_SPEED": return "GENERIC_SUBMERGED_MINING_SPEED";
            case "SNEAKING_SPEED": return "GENERIC_SNEAKING_SPEED";
            case "MINING_EFFICIENCY": return "PLAYER_MINING_EFFICIENCY";
            case "SNEAK_SPEED": return "PLAYER_SNEAK_SPEED";
            case "SUBMERGED_MINING_EFFICIENCY": return "PLAYER_SUBMERGED_MINING_EFFICIENCY";
            case "SWEEPING_DAMAGE_RATIO": return "PLAYER_SWEEPING_DAMAGE_RATIO";
            case "BLOCK_BREAK_SPEED": return "PLAYER_BLOCK_BREAK_SPEED";
            case "BLOCK_INTERACTION_RANGE": return "PLAYER_BLOCK_INTERACTION_RANGE";
            case "ENTITY_INTERACTION_RANGE": return "PLAYER_ENTITY_INTERACTION_RANGE";
            default: return "GENERIC_" + name;
        }
    }
}

package me.ray.midgard.modules.item.ability;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Representa uma vinculação de spell a um item.
 * Define qual spell é ativada, em qual trigger, e quaisquer overrides.
 */
public class SpellBinding {

    private final String spellId;
    private final AbilityTrigger trigger;
    private final double cooldownOverride;
    private final double damageOverride;
    private final int timerTicks;
    private final Map<String, Double> variableOverrides;

    public SpellBinding(String spellId, AbilityTrigger trigger) {
        this(spellId, trigger, -1, -1, 0, new HashMap<>());
    }

    public SpellBinding(String spellId, AbilityTrigger trigger, double cooldownOverride, 
                        double damageOverride, int timerTicks, Map<String, Double> variableOverrides) {
        this.spellId = spellId.toLowerCase();
        this.trigger = trigger;
        this.cooldownOverride = cooldownOverride;
        this.damageOverride = damageOverride;
        this.timerTicks = timerTicks;
        this.variableOverrides = variableOverrides != null ? variableOverrides : new HashMap<>();
    }

    /**
     * Cria um SpellBinding a partir de uma seção de configuração.
     */
    public static SpellBinding fromConfig(ConfigurationSection section) {
        String spellId = section.getString("spell", "");
        String triggerStr = section.getString("trigger", "LEFT_CLICK");
        AbilityTrigger trigger = AbilityTrigger.fromString(triggerStr);
        
        double cooldown = section.getDouble("cooldown-override", -1);
        double damage = section.getDouble("damage-override", -1);
        int timer = section.getInt("timer", 0);
        
        Map<String, Double> overrides = new HashMap<>();
        if (section.isConfigurationSection("variables")) {
            ConfigurationSection vars = section.getConfigurationSection("variables");
            for (String key : vars.getKeys(false)) {
                overrides.put(key, vars.getDouble(key));
            }
        }
        
        return new SpellBinding(spellId, trigger, cooldown, damage, timer, overrides);
    }

    /**
     * Cria um SpellBinding a partir de um Map (usado pelo importer).
     */
    @SuppressWarnings("unchecked")
    public static SpellBinding fromMap(Map<String, Object> map) {
        String spellId = (String) map.getOrDefault("spell", "");
        String triggerStr = (String) map.getOrDefault("trigger", "LEFT_CLICK");
        AbilityTrigger trigger = AbilityTrigger.fromString(triggerStr);
        
        double cooldown = -1;
        if (map.containsKey("cooldown-override")) {
            Object raw = map.get("cooldown-override");
            if (raw instanceof Number) {
                cooldown = ((Number) raw).doubleValue();
            } else {
                try { cooldown = Double.parseDouble(String.valueOf(raw)); } catch (NumberFormatException ignored) { /* Invalid number format */ }
            }
        }
        double damage = -1;
        if (map.containsKey("damage-override")) {
            Object raw = map.get("damage-override");
            if (raw instanceof Number) {
                damage = ((Number) raw).doubleValue();
            } else {
                try { damage = Double.parseDouble(String.valueOf(raw)); } catch (NumberFormatException ignored) { /* Invalid number format */ }
            }
        }
        int timer = 0;
        if (map.containsKey("timer")) {
            Object raw = map.get("timer");
            if (raw instanceof Number) {
                timer = ((Number) raw).intValue();
            } else {
                try { timer = Integer.parseInt(String.valueOf(raw)); } catch (NumberFormatException ignored) { /* Invalid number format */ }
            }
        }
        
        Map<String, Double> overrides = new HashMap<>();
        if (map.containsKey("variables") && map.get("variables") instanceof Map) {
            Map<String, Object> vars = (Map<String, Object>) map.get("variables");
            for (Map.Entry<String, Object> entry : vars.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    overrides.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                }
            }
        }
        
        return new SpellBinding(spellId, trigger, cooldown, damage, timer, overrides);
    }

    public String getSpellId() {
        return spellId;
    }

    public AbilityTrigger getTrigger() {
        return trigger;
    }

    public boolean hasCooldownOverride() {
        return cooldownOverride > 0;
    }

    public double getCooldownOverride() {
        return cooldownOverride;
    }

    public boolean hasDamageOverride() {
        return damageOverride > 0;
    }

    public double getDamageOverride() {
        return damageOverride;
    }

    public int getTimerTicks() {
        return timerTicks;
    }

    public boolean isPassive() {
        return trigger == AbilityTrigger.PASSIVE_TIMER;
    }

    public Map<String, Double> getVariableOverrides() {
        return variableOverrides;
    }

    @Override
    public String toString() {
        return "SpellBinding{spell=" + spellId + ", trigger=" + trigger + "}";
    }
}

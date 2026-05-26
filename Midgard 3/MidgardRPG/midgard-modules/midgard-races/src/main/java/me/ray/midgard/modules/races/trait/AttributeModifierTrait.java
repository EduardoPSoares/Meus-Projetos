package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.Map;

public class AttributeModifierTrait implements RaceTrait {

    @Override
    public String getId() {
        return "attribute_modifier";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        Object attrObj = config.get("attribute");
        if (!(attrObj instanceof String attributeName)) { return; }
        
        // Simple mapper for common names
        if (attributeName.equalsIgnoreCase("STRENGTH")) { attributeName = "GENERIC_ATTACK_DAMAGE"; }
        if (attributeName.equalsIgnoreCase("HEALTH")) { attributeName = "GENERIC_MAX_HEALTH"; }
        if (attributeName.equalsIgnoreCase("MAX_HEALTH")) { attributeName = "GENERIC_MAX_HEALTH"; }
        if (attributeName.equalsIgnoreCase("SPEED")) { attributeName = "GENERIC_MOVEMENT_SPEED"; }
        if (attributeName.equalsIgnoreCase("MOVEMENT_SPEED")) { attributeName = "GENERIC_MOVEMENT_SPEED"; }
        if (attributeName.equalsIgnoreCase("KNOCKBACK_RESISTANCE")) { attributeName = "GENERIC_KNOCKBACK_RESISTANCE"; }
        
        Attribute attribute = getAttribute(attributeName);
        
        if (attribute == null) { return; }

        double value = 0.0;
        if (config.containsKey("value")) {
             Object valObj = config.get("value");
             if (valObj instanceof Number) {
                 value = ((Number) valObj).doubleValue();
             }
        }
        
        Object operationObj = config.getOrDefault("operation", "ADD_NUMBER");
        String operationStr = operationObj instanceof String s ? s : "ADD_NUMBER";
        AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
        try {
            operation = AttributeModifier.Operation.valueOf(operationStr.toUpperCase());
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.warn("Operação inválida '%s', usando ADD_NUMBER", operationStr);
        }

        Object traitIdObj = config.getOrDefault("trait_id", "generic_race_trait");
        String traitId = traitIdObj instanceof String s ? s : "generic_race_trait";
        NamespacedKey modifierKey = createKey("race_" + traitId.toLowerCase());

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) { return; }

        if (trigger == TraitTrigger.ON_REMOVE || trigger == TraitTrigger.ON_QUIT) {
            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.getKey().equals(modifierKey)) {
                    instance.removeModifier(modifier);
                }
            }
            return;
        }

        // For APPLY triggers (JOIN, SELECT, PASSIVE_TICK)
        boolean exists = false;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.getKey().equals(modifierKey)) {
                exists = true;
                // We could verify value and update if needed, but for now assume static config
                if (modifier.getAmount() != value || modifier.getOperation() != operation) {
                    instance.removeModifier(modifier);
                    exists = false;
                }
                break;
            }
        }

        if (!exists) {
            AttributeModifier modifier = createModifier(modifierKey, value, operation);
            instance.addModifier(modifier);
        }
    }

    protected Attribute getAttribute(String name) {
        try {
            return Registry.ATTRIBUTE.get(NamespacedKey.minecraft(name.toLowerCase()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    protected NamespacedKey createKey(String key) {
        return new NamespacedKey("midgard", key);
    }

    protected AttributeModifier createModifier(NamespacedKey key, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(key, amount, operation);
    }
}

package me.ray.midgard.core.effect;

import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.AttributeModifier;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.script.ScriptEngine;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActiveEffect {

    private final StatusEffect effect;
    private final UUID applierId;
    private long remainingDuration;
    private long ticksSinceLastTick;
    
    private final List<AppliedModifier> appliedModifiers = new ArrayList<>();
    private transient boolean initialized = false;

    public ActiveEffect(StatusEffect effect, long duration, UUID applierId) {
        this.effect = effect;
        this.remainingDuration = duration;
        this.applierId = applierId;
    }

    public void start(MidgardProfile profile) {
        Player player = profile.getPlayer();
        if (player != null) {
            ScriptEngine.executeActions(player, effect.getOnStartActions());
        }
        applyModifiers(profile);
        this.initialized = true;
    }
    
    public void resume(MidgardProfile profile) {
        applyModifiers(profile);
        this.initialized = true;
    }

    private void applyModifiers(MidgardProfile profile) {
        CoreAttributeData data = profile.getData(CoreAttributeData.class);
        if (data != null) {
            for (StatusEffect.EffectModifier em : effect.getAttributeModifiers()) {
                AttributeInstance instance = data.getInstance(em.getAttributeId());
                if (instance != null) {
                    AttributeModifier template = em.getModifier();
                    AttributeModifier newMod = new AttributeModifier(
                            template.getName(),
                            template.getAmount(),
                            template.getOperation()
                    );
                    instance.addModifier(newMod);
                    appliedModifiers.add(new AppliedModifier(em.getAttributeId(), newMod));
                }
            }
        }
    }

    public boolean tick(MidgardProfile profile) {
        if (!initialized) {
            resume(profile);
        }
        
        remainingDuration--;
        ticksSinceLastTick++;

        if (effect.getTickInterval() > 0 && ticksSinceLastTick >= effect.getTickInterval()) {
            ticksSinceLastTick = 0;
            Player player = profile.getPlayer();
            if (player != null) {
                ScriptEngine.executeActions(player, effect.getOnTickActions());
            }
        }

        return remainingDuration <= 0;
    }

    public void end(MidgardProfile profile) {
        Player player = profile.getPlayer();
        if (player != null) {
            ScriptEngine.executeActions(player, effect.getOnEndActions());
        }
        removeModifiers(profile);
    }
    
    public void removeModifiers(MidgardProfile profile) {
        CoreAttributeData data = profile.getData(CoreAttributeData.class);
        if (data != null) {
            for (AppliedModifier am : appliedModifiers) {
                AttributeInstance instance = data.getInstance(am.attributeId);
                if (instance != null) {
                    instance.removeModifier(am.modifier);
                }
            }
        }
        appliedModifiers.clear();
    }
    
    public StatusEffect getEffect() {
        return effect;
    }
    
    public long getRemainingDuration() {
        return remainingDuration;
    }
    
    public UUID getApplierId() {
        return applierId;
    }

    private static class AppliedModifier {
        final String attributeId;
        final AttributeModifier modifier;

        AppliedModifier(String attributeId, AttributeModifier modifier) {
            this.attributeId = attributeId;
            this.modifier = modifier;
        }
    }
}

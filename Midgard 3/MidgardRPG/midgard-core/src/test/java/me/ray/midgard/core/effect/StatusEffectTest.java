package me.ray.midgard.core.effect;

import me.ray.midgard.core.attribute.AttributeModifier;
import me.ray.midgard.core.attribute.AttributeOperation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatusEffectTest {

    @Test
    void shouldCreateWithIdAndType() {
        StatusEffect effect = new StatusEffect("poison", StatusEffect.EffectType.DEBUFF);
        assertEquals("poison", effect.getId());
        assertEquals(StatusEffect.EffectType.DEBUFF, effect.getType());
    }

    @Test
    void shouldSetAndGetDisplayName() {
        StatusEffect effect = new StatusEffect("regen", StatusEffect.EffectType.BUFF);
        effect.setDisplayName("Regeneração");
        assertEquals("Regeneração", effect.getDisplayName());
    }

    @Test
    void shouldSetAndGetDuration() {
        StatusEffect effect = new StatusEffect("fire", StatusEffect.EffectType.DEBUFF);
        effect.setDefaultDuration(200);
        assertEquals(200, effect.getDefaultDuration());
    }

    @Test
    void shouldSetAndGetTickInterval() {
        StatusEffect effect = new StatusEffect("regen", StatusEffect.EffectType.BUFF);
        effect.setTickInterval(20);
        assertEquals(20, effect.getTickInterval());
    }

    @Test
    void shouldAddAttributeModifier() {
        StatusEffect effect = new StatusEffect("strength_buff", StatusEffect.EffectType.BUFF);
        AttributeModifier mod = new AttributeModifier("StrBuff", 10.0, AttributeOperation.ADD_NUMBER);
        effect.addAttributeModifier("str", mod);

        List<StatusEffect.EffectModifier> modifiers = effect.getAttributeModifiers();
        assertEquals(1, modifiers.size());
        assertEquals("str", modifiers.get(0).getAttributeId());
        assertEquals(mod, modifiers.get(0).getModifier());
    }

    @Test
    void shouldManageActionLists() {
        StatusEffect effect = new StatusEffect("test", StatusEffect.EffectType.NEUTRAL);
        effect.getOnStartActions().add("msg Hello");
        effect.getOnTickActions().add("particle flame");
        effect.getOnEndActions().add("msg Goodbye");

        assertEquals(1, effect.getOnStartActions().size());
        assertEquals(1, effect.getOnTickActions().size());
        assertEquals(1, effect.getOnEndActions().size());
        assertEquals("msg Hello", effect.getOnStartActions().get(0));
    }

    @Test
    void shouldHaveAllEffectTypes() {
        assertEquals(3, StatusEffect.EffectType.values().length);
        assertNotNull(StatusEffect.EffectType.valueOf("BUFF"));
        assertNotNull(StatusEffect.EffectType.valueOf("DEBUFF"));
        assertNotNull(StatusEffect.EffectType.valueOf("NEUTRAL"));
    }
}

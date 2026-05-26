package me.ray.midgard.modules.races.model;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguredTraitTest {

    @Test
    @DisplayName("Construtor simples deve usar defaults para selectable, exclusionGroup e condition")
    void shouldUseDefaults() {
        RaceTrait trait = mock(RaceTrait.class);
        ConfiguredTrait ct = new ConfiguredTrait("night_vision", trait,
                TraitTrigger.PASSIVE_TICK, 5, Map.of("power", 2));

        assertEquals("night_vision", ct.getId());
        assertSame(trait, ct.getTrait());
        assertEquals(TraitTrigger.PASSIVE_TICK, ct.getTrigger());
        assertEquals(5, ct.getMinLevel());
        assertEquals(2, ct.getConfig().get("power"));
        assertFalse(ct.isSelectable());
        assertNull(ct.getExclusionGroup());
        assertSame(TraitCondition.ALWAYS, ct.getCondition());
    }

    @Test
    @DisplayName("Construtor com selectable e exclusionGroup")
    void shouldStoreSelectableAndExclusionGroup() {
        RaceTrait trait = mock(RaceTrait.class);
        ConfiguredTrait ct = new ConfiguredTrait("darkvision", trait,
                TraitTrigger.ON_SELECT, 10, Map.of(), true, "vision_group");

        assertTrue(ct.isSelectable());
        assertEquals("vision_group", ct.getExclusionGroup());
        assertSame(TraitCondition.ALWAYS, ct.getCondition());
    }

    @Test
    @DisplayName("Construtor completo com TraitCondition")
    void shouldStoreCondition() {
        RaceTrait trait = mock(RaceTrait.class);
        TraitCondition dayCond = TraitCondition.fromString("DAY");
        ConfiguredTrait ct = new ConfiguredTrait("sun_strength", trait,
                TraitTrigger.PASSIVE_TICK, 1, Map.of("value", 5),
                false, null, dayCond);

        assertEquals(TraitCondition.TimeRule.DAY, ct.getCondition().time());
        assertFalse(ct.isSelectable());
        assertNull(ct.getExclusionGroup());
    }

    @Test
    @DisplayName("config deve ser acessível")
    void shouldAccessConfig() {
        RaceTrait trait = mock(RaceTrait.class);
        Map<String, Object> config = Map.of("multiplier", 1.5, "chance", 50);
        ConfiguredTrait ct = new ConfiguredTrait("lifesteal", trait,
                TraitTrigger.ON_ATTACK, 15, config);

        assertEquals(1.5, ct.getConfig().get("multiplier"));
        assertEquals(50, ct.getConfig().get("chance"));
    }
}

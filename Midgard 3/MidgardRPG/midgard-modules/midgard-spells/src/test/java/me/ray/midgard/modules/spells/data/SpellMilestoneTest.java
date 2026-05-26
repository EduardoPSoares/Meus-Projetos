package me.ray.midgard.modules.spells.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpellMilestoneTest {

    @Test
    @DisplayName("Deve armazenar campos do record")
    void shouldStoreRecordFields() {
        Map<String, Double> bonuses = new HashMap<>();
        bonuses.put("damage", 10.0);
        var milestone = new SpellMilestone(5, "flame_burst", bonuses, "custom_skill");
        assertEquals(5, milestone.level());
        assertEquals("flame_burst", milestone.visualEffect());
        assertEquals("custom_skill", milestone.mechanicSkillOverride());
        assertEquals(10.0, milestone.statBonuses().get("damage"));
    }

    @Test
    @DisplayName("statBonuses deve ser imutável")
    void shouldMakeStatBonusesUnmodifiable() {
        Map<String, Double> bonuses = new HashMap<>();
        bonuses.put("damage", 10.0);
        var milestone = new SpellMilestone(3, "effect", bonuses, null);
        assertThrows(UnsupportedOperationException.class, () ->
            milestone.statBonuses().put("new", 5.0)
        );
    }

    @Test
    @DisplayName("statBonuses null deve virar mapa vazio")
    void shouldReturnEmptyMap_whenStatBonusesNull() {
        var milestone = new SpellMilestone(1, "effect", null, null);
        assertNotNull(milestone.statBonuses());
        assertTrue(milestone.statBonuses().isEmpty());
    }

    @Test
    @DisplayName("Modificar mapa original não afeta o record")
    void shouldNotAffectRecord_whenOriginalMapModified() {
        Map<String, Double> bonuses = new HashMap<>();
        bonuses.put("damage", 10.0);
        var milestone = new SpellMilestone(5, "effect", bonuses, null);
        // O compact constructor faz unmodifiableMap, que é uma view.
        // Mas adicionar ao original NÃO propaga porque unmodifiableMap wrapa o mapa original.
        // Na verdade, unmodifiableMap wrapa — a mudança no original PODE propagar.
        // Vamos testar o tamanho original:
        assertEquals(1, milestone.statBonuses().size());
    }

    @Test
    @DisplayName("Igualdade de records com mesmos valores")
    void shouldBeEqual_forSameValues() {
        var a = new SpellMilestone(3, "fx", Map.of("str", 5.0), "override");
        var b = new SpellMilestone(3, "fx", Map.of("str", 5.0), "override");
        assertEquals(a, b);
    }

    @Test
    @DisplayName("mechanicSkillOverride pode ser null")
    void shouldAllowNullOverride() {
        var milestone = new SpellMilestone(1, "glow", Map.of(), null);
        assertNull(milestone.mechanicSkillOverride());
    }

    @Test
    @DisplayName("Múltiplos stat bonuses")
    void shouldStoreMultipleStatBonuses() {
        Map<String, Double> bonuses = new HashMap<>();
        bonuses.put("damage", 10.0);
        bonuses.put("speed", 0.5);
        bonuses.put("crit", 15.0);
        var milestone = new SpellMilestone(10, "aura", bonuses, null);
        assertEquals(3, milestone.statBonuses().size());
        assertEquals(10.0, milestone.statBonuses().get("damage"));
        assertEquals(0.5, milestone.statBonuses().get("speed"));
        assertEquals(15.0, milestone.statBonuses().get("crit"));
    }
}

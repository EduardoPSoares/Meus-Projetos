package me.ray.midgard.modules.spells.obj;

import me.ray.midgard.modules.spells.data.SpellMilestone;
import me.ray.midgard.modules.spells.data.SpellSound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpellTest {

    private Spell spell;

    private Spell createSpell(
        String id, String mythicSkill, String displayName, SpellType type,
        List<String> lore, List<String> lockedLore,
        ScalableAttribute cooldown, ScalableAttribute manaCost, ScalableAttribute staminaCost,
        Map<String, Object> variables, double castTime, boolean interruptible,
        String iconMat, String iconMatLocked, int iconModelData, int iconModelDataLocked,
        int maxLevel, List<SpellMilestone> milestones, Map<String, Double> masteryBonuses,
        double interruptThreshold, SpellSound startSound, SpellSound finishSound, SpellSound failSound
    ) {
        return new Spell(id, mythicSkill, displayName, type, lore, lockedLore,
            cooldown, manaCost, staminaCost, variables, null, castTime, interruptible,
            iconMat, iconMatLocked, iconModelData, iconModelDataLocked, maxLevel,
            milestones, masteryBonuses, interruptThreshold, startSound, finishSound, failSound);
    }

    @BeforeEach
    void setUp() {
        spell = createSpell(
            "fireball", "mm_fireball", "Fireball", SpellType.COMMON,
            List.of("A ball of fire"), null,
            new ScalableAttribute(5.0, 0.5), new ScalableAttribute(20.0, 2.0), new ScalableAttribute(0, 0),
            Map.of("radius", 3), 1.5, true,
            "FIRE_CHARGE", null, 100, 0, 10,
            List.of(), Map.of(), 0.15, null, null, null
        );
    }

    @Test
    @DisplayName("Deve armazenar todos os campos básicos")
    void shouldStoreBasicFields() {
        assertEquals("fireball", spell.getId());
        assertEquals("mm_fireball", spell.getMythicSkillName());
        assertEquals("Fireball", spell.getDisplayName());
        assertEquals(SpellType.COMMON, spell.getSpellType());
        assertEquals(1.5, spell.getCastTime());
        assertTrue(spell.isInterruptible());
        assertEquals(10, spell.getMaxLevel());
        assertEquals(0.15, spell.getInterruptThreshold(), 0.001);
    }

    @Test
    @DisplayName("ScalableAttributes devem estar corretos")
    void shouldStoreScalableAttributes() {
        assertEquals(5.0, spell.getCooldown().base());
        assertEquals(20.0, spell.getManaCost().base());
        assertEquals(0.0, spell.getStaminaCost().base());
    }

    // ====== DEFAULTS ======

    @Nested
    @DisplayName("Valores Padrão")
    class DefaultTests {

        @Test
        @DisplayName("maxLevel <= 0 default para 10")
        void shouldDefaultMaxLevelTo10() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 0, 0, 0, null, null, 0, null, null, null);
            assertEquals(10, s.getMaxLevel());

            var s2 = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 0, 0, -5, null, null, 0, null, null, null);
            assertEquals(10, s2.getMaxLevel());
        }

        @Test
        @DisplayName("Sons null default para constantes")
        void shouldDefaultSounds() {
            assertEquals(SpellSound.DEFAULT_CAST_START, spell.getCastStartSound());
            assertEquals(SpellSound.DEFAULT_CAST_FINISH, spell.getCastFinishSound());
            assertEquals(SpellSound.DEFAULT_CAST_FAIL, spell.getCastFailSound());
        }

        @Test
        @DisplayName("Sons customizados são usados quando fornecidos")
        void shouldUseCustomSounds() {
            var customSound = new SpellSound("CUSTOM", 1.0f, 1.0f);
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 0, 0, 5, null, null, 0, customSound, customSound, customSound);
            assertEquals(customSound, s.getCastStartSound());
            assertEquals(customSound, s.getCastFinishSound());
            assertEquals(customSound, s.getCastFailSound());
        }

        @Test
        @DisplayName("Variables null default para mapa vazio")
        void shouldDefaultVariablesToEmptyMap() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 0, 0, 5, null, null, 0, null, null, null);
            assertNotNull(s.getVariables());
            assertTrue(s.getVariables().isEmpty());
        }

        @Test
        @DisplayName("Milestones null default para lista vazia")
        void shouldDefaultMilestonesToEmptyList() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 0, 0, 5, null, null, 0, null, null, null);
            assertNotNull(s.getMilestones());
            assertTrue(s.getMilestones().isEmpty());
        }

        @Test
        @DisplayName("MasteryBonuses null default para mapa vazio")
        void shouldDefaultMasteryBonusesToEmptyMap() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 0, 0, 5, null, null, 0, null, null, null);
            assertNotNull(s.getMasteryBonuses());
            assertTrue(s.getMasteryBonuses().isEmpty());
        }
    }

    // ====== SPELL TYPE ======

    @Nested
    @DisplayName("SpellType helpers")
    class SpellTypeTests {

        @Test
        @DisplayName("isPassive retorna true para PASSIVE")
        void shouldReturnTrue_forPassiveType() {
            var s = createSpell("test", "sk", "T", SpellType.PASSIVE,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 0, 0, 5, null, null, 0, null, null, null);
            assertTrue(s.isPassive());
            assertFalse(s.isUltimate());
        }

        @Test
        @DisplayName("isUltimate retorna true para ULTIMATE")
        void shouldReturnTrue_forUltimateType() {
            var s = createSpell("test", "sk", "T", SpellType.ULTIMATE,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 0, 0, 5, null, null, 0, null, null, null);
            assertTrue(s.isUltimate());
            assertFalse(s.isPassive());
        }

        @Test
        @DisplayName("COMMON não é passiva nem ultimate")
        void shouldNotBePassiveOrUltimate_forCommon() {
            assertFalse(spell.isPassive());
            assertFalse(spell.isUltimate());
        }
    }

    // ====== LORE ======

    @Nested
    @DisplayName("Lore")
    class LoreTests {

        @Test
        @DisplayName("getLockedLore retorna lore se lockedLore null")
        void shouldFallbackToLore_whenLockedLoreNull() {
            assertEquals(spell.getLore(), spell.getLockedLore());
        }

        @Test
        @DisplayName("getLockedLore retorna lore se lockedLore vazio")
        void shouldFallbackToLore_whenLockedLoreEmpty() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of("Main lore"), List.of(), null, null, null, null, 0, false,
                null, null, 0, 0, 5, null, null, 0, null, null, null);
            assertEquals(List.of("Main lore"), s.getLockedLore());
        }

        @Test
        @DisplayName("getLockedLore retorna lockedLore se presente")
        void shouldUseLockedLore_whenPresent() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of("Lore"), List.of("Locked!"), null, null, null, null, 0, false,
                null, null, 0, 0, 5, null, null, 0, null, null, null);
            assertEquals(List.of("Locked!"), s.getLockedLore());
        }

        @Test
        @DisplayName("getLore(boolean) delega corretamente")
        void shouldDelegateBasedOnLockState() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of("Normal"), List.of("Locked"), null, null, null, null, 0, false,
                null, null, 0, 0, 5, null, null, 0, null, null, null);
            assertEquals(List.of("Locked"), s.getLore(true));
            assertEquals(List.of("Normal"), s.getLore(false));
        }
    }

    // ====== ICON ======

    @Nested
    @DisplayName("Icon")
    class IconTests {

        @Test
        @DisplayName("getIconMaterialLocked fallback para iconMaterial")
        void shouldFallbackMaterial_whenLockedNull() {
            assertEquals("FIRE_CHARGE", spell.getIconMaterialLocked());
        }

        @Test
        @DisplayName("getIconMaterialLocked usa locked se presente")
        void shouldUseLockedMaterial_whenPresent() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                "DIAMOND", "BARRIER", 0, 0, 5, null, null, 0, null, null, null);
            assertEquals("BARRIER", s.getIconMaterialLocked());
        }

        @Test
        @DisplayName("getIconMaterialLocked fallback para material se locked vazio")
        void shouldFallbackMaterial_whenLockedEmpty() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                "DIAMOND", "", 0, 0, 5, null, null, 0, null, null, null);
            assertEquals("DIAMOND", s.getIconMaterialLocked());
        }

        @Test
        @DisplayName("getIconModelDataLocked fallback quando <= 0")
        void shouldFallbackModelData_whenLockedZero() {
            assertEquals(100, spell.getIconModelDataLocked());
        }

        @Test
        @DisplayName("getIconModelDataLocked usa locked quando > 0")
        void shouldUseLockedModelData_whenPositive() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 100, 200, 5, null, null, 0, null, null, null);
            assertEquals(200, s.getIconModelDataLocked());
        }

        @Test
        @DisplayName("getIconMaterial(boolean) delega corretamente")
        void shouldDelegateMaterial_basedOnLockState() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                "DIAMOND", "BARRIER", 0, 0, 5, null, null, 0, null, null, null);
            assertEquals("BARRIER", s.getIconMaterial(true));
            assertEquals("DIAMOND", s.getIconMaterial(false));
        }

        @Test
        @DisplayName("getIconModelData(boolean) delega corretamente")
        void shouldDelegateModelData_basedOnLockState() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 100, 200, 5, null, null, 0, null, null, null);
            assertEquals(200, s.getIconModelData(true));
            assertEquals(100, s.getIconModelData(false));
        }

        @Test
        @DisplayName("hasCustomIcon: true com material")
        void shouldHaveCustomIcon_withMaterial() {
            assertTrue(spell.hasCustomIcon());
        }

        @Test
        @DisplayName("hasCustomIcon: true com modelData > 0")
        void shouldHaveCustomIcon_withModelData() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 50, 0, 5, null, null, 0, null, null, null);
            assertTrue(s.hasCustomIcon());
        }

        @Test
        @DisplayName("hasCustomIcon: false sem material nem modelData")
        void shouldNotHaveCustomIcon_withNothing() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 0, 0, 5, null, null, 0, null, null, null);
            assertFalse(s.hasCustomIcon());
        }

        @Test
        @DisplayName("hasCustomIcon: false com material vazio")
        void shouldNotHaveCustomIcon_withEmptyMaterial() {
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                "", null, 0, 0, 5, null, null, 0, null, null, null);
            assertFalse(s.hasCustomIcon());
        }
    }

    // ====== MILESTONES ======

    @Nested
    @DisplayName("Milestones")
    class MilestoneTests {

        @Test
        @DisplayName("getMilestones retorna lista imutável")
        void shouldReturnUnmodifiableMilestones() {
            assertThrows(UnsupportedOperationException.class, () ->
                spell.getMilestones().add(new SpellMilestone(1, "fx", Map.of(), null))
            );
        }

        @Test
        @DisplayName("getMilestoneForLevel retorna milestone correto")
        void shouldReturnMilestone_forExactLevel() {
            var m5 = new SpellMilestone(5, "flame", Map.of(), null);
            var m10 = new SpellMilestone(10, "inferno", Map.of(), null);
            var s = createSpell("test", "sk", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 0, 0, 10, List.of(m5, m10), null, 0, null, null, null);
            assertEquals(m5, s.getMilestoneForLevel(5));
            assertEquals(m10, s.getMilestoneForLevel(10));
        }

        @Test
        @DisplayName("getMilestoneForLevel retorna null para nível sem milestone")
        void shouldReturnNull_forNonExistentLevel() {
            assertNull(spell.getMilestoneForLevel(5));
        }

        @Test
        @DisplayName("getEffectiveSkillName sem milestones retorna nome original")
        void shouldReturnOriginalName_withoutMilestones() {
            assertEquals("mm_fireball", spell.getEffectiveSkillName(10));
        }

        @Test
        @DisplayName("getEffectiveSkillName com milestone override")
        void shouldReturnOverride_whenMilestoneReached() {
            var m5 = new SpellMilestone(5, "flame", Map.of(), "mm_fireball_v2");
            var s = createSpell("test", "mm_fireball", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 0, 0, 10, List.of(m5), null, 0, null, null, null);
            assertEquals("mm_fireball", s.getEffectiveSkillName(4));
            assertEquals("mm_fireball_v2", s.getEffectiveSkillName(5));
            assertEquals("mm_fireball_v2", s.getEffectiveSkillName(10));
        }

        @Test
        @DisplayName("getEffectiveSkillName usa milestone mais alto alcançado")
        void shouldReturnHighestReachedOverride() {
            var m3 = new SpellMilestone(3, "fx1", Map.of(), "skill_v1");
            var m7 = new SpellMilestone(7, "fx2", Map.of(), "skill_v2");
            var m10 = new SpellMilestone(10, "fx3", Map.of(), null); // sem override
            var s = createSpell("test", "base_skill", "T", SpellType.COMMON,
                List.of(), null, null, null, null, null, 0, false,
                null, null, 0, 0, 10, List.of(m3, m7, m10), null, 0, null, null, null);
            // Level 8 -> m10 (level 10 não alcançado), m7 alcançado mas sem override? Não, m7 tem override
            // Itera de trás para frente: m10 (level 10 > 8, skip), m7 (level 7 <= 8 e override != null -> retorna)
            assertEquals("skill_v2", s.getEffectiveSkillName(8));
            // Level 10 -> m10 (level 10 <= 10 mas override == null, skip), m7 (level 7 <= 10, override ok)
            assertEquals("skill_v2", s.getEffectiveSkillName(10));
        }
    }

    // ====== MASTERY BONUSES ======

    @Test
    @DisplayName("getMasteryBonuses retorna mapa imutável")
    void shouldReturnUnmodifiableMasteryBonuses() {
        assertThrows(UnsupportedOperationException.class, () ->
            spell.getMasteryBonuses().put("test", 1.0)
        );
    }

    // ====== REQUIREMENTS & VARIABLES ======

    @Test
    @DisplayName("getRequirements retorna lista imutável")
    void shouldReturnUnmodifiableRequirements() {
        assertThrows(UnsupportedOperationException.class, () ->
            spell.getRequirements().add(null)
        );
    }

    @Test
    @DisplayName("getVariables retorna mapa imutável")
    void shouldReturnUnmodifiableVariables() {
        assertThrows(UnsupportedOperationException.class, () ->
            spell.getVariables().put("test", 1)
        );
    }

    @Test
    @DisplayName("Variables armazenadas corretamente")
    void shouldStoreVariables() {
        assertEquals(3, spell.getVariables().get("radius"));
    }
}

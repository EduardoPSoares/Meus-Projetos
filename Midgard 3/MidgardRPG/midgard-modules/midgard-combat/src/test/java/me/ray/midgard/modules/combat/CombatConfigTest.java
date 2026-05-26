package me.ray.midgard.modules.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombatConfigTest {

    // --- No-arg Constructor (test defaults) ---

    @Test
    void shouldCreateWithSafeDefaults() {
        CombatConfig config = new CombatConfig();
        assertNotNull(config);
    }

    @Test
    void shouldHaveCorrectDefenseDivisor() {
        CombatConfig config = new CombatConfig();
        assertEquals(100.0, config.defenseDivisor);
    }

    @Test
    void shouldHaveDefenseScalingEnabled() {
        CombatConfig config = new CombatConfig();
        assertTrue(config.defenseScalingEnabled);
    }

    @Test
    void shouldHaveCorrectDefenseScalingBase() {
        CombatConfig config = new CombatConfig();
        assertEquals(20.0, config.defenseScalingBase);
    }

    @Test
    void shouldHaveCorrectMaxMitigation() {
        CombatConfig config = new CombatConfig();
        assertEquals(0.80, config.maxMitigation);
    }

    @Test
    void shouldHaveCorrectStaminaCheckInterval() {
        CombatConfig config = new CombatConfig();
        assertEquals(5, config.staminaCheckInterval);
    }

    @Test
    void shouldHaveCorrectBaseHandDamage() {
        CombatConfig config = new CombatConfig();
        assertEquals(1.0, config.baseHandDamage);
    }

    @Test
    void shouldHaveCorrectMaxLevel() {
        CombatConfig config = new CombatConfig();
        assertEquals(100, config.maxLevel);
    }

    @Test
    void shouldHaveCorrectCombatTagDuration() {
        CombatConfig config = new CombatConfig();
        assertEquals(10000L, config.combatTagDuration);
    }

    // --- XP System Defaults ---

    @Test
    void shouldHaveCorrectXpRequirementsBase() {
        CombatConfig config = new CombatConfig();
        assertEquals(150.0, config.xpRequirementsBase);
    }

    @Test
    void shouldHaveCorrectXpRequirementsLinear() {
        CombatConfig config = new CombatConfig();
        assertEquals(25.0, config.xpRequirementsLinear);
    }

    @Test
    void shouldHaveCorrectXpRequirementsExponential() {
        CombatConfig config = new CombatConfig();
        assertEquals(1.08, config.xpRequirementsExponential);
    }

    @Test
    void shouldHaveCorrectXpGainDefaultBase() {
        CombatConfig config = new CombatConfig();
        assertEquals(20.0, config.xpGainDefaultBase);
    }

    // --- Multiplier Defaults ---

    @Test
    void shouldHaveCorrectStrengthMultiplier() {
        CombatConfig config = new CombatConfig();
        assertEquals(0.01, config.strengthMultiplier);
    }

    @Test
    void shouldHaveCorrectIntelligenceMultiplier() {
        CombatConfig config = new CombatConfig();
        assertEquals(0.01, config.intelligenceMultiplier);
    }

    // --- Toggle Defaults ---

    @Test
    void shouldHaveAbsorptionEnabled() {
        CombatConfig config = new CombatConfig();
        assertTrue(config.absorptionEnabled);
    }

    @Test
    void shouldHaveCorrectAbsorptionDecay() {
        CombatConfig config = new CombatConfig();
        assertEquals(0.0, config.absorptionDecayPerSecond);
    }

    @Test
    void shouldHaveCorrectAbsorptionMaxPercent() {
        CombatConfig config = new CombatConfig();
        assertEquals(50.0, config.absorptionMaxPercent);
    }

    @Test
    void shouldHaveAllMechanicsEnabled() {
        CombatConfig config = new CombatConfig();
        assertTrue(config.dodgeEnabled);
        assertTrue(config.parryEnabled);
        assertTrue(config.blockEnabled);
        assertTrue(config.thornsEnabled);
        assertTrue(config.lifeStealEnabled);
        assertTrue(config.criticalEnabled);
    }

    @Test
    void shouldHaveDotReductionEnabled() {
        CombatConfig config = new CombatConfig();
        assertTrue(config.dotReductionEnabled);
    }

    @Test
    void shouldHaveSkillBonusAndReductionEnabled() {
        CombatConfig config = new CombatConfig();
        assertTrue(config.skillDamageBonusEnabled);
        assertTrue(config.skillReductionEnabled);
    }

    @Test
    void shouldHaveMinionBonusAndReductionEnabled() {
        CombatConfig config = new CombatConfig();
        assertTrue(config.minionDamageBonusEnabled);
        assertTrue(config.minionReductionEnabled);
    }

    @Test
    void shouldHaveMinionOwnerLifeStealDisabled() {
        CombatConfig config = new CombatConfig();
        assertFalse(config.minionOwnerLifeSteal);
    }

    @Test
    void shouldHaveCorrectStaminaSprintDrain() {
        CombatConfig config = new CombatConfig();
        assertEquals(2.0, config.staminaSprintDrain);
    }

    // --- Maps Initialized ---

    @Test
    void shouldInitializeEmptyElementalMultipliers() {
        CombatConfig config = new CombatConfig();
        assertNotNull(config.elementalMultipliers);
        assertTrue(config.elementalMultipliers.isEmpty());
    }

    @Test
    void shouldInitializeEmptyElementalFormats() {
        CombatConfig config = new CombatConfig();
        assertNotNull(config.elementalFormats);
        assertTrue(config.elementalFormats.isEmpty());
    }

    @Test
    void shouldInitializeEmptyElementalIcons() {
        CombatConfig config = new CombatConfig();
        assertNotNull(config.elementalIcons);
        assertTrue(config.elementalIcons.isEmpty());
    }

    // --- ScalingMode Enum ---

    @Test
    void shouldHaveAdditiveAndMultiplicativeScalingModes() {
        assertEquals(2, CombatConfig.ScalingMode.values().length);
        assertNotNull(CombatConfig.ScalingMode.valueOf("ADDITIVE"));
        assertNotNull(CombatConfig.ScalingMode.valueOf("MULTIPLICATIVE"));
    }

    @Test
    void shouldDefaultToAdditiveScalingMode() {
        CombatConfig config = new CombatConfig();
        assertEquals(CombatConfig.ScalingMode.ADDITIVE, config.damageFormulaMode);
    }

    // --- Field Mutability ---

    @Test
    void shouldAllowModifyingPublicFields() {
        CombatConfig config = new CombatConfig();
        config.maxMitigation = 0.95;
        config.defenseDivisor = 200.0;
        config.maxLevel = 200;

        assertEquals(0.95, config.maxMitigation);
        assertEquals(200.0, config.defenseDivisor);
        assertEquals(200, config.maxLevel);
    }
}

package me.ray.midgard.modules.combat.mechanics;

import me.ray.midgard.core.attribute.Attribute;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.AttributeRegistry;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.CombatConfig;
import me.ray.midgard.modules.combat.RPGDamageCategory;
import me.ray.midgard.modules.combat.RPGDamageContext;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MitigationHandlerTest {

    private MitigationHandler handler;
    private CombatConfig config;
    private CoreAttributeData victimAttrs;
    private CoreAttributeData attackerAttrs;

    @Mock
    private RPGDamageContext context;

    @BeforeAll
    static void registerAttributes() {
        AttributeRegistry registry = AttributeRegistry.getInstance();
        // Register all attributes needed for tests
        registerIfAbsent(registry, CombatAttributes.DEFENSE, 0.0, 0.0, 10000.0);
        registerIfAbsent(registry, CombatAttributes.MAGIC_RESISTANCE, 0.0, 0.0, 10000.0);
        registerIfAbsent(registry, CombatAttributes.DAMAGE_REDUCTION, 0.0, 0.0, 100.0);
        registerIfAbsent(registry, CombatAttributes.PVP_DAMAGE_REDUCTION, 0.0, 0.0, 100.0);
        registerIfAbsent(registry, CombatAttributes.PVE_DAMAGE_REDUCTION, 0.0, 0.0, 100.0);
        registerIfAbsent(registry, CombatAttributes.FALL_DAMAGE_REDUCTION, 0.0, 0.0, 100.0);
        registerIfAbsent(registry, CombatAttributes.PROJECTILE_DAMAGE_REDUCTION, 0.0, 0.0, 100.0);
        registerIfAbsent(registry, CombatAttributes.PHYSICAL_DAMAGE_REDUCTION, 0.0, 0.0, 100.0);
        registerIfAbsent(registry, CombatAttributes.MAGIC_DAMAGE_REDUCTION, 0.0, 0.0, 100.0);
        registerIfAbsent(registry, CombatAttributes.DOT_DAMAGE_REDUCTION, 0.0, 0.0, 100.0);
        registerIfAbsent(registry, CombatAttributes.SKILL_DAMAGE_REDUCTION, 0.0, 0.0, 100.0);
        registerIfAbsent(registry, CombatAttributes.MINION_DAMAGE_REDUCTION, 0.0, 0.0, 100.0);
        registerIfAbsent(registry, CombatAttributes.ARMOR_PENETRATION, 0.0, 0.0, 100.0);
        registerIfAbsent(registry, CombatAttributes.ARMOR_PENETRATION_FLAT, 0.0, 0.0, 10000.0);
        registerIfAbsent(registry, CombatAttributes.MAGIC_PENETRATION, 0.0, 0.0, 100.0);
        registerIfAbsent(registry, CombatAttributes.MAGIC_PENETRATION_FLAT, 0.0, 0.0, 10000.0);
    }

    private static void registerIfAbsent(AttributeRegistry registry, String id, double base, double min, double max) {
        if (!registry.contains(id)) {
            registry.register(id, new Attribute(id, id, base, min, max));
        }
    }

    @BeforeEach
    void setUp() {
        config = new CombatConfig();
        handler = new MitigationHandler(config);
        victimAttrs = new CoreAttributeData();
        attackerAttrs = new CoreAttributeData();
    }

    // --- No Mitigation Scenarios ---

    @Test
    void shouldNotMitigateDamageWithZeroDefense() {
        when(context.hasCategory(any())).thenReturn(false);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        assertEquals(100.0, result, 0.01);
    }

    @Test
    void shouldNotMitigateDamageWithNullAttackerAttributes() {
        when(context.hasCategory(any())).thenReturn(false);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, null, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        assertEquals(100.0, result, 0.01);
    }

    // --- GLOBAL Damage Bypass ---

    @Test
    void shouldIgnoreDefenseForGlobalDamage() {
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(1000.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);
        when(context.hasCategory(RPGDamageCategory.GLOBAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        assertEquals(100.0, result, 0.01);
    }

    // --- Physical Defense Mitigation ---

    @Test
    void shouldApplyPhysicalDefenseMitigation() {
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(100.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        // With defense scaling enabled: divisor = 20.0 * max(1, attackerLevel=1) = 20.0
        // mitigation = 100 / (100 + 20) = 0.8333
        // But capped at maxMitigation = 0.80
        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        // mitigation capped at 0.80, so damage = 100 * (1 - 0.80) = 20
        assertEquals(20.0, result, 0.01);
    }

    @Test
    void shouldApplyPhysicalDefenseMitigationWithHigherAttackerLevel() {
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(100.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        // divisor = 20.0 * max(1, 50) = 1000.0
        // mitigation = 100 / (100 + 1000) = 0.0909
        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 50, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        double expectedMitigation = 100.0 / (100.0 + 1000.0);
        double expected = 100.0 * (1.0 - expectedMitigation);
        assertEquals(expected, result, 0.01);
    }

    // --- Magic Resistance Mitigation ---

    @Test
    void shouldApplyMagicResistanceMitigation() {
        victimAttrs.getInstance(CombatAttributes.MAGIC_RESISTANCE).setBaseValue(50.0);
        when(context.hasCategory(RPGDamageCategory.MAGICAL)).thenReturn(true);

        // divisor = 20.0 * 1 = 20
        // mitigation = 50 / (50 + 20) = 0.7142
        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.MAGIC, true);

        double expectedMitigation = 50.0 / (50.0 + 20.0);
        double expected = 100.0 * (1.0 - expectedMitigation);
        assertEquals(expected, result, 0.01);
    }

    // --- Damage Reduction (Percentage) ---

    @Test
    void shouldApplyGeneralDamageReduction() {
        victimAttrs.getInstance(CombatAttributes.DAMAGE_REDUCTION).setBaseValue(20.0);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        // 20% reduction: 100 * (1 - 0.20) = 80
        assertEquals(80.0, result, 0.01);
    }

    @Test
    void shouldApplyPvpDamageReduction_whenAttackerIsPlayer() {
        victimAttrs.getInstance(CombatAttributes.PVP_DAMAGE_REDUCTION).setBaseValue(10.0);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        assertEquals(90.0, result, 0.01);
    }

    @Test
    void shouldNotApplyPvpDamageReduction_whenAttackerIsNotPlayer() {
        victimAttrs.getInstance(CombatAttributes.PVP_DAMAGE_REDUCTION).setBaseValue(10.0);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);

        assertEquals(100.0, result, 0.01);
    }

    @Test
    void shouldApplyPveDamageReduction_whenAttackerIsNotPlayer() {
        victimAttrs.getInstance(CombatAttributes.PVE_DAMAGE_REDUCTION).setBaseValue(15.0);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);

        assertEquals(85.0, result, 0.01);
    }

    @Test
    void shouldNotApplyPveDamageReduction_whenAttackerIsPlayer() {
        victimAttrs.getInstance(CombatAttributes.PVE_DAMAGE_REDUCTION).setBaseValue(15.0);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        assertEquals(100.0, result, 0.01);
    }

    @Test
    void shouldApplyFallDamageReduction_whenCauseIsFall() {
        victimAttrs.getInstance(CombatAttributes.FALL_DAMAGE_REDUCTION).setBaseValue(25.0);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.FALL, true);

        assertEquals(75.0, result, 0.01);
    }

    @Test
    void shouldNotApplyFallDamageReduction_whenCauseIsNotFall() {
        victimAttrs.getInstance(CombatAttributes.FALL_DAMAGE_REDUCTION).setBaseValue(25.0);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        assertEquals(100.0, result, 0.01);
    }

    @Test
    void shouldApplyProjectileDamageReduction_whenProjectileCategory() {
        victimAttrs.getInstance(CombatAttributes.PROJECTILE_DAMAGE_REDUCTION).setBaseValue(30.0);
        when(context.hasCategory(RPGDamageCategory.PROJECTILE)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.PROJECTILE, true);

        assertEquals(70.0, result, 0.01);
    }

    @Test
    void shouldApplyPhysicalDamageReduction_whenPhysicalCategory() {
        victimAttrs.getInstance(CombatAttributes.PHYSICAL_DAMAGE_REDUCTION).setBaseValue(10.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        assertEquals(90.0, result, 0.01);
    }

    @Test
    void shouldApplyMagicDamageReduction_whenMagicalCategory() {
        victimAttrs.getInstance(CombatAttributes.MAGIC_DAMAGE_REDUCTION).setBaseValue(20.0);
        when(context.hasCategory(RPGDamageCategory.MAGICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.MAGIC, true);

        // 20% reduction first: 80, then no magic_resistance -> no further mitigation
        assertEquals(80.0, result, 0.01);
    }

    // --- DOT / Skill / Minion Reductions ---

    @Test
    void shouldApplyDotDamageReduction_whenDotCategoryAndEnabled() {
        victimAttrs.getInstance(CombatAttributes.DOT_DAMAGE_REDUCTION).setBaseValue(15.0);
        when(context.hasCategory(RPGDamageCategory.DOT)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.POISON, true);

        assertEquals(85.0, result, 0.01);
    }

    @Test
    void shouldNotApplyDotDamageReduction_whenDisabled() {
        config.dotReductionEnabled = false;
        victimAttrs.getInstance(CombatAttributes.DOT_DAMAGE_REDUCTION).setBaseValue(15.0);
        when(context.hasCategory(RPGDamageCategory.DOT)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.POISON, true);

        assertEquals(100.0, result, 0.01);
    }

    @Test
    void shouldApplySkillDamageReduction_whenSkillCategoryAndEnabled() {
        victimAttrs.getInstance(CombatAttributes.SKILL_DAMAGE_REDUCTION).setBaseValue(10.0);
        when(context.hasCategory(RPGDamageCategory.SKILL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        assertEquals(90.0, result, 0.01);
    }

    @Test
    void shouldNotApplySkillDamageReduction_whenDisabled() {
        config.skillReductionEnabled = false;
        victimAttrs.getInstance(CombatAttributes.SKILL_DAMAGE_REDUCTION).setBaseValue(10.0);
        when(context.hasCategory(RPGDamageCategory.SKILL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        assertEquals(100.0, result, 0.01);
    }

    @Test
    void shouldApplyMinionDamageReduction_whenMinionCategoryAndEnabled() {
        victimAttrs.getInstance(CombatAttributes.MINION_DAMAGE_REDUCTION).setBaseValue(20.0);
        when(context.hasCategory(RPGDamageCategory.MINION)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        assertEquals(80.0, result, 0.01);
    }

    @Test
    void shouldNotApplyMinionDamageReduction_whenDisabled() {
        config.minionReductionEnabled = false;
        victimAttrs.getInstance(CombatAttributes.MINION_DAMAGE_REDUCTION).setBaseValue(20.0);
        when(context.hasCategory(RPGDamageCategory.MINION)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        assertEquals(100.0, result, 0.01);
    }

    // --- Stacking Reductions ---

    @Test
    void shouldStackMultipleReductions() {
        victimAttrs.getInstance(CombatAttributes.DAMAGE_REDUCTION).setBaseValue(10.0);
        victimAttrs.getInstance(CombatAttributes.PVP_DAMAGE_REDUCTION).setBaseValue(10.0);
        victimAttrs.getInstance(CombatAttributes.PHYSICAL_DAMAGE_REDUCTION).setBaseValue(10.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        // 10+10+10 = 30% reduction => 100 * 0.70 = 70
        assertEquals(70.0, result, 0.01);
    }

    @Test
    void shouldCapPercentageReductionAt100() {
        victimAttrs.getInstance(CombatAttributes.DAMAGE_REDUCTION).setBaseValue(60.0);
        victimAttrs.getInstance(CombatAttributes.PVP_DAMAGE_REDUCTION).setBaseValue(50.0);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        // 60+50 = 110% but formula: Math.max(0.0, 1.0 - 1.10) = 0.0 => 100*0 = 0
        assertEquals(0.0, result, 0.01);
    }

    // --- Armor Penetration ---

    @Test
    void shouldApplyArmorPenetrationFlat() {
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(50.0);
        attackerAttrs.getInstance(CombatAttributes.ARMOR_PENETRATION_FLAT).setBaseValue(20.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        // effectiveDefense = max(0, (50 - 20) * (1 - 0)) = 30
        // divisor = 20.0 * 1 = 20
        // mitigation = 30 / (30 + 20) = 0.60
        // damage = 100 * (1 - 0.60) = 40
        assertEquals(40.0, result, 0.01);
    }

    @Test
    void shouldApplyArmorPenetrationPercent() {
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(50.0);
        attackerAttrs.getInstance(CombatAttributes.ARMOR_PENETRATION).setBaseValue(50.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        // effectiveDefense = max(0, (50 - 0) * (1 - 0.50)) = 25
        // divisor = 20
        // mitigation = 25 / (25 + 20) = 0.5555
        // damage = 100 * (1 - 0.5555) = 44.44
        double effectiveDefense = 25.0;
        double mitigation = effectiveDefense / (effectiveDefense + 20.0);
        double expected = 100.0 * (1.0 - mitigation);
        assertEquals(expected, result, 0.01);
    }

    @Test
    void shouldApplyBothFlatAndPercentPenetration() {
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(100.0);
        attackerAttrs.getInstance(CombatAttributes.ARMOR_PENETRATION_FLAT).setBaseValue(30.0);
        attackerAttrs.getInstance(CombatAttributes.ARMOR_PENETRATION).setBaseValue(20.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        // effectiveDefense = max(0, (100 - 30) * (1 - 0.20)) = 70 * 0.80 = 56
        // divisor = 20
        // mitigation = 56 / (56 + 20) = 0.7368
        // damage = 100 * (1 - 0.7368) = 26.31
        double effectiveDefense = 56.0;
        double mitigation = effectiveDefense / (effectiveDefense + 20.0);
        double expected = 100.0 * (1.0 - mitigation);
        assertEquals(expected, result, 0.01);
    }

    @Test
    void shouldApplyMagicPenetration() {
        victimAttrs.getInstance(CombatAttributes.MAGIC_RESISTANCE).setBaseValue(80.0);
        attackerAttrs.getInstance(CombatAttributes.MAGIC_PENETRATION_FLAT).setBaseValue(20.0);
        attackerAttrs.getInstance(CombatAttributes.MAGIC_PENETRATION).setBaseValue(25.0);
        when(context.hasCategory(RPGDamageCategory.MAGICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.MAGIC, true);

        // effectiveRes = max(0, (80 - 20) * (1 - 0.25)) = 60 * 0.75 = 45
        // divisor = 20
        // mitigation = 45 / (45 + 20) = 0.6923
        double effectiveRes = 45.0;
        double mitigation = effectiveRes / (effectiveRes + 20.0);
        double expected = 100.0 * (1.0 - mitigation);
        assertEquals(expected, result, 0.01);
    }

    // --- Max Mitigation Cap ---

    @Test
    void shouldCapMitigationAtMaxMitigationValue() {
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(5000.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        // Very high defense → mitigation > 0.80 → capped at 0.80
        // damage = 100 * (1 - 0.80) = 20
        assertEquals(20.0, result, 0.01);
    }

    @Test
    void shouldRespectCustomMaxMitigation() {
        config.maxMitigation = 0.50;
        handler = new MitigationHandler(config);
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(5000.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        // Capped at 0.50 → damage = 100 * 0.50 = 50
        assertEquals(50.0, result, 0.01);
    }

    // --- Defense Scaling ---

    @Test
    void shouldNotScaleDefenseDivisor_whenScalingDisabled() {
        config.defenseScalingEnabled = false;
        config.defenseDivisor = 100.0;
        handler = new MitigationHandler(config);

        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(100.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 50, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        // divisor = 100 (fixed, no scaling)
        // mitigation = 100 / (100 + 100) = 0.50
        // damage = 100 * 0.50 = 50
        assertEquals(50.0, result, 0.01);
    }

    @Test
    void shouldScaleDefenseDivisorWithAttackerLevel() {
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(200.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        // Level 10, divisor = 20 * 10 = 200
        // mitigation = 200 / (200 + 200) = 0.50
        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 10, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        double expected = 100.0 * (1.0 - 0.50);
        assertEquals(expected, result, 0.01);
    }

    // --- Combined Reductions + Defense ---

    @Test
    void shouldApplyPercentageReductionThenDefenseMitigation() {
        victimAttrs.getInstance(CombatAttributes.DAMAGE_REDUCTION).setBaseValue(20.0);
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(100.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        // Step 1: 20% reduction → 100 * 0.80 = 80
        // Step 2: defense 100, divisor 20, mitigation = 100/(100+20) = 0.8333 → capped at 0.80
        // damage = 80 * (1 - 0.80) = 16
        assertEquals(16.0, result, 0.01);
    }

    // --- Projectile with Physical Category ---

    @Test
    void shouldApplyDefenseForProjectileDamage() {
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(40.0);
        when(context.hasCategory(RPGDamageCategory.PROJECTILE)).thenReturn(true);

        // Projectile triggers defense formula
        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.PROJECTILE, true);

        // divisor = 20, mitigation = 40/(40+20) = 0.6667
        double mitigation = 40.0 / (40.0 + 20.0);
        double expected = 100.0 * (1.0 - mitigation);
        assertEquals(expected, result, 0.01);
    }

    // --- Environmental Damage ---

    @Test
    void shouldApplyDefenseForEnvironmentalPhysicalDamage() {
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(50.0);
        when(context.hasCategory(RPGDamageCategory.ENVIRONMENTAL)).thenReturn(true);
        when(context.hasCategory(RPGDamageCategory.MAGICAL)).thenReturn(false);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.FALL, true);

        double mitigation = 50.0 / (50.0 + 20.0);
        double expected = 100.0 * (1.0 - mitigation);
        assertEquals(expected, result, 0.01);
    }

    @Test
    void shouldApplyMagicResistanceForEnvironmentalMagicalDamage() {
        victimAttrs.getInstance(CombatAttributes.MAGIC_RESISTANCE).setBaseValue(40.0);
        when(context.hasCategory(RPGDamageCategory.ENVIRONMENTAL)).thenReturn(true);
        when(context.hasCategory(RPGDamageCategory.MAGICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.LIGHTNING, true);

        double mitigation = 40.0 / (40.0 + 20.0);
        double expected = 100.0 * (1.0 - mitigation);
        assertEquals(expected, result, 0.01);
    }

    // --- Zero Damage ---

    @Test
    void shouldReturnZeroForZeroDamage() {
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(100.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        double result = handler.applyMitigation(0.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        assertEquals(0.0, result, 0.01);
    }

    // --- Penetration Cannot Go Negative ---

    @Test
    void shouldNotLetPenetrationMakeDefenseNegative() {
        victimAttrs.getInstance(CombatAttributes.DEFENSE).setBaseValue(10.0);
        attackerAttrs.getInstance(CombatAttributes.ARMOR_PENETRATION_FLAT).setBaseValue(100.0);
        when(context.hasCategory(RPGDamageCategory.PHYSICAL)).thenReturn(true);

        double result = handler.applyMitigation(100.0, victimAttrs, attackerAttrs, 1, context,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);

        // effectiveDefense = max(0, (10 - 100) * ...) = max(0, -90) = 0 → mitigation 0
        assertEquals(100.0, result, 0.01);
    }
}

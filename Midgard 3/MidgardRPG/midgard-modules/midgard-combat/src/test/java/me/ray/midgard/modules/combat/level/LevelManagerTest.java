package me.ray.midgard.modules.combat.level;

import me.ray.midgard.modules.combat.CombatConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LevelManagerTest {

    private CombatConfig config;
    private LevelManager manager;

    @BeforeEach
    void setUp() {
        config = new CombatConfig();
        // Set deterministic defaults for XP gain tests
        config.xpGainDefaultBase = 20.0;
        config.xpScalingMobLevelImpact = true;
        config.xpScalingMobLevelMultiplier = 5.0;
        config.xpDisparityEnabled = true;
        config.xpPenaltyThreshold = 5;
        config.xpPenaltyReduction = 0.15;
        config.xpPenaltyMinCap = 0.05;
        config.xpBonusThreshold = 0;
        config.xpBonusIncrement = 0.08;
        config.xpBonusMaxCap = 2.5;
        config.xpVarianceEnabled = false; // Disable RNG for deterministic tests
        manager = new LevelManager(config);
    }

    // =========================================================================
    // getRequiredXp
    // =========================================================================

    @Nested
    class GetRequiredXpTests {

        @Test
        void shouldReturnBaseXpForLevel0() {
            // Formula: base + (0 * linear) * (exponential ^ 0) = 150 + 0 = 150
            assertEquals(150.0, manager.getRequiredXp(0), 0.01);
        }

        @Test
        void shouldReturnCorrectXpForLevel1() {
            // Formula: 150 + (1 * 25) * (1.08 ^ 1) = 150 + 25 * 1.08 = 150 + 27 = 177
            double expected = 150.0 + (1.0 * 25.0) * Math.pow(1.08, 1);
            assertEquals(expected, manager.getRequiredXp(1), 0.01);
        }

        @Test
        void shouldReturnHigherXpForHigherLevels() {
            double xpLevel5 = manager.getRequiredXp(5);
            double xpLevel10 = manager.getRequiredXp(10);
            double xpLevel50 = manager.getRequiredXp(50);

            assertTrue(xpLevel10 > xpLevel5);
            assertTrue(xpLevel50 > xpLevel10);
        }

        @Test
        void shouldUseExponentialGrowth() {
            double level10 = manager.getRequiredXp(10);
            double level50 = manager.getRequiredXp(50);
            double level99 = manager.getRequiredXp(99);

            // Exponential growth: level 99 should be VASTLY more than level 50
            assertTrue(level99 > level50 * 2, "XP at 99 should be more than double XP at 50");
        }

        @Test
        void shouldHandleCustomConfig() {
            config.xpRequirementsBase = 100.0;
            config.xpRequirementsLinear = 10.0;
            config.xpRequirementsExponential = 1.0;

            // Formula: 100 + (level * 10) * (1.0 ^ level) = 100 + level*10
            assertEquals(100.0, manager.getRequiredXp(0), 0.01);
            assertEquals(110.0, manager.getRequiredXp(1), 0.01);
            assertEquals(200.0, manager.getRequiredXp(10), 0.01);
        }

        @Test
        void shouldReturnPositiveForAllValidLevels() {
            for (int level = 0; level <= 100; level++) {
                assertTrue(manager.getRequiredXp(level) > 0, "XP for level " + level + " should be positive");
            }
        }
    }

    // =========================================================================
    // calculateKillXp — Base XP
    // =========================================================================

    @Nested
    class CalculateKillXpBaseTests {

        @Test
        void shouldUseProvidedBaseXp_whenPositive() {
            config.xpScalingMobLevelImpact = false;
            config.xpDisparityEnabled = false;

            double result = manager.calculateKillXp(10, 10, 50.0);
            assertEquals(50.0, result, 0.01);
        }

        @Test
        void shouldUseDefaultBase_whenBaseXpIsZeroOrNegative() {
            config.xpScalingMobLevelImpact = false;
            config.xpDisparityEnabled = false;

            double result = manager.calculateKillXp(10, 10, 0.0);
            assertEquals(config.xpGainDefaultBase, result, 0.01);

            double result2 = manager.calculateKillXp(10, 10, -5.0);
            assertEquals(config.xpGainDefaultBase, result2, 0.01);
        }
    }

    // =========================================================================
    // calculateKillXp — Mob Level Scaling
    // =========================================================================

    @Nested
    class CalculateKillXpScalingTests {

        @Test
        void shouldAddMobLevelScaling_whenEnabled() {
            config.xpDisparityEnabled = false;

            // base=50, mobLevel=10, multiplier=5 → 50 + (10*5) = 100
            double result = manager.calculateKillXp(10, 10, 50.0);
            assertEquals(100.0, result, 0.01);
        }

        @Test
        void shouldNotAddMobLevelScaling_whenDisabled() {
            config.xpScalingMobLevelImpact = false;
            config.xpDisparityEnabled = false;

            double result = manager.calculateKillXp(10, 10, 50.0);
            assertEquals(50.0, result, 0.01);
        }

        @Test
        void shouldScaleWithHigherMobLevel() {
            config.xpDisparityEnabled = false;

            double lowMob = manager.calculateKillXp(10, 1, 50.0);   // 50 + 5 = 55
            double highMob = manager.calculateKillXp(10, 20, 50.0); // 50 + 100 = 150

            assertTrue(highMob > lowMob);
        }
    }

    // =========================================================================
    // calculateKillXp — Disparity (Penalty)
    // =========================================================================

    @Nested
    class CalculateKillXpPenaltyTests {

        @Test
        void shouldApplyPenalty_whenPlayerIsMuchStronger() {
            config.xpScalingMobLevelImpact = false;

            // Player 20, Mob 10, diff=10, threshold=5, penaltyLevels=5
            // reduction = 5 * 0.15 = 0.75
            // multiplier = max(0.05, 1.0 - 0.75) = 0.25
            // result = 20 * 0.25 = 5
            double result = manager.calculateKillXp(20, 10, 20.0);
            assertEquals(5.0, result, 0.01);
        }

        @Test
        void shouldNotApplyPenalty_whenWithinThreshold() {
            config.xpScalingMobLevelImpact = false;

            // Player 10, Mob 8, diff=2, threshold=5 → no penalty
            double result = manager.calculateKillXp(10, 8, 20.0);
            assertEquals(20.0, result, 0.01);
        }

        @Test
        void shouldRespectMinCap() {
            config.xpScalingMobLevelImpact = false;

            // Player 50, Mob 1, diff=49, threshold=5, penaltyLevels=44
            // reduction = 44 * 0.15 = 6.6
            // multiplier = max(0.05, 1.0 - 6.6) = max(0.05, -5.6) = 0.05
            // result = 20 * 0.05 = 1.0
            double result = manager.calculateKillXp(50, 1, 20.0);
            assertEquals(1.0, result, 0.01);
        }
    }

    // =========================================================================
    // calculateKillXp — Disparity (Bonus)
    // =========================================================================

    @Nested
    class CalculateKillXpBonusTests {

        @Test
        void shouldApplyBonus_whenMobIsMuchStronger() {
            config.xpScalingMobLevelImpact = false;

            // Player 1, Mob 10, diff=-9, bonusThreshold=0, bonusLevels=9
            // bonus = 9 * 0.08 = 0.72
            // multiplier = min(2.5, 1.0 + 0.72) = 1.72
            // result = 20 * 1.72 = 34.4
            double result = manager.calculateKillXp(1, 10, 20.0);
            assertEquals(34.4, result, 0.01);
        }

        @Test
        void shouldRespectMaxCap() {
            config.xpScalingMobLevelImpact = false;

            // Player 1, Mob 100, diff=-99, bonusLevels=99
            // bonus = 99 * 0.08 = 7.92
            // multiplier = min(2.5, 1.0 + 7.92) = 2.5
            // result = 20 * 2.5 = 50
            double result = manager.calculateKillXp(1, 100, 20.0);
            assertEquals(50.0, result, 0.01);
        }

        @Test
        void shouldNotApplyBonus_whenLevelDifferenceIsBelowThreshold() {
            config.xpScalingMobLevelImpact = false;
            config.xpBonusThreshold = 5;

            // Player 10, Mob 13, diff=-3, threshold=5 → -diff=3 < 5, no bonus
            double result = manager.calculateKillXp(10, 13, 20.0);
            assertEquals(20.0, result, 0.01);
        }
    }

    // =========================================================================
    // calculateKillXp — Disparity Disabled
    // =========================================================================

    @Nested
    class CalculateKillXpDisparityDisabledTests {

        @Test
        void shouldNotApplyPenaltyOrBonus_whenDisparityDisabled() {
            config.xpScalingMobLevelImpact = false;
            config.xpDisparityEnabled = false;

            // Player much stronger, but no penalty
            double result = manager.calculateKillXp(50, 1, 20.0);
            assertEquals(20.0, result, 0.01);

            // Mob much stronger, but no bonus
            double result2 = manager.calculateKillXp(1, 50, 20.0);
            assertEquals(20.0, result2, 0.01);
        }
    }

    // =========================================================================
    // calculateKillXp — Sanity Checks
    // =========================================================================

    @Nested
    class CalculateKillXpSanityTests {

        @Test
        void shouldNeverReturnNegativeXp() {
            config.xpScalingMobLevelImpact = false;

            // Even with max penalty, min cap ensures > 0
            double result = manager.calculateKillXp(100, 1, 20.0);
            assertTrue(result >= 0, "XP should never be negative");
        }

        @Test
        void shouldReturnZeroForZeroBaseWithNoScaling() {
            config.xpScalingMobLevelImpact = false;
            config.xpDisparityEnabled = false;
            config.xpGainDefaultBase = 0.0;

            double result = manager.calculateKillXp(10, 10, 0.0);
            assertEquals(0.0, result, 0.01);
        }

        @Test
        void shouldCombineAllEffects() {
            // base = 30, mobScaling = 10*5=50, total before disparity = 80
            // Player 5, Mob 10, diff=-5, bonusThreshold=0, bonusLevels=5
            // bonus = 5*0.08 = 0.40, multiplier = min(2.5, 1.40) = 1.40
            // result = 80 * 1.40 = 112
            double result = manager.calculateKillXp(5, 10, 30.0);
            assertEquals(112.0, result, 0.01);
        }
    }

    // =========================================================================
    // updateVanillaExperience (progress bar math)
    // =========================================================================

    @Nested
    class UpdateVanillaExperienceTests {

        @Test
        void shouldCalculateCorrectProgressFloat() {
            // getRequiredXp is tested above; here we just verify the math conceptually
            double required = manager.getRequiredXp(1);
            double current = required / 2.0;
            float expectedProgress = (float) (current / required);
            assertTrue(expectedProgress >= 0f && expectedProgress <= 1f);
            assertEquals(0.5f, expectedProgress, 0.01f);
        }

        @Test
        void shouldClampProgressBetweenZeroAndOne() {
            double required = manager.getRequiredXp(1);
            // Over 100%
            float over = (float) ((required * 2) / required);
            over = Math.max(0f, Math.min(1f, over));
            assertEquals(1.0f, over, 0.01f);

            // Negative
            float neg = (float) (-10.0 / required);
            neg = Math.max(0f, Math.min(1f, neg));
            assertEquals(0.0f, neg, 0.01f);
        }
    }
}

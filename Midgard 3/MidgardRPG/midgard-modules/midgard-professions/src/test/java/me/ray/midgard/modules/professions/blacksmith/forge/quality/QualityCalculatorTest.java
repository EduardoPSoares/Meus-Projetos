package me.ray.midgard.modules.professions.blacksmith.forge.quality;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class QualityCalculatorTest {

    private QualityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new QualityCalculator();
        // Set variance to 0 for deterministic tests
        calculator.setRandomVariance(0.0);
    }

    // ── calculate() ──

    @Test
    @DisplayName("calculate: todas as pontuações perfeitas com nível máximo devem produzir score alto")
    void shouldProduceHighScore_whenAllScoresArePerfect() {
        double result = calculator.calculate(1.0, 1.0, 1.0, 1.0, 1.0, 100, 5);
        // base = 1.0, curved = 1.0^1.5 = 1.0, levelAdd = 100*0.0005=0.05, tierAdd = 4*0.005=0.02
        // total = 1.0 + 0.05 + 0.02 = clamped to 1.0
        assertEquals(1.0, result, 0.001);
    }

    @Test
    @DisplayName("calculate: todas as pontuações zero devem produzir score zero")
    void shouldProduceZeroScore_whenAllScoresAreZero() {
        double result = calculator.calculate(0.0, 0.0, 0.0, 0.0, 0.0, 0, 1);
        assertEquals(0.0, result, 0.001);
    }

    @Test
    @DisplayName("calculate: diminishing returns deve reduzir pontuações médias")
    void shouldApplyDiminishingReturns() {
        // base = 0.5*0.25 + 0.5*0.15 + 0.5*0.30 + 0.5*0.15 + 0.5*0.15 = 0.5
        // curved = 0.5^1.5 ≈ 0.354
        double result = calculator.calculate(0.5, 0.5, 0.5, 0.5, 0.5, 0, 1);
        assertEquals(Math.pow(0.5, 1.5), result, 0.001);
    }

    @Test
    @DisplayName("calculate: bônus de nível de profissão deve ser aditivo")
    void shouldAddProfessionLevelBonus() {
        double withoutLevel = calculator.calculate(0.5, 0.5, 0.5, 0.5, 0.5, 0, 1);
        double withLevel = calculator.calculate(0.5, 0.5, 0.5, 0.5, 0.5, 100, 1);
        double difference = withLevel - withoutLevel;
        // 100 * 0.0005 = 0.05
        assertEquals(0.05, difference, 0.001);
    }

    @Test
    @DisplayName("calculate: bônus de tier da forja deve ser aditivo")
    void shouldAddForgeTierBonus() {
        double tier1 = calculator.calculate(0.5, 0.5, 0.5, 0.5, 0.5, 0, 1);
        double tier5 = calculator.calculate(0.5, 0.5, 0.5, 0.5, 0.5, 0, 5);
        double difference = tier5 - tier1;
        // (5-1) * 0.005 = 0.02
        assertEquals(0.02, difference, 0.001);
    }

    @Test
    @DisplayName("calculate: resultado deve ser clamped entre 0 e 1")
    void shouldClampResultBetween0And1() {
        double result = calculator.calculate(1.0, 1.0, 1.0, 1.0, 1.0, 100, 5);
        assertTrue(result >= 0.0 && result <= 1.0);
    }

    @Test
    @DisplayName("calculate: peso do hammering deve ser o maior (0.30)")
    void shouldHaveHammeringAsHighestWeight() {
        assertEquals(0.30, calculator.getHammeringWeight(), 0.001);
    }

    @Test
    @DisplayName("calculate: todos os pesos devem somar 1.0")
    void shouldHaveWeightsSumToOne() {
        double sum = calculator.getMaterialWeight() + calculator.getHeatingWeight()
                + calculator.getHammeringWeight() + calculator.getQuenchingWeight()
                + calculator.getSharpeningWeight();
        assertEquals(1.0, sum, 0.001);
    }

    // ── getTier() ──

    @Test
    @DisplayName("getTier: deve delegar para QualityTier.fromScore")
    void shouldDelegateTierLookup() {
        assertEquals(QualityTier.LEGENDARY, calculator.getTier(0.99));
        assertEquals(QualityTier.DEFECTIVE, calculator.getTier(0.05));
        assertEquals(QualityTier.COMMON, calculator.getTier(0.40));
    }

    // ── calculateXP() ──

    @Test
    @DisplayName("calculateXP: COMMON sem first craft deve retornar base")
    void shouldReturnBaseXP_whenCommonAndNotFirstCraft() {
        double xp = calculator.calculateXP(100, QualityTier.COMMON, false);
        assertEquals(100.0, xp, 0.001);
    }

    @Test
    @DisplayName("calculateXP: LEGENDARY deve multiplicar por 5")
    void shouldMultiplyBy5_whenLegendary() {
        double xp = calculator.calculateXP(100, QualityTier.LEGENDARY, false);
        assertEquals(500.0, xp, 0.001);
    }

    @Test
    @DisplayName("calculateXP: first craft deve dobrar XP")
    void shouldDoubleXP_whenFirstCraft() {
        double xp = calculator.calculateXP(100, QualityTier.COMMON, true);
        assertEquals(200.0, xp, 0.001);
    }

    @Test
    @DisplayName("calculateXP: DEFECTIVE deve dar 25% do XP")
    void shouldGive25Percent_whenDefective() {
        double xp = calculator.calculateXP(100, QualityTier.DEFECTIVE, false);
        assertEquals(25.0, xp, 0.001);
    }

    @Test
    @DisplayName("calculateXP: MASTERPIECE com first craft")
    void shouldCalculateMasterpieceFirstCraft() {
        double xp = calculator.calculateXP(50, QualityTier.MASTERPIECE, true);
        // 50 * 3.0 * 2.0 = 300
        assertEquals(300.0, xp, 0.001);
    }

    @Test
    @DisplayName("calculateXP: EXCEPTIONAL deve multiplicar por 2")
    void shouldMultiplyBy2_whenExceptional() {
        double xp = calculator.calculateXP(100, QualityTier.EXCEPTIONAL, false);
        assertEquals(200.0, xp, 0.001);
    }

    @Test
    @DisplayName("calculateXP: SUPERIOR deve multiplicar por 1.5")
    void shouldMultiplyBy1_5_whenSuperior() {
        double xp = calculator.calculateXP(200, QualityTier.SUPERIOR, false);
        assertEquals(300.0, xp, 0.001);
    }

    @Test
    @DisplayName("calculateXP: INFERIOR deve multiplicar por 0.5")
    void shouldMultiplyBy0_5_whenInferior() {
        double xp = calculator.calculateXP(100, QualityTier.INFERIOR, false);
        assertEquals(50.0, xp, 0.001);
    }

    // ── Constructor with custom weights ──

    @Test
    @DisplayName("construtor customizado deve definir pesos corretamente")
    void shouldSetCustomWeights() {
        QualityCalculator custom = new QualityCalculator(0.1, 0.2, 0.3, 0.2, 0.2, 0.001);
        assertEquals(0.1, custom.getMaterialWeight(), 0.001);
        assertEquals(0.2, custom.getHeatingWeight(), 0.001);
        assertEquals(0.3, custom.getHammeringWeight(), 0.001);
    }

    // ── Setters ──

    @Test
    @DisplayName("setQualityCurve deve alterar curva de diminishing returns")
    void shouldChangeQualityCurve() {
        calculator.setQualityCurve(2.0);
        assertEquals(2.0, calculator.getQualityCurve(), 0.001);
    }

    @Test
    @DisplayName("setRandomVariance deve alterar variância")
    void shouldChangeRandomVariance() {
        calculator.setRandomVariance(0.1);
        assertEquals(0.1, calculator.getRandomVariance(), 0.001);
    }
}

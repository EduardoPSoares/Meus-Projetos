package me.ray.midgard.modules.professions.blacksmith.forge.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class QualityTierTest {

    // ── fromScore() ──

    @ParameterizedTest
    @CsvSource({
        "0.00, DEFECTIVE",
        "0.14, DEFECTIVE",
        "0.15, INFERIOR",
        "0.34, INFERIOR",
        "0.35, COMMON",
        "0.54, COMMON",
        "0.55, SUPERIOR",
        "0.74, SUPERIOR",
        "0.75, EXCEPTIONAL",
        "0.89, EXCEPTIONAL",
        "0.90, MASTERPIECE",
        "0.97, MASTERPIECE",
        "0.98, LEGENDARY",
        "1.00, LEGENDARY"
    })
    @DisplayName("fromScore: deve mapear scores aos tiers corretos")
    void shouldMapScoresToCorrectTiers(double score, QualityTier expected) {
        assertEquals(expected, QualityTier.fromScore(score));
    }

    @Test
    @DisplayName("fromScore: score negativo deve retornar DEFECTIVE")
    void shouldReturnDefective_whenNegativeScore() {
        assertEquals(QualityTier.DEFECTIVE, QualityTier.fromScore(-0.5));
    }

    @Test
    @DisplayName("fromScore: score acima de 1.0 deve retornar LEGENDARY")
    void shouldReturnLegendary_whenScoreAboveOne() {
        assertEquals(QualityTier.LEGENDARY, QualityTier.fromScore(1.5));
    }

    // ── Propriedades dos tiers ──

    @Test
    @DisplayName("DEFECTIVE deve ter level 0 e statMultiplier 0.50")
    void shouldHaveCorrectDefectiveProperties() {
        assertEquals(0, QualityTier.DEFECTIVE.getLevel());
        assertEquals(0.50, QualityTier.DEFECTIVE.getStatMultiplier(), 0.001);
        assertEquals("<dark_gray>", QualityTier.DEFECTIVE.getColorTag());
    }

    @Test
    @DisplayName("LEGENDARY deve ter level 6 e statMultiplier 1.75")
    void shouldHaveCorrectLegendaryProperties() {
        assertEquals(6, QualityTier.LEGENDARY.getLevel());
        assertEquals(1.75, QualityTier.LEGENDARY.getStatMultiplier(), 0.001);
        assertEquals("<light_purple>", QualityTier.LEGENDARY.getColorTag());
    }

    @Test
    @DisplayName("COMMON deve ter statMultiplier 1.0 (neutro)")
    void shouldHaveNeutralMultiplierForCommon() {
        assertEquals(1.00, QualityTier.COMMON.getStatMultiplier(), 0.001);
    }

    @Test
    @DisplayName("statMultiplier deve crescer com o tier")
    void shouldHaveIncreasingStatMultiplier() {
        QualityTier[] tiers = QualityTier.values();
        for (int i = 1; i < tiers.length; i++) {
            assertTrue(tiers[i].getStatMultiplier() > tiers[i - 1].getStatMultiplier(),
                    tiers[i].name() + " deve ter multiplier maior que " + tiers[i - 1].name());
        }
    }

    @Test
    @DisplayName("level deve crescer sequencialmente de 0 a 6")
    void shouldHaveSequentialLevels() {
        QualityTier[] tiers = QualityTier.values();
        for (int i = 0; i < tiers.length; i++) {
            assertEquals(i, tiers[i].getLevel());
        }
    }

    // ── Score ranges ──

    @ParameterizedTest
    @EnumSource(QualityTier.class)
    @DisplayName("minScore deve ser menor ou igual a maxScore para todos os tiers")
    void shouldHaveMinScoreLessOrEqualToMaxScore(QualityTier tier) {
        assertTrue(tier.getMinScore() <= tier.getMaxScore(),
                tier.name() + ": minScore=" + tier.getMinScore() + " > maxScore=" + tier.getMaxScore());
    }

    // ── getQualityBar() ──

    @Test
    @DisplayName("getQualityBar deve retornar barra com 10 caracteres")
    void shouldReturnBarWith10Characters() {
        for (QualityTier tier : QualityTier.values()) {
            String bar = tier.getQualityBar();
            assertNotNull(bar);
            assertFalse(bar.isEmpty());
        }
    }

    // ── getFormattedName() ──

    @ParameterizedTest
    @EnumSource(QualityTier.class)
    @DisplayName("getFormattedName deve conter colorTag")
    void shouldContainColorTagInFormattedName(QualityTier tier) {
        String formatted = tier.getFormattedName();
        assertTrue(formatted.startsWith(tier.getColorTag()),
                "Formatted name deve começar com colorTag");
    }

    // ── 7 tiers ──

    @Test
    @DisplayName("deve haver exatamente 7 tiers")
    void shouldHaveExactly7Tiers() {
        assertEquals(7, QualityTier.values().length);
    }
}

package me.ray.midgard.modules.professions.blacksmith.forge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class ForgeTierTest {

    @Test
    @DisplayName("deve haver exatamente 5 tiers")
    void shouldHave5Tiers() {
        assertEquals(5, ForgeTier.values().length);
    }

    @ParameterizedTest
    @CsvSource({
        "1, BASIC",
        "2, INTERMEDIATE",
        "3, ADVANCED",
        "4, MASTER",
        "5, LEGENDARY"
    })
    @DisplayName("fromLevel deve retornar o tier correto")
    void shouldReturnCorrectTierFromLevel(int level, ForgeTier expected) {
        assertEquals(expected, ForgeTier.fromLevel(level));
    }

    @Test
    @DisplayName("fromLevel deve retornar null para level inválido")
    void shouldReturnNull_forInvalidLevel() {
        assertNull(ForgeTier.fromLevel(0));
        assertNull(ForgeTier.fromLevel(6));
        assertNull(ForgeTier.fromLevel(-1));
    }

    @Test
    @DisplayName("BASIC deve ter level mínimo 1 com dimensão 5x4x5")
    void shouldHaveCorrectBasicProperties() {
        assertEquals(1, ForgeTier.BASIC.getLevel());
        assertEquals(5, ForgeTier.BASIC.getWidth());
        assertEquals(4, ForgeTier.BASIC.getHeight());
        assertEquals(5, ForgeTier.BASIC.getDepth());
        assertEquals(1, ForgeTier.BASIC.getRequiredProfessionLevel());
    }

    @Test
    @DisplayName("LEGENDARY deve ter level 5 e requerer nível 95")
    void shouldHaveCorrectLegendaryProperties() {
        assertEquals(5, ForgeTier.LEGENDARY.getLevel());
        assertEquals(95, ForgeTier.LEGENDARY.getRequiredProfessionLevel());
    }

    @Test
    @DisplayName("requiredProfessionLevel deve crescer com cada tier")
    void shouldHaveIncreasingProfessionLevel() {
        ForgeTier[] tiers = ForgeTier.values();
        for (int i = 1; i < tiers.length; i++) {
            assertTrue(tiers[i].getRequiredProfessionLevel() > tiers[i - 1].getRequiredProfessionLevel(),
                    tiers[i].name() + " deve requerer nível maior que " + tiers[i - 1].name());
        }
    }

    @ParameterizedTest
    @EnumSource(ForgeTier.class)
    @DisplayName("dimensões devem ser positivas para todos os tiers")
    void shouldHavePositiveDimensions(ForgeTier tier) {
        assertTrue(tier.getWidth() > 0);
        assertTrue(tier.getHeight() > 0);
        assertTrue(tier.getDepth() > 0);
    }

    @ParameterizedTest
    @EnumSource(ForgeTier.class)
    @DisplayName("getName e getDisplayName não devem ser nulos")
    void shouldHaveNonNullNames(ForgeTier tier) {
        assertNotNull(tier.getName());
        assertNotNull(tier.getDisplayName());
    }
}

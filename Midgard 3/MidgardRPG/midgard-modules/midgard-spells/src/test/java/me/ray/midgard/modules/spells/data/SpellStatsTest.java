package me.ray.midgard.modules.spells.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpellStatsTest {

    private SpellStats stats;

    @BeforeEach
    void setUp() {
        stats = new SpellStats();
    }

    @Test
    @DisplayName("Valores iniciais devem ser zero")
    void shouldHaveZeroDefaults() {
        assertEquals(0, stats.getCasts());
        assertEquals(0, stats.getKillsWithSpell());
        assertEquals(0.0, stats.getTotalDamage());
        assertEquals(0L, stats.getLastCastTime());
    }

    @Nested
    @DisplayName("incrementCasts")
    class IncrementCasts {

        @Test
        @DisplayName("Deve incrementar casts e setar lastCastTime")
        void shouldIncrementCastsAndSetTime() {
            long before = System.currentTimeMillis();
            stats.incrementCasts();
            long after = System.currentTimeMillis();

            assertEquals(1, stats.getCasts());
            assertTrue(stats.getLastCastTime() >= before);
            assertTrue(stats.getLastCastTime() <= after);
        }

        @Test
        @DisplayName("Deve incrementar múltiplas vezes")
        void shouldIncrementMultipleTimes() {
            stats.incrementCasts();
            stats.incrementCasts();
            stats.incrementCasts();
            assertEquals(3, stats.getCasts());
        }
    }

    @Nested
    @DisplayName("addDamage")
    class AddDamage {

        @Test
        @DisplayName("Deve acumular dano")
        void shouldAccumulateDamage() {
            stats.addDamage(50.5);
            stats.addDamage(25.3);
            assertEquals(75.8, stats.getTotalDamage(), 0.001);
        }

        @Test
        @DisplayName("Dano zero não altera total")
        void shouldNotChange_forZeroDamage() {
            stats.addDamage(0);
            assertEquals(0.0, stats.getTotalDamage());
        }
    }

    @Nested
    @DisplayName("incrementKills")
    class IncrementKills {

        @Test
        @DisplayName("Deve incrementar kills")
        void shouldIncrementKills() {
            stats.incrementKills();
            assertEquals(1, stats.getKillsWithSpell());
        }

        @Test
        @DisplayName("Deve incrementar kills múltiplas vezes")
        void shouldIncrementKillsMultipleTimes() {
            for (int i = 0; i < 5; i++) {
                stats.incrementKills();
            }
            assertEquals(5, stats.getKillsWithSpell());
        }
    }

    @Test
    @DisplayName("Cenário completo: cast, dano, kill")
    void shouldTrackFullScenario() {
        stats.incrementCasts();
        stats.addDamage(100.0);
        stats.incrementKills();
        stats.incrementCasts();
        stats.addDamage(50.0);

        assertEquals(2, stats.getCasts());
        assertEquals(150.0, stats.getTotalDamage(), 0.001);
        assertEquals(1, stats.getKillsWithSpell());
        assertTrue(stats.getLastCastTime() > 0);
    }
}

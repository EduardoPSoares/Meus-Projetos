package me.ray.midgard.modules.item.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class ItemStatTest {

    @Test
    void shouldHaveExpectedStatCount() {
        // Ensure all stats are present (85 total based on source)
        assertTrue(ItemStat.values().length >= 80, "Should have at least 80 stats");
    }

    @Nested
    class PathAndName {

        @ParameterizedTest
        @EnumSource(ItemStat.class)
        void shouldHaveNonNullPath(ItemStat stat) {
            assertNotNull(stat.getPath());
            assertFalse(stat.getPath().isEmpty());
        }

        @ParameterizedTest
        @EnumSource(ItemStat.class)
        void shouldHaveNonNullName(ItemStat stat) {
            assertNotNull(stat.getName());
            assertFalse(stat.getName().isEmpty());
        }

        @Test
        void shouldHaveCorrectPath_forKnownStats() {
            assertEquals("attack-damage", ItemStat.ATTACK_DAMAGE.getPath());
            assertEquals("critical-strike-chance", ItemStat.CRITICAL_STRIKE_CHANCE.getPath());
            assertEquals("max-health", ItemStat.MAX_HEALTH.getPath());
            assertEquals("armor", ItemStat.ARMOR.getPath());
            assertEquals("lifesteal", ItemStat.LIFESTEAL.getPath());
        }

        @Test
        void shouldHaveCorrectName_forKnownStats() {
            assertEquals("Dano", ItemStat.ATTACK_DAMAGE.getName());
            assertEquals("Armadura", ItemStat.ARMOR.getName());
            assertEquals("Vida Máxima", ItemStat.MAX_HEALTH.getName());
            assertEquals("Roubo de Vida", ItemStat.LIFESTEAL.getName());
        }

        @ParameterizedTest
        @EnumSource(ItemStat.class)
        void pathsShouldBeKebabCase(ItemStat stat) {
            String path = stat.getPath();
            // All paths should be lowercase with hyphens
            assertEquals(path.toLowerCase(), path, "Path should be lowercase: " + path);
            assertFalse(path.contains("_"), "Path should use hyphens not underscores: " + path);
            assertFalse(path.contains(" "), "Path should not contain spaces: " + path);
        }
    }

    @Nested
    class FromPath {

        @ParameterizedTest
        @EnumSource(ItemStat.class)
        void shouldFindStat_byExactPath(ItemStat stat) {
            assertEquals(stat, ItemStat.fromPath(stat.getPath()));
        }

        @Test
        void shouldBeCaseInsensitive() {
            assertEquals(ItemStat.ATTACK_DAMAGE, ItemStat.fromPath("ATTACK-DAMAGE"));
            assertEquals(ItemStat.MAX_HEALTH, ItemStat.fromPath("Max-Health"));
        }

        @Test
        void shouldReturnNull_forUnknownPath() {
            assertNull(ItemStat.fromPath("non-existent-stat"));
        }

        @Test
        void shouldReturnNull_forNullPath() {
            assertNull(ItemStat.fromPath(null));
        }

        @Test
        void shouldReturnNull_forEmptyPath() {
            assertNull(ItemStat.fromPath(""));
        }
    }

    @Nested
    class ElementalStats {

        @Test
        void shouldHaveElementalDamageStats() {
            assertNotNull(ItemStat.FIRE_DAMAGE);
            assertNotNull(ItemStat.ICE_DAMAGE);
            assertNotNull(ItemStat.LIGHT_DAMAGE);
            assertNotNull(ItemStat.DARKNESS_DAMAGE);
            assertNotNull(ItemStat.DIVINE_DAMAGE);
        }

        @Test
        void shouldHaveElementalReductionStats() {
            assertNotNull(ItemStat.FIRE_DAMAGE_REDUCTION);
            assertNotNull(ItemStat.ICE_DAMAGE_REDUCTION);
            assertNotNull(ItemStat.LIGHT_DAMAGE_REDUCTION);
            assertNotNull(ItemStat.DARKNESS_DAMAGE_REDUCTION);
            assertNotNull(ItemStat.DIVINE_DAMAGE_REDUCTION);
        }
    }

    @Nested
    class RPGStats {

        @Test
        void shouldHaveBaseRPGStats() {
            assertNotNull(ItemStat.STRENGTH);
            assertNotNull(ItemStat.INTELLIGENCE);
            assertNotNull(ItemStat.DEXTERITY);
        }

        @Test
        void shouldHavePenetrationStats() {
            assertNotNull(ItemStat.ARMOR_PENETRATION);
            assertNotNull(ItemStat.ARMOR_PENETRATION_FLAT);
            assertNotNull(ItemStat.MAGIC_PENETRATION);
            assertNotNull(ItemStat.MAGIC_PENETRATION_FLAT);
        }
    }
}

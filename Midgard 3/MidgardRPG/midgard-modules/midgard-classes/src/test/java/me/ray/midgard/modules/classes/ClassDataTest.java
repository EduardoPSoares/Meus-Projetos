package me.ray.midgard.modules.classes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClassDataTest {

    private ClassData data;

    @BeforeEach
    void setUp() {
        data = new ClassData();
    }

    // ============================================
    // INITIAL VALUES
    // ============================================

    @Test
    void shouldHaveNullClassNameByDefault() {
        assertNull(data.getClassName());
    }

    @Test
    void shouldHaveLevel1ByDefault() {
        assertEquals(1, data.getLevel());
    }

    @Test
    void shouldHaveZeroExperienceByDefault() {
        assertEquals(0.0, data.getExperience());
    }

    @Test
    void shouldHaveZeroAttributePointsByDefault() {
        assertEquals(0, data.getAttributePoints());
    }

    @Test
    void shouldHaveZeroSkillPointsByDefault() {
        assertEquals(0, data.getSkillPoints());
    }

    @Test
    void shouldHaveEmptySpentPointsByDefault() {
        assertNotNull(data.getSpentPoints());
        assertTrue(data.getSpentPoints().isEmpty());
    }

    @Test
    void shouldHaveEmptyUnlockedSkillNodesByDefault() {
        assertNotNull(data.getUnlockedSkillNodes());
        assertTrue(data.getUnlockedSkillNodes().isEmpty());
    }

    @Test
    void shouldNotHaveClassByDefault() {
        assertFalse(data.hasClass());
    }

    // ============================================
    // CLASS NAME
    // ============================================

    @Nested
    class ClassNameTests {

        @Test
        void shouldSetAndGetClassName() {
            data.setClassName("guerreiro");
            assertEquals("guerreiro", data.getClassName());
        }

        @Test
        void shouldAllowNullClassName() {
            data.setClassName("mago");
            data.setClassName(null);
            assertNull(data.getClassName());
        }

        @Test
        void shouldAllowEmptyClassName() {
            data.setClassName("");
            assertEquals("", data.getClassName());
        }

        @Test
        void hasClassShouldReturnTrueWhenSet() {
            data.setClassName("arqueiro");
            assertTrue(data.hasClass());
        }

        @Test
        void hasClassShouldReturnFalseWhenNull() {
            data.setClassName(null);
            assertFalse(data.hasClass());
        }

        @Test
        void hasClassShouldReturnFalseWhenEmpty() {
            data.setClassName("");
            assertFalse(data.hasClass());
        }
    }

    // ============================================
    // LEVEL
    // ============================================

    @Nested
    class LevelTests {

        @Test
        void shouldSetAndGetLevel() {
            data.setLevel(50);
            assertEquals(50, data.getLevel());
        }

        @Test
        void shouldAllowLevelZero() {
            data.setLevel(0);
            assertEquals(0, data.getLevel());
        }

        @Test
        void shouldAllowHighLevel() {
            data.setLevel(999);
            assertEquals(999, data.getLevel());
        }
    }

    // ============================================
    // EXPERIENCE
    // ============================================

    @Nested
    class ExperienceTests {

        @Test
        void shouldSetAndGetExperience() {
            data.setExperience(1500.75);
            assertEquals(1500.75, data.getExperience());
        }

        @Test
        void shouldAllowZeroExperience() {
            data.setExperience(0);
            assertEquals(0, data.getExperience());
        }

        @Test
        void shouldAllowLargeExperience() {
            data.setExperience(999999.99);
            assertEquals(999999.99, data.getExperience());
        }
    }

    // ============================================
    // ATTRIBUTE POINTS
    // ============================================

    @Nested
    class AttributePointsTests {

        @Test
        void shouldSetAndGetAttributePoints() {
            data.setAttributePoints(10);
            assertEquals(10, data.getAttributePoints());
        }

        @Test
        void shouldAddAttributePoints() {
            data.setAttributePoints(5);
            data.addAttributePoints(3);
            assertEquals(8, data.getAttributePoints());
        }

        @Test
        void shouldAddNegativeAttributePoints() {
            data.setAttributePoints(10);
            data.addAttributePoints(-3);
            assertEquals(7, data.getAttributePoints());
        }

        @Test
        void shouldAddZeroAttributePoints() {
            data.setAttributePoints(5);
            data.addAttributePoints(0);
            assertEquals(5, data.getAttributePoints());
        }

        @Test
        void shouldAddMultipleTimesAttributePoints() {
            data.addAttributePoints(3);
            data.addAttributePoints(5);
            data.addAttributePoints(2);
            assertEquals(10, data.getAttributePoints());
        }
    }

    // ============================================
    // SKILL POINTS
    // ============================================

    @Nested
    class SkillPointsTests {

        @Test
        void shouldSetAndGetSkillPoints() {
            data.setSkillPoints(15);
            assertEquals(15, data.getSkillPoints());
        }

        @Test
        void shouldAddSkillPoints() {
            data.setSkillPoints(5);
            data.addSkillPoints(10);
            assertEquals(15, data.getSkillPoints());
        }

        @Test
        void shouldAddNegativeSkillPoints() {
            data.setSkillPoints(10);
            data.addSkillPoints(-4);
            assertEquals(6, data.getSkillPoints());
        }

        @Test
        void shouldAddZeroSkillPoints() {
            data.setSkillPoints(7);
            data.addSkillPoints(0);
            assertEquals(7, data.getSkillPoints());
        }
    }

    // ============================================
    // SPENT POINTS
    // ============================================

    @Nested
    class SpentPointsTests {

        @Test
        void shouldReturnEmptyMapInitially() {
            assertTrue(data.getSpentPoints().isEmpty());
        }

        @Test
        void shouldReturnZeroForUnknownAttribute() {
            assertEquals(0, data.getSpentPoints("strength"));
        }

        @Test
        void shouldReturnZeroForNullAttribute() {
            assertEquals(0, data.getSpentPoints(null));
        }

        @Test
        void shouldAddSpentPoints() {
            data.addSpentPoints("strength", 5);
            assertEquals(5, data.getSpentPoints("strength"));
        }

        @Test
        void shouldAccumulateSpentPoints() {
            data.addSpentPoints("strength", 3);
            data.addSpentPoints("strength", 2);
            assertEquals(5, data.getSpentPoints("strength"));
        }

        @Test
        void shouldTrackMultipleAttributes() {
            data.addSpentPoints("strength", 5);
            data.addSpentPoints("defense", 3);
            data.addSpentPoints("vitality", 7);

            assertEquals(5, data.getSpentPoints("strength"));
            assertEquals(3, data.getSpentPoints("defense"));
            assertEquals(7, data.getSpentPoints("vitality"));
        }

        @Test
        void shouldIgnoreNullAttributeOnAdd() {
            data.addSpentPoints(null, 5);
            assertTrue(data.getSpentPoints().isEmpty());
        }

        @Test
        void shouldSetSpentPointsMap() {
            Map<String, Integer> points = new HashMap<>();
            points.put("strength", 10);
            points.put("defense", 5);

            data.setSpentPoints(points);

            assertEquals(10, data.getSpentPoints("strength"));
            assertEquals(5, data.getSpentPoints("defense"));
        }

        @Test
        void shouldSetSpentPointsWithNull() {
            data.addSpentPoints("strength", 5);
            data.setSpentPoints(null);
            assertNotNull(data.getSpentPoints());
            assertTrue(data.getSpentPoints().isEmpty());
        }

        @Test
        void shouldCreateDefensiveCopyOnSet() {
            Map<String, Integer> points = new HashMap<>();
            points.put("strength", 10);
            data.setSpentPoints(points);

            // Modify original map — data should not be affected
            points.put("strength", 999);
            assertEquals(10, data.getSpentPoints("strength"));
        }

        @Test
        void shouldLazyInitSpentPointsIfNull() throws Exception {
            // Force spentPoints to null via reflection
            var field = ClassData.class.getDeclaredField("spentPoints");
            field.setAccessible(true);
            field.set(data, null);

            assertNotNull(data.getSpentPoints());
            assertTrue(data.getSpentPoints().isEmpty());
        }
    }

    // ============================================
    // SKILL NODES
    // ============================================

    @Nested
    class SkillNodeTests {

        @Test
        void shouldNotHaveUnlockedNodeByDefault() {
            assertFalse(data.isSkillNodeUnlocked("fireball"));
        }

        @Test
        void shouldUnlockSkillNode() {
            data.unlockSkillNode("fireball");
            assertTrue(data.isSkillNodeUnlocked("fireball"));
        }

        @Test
        void shouldUnlockMultipleNodes() {
            data.unlockSkillNode("fireball");
            data.unlockSkillNode("icebolt");
            data.unlockSkillNode("heal");

            assertTrue(data.isSkillNodeUnlocked("fireball"));
            assertTrue(data.isSkillNodeUnlocked("icebolt"));
            assertTrue(data.isSkillNodeUnlocked("heal"));
        }

        @Test
        void shouldNotDuplicateOnDoubleUnlock() {
            data.unlockSkillNode("fireball");
            data.unlockSkillNode("fireball");

            Set<String> nodes = data.getUnlockedSkillNodes();
            assertEquals(1, nodes.size());
            assertTrue(nodes.contains("fireball"));
        }

        @Test
        void shouldReturnAllUnlockedNodes() {
            data.unlockSkillNode("a");
            data.unlockSkillNode("b");
            data.unlockSkillNode("c");

            Set<String> nodes = data.getUnlockedSkillNodes();
            assertEquals(3, nodes.size());
            assertTrue(nodes.contains("a"));
            assertTrue(nodes.contains("b"));
            assertTrue(nodes.contains("c"));
        }
    }

    // ============================================
    // COMBINED STATE
    // ============================================

    @Nested
    class CombinedStateTests {

        @Test
        void shouldSupportFullProgression() {
            data.setClassName("guerreiro");
            data.setLevel(25);
            data.setExperience(12500.5);
            data.setAttributePoints(50);
            data.setSkillPoints(12);
            data.addSpentPoints("strength", 20);
            data.addSpentPoints("defense", 15);
            data.unlockSkillNode("sword_mastery");
            data.unlockSkillNode("shield_bash");

            assertTrue(data.hasClass());
            assertEquals("guerreiro", data.getClassName());
            assertEquals(25, data.getLevel());
            assertEquals(12500.5, data.getExperience());
            assertEquals(50, data.getAttributePoints());
            assertEquals(12, data.getSkillPoints());
            assertEquals(20, data.getSpentPoints("strength"));
            assertEquals(15, data.getSpentPoints("defense"));
            assertTrue(data.isSkillNodeUnlocked("sword_mastery"));
            assertTrue(data.isSkillNodeUnlocked("shield_bash"));
            assertFalse(data.isSkillNodeUnlocked("fireball"));
        }

        @Test
        void shouldResetClassState() {
            data.setClassName("mago");
            data.setLevel(10);
            data.setExperience(5000);

            data.setClassName(null);
            data.setLevel(1);
            data.setExperience(0);

            assertFalse(data.hasClass());
            assertEquals(1, data.getLevel());
            assertEquals(0.0, data.getExperience());
        }
    }
}

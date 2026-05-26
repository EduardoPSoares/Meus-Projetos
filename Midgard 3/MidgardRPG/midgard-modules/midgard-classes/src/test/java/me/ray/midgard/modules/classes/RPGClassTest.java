package me.ray.midgard.modules.classes;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RPGClassTest {

    private RPGClass createClass(ItemStack icon) {
        return new RPGClass(
                "guerreiro",
                "Guerreiro",
                icon,
                List.of("Um bravo guerreiro", "Mestre das armas"),
                Map.of("strength", 15.0, "defense", 10.0),
                Map.of("strength", 2.0, "defense", 1.5),
                100.0, 12.0,
                50.0, 5.0,
                List.of(new ClassSkillLink("slash", 1), new ClassSkillLink("whirlwind", 5))
        );
    }

    private RPGClass createMinimalClass() {
        return new RPGClass(
                "test",
                "Test",
                null,
                null,
                null,
                null,
                0, 0,
                0, 0,
                null
        );
    }

    // ============================================
    // CONSTRUCTOR & GETTERS
    // ============================================

    @Nested
    class ConstructorTests {

        @Test
        void shouldReturnCorrectId() {
            RPGClass rpgClass = createClass(null);
            assertEquals("guerreiro", rpgClass.getId());
        }

        @Test
        void shouldReturnCorrectDisplayName() {
            RPGClass rpgClass = createClass(null);
            assertEquals("Guerreiro", rpgClass.getDisplayName());
        }

        @Test
        void shouldReturnCorrectLore() {
            RPGClass rpgClass = createClass(null);
            assertEquals(List.of("Um bravo guerreiro", "Mestre das armas"), rpgClass.getLore());
        }

        @Test
        void shouldReturnCorrectBaseAttributes() {
            RPGClass rpgClass = createClass(null);
            Map<String, Double> attrs = rpgClass.getBaseAttributes();
            assertEquals(15.0, attrs.get("strength"));
            assertEquals(10.0, attrs.get("defense"));
        }

        @Test
        void shouldReturnCorrectAttributesPerLevel() {
            RPGClass rpgClass = createClass(null);
            Map<String, Double> perLevel = rpgClass.getAttributesPerLevel();
            assertEquals(2.0, perLevel.get("strength"));
            assertEquals(1.5, perLevel.get("defense"));
        }

        @Test
        void shouldReturnCorrectBaseHealth() {
            RPGClass rpgClass = createClass(null);
            assertEquals(100.0, rpgClass.getBaseHealth());
        }

        @Test
        void shouldReturnCorrectHealthPerLevel() {
            RPGClass rpgClass = createClass(null);
            assertEquals(12.0, rpgClass.getHealthPerLevel());
        }

        @Test
        void shouldReturnCorrectBaseMana() {
            RPGClass rpgClass = createClass(null);
            assertEquals(50.0, rpgClass.getBaseMana());
        }

        @Test
        void shouldReturnCorrectManaPerLevel() {
            RPGClass rpgClass = createClass(null);
            assertEquals(5.0, rpgClass.getManaPerLevel());
        }

        @Test
        void shouldReturnCorrectSkills() {
            RPGClass rpgClass = createClass(null);
            List<ClassSkillLink> skills = rpgClass.getSkills();
            assertNotNull(skills);
            assertEquals(2, skills.size());
            assertEquals("slash", skills.get(0).getSkillId());
            assertEquals("whirlwind", skills.get(1).getSkillId());
        }
    }

    // ============================================
    // ICON
    // ============================================

    @Nested
    class IconTests {

        @Test
        void shouldReturnNullWhenIconIsNull() {
            RPGClass rpgClass = createClass(null);
            assertNull(rpgClass.getIcon());
        }

        @Test
        void shouldReturnCloneOfIcon() {
            ItemStack original = mock(ItemStack.class);
            ItemStack cloned = mock(ItemStack.class);
            when(original.clone()).thenReturn(cloned);

            RPGClass rpgClass = createClass(original);
            ItemStack result = rpgClass.getIcon();
            assertSame(cloned, result);
            verify(original).clone();
        }

        @Test
        void shouldReturnDifferentCloneEachTime() {
            ItemStack original = mock(ItemStack.class);
            ItemStack clone1 = mock(ItemStack.class);
            ItemStack clone2 = mock(ItemStack.class);
            when(original.clone()).thenReturn(clone1, clone2);

            RPGClass rpgClass = createClass(original);
            assertSame(clone1, rpgClass.getIcon());
            assertSame(clone2, rpgClass.getIcon());
        }
    }

    // ============================================
    // NULL FIELDS
    // ============================================

    @Nested
    class NullFieldTests {

        @Test
        void shouldHandleNullLore() {
            RPGClass rpgClass = createMinimalClass();
            assertNull(rpgClass.getLore());
        }

        @Test
        void shouldHandleNullBaseAttributes() {
            RPGClass rpgClass = createMinimalClass();
            assertNull(rpgClass.getBaseAttributes());
        }

        @Test
        void shouldHandleNullAttributesPerLevel() {
            RPGClass rpgClass = createMinimalClass();
            assertNull(rpgClass.getAttributesPerLevel());
        }

        @Test
        void shouldHandleNullSkills() {
            RPGClass rpgClass = createMinimalClass();
            assertNull(rpgClass.getSkills());
        }

        @Test
        void shouldHandleNullIcon() {
            RPGClass rpgClass = createMinimalClass();
            assertNull(rpgClass.getIcon());
        }

        @Test
        void shouldHaveZeroBaseHealthWhenMinimal() {
            RPGClass rpgClass = createMinimalClass();
            assertEquals(0.0, rpgClass.getBaseHealth());
        }

        @Test
        void shouldHaveZeroBaseManaWhenMinimal() {
            RPGClass rpgClass = createMinimalClass();
            assertEquals(0.0, rpgClass.getBaseMana());
        }
    }

    // ============================================
    // IMMUTABILITY
    // ============================================

    @Nested
    class ImmutabilityTests {

        @Test
        void shouldReturnSameIdOnMultipleCalls() {
            RPGClass rpgClass = createClass(null);
            assertSame(rpgClass.getId(), rpgClass.getId());
        }

        @Test
        void shouldReturnSameDisplayNameOnMultipleCalls() {
            RPGClass rpgClass = createClass(null);
            assertSame(rpgClass.getDisplayName(), rpgClass.getDisplayName());
        }

        @Test
        void shouldReturnConsistentBaseHealth() {
            RPGClass rpgClass = createClass(null);
            assertEquals(rpgClass.getBaseHealth(), rpgClass.getBaseHealth());
        }
    }

    // ============================================
    // EDGE CASES
    // ============================================

    @Nested
    class EdgeCaseTests {

        @Test
        void shouldSupportEmptyLore() {
            RPGClass rpgClass = new RPGClass(
                    "test", "Test", null, Collections.emptyList(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    0, 0, 0, 0, Collections.emptyList()
            );
            assertTrue(rpgClass.getLore().isEmpty());
            assertTrue(rpgClass.getBaseAttributes().isEmpty());
            assertTrue(rpgClass.getSkills().isEmpty());
        }

        @Test
        void shouldSupportNegativeValues() {
            RPGClass rpgClass = new RPGClass(
                    "test", "Test", null, null,
                    Map.of("strength", -5.0), Map.of("strength", -1.0),
                    -10, -2, -5, -1, null
            );
            assertEquals(-10, rpgClass.getBaseHealth());
            assertEquals(-5, rpgClass.getBaseMana());
            assertEquals(-5.0, rpgClass.getBaseAttributes().get("strength"));
        }
    }
}

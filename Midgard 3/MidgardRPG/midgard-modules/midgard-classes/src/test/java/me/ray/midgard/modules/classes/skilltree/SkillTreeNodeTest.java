package me.ray.midgard.modules.classes.skilltree;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SkillTreeNodeTest {

    private SkillTreeNode createNode() {
        Map<Integer, List<String>> lorePerLevel = new HashMap<>();
        lorePerLevel.put(1, List.of("Nível 1: Dano básico"));
        lorePerLevel.put(2, List.of("Nível 2: Dano aumentado", "Bônus de velocidade"));
        lorePerLevel.put(3, List.of("Nível 3: Dano máximo"));

        Map<String, Integer> parents = new HashMap<>();
        parents.put("basic_combat", 2);
        parents.put("weapon_mastery", 1);

        return new SkillTreeNode("sword_mastery", "Maestria com Espada", lorePerLevel, 3, 2, 1, false, parents);
    }

    private SkillTreeNode createRootNode() {
        return new SkillTreeNode("root_skill", "Raiz", Map.of(1, List.of("Habilidade base")), 1, 0, 0, true, Collections.emptyMap());
    }

    // ============================================
    // BASIC GETTERS
    // ============================================

    @Nested
    class BasicGetterTests {

        @Test
        void shouldReturnCorrectId() {
            SkillTreeNode node = createNode();
            assertEquals("sword_mastery", node.getId());
        }

        @Test
        void shouldReturnCorrectName() {
            SkillTreeNode node = createNode();
            assertEquals("Maestria com Espada", node.getName());
        }

        @Test
        void shouldReturnDisplayNameAsAlias() {
            SkillTreeNode node = createNode();
            assertEquals(node.getName(), node.getDisplayName());
        }

        @Test
        void shouldReturnCorrectX() {
            SkillTreeNode node = createNode();
            assertEquals(2, node.getX());
        }

        @Test
        void shouldReturnCorrectY() {
            SkillTreeNode node = createNode();
            assertEquals(1, node.getY());
        }

        @Test
        void shouldReturnCorrectMaxLevel() {
            SkillTreeNode node = createNode();
            assertEquals(3, node.getMaxLevel());
        }

        @Test
        void shouldNotBeRoot() {
            SkillTreeNode node = createNode();
            assertFalse(node.isRoot());
        }
    }

    // ============================================
    // ROOT NODE
    // ============================================

    @Nested
    class RootNodeTests {

        @Test
        void shouldBeRoot() {
            SkillTreeNode root = createRootNode();
            assertTrue(root.isRoot());
        }

        @Test
        void shouldHaveEmptyParents() {
            SkillTreeNode root = createRootNode();
            assertTrue(root.getParents().isEmpty());
        }

        @Test
        void shouldHaveMaxLevel1() {
            SkillTreeNode root = createRootNode();
            assertEquals(1, root.getMaxLevel());
        }

        @Test
        void shouldHaveOriginCoordinates() {
            SkillTreeNode root = createRootNode();
            assertEquals(0, root.getX());
            assertEquals(0, root.getY());
        }
    }

    // ============================================
    // LORE PER LEVEL
    // ============================================

    @Nested
    class LoreTests {

        @Test
        void shouldReturnLoreForLevel1() {
            SkillTreeNode node = createNode();
            List<String> lore = node.getLore(1);
            assertEquals(1, lore.size());
            assertEquals("Nível 1: Dano básico", lore.get(0));
        }

        @Test
        void shouldReturnLoreForLevel2() {
            SkillTreeNode node = createNode();
            List<String> lore = node.getLore(2);
            assertEquals(2, lore.size());
            assertEquals("Nível 2: Dano aumentado", lore.get(0));
            assertEquals("Bônus de velocidade", lore.get(1));
        }

        @Test
        void shouldReturnLoreForLevel3() {
            SkillTreeNode node = createNode();
            List<String> lore = node.getLore(3);
            assertEquals(1, lore.size());
            assertEquals("Nível 3: Dano máximo", lore.get(0));
        }

        @Test
        void shouldReturnEmptyListForInvalidLevel() {
            SkillTreeNode node = createNode();
            List<String> lore = node.getLore(99);
            assertNotNull(lore);
            assertTrue(lore.isEmpty());
        }

        @Test
        void shouldReturnEmptyListForLevel0() {
            SkillTreeNode node = createNode();
            List<String> lore = node.getLore(0);
            assertNotNull(lore);
            assertTrue(lore.isEmpty());
        }

        @Test
        void shouldReturnEmptyListForNegativeLevel() {
            SkillTreeNode node = createNode();
            List<String> lore = node.getLore(-1);
            assertNotNull(lore);
            assertTrue(lore.isEmpty());
        }
    }

    // ============================================
    // PARENTS
    // ============================================

    @Nested
    class ParentTests {

        @Test
        void shouldReturnCorrectParents() {
            SkillTreeNode node = createNode();
            Map<String, Integer> parents = node.getParents();
            assertEquals(2, parents.size());
            assertEquals(2, parents.get("basic_combat"));
            assertEquals(1, parents.get("weapon_mastery"));
        }

        @Test
        void shouldSupportNullParents() {
            SkillTreeNode node = new SkillTreeNode("test", "Test",
                    Collections.emptyMap(), 1, 0, 0, false, null);
            assertNull(node.getParents());
        }
    }

    // ============================================
    // COORDINATES
    // ============================================

    @Nested
    class CoordinateTests {

        @Test
        void shouldSupportNegativeCoordinates() {
            SkillTreeNode node = new SkillTreeNode("test", "Test",
                    Collections.emptyMap(), 1, -3, -2, false, Collections.emptyMap());
            assertEquals(-3, node.getX());
            assertEquals(-2, node.getY());
        }

        @Test
        void shouldSupportLargeCoordinates() {
            SkillTreeNode node = new SkillTreeNode("test", "Test",
                    Collections.emptyMap(), 1, 100, 200, false, Collections.emptyMap());
            assertEquals(100, node.getX());
            assertEquals(200, node.getY());
        }
    }

    // ============================================
    // EDGE CASES
    // ============================================

    @Nested
    class EdgeCaseTests {

        @Test
        void shouldSupportEmptyLoreMap() {
            SkillTreeNode node = new SkillTreeNode("test", "Test",
                    Collections.emptyMap(), 1, 0, 0, false, Collections.emptyMap());
            assertTrue(node.getLore(1).isEmpty());
        }

        @Test
        void shouldSupportNodeWithMaxLevelZero() {
            SkillTreeNode node = new SkillTreeNode("test", "Test",
                    Collections.emptyMap(), 0, 0, 0, false, Collections.emptyMap());
            assertEquals(0, node.getMaxLevel());
        }

        @Test
        void shouldSupportNullName() {
            SkillTreeNode node = new SkillTreeNode("test", null,
                    Collections.emptyMap(), 1, 0, 0, false, Collections.emptyMap());
            assertNull(node.getName());
            assertNull(node.getDisplayName());
        }
    }
}

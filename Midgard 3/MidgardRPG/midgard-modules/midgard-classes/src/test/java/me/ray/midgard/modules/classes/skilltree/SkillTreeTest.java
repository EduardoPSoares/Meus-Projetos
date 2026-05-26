package me.ray.midgard.modules.classes.skilltree;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SkillTreeTest {

    private SkillTree tree;

    @BeforeEach
    void setUp() {
        tree = new SkillTree("combat", "Combate", List.of("Árvore de habilidades de combate"), null, 20);
    }

    // ============================================
    // CONSTRUCTOR & BASIC GETTERS
    // ============================================

    @Nested
    class BasicGetterTests {

        @Test
        void shouldReturnCorrectId() {
            assertEquals("combat", tree.getId());
        }

        @Test
        void shouldReturnCorrectName() {
            assertEquals("Combate", tree.getName());
        }

        @Test
        void shouldReturnCorrectLore() {
            assertEquals(List.of("Árvore de habilidades de combate"), tree.getLore());
        }

        @Test
        void shouldReturnCorrectMaxPoints() {
            assertEquals(20, tree.getMaxPoints());
        }

        @Test
        void shouldReturnNullIcon() {
            assertNull(tree.getIcon());
        }

        @Test
        void shouldHaveEmptyNodesInitially() {
            assertTrue(tree.getNodes().isEmpty());
        }
    }

    // ============================================
    // NODE MANAGEMENT
    // ============================================

    @Nested
    class NodeManagementTests {

        private SkillTreeNode createNode(String id) {
            return new SkillTreeNode(id, "Node " + id,
                    Map.of(1, List.of("Lore")), 1, 0, 0, false, Collections.emptyMap());
        }

        @Test
        void shouldAddNode() {
            SkillTreeNode node = createNode("skill1");
            tree.addNode(node);
            assertEquals(1, tree.getNodes().size());
        }

        @Test
        void shouldGetNodeById() {
            SkillTreeNode node = createNode("skill1");
            tree.addNode(node);
            assertSame(node, tree.getNode("skill1"));
        }

        @Test
        void shouldReturnNullForUnknownNode() {
            assertNull(tree.getNode("nonexistent"));
        }

        @Test
        void shouldAddMultipleNodes() {
            tree.addNode(createNode("skill1"));
            tree.addNode(createNode("skill2"));
            tree.addNode(createNode("skill3"));
            assertEquals(3, tree.getNodes().size());
        }

        @Test
        void shouldOverrideNodeWithSameId() {
            SkillTreeNode node1 = createNode("skill1");
            SkillTreeNode node2 = new SkillTreeNode("skill1", "Updated",
                    Collections.emptyMap(), 5, 0, 0, true, Collections.emptyMap());

            tree.addNode(node1);
            tree.addNode(node2);

            assertEquals(1, tree.getNodes().size());
            assertSame(node2, tree.getNode("skill1"));
            assertEquals("Updated", tree.getNode("skill1").getName());
        }

        @Test
        void shouldReturnAllNodesAsMap() {
            tree.addNode(createNode("a"));
            tree.addNode(createNode("b"));

            Map<String, SkillTreeNode> nodes = tree.getNodes();
            assertEquals(2, nodes.size());
            assertNotNull(nodes.get("a"));
            assertNotNull(nodes.get("b"));
        }
    }

    // ============================================
    // EDGE CASES
    // ============================================

    @Nested
    class EdgeCaseTests {

        @Test
        void shouldSupportNullLore() {
            SkillTree t = new SkillTree("test", "Test", null, null, 0);
            assertNull(t.getLore());
        }

        @Test
        void shouldSupportEmptyLore() {
            SkillTree t = new SkillTree("test", "Test", Collections.emptyList(), null, 0);
            assertTrue(t.getLore().isEmpty());
        }

        @Test
        void shouldSupportZeroMaxPoints() {
            SkillTree t = new SkillTree("test", "Test", null, null, 0);
            assertEquals(0, t.getMaxPoints());
        }

        @Test
        void shouldSupportNegativeMaxPoints() {
            SkillTree t = new SkillTree("test", "Test", null, null, -1);
            assertEquals(-1, t.getMaxPoints());
        }

        @Test
        void shouldSupportNullName() {
            SkillTree t = new SkillTree("test", null, null, null, 10);
            assertNull(t.getName());
        }
    }
}

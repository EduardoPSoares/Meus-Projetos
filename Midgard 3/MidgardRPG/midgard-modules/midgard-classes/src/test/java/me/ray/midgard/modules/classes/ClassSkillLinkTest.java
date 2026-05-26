package me.ray.midgard.modules.classes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassSkillLinkTest {

    // ============================================
    // CONSTRUCTOR & GETTERS
    // ============================================

    @Test
    void shouldReturnCorrectSkillId() {
        ClassSkillLink link = new ClassSkillLink("fireball", 5);
        assertEquals("fireball", link.getSkillId());
    }

    @Test
    void shouldReturnCorrectUnlockLevel() {
        ClassSkillLink link = new ClassSkillLink("fireball", 5);
        assertEquals(5, link.getUnlockLevel());
    }

    @Test
    void shouldSupportLevel1Unlock() {
        ClassSkillLink link = new ClassSkillLink("basic_attack", 1);
        assertEquals(1, link.getUnlockLevel());
    }

    @Test
    void shouldSupportHighLevelUnlock() {
        ClassSkillLink link = new ClassSkillLink("ultimate", 100);
        assertEquals(100, link.getUnlockLevel());
    }

    @Test
    void shouldSupportNullSkillId() {
        ClassSkillLink link = new ClassSkillLink(null, 1);
        assertNull(link.getSkillId());
    }

    @Test
    void shouldSupportZeroUnlockLevel() {
        ClassSkillLink link = new ClassSkillLink("passive", 0);
        assertEquals(0, link.getUnlockLevel());
    }

    @Test
    void shouldReturnConsistentValues() {
        ClassSkillLink link = new ClassSkillLink("heal", 10);
        assertEquals("heal", link.getSkillId());
        assertEquals("heal", link.getSkillId());
        assertEquals(10, link.getUnlockLevel());
        assertEquals(10, link.getUnlockLevel());
    }

    // ============================================
    // DIFFERENT INSTANCES
    // ============================================

    @Test
    void shouldCreateIndependentInstances() {
        ClassSkillLink link1 = new ClassSkillLink("fireball", 5);
        ClassSkillLink link2 = new ClassSkillLink("icebolt", 10);

        assertNotEquals(link1.getSkillId(), link2.getSkillId());
        assertNotEquals(link1.getUnlockLevel(), link2.getUnlockLevel());
    }

    @Test
    void shouldAllowSameSkillIdDifferentLevels() {
        ClassSkillLink link1 = new ClassSkillLink("slash", 1);
        ClassSkillLink link2 = new ClassSkillLink("slash", 20);

        assertEquals(link1.getSkillId(), link2.getSkillId());
        assertNotEquals(link1.getUnlockLevel(), link2.getUnlockLevel());
    }
}

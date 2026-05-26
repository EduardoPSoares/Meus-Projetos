package me.ray.midgard.core.skill;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkillResultTest {

    @Test
    void testEnumValues() {
        // Garantir que os valores essenciais existem e não foram removidos acidentalmente
        assertNotNull(SkillResult.valueOf("SUCCESS"));
        assertNotNull(SkillResult.valueOf("FAILURE"));
        assertNotNull(SkillResult.valueOf("ON_COOLDOWN"));
        assertNotNull(SkillResult.valueOf("NO_MANA"));
    }
}

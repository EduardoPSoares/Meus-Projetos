package me.ray.midgard.modules.races.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class TraitTriggerTest {

    @Test
    @DisplayName("Deve ter 17 triggers disponíveis")
    void shouldHave16Triggers() {
        assertEquals(17, TraitTrigger.values().length);
    }

    @Test
    @DisplayName("Triggers de combate devem existir")
    void shouldHaveCombatTriggers() {
        assertNotNull(TraitTrigger.ON_ATTACK);
        assertNotNull(TraitTrigger.ON_DEFEND);
        assertNotNull(TraitTrigger.ON_KILL);
        assertNotNull(TraitTrigger.ON_DEATH);
        assertNotNull(TraitTrigger.ON_DAMAGE);
    }

    @Test
    @DisplayName("Triggers de ciclo de vida devem existir")
    void shouldHaveLifecycleTriggers() {
        assertNotNull(TraitTrigger.ON_SELECT);
        assertNotNull(TraitTrigger.ON_REMOVE);
        assertNotNull(TraitTrigger.ON_JOIN);
        assertNotNull(TraitTrigger.ON_QUIT);
    }

    @Test
    @DisplayName("PASSIVE_TICK deve existir")
    void shouldHavePassiveTick() {
        assertNotNull(TraitTrigger.PASSIVE_TICK);
    }

    @Test
    @DisplayName("valueOf deve funcionar para todos os valores")
    void shouldParseAllValues() {
        for (TraitTrigger trigger : TraitTrigger.values()) {
            assertEquals(trigger, TraitTrigger.valueOf(trigger.name()));
        }
    }
}

package me.ray.midgard.modules.combat.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XPCommandTest {

    @Test
    void shouldHaveCorrectName() {
        XPCommand cmd = new XPCommand();
        assertEquals("xp", cmd.getName());
    }

    @Test
    void shouldHaveCorrectPermission() {
        XPCommand cmd = new XPCommand();
        assertEquals("midgard.admin.xp", cmd.getPermission());
    }

    @Test
    void shouldNotBePlayerOnly() {
        XPCommand cmd = new XPCommand();
        assertFalse(cmd.isPlayerOnly());
    }

    @Test
    void shouldReturnDefaultUsage() {
        XPCommand cmd = new XPCommand();
        assertEquals("/rpg admin xp", cmd.getUsage());
    }
}

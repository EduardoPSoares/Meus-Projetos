package me.ray.midgard.modules.commands.admin;

import me.ray.midgard.modules.commands.CommandsModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CommandsAdminCommandTest {

    @Mock
    private CommandsModule module;

    private CommandsAdminCommand command;

    @BeforeEach
    void setUp() {
        command = new CommandsAdminCommand(module);
    }

    @Test
    void shouldHaveCorrectName() {
        assertEquals("commands", command.getName());
    }

    @Test
    void shouldHaveCorrectPermission() {
        assertEquals("midgard.admin.commands", command.getPermission());
    }

    @Test
    void shouldNotBePlayerOnly() {
        assertFalse(command.isPlayerOnly());
    }

    @Test
    void shouldHaveAliases() {
        List<String> aliases = command.getAliases();
        assertNotNull(aliases);
        assertEquals(2, aliases.size());
        assertTrue(aliases.contains("cmds"));
        assertTrue(aliases.contains("cmd"));
    }
}

package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EssentialsBaseCommandTest {

    @Mock
    private EssentialsManager manager;

    /**
     * Concrete subclass for testing the abstract base class.
     */
    private static class TestCommand extends EssentialsBaseCommand {
        TestCommand(EssentialsManager manager, String name, String permission, boolean playerOnly) {
            super(manager, name, permission, playerOnly);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            // no-op for test
        }
    }

    @Test
    void shouldReturnCorrectName() {
        TestCommand cmd = new TestCommand(manager, "warp", "midgard.warp", true);
        assertEquals("warp", cmd.getName());
    }

    @Test
    void shouldReturnCorrectPermission() {
        TestCommand cmd = new TestCommand(manager, "warp", "midgard.warp", true);
        assertEquals("midgard.warp", cmd.getPermission());
    }

    @Test
    void shouldReturnPlayerOnlyFlag() {
        TestCommand cmd = new TestCommand(manager, "warp", "midgard.warp", true);
        assertTrue(cmd.isPlayerOnly());

        TestCommand cmd2 = new TestCommand(manager, "setspawn", "midgard.setspawn", false);
        assertFalse(cmd2.isPlayerOnly());
    }

    @ParameterizedTest
    @ValueSource(strings = {"gamemode", "fly", "heal", "feed", "spawn", "setspawn",
            "warp", "setwarp", "delwarp", "home", "sethome", "delhome",
            "tpa", "tpaccept", "tpdeny"})
    void shouldLookupDescriptionFromManager(String commandName) {
        when(manager.getMessage(anyString())).thenReturn("test description");
        TestCommand cmd = new TestCommand(manager, commandName, "perm", true);
        String desc = cmd.getDescription();
        assertNotNull(desc);
        verify(manager).getMessage(startsWith("command.desc."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"gamemode", "fly", "heal", "feed", "spawn", "setspawn",
            "warp", "setwarp", "delwarp", "home", "sethome", "delhome",
            "tpa", "tpaccept", "tpdeny"})
    void shouldLookupUsageFromManager(String commandName) {
        when(manager.getMessage(anyString())).thenReturn("test usage");
        TestCommand cmd = new TestCommand(manager, commandName, "perm", true);
        String usage = cmd.getUsage();
        assertNotNull(usage);
        verify(manager).getMessage(startsWith("command.usage."));
    }

    @Test
    void shouldFallbackToDefaultDescriptionForUnknownCommand() {
        when(manager.getMessage("command.desc.default")).thenReturn("default desc");
        TestCommand cmd = new TestCommand(manager, "unknowncommand", "perm", true);
        String desc = cmd.getDescription();
        assertEquals("default desc", desc);
    }

    @Test
    void shouldFallbackToSlashNameForUnknownUsage() {
        TestCommand cmd = new TestCommand(manager, "unknowncommand", "perm", true);
        String usage = cmd.getUsage();
        assertEquals("/unknowncommand", usage);
    }

    @Test
    void shouldExposeManager() {
        TestCommand cmd = new TestCommand(manager, "warp", "perm", true);
        assertSame(manager, cmd.manager);
    }

    @Test
    void shouldReturnEmptyAliasesByDefault() {
        TestCommand cmd = new TestCommand(manager, "warp", "perm", true);
        assertEquals(Collections.emptyList(), cmd.getAliases());
    }
}

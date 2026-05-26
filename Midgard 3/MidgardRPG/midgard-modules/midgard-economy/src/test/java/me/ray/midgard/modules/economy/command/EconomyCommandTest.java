package me.ray.midgard.modules.economy.command;

import me.ray.midgard.modules.economy.EconomyModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EconomyCommandTest {

    @Mock
    private EconomyModule module;

    private EconomyCommand command;

    @BeforeEach
    void setUp() {
        when(module.getMessage(anyString())).thenReturn("mock msg");
        command = new EconomyCommand(module);
    }

    @Test
    void shouldHaveCorrectName() {
        assertEquals("mideco", command.getName());
    }

    @Test
    void shouldHaveCorrectPermission() {
        assertEquals("midgard.command.economy", command.getPermission());
    }

    @Test
    void shouldBePlayerOnly() {
        assertTrue(command.isPlayerOnly());
    }

    @Test
    void shouldHaveCorrectAliases() {
        assertEquals(List.of("coins"), command.getAliases());
    }

    // --- tabComplete ---

    @Test
    void shouldReturnBaseSubcommandsWithoutAdmin() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("midgard.admin")).thenReturn(false);

        List<String> result = command.tabComplete(sender, new String[]{""});
        assertTrue(result.contains("compact"));
        assertTrue(result.contains("decompact"));
        assertFalse(result.contains("give"));
    }

    @Test
    void shouldIncludeGiveForAdmin() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("midgard.admin")).thenReturn(true);

        List<String> result = command.tabComplete(sender, new String[]{""});
        assertTrue(result.contains("compact"));
        assertTrue(result.contains("decompact"));
        assertTrue(result.contains("give"));
    }

    @Test
    void shouldFilterByPartialInput() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("midgard.admin")).thenReturn(true);

        List<String> result = command.tabComplete(sender, new String[]{"c"});
        assertTrue(result.contains("compact"));
        assertFalse(result.contains("decompact"));
        assertFalse(result.contains("give"));
    }

    @Test
    void shouldReturnEmptyForSecondArg() {
        CommandSender sender = mock(CommandSender.class);
        List<String> result = command.tabComplete(sender, new String[]{"compact", ""});
        assertTrue(result.isEmpty());
    }
}

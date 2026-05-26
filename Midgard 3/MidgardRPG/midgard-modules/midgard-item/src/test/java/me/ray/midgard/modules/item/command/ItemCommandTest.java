package me.ray.midgard.modules.item.command;

import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.manager.ItemManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItemCommandTest {

    @Mock
    private ItemModule module;

    @Mock
    private ItemManager itemManager;

    private ItemCommand command;

    @BeforeEach
    void setUp() {
        when(module.getItemManager()).thenReturn(itemManager);
        command = new ItemCommand(module);
    }

    @Nested
    class Metadata {

        @Test
        void shouldHaveCorrectPermission() {
            assertEquals("midgard.admin.item", command.getPermission());
        }

        @Test
        void shouldRequirePlayer() {
            assertTrue(command.isPlayerOnly());
        }

        @Test
        void shouldHaveDescription() {
            assertNotNull(command.getDescription());
            assertFalse(command.getDescription().isEmpty());
        }

        @Test
        void shouldHaveUsage() {
            assertNotNull(command.getUsage());
            assertFalse(command.getUsage().isEmpty());
        }

        @Test
        void shouldHaveAliases() {
            List<String> aliases = command.getAliases();
            assertNotNull(aliases);
            assertTrue(aliases.contains("items"));
        }
    }

    @Nested
    class TabComplete {

        @Test
        void shouldReturnSubcommands_forFirstArg() {
            org.bukkit.command.CommandSender sender = mock(org.bukkit.command.CommandSender.class);
            List<String> completions = command.tabComplete(sender, new String[]{""});

            assertTrue(completions.contains("give"));
            assertTrue(completions.contains("reload"));
            assertTrue(completions.contains("refine"));
        }

        @Test
        void shouldFilterSubcommands_byPartialInput() {
            org.bukkit.command.CommandSender sender = mock(org.bukkit.command.CommandSender.class);
            List<String> completions = command.tabComplete(sender, new String[]{"g"});

            assertTrue(completions.contains("give"));
            assertFalse(completions.contains("reload"));
        }

        @Test
        void shouldReturnItemIds_forGiveSecondArg() {
            org.bukkit.command.CommandSender sender = mock(org.bukkit.command.CommandSender.class);
            when(itemManager.getItemIds()).thenReturn(List.of("iron_sword", "gold_ring"));

            List<String> completions = command.tabComplete(sender, new String[]{"give", ""});

            assertTrue(completions.contains("iron_sword"));
            assertTrue(completions.contains("gold_ring"));
        }

        @Test
        void shouldReturnAmounts_forGiveThirdArg() {
            org.bukkit.command.CommandSender sender = mock(org.bukkit.command.CommandSender.class);
            List<String> completions = command.tabComplete(sender, new String[]{"give", "iron_sword", ""});

            assertTrue(completions.contains("1"));
            assertTrue(completions.contains("64"));
        }

        @Test
        void shouldReturnEmptyList_forUnknownSubcommandArgs() {
            org.bukkit.command.CommandSender sender = mock(org.bukkit.command.CommandSender.class);
            List<String> completions = command.tabComplete(sender, new String[]{"reload", "extra"});

            assertTrue(completions.isEmpty());
        }
    }
}

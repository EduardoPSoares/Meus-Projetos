package me.ray.midgard.modules.commands.registry;

import me.ray.midgard.core.command.CommandCategory;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CentralCommandRegistryTest {

    @Mock
    private JavaPlugin plugin;

    private static Server mockServer;

    private CentralCommandRegistry registry;

    @BeforeAll
    static void initBukkit() throws Exception {
        mockServer = mock(Server.class);
        ConsoleCommandSender consoleSender = mock(ConsoleCommandSender.class);
        when(mockServer.getConsoleSender()).thenReturn(consoleSender);

        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, mockServer);
    }

    @BeforeEach
    void setUp() {
        when(plugin.getServer()).thenReturn(mockServer);
        registry = new CentralCommandRegistry(plugin);
    }

    // --- register ---

    @Test
    void shouldRegisterCommand() {
        CommandDescriptor desc = CommandDescriptor.builder("test")
                .module("combat")
                .build();

        registry.register(desc);
        assertTrue(registry.isRegistered("test"));
    }

    @Test
    void shouldRegisterCommandCaseInsensitive() {
        CommandDescriptor desc = CommandDescriptor.builder("TestCmd")
                .module("combat")
                .build();

        registry.register(desc);
        assertTrue(registry.isRegistered("testcmd"));
        assertTrue(registry.isRegistered("TESTCMD"));
    }

    @Test
    void shouldRegisterAliases() {
        CommandDescriptor desc = CommandDescriptor.builder("spell")
                .module("spells")
                .aliases("sp", "s")
                .build();

        registry.register(desc);
        assertTrue(registry.isRegistered("spell"));
        assertTrue(registry.isRegistered("sp"));
        assertTrue(registry.isRegistered("s"));
    }

    @Test
    void shouldIgnoreNullDescriptor() {
        registry.register(null);
        assertEquals(0, registry.getRegisteredCommandCount());
    }

    @Test
    void shouldIgnoreDescriptorWithNullName() {
        CommandDescriptor desc = CommandDescriptor.builder(null)
                .module("test")
                .build();
        // getName() returns null → register should early return
        registry.register(desc);
    }

    @Test
    void shouldOverwriteDuplicateCommand() {
        CommandDescriptor first = CommandDescriptor.builder("cmd")
                .module("module1")
                .description("first")
                .build();
        CommandDescriptor second = CommandDescriptor.builder("cmd")
                .module("module2")
                .description("second")
                .build();

        registry.register(first);
        registry.register(second);

        CommandDescriptor result = registry.get("cmd");
        assertEquals("second", result.getDescription());
    }

    // --- get ---

    @Test
    void shouldReturnDescriptorByName() {
        CommandDescriptor desc = CommandDescriptor.builder("attack")
                .module("combat")
                .description("Attack command")
                .build();

        registry.register(desc);
        CommandDescriptor result = registry.get("attack");

        assertNotNull(result);
        assertEquals("attack", result.getName());
        assertEquals("Attack command", result.getDescription());
    }

    @Test
    void shouldReturnNullForUnregisteredCommand() {
        assertNull(registry.get("nonexistent"));
    }

    @Test
    void shouldReturnNullForNullName() {
        assertNull(registry.get(null));
    }

    @Test
    void shouldGetByCaseInsensitive() {
        registry.register(CommandDescriptor.builder("MyCmd").module("mod").build());
        assertNotNull(registry.get("mycmd"));
        assertNotNull(registry.get("MYCMD"));
    }

    // --- isRegistered ---

    @Test
    void shouldReturnFalseWhenNotRegistered() {
        assertFalse(registry.isRegistered("nope"));
    }

    @Test
    void shouldReturnFalseForNullName() {
        assertFalse(registry.isRegistered(null));
    }

    // --- unregister ---

    @Test
    void shouldUnregisterCommand() {
        registry.register(CommandDescriptor.builder("cmd").module("mod").build());
        assertTrue(registry.isRegistered("cmd"));

        registry.unregister("cmd");
        assertFalse(registry.isRegistered("cmd"));
    }

    @Test
    void shouldUnregisterAliasesToo() {
        registry.register(CommandDescriptor.builder("spell")
                .module("spells")
                .aliases("sp", "s")
                .build());

        registry.unregister("spell");
        assertFalse(registry.isRegistered("spell"));
        assertFalse(registry.isRegistered("sp"));
        assertFalse(registry.isRegistered("s"));
    }

    @Test
    void shouldHandleUnregisterNull() {
        registry.unregister(null);
        // No exception
    }

    @Test
    void shouldHandleUnregisterNonExistent() {
        registry.unregister("doesnotexist");
        // No exception
    }

    // --- unregisterAll ---

    @Test
    void shouldClearEverything() {
        registry.register(CommandDescriptor.builder("a").module("m1").build());
        registry.register(CommandDescriptor.builder("b").module("m2").build());
        assertEquals(2, registry.getRegisteredCommandCount());

        registry.unregisterAll();
        assertEquals(0, registry.getRegisteredCommandCount());
        assertFalse(registry.isRegistered("a"));
        assertFalse(registry.isRegistered("b"));
    }

    // --- getAllCommands ---

    @Test
    void shouldReturnAllCommandsWithoutDuplicates() {
        registry.register(CommandDescriptor.builder("cmd1")
                .module("mod1")
                .aliases("c1")
                .build());
        registry.register(CommandDescriptor.builder("cmd2")
                .module("mod2")
                .build());

        Collection<CommandDescriptor> all = registry.getAllCommands();
        assertEquals(2, all.size());
    }

    @Test
    void shouldReturnEmptyWhenNoCommands() {
        assertTrue(registry.getAllCommands().isEmpty());
    }

    // --- getByCategory ---

    @Test
    void shouldFilterByCategory() {
        registry.register(CommandDescriptor.builder("admin1")
                .module("core")
                .category(CommandCategory.ADMIN)
                .build());
        registry.register(CommandDescriptor.builder("player1")
                .module("core")
                .category(CommandCategory.PLAYER)
                .build());
        registry.register(CommandDescriptor.builder("admin2")
                .module("core")
                .category(CommandCategory.ADMIN)
                .build());

        List<CommandDescriptor> admins = registry.getByCategory(CommandCategory.ADMIN);
        assertEquals(2, admins.size());

        List<CommandDescriptor> players = registry.getByCategory(CommandCategory.PLAYER);
        assertEquals(1, players.size());
    }

    @Test
    void shouldReturnEmptyForCategoryWithNoCommands() {
        registry.register(CommandDescriptor.builder("cmd")
                .module("mod")
                .category(CommandCategory.PLAYER)
                .build());

        List<CommandDescriptor> mods = registry.getByCategory(CommandCategory.MODERATOR);
        assertTrue(mods.isEmpty());
    }

    // --- getByModule ---

    @Test
    void shouldFilterByModule() {
        registry.register(CommandDescriptor.builder("a").module("combat").build());
        registry.register(CommandDescriptor.builder("b").module("combat").build());
        registry.register(CommandDescriptor.builder("c").module("spells").build());

        List<CommandDescriptor> combat = registry.getByModule("combat");
        assertEquals(2, combat.size());

        List<CommandDescriptor> spells = registry.getByModule("spells");
        assertEquals(1, spells.size());
    }

    @Test
    void shouldFilterByModuleCaseInsensitive() {
        registry.register(CommandDescriptor.builder("a").module("Combat").build());

        List<CommandDescriptor> result = registry.getByModule("combat");
        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnEmptyForUnknownModule() {
        List<CommandDescriptor> result = registry.getByModule("nonexistent");
        assertTrue(result.isEmpty());
    }

    // --- getRegisteredCommandCount ---

    @Test
    void shouldCountRegisteredCommands() {
        assertEquals(0, registry.getRegisteredCommandCount());

        registry.register(CommandDescriptor.builder("a").module("m").build());
        assertEquals(1, registry.getRegisteredCommandCount());

        registry.register(CommandDescriptor.builder("b").module("m").build());
        assertEquals(2, registry.getRegisteredCommandCount());
    }

    @Test
    void shouldNotCountAliasesAsSeparateCommands() {
        registry.register(CommandDescriptor.builder("cmd")
                .module("mod")
                .aliases("c", "cm")
                .build());

        // aliases map to the same descriptor → getAllCommands deduplicates
        assertEquals(1, registry.getRegisteredCommandCount());
    }

    // --- getModulesWithCommands ---

    @Test
    void shouldReturnModulesWithCommands() {
        registry.register(CommandDescriptor.builder("a").module("combat").build());
        registry.register(CommandDescriptor.builder("b").module("spells").build());
        registry.register(CommandDescriptor.builder("c").module("combat").build());

        Set<String> modules = registry.getModulesWithCommands();
        assertEquals(2, modules.size());
        assertTrue(modules.contains("combat"));
        assertTrue(modules.contains("spells"));
    }

    @Test
    void shouldReturnEmptyModules_whenNoCommands() {
        Set<String> modules = registry.getModulesWithCommands();
        assertTrue(modules.isEmpty());
    }

    // --- Integration scenarios ---

    @Nested
    class IntegrationScenarios {

        @Test
        void shouldHandleRegisterUnregisterReRegister() {
            CommandDescriptor desc = CommandDescriptor.builder("cycle")
                    .module("test")
                    .build();

            registry.register(desc);
            assertTrue(registry.isRegistered("cycle"));

            registry.unregister("cycle");
            assertFalse(registry.isRegistered("cycle"));

            registry.register(desc);
            assertTrue(registry.isRegistered("cycle"));
        }

        @Test
        void shouldHandleMultipleModulesAndCategories() {
            registry.register(CommandDescriptor.builder("atk")
                    .module("combat").category(CommandCategory.PLAYER).build());
            registry.register(CommandDescriptor.builder("heal")
                    .module("combat").category(CommandCategory.PLAYER).build());
            registry.register(CommandDescriptor.builder("reload")
                    .module("core").category(CommandCategory.ADMIN).build());
            registry.register(CommandDescriptor.builder("vanish")
                    .module("essentials").category(CommandCategory.MODERATOR).build());

            assertEquals(4, registry.getRegisteredCommandCount());
            assertEquals(2, registry.getByModule("combat").size());
            assertEquals(1, registry.getByCategory(CommandCategory.ADMIN).size());
            assertEquals(1, registry.getByCategory(CommandCategory.MODERATOR).size());
            assertEquals(2, registry.getByCategory(CommandCategory.PLAYER).size());
            assertEquals(3, registry.getModulesWithCommands().size());
        }
    }
}

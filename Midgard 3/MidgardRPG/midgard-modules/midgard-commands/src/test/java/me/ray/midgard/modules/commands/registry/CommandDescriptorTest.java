package me.ray.midgard.modules.commands.registry;

import me.ray.midgard.core.command.CommandCategory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandDescriptorTest {

    // --- Builder Defaults ---

    @Test
    void shouldBuildWithNameOnly() {
        CommandDescriptor desc = CommandDescriptor.builder("test").build();

        assertEquals("test", desc.getName());
        assertEquals("", desc.getDescription());
        assertEquals("", desc.getUsage());
        assertNull(desc.getPermission());
        assertTrue(desc.getAliases().isEmpty());
        assertEquals(CommandCategory.PLAYER, desc.getCategory());
        assertEquals("unknown", desc.getModule());
        assertFalse(desc.isPlayerOnly());
        assertTrue(desc.isEnabled());
        assertEquals(CommandDescriptor.CommandSource.RPG_UNIFIED, desc.getSource());
    }

    // --- Builder Full ---

    @Test
    void shouldBuildWithAllFields() {
        CommandDescriptor desc = CommandDescriptor.builder("mycommand")
                .description("A test command")
                .usage("/rpg mycommand <arg>")
                .permission("midgard.test")
                .aliases("mc", "mycmd")
                .category(CommandCategory.ADMIN)
                .module("testmodule")
                .playerOnly(true)
                .enabled(false)
                .source(CommandDescriptor.CommandSource.RPG_ADMIN)
                .build();

        assertEquals("mycommand", desc.getName());
        assertEquals("A test command", desc.getDescription());
        assertEquals("/rpg mycommand <arg>", desc.getUsage());
        assertEquals("midgard.test", desc.getPermission());
        assertEquals(List.of("mc", "mycmd"), desc.getAliases());
        assertEquals(CommandCategory.ADMIN, desc.getCategory());
        assertEquals("testmodule", desc.getModule());
        assertTrue(desc.isPlayerOnly());
        assertFalse(desc.isEnabled());
        assertEquals(CommandDescriptor.CommandSource.RPG_ADMIN, desc.getSource());
    }

    // --- Aliases ---

    @Test
    void shouldAcceptAliasesAsList() {
        CommandDescriptor desc = CommandDescriptor.builder("cmd")
                .aliases(List.of("a", "b", "c"))
                .build();

        assertEquals(3, desc.getAliases().size());
        assertEquals(List.of("a", "b", "c"), desc.getAliases());
    }

    @Test
    void shouldAcceptAliasesAsVarargs() {
        CommandDescriptor desc = CommandDescriptor.builder("cmd")
                .aliases("x", "y")
                .build();

        assertEquals(List.of("x", "y"), desc.getAliases());
    }

    @Test
    void shouldReturnImmutableAliases() {
        CommandDescriptor desc = CommandDescriptor.builder("cmd")
                .aliases("a", "b")
                .build();

        assertThrows(UnsupportedOperationException.class, () -> desc.getAliases().add("c"));
    }

    @Test
    void shouldReturnEmptyAliases_whenNoneProvided() {
        CommandDescriptor desc = CommandDescriptor.builder("cmd").build();
        assertNotNull(desc.getAliases());
        assertTrue(desc.getAliases().isEmpty());
    }

    // --- Categories ---

    @Test
    void shouldDefaultToPlayerCategory() {
        CommandDescriptor desc = CommandDescriptor.builder("cmd").build();
        assertEquals(CommandCategory.PLAYER, desc.getCategory());
    }

    @Test
    void shouldSetAdminCategory() {
        CommandDescriptor desc = CommandDescriptor.builder("cmd")
                .category(CommandCategory.ADMIN)
                .build();
        assertEquals(CommandCategory.ADMIN, desc.getCategory());
    }

    @Test
    void shouldSetModeratorCategory() {
        CommandDescriptor desc = CommandDescriptor.builder("cmd")
                .category(CommandCategory.MODERATOR)
                .build();
        assertEquals(CommandCategory.MODERATOR, desc.getCategory());
    }

    // --- Command Sources ---

    @Nested
    class CommandSourceTest {

        @Test
        void shouldHaveAllExpectedValues() {
            CommandDescriptor.CommandSource[] values = CommandDescriptor.CommandSource.values();
            assertEquals(4, values.length);
        }

        @Test
        void shouldContainRpgUnified() {
            assertNotNull(CommandDescriptor.CommandSource.valueOf("RPG_UNIFIED"));
        }

        @Test
        void shouldContainRpgAdmin() {
            assertNotNull(CommandDescriptor.CommandSource.valueOf("RPG_ADMIN"));
        }

        @Test
        void shouldContainStandalone() {
            assertNotNull(CommandDescriptor.CommandSource.valueOf("STANDALONE"));
        }

        @Test
        void shouldContainExternal() {
            assertNotNull(CommandDescriptor.CommandSource.valueOf("EXTERNAL"));
        }
    }

    // --- toString ---

    @Test
    void shouldIncludeNameInToString() {
        CommandDescriptor desc = CommandDescriptor.builder("testcmd")
                .module("mymod")
                .category(CommandCategory.ADMIN)
                .build();

        String str = desc.toString();
        assertTrue(str.contains("testcmd"));
        assertTrue(str.contains("mymod"));
        assertTrue(str.contains("ADMIN"));
    }

    // --- Edge Cases ---

    @Test
    void shouldHandleNullPermission() {
        CommandDescriptor desc = CommandDescriptor.builder("cmd")
                .permission(null)
                .build();
        assertNull(desc.getPermission());
    }

    @Test
    void shouldHandleEmptyDescription() {
        CommandDescriptor desc = CommandDescriptor.builder("cmd")
                .description("")
                .build();
        assertEquals("", desc.getDescription());
    }

    @Test
    void shouldPreserveBuilderOrder() {
        // Overwrites should use the last value
        CommandDescriptor desc = CommandDescriptor.builder("cmd")
                .module("first")
                .module("second")
                .build();
        assertEquals("second", desc.getModule());
    }
}

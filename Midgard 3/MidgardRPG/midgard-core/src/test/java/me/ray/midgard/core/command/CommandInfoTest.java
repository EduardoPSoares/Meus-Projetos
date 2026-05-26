package me.ray.midgard.core.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandInfoTest {

    @Test
    void shouldBuildWithDefaults() {
        CommandInfo info = CommandInfo.builder("test").build();

        assertEquals("test", info.getName());
        assertEquals("", info.getDescription());
        assertEquals("", info.getUsage());
        assertNull(info.getPermission());
        assertTrue(info.getAliases().isEmpty());
        assertEquals(CommandCategory.PLAYER, info.getCategory());
        assertEquals("core", info.getModule());
    }

    @Test
    void shouldBuildWithAllFields() {
        CommandInfo info = CommandInfo.builder("teleport")
                .description("Teleporta o jogador")
                .usage("/tp <player>")
                .permission("midgard.tp")
                .aliases("tp", "tele")
                .category(CommandCategory.ADMIN)
                .module("essentials")
                .build();

        assertEquals("teleport", info.getName());
        assertEquals("Teleporta o jogador", info.getDescription());
        assertEquals("/tp <player>", info.getUsage());
        assertEquals("midgard.tp", info.getPermission());
        assertEquals(List.of("tp", "tele"), info.getAliases());
        assertEquals(CommandCategory.ADMIN, info.getCategory());
        assertEquals("essentials", info.getModule());
    }

    @Test
    void shouldAcceptAliasesAsList() {
        List<String> aliases = List.of("h", "ajuda");
        CommandInfo info = CommandInfo.builder("help")
                .aliases(aliases)
                .build();

        assertEquals(aliases, info.getAliases());
    }

    @Test
    void shouldHandleNullAliasesAsList() {
        CommandInfo info = CommandInfo.builder("test")
                .aliases((List<String>) null)
                .build();
        assertTrue(info.getAliases().isEmpty());
    }
}

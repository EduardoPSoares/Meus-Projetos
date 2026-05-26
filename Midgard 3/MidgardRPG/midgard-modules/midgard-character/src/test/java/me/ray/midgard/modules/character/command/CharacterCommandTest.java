package me.ray.midgard.modules.character.command;

import me.ray.midgard.modules.character.i18n.CharacterMessages;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CharacterCommandTest {

    private CharacterCommand command;

    @BeforeEach
    void setUp() {
        command = new CharacterCommand();
    }

    // ============================================
    // CONSTRUCTOR / METADATA
    // ============================================

    @Test
    void shouldHaveCorrectName() {
        assertEquals("character", command.getName());
    }

    @Test
    void shouldHaveNullPermission() {
        assertNull(command.getPermission());
    }

    @Test
    void shouldBePlayerOnly() {
        assertTrue(command.isPlayerOnly());
    }

    // ============================================
    // ALIASES
    // ============================================

    @Test
    void shouldReturnCorrectAliases() {
        List<String> aliases = command.getAliases();
        assertNotNull(aliases);
        assertEquals(3, aliases.size());
    }

    @Test
    void shouldContainCharAlias() {
        assertTrue(command.getAliases().contains("char"));
    }

    @Test
    void shouldContainStatsAlias() {
        assertTrue(command.getAliases().contains("stats"));
    }

    @Test
    void shouldContainAttributesAlias() {
        assertTrue(command.getAliases().contains("attributes"));
    }

    @Test
    void aliasesShouldBeInCorrectOrder() {
        List<String> aliases = command.getAliases();
        assertEquals("char", aliases.get(0));
        assertEquals("stats", aliases.get(1));
        assertEquals("attributes", aliases.get(2));
    }

    // ============================================
    // DESCRIPTION & USAGE (Fallback quando módulo é null)
    // ============================================

    @Test
    void descriptionShouldReturnNullWhenModuleIsNull() {
        // CharacterModule.getInstance() retorna null pois não foi inicializado
        // getDefaultValue() retorna null porque o builder usa fallback(), não defaultValue()
        String description = command.getDescription();
        assertNull(description, "Description deveria ser null quando módulo é null e getDefaultValue() retorna null");
    }

    @Test
    void usageShouldReturnNullWhenModuleIsNull() {
        String usage = command.getUsage();
        assertNull(usage, "Usage deveria ser null quando módulo é null e getDefaultValue() retorna null");
    }

    // ============================================
    // TAB COMPLETE
    // ============================================

    @Test
    void tabCompleteShouldReturnEmptyList() {
        CommandSender sender = mock(CommandSender.class);
        List<String> result = command.tabComplete(sender, new String[]{});
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void tabCompleteShouldReturnEmptyListWithArgs() {
        CommandSender sender = mock(CommandSender.class);
        List<String> result = command.tabComplete(sender, new String[]{"test", "arg"});
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void tabCompleteShouldReturnEmptyListWithNullSender() {
        // tabComplete não valida sender, apenas retorna lista vazia
        List<String> result = command.tabComplete(null, new String[]{});
        assertTrue(result.isEmpty());
    }
}

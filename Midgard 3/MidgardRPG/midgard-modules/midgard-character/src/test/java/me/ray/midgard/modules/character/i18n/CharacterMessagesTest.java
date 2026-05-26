package me.ray.midgard.modules.character.i18n;

import me.ray.midgard.core.i18n.MessageKey;
import me.ray.midgard.core.i18n.MessageRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CharacterMessagesTest {

    // ============================================
    // COMMAND KEYS
    // ============================================

    @Test
    void commandDescriptionShouldHaveCorrectKey() {
        assertEquals("character.command.description", CharacterMessages.COMMAND_DESCRIPTION.getKey());
    }

    @Test
    void commandDescriptionShouldHaveCorrectModule() {
        assertEquals("character", CharacterMessages.COMMAND_DESCRIPTION.getModule());
    }

    @Test
    void commandDescriptionShouldHaveFallback() {
        assertTrue(CharacterMessages.COMMAND_DESCRIPTION.hasFallback());
        assertEquals("Abre o menu de atributos do personagem", CharacterMessages.COMMAND_DESCRIPTION.getFallbackKey());
    }

    @Test
    void commandUsageShouldHaveCorrectKey() {
        assertEquals("character.command.usage", CharacterMessages.COMMAND_USAGE.getKey());
    }

    @Test
    void commandUsageShouldHaveCorrectModule() {
        assertEquals("character", CharacterMessages.COMMAND_USAGE.getModule());
    }

    @Test
    void commandUsageShouldHaveFallback() {
        assertTrue(CharacterMessages.COMMAND_USAGE.hasFallback());
        assertEquals("/rpg character", CharacterMessages.COMMAND_USAGE.getFallbackKey());
    }

    // ============================================
    // ERROR KEYS
    // ============================================

    @Test
    void errorPlayerOnlyShouldHaveCorrectKey() {
        assertEquals("character.errors.player_only", CharacterMessages.ERROR_PLAYER_ONLY.getKey());
    }

    @Test
    void errorPlayerOnlyShouldHaveFallbackWithMiniMessage() {
        assertTrue(CharacterMessages.ERROR_PLAYER_ONLY.hasFallback());
        assertNotNull(CharacterMessages.ERROR_PLAYER_ONLY.getFallbackKey());
        assertTrue(CharacterMessages.ERROR_PLAYER_ONLY.getFallbackKey().contains("<red>"));
    }

    @Test
    void errorModuleUnavailableShouldHaveCorrectKey() {
        assertEquals("character.errors.module_unavailable", CharacterMessages.ERROR_MODULE_UNAVAILABLE.getKey());
    }

    @Test
    void errorModuleUnavailableShouldHaveModule() {
        assertEquals("character", CharacterMessages.ERROR_MODULE_UNAVAILABLE.getModule());
    }

    @Test
    void errorModuleUnavailableShouldHaveFallback() {
        assertTrue(CharacterMessages.ERROR_MODULE_UNAVAILABLE.hasFallback());
    }

    @Test
    void errorMenuOpenFailedShouldHaveCorrectKey() {
        assertEquals("character.errors.menu_open_failed", CharacterMessages.ERROR_MENU_OPEN_FAILED.getKey());
    }

    @Test
    void errorMenuOpenFailedShouldHaveFallback() {
        assertTrue(CharacterMessages.ERROR_MENU_OPEN_FAILED.hasFallback());
    }

    // ============================================
    // MENU KEYS
    // ============================================

    @Test
    void menuTitleShouldHaveCorrectKey() {
        assertEquals("character.menu.title", CharacterMessages.MENU_TITLE.getKey());
    }

    @Test
    void menuTitleShouldHaveFallback() {
        assertTrue(CharacterMessages.MENU_TITLE.hasFallback());
        assertEquals("Informações do Personagem", CharacterMessages.MENU_TITLE.getFallbackKey());
    }

    @Test
    void menuNoClassShouldHaveCorrectKey() {
        assertEquals("character.menu.no_class", CharacterMessages.MENU_NO_CLASS.getKey());
    }

    @Test
    void menuNoClassShouldHaveFallback() {
        assertTrue(CharacterMessages.MENU_NO_CLASS.hasFallback());
        assertEquals("Nenhuma", CharacterMessages.MENU_NO_CLASS.getFallbackKey());
    }

    @Test
    void menuNotEnoughPointsShouldHaveCorrectKey() {
        assertEquals("character.menu.not_enough_points", CharacterMessages.MENU_NOT_ENOUGH_POINTS.getKey());
    }

    @Test
    void menuNotEnoughPointsShouldHaveFallback() {
        assertTrue(CharacterMessages.MENU_NOT_ENOUGH_POINTS.hasFallback());
    }

    @Test
    void menuErrorShouldHaveCorrectKey() {
        assertEquals("character.menu.menu_error", CharacterMessages.MENU_ERROR.getKey());
    }

    @Test
    void menuErrorShouldHaveModule() {
        assertEquals("character", CharacterMessages.MENU_ERROR.getModule());
    }

    @Test
    void menuXpMaxLevelShouldHaveCorrectKey() {
        assertEquals("character.menu.xp_max_level", CharacterMessages.MENU_XP_MAX_LEVEL.getKey());
    }

    @Test
    void menuXpMaxLevelShouldHaveFallback() {
        assertTrue(CharacterMessages.MENU_XP_MAX_LEVEL.hasFallback());
        assertEquals("100%", CharacterMessages.MENU_XP_MAX_LEVEL.getFallbackKey());
    }

    // ============================================
    // HOTBAR KEYS
    // ============================================

    @Test
    void hotbarCompassNameShouldHaveCorrectKey() {
        assertEquals("character.hotbar.compass.name", CharacterMessages.HOTBAR_COMPASS_NAME.getKey());
    }

    @Test
    void hotbarCompassNameShouldHaveModule() {
        assertEquals("character", CharacterMessages.HOTBAR_COMPASS_NAME.getModule());
    }

    @Test
    void hotbarCompassNameShouldHaveFallback() {
        assertTrue(CharacterMessages.HOTBAR_COMPASS_NAME.hasFallback());
        assertTrue(CharacterMessages.HOTBAR_COMPASS_NAME.getFallbackKey().contains("ᴘᴇʀsᴏɴᴀɢᴇᴍ"));
    }

    @Test
    void hotbarCompassReceivedShouldHaveCorrectKey() {
        assertEquals("character.hotbar.compass_received", CharacterMessages.HOTBAR_COMPASS_RECEIVED.getKey());
    }

    @Test
    void hotbarCompassReceivedShouldHaveFallback() {
        assertTrue(CharacterMessages.HOTBAR_COMPASS_RECEIVED.hasFallback());
        assertTrue(CharacterMessages.HOTBAR_COMPASS_RECEIVED.getFallbackKey().contains("<green>"));
    }

    // ============================================
    // REGISTRATION
    // ============================================

    @Test
    void allKeysShouldBeRegisteredInRegistry() {
        CharacterMessages.init();

        assertTrue(MessageRegistry.getInstance().isRegistered("character.command.description"));
        assertTrue(MessageRegistry.getInstance().isRegistered("character.command.usage"));
        assertTrue(MessageRegistry.getInstance().isRegistered("character.errors.player_only"));
        assertTrue(MessageRegistry.getInstance().isRegistered("character.errors.module_unavailable"));
        assertTrue(MessageRegistry.getInstance().isRegistered("character.errors.menu_open_failed"));
        assertTrue(MessageRegistry.getInstance().isRegistered("character.menu.title"));
        assertTrue(MessageRegistry.getInstance().isRegistered("character.menu.no_class"));
        assertTrue(MessageRegistry.getInstance().isRegistered("character.menu.not_enough_points"));
        assertTrue(MessageRegistry.getInstance().isRegistered("character.menu.menu_error"));
        assertTrue(MessageRegistry.getInstance().isRegistered("character.menu.xp_max_level"));
        assertTrue(MessageRegistry.getInstance().isRegistered("character.hotbar.compass.name"));
        assertTrue(MessageRegistry.getInstance().isRegistered("character.hotbar.compass_received"));
    }

    @Test
    void allKeysShouldBelongToCharacterModule() {
        CharacterMessages.init();

        Set<MessageKey> keys = MessageRegistry.getInstance().getKeysByModule("character");
        assertNotNull(keys);
        assertFalse(keys.isEmpty());
        for (MessageKey key : keys) {
            assertEquals("character", key.getModule());
        }
    }

    @Test
    void allKeysShouldHaveNonNullFallbacks() {
        MessageKey[] allKeys = {
                CharacterMessages.COMMAND_DESCRIPTION,
                CharacterMessages.COMMAND_USAGE,
                CharacterMessages.ERROR_PLAYER_ONLY,
                CharacterMessages.ERROR_MODULE_UNAVAILABLE,
                CharacterMessages.ERROR_MENU_OPEN_FAILED,
                CharacterMessages.MENU_TITLE,
                CharacterMessages.MENU_NO_CLASS,
                CharacterMessages.MENU_NOT_ENOUGH_POINTS,
                CharacterMessages.MENU_ERROR,
                CharacterMessages.MENU_XP_MAX_LEVEL,
                CharacterMessages.HOTBAR_COMPASS_NAME,
                CharacterMessages.HOTBAR_COMPASS_RECEIVED
        };

        for (MessageKey key : allKeys) {
            assertNotNull(key, "MessageKey não deveria ser null");
            assertNotNull(key.getKey(), "Key string não deveria ser null para " + key);
            assertNotNull(key.getModule(), "Module não deveria ser null para " + key.getKey());
            assertTrue(key.hasFallback(), "Deve ter fallback: " + key.getKey());
            assertNotNull(key.getFallbackKey(), "FallbackKey não deveria ser null para " + key.getKey());
        }
    }

    @Test
    void allKeysShouldStartWithCharacterPrefix() {
        MessageKey[] allKeys = {
                CharacterMessages.COMMAND_DESCRIPTION,
                CharacterMessages.COMMAND_USAGE,
                CharacterMessages.ERROR_PLAYER_ONLY,
                CharacterMessages.ERROR_MODULE_UNAVAILABLE,
                CharacterMessages.ERROR_MENU_OPEN_FAILED,
                CharacterMessages.MENU_TITLE,
                CharacterMessages.MENU_NO_CLASS,
                CharacterMessages.MENU_NOT_ENOUGH_POINTS,
                CharacterMessages.MENU_ERROR,
                CharacterMessages.MENU_XP_MAX_LEVEL,
                CharacterMessages.HOTBAR_COMPASS_NAME,
                CharacterMessages.HOTBAR_COMPASS_RECEIVED
        };

        for (MessageKey key : allKeys) {
            assertTrue(key.getKey().startsWith("character."),
                    "Key deveria começar com 'character.': " + key.getKey());
        }
    }

    @Test
    void initShouldNotThrowException() {
        assertDoesNotThrow(() -> CharacterMessages.init());
    }

    @Test
    void totalCharacterKeysShouldBeAtLeastTwelve() {
        CharacterMessages.init();
        Set<MessageKey> keys = MessageRegistry.getInstance().getKeysByModule("character");
        assertNotNull(keys);
        assertTrue(keys.size() >= 12, "Devem existir pelo menos 12 chaves no módulo character, encontradas: " + keys.size());
    }
}

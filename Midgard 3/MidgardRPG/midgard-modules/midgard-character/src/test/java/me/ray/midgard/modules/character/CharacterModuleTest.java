package me.ray.midgard.modules.character;

import me.ray.midgard.core.ModulePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CharacterModuleTest {

    @BeforeEach
    void setUp() throws Exception {
        // Garantir que o singleton está limpo
        resetInstance();
    }

    @AfterEach
    void tearDown() throws Exception {
        resetInstance();
    }

    private void resetInstance() throws Exception {
        Field instanceField = CharacterModule.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    // ============================================
    // CONSTRUCTOR
    // ============================================

    @Test
    void shouldHaveCorrectModuleName() {
        CharacterModule module = new CharacterModule();
        assertEquals("MidgardCharacter", module.getName());
    }

    @Test
    void shouldHaveNormalPriority() {
        CharacterModule module = new CharacterModule();
        assertEquals(ModulePriority.NORMAL, module.getPriority());
    }

    // ============================================
    // SINGLETON
    // ============================================

    @Test
    void getInstanceShouldReturnNullByDefault() {
        assertNull(CharacterModule.getInstance());
    }

    @Test
    void getInstanceShouldReturnSetInstance() throws Exception {
        CharacterModule module = new CharacterModule();
        Field instanceField = CharacterModule.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, module);

        assertSame(module, CharacterModule.getInstance());
    }

    // ============================================
    // getMessage
    // ============================================

    @Test
    void getMessageShouldReturnPathWhenMessagesConfigIsNull() {
        CharacterModule module = new CharacterModule();
        // messagesConfig é null por padrão (sem loadMessages)
        String result = module.getMessage("some.path");
        assertEquals("some.path", result);
    }

    @Test
    void getMessageShouldReturnPathForMissingKey() {
        CharacterModule module = new CharacterModule();
        String result = module.getMessage("nonexistent.key");
        assertEquals("nonexistent.key", result);
    }

    // ============================================
    // getMessageList
    // ============================================

    @Test
    void getMessageListShouldReturnEmptyListWhenConfigIsNull() {
        CharacterModule module = new CharacterModule();
        List<String> result = module.getMessageList("some.path");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ============================================
    // getCharacterConfig
    // ============================================

    @Test
    void characterConfigShouldBeNullInitially() throws Exception {
        CharacterModule module = new CharacterModule();
        Field configField = CharacterModule.class.getDeclaredField("characterConfig");
        configField.setAccessible(true);
        assertNull(configField.get(module));
    }

    // ============================================
    // getCompassKey
    // ============================================

    @Test
    void compassKeyShouldBeNullBeforeEnable() {
        CharacterModule module = new CharacterModule();
        assertNull(module.getCompassKey());
    }

    // ============================================
    // onDisable
    // ============================================

    @Test
    void onDisableShouldClearInstance() throws Exception {
        CharacterModule module = new CharacterModule();
        Field instanceField = CharacterModule.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, module);

        assertNotNull(CharacterModule.getInstance());

        module.onDisable();

        assertNull(CharacterModule.getInstance());
    }
}

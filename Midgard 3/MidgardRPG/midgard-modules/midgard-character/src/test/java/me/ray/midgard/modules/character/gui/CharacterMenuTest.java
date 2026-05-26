package me.ray.midgard.modules.character.gui;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CharacterMenuTest {

    // ============================================
    // applyPlaceholders — via reflection (método privado)
    // ============================================

    private String invokeApplyPlaceholders(Object instance, String text, Map<String, String> placeholders) throws Exception {
        Method method = CharacterMenu.class.getDeclaredMethod("applyPlaceholders", String.class, Map.class);
        method.setAccessible(true);
        return (String) method.invoke(instance, text, placeholders);
    }

    /**
     * Não podemos instanciar CharacterMenu normalmente (requer Player + CharacterModule),
     * então usamos sun.misc.Unsafe ou Mockito para criar instância parcial.
     * Porém, como applyPlaceholders é de instância, criamos via Mockito.
     */
    private CharacterMenu createUnsafeInstance() throws Exception {
        // Usar Objenesis (embutido no Mockito) para criar sem construtor
        return org.mockito.Mockito.mock(CharacterMenu.class, org.mockito.Mockito.CALLS_REAL_METHODS);
    }

    @Test
    void applyPlaceholdersShouldReplaceSimplePlaceholder() throws Exception {
        CharacterMenu instance = createUnsafeInstance();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%name%", "TestPlayer");

        String result = invokeApplyPlaceholders(instance, "Olá, %name%!", placeholders);
        assertEquals("Olá, TestPlayer!", result);
    }

    @Test
    void applyPlaceholdersShouldReplaceMultiplePlaceholders() throws Exception {
        CharacterMenu instance = createUnsafeInstance();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%name%", "João");
        placeholders.put("%level%", "10");
        placeholders.put("%class%", "Guerreiro");

        String result = invokeApplyPlaceholders(instance, "%name% - Nível %level% (%class%)", placeholders);
        assertEquals("João - Nível 10 (Guerreiro)", result);
    }

    @Test
    void applyPlaceholdersShouldReturnNullForNullInput() throws Exception {
        CharacterMenu instance = createUnsafeInstance();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%test%", "value");

        String result = invokeApplyPlaceholders(instance, null, placeholders);
        assertNull(result);
    }

    @Test
    void applyPlaceholdersShouldReturnOriginalWhenNoMatch() throws Exception {
        CharacterMenu instance = createUnsafeInstance();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%other%", "value");

        String result = invokeApplyPlaceholders(instance, "Texto sem placeholders", placeholders);
        assertEquals("Texto sem placeholders", result);
    }

    @Test
    void applyPlaceholdersShouldHandleEmptyPlaceholders() throws Exception {
        CharacterMenu instance = createUnsafeInstance();
        Map<String, String> placeholders = new HashMap<>();

        String result = invokeApplyPlaceholders(instance, "Texto original", placeholders);
        assertEquals("Texto original", result);
    }

    @Test
    void applyPlaceholdersShouldHandleEmptyStringValues() throws Exception {
        CharacterMenu instance = createUnsafeInstance();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%name%", "");

        String result = invokeApplyPlaceholders(instance, "Olá, %name%!", placeholders);
        assertEquals("Olá, !", result);
    }

    @Test
    void applyPlaceholdersShouldReplaceAllOccurrences() throws Exception {
        CharacterMenu instance = createUnsafeInstance();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%val%", "X");

        String result = invokeApplyPlaceholders(instance, "%val% - %val% - %val%", placeholders);
        assertEquals("X - X - X", result);
    }

    // ============================================
    // getSpentPoints — via reflection
    // ============================================

    private int invokeGetSpentPoints(Object instance, Object data, String attr) throws Exception {
        Method method = CharacterMenu.class.getDeclaredMethod("getSpentPoints",
                me.ray.midgard.modules.classes.ClassData.class, String.class);
        method.setAccessible(true);
        return (int) method.invoke(instance, data, attr);
    }

    @Test
    void getSpentPointsShouldReturnZeroForNullData() throws Exception {
        CharacterMenu instance = createUnsafeInstance();
        int result = invokeGetSpentPoints(instance, null, "strength");
        assertEquals(0, result);
    }

    @Test
    void getSpentPointsShouldDelegateToClassData() throws Exception {
        CharacterMenu instance = createUnsafeInstance();
        me.ray.midgard.modules.classes.ClassData data = new me.ray.midgard.modules.classes.ClassData();
        data.addSpentPoints("strength", 5);

        int result = invokeGetSpentPoints(instance, data, "strength");
        assertEquals(5, result);
    }

    @Test
    void getSpentPointsShouldReturnZeroForUnknownAttribute() throws Exception {
        CharacterMenu instance = createUnsafeInstance();
        me.ray.midgard.modules.classes.ClassData data = new me.ray.midgard.modules.classes.ClassData();

        int result = invokeGetSpentPoints(instance, data, "nonexistent");
        assertEquals(0, result);
    }
}

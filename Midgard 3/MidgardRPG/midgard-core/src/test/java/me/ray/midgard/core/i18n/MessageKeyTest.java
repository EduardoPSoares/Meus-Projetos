package me.ray.midgard.core.i18n;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MessageKeyTest {

    @Test
    void shouldCreateSimpleKey() {
        MessageKey key = MessageKey.of("combat.mode.enabled");
        assertEquals("combat.mode.enabled", key.getKey());
    }

    @Test
    void shouldCreateKeyWithModule() {
        MessageKey key = MessageKey.of("combat", "mode.enabled");
        assertEquals("combat.mode.enabled", key.getKey());
        assertEquals("combat", key.getModule());
    }

    @Test
    void shouldInferModuleFromKey() {
        MessageKey key = MessageKey.of("combat.mode.enabled");
        assertEquals("combat", key.inferModule());
    }

    @Test
    void shouldReturnNullModuleForKeyWithoutDot() {
        MessageKey key = MessageKey.of("simple");
        assertNull(key.inferModule());
    }

    @Test
    void shouldReturnSetModuleOverInferred() {
        MessageKey key = MessageKey.builder("combat.mode.enabled")
                .module("overridden")
                .build();
        assertEquals("overridden", key.inferModule());
    }

    @Test
    void shouldCreateWithPlaceholders() {
        MessageKey key = MessageKey.builder("msg.test")
                .placeholders("player", "amount")
                .build();

        Set<String> expected = key.getExpectedPlaceholders();
        assertEquals(2, expected.size());
        assertTrue(expected.contains("player"));
        assertTrue(expected.contains("amount"));
    }

    @Test
    void shouldReturnImmutablePlaceholders() {
        MessageKey key = MessageKey.builder("msg.test")
                .placeholders("player")
                .build();

        assertThrows(UnsupportedOperationException.class, ()
                -> key.getExpectedPlaceholders().add("another"));
    }

    @Test
    void shouldCreateWithFallback() {
        MessageKey key = MessageKey.builder("msg.specific")
                .fallback("msg.generic")
                .build();

        assertTrue(key.hasFallback());
        assertEquals("msg.generic", key.getFallbackKey());
    }

    @Test
    void shouldReportNoFallbackWhenNotSet() {
        MessageKey key = MessageKey.of("msg.test");
        assertFalse(key.hasFallback());
    }

    @Test
    void shouldCreateWithDefaultValue() {
        MessageKey key = MessageKey.builder("msg.test")
                .defaultValue("<red>Padrão</red>")
                .build();

        assertTrue(key.hasDefaultValue());
        assertEquals("<red>Padrão</red>", key.getDefaultValue());
    }

    @Test
    void shouldCopyWithPlaceholders() {
        MessageKey original = MessageKey.of("msg.test");
        MessageKey withPh = original.withPlaceholders("player", "amount");

        assertEquals("msg.test", withPh.getKey());
        assertEquals(2, withPh.getExpectedPlaceholders().size());
    }

    @Test
    void shouldCopyWithFallback() {
        MessageKey original = MessageKey.of("msg.test");
        MessageKey withFb = original.withFallback("msg.generic");

        assertEquals("msg.test", withFb.getKey());
        assertTrue(withFb.hasFallback());
        assertEquals("msg.generic", withFb.getFallbackKey());
    }

    @Test
    void shouldSetSourceInfo() {
        MessageKey key = MessageKey.of("msg.test")
                .withSourceInfo("MyClass", 42);

        assertTrue(key.hasSourceInfo());
        assertEquals("MyClass", key.getSourceClass());
        assertEquals(42, key.getSourceLine());
    }

    @Test
    void shouldNotHaveSourceInfoByDefault() {
        MessageKey key = MessageKey.of("msg.test");
        assertFalse(key.hasSourceInfo());
    }

    @Test
    void shouldGetExpectedFilePathWithModule() {
        MessageKey key = MessageKey.of("combat.mode.enabled");
        assertEquals("modules/combat/lang/messages.yml", key.getExpectedFilePath());
    }

    @Test
    void shouldGetExpectedFilePathWithoutModule() {
        MessageKey key = MessageKey.of("simple");
        assertEquals("lang/messages.yml", key.getExpectedFilePath());
    }

    @Test
    void shouldBeEqualByKeyOnly() {
        MessageKey k1 = MessageKey.builder("msg.test").module("mod1").build();
        MessageKey k2 = MessageKey.builder("msg.test").module("mod2").build();

        assertEquals(k1, k2);
        assertEquals(k1.hashCode(), k2.hashCode());
    }

    @Test
    void shouldNotBeEqualWithDifferentKeys() {
        MessageKey k1 = MessageKey.of("msg.one");
        MessageKey k2 = MessageKey.of("msg.two");
        assertNotEquals(k1, k2);
    }

    @Test
    void shouldThrowOnNullKey() {
        assertThrows(IllegalArgumentException.class, () -> MessageKey.builder(null));
    }

    @Test
    void shouldThrowOnEmptyKey() {
        assertThrows(IllegalArgumentException.class, () -> MessageKey.builder(""));
    }

    @Test
    void shouldThrowOnBlankKey() {
        assertThrows(IllegalArgumentException.class, () -> MessageKey.builder("  "));
    }

    @Test
    void shouldTrimKeyWhitespace() {
        MessageKey key = MessageKey.of("  combat.test  ");
        assertEquals("combat.test", key.getKey());
    }

    @Test
    void shouldHaveReadableToString() {
        MessageKey key = MessageKey.builder("combat.test")
                .module("combat")
                .placeholders("player")
                .build();
        String str = key.toString();
        assertTrue(str.contains("combat.test"));
        assertTrue(str.contains("combat"));
    }
}

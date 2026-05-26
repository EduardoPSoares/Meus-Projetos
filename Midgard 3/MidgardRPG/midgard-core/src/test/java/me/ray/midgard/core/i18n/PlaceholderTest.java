package me.ray.midgard.core.i18n;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderTest {

    @Test
    void shouldCreateStringPlaceholder() {
        Placeholder p = Placeholder.of("name", "Steve");
        assertEquals("name", p.getKey());
        assertEquals("Steve", p.getValue());
    }

    @Test
    void shouldCreateNumberPlaceholder() {
        Placeholder p = Placeholder.of("damage", 42);
        assertEquals("42", p.getValue());
    }

    @Test
    void shouldCreateDecimalPlaceholder() {
        Placeholder p = Placeholder.of("rate", 3.14159, 2);
        // String.format is locale-sensitive: pt-BR uses comma
        String expected = String.format("%.2f", 3.14159);
        assertEquals(expected, p.getValue());
    }

    @Test
    void shouldCreateBooleanPlaceholderTrue() {
        Placeholder p = Placeholder.of("active", true);
        assertTrue(p.getValue().contains("Sim"));
    }

    @Test
    void shouldCreateBooleanPlaceholderFalse() {
        Placeholder p = Placeholder.of("active", false);
        assertTrue(p.getValue().contains("Não"));
    }

    @Test
    void shouldCreateCustomBooleanPlaceholder() {
        Placeholder p = Placeholder.of("on", true, "ON", "OFF");
        assertEquals("ON", p.getValue());

        Placeholder p2 = Placeholder.of("on", false, "ON", "OFF");
        assertEquals("OFF", p2.getValue());
    }

    @Test
    void shouldFormatTime() {
        Placeholder p = Placeholder.time("duration", 125);
        assertEquals("02:05", p.getValue());
    }

    @Test
    void shouldFormatTimeZero() {
        Placeholder p = Placeholder.time("duration", 0);
        assertEquals("00:00", p.getValue());
    }

    @Test
    void shouldFormatTimeDetailed() {
        Placeholder p = Placeholder.timeDetailed("duration", 3661);
        assertEquals("1h 1m 1s", p.getValue());
    }

    @Test
    void shouldFormatTimeDetailedOnlySeconds() {
        Placeholder p = Placeholder.timeDetailed("duration", 45);
        assertEquals("45s", p.getValue());
    }

    @Test
    void shouldFormatTimeDetailedZero() {
        Placeholder p = Placeholder.timeDetailed("duration", 0);
        assertEquals("0s", p.getValue());
    }

    @Test
    void shouldFormatPercent() {
        Placeholder p = Placeholder.percent("chance", 0.75);
        // String.format is locale-sensitive
        String expected = String.format("%.1f%%", 75.0);
        assertEquals(expected, p.getValue());
    }

    @Test
    void shouldFormatPercentOver1() {
        Placeholder p = Placeholder.percent("chance", 50.0);
        String expected = String.format("%.1f%%", 50.0);
        assertEquals(expected, p.getValue());
    }

    @Test
    void shouldFormatFormattedKey() {
        Placeholder p = Placeholder.of("player", "Steve");
        assertEquals("%player%", p.getFormattedKey());
    }

    @Test
    void shouldApplyToString() {
        Placeholder p = Placeholder.of("name", "Steve");
        String result = p.apply("Hello %name%!");
        assertEquals("Hello Steve!", result);
    }

    @Test
    void shouldReturnNullForNullApply() {
        Placeholder p = Placeholder.of("name", "Steve");
        assertNull(p.apply(null));
    }

    @Test
    void shouldApplyAllPlaceholders() {
        Placeholder p1 = Placeholder.of("name", "Steve");
        Placeholder p2 = Placeholder.of("level", 10);

        String result = Placeholder.applyAll("%name% is level %level%", p1, p2);
        assertEquals("Steve is level 10", result);
    }

    @Test
    void shouldReturnNullForNullApplyAll() {
        assertNull(Placeholder.applyAll(null, Placeholder.of("x", "y")));
    }

    @Test
    void shouldReturnOriginalForNullPlaceholders() {
        assertEquals("hello", Placeholder.applyAll("hello", (Placeholder[]) null));
    }

    @Test
    void shouldCreateToMap() {
        Placeholder p1 = Placeholder.of("name", "Steve");
        Placeholder p2 = Placeholder.of("level", 5);

        Map<String, String> map = Placeholder.toMap(p1, p2);
        assertEquals(2, map.size());
        assertEquals("Steve", map.get("name"));
        assertEquals("5", map.get("level"));
    }

    @Test
    void shouldHandleNullsInToMap() {
        Map<String, String> map = Placeholder.toMap(null, Placeholder.of("a", "b"), null);
        assertEquals(1, map.size());
        assertEquals("b", map.get("a"));
    }

    @Test
    void shouldHandleNullValueAsEmpty() {
        Placeholder p = Placeholder.of("key", (String) null);
        assertEquals("", p.getValue());
    }

    @Test
    void shouldThrowOnNullKey() {
        assertThrows(NullPointerException.class, () -> Placeholder.of(null, "val"));
    }

    @Test
    void shouldSupportEqualsAndHashCode() {
        Placeholder p1 = Placeholder.of("name", "Steve");
        Placeholder p2 = Placeholder.of("name", "Steve");
        Placeholder p3 = Placeholder.of("name", "Alex");

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);
    }

    @Test
    void shouldCreateProgressBar() {
        Placeholder p = Placeholder.progressBar("bar", 5, 10, 10);
        assertNotNull(p.getValue());
        assertTrue(p.getValue().contains("█"));
        assertTrue(p.getValue().contains("░"));
    }

    @Test
    void shouldCreateColoredPlaceholder() {
        Placeholder p = Placeholder.colored("hp", 80, 100);
        assertNotNull(p.getValue());
        assertTrue(p.getValue().contains("green"));
    }

    @Test
    void shouldCreateColoredLowValue() {
        Placeholder p = Placeholder.colored("hp", 10, 100);
        assertNotNull(p.getValue());
        assertTrue(p.getValue().contains("red"));
    }
}

package me.ray.midgard.modules.spells.obj;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SpellTypeTest {

    @Test
    @DisplayName("Deve ter 3 tipos")
    void shouldHaveThreeTypes() {
        assertEquals(3, SpellType.values().length);
    }

    @Test
    @DisplayName("fromString: case insensitive")
    void shouldParseCaseInsensitive() {
        assertEquals(SpellType.PASSIVE, SpellType.fromString("passive"));
        assertEquals(SpellType.PASSIVE, SpellType.fromString("PASSIVE"));
        assertEquals(SpellType.PASSIVE, SpellType.fromString("Passive"));
    }

    @Test
    @DisplayName("fromString: todos os tipos válidos")
    void shouldParseValidTypes() {
        assertEquals(SpellType.PASSIVE, SpellType.fromString("PASSIVE"));
        assertEquals(SpellType.COMMON, SpellType.fromString("COMMON"));
        assertEquals(SpellType.ULTIMATE, SpellType.fromString("ULTIMATE"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("fromString: null/empty retorna COMMON")
    void shouldReturnCommon_forNullOrEmpty(String value) {
        assertEquals(SpellType.COMMON, SpellType.fromString(value));
    }

    @Test
    @DisplayName("fromString: string inválida retorna COMMON")
    void shouldReturnCommon_forInvalidString() {
        assertEquals(SpellType.COMMON, SpellType.fromString("INVALID"));
        assertEquals(SpellType.COMMON, SpellType.fromString("xyz"));
    }
}

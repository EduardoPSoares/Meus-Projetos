package me.ray.midgard.modules.item.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ItemTypeTest {

    @Test
    void shouldHaveAllExpectedValues() {
        ItemType[] values = ItemType.values();
        assertEquals(10, values.length);
    }

    @ParameterizedTest
    @EnumSource(ItemType.class)
    void fromString_shouldReturnCorrectType_forExactName(ItemType type) {
        assertEquals(type, ItemType.fromString(type.name()));
    }

    @ParameterizedTest
    @EnumSource(ItemType.class)
    void fromString_shouldBeCaseInsensitive(ItemType type) {
        assertEquals(type, ItemType.fromString(type.name().toLowerCase()));
    }

    @Test
    void fromString_shouldReturnMisc_forUnknownType() {
        assertEquals(ItemType.MISC, ItemType.fromString("INVALID_TYPE"));
    }

    @Test
    void fromString_shouldReturnMisc_forEmptyString() {
        assertEquals(ItemType.MISC, ItemType.fromString(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sword", "Sword", "SWORD", "sWoRd"})
    void fromString_shouldHandleMixedCase(String input) {
        assertEquals(ItemType.SWORD, ItemType.fromString(input));
    }

    @Test
    void fromString_shouldReturnMisc_forSpecialCharacters() {
        assertEquals(ItemType.MISC, ItemType.fromString("@#$%"));
    }

    @Test
    void shouldContainExpectedTypes() {
        assertNotNull(ItemType.valueOf("SWORD"));
        assertNotNull(ItemType.valueOf("AXE"));
        assertNotNull(ItemType.valueOf("BOW"));
        assertNotNull(ItemType.valueOf("STAFF"));
        assertNotNull(ItemType.valueOf("WAND"));
        assertNotNull(ItemType.valueOf("ARMOR"));
        assertNotNull(ItemType.valueOf("CONSUMABLE"));
        assertNotNull(ItemType.valueOf("MATERIAL"));
        assertNotNull(ItemType.valueOf("GEM_STONE"));
        assertNotNull(ItemType.valueOf("MISC"));
    }
}

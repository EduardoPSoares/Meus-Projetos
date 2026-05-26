package com.midgard.fooddecay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightFormatUtilTest {

    @Test
    void formatsKgWithUpToThreeDecimals() {
        assertEquals("1", WeightFormatUtil.formatKgDisplay(1.0));
        assertEquals("0.125", WeightFormatUtil.formatKgDisplay(0.125));
        assertEquals("0.12", WeightFormatUtil.formatKgDisplay(0.120));
        assertEquals("1.235", WeightFormatUtil.formatKgDisplay(1.23456));
    }

    @Test
    void buildsLoreLineWithFormattedWeightAndStackLimit() {
        String lore = WeightFormatUtil.buildLoreLine("\u00A78\u00A7l\u00A7r", "&e", 0.125, "Medio", 16);

        assertTrue(lore.contains("\u2696 0.125 kg"));
        assertTrue(lore.contains("&8|"));
        assertTrue(lore.contains("max 16"));
    }
}

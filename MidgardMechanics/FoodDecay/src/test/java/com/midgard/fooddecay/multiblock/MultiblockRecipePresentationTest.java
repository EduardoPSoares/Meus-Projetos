package com.midgard.fooddecay.multiblock;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiblockRecipePresentationTest {

    @Test
    void formatsMmoItemsLabelsWhenHookPreviewIsUnavailable() {
        MultiblockRecipe recipe = new MultiblockRecipe(
                "jerky",
                null,
                null, "CONSUMABLE", "carne_bovina", null, 0,
                null, "CONSUMABLE", "carne_seca", null, null, null, 0,
                0, null,
                30, null,
                null, null, List.of()
        );

        assertEquals("Carne Bovina", recipe.getInputDisplayName());
        assertEquals("Carne Bovina (CONSUMABLE:carne_bovina)", recipe.getInputReferenceLabel());
        assertEquals("Carne Seca", recipe.getOutputDisplayName());
        assertEquals("Carne Seca (CONSUMABLE:carne_seca)", recipe.getOutputReferenceLabel());
    }

    @Test
    void keepsCustomOutputNameReadableForAdminLabels() {
        MultiblockRecipe recipe = new MultiblockRecipe(
                "smoked_fish",
                null,
                null, "CONSUMABLE", "peixe_cru", null, 0,
                null, "CONSUMABLE", "peixe_defumado", null, "&6Peixe Defumado", null, 0,
                0, null,
                20, null,
                null, null, List.of()
        );

        assertEquals("Peixe Defumado", recipe.getOutputDisplayName().replaceAll("(?i)&[0-9A-FK-OR]", "").trim());
        assertEquals(
                "Peixe Defumado (base: Peixe Defumado (CONSUMABLE:peixe_defumado))",
                recipe.getOutputReferenceLabel()
        );
    }

    @Test
    void formatsItemsAdderLabelsWhenHookPreviewIsUnavailable() {
        MultiblockRecipe recipe = new MultiblockRecipe(
                "aged_beef",
                null,
                null, null, null, "my_pack:carne_bovina", 0,
                null, null, null, "my_pack:carne_maturada", null, null, 0,
                0, null,
                40, null,
                null, null, List.of()
        );

        assertEquals("Carne Bovina", recipe.getInputDisplayName());
        assertEquals("Carne Bovina (my_pack:carne_bovina)", recipe.getInputReferenceLabel());
        assertEquals("Carne Maturada", recipe.getOutputDisplayName());
        assertEquals("Carne Maturada (my_pack:carne_maturada)", recipe.getOutputReferenceLabel());
    }
}

package com.midgard.fooddecay.multiblock;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecipeIngredientTest {

    @Test
    void parsesVanillaIngredientWithAmountAndModelData() {
        RecipeIngredient ingredient = RecipeIngredient.parseShorthand("BREAD#15*3");

        assertNotNull(ingredient);
        assertEquals(Material.BREAD, ingredient.getMaterial());
        assertEquals(15, ingredient.getCustomModelData());
        assertEquals(3, ingredient.getAmount());
        assertEquals("BREAD#15*3", ingredient.toShorthand());
    }

    @Test
    void parsesMmoItemsAndItemsAdderShorthand() {
        RecipeIngredient mmoIngredient = RecipeIngredient.parseShorthand("mmo:CONSUMABLE:carne_bovina*2");
        RecipeIngredient itemsAdderIngredient = RecipeIngredient.parseShorthand("ia:midgard:tomate");

        assertNotNull(mmoIngredient);
        assertEquals("CONSUMABLE", mmoIngredient.getMmoType());
        assertEquals("carne_bovina", mmoIngredient.getMmoId());
        assertEquals(2, mmoIngredient.getAmount());

        assertNotNull(itemsAdderIngredient);
        assertEquals("midgard:tomate", itemsAdderIngredient.getItemsAdderId());
        assertEquals(1, itemsAdderIngredient.getAmount());
    }

    @Test
    void rejectsInvalidShorthand() {
        assertNull(RecipeIngredient.parseShorthand("INVALID MATERIAL"));
        assertNull(RecipeIngredient.parseShorthand("mmo:sem-id"));
        assertNull(RecipeIngredient.parseShorthand("ia:item-sem-namespace"));
    }
}

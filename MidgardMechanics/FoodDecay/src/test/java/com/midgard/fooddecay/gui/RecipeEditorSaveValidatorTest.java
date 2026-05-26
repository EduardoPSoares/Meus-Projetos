package com.midgard.fooddecay.gui;

import com.midgard.fooddecay.FoodTrait;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeEditorSaveValidatorTest {

    @Test
    void reportsMissingBaseRecipeFieldsOnRecipePage() {
        RecipeEditorSaveValidator.ValidationResult validation = RecipeEditorSaveValidator.validate(
                false,
                false,
                0,
                null,
                null,
                0,
                null,
                0D
        );

        assertTrue(validation.hasMissingRequirements());
        assertIterableEquals(
                List.of("Entrada", "Saida", "Tempo de processamento", "Traco aplicado"),
                validation.missingRequirements()
        );
        assertEquals(RecipeEditorPage.RECIPE, validation.firstMissingPage());
    }

    @Test
    void reportsMmocoreProblemsOnMmocorePage() {
        RecipeEditorSaveValidator.ValidationResult validation = RecipeEditorSaveValidator.validate(
                true,
                true,
                20,
                FoodTrait.SALTED,
                "cooking",
                0,
                null,
                5D
        );

        assertTrue(validation.hasMissingRequirements());
        assertIterableEquals(
                List.of("Nivel minimo da profissao", "Profissao da experiencia"),
                validation.missingRequirements()
        );
        assertEquals(RecipeEditorPage.MMOCORE, validation.firstMissingPage());
    }

    @Test
    void acceptsCompleteRecipeState() {
        RecipeEditorSaveValidator.ValidationResult validation = RecipeEditorSaveValidator.validate(
                true,
                true,
                30,
                FoodTrait.DRIED,
                "cooking",
                10,
                "cooking",
                8D
        );

        assertFalse(validation.hasMissingRequirements());
        assertTrue(validation.missingRequirements().isEmpty());
        assertEquals(RecipeEditorPage.RECIPE, validation.firstMissingPage());
    }
}

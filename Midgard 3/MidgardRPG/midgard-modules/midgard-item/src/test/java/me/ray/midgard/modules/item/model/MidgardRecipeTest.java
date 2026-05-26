package me.ray.midgard.modules.item.model;

import me.ray.midgard.modules.item.model.MidgardRecipe.RecipeType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MidgardRecipeTest {

    @Nested
    class TypeOnlyConstructor {

        @Test
        void shouldGenerateId() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.SHAPED);
            assertNotNull(recipe.getId());
            assertEquals(8, recipe.getId().length());
        }

        @Test
        void shouldSetType() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.FURNACE);
            assertEquals(RecipeType.FURNACE, recipe.getType());
        }

        @Test
        void shouldHaveDefaultValues() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.SHAPED);
            assertEquals(1, recipe.getOutputAmount());
            assertTrue(recipe.isShaped());
            assertEquals(200, recipe.getCookTime());
            assertEquals(0.0, recipe.getExperience());
            assertFalse(recipe.isHiddenFromBook());
            assertTrue(recipe.getIngredients().isEmpty());
        }
    }

    @Nested
    class IdAndTypeConstructor {

        @Test
        void shouldSetIdAndType() {
            MidgardRecipe recipe = new MidgardRecipe("my-recipe", RecipeType.BLAST_FURNACE);
            assertEquals("my-recipe", recipe.getId());
            assertEquals(RecipeType.BLAST_FURNACE, recipe.getType());
        }

        @Test
        void shouldHaveDefaultValues() {
            MidgardRecipe recipe = new MidgardRecipe("id", RecipeType.SHAPELESS);
            assertEquals(1, recipe.getOutputAmount());
            assertTrue(recipe.isShaped());
            assertEquals(200, recipe.getCookTime());
            assertEquals(0.0, recipe.getExperience());
        }
    }

    @Nested
    class Ingredients {

        @Test
        void shouldSetAndGetIngredients() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.SHAPED);
            recipe.setIngredient(0, "IRON_INGOT");
            recipe.setIngredient(1, "GOLD_INGOT");

            assertEquals("IRON_INGOT", recipe.getIngredients().get(0));
            assertEquals("GOLD_INGOT", recipe.getIngredients().get(1));
        }

        @Test
        void shouldRemoveIngredient_whenNull() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.SHAPED);
            recipe.setIngredient(0, "IRON_INGOT");
            recipe.setIngredient(0, null);

            assertFalse(recipe.getIngredients().containsKey(0));
        }

        @Test
        void shouldSetIngredientMap() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.SHAPED);
            Map<Integer, String> ingredients = new HashMap<>();
            ingredients.put(0, "DIAMOND");
            ingredients.put(4, "STICK");

            recipe.setIngredients(ingredients);
            assertEquals(2, recipe.getIngredients().size());
            assertEquals("DIAMOND", recipe.getIngredients().get(0));
        }
    }

    @Nested
    class Setters {

        @Test
        void shouldSetOutputAmount() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.SHAPED);
            recipe.setOutputAmount(4);
            assertEquals(4, recipe.getOutputAmount());
        }

        @Test
        void shouldSetHiddenFromBook() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.SHAPED);
            recipe.setHiddenFromBook(true);
            assertTrue(recipe.isHiddenFromBook());
        }

        @Test
        void shouldSetShaped() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.SHAPED);
            recipe.setShaped(false);
            assertFalse(recipe.isShaped());
        }

        @Test
        void shouldSetExperience() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.FURNACE);
            recipe.setExperience(1.5);
            assertEquals(1.5, recipe.getExperience());
        }

        @Test
        void shouldSetCookTime() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.FURNACE);
            recipe.setCookTime(100);
            assertEquals(100, recipe.getCookTime());
        }

        @Test
        void shouldSetType() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.SHAPED);
            recipe.setType(RecipeType.SHAPELESS);
            assertEquals(RecipeType.SHAPELESS, recipe.getType());
        }
    }

    @Nested
    class ForgeSpecific {

        @Test
        void shouldSetForgeFields() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.FORGE);
            recipe.setForgeDifficulty(7);
            recipe.setForgeMinLevel(5);
            recipe.setForgeTier("ADVANCED");
            recipe.setForgeRecipeId("recipe-123");

            assertEquals(7, recipe.getForgeDifficulty());
            assertEquals(5, recipe.getForgeMinLevel());
            assertEquals("ADVANCED", recipe.getForgeTier());
            assertEquals("recipe-123", recipe.getForgeRecipeId());
        }
    }

    @Nested
    class SmeltingSpecific {

        @Test
        void shouldSetSmeltingFields() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.SMELTING);
            recipe.setSmeltingMinTemperature(1200);

            Map<String, Integer> metals = new HashMap<>();
            metals.put("IRON", 500);
            metals.put("GOLD", 200);
            recipe.setSmeltingMetals(metals);

            assertEquals(1200, recipe.getSmeltingMinTemperature());
            assertEquals(500, recipe.getSmeltingMetals().get("IRON"));
            assertEquals(200, recipe.getSmeltingMetals().get("GOLD"));
        }

        @Test
        void shouldSetIndividualSmeltingMetal() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.SMELTING);
            recipe.setSmeltingMetal("COPPER", 300);

            assertEquals(300, recipe.getSmeltingMetals().get("COPPER"));
        }

        @Test
        void shouldRemoveSmeltingMetal_whenAmountZeroOrNegative() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.SMELTING);
            recipe.setSmeltingMetal("COPPER", 300);
            recipe.setSmeltingMetal("COPPER", 0);

            assertFalse(recipe.getSmeltingMetals().containsKey("COPPER"));
        }

        @Test
        void shouldReturnEmptyMap_whenSmeltingMetalsNull() {
            MidgardRecipe recipe = new MidgardRecipe(RecipeType.SMELTING);
            assertNotNull(recipe.getSmeltingMetals());
            assertTrue(recipe.getSmeltingMetals().isEmpty());
        }
    }

    @Nested
    class RecipeTypeEnum {

        @Test
        void shouldHaveAllExpectedTypes() {
            assertEquals(12, RecipeType.values().length);
        }

        @Test
        void shouldContainExpectedTypes() {
            assertNotNull(RecipeType.valueOf("SHAPED"));
            assertNotNull(RecipeType.valueOf("SHAPELESS"));
            assertNotNull(RecipeType.valueOf("FURNACE"));
            assertNotNull(RecipeType.valueOf("BLAST_FURNACE"));
            assertNotNull(RecipeType.valueOf("SMOKER"));
            assertNotNull(RecipeType.valueOf("CAMPFIRE"));
            assertNotNull(RecipeType.valueOf("SMITHING"));
            assertNotNull(RecipeType.valueOf("STONE_CUTTING"));
            assertNotNull(RecipeType.valueOf("MEGA_SHAPED"));
            assertNotNull(RecipeType.valueOf("SUPER_SHAPED"));
            assertNotNull(RecipeType.valueOf("FORGE"));
            assertNotNull(RecipeType.valueOf("SMELTING"));
        }
    }
}

package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import org.bukkit.Color;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlloyRecipeAndMoltenMetalTest {

    // ═══ AlloyRecipe Tests ═══

    @Test
    @DisplayName("AlloyRecipe: deve armazenar dados corretamente")
    void shouldStoreRecipeData() {
        Map<MoltenMetal, Integer> ingredients = Map.of(
                MoltenMetal.COPPER, 432,
                MoltenMetal.GOLD, 144
        );
        AlloyRecipe recipe = new AlloyRecipe("bronze", MoltenMetal.BRONZE, 576, ingredients, 700);

        assertEquals("bronze", recipe.getId());
        assertEquals(MoltenMetal.BRONZE, recipe.getResult());
        assertEquals(576, recipe.getResultAmount());
        assertEquals(700, recipe.getMinSmelteryTemperature());
    }

    @Test
    @DisplayName("AlloyRecipe: getIngredients deve ser imutável")
    void shouldReturnUnmodifiableIngredients() {
        AlloyRecipe recipe = new AlloyRecipe("test", MoltenMetal.BRONZE, 576,
                Map.of(MoltenMetal.COPPER, 432), 700);
        assertThrows(UnsupportedOperationException.class,
                () -> recipe.getIngredients().put(MoltenMetal.IRON, 100));
    }

    @Test
    @DisplayName("AlloyRecipe: getMaxCrafts deve calcular corretamente")
    void shouldCalculateMaxCrafts() {
        AlloyRecipe recipe = new AlloyRecipe("bronze", MoltenMetal.BRONZE, 576,
                Map.of(MoltenMetal.COPPER, 432, MoltenMetal.GOLD, 144), 700);

        // 864/432 = 2, 288/144 = 2 → min = 2
        Map<MoltenMetal, Integer> tank = Map.of(
                MoltenMetal.COPPER, 864,
                MoltenMetal.GOLD, 288
        );
        assertEquals(2, recipe.getMaxCrafts(tank));
    }

    @Test
    @DisplayName("AlloyRecipe: getMaxCrafts com ingrediente insuficiente")
    void shouldLimitByScarcestIngredient() {
        AlloyRecipe recipe = new AlloyRecipe("bronze", MoltenMetal.BRONZE, 576,
                Map.of(MoltenMetal.COPPER, 432, MoltenMetal.GOLD, 144), 700);

        // 864/432 = 2, 100/144 = 0 → min = 0
        Map<MoltenMetal, Integer> tank = Map.of(
                MoltenMetal.COPPER, 864,
                MoltenMetal.GOLD, 100
        );
        assertEquals(0, recipe.getMaxCrafts(tank));
    }

    @Test
    @DisplayName("AlloyRecipe: getMaxCrafts sem ingredientes deve retornar 0")
    void shouldReturnZeroCrafts_whenNoIngredients() {
        AlloyRecipe recipe = new AlloyRecipe("bronze", MoltenMetal.BRONZE, 576,
                Map.of(MoltenMetal.COPPER, 432), 700);
        assertEquals(0, recipe.getMaxCrafts(Map.of()));
    }

    @Test
    @DisplayName("AlloyRecipe: canForm deve verificar temperatura e ingredientes")
    void shouldCheckTemperatureAndIngredients() {
        AlloyRecipe recipe = new AlloyRecipe("bronze", MoltenMetal.BRONZE, 576,
                Map.of(MoltenMetal.COPPER, 432, MoltenMetal.GOLD, 144), 700);

        Map<MoltenMetal, Integer> tank = Map.of(
                MoltenMetal.COPPER, 432,
                MoltenMetal.GOLD, 144
        );

        // Enough ingredients, enough temp
        assertTrue(recipe.canForm(tank, 800));
        // Enough ingredients, insufficient temp
        assertFalse(recipe.canForm(tank, 600));
        // Insufficient ingredients
        assertFalse(recipe.canForm(Map.of(MoltenMetal.COPPER, 100), 800));
    }

    // ═══ MoltenMetal Tests ═══

    @ParameterizedTest
    @EnumSource(MoltenMetal.class)
    @DisplayName("MoltenMetal: todos devem ter meltingPoint positivo")
    void shouldHavePositiveMeltingPoint(MoltenMetal metal) {
        assertTrue(metal.getMeltingPoint() > 0,
                metal.name() + " deve ter meltingPoint > 0");
    }

    @ParameterizedTest
    @EnumSource(MoltenMetal.class)
    @DisplayName("MoltenMetal: todos devem ter cor definida")
    void shouldHaveColor(MoltenMetal metal) {
        assertNotNull(metal.getColor());
    }

    @ParameterizedTest
    @EnumSource(MoltenMetal.class)
    @DisplayName("MoltenMetal: todos devem ter hardness > 0")
    void shouldHavePositiveHardness(MoltenMetal metal) {
        assertTrue(metal.getHardness() > 0);
    }

    @ParameterizedTest
    @EnumSource(MoltenMetal.class)
    @DisplayName("MoltenMetal: todos devem ter visual block definido")
    void shouldHaveVisualBlock(MoltenMetal metal) {
        assertNotNull(metal.getVisualBlock());
    }

    @Test
    @DisplayName("Ligas não devem ter sourceItem")
    void shouldHaveNoSourceItem_forAlloys() {
        for (MoltenMetal metal : MoltenMetal.values()) {
            if (metal.isAlloy()) {
                assertNull(metal.getSourceItem(),
                        metal.name() + " é liga e não deve ter sourceItem");
            }
        }
    }

    @Test
    @DisplayName("Metais base devem ter sourceItem")
    void shouldHaveSourceItem_forBaseMetals() {
        for (MoltenMetal metal : MoltenMetal.values()) {
            if (!metal.isAlloy()) {
                assertNotNull(metal.getSourceItem(),
                        metal.name() + " é metal base e deve ter sourceItem");
            }
        }
    }

    @Test
    @DisplayName("getUnitsPerItem: ligas devem retornar 0")
    void shouldReturnZeroUnits_forAlloys() {
        assertEquals(0, MoltenMetal.BRONZE.getUnitsPerItem());
        assertEquals(0, MoltenMetal.STEEL.getUnitsPerItem());
    }

    @Test
    @DisplayName("getUnitsPerItem: IRON deve retornar 144 (1 ingot)")
    void shouldReturn144_forIron() {
        assertEquals(144, MoltenMetal.IRON.getUnitsPerItem());
    }

    @Test
    @DisplayName("getUnitsPerItem: DIAMOND deve retornar 666")
    void shouldReturn666_forDiamond() {
        assertEquals(666, MoltenMetal.DIAMOND.getUnitsPerItem());
    }

    @Test
    @DisplayName("getUnitsPerItem: LAPIS deve retornar 100")
    void shouldReturn100_forLapis() {
        assertEquals(100, MoltenMetal.LAPIS.getUnitsPerItem());
    }

    @Test
    @DisplayName("fromSourceItem: IRON_INGOT → IRON")
    void shouldFindIronFromIngot() {
        assertEquals(MoltenMetal.IRON, MoltenMetal.fromSourceItem(Material.IRON_INGOT));
    }

    @Test
    @DisplayName("fromSourceItem: GOLD_INGOT → GOLD")
    void shouldFindGoldFromIngot() {
        assertEquals(MoltenMetal.GOLD, MoltenMetal.fromSourceItem(Material.GOLD_INGOT));
    }

    @Test
    @DisplayName("fromSourceItem: material inválido → null")
    void shouldReturnNull_forInvalidMaterial() {
        assertNull(MoltenMetal.fromSourceItem(Material.DIRT));
    }

    @Test
    @DisplayName("getSmeltTimePerItem deve ser proporcional ao melting point")
    void shouldCalculateSmeltTime() {
        // IRON: 1538 / 10 = 153 (int)
        assertEquals(153, MoltenMetal.IRON.getSmeltTimePerItem());
    }

    @Test
    @DisplayName("BRONZE deve ser liga")
    void shouldBeAlloy_forBronze() {
        assertTrue(MoltenMetal.BRONZE.isAlloy());
    }

    @Test
    @DisplayName("IRON não deve ser liga")
    void shouldNotBeAlloy_forIron() {
        assertFalse(MoltenMetal.IRON.isAlloy());
    }
}

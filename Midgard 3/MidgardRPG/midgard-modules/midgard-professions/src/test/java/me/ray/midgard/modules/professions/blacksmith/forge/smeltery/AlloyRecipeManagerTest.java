package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlloyRecipeManagerTest {

    private AlloyRecipeManager manager;
    private AlloyRecipe bronzeRecipe;
    private AlloyRecipe steelRecipe;

    @BeforeEach
    void setUp() {
        manager = new AlloyRecipeManager();
        bronzeRecipe = new AlloyRecipe("bronze", MoltenMetal.BRONZE, 576,
                Map.of(MoltenMetal.COPPER, 432, MoltenMetal.GOLD, 144), 700);
        steelRecipe = new AlloyRecipe("steel", MoltenMetal.STEEL, 288,
                Map.of(MoltenMetal.IRON, 288), 1200);
    }

    @Test
    @DisplayName("register e size")
    void shouldRegisterRecipes() {
        assertEquals(0, manager.size());
        manager.register(bronzeRecipe);
        assertEquals(1, manager.size());
        manager.register(steelRecipe);
        assertEquals(2, manager.size());
    }

    @Test
    @DisplayName("getAllRecipes deve ser imutável")
    void shouldReturnUnmodifiableList() {
        manager.register(bronzeRecipe);
        assertThrows(UnsupportedOperationException.class,
                () -> manager.getAllRecipes().add(steelRecipe));
    }

    @Test
    @DisplayName("clear deve remover todas as receitas")
    void shouldClear() {
        manager.register(bronzeRecipe);
        manager.register(steelRecipe);
        assertEquals(2, manager.size());
        manager.clear();
        assertEquals(0, manager.size());
    }

    @Test
    @DisplayName("findFormableAlloys quando todos ingredientes e temperatura suficientes")
    void shouldFindFormableAlloys() {
        manager.register(bronzeRecipe);
        manager.register(steelRecipe);

        Map<MoltenMetal, Integer> tank = Map.of(
                MoltenMetal.COPPER, 432,
                MoltenMetal.GOLD, 144,
                MoltenMetal.IRON, 288
        );

        List<AlloyRecipe> result = manager.findFormableAlloys(tank, 1500);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findFormableAlloys com temperatura insuficiente para steel")
    void shouldFilterByTemperature() {
        manager.register(bronzeRecipe);  // min 700
        manager.register(steelRecipe);   // min 1200

        Map<MoltenMetal, Integer> tank = Map.of(
                MoltenMetal.COPPER, 432,
                MoltenMetal.GOLD, 144,
                MoltenMetal.IRON, 288
        );

        List<AlloyRecipe> result = manager.findFormableAlloys(tank, 900);
        assertEquals(1, result.size());
        assertEquals("bronze", result.get(0).getId());
    }

    @Test
    @DisplayName("findFormableAlloys com tanque vazio retorna vazio")
    void shouldReturnEmpty_forEmptyTank() {
        manager.register(bronzeRecipe);
        List<AlloyRecipe> result = manager.findFormableAlloys(Map.of(), 2000);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findFormableAlloys sem receitas registradas retorna vazio")
    void shouldReturnEmpty_forNoRecipes() {
        Map<MoltenMetal, Integer> tank = Map.of(MoltenMetal.COPPER, 1000);
        assertTrue(manager.findFormableAlloys(tank, 2000).isEmpty());
    }
}

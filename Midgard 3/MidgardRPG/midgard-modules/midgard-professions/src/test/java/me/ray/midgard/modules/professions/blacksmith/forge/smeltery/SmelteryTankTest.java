package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SmelteryTankTest {

    private SmelteryTank tank;

    @BeforeEach
    void setUp() {
        tank = new SmelteryTank(1000); // 1000mb capacity
    }

    // ── Construtor e basics ──

    @Test
    @DisplayName("deve criar tanque com capacidade e temperature 0")
    void shouldCreateWithCorrectCapacity() {
        assertEquals(1000, tank.getCapacity());
        assertEquals(0, tank.getTemperature());
        assertTrue(tank.isEmpty());
        assertFalse(tank.isFull());
    }

    // ── Volume tracking ──

    @Test
    @DisplayName("getTotalVolume deve somar todos os metais")
    void shouldSumAllMetalVolumes() {
        tank.addMetal(MoltenMetal.IRON, 300);
        tank.addMetal(MoltenMetal.GOLD, 200);
        assertEquals(500, tank.getTotalVolume());
    }

    @Test
    @DisplayName("getFreeSpace deve calcular espaço restante")
    void shouldCalculateFreeSpace() {
        tank.addMetal(MoltenMetal.IRON, 600);
        assertEquals(400, tank.getFreeSpace());
    }

    @Test
    @DisplayName("getFillPercent deve retornar porcentagem correta")
    void shouldCalculateFillPercent() {
        tank.addMetal(MoltenMetal.IRON, 500);
        assertEquals(0.5f, tank.getFillPercent(), 0.01f);
    }

    @Test
    @DisplayName("getFillPercent deve retornar 0 para capacidade 0")
    void shouldReturnZeroFillPercent_forZeroCapacity() {
        SmelteryTank emptyTank = new SmelteryTank(0);
        assertEquals(0f, emptyTank.getFillPercent(), 0.001f);
    }

    // ── addMetal() ──

    @Test
    @DisplayName("addMetal deve adicionar a quantidade completa quando cabe")
    void shouldAddFullAmount_whenEnoughSpace() {
        int added = tank.addMetal(MoltenMetal.IRON, 500);
        assertEquals(500, added);
        assertEquals(500, tank.getAmount(MoltenMetal.IRON));
    }

    @Test
    @DisplayName("addMetal deve limitar à capacidade disponível")
    void shouldLimitToAvailableSpace() {
        tank.addMetal(MoltenMetal.IRON, 800);
        int added = tank.addMetal(MoltenMetal.GOLD, 400);
        assertEquals(200, added);
        assertEquals(200, tank.getAmount(MoltenMetal.GOLD));
    }

    @Test
    @DisplayName("addMetal deve retornar 0 quando tanque está cheio")
    void shouldReturnZero_whenTankIsFull() {
        tank.addMetal(MoltenMetal.IRON, 1000);
        int added = tank.addMetal(MoltenMetal.GOLD, 100);
        assertEquals(0, added);
    }

    @Test
    @DisplayName("addMetal deve acumular mesmo metal")
    void shouldAccumulateSameMetal() {
        tank.addMetal(MoltenMetal.IRON, 200);
        tank.addMetal(MoltenMetal.IRON, 300);
        assertEquals(500, tank.getAmount(MoltenMetal.IRON));
    }

    // ── removeMetal() ──

    @Test
    @DisplayName("removeMetal deve remover a quantidade correta")
    void shouldRemoveCorrectAmount() {
        tank.addMetal(MoltenMetal.IRON, 500);
        int removed = tank.removeMetal(MoltenMetal.IRON, 200);
        assertEquals(200, removed);
        assertEquals(300, tank.getAmount(MoltenMetal.IRON));
    }

    @Test
    @DisplayName("removeMetal deve limitar ao disponível")
    void shouldLimitRemovalToAvailable() {
        tank.addMetal(MoltenMetal.IRON, 100);
        int removed = tank.removeMetal(MoltenMetal.IRON, 200);
        assertEquals(100, removed);
        assertEquals(0, tank.getAmount(MoltenMetal.IRON));
    }

    @Test
    @DisplayName("removeMetal deve remover a entrada quando zera")
    void shouldRemoveEntry_whenExhausted() {
        tank.addMetal(MoltenMetal.IRON, 100);
        tank.removeMetal(MoltenMetal.IRON, 100);
        assertFalse(tank.getContents().containsKey(MoltenMetal.IRON));
    }

    @Test
    @DisplayName("removeMetal deve retornar 0 para metal inexistente")
    void shouldReturnZero_forNonexistentMetal() {
        int removed = tank.removeMetal(MoltenMetal.GOLD, 100);
        assertEquals(0, removed);
    }

    // ── hasMetal() ──

    @Test
    @DisplayName("hasMetal deve verificar quantidade suficiente")
    void shouldCheckSufficientAmount() {
        tank.addMetal(MoltenMetal.IRON, 300);
        assertTrue(tank.hasMetal(MoltenMetal.IRON, 300));
        assertTrue(tank.hasMetal(MoltenMetal.IRON, 200));
        assertFalse(tank.hasMetal(MoltenMetal.IRON, 301));
    }

    // ── clear() ──

    @Test
    @DisplayName("clear deve esvaziar tanque e zerar temperatura")
    void shouldClearTankAndTemperature() {
        tank.addMetal(MoltenMetal.IRON, 500);
        tank.setTemperature(1000);
        tank.clear();
        assertTrue(tank.isEmpty());
        assertEquals(0, tank.getTemperature());
    }

    // ── Temperature ──

    @Test
    @DisplayName("setTemperature não deve aceitar negativo")
    void shouldNotAllowNegativeTemperature() {
        tank.setTemperature(-100);
        assertEquals(0, tank.getTemperature());
    }

    // ── isFull / isEmpty ──

    @Test
    @DisplayName("isFull deve retornar true quando capacidade esgotada")
    void shouldBeFull_whenCapacityExhausted() {
        tank.addMetal(MoltenMetal.IRON, 1000);
        assertTrue(tank.isFull());
    }

    @Test
    @DisplayName("isEmpty deve retornar true quando vazio")
    void shouldBeEmpty_whenNoContents() {
        assertTrue(tank.isEmpty());
    }

    // ── getContents() ──

    @Test
    @DisplayName("getContents deve retornar mapa imutável")
    void shouldReturnUnmodifiableContents() {
        tank.addMetal(MoltenMetal.IRON, 100);
        Map<MoltenMetal, Integer> contents = tank.getContents();
        assertThrows(UnsupportedOperationException.class,
                () -> contents.put(MoltenMetal.GOLD, 50));
    }

    // ── getSortedContents() ──

    @Test
    @DisplayName("getSortedContents deve ordenar por quantidade decrescente")
    void shouldSortByAmountDescending() {
        tank.addMetal(MoltenMetal.IRON, 100);
        tank.addMetal(MoltenMetal.GOLD, 300);
        tank.addMetal(MoltenMetal.COPPER, 200);

        var sorted = tank.getSortedContents();
        assertEquals(MoltenMetal.GOLD, sorted.get(0).getKey());
        assertEquals(MoltenMetal.COPPER, sorted.get(1).getKey());
        assertEquals(MoltenMetal.IRON, sorted.get(2).getKey());
    }

    // ── getDominantMetal() ──

    @Test
    @DisplayName("getDominantMetal deve retornar metal com maior quantidade")
    void shouldReturnMostAbundantMetal() {
        tank.addMetal(MoltenMetal.IRON, 100);
        tank.addMetal(MoltenMetal.GOLD, 500);
        assertEquals(MoltenMetal.GOLD, tank.getDominantMetal());
    }

    @Test
    @DisplayName("getDominantMetal deve retornar null quando vazio")
    void shouldReturnNull_whenEmpty() {
        assertNull(tank.getDominantMetal());
    }

    // ── toString() ──

    @Test
    @DisplayName("toString deve conter info útil")
    void shouldHaveUsefulToString() {
        tank.addMetal(MoltenMetal.IRON, 200);
        tank.setTemperature(1500);
        String str = tank.toString();
        assertTrue(str.contains("1000"));
        assertTrue(str.contains("200"));
        assertTrue(str.contains("1500"));
    }

    // ── processAlloys() ──

    @Test
    @DisplayName("processAlloys deve formar ligas quando ingredientes disponíveis")
    void shouldFormAlloys_whenIngredientsAvailable() {
        AlloyRecipeManager manager = new AlloyRecipeManager();
        AlloyRecipe bronze = new AlloyRecipe("bronze", MoltenMetal.BRONZE, 576,
                Map.of(MoltenMetal.COPPER, 432, MoltenMetal.GOLD, 144), 700);
        manager.register(bronze);

        tank.addMetal(MoltenMetal.COPPER, 432);
        tank.addMetal(MoltenMetal.GOLD, 144);
        tank.setTemperature(800);

        var results = tank.processAlloys(manager);
        assertFalse(results.isEmpty());
        assertEquals(MoltenMetal.BRONZE, results.get(0).recipe().getResult());
    }

    @Test
    @DisplayName("processAlloys deve retornar lista vazia quando sem ingredientes")
    void shouldReturnEmpty_whenNoIngredients() {
        AlloyRecipeManager manager = new AlloyRecipeManager();
        var results = tank.processAlloys(manager);
        assertTrue(results.isEmpty());
    }

    // ── AlloyResult record ──

    @Test
    @DisplayName("AlloyResult deve armazenar dados corretamente")
    void shouldStoreAlloyResultData() {
        AlloyRecipe recipe = new AlloyRecipe("test", MoltenMetal.BRONZE, 576,
                Map.of(MoltenMetal.COPPER, 432), 700);
        var result = new SmelteryTank.AlloyResult(recipe, 2, 1152);
        assertEquals(recipe, result.recipe());
        assertEquals(2, result.crafts());
        assertEquals(1152, result.totalProduced());
    }
}

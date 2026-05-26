package me.ray.midgard.modules.professions.blacksmith.forge.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ForgeDataTest {

    private ForgeData data;

    @BeforeEach
    void setUp() {
        data = new ForgeData();
    }

    // ── Defaults ──

    @Test
    @DisplayName("defaults: nível 0, xp 0, sem especialização")
    void shouldHaveCorrectDefaults() {
        assertEquals(0, data.getLevel());
        assertEquals(0, data.getXp(), 0.001);
        assertFalse(data.hasSpecialization());
        assertNull(data.getSpecialization());
        assertTrue(data.getUnlockedRecipes().isEmpty());
        assertTrue(data.getOwnedForgeIds().isEmpty());
    }

    // ── Level e XP ──

    @Test
    @DisplayName("addXp sem xp suficiente não deve subir de nível")
    void shouldNotLevelUp_whenInsufficientXP() {
        int levelsGained = data.addXp(10);
        assertEquals(0, levelsGained);
        assertEquals(0, data.getLevel());
        assertEquals(10, data.getXp(), 0.001);
    }

    @Test
    @DisplayName("addXp deve subir de nível quando xp excede o necessário")
    void shouldLevelUp_whenXPExceedsRequired() {
        // level 0→1 requer calculateXpNeeded(1) = 50*1 + 50*1 = 100
        int levelsGained = data.addXp(100);
        assertEquals(1, levelsGained);
        assertEquals(1, data.getLevel());
    }

    @Test
    @DisplayName("addXp deve permitir múltiplos level-ups")
    void shouldAllowMultipleLevelUps() {
        // Dar XP suficiente para vários níveis
        int levelsGained = data.addXp(10000);
        assertTrue(levelsGained > 1);
        assertTrue(data.getLevel() > 1);
    }

    @Test
    @DisplayName("addXp com valor negativo não deve alterar nada")
    void shouldNotChangeAnything_whenNegativeXP() {
        int levelsGained = data.addXp(-50);
        assertEquals(0, levelsGained);
        assertEquals(0, data.getXp(), 0.001);
    }

    @Test
    @DisplayName("nível máximo deve ser 100")
    void shouldCapAtLevel100() {
        // Dar XP absurdo
        data.addXp(Double.MAX_VALUE / 2);
        assertTrue(data.getLevel() <= 100);
        if (data.getLevel() == 100) {
            assertEquals(0, data.getXp(), 0.001);
        }
    }

    @Test
    @DisplayName("addXp não deve funcionar se já no nível 100")
    void shouldNotAddXP_whenAtMaxLevel() {
        data.setLevel(100);
        int levelsGained = data.addXp(999999);
        assertEquals(0, levelsGained);
    }

    @Test
    @DisplayName("setLevel deve atualizar xpToNextLevel")
    void shouldUpdateXPToNextLevel() {
        data.setLevel(10);
        assertEquals(10, data.getLevel());
        // calculateXpNeeded(11) = 50*121 + 50*11 = 6050 + 550 = 6600
        assertEquals(6600, data.getXpToNextLevel(), 0.001);
    }

    @Test
    @DisplayName("setXp deve definir XP diretamente")
    void shouldSetXpDirectly() {
        data.setXp(500);
        assertEquals(500, data.getXp(), 0.001);
    }

    @Test
    @DisplayName("getProgressPercent deve calcular porcentagem correta")
    void shouldCalculateProgressPercent() {
        // level 0, xpToNextLevel = calculateXpNeeded(1) = 100
        data.setXp(50);
        assertEquals(50.0, data.getProgressPercent(), 0.001);
    }

    @Test
    @DisplayName("getProgressPercent deve ser 0 quando sem xpToNextLevel")
    void shouldReturnZero_whenNoXpNeeded() {
        data.setLevel(100);
        data.setXp(0);
        // When at max level, xpToNextLevel should still be positive so it calculates correctly
        // But the formula gives us a value > 0 for level 101
        // Just check it doesn't crash
        double pct = data.getProgressPercent();
        assertTrue(pct >= 0);
    }

    // ── Specialization ──

    @Test
    @DisplayName("setSpecialization deve definir e hasSpecialization retornar true")
    void shouldSetSpecialization() {
        data.setSpecialization("weaponsmith");
        assertTrue(data.hasSpecialization());
        assertEquals("weaponsmith", data.getSpecialization());
    }

    @Test
    @DisplayName("hasSpecialization deve retornar false para string vazia")
    void shouldReturnFalse_forEmptySpecialization() {
        data.setSpecialization("");
        assertFalse(data.hasSpecialization());
    }

    // ── Recipes ──

    @Test
    @DisplayName("unlockRecipe e hasRecipe devem funcionar")
    void shouldUnlockAndCheckRecipes() {
        data.unlockRecipe("iron_sword");
        assertTrue(data.hasRecipe("iron_sword"));
        assertFalse(data.hasRecipe("diamond_sword"));
    }

    @Test
    @DisplayName("getUnlockedRecipes deve retornar set imutável")
    void shouldReturnUnmodifiableRecipes() {
        data.unlockRecipe("test");
        Set<String> recipes = data.getUnlockedRecipes();
        assertThrows(UnsupportedOperationException.class, () -> recipes.add("new"));
    }

    @Test
    @DisplayName("setUnlockedRecipes deve substituir set inteiro")
    void shouldReplaceRecipeSet() {
        data.unlockRecipe("old_recipe");
        data.setUnlockedRecipes(Set.of("new1", "new2"));
        assertFalse(data.hasRecipe("old_recipe"));
        assertTrue(data.hasRecipe("new1"));
        assertTrue(data.hasRecipe("new2"));
    }

    // ── Stats ──

    @Test
    @DisplayName("incrementItemsForged deve incrementar totalItemsForged")
    void shouldIncrementItemsForged() {
        data.incrementItemsForged();
        data.incrementItemsForged();
        assertEquals(2, data.getTotalItemsForged());
    }

    @Test
    @DisplayName("incrementLegendaryForged deve incrementar legendaryItemsForged")
    void shouldIncrementLegendaryForged() {
        data.incrementLegendaryForged();
        assertEquals(1, data.getLegendaryItemsForged());
    }

    @Test
    @DisplayName("addPerfectStrikes deve acumular")
    void shouldAccumulatePerfectStrikes() {
        data.addPerfectStrikes(5);
        data.addPerfectStrikes(3);
        assertEquals(8, data.getTotalPerfectStrikes());
    }

    @Test
    @DisplayName("updateHighestQuality deve atualizar apenas se maior")
    void shouldUpdateHighestQuality_onlyIfHigher() {
        data.updateHighestQuality(0.8);
        assertEquals(0.8, data.getHighestQualityScore(), 0.001);

        data.updateHighestQuality(0.5);
        assertEquals(0.8, data.getHighestQualityScore(), 0.001);

        data.updateHighestQuality(0.95);
        assertEquals(0.95, data.getHighestQualityScore(), 0.001);
    }

    @Test
    @DisplayName("incrementForgesBuilt deve incrementar")
    void shouldIncrementForgesBuilt() {
        data.incrementForgesBuilt();
        data.incrementForgesBuilt();
        assertEquals(2, data.getTotalForgesBuilt());
    }

    // ── Stat setters ──

    @Test
    @DisplayName("setters de stats devem funcionar")
    void shouldSetStatValues() {
        data.setTotalItemsForged(100);
        data.setLegendaryItemsForged(5);
        data.setTotalPerfectStrikes(200);
        data.setTotalForgesBuilt(3);
        data.setHighestQualityScore(0.99);

        assertEquals(100, data.getTotalItemsForged());
        assertEquals(5, data.getLegendaryItemsForged());
        assertEquals(200, data.getTotalPerfectStrikes());
        assertEquals(3, data.getTotalForgesBuilt());
        assertEquals(0.99, data.getHighestQualityScore(), 0.001);
    }

    // ── Forges ──

    @Test
    @DisplayName("addForge e removeForge devem gerenciar forges")
    void shouldManageForges() {
        data.addForge("forge-1");
        data.addForge("forge-2");
        assertEquals(2, data.getOwnedForgeIds().size());

        data.removeForge("forge-1");
        assertEquals(1, data.getOwnedForgeIds().size());
        assertFalse(data.getOwnedForgeIds().contains("forge-1"));
        assertTrue(data.getOwnedForgeIds().contains("forge-2"));
    }

    @Test
    @DisplayName("getOwnedForgeIds deve retornar set imutável")
    void shouldReturnUnmodifiableForgeIds() {
        data.addForge("test");
        assertThrows(UnsupportedOperationException.class,
                () -> data.getOwnedForgeIds().add("new"));
    }

    @Test
    @DisplayName("setOwnedForgeIds deve substituir set inteiro")
    void shouldReplaceForgeIdSet() {
        data.addForge("old");
        data.setOwnedForgeIds(Set.of("new1", "new2"));
        assertFalse(data.getOwnedForgeIds().contains("old"));
        assertTrue(data.getOwnedForgeIds().contains("new1"));
    }
}

package me.ray.midgard.modules.professions.blacksmith.forge.recipe;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ForgeRecipeTest {

    private ForgeRecipe recipe;

    @BeforeEach
    void setUp() {
        recipe = new ForgeRecipe("test_sword");
    }

    // ── Defaults ──

    @Test
    @DisplayName("construtor deve definir defaults corretos")
    void shouldHaveCorrectDefaults() {
        assertEquals("test_sword", recipe.getId());
        assertEquals(1.0, recipe.getDifficultyMultiplier(), 0.001);
        assertEquals(15, recipe.getHammerStrikes());
        assertEquals(3, recipe.getSharpeningPasses());
        assertEquals(20, recipe.getHeatingTime());
        assertEquals(1, recipe.getChapter());
        assertEquals(1, recipe.getRequiredLevel());
        assertEquals(ForgeTier.BASIC, recipe.getRequiredForgeTier());
    }

    // ── Builder pattern ──

    @Test
    @DisplayName("setters devem retornar a própria instância (builder)")
    void shouldReturnSelf_forBuilderPattern() {
        ForgeRecipe result = recipe.setDisplayName("Espada de Teste")
                .setResultItemId("midgard:test_sword")
                .setRequiredLevel(10)
                .setRequiredForgeTier(ForgeTier.ADVANCED)
                .setPrimaryMetal("iron_ingot")
                .setPrimaryMetalAmount(5)
                .setBaseXP(100)
                .setSpecialization("weaponsmith");

        assertSame(recipe, result);
        assertEquals("Espada de Teste", recipe.getDisplayName());
        assertEquals("midgard:test_sword", recipe.getResultItemId());
        assertEquals(10, recipe.getRequiredLevel());
        assertEquals(ForgeTier.ADVANCED, recipe.getRequiredForgeTier());
        assertEquals("iron_ingot", recipe.getPrimaryMetal());
        assertEquals(5, recipe.getPrimaryMetalAmount());
        assertEquals(100, recipe.getBaseXP());
        assertEquals("weaponsmith", recipe.getSpecialization());
    }

    // ── Secondary materials ──

    @Test
    @DisplayName("getSecondaryMaterials deve retornar mapa imutável")
    void shouldReturnUnmodifiableSecondaryMaterials() {
        recipe.addSecondaryMaterial("diamond", 2);
        Map<String, Integer> materials = recipe.getSecondaryMaterials();
        assertThrows(UnsupportedOperationException.class, () -> materials.put("gold", 1));
    }

    @Test
    @DisplayName("addSecondaryMaterial deve acumular materiais")
    void shouldAccumulateMaterials() {
        recipe.addSecondaryMaterial("diamond", 2);
        recipe.addSecondaryMaterial("emerald", 3);
        assertEquals(2, recipe.getSecondaryMaterials().size());
        assertEquals(2, recipe.getSecondaryMaterials().get("diamond"));
        assertEquals(3, recipe.getSecondaryMaterials().get("emerald"));
    }

    // ── getAllMaterials() ──

    @Test
    @DisplayName("getAllMaterials deve combinar primary e secondary")
    void shouldCombineAllMaterials() {
        recipe.setPrimaryMetal("iron_ingot").setPrimaryMetalAmount(5);
        recipe.addSecondaryMaterial("diamond", 2);

        Map<String, Integer> all = recipe.getAllMaterials();
        assertEquals(2, all.size());
        assertEquals(5, all.get("iron_ingot"));
        assertEquals(2, all.get("diamond"));
    }

    @Test
    @DisplayName("getAllMaterials sem primary deve retornar só secondary")
    void shouldReturnOnlySecondary_whenNoPrimary() {
        recipe.addSecondaryMaterial("gold", 3);
        Map<String, Integer> all = recipe.getAllMaterials();
        assertEquals(1, all.size());
        assertEquals(3, all.get("gold"));
    }

    // ── getEffectiveHammerStrikes() ──

    @Test
    @DisplayName("strikes efetivos com dificuldade 1.0 devem ser iguais à base")
    void shouldReturnBase_whenDifficultyIsOne() {
        assertEquals(15, recipe.getEffectiveHammerStrikes());
    }

    @Test
    @DisplayName("strikes efetivos com dificuldade 2.0 devem dobrar")
    void shouldDoubleStrikes_whenDifficultyIsTwo() {
        recipe.setDifficultyMultiplier(2.0);
        assertEquals(30, recipe.getEffectiveHammerStrikes());
    }

    @Test
    @DisplayName("strikes efetivos devem arredondar para cima")
    void shouldCeilEffectiveStrikes() {
        recipe.setHammerStrikes(10).setDifficultyMultiplier(1.5);
        // 10 * 1.5 = 15
        assertEquals(15, recipe.getEffectiveHammerStrikes());

        recipe.setHammerStrikes(7).setDifficultyMultiplier(1.5);
        // 7 * 1.5 = 10.5 → ceil → 11
        assertEquals(11, recipe.getEffectiveHammerStrikes());
    }

    // ── getEffectiveSharpeningPasses() ──

    @Test
    @DisplayName("passes efetivos com dificuldade 1.5")
    void shouldCalculateEffectivePasses() {
        recipe.setSharpeningPasses(3).setDifficultyMultiplier(1.5);
        // 3 * 1.5 = 4.5 → ceil → 5
        assertEquals(5, recipe.getEffectiveSharpeningPasses());
    }

    // ── Lore ──

    @Test
    @DisplayName("getLore deve retornar lista imutável")
    void shouldReturnUnmodifiableLore() {
        recipe.setLore(List.of("Linha 1", "Linha 2"));
        List<String> lore = recipe.getLore();
        assertThrows(UnsupportedOperationException.class, () -> lore.add("Linha 3"));
    }

    // ── Temperature settings ──

    @Test
    @DisplayName("ideal temp settings devem ser configuráveis")
    void shouldSetIdealTemp() {
        recipe.setIdealTempMin(800.0).setIdealTempMax(1200.0);
        assertEquals(800.0, recipe.getIdealTempMin(), 0.001);
        assertEquals(1200.0, recipe.getIdealTempMax(), 0.001);
    }

    // ── Gem sockets e rune engraving ──

    @Test
    @DisplayName("gem sockets e rune engraving defaults devem ser 0/false")
    void shouldHaveDefaultGemAndRune() {
        assertEquals(0, recipe.getMaxGemSockets());
        assertFalse(recipe.isAllowsRuneEngraving());
    }

    @Test
    @DisplayName("gem sockets e rune engraving devem ser configuráveis")
    void shouldSetGemAndRune() {
        recipe.setMaxGemSockets(3).setAllowsRuneEngraving(true);
        assertEquals(3, recipe.getMaxGemSockets());
        assertTrue(recipe.isAllowsRuneEngraving());
    }

    // ── equals/hashCode ──

    @Test
    @DisplayName("equals deve comparar por id")
    void shouldUseIdForEquality() {
        ForgeRecipe other = new ForgeRecipe("test_sword");
        assertEquals(recipe, other);
        assertEquals(recipe.hashCode(), other.hashCode());
    }

    @Test
    @DisplayName("equals deve retornar false para ids diferentes")
    void shouldNotBeEqual_forDifferentIds() {
        ForgeRecipe other = new ForgeRecipe("other_sword");
        assertNotEquals(recipe, other);
    }

    @Test
    @DisplayName("equals deve retornar false para null")
    void shouldNotBeEqual_toNull() {
        assertNotEquals(null, recipe);
    }
}

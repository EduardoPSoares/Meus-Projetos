package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class SmelteryTierAndBlockTypeTest {

    // ═══ SmelteryTier Tests ═══

    @Test
    @DisplayName("Deve ter exatamente 5 tiers")
    void shouldHaveFiveTiers() {
        assertEquals(5, SmelteryTier.values().length);
    }

    @ParameterizedTest
    @CsvSource({
            "1, SMALL",
            "2, MEDIUM",
            "3, LARGE",
            "4, MASTER",
            "5, LEGENDARY"
    })
    @DisplayName("fromLevel deve retornar tier correto")
    void shouldReturnCorrectTier(int level, String expectedName) {
        SmelteryTier tier = SmelteryTier.fromLevel(level);
        assertNotNull(tier);
        assertEquals(expectedName, tier.name());
        assertEquals(level, tier.getLevel());
    }

    @Test
    @DisplayName("fromLevel com valor inválido deve retornar null")
    void shouldReturnNull_forInvalidLevel() {
        assertNull(SmelteryTier.fromLevel(0));
        assertNull(SmelteryTier.fromLevel(6));
        assertNull(SmelteryTier.fromLevel(-1));
    }

    @Test
    @DisplayName("SMALL deve ter dimensões interiores 3x3x3")
    void shouldHaveCorrectSmallDimensions() {
        SmelteryTier tier = SmelteryTier.SMALL;
        assertEquals(3, tier.getInteriorWidth());
        assertEquals(3, tier.getInteriorHeight());
        assertEquals(3, tier.getInteriorDepth());
    }

    @Test
    @DisplayName("getTotalWidth/Height/Depth = interior + 2")
    void shouldCalculateTotalDimensions() {
        SmelteryTier tier = SmelteryTier.SMALL;
        assertEquals(tier.getInteriorWidth() + 2, tier.getTotalWidth());
        assertEquals(tier.getInteriorHeight() + 2, tier.getTotalHeight());
        assertEquals(tier.getInteriorDepth() + 2, tier.getTotalDepth());
    }

    @ParameterizedTest
    @EnumSource(SmelteryTier.class)
    @DisplayName("Total dimensions = interior + 2 para todos os tiers")
    void shouldAddTwoForAllTiers(SmelteryTier tier) {
        assertEquals(tier.getInteriorWidth() + 2, tier.getTotalWidth());
        assertEquals(tier.getInteriorHeight() + 2, tier.getTotalHeight());
        assertEquals(tier.getInteriorDepth() + 2, tier.getTotalDepth());
    }

    @Test
    @DisplayName("tankCapacity deve crescer com o tier")
    void shouldHaveIncreasingTankCapacity() {
        SmelteryTier[] tiers = SmelteryTier.values();
        for (int i = 1; i < tiers.length; i++) {
            assertTrue(tiers[i].getTankCapacity() > tiers[i - 1].getTankCapacity(),
                    tiers[i].name() + " deve ter mais capacidade que " + tiers[i - 1].name());
        }
    }

    @Test
    @DisplayName("maxTemperature deve crescer com o tier")
    void shouldHaveIncreasingMaxTemp() {
        SmelteryTier[] tiers = SmelteryTier.values();
        for (int i = 1; i < tiers.length; i++) {
            assertTrue(tiers[i].getMaxTemperature() > tiers[i - 1].getMaxTemperature(),
                    tiers[i].name() + " deve ter mais temp que " + tiers[i - 1].name());
        }
    }

    @Test
    @DisplayName("requiredProfessionLevel deve crescer com o tier")
    void shouldHaveIncreasingRequiredLevel() {
        SmelteryTier[] tiers = SmelteryTier.values();
        for (int i = 1; i < tiers.length; i++) {
            assertTrue(tiers[i].getRequiredProfessionLevel() >= tiers[i - 1].getRequiredProfessionLevel(),
                    tiers[i].name() + " deve exigir level >= que " + tiers[i - 1].name());
        }
    }

    @Test
    @DisplayName("SMALL deve exigir nível 0 de profissão")
    void shouldRequireLevel0_forSmall() {
        assertEquals(0, SmelteryTier.SMALL.getRequiredProfessionLevel());
    }

    @Test
    @DisplayName("LEGENDARY: level 5, maxTemp 3500, maxDrains 6, reqLevel 85")
    void shouldHaveCorrectLegendaryValues() {
        SmelteryTier tier = SmelteryTier.LEGENDARY;
        assertEquals(5, tier.getLevel());
        assertEquals(3500, tier.getMaxTemperature());
        assertEquals(6, tier.getMaxDrains());
        assertEquals(85, tier.getRequiredProfessionLevel());
    }

    @Test
    @DisplayName("maxDrains deve crescer com o tier")
    void shouldHaveIncreasingMaxDrains() {
        SmelteryTier[] tiers = SmelteryTier.values();
        for (int i = 1; i < tiers.length; i++) {
            assertTrue(tiers[i].getMaxDrains() >= tiers[i - 1].getMaxDrains(),
                    tiers[i].name() + " deve ter maxDrains >= que " + tiers[i - 1].name());
        }
    }

    // ═══ SmelteryBlockType Tests ═══

    @Test
    @DisplayName("Deve ter exatamente 9 tipos de bloco")
    void shouldHaveNineBlockTypes() {
        assertEquals(9, SmelteryBlockType.values().length);
    }

    @Test
    @DisplayName("Blocos interativos: CONTROLLER, DRAIN, ITEM_INPUT, FUEL_INPUT, CASTING_TABLE, CASTING_BASIN")
    void shouldIdentifyInteractiveBlocks() {
        assertTrue(SmelteryBlockType.CONTROLLER.isInteractive());
        assertTrue(SmelteryBlockType.DRAIN.isInteractive());
        assertTrue(SmelteryBlockType.ITEM_INPUT.isInteractive());
        assertTrue(SmelteryBlockType.FUEL_INPUT.isInteractive());
        assertTrue(SmelteryBlockType.CASTING_TABLE.isInteractive());
        assertTrue(SmelteryBlockType.CASTING_BASIN.isInteractive());
    }

    @Test
    @DisplayName("Blocos não interativos: WALL, TANK_WINDOW, AIR")
    void shouldIdentifyNonInteractiveBlocks() {
        assertFalse(SmelteryBlockType.WALL.isInteractive());
        assertFalse(SmelteryBlockType.TANK_WINDOW.isInteractive());
        assertFalse(SmelteryBlockType.AIR.isInteractive());
    }

    @Test
    @DisplayName("WALL deve ter material NETHER_BRICKS")
    void shouldHaveNetherBricks_forWall() {
        assertEquals(Material.NETHER_BRICKS, SmelteryBlockType.WALL.getDefaultMaterial());
    }

    @Test
    @DisplayName("AIR deve ter material AIR")
    void shouldHaveAir_forAir() {
        assertEquals(Material.AIR, SmelteryBlockType.AIR.getDefaultMaterial());
    }

    @Test
    @DisplayName("CONTROLLER deve ter material BLAST_FURNACE")
    void shouldHaveBlastFurnace_forController() {
        assertEquals(Material.BLAST_FURNACE, SmelteryBlockType.CONTROLLER.getDefaultMaterial());
    }

    @ParameterizedTest
    @EnumSource(SmelteryBlockType.class)
    @DisplayName("Todos devem ter defaultMaterial não-null")
    void shouldHaveDefaultMaterial(SmelteryBlockType type) {
        assertNotNull(type.getDefaultMaterial());
    }

    // ═══ SmeltingRecipe Tests ═══

    @Test
    @DisplayName("SmeltingRecipe: deve armazenar dados corretamente")
    void shouldStoreSmeltingRecipeData() {
        SmeltingRecipe recipe = new SmeltingRecipe("iron_smelt",
                Material.IRON_INGOT, MoltenMetal.IRON, 144, 200, 800);

        assertEquals("iron_smelt", recipe.getId());
        assertEquals(Material.IRON_INGOT, recipe.getInput());
        assertEquals(MoltenMetal.IRON, recipe.getOutput());
        assertEquals(144, recipe.getOutputAmount());
        assertEquals(200, recipe.getSmeltTime());
        assertEquals(800, recipe.getMinTemperature());
    }
}

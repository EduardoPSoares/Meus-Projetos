package me.ray.midgard.modules.professions.blacksmith.forge.structure;

import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock.ForgeBlockType;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class ForgeBlockTest {

    // ── Constructor e getters ──

    @Test
    @DisplayName("deve armazenar coordenadas relativas e material")
    void shouldStoreRelativeCoords() {
        ForgeBlock block = new ForgeBlock(2, 1, 3, Material.STONE_BRICKS, ForgeBlockType.STRUCTURE);
        assertEquals(2, block.getRelX());
        assertEquals(1, block.getRelY());
        assertEquals(3, block.getRelZ());
        assertEquals(Material.STONE_BRICKS, block.getMaterial());
        assertEquals(ForgeBlockType.STRUCTURE, block.getBlockType());
    }

    // ── toVector() ──

    @Test
    @DisplayName("toVector deve retornar Vector com coordenadas relativas")
    void shouldReturnCorrectVector() {
        ForgeBlock block = new ForgeBlock(5, 2, 7, Material.IRON_BLOCK, ForgeBlockType.STRUCTURE);
        var vec = block.toVector();
        assertEquals(5, vec.getBlockX());
        assertEquals(2, vec.getBlockY());
        assertEquals(7, vec.getBlockZ());
    }

    // ── isAir() ──

    @Test
    @DisplayName("isAir deve retornar true para Material.AIR")
    void shouldBeAir_whenMaterialIsAir() {
        ForgeBlock block = new ForgeBlock(0, 0, 0, Material.AIR, ForgeBlockType.AIR);
        assertTrue(block.isAir());
    }

    @Test
    @DisplayName("isAir deve retornar true para null material")
    void shouldBeAir_whenMaterialIsNull() {
        ForgeBlock block = new ForgeBlock(0, 0, 0, null, ForgeBlockType.AIR);
        assertTrue(block.isAir());
    }

    @Test
    @DisplayName("isAir deve retornar false para bloco sólido")
    void shouldNotBeAir_whenSolidBlock() {
        ForgeBlock block = new ForgeBlock(0, 0, 0, Material.STONE_BRICKS, ForgeBlockType.STRUCTURE);
        assertFalse(block.isAir());
    }

    // ── isInteractive() ──

    @Test
    @DisplayName("FURNACE deve ser interativo")
    void shouldBeInteractive_forFurnace() {
        ForgeBlock block = new ForgeBlock(0, 0, 0, Material.FURNACE, ForgeBlockType.FURNACE);
        assertTrue(block.isInteractive());
    }

    @Test
    @DisplayName("ANVIL deve ser interativo")
    void shouldBeInteractive_forAnvil() {
        ForgeBlock block = new ForgeBlock(0, 0, 0, Material.ANVIL, ForgeBlockType.ANVIL);
        assertTrue(block.isInteractive());
    }

    @Test
    @DisplayName("STRUCTURE não deve ser interativo")
    void shouldNotBeInteractive_forStructure() {
        ForgeBlock block = new ForgeBlock(0, 0, 0, Material.STONE_BRICKS, ForgeBlockType.STRUCTURE);
        assertFalse(block.isInteractive());
    }

    @Test
    @DisplayName("AIR não deve ser interativo")
    void shouldNotBeInteractive_forAir() {
        ForgeBlock block = new ForgeBlock(0, 0, 0, Material.AIR, ForgeBlockType.AIR);
        assertFalse(block.isInteractive());
    }

    @Test
    @DisplayName("FUEL_ZONE não deve ser interativo")
    void shouldNotBeInteractive_forFuelZone() {
        ForgeBlock block = new ForgeBlock(0, 0, 0, Material.AIR, ForgeBlockType.FUEL_ZONE);
        assertFalse(block.isInteractive());
    }

    @Test
    @DisplayName("CAULDRON deve ser interativo")
    void shouldBeInteractive_forCauldron() {
        ForgeBlock block = new ForgeBlock(0, 0, 0, Material.CAULDRON, ForgeBlockType.CAULDRON);
        assertTrue(block.isInteractive());
    }

    @Test
    @DisplayName("GRINDSTONE deve ser interativo")
    void shouldBeInteractive_forGrindstone() {
        ForgeBlock block = new ForgeBlock(0, 0, 0, Material.GRINDSTONE, ForgeBlockType.GRINDSTONE);
        assertTrue(block.isInteractive());
    }

    @Test
    @DisplayName("SMITHING_TABLE deve ser interativo")
    void shouldBeInteractive_forSmithingTable() {
        ForgeBlock block = new ForgeBlock(0, 0, 0, Material.SMITHING_TABLE, ForgeBlockType.SMITHING_TABLE);
        assertTrue(block.isInteractive());
    }

    // ── isFuelZone() ──

    @Test
    @DisplayName("isFuelZone deve retornar true para FUEL_ZONE")
    void shouldBeFuelZone_whenFuelZoneType() {
        ForgeBlock block = new ForgeBlock(0, 0, 0, Material.AIR, ForgeBlockType.FUEL_ZONE);
        assertTrue(block.isFuelZone());
    }

    @Test
    @DisplayName("isFuelZone deve retornar false para outros tipos")
    void shouldNotBeFuelZone_forOtherTypes() {
        ForgeBlock block = new ForgeBlock(0, 0, 0, Material.STONE_BRICKS, ForgeBlockType.STRUCTURE);
        assertFalse(block.isFuelZone());
    }

    // ── ForgeBlockType enum ──

    @Test
    @DisplayName("ForgeBlockType deve ter 11 tipos")
    void shouldHave11BlockTypes() {
        assertEquals(11, ForgeBlockType.values().length);
    }
}

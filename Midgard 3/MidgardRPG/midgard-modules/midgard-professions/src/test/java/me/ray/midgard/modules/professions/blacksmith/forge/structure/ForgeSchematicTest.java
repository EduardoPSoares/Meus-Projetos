package me.ray.midgard.modules.professions.blacksmith.forge.structure;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock.ForgeBlockType;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ForgeSchematicTest {

    private ForgeSchematic schematic;

    @BeforeEach
    void setUp() {
        List<ForgeBlock> blocks = List.of(
                new ForgeBlock(0, 0, 0, Material.STONE_BRICKS, ForgeBlockType.STRUCTURE),
                new ForgeBlock(1, 0, 0, Material.STONE_BRICKS, ForgeBlockType.STRUCTURE),
                new ForgeBlock(2, 1, 2, Material.FURNACE, ForgeBlockType.FURNACE),
                new ForgeBlock(3, 1, 2, Material.ANVIL, ForgeBlockType.ANVIL),
                new ForgeBlock(1, 1, 1, Material.AIR, ForgeBlockType.AIR),
                new ForgeBlock(2, 0, 1, Material.AIR, ForgeBlockType.FUEL_ZONE)
        );

        schematic = new ForgeSchematic(ForgeTier.BASIC, 5, 4, 5, 0, 0, 0, blocks);
    }

    // ── Estrutura básica ──

    @Test
    @DisplayName("deve retornar tier correto")
    void shouldReturnCorrectTier() {
        assertEquals(ForgeTier.BASIC, schematic.getTier());
    }

    @Test
    @DisplayName("deve retornar dimensões corretas")
    void shouldReturnCorrectDimensions() {
        assertEquals(5, schematic.getWidth());
        assertEquals(4, schematic.getHeight());
        assertEquals(5, schematic.getDepth());
    }

    @Test
    @DisplayName("blocks deve ser lista imutável")
    void shouldReturnUnmodifiableBlocks() {
        assertThrows(UnsupportedOperationException.class, () ->
                schematic.getBlocks().add(new ForgeBlock(0, 0, 0, Material.STONE, ForgeBlockType.STRUCTURE)));
    }

    // ── getSolidBlocks() ──

    @Test
    @DisplayName("getSolidBlocks deve excluir AIR e FUEL_ZONE")
    void shouldExcludeAirAndFuelZone() {
        List<ForgeBlock> solid = schematic.getSolidBlocks();
        // STONE_BRICKS x2, FURNACE, ANVIL = 4 blocos sólidos
        assertEquals(4, solid.size());
        for (ForgeBlock block : solid) {
            assertFalse(block.isAir());
            assertFalse(block.isFuelZone());
        }
    }

    // ── getFuelZoneBlocks() ──

    @Test
    @DisplayName("getFuelZoneBlocks deve retornar apenas blocos FUEL_ZONE")
    void shouldReturnOnlyFuelZoneBlocks() {
        List<ForgeBlock> fuelZones = schematic.getFuelZoneBlocks();
        assertEquals(1, fuelZones.size());
        assertTrue(fuelZones.get(0).isFuelZone());
    }

    // ── getInteractiveBlock() ──

    @Test
    @DisplayName("deve encontrar bloco interativo FURNACE")
    void shouldFindInteractiveFurnace() {
        ForgeBlock furnace = schematic.getInteractiveBlock(ForgeBlockType.FURNACE);
        assertNotNull(furnace);
        assertEquals(Material.FURNACE, furnace.getMaterial());
    }

    @Test
    @DisplayName("deve encontrar bloco interativo ANVIL")
    void shouldFindInteractiveAnvil() {
        ForgeBlock anvil = schematic.getInteractiveBlock(ForgeBlockType.ANVIL);
        assertNotNull(anvil);
        assertEquals(Material.ANVIL, anvil.getMaterial());
    }

    @Test
    @DisplayName("deve retornar null para tipo interativo inexistente")
    void shouldReturnNull_forMissingInteractiveType() {
        assertNull(schematic.getInteractiveBlock(ForgeBlockType.CAULDRON));
    }

    // ── getInteractiveBlocks() ──

    @Test
    @DisplayName("getInteractiveBlocks deve ser imutável")
    void shouldReturnUnmodifiableInteractiveBlocks() {
        var interactive = schematic.getInteractiveBlocks();
        assertThrows(UnsupportedOperationException.class, () ->
                interactive.put(ForgeBlockType.CAULDRON, new ForgeBlock(0, 0, 0, Material.CAULDRON, ForgeBlockType.CAULDRON)));
    }

    @Test
    @DisplayName("getInteractiveBlocks deve conter FURNACE e ANVIL")
    void shouldContainFurnaceAndAnvil() {
        var interactive = schematic.getInteractiveBlocks();
        assertEquals(2, interactive.size());
        assertTrue(interactive.containsKey(ForgeBlockType.FURNACE));
        assertTrue(interactive.containsKey(ForgeBlockType.ANVIL));
    }

    // ── ValidationResult ──

    @Test
    @DisplayName("ValidationResult: isComplete deve ser true quando sem missing/wrong")
    void shouldBeComplete_whenNoMissingOrWrong() {
        var result = new ForgeSchematic.ValidationResult(
                List.of(new ForgeBlock(0, 0, 0, Material.STONE, ForgeBlockType.STRUCTURE)),
                List.of(),
                List.of()
        );
        assertTrue(result.isComplete());
    }

    @Test
    @DisplayName("ValidationResult: isComplete deve ser false quando há missing")
    void shouldNotBeComplete_whenMissingBlocks() {
        var result = new ForgeSchematic.ValidationResult(
                List.of(),
                List.of(new ForgeBlock(0, 0, 0, Material.STONE, ForgeBlockType.STRUCTURE)),
                List.of()
        );
        assertFalse(result.isComplete());
    }

    @Test
    @DisplayName("ValidationResult: totalRequired deve somar correct + missing + wrong")
    void shouldCalculateTotalRequired() {
        var correct = new ForgeBlock(0, 0, 0, Material.STONE, ForgeBlockType.STRUCTURE);
        var missing = new ForgeBlock(1, 0, 0, Material.STONE, ForgeBlockType.STRUCTURE);
        var wrong = new ForgeBlock(2, 0, 0, Material.STONE, ForgeBlockType.STRUCTURE);

        var result = new ForgeSchematic.ValidationResult(
                List.of(correct), List.of(missing), List.of(wrong)
        );
        assertEquals(3, result.totalRequired());
    }

    @Test
    @DisplayName("ValidationResult: completionPercent com 2 de 4 corretos deve ser 0.5")
    void shouldCalculateCompletionPercent() {
        var b = new ForgeBlock(0, 0, 0, Material.STONE, ForgeBlockType.STRUCTURE);
        var result = new ForgeSchematic.ValidationResult(
                List.of(b, b), List.of(b), List.of(b)
        );
        assertEquals(0.5, result.completionPercent(), 0.001);
    }

    @Test
    @DisplayName("ValidationResult: completionPercent com 0 total deve ser 1.0")
    void shouldReturn1_whenNoBlocksRequired() {
        var result = new ForgeSchematic.ValidationResult(List.of(), List.of(), List.of());
        assertEquals(1.0, result.completionPercent(), 0.001);
    }

    // ── createBasicForge() ──

    @Test
    @DisplayName("createBasicForge deve retornar schematic com tier BASIC")
    void shouldCreateBasicForge_withCorrectTier() {
        ForgeSchematic basic = ForgeSchematic.createBasicForge();
        assertNotNull(basic);
        assertEquals(ForgeTier.BASIC, basic.getTier());
    }

    @Test
    @DisplayName("createBasicForge deve ser cached (mesma instância)")
    void shouldReturnCachedInstance() {
        ForgeSchematic first = ForgeSchematic.createBasicForge();
        ForgeSchematic second = ForgeSchematic.createBasicForge();
        assertSame(first, second);
    }

    @Test
    @DisplayName("basic forge deve ter blocos sólidos")
    void shouldHaveSolidBlocks() {
        ForgeSchematic basic = ForgeSchematic.createBasicForge();
        assertFalse(basic.getSolidBlocks().isEmpty());
    }
}

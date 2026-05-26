package me.ray.midgard.modules.professions.blacksmith.forge.structure;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeRotation;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ForgeTemplateAndStructureTest {

    // ═══ ForgeTemplate Tests ═══

    @Test
    @DisplayName("ForgeTemplate: construtor simples deve definir defaults")
    void templateShouldSetDefaults() {
        UUID id = UUID.randomUUID();
        ForgeTemplate template = new ForgeTemplate(id, "Forja Básica", ForgeTier.BASIC, 1);

        assertEquals(id, template.getTemplateId());
        assertEquals("Forja Básica", template.getName());
        assertEquals(ForgeTier.BASIC, template.getTier());
        assertEquals(1, template.getRequiredLevel());
        assertTrue(template.isActive());
        assertTrue(template.getCreatedAt() > 0);
        assertNull(template.getSchematic());
    }

    @Test
    @DisplayName("ForgeTemplate: full constructor para carregamento DB")
    void templateFullConstructor() {
        UUID id = UUID.randomUUID();
        long ts = 1700000000000L;
        ForgeTemplate template = new ForgeTemplate(id, "Lendária", ForgeTier.LEGENDARY, 95, ts, false);

        assertEquals(id, template.getTemplateId());
        assertEquals("Lendária", template.getName());
        assertEquals(ForgeTier.LEGENDARY, template.getTier());
        assertEquals(95, template.getRequiredLevel());
        assertEquals(ts, template.getCreatedAt());
        assertFalse(template.isActive());
    }

    @Test
    @DisplayName("ForgeTemplate: setters devem atualizar valores")
    void templateSettersShouldWork() {
        ForgeTemplate template = new ForgeTemplate(UUID.randomUUID(), "Old", ForgeTier.BASIC, 1);

        template.setName("New");
        assertEquals("New", template.getName());

        template.setTier(ForgeTier.ADVANCED);
        assertEquals(ForgeTier.ADVANCED, template.getTier());

        template.setRequiredLevel(50);
        assertEquals(50, template.getRequiredLevel());

        template.setActive(false);
        assertFalse(template.isActive());
    }

    // ═══ ForgeStructure Tests ═══

    @Test
    @DisplayName("ForgeStructure: construtor simples deve inicializar corretamente")
    void structureShouldInitialize() {
        UUID forgeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ForgeStructure structure = new ForgeStructure(forgeId, ownerId, "world",
                100, 64, 200, ForgeTier.BASIC, ForgeRotation.NORTH);

        assertEquals(forgeId, structure.getForgeId());
        assertEquals(ownerId, structure.getOwnerUuid());
        assertEquals("world", structure.getWorldName());
        assertEquals(100, structure.getX());
        assertEquals(64, structure.getY());
        assertEquals(200, structure.getZ());
        assertEquals(ForgeTier.BASIC, structure.getTier());
        assertEquals(ForgeRotation.NORTH, structure.getRotation());
        assertTrue(structure.isActive());
        assertEquals(0, structure.getTotalItemsForged());
    }

    @Test
    @DisplayName("ForgeStructure: incrementItemsForged deve incrementar")
    void structureShouldIncrementItems() {
        ForgeStructure structure = new ForgeStructure(UUID.randomUUID(), UUID.randomUUID(),
                "world", 0, 0, 0, ForgeTier.BASIC, ForgeRotation.NORTH);

        assertEquals(0, structure.getTotalItemsForged());
        structure.incrementItemsForged();
        assertEquals(1, structure.getTotalItemsForged());
        structure.incrementItemsForged();
        assertEquals(2, structure.getTotalItemsForged());
    }

    @Test
    @DisplayName("ForgeStructure: setters devem funcionar")
    void structureSettersShouldWork() {
        ForgeStructure structure = new ForgeStructure(UUID.randomUUID(), UUID.randomUUID(),
                "world", 0, 0, 0, ForgeTier.BASIC, ForgeRotation.NORTH);

        structure.setActive(false);
        assertFalse(structure.isActive());

        structure.setName("Minha Forja");
        assertEquals("Minha Forja", structure.getName());

        long ts = System.currentTimeMillis();
        structure.setLastUsed(ts);
        assertEquals(ts, structure.getLastUsed());
    }

    @Test
    @DisplayName("ForgeStructure: full constructor para carregamento DB")
    void structureFullConstructor() {
        UUID forgeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        long created = 1700000000000L;
        long used = 1700000100000L;

        ForgeStructure structure = new ForgeStructure(forgeId, ownerId, "nether",
                50, 30, 70, ForgeTier.MASTER, ForgeRotation.WEST,
                created, used, 42, false, "Forja do Mestre");

        assertEquals(forgeId, structure.getForgeId());
        assertEquals(ownerId, structure.getOwnerUuid());
        assertEquals("nether", structure.getWorldName());
        assertEquals(50, structure.getX());
        assertEquals(30, structure.getY());
        assertEquals(70, structure.getZ());
        assertEquals(ForgeTier.MASTER, structure.getTier());
        assertEquals(ForgeRotation.WEST, structure.getRotation());
        assertEquals(created, structure.getCreatedAt());
        assertEquals(used, structure.getLastUsed());
        assertEquals(42, structure.getTotalItemsForged());
        assertFalse(structure.isActive());
        assertEquals("Forja do Mestre", structure.getName());
    }

    @Test
    @DisplayName("ForgeStructure: equals/hashCode baseado em forgeId")
    void structureEqualsByForgeId() {
        UUID forgeId = UUID.randomUUID();

        ForgeStructure s1 = new ForgeStructure(forgeId, UUID.randomUUID(),
                "world", 100, 64, 200, ForgeTier.BASIC, ForgeRotation.NORTH);
        ForgeStructure s2 = new ForgeStructure(forgeId, UUID.randomUUID(),
                "nether", 0, 0, 0, ForgeTier.LEGENDARY, ForgeRotation.SOUTH);

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    @DisplayName("ForgeStructure: forjas diferentes não são iguais")
    void structureShouldNotEqual_differentIds() {
        ForgeStructure s1 = new ForgeStructure(UUID.randomUUID(), UUID.randomUUID(),
                "world", 0, 0, 0, ForgeTier.BASIC, ForgeRotation.NORTH);
        ForgeStructure s2 = new ForgeStructure(UUID.randomUUID(), UUID.randomUUID(),
                "world", 0, 0, 0, ForgeTier.BASIC, ForgeRotation.NORTH);

        assertNotEquals(s1, s2);
    }

    @Test
    @DisplayName("ForgeStructure: getFuelZoneLocations retorna lista vazia se não inicializada")
    void structureFuelZoneDefaults() {
        ForgeStructure structure = new ForgeStructure(UUID.randomUUID(), UUID.randomUUID(),
                "world", 0, 0, 0, ForgeTier.BASIC, ForgeRotation.NORTH);
        assertTrue(structure.getFuelZoneLocations().isEmpty());
    }

    @Test
    @DisplayName("ForgeStructure: getInteractiveLocations retorna null se não inicializada")
    void structureInteractiveLocationsDefaults() {
        ForgeStructure structure = new ForgeStructure(UUID.randomUUID(), UUID.randomUUID(),
                "world", 0, 0, 0, ForgeTier.BASIC, ForgeRotation.NORTH);
        assertNull(structure.getInteractiveLocations());
    }

    @Test
    @DisplayName("ForgeStructure: full constructor com nome null usa tier name")
    void structureNullNameUsesTierName() {
        ForgeStructure structure = new ForgeStructure(UUID.randomUUID(), UUID.randomUUID(),
                "world", 0, 0, 0, ForgeTier.BASIC, ForgeRotation.NORTH,
                0L, 0L, 0, true, null);
        // tier.getName() depends on ProfessionsModule.getInstance(), but the fallback is tier name
        assertNotNull(structure.getName());
    }
}

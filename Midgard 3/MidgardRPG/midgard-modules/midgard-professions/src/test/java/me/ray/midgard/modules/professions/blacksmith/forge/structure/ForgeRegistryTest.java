package me.ray.midgard.modules.professions.blacksmith.forge.structure;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeRotation;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ForgeRegistryTest {

    private ForgeRegistry registry;
    private UUID ownerId;
    private ForgeStructure forge1;
    private ForgeStructure forge2;
    private ForgeStructure forge3;

    @BeforeEach
    void setUp() {
        registry = new ForgeRegistry();
        ownerId = UUID.randomUUID();

        // forge1: owner=ownerId, BASIC tier, coords (100, 64, 200)
        forge1 = new ForgeStructure(UUID.randomUUID(), ownerId, "world",
                100, 64, 200, ForgeTier.BASIC, ForgeRotation.NORTH);

        // forge2: owner=ownerId, ADVANCED tier, coords (200, 64, 300), mesmo chunk que forge1? chunk(100>>4=6, 200>>4=12) vs (200>>4=12, 300>>4=18) → chunks diferentes
        forge2 = new ForgeStructure(UUID.randomUUID(), ownerId, "world",
                200, 64, 300, ForgeTier.ADVANCED, ForgeRotation.SOUTH);

        // forge3: outro owner
        forge3 = new ForgeStructure(UUID.randomUUID(), UUID.randomUUID(), "world",
                100, 64, 200, ForgeTier.BASIC, ForgeRotation.EAST);
    }

    @Test
    @DisplayName("register: forge deve ser recuperável por ID")
    void shouldRetrieveForgeById() {
        registry.register(forge1);
        assertSame(forge1, registry.getById(forge1.getForgeId()));
    }

    @Test
    @DisplayName("getById: retornar null para ID inexistente")
    void shouldReturnNull_forUnknownId() {
        assertNull(registry.getById(UUID.randomUUID()));
    }

    @Test
    @DisplayName("register: deve indexar por owner")
    void shouldIndexByOwner() {
        registry.register(forge1);
        registry.register(forge2);

        List<ForgeStructure> ownerForges = registry.getByOwner(ownerId);
        assertEquals(2, ownerForges.size());
        assertTrue(ownerForges.contains(forge1));
        assertTrue(ownerForges.contains(forge2));
    }

    @Test
    @DisplayName("getByOwner: deve retornar lista vazia para owner sem forjas")
    void shouldReturnEmptyList_forUnknownOwner() {
        assertTrue(registry.getByOwner(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("unregister: deve remover forge de todos os índices")
    void shouldRemoveFromAllIndexes() {
        registry.register(forge1);
        assertEquals(1, registry.size());

        registry.unregister(forge1.getForgeId());

        assertNull(registry.getById(forge1.getForgeId()));
        assertTrue(registry.getByOwner(ownerId).isEmpty());
        assertEquals(0, registry.size());
    }

    @Test
    @DisplayName("unregister: ID inexistente não deve causar erro")
    void shouldHandleUnregisterOfUnknownId() {
        assertDoesNotThrow(() -> registry.unregister(UUID.randomUUID()));
    }

    @Test
    @DisplayName("unregister: deve remover set de owner se vazio")
    void shouldCleanUpOwnerIndex_whenEmpty() {
        registry.register(forge1);
        registry.unregister(forge1.getForgeId());
        // owner sem forjas → getByOwner deve retornar lista vazia
        assertTrue(registry.getByOwner(ownerId).isEmpty());
    }

    @Test
    @DisplayName("countByOwner: deve contar corretamente")
    void shouldCountByOwner() {
        registry.register(forge1);
        registry.register(forge2);
        registry.register(forge3);

        assertEquals(2, registry.countByOwner(ownerId));
        assertEquals(1, registry.countByOwner(forge3.getOwnerUuid()));
        assertEquals(0, registry.countByOwner(UUID.randomUUID()));
    }

    @Test
    @DisplayName("countByOwnerAndTier: deve filtrar por tier")
    void shouldCountByOwnerAndTier() {
        registry.register(forge1);  // BASIC
        registry.register(forge2);  // ADVANCED

        assertEquals(1, registry.countByOwnerAndTier(ownerId, ForgeTier.BASIC));
        assertEquals(1, registry.countByOwnerAndTier(ownerId, ForgeTier.ADVANCED));
        assertEquals(0, registry.countByOwnerAndTier(ownerId, ForgeTier.LEGENDARY));
    }

    @Test
    @DisplayName("getAll: deve retornar coleção imutável com todas as forjas")
    void shouldReturnUnmodifiableCollection() {
        registry.register(forge1);
        registry.register(forge2);

        Collection<ForgeStructure> all = registry.getAll();
        assertEquals(2, all.size());
        assertThrows(UnsupportedOperationException.class,
                () -> all.add(forge3));
    }

    @Test
    @DisplayName("size: deve refletir contagem real")
    void shouldReflectSize() {
        assertEquals(0, registry.size());
        registry.register(forge1);
        assertEquals(1, registry.size());
        registry.register(forge2);
        assertEquals(2, registry.size());
        registry.unregister(forge1.getForgeId());
        assertEquals(1, registry.size());
    }

    @Test
    @DisplayName("clear: deve limpar todos os índices")
    void shouldClearAll() {
        registry.register(forge1);
        registry.register(forge2);
        registry.register(forge3);
        assertEquals(3, registry.size());

        registry.clear();

        assertEquals(0, registry.size());
        assertNull(registry.getById(forge1.getForgeId()));
        assertTrue(registry.getByOwner(ownerId).isEmpty());
    }

    @Test
    @DisplayName("Chunk indexing: forjas no mesmo chunk devem ser agrupadas")
    void shouldGroupByChunk() {
        // forge1 at (100,64,200) → chunk (6,12)
        // Create another forge in the exact same chunk
        ForgeStructure sameChunk = new ForgeStructure(UUID.randomUUID(), UUID.randomUUID(), "world",
                105, 70, 205, ForgeTier.BASIC, ForgeRotation.NORTH);
        // 105>>4=6, 205>>4=12 → same chunk as forge1

        registry.register(forge1);
        registry.register(sameChunk);

        // Both forges share the same chunk key
        assertEquals(2, registry.size());
    }

    @Test
    @DisplayName("register: múltiplas forjas do mesmo owner em tiers diferentes")
    void shouldTrackMultipleTiersPerOwner() {
        ForgeStructure legendary = new ForgeStructure(UUID.randomUUID(), ownerId, "world",
                300, 64, 400, ForgeTier.LEGENDARY, ForgeRotation.WEST);
        registry.register(forge1);  // BASIC
        registry.register(forge2);  // ADVANCED
        registry.register(legendary); // LEGENDARY

        assertEquals(3, registry.countByOwner(ownerId));
        assertEquals(1, registry.countByOwnerAndTier(ownerId, ForgeTier.BASIC));
        assertEquals(1, registry.countByOwnerAndTier(ownerId, ForgeTier.ADVANCED));
        assertEquals(1, registry.countByOwnerAndTier(ownerId, ForgeTier.LEGENDARY));
    }

    @Test
    @DisplayName("unregister: remover uma forja não afeta outras do mesmo owner")
    void shouldNotAffectOtherForges_whenUnregisterOne() {
        registry.register(forge1);
        registry.register(forge2);

        registry.unregister(forge1.getForgeId());

        assertNull(registry.getById(forge1.getForgeId()));
        assertSame(forge2, registry.getById(forge2.getForgeId()));
        assertEquals(1, registry.countByOwner(ownerId));
    }
}

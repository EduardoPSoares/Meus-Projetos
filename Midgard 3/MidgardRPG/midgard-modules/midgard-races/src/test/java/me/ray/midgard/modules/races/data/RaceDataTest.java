package me.ray.midgard.modules.races.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class RaceDataTest {

    private RaceData data;

    @BeforeEach
    void setUp() {
        data = new RaceData();
    }

    @Test
    @DisplayName("Defaults: sem raça, nível 1, xp 0")
    void shouldHaveDefaults() {
        assertNull(data.getRaceId());
        assertFalse(data.hasRace());
        assertEquals(1, data.getLevel());
        assertEquals(0, data.getExperience(), 0.001);
        assertEquals(0, data.getLastRaceChange());
        assertTrue(data.getUnlockedMutations().isEmpty());
    }

    @Test
    @DisplayName("setRaceId e hasRace")
    void shouldSetRaceId() {
        data.setRaceId("elf");
        assertEquals("elf", data.getRaceId());
        assertTrue(data.hasRace());
    }

    @Test
    @DisplayName("hasRace deve retornar false para string vazia")
    void shouldReturnFalse_forEmptyRaceId() {
        data.setRaceId("");
        assertFalse(data.hasRace());
    }

    @Test
    @DisplayName("setLevel e getLevel")
    void shouldSetLevel() {
        data.setLevel(25);
        assertEquals(25, data.getLevel());
    }

    @Test
    @DisplayName("setExperience e addExperience")
    void shouldManageExperience() {
        data.setExperience(100.0);
        assertEquals(100.0, data.getExperience(), 0.001);

        data.addExperience(50.5);
        assertEquals(150.5, data.getExperience(), 0.001);
    }

    @Test
    @DisplayName("setLastRaceChange")
    void shouldSetLastRaceChange() {
        long ts = System.currentTimeMillis();
        data.setLastRaceChange(ts);
        assertEquals(ts, data.getLastRaceChange());
    }

    @Test
    @DisplayName("unlockMutation e hasMutation")
    void shouldUnlockMutations() {
        assertFalse(data.hasMutation("darkvision"));
        data.unlockMutation("darkvision");
        assertTrue(data.hasMutation("darkvision"));
    }

    @Test
    @DisplayName("Múltiplas mutations")
    void shouldTrackMultipleMutations() {
        data.unlockMutation("darkvision");
        data.unlockMutation("claws");
        data.unlockMutation("thick_skin");

        assertTrue(data.hasMutation("darkvision"));
        assertTrue(data.hasMutation("claws"));
        assertTrue(data.hasMutation("thick_skin"));
        assertFalse(data.hasMutation("wings"));
        assertEquals(3, data.getUnlockedMutations().size());
    }

    @Test
    @DisplayName("setUnlockedMutations deve substituir o set")
    void shouldReplaceUnlockedMutations() {
        data.unlockMutation("old");
        Set<String> newMutations = ConcurrentHashMap.newKeySet();
        newMutations.add("new1");
        newMutations.add("new2");
        data.setUnlockedMutations(newMutations);

        assertFalse(data.hasMutation("old"));
        assertTrue(data.hasMutation("new1"));
        assertTrue(data.hasMutation("new2"));
    }

    @Test
    @DisplayName("getUnlockedMutations com null interno deve retornar set vazio")
    void shouldHandleNullMutationsSet() {
        data.setUnlockedMutations(null);
        // deve recriar o set internamente
        assertNotNull(data.getUnlockedMutations());
        assertTrue(data.getUnlockedMutations().isEmpty());
    }
}

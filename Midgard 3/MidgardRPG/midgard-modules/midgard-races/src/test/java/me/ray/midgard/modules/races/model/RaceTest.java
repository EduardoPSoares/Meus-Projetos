package me.ray.midgard.modules.races.model;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RaceTest {

    private Race createRace(String id, String parent, int minLevel) {
        return new Race(id, "Display " + id, parent, minLevel, 10, 5,
                mock(ItemStack.class),
                List.of("Desc line 1", "Desc line 2"),
                Map.of("MAX_HEALTH", 2.0, "MOVEMENT_SPEED", 0.01),
                Map.of("MAX_HEALTH", 0.5),
                List.of(),
                List.of("perm.one"),
                List.of("cmd select"),
                List.of("cmd remove"));
    }

    @Test
    @DisplayName("Deve armazenar todos os campos corretamente")
    void shouldStoreAllFields() {
        ItemStack icon = mock(ItemStack.class);
        List<String> desc = List.of("Linha 1");
        Map<String, Double> attrs = Map.of("MAX_HEALTH", 4.0);
        Map<String, Double> perLevel = Map.of("ARMOR", 0.1);
        List<ConfiguredTrait> traits = List.of();
        List<String> perms = List.of("perm.a");
        List<String> onSelect = List.of("cmd1");
        List<String> onRemove = List.of("cmd2");

        Race race = new Race("elf", "Elfo", null, 0, 11, 3, icon,
                desc, attrs, perLevel, traits, perms, onSelect, onRemove);

        assertEquals("elf", race.getId());
        assertEquals("Elfo", race.getDisplayName());
        assertNull(race.getParentRace());
        assertEquals(0, race.getMinLevel());
        assertEquals(11, race.getSlot());
        assertEquals(3, race.getTreeSlot());
        assertSame(icon, race.getIcon());
        assertEquals(desc, race.getDescription());
        assertEquals(attrs, race.getAttributes());
        assertEquals(perLevel, race.getPerLevelAttributes());
        assertEquals(traits, race.getTraits());
        assertEquals(perms, race.getPermissions());
        assertEquals(onSelect, race.getOnSelectCommands());
        assertEquals(onRemove, race.getOnRemoveCommands());
    }

    @Test
    @DisplayName("isSubRace deve retornar true quando parent definido")
    void shouldBeSubRace_whenParentSet() {
        Race race = createRace("dark_elf", "elf", 10);
        assertTrue(race.isSubRace());
    }

    @Test
    @DisplayName("isSubRace deve retornar false quando parent null")
    void shouldNotBeSubRace_whenParentNull() {
        Race race = createRace("human", null, 0);
        assertFalse(race.isSubRace());
    }

    @Test
    @DisplayName("isSubRace deve retornar false quando parent vazio")
    void shouldNotBeSubRace_whenParentEmpty() {
        Race race = createRace("human", "", 0);
        assertFalse(race.isSubRace());
    }

    @Test
    @DisplayName("getAttributes deve conter atributos configurados")
    void shouldReturnAttributes() {
        Race race = createRace("orc", null, 0);
        assertEquals(2.0, race.getAttributes().get("MAX_HEALTH"));
        assertEquals(0.01, race.getAttributes().get("MOVEMENT_SPEED"));
    }

    @Test
    @DisplayName("getPerLevelAttributes deve conter bônus por nível")
    void shouldReturnPerLevelAttributes() {
        Race race = createRace("orc", null, 0);
        assertEquals(0.5, race.getPerLevelAttributes().get("MAX_HEALTH"));
    }
}

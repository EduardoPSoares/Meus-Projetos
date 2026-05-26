package me.ray.midgard.modules.races.event;

import me.ray.midgard.modules.races.model.Race;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RaceEventTest {

    private final Player player = mock(Player.class);

    private Race createRace(String id) {
        return new Race(id, "Display " + id, null, 0, 0, 0, null,
                List.of(), Map.of(), Map.of(), List.of(), List.of(), List.of(), List.of());
    }

    // ═══ PlayerChangeRaceEvent ═══

    @Test
    @DisplayName("PlayerChangeRaceEvent: deve armazenar oldRace e newRace")
    void shouldStoreRaces() {
        Race old = createRace("human");
        Race newRace = createRace("elf");
        PlayerChangeRaceEvent event = new PlayerChangeRaceEvent(player, old, newRace);

        assertSame(player, event.getPlayer());
        assertSame(old, event.getOldRace());
        assertSame(newRace, event.getNewRace());
    }

    @Test
    @DisplayName("PlayerChangeRaceEvent: deve ser cancelável")
    void shouldBeCancellable() {
        PlayerChangeRaceEvent event = new PlayerChangeRaceEvent(player, null, createRace("elf"));
        assertFalse(event.isCancelled());
        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("PlayerChangeRaceEvent: HandlerList não null")
    void shouldHaveHandlerList_change() {
        PlayerChangeRaceEvent event = new PlayerChangeRaceEvent(player, null, createRace("elf"));
        assertNotNull(event.getHandlers());
        assertNotNull(PlayerChangeRaceEvent.getHandlerList());
        assertSame(PlayerChangeRaceEvent.getHandlerList(), event.getHandlers());
    }

    @Test
    @DisplayName("PlayerChangeRaceEvent: oldRace pode ser null (primeira seleção)")
    void shouldAllowNullOldRace() {
        PlayerChangeRaceEvent event = new PlayerChangeRaceEvent(player, null, createRace("orc"));
        assertNull(event.getOldRace());
        assertNotNull(event.getNewRace());
    }

    // ═══ PlayerRaceLevelUpEvent ═══

    @Test
    @DisplayName("PlayerRaceLevelUpEvent: deve armazenar race e níveis")
    void shouldStoreLevelUpData() {
        Race race = createRace("dwarf");
        PlayerRaceLevelUpEvent event = new PlayerRaceLevelUpEvent(player, race, 5, 6);

        assertSame(player, event.getPlayer());
        assertSame(race, event.getRace());
        assertEquals(5, event.getOldLevel());
        assertEquals(6, event.getNewLevel());
    }

    @Test
    @DisplayName("PlayerRaceLevelUpEvent: HandlerList não null")
    void shouldHaveHandlerList_levelUp() {
        PlayerRaceLevelUpEvent event = new PlayerRaceLevelUpEvent(player, createRace("elf"), 1, 2);
        assertNotNull(event.getHandlers());
        assertNotNull(PlayerRaceLevelUpEvent.getHandlerList());
        assertSame(PlayerRaceLevelUpEvent.getHandlerList(), event.getHandlers());
    }
}

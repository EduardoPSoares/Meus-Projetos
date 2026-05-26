package me.ray.midgard.core.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PlayerLevelUpEventTest {

    @Mock
    Player mockPlayer;

    @Test
    void shouldStoreValues() {
        PlayerLevelUpEvent event = new PlayerLevelUpEvent(mockPlayer, 5, 6);

        assertSame(mockPlayer, event.getPlayer());
        assertEquals(5, event.getOldLevel());
        assertEquals(6, event.getNewLevel());
    }

    @Test
    void shouldHaveHandlerList() {
        PlayerLevelUpEvent event = new PlayerLevelUpEvent(mockPlayer, 1, 2);
        assertNotNull(event.getHandlers());
    }

    @Test
    void shouldHaveStaticHandlerList() {
        HandlerList list = PlayerLevelUpEvent.getHandlerList();
        assertNotNull(list);
    }

    @Test
    void shouldHaveConsistentHandlerList() {
        PlayerLevelUpEvent event = new PlayerLevelUpEvent(mockPlayer, 1, 2);
        assertSame(PlayerLevelUpEvent.getHandlerList(), event.getHandlers());
    }
}

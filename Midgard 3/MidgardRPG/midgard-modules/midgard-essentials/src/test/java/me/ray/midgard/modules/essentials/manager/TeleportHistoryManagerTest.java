package me.ray.midgard.modules.essentials.manager;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeleportHistoryManagerTest {

    private TeleportHistoryManager manager;

    @Mock
    private Player player;
    @Mock
    private Player player2;
    @Mock
    private World world;

    private final UUID uuid1 = UUID.randomUUID();
    private final UUID uuid2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        manager = new TeleportHistoryManager();
        lenient().when(player.getUniqueId()).thenReturn(uuid1);
        lenient().when(player2.getUniqueId()).thenReturn(uuid2);
    }

    @Test
    void shouldReturnFalseWhenNoHistory() {
        assertFalse(manager.hasLastLocation(player));
    }

    @Test
    void shouldReturnNullWhenNoHistory() {
        assertNull(manager.getLastLocation(player));
    }

    @Test
    void shouldSetAndGetLastLocation() {
        Location loc = new Location(world, 1, 2, 3);
        manager.setLastLocation(player, loc);

        assertTrue(manager.hasLastLocation(player));
        assertEquals(loc, manager.getLastLocation(player));
    }

    @Test
    void shouldOverwritePreviousLocation() {
        Location loc1 = new Location(world, 1, 2, 3);
        Location loc2 = new Location(world, 4, 5, 6);

        manager.setLastLocation(player, loc1);
        manager.setLastLocation(player, loc2);

        assertEquals(loc2, manager.getLastLocation(player));
    }

    @Test
    void shouldClearHistory() {
        Location loc = new Location(world, 1, 2, 3);
        manager.setLastLocation(player, loc);

        manager.clearHistory(player);

        assertFalse(manager.hasLastLocation(player));
        assertNull(manager.getLastLocation(player));
    }

    @Test
    void shouldKeepSeparateHistoryPerPlayer() {
        Location loc1 = new Location(world, 1, 2, 3);
        Location loc2 = new Location(world, 7, 8, 9);

        manager.setLastLocation(player, loc1);
        manager.setLastLocation(player2, loc2);

        assertEquals(loc1, manager.getLastLocation(player));
        assertEquals(loc2, manager.getLastLocation(player2));
    }

    @Test
    void shouldClearOnlyForSpecificPlayer() {
        Location loc1 = new Location(world, 1, 2, 3);
        Location loc2 = new Location(world, 7, 8, 9);

        manager.setLastLocation(player, loc1);
        manager.setLastLocation(player2, loc2);

        manager.clearHistory(player);

        assertFalse(manager.hasLastLocation(player));
        assertTrue(manager.hasLastLocation(player2));
    }

    @Test
    void shouldClearNonExistentPlayerWithoutError() {
        assertDoesNotThrow(() -> manager.clearHistory(player));
    }
}

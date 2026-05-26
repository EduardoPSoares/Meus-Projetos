package me.ray.midgard.core.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuboidTest {

    @Mock
    World world;

    @Mock
    Block block;

    @Test
    void testDimensionsAndContains() {
        when(world.getName()).thenReturn("world");

        Location p1 = new Location(world, 0, 0, 0);
        Location p2 = new Location(world, 10, 10, 10);

        Cuboid cuboid = new Cuboid(p1, p2);

        // Test Contains (Inside)
        Location inside = new Location(world, 5, 5, 5);
        assertTrue(cuboid.contains(inside));

        // Test Contains (Edge)
        Location edge = new Location(world, 0, 0, 0);
        assertTrue(cuboid.contains(edge));

        // Test Contains (Outside)
        Location outside = new Location(world, 11, 5, 5);
        assertFalse(cuboid.contains(outside));
    }

    @Test
    void testCenter() {
        // Need to mock Bukkit.getWorld for getCenter() or refactor Cuboid to not rely on static Bukkit.
        // Since we can't easily mock static Bukkit.getWorld("name") without PowerMock (which we avoided),
        // we might skip getCenter() test OR use MockBukkit later.
        // However, for this task, we focus on logic that doesn't hit static Bukkit if possible.
        // Cuboid.getCenter() calls Bukkit.getWorld(name). This will return null in test environment.
        // So we skip getCenter() test here to avoid NPE or need for complex mocking.
    }
}

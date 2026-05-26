package me.ray.midgard.core.loot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LootContextTest {

    @Test
    void shouldCreateWithNullProfile() {
        LootContext ctx = new LootContext(null, null, 0);
        assertTrue(ctx.getLooter().isEmpty());
        assertTrue(ctx.getPlayer().isEmpty());
        assertNull(ctx.getLocation());
        assertEquals(0.0, ctx.getLuck());
    }

    @Test
    void shouldStoreLuck() {
        LootContext ctx = new LootContext(null, null, 42.5);
        assertEquals(42.5, ctx.getLuck());
    }

    @Test
    void shouldCreateViaStaticFactoryWithLocation() {
        LootContext ctx = LootContext.of(null);
        assertNotNull(ctx);
        assertTrue(ctx.getLooter().isEmpty());
        assertEquals(0.0, ctx.getLuck());
    }
}

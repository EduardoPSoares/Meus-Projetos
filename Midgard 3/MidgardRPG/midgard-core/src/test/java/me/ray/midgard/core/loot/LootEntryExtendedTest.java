package me.ray.midgard.core.loot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LootEntryExtendedTest {

    @Test
    void shouldStoreAllFields() {
        LootEntry entry = new LootEntry(LootType.ITEM, "DIAMOND_SWORD", 75.0, 1, 3);
        assertEquals(LootType.ITEM, entry.getType());
        assertEquals("DIAMOND_SWORD", entry.getValue());
        assertEquals(75.0, entry.getChance());
    }

    @Test
    void shouldRollAmountInRange() {
        LootEntry entry = new LootEntry(LootType.ITEM, "STONE", 100, 3, 7);
        for (int i = 0; i < 200; i++) {
            int amount = entry.rollAmount();
            assertTrue(amount >= 3 && amount <= 7, "Amount " + amount + " out of range [3, 7]");
        }
    }

    @Test
    void shouldReturnFixedAmountWhenMinEqualsMax() {
        LootEntry entry = new LootEntry(LootType.ITEM, "STONE", 100, 5, 5);
        for (int i = 0; i < 50; i++) {
            assertEquals(5, entry.rollAmount());
        }
    }

    @Test
    void shouldDropWith100PercentChance() {
        LootEntry entry = new LootEntry(LootType.ITEM, "STONE", 100.0, 1, 1);
        LootContext ctx = new LootContext(null, null, 0);
        for (int i = 0; i < 100; i++) {
            assertTrue(entry.canDrop(ctx));
        }
    }

    @Test
    void shouldNotDropWith0PercentChanceNoLuck() {
        LootEntry entry = new LootEntry(LootType.ITEM, "STONE", 0.0, 1, 1);
        LootContext ctx = new LootContext(null, null, 0);
        for (int i = 0; i < 100; i++) {
            assertFalse(entry.canDrop(ctx));
        }
    }

    @Test
    void shouldRespectLuckBoost() {
        // 50% chance + 100 luck = 150% = always drops
        LootEntry entry = new LootEntry(LootType.ITEM, "STONE", 50.0, 1, 1);
        LootContext luckyCtx = new LootContext(null, null, 100.0);
        for (int i = 0; i < 100; i++) {
            assertTrue(entry.canDrop(luckyCtx));
        }
    }

    @Test
    void shouldCacheObject() {
        LootEntry entry = new LootEntry(LootType.ITEM, "STONE", 100, 1, 1);
        assertNull(entry.getCachedObject());

        Object cached = new Object();
        entry.setCachedObject(cached);
        assertSame(cached, entry.getCachedObject());
    }

    @Test
    void shouldStartWithEmptyConditions() {
        LootEntry entry = new LootEntry(LootType.COMMAND, "say hello", 50, 1, 1);
        assertNotNull(entry.getConditions());
        assertTrue(entry.getConditions().isEmpty());
    }

    @Test
    void shouldSupportAllLootTypes() {
        for (LootType type : LootType.values()) {
            LootEntry entry = new LootEntry(type, "test", 50, 1, 1);
            assertEquals(type, entry.getType());
        }
    }
}

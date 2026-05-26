package me.ray.midgard.core.loot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LootTableExtendedTest {

    @Test
    void shouldCreateWithId() {
        LootTable table = new LootTable("boss_drops");
        assertEquals("boss_drops", table.getId());
    }

    @Test
    void shouldHaveDefaultRolls() {
        LootTable table = new LootTable("test");
        assertEquals(1, table.getMinRolls());
        assertEquals(1, table.getMaxRolls());
    }

    @Test
    void shouldSetRolls() {
        LootTable table = new LootTable("test");
        table.setMinRolls(2);
        table.setMaxRolls(5);
        assertEquals(2, table.getMinRolls());
        assertEquals(5, table.getMaxRolls());
    }

    @Test
    void shouldStartWithEmptyEntries() {
        LootTable table = new LootTable("test");
        assertTrue(table.getEntries().isEmpty());
    }

    @Test
    void shouldAddEntries() {
        LootTable table = new LootTable("test");
        LootEntry e1 = new LootEntry(LootType.ITEM, "DIAMOND", 50, 1, 3);
        LootEntry e2 = new LootEntry(LootType.MONEY, "100", 100, 1, 1);

        table.addEntry(e1);
        table.addEntry(e2);

        assertEquals(2, table.getEntries().size());
        assertSame(e1, table.getEntries().get(0));
        assertSame(e2, table.getEntries().get(1));
    }

    @Test
    void shouldPreserveEntryOrder() {
        LootTable table = new LootTable("test");
        for (int i = 0; i < 10; i++) {
            table.addEntry(new LootEntry(LootType.ITEM, "ITEM_" + i, i * 10, 1, 1));
        }
        assertEquals(10, table.getEntries().size());
        for (int i = 0; i < 10; i++) {
            assertEquals("ITEM_" + i, table.getEntries().get(i).getValue());
        }
    }
}

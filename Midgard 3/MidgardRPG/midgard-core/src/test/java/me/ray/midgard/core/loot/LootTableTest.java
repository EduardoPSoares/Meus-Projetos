package me.ray.midgard.core.loot;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LootTableTest {

    @Test
    void testRollsRange() {
        LootTable table = new LootTable("test_table");
        table.setMinRolls(1);
        table.setMaxRolls(3);

        assertEquals(1, table.getMinRolls());
        assertEquals(3, table.getMaxRolls());
        
        // Testa se entries são adicionadas corretamente
        LootEntry entry = new LootEntry(LootType.ITEM, "DIAMOND", 100, 1, 1);
        table.addEntry(entry);
        
        assertEquals(1, table.getEntries().size());
        assertEquals(entry, table.getEntries().get(0));
    }
}

package me.ray.midgard.core.loot;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LootEntryTest {

    @Test
    void testChanceCalculation() {
        // Entry com 100% de chance deve sempre dropar
        LootEntry entry100 = new LootEntry(LootType.ITEM, "STONE", 100.0, 1, 1);
        LootContext ctx = LootContext.of(null); // Sem player, sem luck
        
        assertTrue(entry100.canDrop(ctx), "100% chance deve sempre dropar");
        
        // Entry com 0% de chance nunca deve dropar (sem luck)
        LootEntry entry0 = new LootEntry(LootType.ITEM, "STONE", 0.0, 1, 1);
        assertFalse(entry0.canDrop(ctx), "0% chance não deve dropar sem luck");
    }

    @Test
    void testLuckInfluence() {
        // Entry com 0% chance + 50% Luck = 50% chance efetiva
        // Vamos testar com 100% luck para garantir drop
        LootEntry entry = new LootEntry(LootType.ITEM, "STONE", 0.0, 1, 1);
        LootContext luckyCtx = new LootContext(null, null, 101.0); // 101 luck
        
        assertTrue(entry.canDrop(luckyCtx), "Luck deve aumentar a chance de drop");
    }

    @Test
    void testAmountRoll() {
        LootEntry entry = new LootEntry(LootType.ITEM, "STONE", 100, 5, 10);
        
        for (int i = 0; i < 50; i++) {
            int amount = entry.rollAmount();
            assertTrue(amount >= 5 && amount <= 10, "Amount " + amount + " fora do range 5-10");
        }
        
        LootEntry fixedEntry = new LootEntry(LootType.ITEM, "STONE", 100, 3, 3);
        assertEquals(3, fixedEntry.rollAmount(), "Min=Max deve retornar valor fixo");
    }
}

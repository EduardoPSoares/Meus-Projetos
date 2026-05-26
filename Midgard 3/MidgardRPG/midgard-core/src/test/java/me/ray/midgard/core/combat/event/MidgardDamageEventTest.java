package me.ray.midgard.core.combat.event;

import me.ray.midgard.core.combat.DamageType;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MidgardDamageEventTest {

    @Mock
    LivingEntity attacker;

    @Mock
    LivingEntity victim;

    @Test
    void shouldStoreInitialValues() {
        MidgardDamageEvent event = new MidgardDamageEvent(attacker, victim, 100.0, DamageType.PHYSICAL, true);

        assertSame(attacker, event.getAttacker());
        assertSame(victim, event.getVictim());
        assertEquals(100.0, event.getDamage());
        assertEquals(100.0, event.getOriginalDamage());
        assertEquals(DamageType.PHYSICAL, event.getType());
        assertTrue(event.isCritical());
        assertFalse(event.isCancelled());
    }

    @Test
    void shouldAllowDamageModification() {
        MidgardDamageEvent event = new MidgardDamageEvent(attacker, victim, 50.0, DamageType.MAGICAL, false);
        event.setDamage(75.0);

        assertEquals(75.0, event.getDamage());
        assertEquals(50.0, event.getOriginalDamage(), "Original damage should remain unchanged");
    }

    @Test
    void shouldSupportCancellation() {
        MidgardDamageEvent event = new MidgardDamageEvent(attacker, victim, 50.0, DamageType.TRUE, false);
        assertFalse(event.isCancelled());

        event.setCancelled(true);
        assertTrue(event.isCancelled());

        event.setCancelled(false);
        assertFalse(event.isCancelled());
    }

    @Test
    void shouldAllowCriticalToggle() {
        MidgardDamageEvent event = new MidgardDamageEvent(attacker, victim, 50.0, DamageType.FIRE, false);
        assertFalse(event.isCritical());

        event.setCritical(true);
        assertTrue(event.isCritical());
    }

    @Test
    void shouldExposeHandlerList() {
        assertNotNull(MidgardDamageEvent.getHandlerList());
        MidgardDamageEvent event = new MidgardDamageEvent(attacker, victim, 10, DamageType.ICE, false);
        assertNotNull(event.getHandlers());
    }

    @Test
    void shouldSupportAllDamageTypes() {
        for (DamageType type : DamageType.values()) {
            MidgardDamageEvent event = new MidgardDamageEvent(attacker, victim, 10, type, false);
            assertEquals(type, event.getType());
        }
    }
}

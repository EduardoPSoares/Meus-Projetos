package me.ray.midgard.core.utils;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CooldownManagerTest {

    @Test
    void testCooldownFlow() throws InterruptedException {
        CooldownManager manager = new CooldownManager();
        UUID player = UUID.randomUUID();
        String key = "test_skill";

        // Inicialmente sem cooldown
        assertFalse(manager.isOnCooldown(player, key));
        assertEquals(0, manager.getRemainingMillis(player, key));

        // Adiciona cooldown de 100ms
        manager.setCooldown(player, key, Duration.ofMillis(100));

        // Deve estar em cooldown
        assertTrue(manager.isOnCooldown(player, key));
        
        // Aguarda expirar
        Thread.sleep(150);

        // Não deve estar mais em cooldown
        assertFalse(manager.isOnCooldown(player, key));
    }

    @Test
    void testRemainingFormatted() {
        CooldownManager manager = new CooldownManager();
        UUID player = UUID.randomUUID();
        String key = "format_test";

        manager.setCooldown(player, key, Duration.ofSeconds(5));
        
        // Verifica se a formatação contém "s" e é razoável (ex: "4.9s" ou "5.0s")
        String formatted = manager.getRemainingFormatted(player, key);
        assertTrue(formatted.endsWith("s"));
        // Locale-dependent: may use . or , as decimal separator
        assertTrue(formatted.matches(".*[.,].*"));
    }
}

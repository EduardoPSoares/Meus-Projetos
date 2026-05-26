package me.ray.midgard.modules.professions.blacksmith.forge.fuel;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class ForgeFuelTest {

    @Test
    @DisplayName("Deve armazenar todos os atributos corretamente")
    void shouldStoreAllAttributes() {
        ForgeFuel fuel = new ForgeFuel(Material.COAL, "Carvão", 1.0, 200, 0.0, 0);

        assertEquals(Material.COAL, fuel.getMaterial());
        assertEquals("Carvão", fuel.getDisplayName());
        assertEquals(1.0, fuel.getHeatingPower(), 0.001);
        assertEquals(200, fuel.getBurnTime());
        assertEquals(0.0, fuel.getQualityBonus(), 0.001);
        assertEquals(0, fuel.getMinForgeLevel());
    }

    @Test
    @DisplayName("Combustível de alta qualidade com bônus")
    void shouldStoreHighQualityFuel() {
        ForgeFuel fuel = new ForgeFuel(Material.NETHER_STAR, "Estrela do Nether",
                3.0, 1200, 0.15, 80);

        assertEquals(Material.NETHER_STAR, fuel.getMaterial());
        assertEquals(3.0, fuel.getHeatingPower(), 0.001);
        assertEquals(1200, fuel.getBurnTime());
        assertEquals(0.15, fuel.getQualityBonus(), 0.001);
        assertEquals(80, fuel.getMinForgeLevel());
    }
}

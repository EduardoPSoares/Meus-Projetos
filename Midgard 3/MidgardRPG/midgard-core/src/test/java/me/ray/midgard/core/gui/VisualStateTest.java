package me.ray.midgard.core.gui;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VisualStateTest {

    @Test
    void shouldHaveEightStates() {
        assertEquals(8, VisualState.values().length);
    }

    @Test
    void shouldHaveCorrectMaterials() {
        assertEquals(Material.LIME_DYE, VisualState.AVAILABLE.getMaterial());
        assertEquals(Material.YELLOW_DYE, VisualState.SELECTED.getMaterial());
        assertEquals(Material.GRAY_DYE, VisualState.LOCKED.getMaterial());
        assertEquals(Material.RED_DYE, VisualState.ERROR.getMaterial());
        assertEquals(Material.LIGHT_BLUE_DYE, VisualState.INFO.getMaterial());
        assertEquals(Material.PURPLE_DYE, VisualState.SPECIAL.getMaterial());
        assertEquals(Material.GREEN_DYE, VisualState.SUCCESS.getMaterial());
        assertEquals(Material.ORANGE_DYE, VisualState.WARNING.getMaterial());
    }

    @Test
    void shouldHaveColorCodes() {
        assertEquals("§a", VisualState.AVAILABLE.getColorCode());
        assertEquals("§e", VisualState.SELECTED.getColorCode());
        assertEquals("§7", VisualState.LOCKED.getColorCode());
        assertEquals("§c", VisualState.ERROR.getColorCode());
    }

    @Test
    void shouldFormatText() {
        assertEquals("§aDisponível", VisualState.AVAILABLE.format("Disponível"));
        assertEquals("§cErro!", VisualState.ERROR.format("Erro!"));
    }

    @Test
    void shouldFormatEmptyText() {
        assertEquals("§a", VisualState.AVAILABLE.format(""));
    }
}

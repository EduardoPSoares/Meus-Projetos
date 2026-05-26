package me.ray.midgard.modules.item.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemCategoryTest {

    @Nested
    class DirectConstructor {

        @Test
        void shouldStoreAllFields() {
            ItemCategory cat = new ItemCategory("swords", "Espadas", Material.DIAMOND_SWORD, 1001, 10, 1, "display-sword");

            assertEquals("swords", cat.getId());
            assertEquals("Espadas", cat.getName());
            assertEquals(Material.DIAMOND_SWORD, cat.getIcon());
            assertEquals(1001, cat.getModelData());
            assertEquals(10, cat.getSlot());
            assertEquals(1, cat.getPage());
            assertEquals("display-sword", cat.getDisplayItemId());
        }

        @Test
        void shouldHandleNullDisplayItemId() {
            ItemCategory cat = new ItemCategory("misc", "Misc", Material.CHEST, 0, 0, 1, null);
            assertNull(cat.getDisplayItemId());
        }
    }

    @Nested
    class ConfigConstructor {

        @Mock
        ConfigurationSection section;

        @Test
        void shouldLoadFromConfig() {
            when(section.getString("name", "weapons")).thenReturn("Armas");
            when(section.getString("icon", "CHEST")).thenReturn("DIAMOND_SWORD");
            when(section.getInt("model-data", 0)).thenReturn(100);
            when(section.getInt("slot", -1)).thenReturn(5);
            when(section.getInt("page", 1)).thenReturn(2);
            when(section.getString("display-item", null)).thenReturn("custom-sword");

            ItemCategory cat = new ItemCategory("weapons", section);

            assertEquals("weapons", cat.getId());
            assertEquals("Armas", cat.getName());
            assertEquals(Material.DIAMOND_SWORD, cat.getIcon());
            assertEquals(100, cat.getModelData());
            assertEquals(5, cat.getSlot());
            assertEquals(2, cat.getPage());
            assertEquals("custom-sword", cat.getDisplayItemId());
        }

        @Test
        void shouldUseDefaults_whenConfigValuesAbsent() {
            when(section.getString("name", "test")).thenReturn("test");
            when(section.getString("icon", "CHEST")).thenReturn("CHEST");
            when(section.getInt("model-data", 0)).thenReturn(0);
            when(section.getInt("slot", -1)).thenReturn(-1);
            when(section.getInt("page", 1)).thenReturn(1);
            when(section.getString("display-item", null)).thenReturn(null);

            ItemCategory cat = new ItemCategory("test", section);

            assertEquals("test", cat.getId());
            assertEquals("test", cat.getName());
            assertEquals(Material.CHEST, cat.getIcon());
            assertEquals(0, cat.getModelData());
            assertEquals(-1, cat.getSlot());
            assertEquals(1, cat.getPage());
            assertNull(cat.getDisplayItemId());
        }

        @Test
        void shouldFallbackToChest_forInvalidMaterial() {
            when(section.getString("name", "bad")).thenReturn("Bad");
            when(section.getString("icon", "CHEST")).thenReturn("NOT_A_REAL_MATERIAL");
            when(section.getInt("model-data", 0)).thenReturn(0);
            when(section.getInt("slot", -1)).thenReturn(0);
            when(section.getInt("page", 1)).thenReturn(1);
            when(section.getString("display-item", null)).thenReturn(null);

            ItemCategory cat = new ItemCategory("bad", section);

            assertEquals(Material.CHEST, cat.getIcon());
        }
    }
}

package me.ray.midgard.modules.mythicmobs.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MidgardShieldMechanicTest {

    @Mock MythicLineConfig config;
    @Mock AbstractEntity target;
    @Mock SkillMetadata skillMetadata;
    @Mock PlaceholderDouble placeholderAmount;
    @Mock LivingEntity livingEntity;

    MockedStatic<BukkitAdapter> bukkitAdapterStatic;

    @BeforeEach
    void setUp() {
        bukkitAdapterStatic = Mockito.mockStatic(BukkitAdapter.class);
        when(target.isLiving()).thenReturn(true);
        bukkitAdapterStatic.when(() -> BukkitAdapter.adapt(target)).thenReturn(livingEntity);
        when(placeholderAmount.get(any(), any())).thenReturn(10.0);
    }

    @AfterEach
    void tearDown() {
        bukkitAdapterStatic.close();
    }

    private MidgardShieldMechanic createMechanic(boolean addMode) {
        when(config.getPlaceholderDouble(any(String[].class), anyDouble())).thenReturn(placeholderAmount);
        when(config.getBoolean(any(String[].class), eq(false))).thenReturn(addMode);
        return new MidgardShieldMechanic(config);
    }

    @Nested
    @DisplayName("Set Mode (add=false)")
    class SetMode {
        @Test
        void shouldSetAbsorption() {
            when(livingEntity.getAbsorptionAmount()).thenReturn(5.0);
            when(placeholderAmount.get(any(), any())).thenReturn(10.0);
            MidgardShieldMechanic mechanic = createMechanic(false);
            SkillResult result = mechanic.castAtEntity(skillMetadata, target);
            assertEquals(SkillResult.SUCCESS, result);
            verify(livingEntity).setAbsorptionAmount(10.0);
        }

        @Test
        void shouldOverridePreviousAbsorption() {
            when(livingEntity.getAbsorptionAmount()).thenReturn(20.0);
            when(placeholderAmount.get(any(), any())).thenReturn(5.0);
            createMechanic(false).castAtEntity(skillMetadata, target);
            verify(livingEntity).setAbsorptionAmount(5.0);
        }
    }

    @Nested
    @DisplayName("Add/Stack Mode (add=true)")
    class AddMode {
        @Test
        void shouldAddToExistingAbsorption() {
            when(livingEntity.getAbsorptionAmount()).thenReturn(5.0);
            when(placeholderAmount.get(any(), any())).thenReturn(10.0);
            createMechanic(true).castAtEntity(skillMetadata, target);
            verify(livingEntity).setAbsorptionAmount(15.0); // 5 + 10
        }

        @Test
        void shouldStackOnZeroAbsorption() {
            when(livingEntity.getAbsorptionAmount()).thenReturn(0.0);
            when(placeholderAmount.get(any(), any())).thenReturn(10.0);
            createMechanic(true).castAtEntity(skillMetadata, target);
            verify(livingEntity).setAbsorptionAmount(10.0);
        }
    }

    @Nested
    @DisplayName("Clamping")
    class Clamping {
        @Test
        void shouldClampNegativeToZero_setMode() {
            when(livingEntity.getAbsorptionAmount()).thenReturn(0.0);
            when(placeholderAmount.get(any(), any())).thenReturn(-5.0);
            createMechanic(false).castAtEntity(skillMetadata, target);
            verify(livingEntity).setAbsorptionAmount(0.0);
        }

        @Test
        void shouldClampNegativeToZero_addMode() {
            when(livingEntity.getAbsorptionAmount()).thenReturn(3.0);
            when(placeholderAmount.get(any(), any())).thenReturn(-10.0);
            createMechanic(true).castAtEntity(skillMetadata, target);
            verify(livingEntity).setAbsorptionAmount(0.0); // 3 + (-10) = -7, clamped to 0
        }
    }

    @Nested
    @DisplayName("Invalid Target")
    class InvalidTarget {
        @Test
        void shouldReturnInvalidTarget_whenNotLiving() {
            when(target.isLiving()).thenReturn(false);
            assertEquals(SkillResult.INVALID_TARGET, createMechanic(false).castAtEntity(skillMetadata, target));
        }
    }

    @Nested
    @DisplayName("Large Values")
    class LargeValues {
        @Test
        void shouldHandleLargeAbsorption() {
            when(livingEntity.getAbsorptionAmount()).thenReturn(0.0);
            when(placeholderAmount.get(any(), any())).thenReturn(1000.0);
            createMechanic(false).castAtEntity(skillMetadata, target);
            verify(livingEntity).setAbsorptionAmount(1000.0);
        }

        @Test
        void shouldHandleZeroAmount() {
            when(livingEntity.getAbsorptionAmount()).thenReturn(10.0);
            when(placeholderAmount.get(any(), any())).thenReturn(0.0);
            createMechanic(false).castAtEntity(skillMetadata, target);
            verify(livingEntity).setAbsorptionAmount(0.0);
        }
    }
}

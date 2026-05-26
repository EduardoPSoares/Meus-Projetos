package me.ray.midgard.modules.mythicmobs.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.profile.ProfileManager;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.CombatData;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MidgardStaminaConditionTest {

    @Mock MythicLineConfig config;
    @Mock AbstractEntity entity;
    @Mock Player player;
    @Mock ProfileManager profileManager;
    @Mock MidgardProfile profile;
    @Mock CombatData combatData;
    @Mock CoreAttributeData coreAttributeData;
    @Mock AttributeInstance maxStaminaAttr;

    MockedStatic<BukkitAdapter> bukkitAdapterStatic;
    MockedStatic<MidgardCore> midgardCoreStatic;

    @BeforeEach
    void setUp() {
        bukkitAdapterStatic = Mockito.mockStatic(BukkitAdapter.class);
        midgardCoreStatic = Mockito.mockStatic(MidgardCore.class);

        when(entity.isPlayer()).thenReturn(true);
        bukkitAdapterStatic.when(() -> BukkitAdapter.adapt(entity)).thenReturn(player);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        midgardCoreStatic.when(MidgardCore::getProfileManager).thenReturn(profileManager);
        when(profileManager.getProfile(any(UUID.class))).thenReturn(profile);
        when(profile.getOrCreateData(CombatData.class)).thenReturn(combatData);
        when(profile.getOrCreateData(CoreAttributeData.class)).thenReturn(coreAttributeData);
        when(coreAttributeData.getInstance(CombatAttributes.MAX_STAMINA)).thenReturn(maxStaminaAttr);
        when(maxStaminaAttr.getValue()).thenReturn(100.0);
    }

    @AfterEach
    void tearDown() {
        bukkitAdapterStatic.close();
        midgardCoreStatic.close();
    }

    private MidgardStaminaCondition createCondition(String range, boolean percentage) {
        when(config.getString(any(String[].class), anyString())).thenReturn(range);
        when(config.getBoolean(any(String[].class), eq(false))).thenReturn(percentage);
        return new MidgardStaminaCondition(config);
    }

    @Nested
    @DisplayName("Absolute Value Comparisons")
    class AbsoluteComparisons {
        @ParameterizedTest
        @CsvSource({
            ">=50, 60, true",
            ">=50, 50, true",
            ">=50, 40, false",
            "<=50, 40, true",
            "<=50, 50, true",
            "<=50, 60, false",
            ">50, 60, true",
            ">50, 50, false",
            "<50, 40, true",
            "<50, 50, false",
        })
        void shouldCompareAbsoluteValues(String range, double currentStamina, boolean expected) {
            when(combatData.getCurrentStamina()).thenReturn(currentStamina);
            assertEquals(expected, createCondition(range, false).check(entity));
        }

        @Test
        void shouldCompareWithEqualSign() {
            when(combatData.getCurrentStamina()).thenReturn(50.0);
            assertTrue(createCondition("=50", false).check(entity));
        }

        @Test
        void shouldCompareWithPlainNumber() {
            when(combatData.getCurrentStamina()).thenReturn(50.0);
            assertTrue(createCondition("50", false).check(entity));
        }
    }

    @Nested
    @DisplayName("Percentage Mode")
    class PercentageMode {
        @Test
        void shouldCompareAsPercentage() {
            when(combatData.getCurrentStamina()).thenReturn(80.0);
            when(maxStaminaAttr.getValue()).thenReturn(200.0);
            // 80/200 * 100 = 40%
            assertTrue(createCondition(">=40", true).check(entity));
        }

        @Test
        void shouldReturnFalse_whenMaxStaminaIsZero() {
            when(combatData.getCurrentStamina()).thenReturn(50.0);
            when(maxStaminaAttr.getValue()).thenReturn(0.0);
            assertFalse(createCondition(">=50", true).check(entity));
        }

        @Test
        void shouldUseDefaultMax_whenAttrNull() {
            when(coreAttributeData.getInstance(CombatAttributes.MAX_STAMINA)).thenReturn(null);
            when(combatData.getCurrentStamina()).thenReturn(50.0);
            // Default max is 100, so 50/100*100 = 50%
            assertTrue(createCondition(">=50", true).check(entity));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        @Test
        void shouldReturnFalse_whenNotPlayer() {
            when(entity.isPlayer()).thenReturn(false);
            assertFalse(createCondition(">0", false).check(entity));
        }

        @Test
        void shouldReturnFalse_whenProfileNull() {
            when(profileManager.getProfile(any(UUID.class))).thenReturn(null);
            assertFalse(createCondition(">0", false).check(entity));
        }

        @Test
        void shouldReturnFalse_whenInvalidRange() {
            when(combatData.getCurrentStamina()).thenReturn(50.0);
            assertFalse(createCondition("xyz", false).check(entity));
        }
    }
}

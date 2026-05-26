package me.ray.midgard.modules.mythicmobs.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.profile.ProfileManager;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MidgardLevelConditionTest {

    @Mock MythicLineConfig config;
    @Mock AbstractEntity entity;
    @Mock Player player;
    @Mock ProfileManager profileManager;
    @Mock MidgardProfile profile;
    @Mock CombatData combatData;

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
    }

    @AfterEach
    void tearDown() {
        bukkitAdapterStatic.close();
        midgardCoreStatic.close();
    }

    private MidgardLevelCondition createCondition(String range) {
        when(config.getString(any(String[].class), anyString())).thenReturn(range);
        return new MidgardLevelCondition(config);
    }

    @Nested
    @DisplayName("GreaterThan (>)")
    class GreaterThan {
        @Test
        void shouldReturnTrue_whenLevelAboveThreshold() {
            when(combatData.getLevel()).thenReturn(6);
            assertTrue(createCondition(">5").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenLevelEqualsThreshold() {
            when(combatData.getLevel()).thenReturn(5);
            assertFalse(createCondition(">5").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenLevelBelowThreshold() {
            when(combatData.getLevel()).thenReturn(4);
            assertFalse(createCondition(">5").checkEntity(entity));
        }
    }

    @Nested
    @DisplayName("GreaterThanOrEqual (>=)")
    class GreaterThanOrEqual {
        @Test
        void shouldReturnTrue_whenLevelAboveThreshold() {
            when(combatData.getLevel()).thenReturn(6);
            assertTrue(createCondition(">=5").checkEntity(entity));
        }

        @Test
        void shouldReturnTrue_whenLevelEqualsThreshold() {
            when(combatData.getLevel()).thenReturn(5);
            assertTrue(createCondition(">=5").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenLevelBelowThreshold() {
            when(combatData.getLevel()).thenReturn(4);
            assertFalse(createCondition(">=5").checkEntity(entity));
        }
    }

    @Nested
    @DisplayName("LessThan (<)")
    class LessThan {
        @Test
        void shouldReturnTrue_whenLevelBelowThreshold() {
            when(combatData.getLevel()).thenReturn(4);
            assertTrue(createCondition("<5").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenLevelEqualsThreshold() {
            when(combatData.getLevel()).thenReturn(5);
            assertFalse(createCondition("<5").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenLevelAboveThreshold() {
            when(combatData.getLevel()).thenReturn(6);
            assertFalse(createCondition("<5").checkEntity(entity));
        }
    }

    @Nested
    @DisplayName("LessThanOrEqual (<=)")
    class LessThanOrEqual {
        @Test
        void shouldReturnTrue_whenLevelBelowThreshold() {
            when(combatData.getLevel()).thenReturn(4);
            assertTrue(createCondition("<=5").checkEntity(entity));
        }

        @Test
        void shouldReturnTrue_whenLevelEqualsThreshold() {
            when(combatData.getLevel()).thenReturn(5);
            assertTrue(createCondition("<=5").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenLevelAboveThreshold() {
            when(combatData.getLevel()).thenReturn(6);
            assertFalse(createCondition("<=5").checkEntity(entity));
        }
    }

    @Nested
    @DisplayName("Range (X-Y)")
    class Range {
        @Test
        void shouldReturnTrue_whenLevelAtMinimum() {
            when(combatData.getLevel()).thenReturn(5);
            assertTrue(createCondition("5-10").checkEntity(entity));
        }

        @Test
        void shouldReturnTrue_whenLevelAtMaximum() {
            when(combatData.getLevel()).thenReturn(10);
            assertTrue(createCondition("5-10").checkEntity(entity));
        }

        @Test
        void shouldReturnTrue_whenLevelInMiddle() {
            when(combatData.getLevel()).thenReturn(7);
            assertTrue(createCondition("5-10").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenLevelBelowRange() {
            when(combatData.getLevel()).thenReturn(4);
            assertFalse(createCondition("5-10").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenLevelAboveRange() {
            when(combatData.getLevel()).thenReturn(11);
            assertFalse(createCondition("5-10").checkEntity(entity));
        }
    }

    @Nested
    @DisplayName("Exact (= or plain number)")
    class Exact {
        @Test
        void shouldReturnTrue_whenLevelMatchesExact() {
            when(combatData.getLevel()).thenReturn(5);
            assertTrue(createCondition("=5").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenLevelDoesNotMatchExact() {
            when(combatData.getLevel()).thenReturn(6);
            assertFalse(createCondition("=5").checkEntity(entity));
        }

        @Test
        void shouldReturnTrue_whenLevelMatchesPlainNumber() {
            when(combatData.getLevel()).thenReturn(5);
            assertTrue(createCondition("5").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenLevelDoesNotMatchPlainNumber() {
            when(combatData.getLevel()).thenReturn(6);
            assertFalse(createCondition("5").checkEntity(entity));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        @Test
        void shouldReturnFalse_whenNotPlayer() {
            when(entity.isPlayer()).thenReturn(false);
            assertFalse(createCondition(">0").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenProfileNull() {
            when(profileManager.getProfile(any(UUID.class))).thenReturn(null);
            assertFalse(createCondition(">0").checkEntity(entity));
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", ">>5", "5-", "-5-10", ""})
        void shouldReturnFalse_whenInvalidFormat(String range) {
            when(combatData.getLevel()).thenReturn(5);
            assertFalse(createCondition(range).checkEntity(entity));
        }

        @Test
        void shouldUseDefaultRange_whenConfigDefault() {
            // Default range from constructor is ">0", so level 1 should pass
            when(combatData.getLevel()).thenReturn(1);
            assertTrue(createCondition(">0").checkEntity(entity));
        }

        @Test
        void shouldHandleSpacesInRange() {
            when(combatData.getLevel()).thenReturn(5);
            assertTrue(createCondition(">= 5").checkEntity(entity));
        }

        @Test
        void shouldHandleZeroLevel() {
            when(combatData.getLevel()).thenReturn(0);
            assertTrue(createCondition(">=0").checkEntity(entity));
        }

        @Test
        void shouldHandleNegativeThreshold() {
            when(combatData.getLevel()).thenReturn(0);
            assertTrue(createCondition(">-1").checkEntity(entity));
        }
    }

    @Nested
    @DisplayName("check() delegates to checkEntity()")
    class CheckDelegation {
        @Test
        void shouldDelegateToCheckEntity() {
            when(combatData.getLevel()).thenReturn(10);
            MidgardLevelCondition condition = createCondition(">5");
            assertEquals(condition.checkEntity(entity), condition.check(entity));
        }
    }
}

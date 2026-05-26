package me.ray.midgard.modules.mythicmobs.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.profile.ProfileManager;
import org.bukkit.entity.Player;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MidgardAttributeConditionTest {

    @Mock MythicLineConfig config;
    @Mock AbstractEntity entity;
    @Mock Player player;
    @Mock ProfileManager profileManager;
    @Mock MidgardProfile profile;
    @Mock CoreAttributeData coreAttributeData;
    @Mock AttributeInstance attributeInstance;

    MockedStatic<BukkitAdapter> bukkitAdapterStatic;
    MockedStatic<MidgardCore> midgardCoreStatic;

    @BeforeEach
    void setUp() {
        bukkitAdapterStatic = Mockito.mockStatic(BukkitAdapter.class);
        midgardCoreStatic = Mockito.mockStatic(MidgardCore.class);

        when(entity.isPlayer()).thenReturn(true);
        bukkitAdapterStatic.when(() -> BukkitAdapter.adapt(entity)).thenReturn(player);
        midgardCoreStatic.when(MidgardCore::getProfileManager).thenReturn(profileManager);
        when(profileManager.getProfile(any(Player.class))).thenReturn(profile);
        when(profile.getOrCreateData(CoreAttributeData.class)).thenReturn(coreAttributeData);
        when(coreAttributeData.getInstance(anyString())).thenReturn(attributeInstance);
    }

    @AfterEach
    void tearDown() {
        bukkitAdapterStatic.close();
        midgardCoreStatic.close();
    }

    private MidgardAttributeCondition createCondition(String attribute, String valueStr) {
        when(config.getString(any(String[].class), eq("strength"))).thenReturn(attribute);
        when(config.getString(any(String[].class), eq(">0"))).thenReturn(valueStr);
        return new MidgardAttributeCondition(config);
    }

    @Nested
    @DisplayName("Constructor Parsing")
    class ConstructorParsing {
        @Test
        void shouldParseGreaterThan() {
            assertDoesNotThrow(() -> createCondition("strength", ">10"));
        }

        @Test
        void shouldParseGreaterThanOrEqual() {
            assertDoesNotThrow(() -> createCondition("strength", ">=10"));
        }

        @Test
        void shouldParseLessThan() {
            assertDoesNotThrow(() -> createCondition("strength", "<10"));
        }

        @Test
        void shouldParseLessThanOrEqual() {
            assertDoesNotThrow(() -> createCondition("strength", "<=10"));
        }

        @Test
        void shouldParseNotEqual() {
            assertDoesNotThrow(() -> createCondition("strength", "!=10"));
        }

        @Test
        void shouldParseDoubleEquals() {
            assertDoesNotThrow(() -> createCondition("strength", "==10"));
        }

        @Test
        void shouldParseSingleEquals() {
            assertDoesNotThrow(() -> createCondition("strength", "=10"));
        }

        @Test
        void shouldParsePlainNumber() {
            assertDoesNotThrow(() -> createCondition("strength", "10"));
        }

        @Test
        void shouldParseDecimalValues() {
            assertDoesNotThrow(() -> createCondition("strength", ">10.5"));
        }

        @Test
        void shouldThrowOnInvalidFormat() {
            assertThrows(IllegalArgumentException.class,
                    () -> createCondition("strength", ">abc"));
        }

        @Test
        void shouldThrowOnEmptyValue() {
            assertThrows(IllegalArgumentException.class,
                    () -> createCondition("strength", ">"));
        }

        @Test
        void shouldThrowOnCompletelyInvalid() {
            assertThrows(IllegalArgumentException.class,
                    () -> createCondition("strength", "abc"));
        }
    }

    @Nested
    @DisplayName("Check - GreaterThan")
    class CheckGreaterThan {
        @Test
        void shouldReturnTrue_whenAboveValue() {
            when(attributeInstance.getValue()).thenReturn(15.0);
            assertTrue(createCondition("strength", ">10").check(entity));
        }

        @Test
        void shouldReturnFalse_whenEqualValue() {
            when(attributeInstance.getValue()).thenReturn(10.0);
            assertFalse(createCondition("strength", ">10").check(entity));
        }

        @Test
        void shouldReturnFalse_whenBelowValue() {
            when(attributeInstance.getValue()).thenReturn(5.0);
            assertFalse(createCondition("strength", ">10").check(entity));
        }
    }

    @Nested
    @DisplayName("Check - LessThan")
    class CheckLessThan {
        @Test
        void shouldReturnTrue_whenBelowValue() {
            when(attributeInstance.getValue()).thenReturn(5.0);
            assertTrue(createCondition("strength", "<10").check(entity));
        }

        @Test
        void shouldReturnFalse_whenEqualValue() {
            when(attributeInstance.getValue()).thenReturn(10.0);
            assertFalse(createCondition("strength", "<10").check(entity));
        }
    }

    @Nested
    @DisplayName("Check - GreaterThanOrEqual")
    class CheckGreaterThanOrEqual {
        @Test
        void shouldReturnTrue_whenAboveValue() {
            when(attributeInstance.getValue()).thenReturn(15.0);
            assertTrue(createCondition("strength", ">=10").check(entity));
        }

        @Test
        void shouldReturnTrue_whenEqualValue() {
            when(attributeInstance.getValue()).thenReturn(10.0);
            assertTrue(createCondition("strength", ">=10").check(entity));
        }

        @Test
        void shouldReturnFalse_whenBelowValue() {
            when(attributeInstance.getValue()).thenReturn(5.0);
            assertFalse(createCondition("strength", ">=10").check(entity));
        }
    }

    @Nested
    @DisplayName("Check - LessThanOrEqual")
    class CheckLessThanOrEqual {
        @Test
        void shouldReturnTrue_whenBelowValue() {
            when(attributeInstance.getValue()).thenReturn(5.0);
            assertTrue(createCondition("strength", "<=10").check(entity));
        }

        @Test
        void shouldReturnTrue_whenEqualValue() {
            when(attributeInstance.getValue()).thenReturn(10.0);
            assertTrue(createCondition("strength", "<=10").check(entity));
        }

        @Test
        void shouldReturnFalse_whenAboveValue() {
            when(attributeInstance.getValue()).thenReturn(15.0);
            assertFalse(createCondition("strength", "<=10").check(entity));
        }
    }

    @Nested
    @DisplayName("Check - Equals")
    class CheckEquals {
        @Test
        void shouldReturnTrue_whenExactMatch() {
            when(attributeInstance.getValue()).thenReturn(10.0);
            assertTrue(createCondition("strength", "==10").check(entity));
        }

        @Test
        void shouldReturnFalse_whenNotMatch() {
            when(attributeInstance.getValue()).thenReturn(11.0);
            assertFalse(createCondition("strength", "==10").check(entity));
        }

        @Test
        void shouldReturnTrue_whenSingleEqualsMatch() {
            when(attributeInstance.getValue()).thenReturn(10.0);
            assertTrue(createCondition("strength", "=10").check(entity));
        }

        @Test
        void shouldReturnTrue_whenPlainNumberMatch() {
            when(attributeInstance.getValue()).thenReturn(10.0);
            assertTrue(createCondition("strength", "10").check(entity));
        }
    }

    @Nested
    @DisplayName("Check - NotEqual")
    class CheckNotEqual {
        @Test
        void shouldReturnTrue_whenDifferent() {
            when(attributeInstance.getValue()).thenReturn(15.0);
            assertTrue(createCondition("strength", "!=10").check(entity));
        }

        @Test
        void shouldReturnFalse_whenEqual() {
            when(attributeInstance.getValue()).thenReturn(10.0);
            assertFalse(createCondition("strength", "!=10").check(entity));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        @Test
        void shouldReturnFalse_whenNotPlayer() {
            when(entity.isPlayer()).thenReturn(false);
            assertFalse(createCondition("strength", ">0").check(entity));
        }

        @Test
        void shouldReturnFalse_whenProfileNull() {
            when(profileManager.getProfile(any(Player.class))).thenReturn(null);
            assertFalse(createCondition("strength", ">0").check(entity));
        }

        @Test
        void shouldReturnFalse_whenAttributeInstanceNull() {
            when(coreAttributeData.getInstance(anyString())).thenReturn(null);
            assertFalse(createCondition("strength", ">0").check(entity));
        }

        @Test
        void shouldHandleNegativeValues() {
            when(attributeInstance.getValue()).thenReturn(-5.0);
            assertTrue(createCondition("strength", "<0").check(entity));
        }

        @Test
        void shouldHandleDecimalComparison() {
            when(attributeInstance.getValue()).thenReturn(10.5);
            assertTrue(createCondition("strength", ">10.0").check(entity));
        }
    }
}

package me.ray.midgard.modules.mythicmobs.conditions;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.profile.ProfileManager;
import me.ray.midgard.modules.classes.ClassData;
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
class MidgardClassConditionTest {

    @Mock MythicLineConfig config;
    @Mock AbstractEntity entity;
    @Mock Player player;
    @Mock ProfileManager profileManager;
    @Mock MidgardProfile profile;
    @Mock ClassData classData;

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
        when(profile.getOrCreateData(ClassData.class)).thenReturn(classData);
    }

    @AfterEach
    void tearDown() {
        bukkitAdapterStatic.close();
        midgardCoreStatic.close();
    }

    private MidgardClassCondition createCondition(String className) {
        when(config.getString(any(String[].class), anyString())).thenReturn(className);
        return new MidgardClassCondition(config);
    }

    @Nested
    @DisplayName("Class Name Matching")
    class ClassNameMatching {
        @Test
        void shouldReturnTrue_whenClassMatches() {
            when(classData.getClassName()).thenReturn("Warrior");
            assertTrue(createCondition("Warrior").checkEntity(entity));
        }

        @Test
        void shouldReturnTrue_whenCaseInsensitiveMatch() {
            when(classData.getClassName()).thenReturn("warrior");
            assertTrue(createCondition("Warrior").checkEntity(entity));
        }

        @Test
        void shouldReturnTrue_whenUpperCaseMatch() {
            when(classData.getClassName()).thenReturn("WARRIOR");
            assertTrue(createCondition("warrior").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenClassDoesNotMatch() {
            when(classData.getClassName()).thenReturn("Mage");
            assertFalse(createCondition("Warrior").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenClassEmpty() {
            when(classData.getClassName()).thenReturn("");
            assertFalse(createCondition("Warrior").checkEntity(entity));
        }

        @Test
        void shouldHandleSpecialCharacters() {
            when(classData.getClassName()).thenReturn("Dark Knight");
            assertTrue(createCondition("Dark Knight").checkEntity(entity));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        @Test
        void shouldReturnFalse_whenNotPlayer() {
            when(entity.isPlayer()).thenReturn(false);
            assertFalse(createCondition("Warrior").checkEntity(entity));
        }

        @Test
        void shouldReturnFalse_whenProfileNull() {
            when(profileManager.getProfile(any(UUID.class))).thenReturn(null);
            assertFalse(createCondition("Warrior").checkEntity(entity));
        }

        @Test
        void shouldDelegateCheckToCheckEntity() {
            when(classData.getClassName()).thenReturn("Warrior");
            MidgardClassCondition condition = createCondition("Warrior");
            assertEquals(condition.checkEntity(entity), condition.check(entity));
        }

        @Test
        void shouldHandleEmptyConfigClassName() {
            when(classData.getClassName()).thenReturn("");
            // Empty config class and empty player class match
            assertTrue(createCondition("").checkEntity(entity));
        }
    }
}

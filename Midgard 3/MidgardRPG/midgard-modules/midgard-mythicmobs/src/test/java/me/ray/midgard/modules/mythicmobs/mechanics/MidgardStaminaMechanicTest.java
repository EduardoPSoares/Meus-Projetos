package me.ray.midgard.modules.mythicmobs.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MidgardStaminaMechanicTest {

    @Mock MythicLineConfig config;
    @Mock AbstractEntity target;
    @Mock SkillMetadata skillMetadata;
    @Mock PlaceholderDouble placeholderAmount;
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

        when(target.isPlayer()).thenReturn(true);
        bukkitAdapterStatic.when(() -> BukkitAdapter.adapt(target)).thenReturn(player);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        midgardCoreStatic.when(MidgardCore::getProfileManager).thenReturn(profileManager);
        when(profileManager.getProfile(any(UUID.class))).thenReturn(profile);
        when(profile.getOrCreateData(CombatData.class)).thenReturn(combatData);
        when(profile.getOrCreateData(CoreAttributeData.class)).thenReturn(coreAttributeData);
        when(coreAttributeData.getInstance(CombatAttributes.MAX_STAMINA)).thenReturn(maxStaminaAttr);
        when(maxStaminaAttr.getValue()).thenReturn(100.0);
        when(combatData.getCurrentStamina()).thenReturn(50.0);
        when(placeholderAmount.get(any(), any())).thenReturn(20.0);
    }

    @AfterEach
    void tearDown() {
        bukkitAdapterStatic.close();
        midgardCoreStatic.close();
    }

    private MidgardStaminaMechanic createMechanic(String mode) {
        when(config.getPlaceholderDouble(any(String[].class), anyDouble())).thenReturn(placeholderAmount);
        when(config.getString(any(String[].class), eq("GIVE"))).thenReturn(mode);
        return new MidgardStaminaMechanic(config);
    }

    @Nested
    @DisplayName("GIVE/ADD Mode")
    class GiveMode {
        @Test
        void shouldAddStamina() {
            MidgardStaminaMechanic mechanic = createMechanic("GIVE");
            SkillResult result = mechanic.castAtEntity(skillMetadata, target);
            assertEquals(SkillResult.SUCCESS, result);
            verify(combatData).setCurrentStamina(70.0); // 50 + 20
        }

        @Test
        void shouldAddStaminaWithAddAlias() {
            MidgardStaminaMechanic mechanic = createMechanic("ADD");
            mechanic.castAtEntity(skillMetadata, target);
            verify(combatData).setCurrentStamina(70.0);
        }

        @Test
        void shouldClampToMaxStamina() {
            when(combatData.getCurrentStamina()).thenReturn(90.0);
            MidgardStaminaMechanic mechanic = createMechanic("GIVE");
            mechanic.castAtEntity(skillMetadata, target);
            verify(combatData).setCurrentStamina(100.0);
        }
    }

    @Nested
    @DisplayName("TAKE/SUBTRACT Mode")
    class TakeMode {
        @Test
        void shouldSubtractStamina() {
            MidgardStaminaMechanic mechanic = createMechanic("TAKE");
            mechanic.castAtEntity(skillMetadata, target);
            verify(combatData).setCurrentStamina(30.0);
        }

        @Test
        void shouldSubtractStaminaWithSubtractAlias() {
            MidgardStaminaMechanic mechanic = createMechanic("SUBTRACT");
            mechanic.castAtEntity(skillMetadata, target);
            verify(combatData).setCurrentStamina(30.0);
        }

        @Test
        void shouldSubtractStaminaWithRemoveAlias() {
            MidgardStaminaMechanic mechanic = createMechanic("REMOVE");
            mechanic.castAtEntity(skillMetadata, target);
            verify(combatData).setCurrentStamina(30.0);
        }

        @Test
        void shouldClampToZero() {
            when(combatData.getCurrentStamina()).thenReturn(10.0);
            MidgardStaminaMechanic mechanic = createMechanic("TAKE");
            mechanic.castAtEntity(skillMetadata, target);
            verify(combatData).setCurrentStamina(0.0);
        }
    }

    @Nested
    @DisplayName("SET Mode")
    class SetMode {
        @Test
        void shouldSetStaminaDirectly() {
            when(placeholderAmount.get(any(), any())).thenReturn(75.0);
            MidgardStaminaMechanic mechanic = createMechanic("SET");
            mechanic.castAtEntity(skillMetadata, target);
            verify(combatData).setCurrentStamina(75.0);
        }

        @Test
        void shouldClampSetToMax() {
            when(placeholderAmount.get(any(), any())).thenReturn(150.0);
            MidgardStaminaMechanic mechanic = createMechanic("SET");
            mechanic.castAtEntity(skillMetadata, target);
            verify(combatData).setCurrentStamina(100.0);
        }

        @Test
        void shouldClampSetToZero() {
            when(placeholderAmount.get(any(), any())).thenReturn(-10.0);
            MidgardStaminaMechanic mechanic = createMechanic("SET");
            mechanic.castAtEntity(skillMetadata, target);
            verify(combatData).setCurrentStamina(0.0);
        }
    }

    @Nested
    @DisplayName("Invalid Target")
    class InvalidTarget {
        @Test
        void shouldReturnInvalidTarget_whenNotPlayer() {
            when(target.isPlayer()).thenReturn(false);
            assertEquals(SkillResult.INVALID_TARGET, createMechanic("GIVE").castAtEntity(skillMetadata, target));
        }

        @Test
        void shouldReturnInvalidTarget_whenProfileNull() {
            when(profileManager.getProfile(any(UUID.class))).thenReturn(null);
            assertEquals(SkillResult.INVALID_TARGET, createMechanic("GIVE").castAtEntity(skillMetadata, target));
        }
    }

    @Nested
    @DisplayName("Max Stamina Edge Cases")
    class MaxEdgeCases {
        @Test
        void shouldUseDefaultMax_whenAttrNull() {
            when(coreAttributeData.getInstance(CombatAttributes.MAX_STAMINA)).thenReturn(null);
            when(combatData.getCurrentStamina()).thenReturn(90.0);
            MidgardStaminaMechanic mechanic = createMechanic("GIVE");
            mechanic.castAtEntity(skillMetadata, target);
            verify(combatData).setCurrentStamina(100.0); // default max 100
        }
    }
}

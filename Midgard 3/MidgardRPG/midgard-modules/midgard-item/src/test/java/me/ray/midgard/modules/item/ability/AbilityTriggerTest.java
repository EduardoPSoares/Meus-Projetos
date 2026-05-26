package me.ray.midgard.modules.item.ability;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class AbilityTriggerTest {

    @Test
    void shouldHaveAllExpectedValues() {
        assertEquals(12, AbilityTrigger.values().length);
    }

    @Nested
    class FromString {

        @ParameterizedTest
        @EnumSource(AbilityTrigger.class)
        void shouldReturnSelf_forExactName(AbilityTrigger trigger) {
            assertEquals(trigger, AbilityTrigger.fromString(trigger.name()));
        }

        @ParameterizedTest
        @NullAndEmptySource
        void shouldReturnLeftClick_forNullOrEmpty(String input) {
            assertEquals(AbilityTrigger.LEFT_CLICK, AbilityTrigger.fromString(input));
        }

        // LEFT_CLICK aliases
        @ParameterizedTest
        @ValueSource(strings = {"LEFT_CLICK", "LEFTCLICK", "SWING", "ATTACK", "left_click", "swing", "attack"})
        void shouldReturnLeftClick(String input) {
            assertEquals(AbilityTrigger.LEFT_CLICK, AbilityTrigger.fromString(input));
        }

        // RIGHT_CLICK aliases
        @ParameterizedTest
        @ValueSource(strings = {"RIGHT_CLICK", "RIGHTCLICK", "USE", "INTERACT", "right_click", "use"})
        void shouldReturnRightClick(String input) {
            assertEquals(AbilityTrigger.RIGHT_CLICK, AbilityTrigger.fromString(input));
        }

        // SHIFT_LEFT_CLICK aliases
        @ParameterizedTest
        @ValueSource(strings = {"SHIFT_LEFT_CLICK", "SHIFTLEFTCLICK", "SNEAK_LEFT_CLICK"})
        void shouldReturnShiftLeftClick(String input) {
            assertEquals(AbilityTrigger.SHIFT_LEFT_CLICK, AbilityTrigger.fromString(input));
        }

        // SHIFT_RIGHT_CLICK aliases
        @ParameterizedTest
        @ValueSource(strings = {"SHIFT_RIGHT_CLICK", "SHIFTRIGHTCLICK", "SNEAK_RIGHT_CLICK"})
        void shouldReturnShiftRightClick(String input) {
            assertEquals(AbilityTrigger.SHIFT_RIGHT_CLICK, AbilityTrigger.fromString(input));
        }

        // SNEAK aliases
        @ParameterizedTest
        @ValueSource(strings = {"SNEAK", "CROUCH", "SHIFT", "sneak", "crouch"})
        void shouldReturnSneak(String input) {
            assertEquals(AbilityTrigger.SNEAK, AbilityTrigger.fromString(input));
        }

        // ON_EQUIP aliases
        @ParameterizedTest
        @ValueSource(strings = {"ON_EQUIP", "EQUIP", "equip"})
        void shouldReturnOnEquip(String input) {
            assertEquals(AbilityTrigger.ON_EQUIP, AbilityTrigger.fromString(input));
        }

        // ON_UNEQUIP aliases
        @ParameterizedTest
        @ValueSource(strings = {"ON_UNEQUIP", "UNEQUIP"})
        void shouldReturnOnUnequip(String input) {
            assertEquals(AbilityTrigger.ON_UNEQUIP, AbilityTrigger.fromString(input));
        }

        // PASSIVE_TIMER aliases
        @ParameterizedTest
        @ValueSource(strings = {"TIMER", "PASSIVE_TIMER", "PASSIVE", "timer", "passive"})
        void shouldReturnPassiveTimer(String input) {
            assertEquals(AbilityTrigger.PASSIVE_TIMER, AbilityTrigger.fromString(input));
        }

        // ON_DAMAGE_TAKEN aliases
        @ParameterizedTest
        @ValueSource(strings = {"ON_DAMAGE_TAKEN", "WHEN_HIT", "DAMAGED"})
        void shouldReturnOnDamageTaken(String input) {
            assertEquals(AbilityTrigger.ON_DAMAGE_TAKEN, AbilityTrigger.fromString(input));
        }

        // ON_DAMAGE_DEALT aliases
        @ParameterizedTest
        @ValueSource(strings = {"ON_DAMAGE_DEALT", "WHEN_ATTACK", "HIT"})
        void shouldReturnOnDamageDealt(String input) {
            assertEquals(AbilityTrigger.ON_DAMAGE_DEALT, AbilityTrigger.fromString(input));
        }

        // ON_KILL aliases
        @ParameterizedTest
        @ValueSource(strings = {"ON_KILL", "KILL"})
        void shouldReturnOnKill(String input) {
            assertEquals(AbilityTrigger.ON_KILL, AbilityTrigger.fromString(input));
        }

        @Test
        void shouldReturnUnknown_forUnrecognizedInput() {
            assertEquals(AbilityTrigger.UNKNOWN, AbilityTrigger.fromString("TOTALLY_INVALID"));
        }

        @Test
        void shouldHandleHyphens() {
            assertEquals(AbilityTrigger.LEFT_CLICK, AbilityTrigger.fromString("left-click"));
        }

        @Test
        void shouldHandleSpaces() {
            assertEquals(AbilityTrigger.RIGHT_CLICK, AbilityTrigger.fromString("right click"));
        }
    }

    @Nested
    class InteractionTrigger {

        @ParameterizedTest
        @EnumSource(value = AbilityTrigger.class, names = {"LEFT_CLICK", "RIGHT_CLICK", "SHIFT_LEFT_CLICK", "SHIFT_RIGHT_CLICK"})
        void shouldReturnTrue_forInteractionTriggers(AbilityTrigger trigger) {
            assertTrue(trigger.isInteractionTrigger());
        }

        @ParameterizedTest
        @EnumSource(value = AbilityTrigger.class, names = {"SNEAK", "ON_EQUIP", "ON_UNEQUIP", "PASSIVE_TIMER", "ON_DAMAGE_TAKEN", "ON_DAMAGE_DEALT", "ON_KILL", "UNKNOWN"})
        void shouldReturnFalse_forNonInteractionTriggers(AbilityTrigger trigger) {
            assertFalse(trigger.isInteractionTrigger());
        }
    }

    @Nested
    class CombatTrigger {

        @ParameterizedTest
        @EnumSource(value = AbilityTrigger.class, names = {"ON_DAMAGE_TAKEN", "ON_DAMAGE_DEALT", "ON_KILL"})
        void shouldReturnTrue_forCombatTriggers(AbilityTrigger trigger) {
            assertTrue(trigger.isCombatTrigger());
        }

        @ParameterizedTest
        @EnumSource(value = AbilityTrigger.class, names = {"LEFT_CLICK", "RIGHT_CLICK", "SHIFT_LEFT_CLICK", "SHIFT_RIGHT_CLICK", "SNEAK", "ON_EQUIP", "ON_UNEQUIP", "PASSIVE_TIMER", "UNKNOWN"})
        void shouldReturnFalse_forNonCombatTriggers(AbilityTrigger trigger) {
            assertFalse(trigger.isCombatTrigger());
        }
    }
}

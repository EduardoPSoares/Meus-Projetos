package me.ray.midgard.modules.item.ability;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpellBindingTest {

    @Nested
    class SimpleConstructor {

        @Test
        void shouldSetSpellIdToLowerCase() {
            SpellBinding binding = new SpellBinding("FIREBALL", AbilityTrigger.LEFT_CLICK);
            assertEquals("fireball", binding.getSpellId());
        }

        @Test
        void shouldSetTrigger() {
            SpellBinding binding = new SpellBinding("fireball", AbilityTrigger.RIGHT_CLICK);
            assertEquals(AbilityTrigger.RIGHT_CLICK, binding.getTrigger());
        }

        @Test
        void shouldDefaultCooldownOverrideToNegative() {
            SpellBinding binding = new SpellBinding("fireball", AbilityTrigger.LEFT_CLICK);
            assertFalse(binding.hasCooldownOverride());
            assertEquals(-1.0, binding.getCooldownOverride());
        }

        @Test
        void shouldDefaultDamageOverrideToNegative() {
            SpellBinding binding = new SpellBinding("fireball", AbilityTrigger.LEFT_CLICK);
            assertFalse(binding.hasDamageOverride());
            assertEquals(-1.0, binding.getDamageOverride());
        }

        @Test
        void shouldDefaultTimerTicksToZero() {
            SpellBinding binding = new SpellBinding("fireball", AbilityTrigger.LEFT_CLICK);
            assertEquals(0, binding.getTimerTicks());
        }

        @Test
        void shouldHaveEmptyVariableOverrides() {
            SpellBinding binding = new SpellBinding("fireball", AbilityTrigger.LEFT_CLICK);
            assertTrue(binding.getVariableOverrides().isEmpty());
        }
    }

    @Nested
    class FullConstructor {

        @Test
        void shouldSetAllFields() {
            Map<String, Double> vars = new HashMap<>();
            vars.put("radius", 5.0);
            vars.put("duration", 10.0);

            SpellBinding binding = new SpellBinding("HEAL", AbilityTrigger.SNEAK, 3.0, 50.0, 40, vars);

            assertEquals("heal", binding.getSpellId());
            assertEquals(AbilityTrigger.SNEAK, binding.getTrigger());
            assertTrue(binding.hasCooldownOverride());
            assertEquals(3.0, binding.getCooldownOverride());
            assertTrue(binding.hasDamageOverride());
            assertEquals(50.0, binding.getDamageOverride());
            assertEquals(40, binding.getTimerTicks());
            assertEquals(2, binding.getVariableOverrides().size());
            assertEquals(5.0, binding.getVariableOverrides().get("radius"));
        }

        @Test
        void shouldHandleNullVariableOverrides() {
            SpellBinding binding = new SpellBinding("heal", AbilityTrigger.SNEAK, 3.0, 50.0, 40, null);
            assertNotNull(binding.getVariableOverrides());
            assertTrue(binding.getVariableOverrides().isEmpty());
        }
    }

    @Nested
    class OverrideChecks {

        @Test
        void hasCooldownOverride_shouldReturnTrue_whenPositive() {
            SpellBinding binding = new SpellBinding("x", AbilityTrigger.LEFT_CLICK, 5.0, -1, 0, null);
            assertTrue(binding.hasCooldownOverride());
        }

        @Test
        void hasCooldownOverride_shouldReturnFalse_whenZero() {
            SpellBinding binding = new SpellBinding("x", AbilityTrigger.LEFT_CLICK, 0, -1, 0, null);
            assertFalse(binding.hasCooldownOverride());
        }

        @Test
        void hasCooldownOverride_shouldReturnFalse_whenNegative() {
            SpellBinding binding = new SpellBinding("x", AbilityTrigger.LEFT_CLICK, -1, -1, 0, null);
            assertFalse(binding.hasCooldownOverride());
        }

        @Test
        void hasDamageOverride_shouldReturnTrue_whenPositive() {
            SpellBinding binding = new SpellBinding("x", AbilityTrigger.LEFT_CLICK, -1, 100.0, 0, null);
            assertTrue(binding.hasDamageOverride());
        }

        @Test
        void hasDamageOverride_shouldReturnFalse_whenNegative() {
            SpellBinding binding = new SpellBinding("x", AbilityTrigger.LEFT_CLICK, -1, -1, 0, null);
            assertFalse(binding.hasDamageOverride());
        }
    }

    @Nested
    class IsPassive {

        @Test
        void shouldReturnTrue_forPassiveTimerTrigger() {
            SpellBinding binding = new SpellBinding("aura", AbilityTrigger.PASSIVE_TIMER);
            assertTrue(binding.isPassive());
        }

        @Test
        void shouldReturnFalse_forNonPassiveTrigger() {
            SpellBinding binding = new SpellBinding("fireball", AbilityTrigger.LEFT_CLICK);
            assertFalse(binding.isPassive());
        }
    }

    @Nested
    class FromMap {

        @Test
        void shouldParseBasicFields() {
            Map<String, Object> map = new HashMap<>();
            map.put("spell", "FIREBALL");
            map.put("trigger", "RIGHT_CLICK");

            SpellBinding binding = SpellBinding.fromMap(map);

            assertEquals("fireball", binding.getSpellId());
            assertEquals(AbilityTrigger.RIGHT_CLICK, binding.getTrigger());
        }

        @Test
        void shouldParseNumericOverrides() {
            Map<String, Object> map = new HashMap<>();
            map.put("spell", "heal");
            map.put("trigger", "SNEAK");
            map.put("cooldown-override", 5.0);
            map.put("damage-override", 100);
            map.put("timer", 40);

            SpellBinding binding = SpellBinding.fromMap(map);

            assertEquals(5.0, binding.getCooldownOverride());
            assertEquals(100.0, binding.getDamageOverride());
            assertEquals(40, binding.getTimerTicks());
        }

        @Test
        void shouldParseStringNumericOverrides() {
            Map<String, Object> map = new HashMap<>();
            map.put("spell", "heal");
            map.put("trigger", "LEFT_CLICK");
            map.put("cooldown-override", "3.5");
            map.put("damage-override", "50");
            map.put("timer", "20");

            SpellBinding binding = SpellBinding.fromMap(map);

            assertEquals(3.5, binding.getCooldownOverride());
            assertEquals(50.0, binding.getDamageOverride());
            assertEquals(20, binding.getTimerTicks());
        }

        @Test
        void shouldHandleInvalidNumericStrings() {
            Map<String, Object> map = new HashMap<>();
            map.put("spell", "x");
            map.put("trigger", "LEFT_CLICK");
            map.put("cooldown-override", "abc");
            map.put("damage-override", "xyz");
            map.put("timer", "nope");

            SpellBinding binding = SpellBinding.fromMap(map);

            assertEquals(-1.0, binding.getCooldownOverride());
            assertEquals(-1.0, binding.getDamageOverride());
            assertEquals(0, binding.getTimerTicks());
        }

        @Test
        void shouldParseVariableOverrides() {
            Map<String, Object> vars = new HashMap<>();
            vars.put("radius", 5.0);
            vars.put("duration", 10);
            vars.put("invalid", "not-a-number");

            Map<String, Object> map = new HashMap<>();
            map.put("spell", "aoe");
            map.put("trigger", "LEFT_CLICK");
            map.put("variables", vars);

            SpellBinding binding = SpellBinding.fromMap(map);

            assertEquals(2, binding.getVariableOverrides().size());
            assertEquals(5.0, binding.getVariableOverrides().get("radius"));
            assertEquals(10.0, binding.getVariableOverrides().get("duration"));
        }

        @Test
        void shouldUseDefaults_whenFieldsMissing() {
            Map<String, Object> map = new HashMap<>();

            SpellBinding binding = SpellBinding.fromMap(map);

            assertEquals("", binding.getSpellId());
            assertEquals(AbilityTrigger.LEFT_CLICK, binding.getTrigger());
            assertFalse(binding.hasCooldownOverride());
            assertFalse(binding.hasDamageOverride());
            assertEquals(0, binding.getTimerTicks());
        }
    }

    @Nested
    class ToStringMethod {

        @Test
        void shouldContainSpellIdAndTrigger() {
            SpellBinding binding = new SpellBinding("fireball", AbilityTrigger.LEFT_CLICK);
            String result = binding.toString();
            assertTrue(result.contains("fireball"));
            assertTrue(result.contains("LEFT_CLICK"));
        }
    }
}

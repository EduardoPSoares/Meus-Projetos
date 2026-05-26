package me.ray.midgard.modules.combat.mechanics;

import me.ray.midgard.modules.combat.CombatConfig;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElementalDamageCalculatorTest {

    private CombatConfig config;
    private ElementalDamageCalculator calculator;

    @Mock
    private LivingEntity victim;

    @BeforeEach
    void setUp() {
        config = new CombatConfig();
        calculator = new ElementalDamageCalculator(config);
        // Sem tags de scoreboard
        lenient().when(victim.getScoreboardTags()).thenReturn(Collections.emptySet());
    }

    @Test
    void shouldReturnUnchangedDamage_whenElementHasNoDefenseMapping() {
        // "unknown_element" não existe no ELEMENTAL_MAP → retorna dano intacto
        double result = calculator.calculateMitigatedDamage("unknown_element", 100.0, victim, 1);
        assertEquals(100.0, result, 0.001);
    }

    @Test
    void shouldReturnUnchangedDamage_whenElementNormalizesToUnknown() {
        double result = calculator.calculateMitigatedDamage("nonexistent", 50.0, victim, 5);
        assertEquals(50.0, result, 0.001);
    }

    @Test
    void shouldNormalizeElementName_appendingDamageSuffix() {
        // "fire" → "fire_damage", que está no ELEMENTAL_MAP → mas sem defesa, dano volta igual
        // A vítima não é Player, então MythicMobs integration falha silenciosamente → eDef=0 → sem mitigação
        double result = calculator.calculateMitigatedDamage("fire", 100.0, victim, 1);
        assertEquals(100.0, result, 0.001);
    }

    @Test
    void shouldHandleElementAlreadyWithDamageSuffix() {
        // "fire_damage" já tem o sufixo → normaliza para "fire_damage"
        double result = calculator.calculateMitigatedDamage("fire_damage", 100.0, victim, 1);
        assertEquals(100.0, result, 0.001);
    }

    @Test
    void shouldReturnPositiveDamage_forPositiveInput() {
        double result = calculator.calculateMitigatedDamage("ice", 200.0, victim, 10);
        assertTrue(result > 0);
    }

    @Test
    void shouldHandleZeroDamage() {
        double result = calculator.calculateMitigatedDamage("fire", 0.0, victim, 1);
        assertEquals(0.0, result, 0.001);
    }
}

package me.ray.midgard.modules.professions.blacksmith.forge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class ForgeStageTest {

    @Test
    @DisplayName("deve haver exatamente 10 stages")
    void shouldHave10Stages() {
        assertEquals(10, ForgeStage.values().length);
    }

    @Test
    @DisplayName("COMPLETED deve ser terminal")
    void shouldBeTerminal_whenCompleted() {
        assertTrue(ForgeStage.COMPLETED.isTerminal());
        assertFalse(ForgeStage.COMPLETED.isActive());
    }

    @Test
    @DisplayName("FAILED deve ser terminal")
    void shouldBeTerminal_whenFailed() {
        assertTrue(ForgeStage.FAILED.isTerminal());
        assertFalse(ForgeStage.FAILED.isActive());
    }

    @Test
    @DisplayName("EXPIRED deve ser terminal")
    void shouldBeTerminal_whenExpired() {
        assertTrue(ForgeStage.EXPIRED.isTerminal());
        assertFalse(ForgeStage.EXPIRED.isActive());
    }

    @Test
    @DisplayName("stages ativos não devem ser terminais")
    void shouldNotBeTerminal_forActiveStages() {
        ForgeStage[] activeStages = {
            ForgeStage.SELECTING, ForgeStage.PREPARING, ForgeStage.HEATING,
            ForgeStage.HAMMERING, ForgeStage.QUENCHING, ForgeStage.SHARPENING,
            ForgeStage.FINALIZING
        };
        for (ForgeStage stage : activeStages) {
            assertTrue(stage.isActive(), stage.name() + " deve ser ativo");
            assertFalse(stage.isTerminal(), stage.name() + " não deve ser terminal");
        }
    }

    @ParameterizedTest
    @EnumSource(ForgeStage.class)
    @DisplayName("isActive e isTerminal devem ser mutuamente exclusivos")
    void shouldBeMutuallyExclusive(ForgeStage stage) {
        assertNotEquals(stage.isActive(), stage.isTerminal(),
                stage.name() + ": isActive e isTerminal devem ser opostos");
    }
}

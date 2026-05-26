package me.ray.midgard.modules.professions.blacksmith.forge.session;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeStage;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.quality.MaterialGrade;
import me.ray.midgard.modules.professions.blacksmith.forge.quality.QualityTier;
import me.ray.midgard.modules.professions.blacksmith.forge.recipe.ForgeRecipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ForgeSessionTest {

    private ForgeSession session;
    private ForgeRecipe recipe;
    private UUID playerId;
    private UUID forgeId;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        forgeId = UUID.randomUUID();
        recipe = new ForgeRecipe("test_recipe")
                .setIdealTempMin(800.0)
                .setIdealTempMax(1200.0)
                .setHammerStrikes(10)
                .setSharpeningPasses(3);
        session = new ForgeSession(playerId, forgeId, recipe);
    }

    // ── Valores iniciais ──

    @Test
    @DisplayName("deve inicializar com valores corretos")
    void shouldInitializeCorrectly() {
        assertNotNull(session.getSessionId());
        assertEquals(playerId, session.getPlayerId());
        assertEquals(forgeId, session.getForgeId());
        assertSame(recipe, session.getRecipe());
        assertEquals(ForgeStage.SELECTING, session.getCurrentStage());
        assertTrue(session.isActive());
    }

    // ── Stage management ──

    @Test
    @DisplayName("advanceToNextStage deve seguir sequência correta")
    void shouldFollowCorrectSequence() {
        assertEquals(ForgeStage.SELECTING, session.getCurrentStage());
        session.advanceToNextStage();
        assertEquals(ForgeStage.PREPARING, session.getCurrentStage());
        session.advanceToNextStage();
        assertEquals(ForgeStage.HEATING, session.getCurrentStage());
        session.advanceToNextStage();
        assertEquals(ForgeStage.HAMMERING, session.getCurrentStage());
        session.advanceToNextStage();
        assertEquals(ForgeStage.QUENCHING, session.getCurrentStage());
        session.advanceToNextStage();
        assertEquals(ForgeStage.SHARPENING, session.getCurrentStage());
        session.advanceToNextStage();
        assertEquals(ForgeStage.FINALIZING, session.getCurrentStage());
        session.advanceToNextStage();
        assertEquals(ForgeStage.COMPLETED, session.getCurrentStage());
    }

    @Test
    @DisplayName("setCurrentStage deve definir stage diretamente")
    void shouldSetStageDIrectly() {
        session.setCurrentStage(ForgeStage.HAMMERING);
        assertEquals(ForgeStage.HAMMERING, session.getCurrentStage());
    }

    @Test
    @DisplayName("advanceToNextStage de COMPLETED não deve mudar")
    void shouldNotAdvance_fromCompleted() {
        session.setCurrentStage(ForgeStage.COMPLETED);
        session.advanceToNextStage();
        assertEquals(ForgeStage.COMPLETED, session.getCurrentStage());
    }

    @Test
    @DisplayName("advanceToNextStage de FAILED não deve mudar")
    void shouldNotAdvance_fromFailed() {
        session.setCurrentStage(ForgeStage.FAILED);
        session.advanceToNextStage();
        assertEquals(ForgeStage.FAILED, session.getCurrentStage());
    }

    // ── isActive / isExpired ──

    @Test
    @DisplayName("isActive deve ser true para stages ativos não expirados")
    void shouldBeActive_whenStageIsActiveAndNotExpired() {
        assertTrue(session.isActive());
    }

    @Test
    @DisplayName("isActive deve ser false para stages terminais")
    void shouldNotBeActive_whenTerminal() {
        session.setCurrentStage(ForgeStage.COMPLETED);
        assertFalse(session.isActive());
    }

    @Test
    @DisplayName("getRemainingTime deve ser positivo imediatamente")
    void shouldHavePositiveRemainingTime() {
        assertTrue(session.getRemainingTime() > 0);
    }

    // ── Material quality ──

    @Test
    @DisplayName("setMaterialGrade deve atualizar qualityMultiplier")
    void shouldUpdateQualityFromGrade() {
        session.setMaterialGrade(MaterialGrade.PRISTINE);
        assertEquals(MaterialGrade.PRISTINE, session.getMaterialGrade());
        assertEquals(1.0, session.getMaterialQuality(), 0.001);
    }

    @Test
    @DisplayName("default material grade deve ser REFINED (0.8)")
    void shouldHaveDefaultGradeRefined() {
        assertEquals(MaterialGrade.REFINED, session.getMaterialGrade());
        assertEquals(0.8, session.getMaterialQuality(), 0.001);
    }

    @Test
    @DisplayName("materialsConsumed deve ser gerenciável")
    void shouldTrackMaterialsConsumed() {
        assertFalse(session.isMaterialsConsumed());
        session.setMaterialsConsumed(true);
        assertTrue(session.isMaterialsConsumed());
    }

    // ── Heating score ──

    @Test
    @DisplayName("calculateHeatingScore: temp no centro do ideal deve dar score ~ 1.0")
    void shouldGiveHighScore_whenTempIsCentered() {
        // idealMin=800, idealMax=1200, center=1000
        session.calculateHeatingScore(1000);
        assertEquals(1.0, session.getHeatingScore(), 0.05);
    }

    @Test
    @DisplayName("calculateHeatingScore: temp no limite inferior do range")
    void shouldGiveLowerScore_whenAtMinTemp() {
        session.calculateHeatingScore(800);
        // distFromCenter = 200, idealHalfRange = 200
        // score = 1.0 - (200/200)*0.3 = 0.7
        assertEquals(0.7, session.getHeatingScore(), 0.01);
    }

    @Test
    @DisplayName("calculateHeatingScore: temp abaixo do ideal")
    void shouldReduceScore_whenTooGold() {
        session.calculateHeatingScore(600);
        // deficit = 800 - 600 = 200
        // score = max(0.1, 0.7 - 200/200) = max(0.1, -0.3) = 0.1
        assertTrue(session.getHeatingScore() >= 0.1);
        assertTrue(session.getHeatingScore() < 0.7);
    }

    @Test
    @DisplayName("calculateHeatingScore: temp muito acima (>300 excess) deve falhar")
    void shouldFail_whenTempWayTooHigh() {
        session.calculateHeatingScore(1501); // excess = 301
        assertEquals(0.0, session.getHeatingScore(), 0.001);
        assertEquals(ForgeStage.FAILED, session.getCurrentStage());
    }

    @Test
    @DisplayName("calculateHeatingScore: temp um pouco acima do ideal")
    void shouldReduceScore_whenSlightlyTooHot() {
        session.calculateHeatingScore(1300);
        // excess = 100
        // score = max(0.1, 0.5 - 100/400) = max(0.1, 0.25) = 0.25
        assertEquals(0.25, session.getHeatingScore(), 0.01);
    }

    // ── Hammering ──

    @Test
    @DisplayName("recordHammerStrike: PERFECT deve dar +3 progresso")
    void shouldAddProgress_forPerfectStrike() {
        session.recordHammerStrike(ForgeSession.StrikeResult.PERFECT);
        assertEquals(1, session.getTotalStrikes());
        assertEquals(1, session.getPerfectStrikes());
        assertEquals(3, session.getHammeringProgress());
    }

    @Test
    @DisplayName("recordHammerStrike: GOOD deve dar +2 progresso")
    void shouldAddProgress_forGoodStrike() {
        session.recordHammerStrike(ForgeSession.StrikeResult.GOOD);
        assertEquals(1, session.getGoodStrikes());
        assertEquals(2, session.getHammeringProgress());
    }

    @Test
    @DisplayName("recordHammerStrike: MISS deve dar +1 progresso")
    void shouldAddProgress_forMiss() {
        session.recordHammerStrike(ForgeSession.StrikeResult.MISS);
        assertEquals(1, session.getMissedStrikes());
        assertEquals(1, session.getHammeringProgress());
    }

    @Test
    @DisplayName("calculateHammeringScore: todos perfeitos deve dar score alto")
    void shouldGiveHighHammeringScore_whenAllPerfect() {
        for (int i = 0; i < 10; i++) {
            session.recordHammerStrike(ForgeSession.StrikeResult.PERFECT);
        }
        session.calculateHammeringScore();
        // score = (10*3)/(10*3) + 0.03 + 0.05 = 1.08 → clamped to 1.0
        assertEquals(1.0, session.getHammeringScore(), 0.001);
    }

    @Test
    @DisplayName("calculateHammeringScore: todos miss deve dar score baixo")
    void shouldGiveLowHammeringScore_whenAllMiss() {
        for (int i = 0; i < 10; i++) {
            session.recordHammerStrike(ForgeSession.StrikeResult.MISS);
        }
        session.calculateHammeringScore();
        // score = (0 + 0) / (10*3) = 0
        assertEquals(0.0, session.getHammeringScore(), 0.001);
    }

    @Test
    @DisplayName("calculateHammeringScore: sem strikes deve dar 0")
    void shouldGiveZeroScore_whenNoStrikes() {
        session.calculateHammeringScore();
        assertEquals(0, session.getHammeringScore(), 0.001);
    }

    @Test
    @DisplayName("calculateHammeringScore: 5+ perfects deve ter bonus")
    void shouldHaveBonus_for5Perfects() {
        for (int i = 0; i < 5; i++) {
            session.recordHammerStrike(ForgeSession.StrikeResult.PERFECT);
        }
        for (int i = 0; i < 5; i++) {
            session.recordHammerStrike(ForgeSession.StrikeResult.MISS);
        }
        session.calculateHammeringScore();
        // base = (5*3) / (10*3) = 0.5, bonus: +0.03 (5+ perfects) = 0.53
        assertEquals(0.53, session.getHammeringScore(), 0.01);
    }

    // ── Quenching ──

    @Test
    @DisplayName("setQuenchingScore deve clampar entre 0 e 1")
    void shouldClampQuenchingScore() {
        session.setQuenchingScore(1.5);
        assertEquals(1.0, session.getQuenchingScore(), 0.001);

        session.setQuenchingScore(-0.5);
        assertEquals(0.0, session.getQuenchingScore(), 0.001);
    }

    // ── Sharpening ──

    @Test
    @DisplayName("setSharpeningScore deve clampar entre 0 e 1")
    void shouldClampSharpeningScore() {
        session.setSharpeningScore(2.0);
        assertEquals(1.0, session.getSharpeningScore(), 0.001);
    }

    @Test
    @DisplayName("incrementCompletedPasses deve incrementar")
    void shouldIncrementPasses() {
        session.incrementCompletedPasses();
        session.incrementCompletedPasses();
        assertEquals(2, session.getCompletedPasses());
    }

    // ── Final results ──

    @Test
    @DisplayName("deve armazenar resultados finais")
    void shouldStoreFinalResults() {
        session.setFinalQualityScore(0.85);
        session.setQualityTier(QualityTier.EXCEPTIONAL);
        session.setXpGained(250.0);

        assertEquals(0.85, session.getFinalQualityScore(), 0.001);
        assertEquals(QualityTier.EXCEPTIONAL, session.getQualityTier());
        assertEquals(250.0, session.getXpGained(), 0.001);
    }

    // ── StrikeResult enum ──

    @Test
    @DisplayName("StrikeResult deve ter 3 valores")
    void shouldHave3StrikeResults() {
        assertEquals(3, ForgeSession.StrikeResult.values().length);
    }

    // ── Heating state ──

    @Test
    @DisplayName("temperatura e estado de aquecimento devem ser gerenciáveis")
    void shouldManageHeatingState() {
        session.setCurrentTemperature(500.0);
        assertEquals(500.0, session.getCurrentTemperature(), 0.001);

        session.setMetalHeated(true);
        assertTrue(session.isMetalHeated());
    }
}

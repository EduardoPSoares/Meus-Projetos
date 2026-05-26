package me.ray.midgard.modules.spells.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpellProfileTest {

    private SpellProfile profile;

    @BeforeEach
    void setUp() {
        profile = new SpellProfile();
    }

    // ====== CASTING STYLE ======

    @Test
    @DisplayName("CastingStyle padrão deve ser SKILLBAR")
    void shouldDefaultToSkillbar() {
        assertEquals(SpellProfile.CastingStyle.SKILLBAR, profile.getCastingStyle());
    }

    @Test
    @DisplayName("Deve alterar CastingStyle")
    void shouldChangeCastingStyle() {
        profile.setCastingStyle(SpellProfile.CastingStyle.COMBO);
        assertEquals(SpellProfile.CastingStyle.COMBO, profile.getCastingStyle());
    }

    @Test
    @DisplayName("CastingStyle enum deve ter 2 valores")
    void shouldHaveTwoCastingStyles() {
        assertEquals(2, SpellProfile.CastingStyle.values().length);
    }

    // ====== SKILL BAR ======

    @Nested
    @DisplayName("SkillBar")
    class SkillBarTests {

        @Test
        @DisplayName("Slot vazio retorna null")
        void shouldReturnNull_forEmptySlot() {
            assertNull(profile.getSkillInSlot(1));
        }

        @Test
        @DisplayName("Deve setar e recuperar spell no slot")
        void shouldSetAndGetSpell() {
            profile.setSkillBarSlot(1, "fireball");
            assertEquals("fireball", profile.getSkillInSlot(1));
        }

        @Test
        @DisplayName("Setar null remove spell do slot")
        void shouldRemoveSpell_whenNull() {
            profile.setSkillBarSlot(1, "fireball");
            profile.setSkillBarSlot(1, null);
            assertNull(profile.getSkillInSlot(1));
        }

        @Test
        @DisplayName("Mesma spell muda de slot (deduplicação)")
        void shouldDeduplicateSpell_acrossSlots() {
            profile.setSkillBarSlot(1, "fireball");
            profile.setSkillBarSlot(3, "fireball");
            assertNull(profile.getSkillInSlot(1));
            assertEquals("fireball", profile.getSkillInSlot(3));
        }

        @Test
        @DisplayName("Spells diferentes em slots diferentes")
        void shouldAllowDifferentSpells_inDifferentSlots() {
            profile.setSkillBarSlot(1, "fireball");
            profile.setSkillBarSlot(2, "icebolt");
            assertEquals("fireball", profile.getSkillInSlot(1));
            assertEquals("icebolt", profile.getSkillInSlot(2));
        }

        @Test
        @DisplayName("Sobrescrever slot com outra spell")
        void shouldOverwriteSlot() {
            profile.setSkillBarSlot(1, "fireball");
            profile.setSkillBarSlot(1, "icebolt");
            assertEquals("icebolt", profile.getSkillInSlot(1));
        }
    }

    // ====== COMBOS ======

    @Nested
    @DisplayName("Combos")
    class ComboTests {

        @Test
        @DisplayName("Combo slot vazio retorna null")
        void shouldReturnNull_forEmptyComboSlot() {
            assertNull(profile.getComboSlot(1));
        }

        @Test
        @DisplayName("Deve setar combo slot")
        void shouldSetComboSlot() {
            profile.setComboSlot(1, "LLR", "fireball");
            var binding = profile.getComboSlot(1);
            assertNotNull(binding);
            assertEquals("LLR", binding.getSequence());
            assertEquals("fireball", binding.getSpellId());
        }

        @Test
        @DisplayName("Slot fora do range 1-4 é ignorado")
        void shouldIgnoreInvalidSlot() {
            profile.setComboSlot(0, "LLL", "spell");
            profile.setComboSlot(5, "RRR", "spell");
            assertNull(profile.getComboSlot(0));
            assertNull(profile.getComboSlot(5));
        }

        @Test
        @DisplayName("getSpellByCombo: case insensitive")
        void shouldFindSpellByCombo_caseInsensitive() {
            profile.setComboSlot(1, "LLR", "fireball");
            assertEquals("fireball", profile.getSpellByCombo("llr"));
            assertEquals("fireball", profile.getSpellByCombo("LLR"));
        }

        @Test
        @DisplayName("getSpellByCombo: combo inexistente retorna null")
        void shouldReturnNull_forUnknownCombo() {
            assertNull(profile.getSpellByCombo("LRL"));
        }

        @Test
        @DisplayName("Mesma spell em combo diferente limpa a anterior")
        void shouldDeduplicateSpell_acrossComboSlots() {
            profile.setComboSlot(1, "LLL", "fireball");
            profile.setComboSlot(2, "RRR", "fireball");
            // Slot 1 deve ter spellId limpo
            assertNull(profile.getComboSlot(1).getSpellId());
            assertEquals("fireball", profile.getComboSlot(2).getSpellId());
        }

        @Test
        @DisplayName("getComboSlots retorna mapa imutável")
        void shouldReturnUnmodifiableComboSlots() {
            profile.setComboSlot(1, "LLR", "fireball");
            assertThrows(UnsupportedOperationException.class, () ->
                profile.getComboSlots().put(5, new SpellProfile.ComboBinding("XXX", "test"))
            );
        }
    }

    // ====== COMBO LEGACY ======

    @Nested
    @DisplayName("ComboLegacy")
    class ComboLegacyTests {

        @Test
        @DisplayName("Deve setar combo em slot vazio")
        void shouldSetComboInEmptySlot() {
            profile.setComboLegacy("LLR", "fireball");
            assertEquals("fireball", profile.getSpellByCombo("LLR"));
        }

        @Test
        @DisplayName("Deve atualizar combo existente com mesma sequência")
        void shouldUpdateExistingCombo() {
            profile.setComboSlot(1, "LLR", "fireball");
            profile.setComboLegacy("LLR", "icebolt");
            assertEquals("icebolt", profile.getSpellByCombo("LLR"));
        }

        @Test
        @DisplayName("Deve deduplificar spell de outros combos")
        void shouldDeduplicateSpell_fromOtherCombos() {
            profile.setComboSlot(1, "LLL", "fireball");
            profile.setComboSlot(2, "RRR", "icebolt");
            profile.setComboLegacy("RRR", "fireball");
            // Slot 1 deve ter spellId limpo porque fireball agora está no RRR
            assertNull(profile.getComboSlot(1).getSpellId());
        }
    }

    // ====== COMBO BINDING ======

    @Nested
    @DisplayName("ComboBinding")
    class ComboBindingTests {

        @Test
        @DisplayName("Deve armazenar e alterar campos")
        void shouldStoreAndModifyFields() {
            var binding = new SpellProfile.ComboBinding("LLR", "spell1");
            assertEquals("LLR", binding.getSequence());
            assertEquals("spell1", binding.getSpellId());

            binding.setSequence("RRL");
            binding.setSpellId("spell2");
            assertEquals("RRL", binding.getSequence());
            assertEquals("spell2", binding.getSpellId());
        }
    }

    // ====== SPELL LEVELS ======

    @Nested
    @DisplayName("SpellLevels")
    class SpellLevelTests {

        @Test
        @DisplayName("Nível padrão de spell desconhecida é 1")
        void shouldDefaultToLevel1() {
            assertEquals(1, profile.getSpellLevel("unknown"));
        }

        @Test
        @DisplayName("Deve setar e recuperar nível")
        void shouldSetAndGetLevel() {
            profile.setSpellLevel("fireball", 5);
            assertEquals(5, profile.getSpellLevel("fireball"));
        }

        @Test
        @DisplayName("Nível mínimo é 1")
        void shouldClampLevelToMinimum1() {
            profile.setSpellLevel("fireball", 0);
            assertEquals(1, profile.getSpellLevel("fireball"));
            profile.setSpellLevel("fireball", -5);
            assertEquals(1, profile.getSpellLevel("fireball"));
        }

        @Test
        @DisplayName("Deve normalizar spellId para lowercase")
        void shouldNormalizeSpellId() {
            profile.setSpellLevel("FireBall", 3);
            assertEquals(3, profile.getSpellLevel("fireball"));
        }
    }

    // ====== COOLDOWNS ======

    @Nested
    @DisplayName("Cooldowns")
    class CooldownTests {

        @Test
        @DisplayName("Spell sem cooldown não está em cooldown")
        void shouldNotBeOnCooldown_byDefault() {
            assertFalse(profile.isOnCooldown("fireball"));
        }

        @Test
        @DisplayName("Deve aplicar cooldown")
        void shouldApplyCooldown() {
            profile.setCooldown("fireball", 10.0);
            assertTrue(profile.isOnCooldown("fireball"));
        }

        @Test
        @DisplayName("Cooldown expirado retorna false")
        void shouldNotBeOnCooldown_afterExpired() {
            profile.setCooldown("fireball", 0.0);
            assertFalse(profile.isOnCooldown("fireball"));
        }

        @Test
        @DisplayName("getCooldownRemainingKey retorna 0 sem cooldown ativo")
        void shouldReturnZero_forNoCooldown() {
            assertEquals(0, profile.getCooldownRemainingKey("fireball"));
        }

        @Test
        @DisplayName("getCooldownRemainingKey retorna valor positivo com cooldown ativo")
        void shouldReturnPositive_forActiveCooldown() {
            profile.setCooldown("fireball", 60.0);
            assertTrue(profile.getCooldownRemainingKey("fireball") > 0);
        }

        @Test
        @DisplayName("Deve normalizar spellId para lowercase")
        void shouldNormalizeCooldownSpellId() {
            profile.setCooldown("FireBall", 60.0);
            assertTrue(profile.isOnCooldown("fireball"));
        }
    }

    // ====== UNLOCK / LEARN ======

    @Nested
    @DisplayName("Unlock/Learn")
    class UnlockTests {

        @Test
        @DisplayName("Inicialmente sem spells desbloqueadas")
        void shouldHaveNoSpells_initially() {
            assertTrue(profile.getUnlockedSpells().isEmpty());
        }

        @Test
        @DisplayName("Deve desbloquear spell")
        void shouldUnlockSpell() {
            profile.unlockSpell("fireball");
            assertTrue(profile.hasSpell("fireball"));
        }

        @Test
        @DisplayName("Spells desbloqueadas é imutável")
        void shouldReturnUnmodifiableSet() {
            profile.unlockSpell("fireball");
            assertThrows(UnsupportedOperationException.class, () ->
                profile.getUnlockedSpells().add("icebolt")
            );
        }

        @Test
        @DisplayName("hasSpell: case insensitive")
        void shouldCheckSpell_caseInsensitive() {
            profile.unlockSpell("FireBall");
            assertTrue(profile.hasSpell("fireball"));
        }
    }

    // ====== ULTIMATE ======

    @Nested
    @DisplayName("Ultimate")
    class UltimateTests {

        @Test
        @DisplayName("Sem ultimate equipada por padrão")
        void shouldHaveNoUltimate_initially() {
            assertNull(profile.getEquippedUltimate());
        }

        @Test
        @DisplayName("Deve equipar ultimate (lowercased)")
        void shouldEquipUltimate() {
            profile.setEquippedUltimate("MegaBlast");
            assertEquals("megablast", profile.getEquippedUltimate());
        }

        @Test
        @DisplayName("Setar null remove ultimate")
        void shouldRemoveUltimate_whenNull() {
            profile.setEquippedUltimate("blast");
            profile.setEquippedUltimate(null);
            assertNull(profile.getEquippedUltimate());
        }
    }

    // ====== UNLEARN ======

    @Nested
    @DisplayName("unlearnSpell")
    class UnlearnTests {

        @Test
        @DisplayName("Deve remover spell de todas as estruturas")
        void shouldRemoveFromAllStructures() {
            String spellId = "fireball";
            // Configurar tudo
            profile.unlockSpell(spellId);
            profile.lockSpell(spellId);
            profile.setSkillBarSlot(1, spellId);
            profile.setComboSlot(1, "LLR", spellId);
            profile.setEquippedUltimate(spellId);
            profile.setSpellLevel(spellId, 5);
            profile.setSpellXP(spellId, 100.0);
            profile.achieveMilestone(spellId, 3);
            profile.setMastered(spellId);
            profile.getSpellStats(spellId).incrementCasts();
            profile.setCooldown(spellId, 60.0);

            // Desaprender
            profile.unlearnSpell(spellId);

            // Verificar tudo limpo
            assertFalse(profile.hasSpell(spellId));
            assertFalse(profile.isLocked(spellId));
            assertNull(profile.getSkillInSlot(1));
            assertNull(profile.getEquippedUltimate());
            assertEquals(1, profile.getSpellLevel(spellId)); // volta ao default
            assertEquals(0.0, profile.getSpellXP(spellId));
            assertFalse(profile.hasMilestone(spellId, 3));
            assertFalse(profile.isMastered(spellId));
        }

        @Test
        @DisplayName("Deve limpar combo slot que contém a spell")
        void shouldClearComboSlot() {
            profile.setComboSlot(2, "RRL", "icebolt");
            profile.unlearnSpell("icebolt");
            // O combo binding é removido pelo removeIf
            assertNull(profile.getComboSlot(2));
        }

        @Test
        @DisplayName("Não deve afetar outras spells")
        void shouldNotAffectOtherSpells() {
            profile.unlockSpell("fireball");
            profile.unlockSpell("icebolt");
            profile.setSkillBarSlot(1, "fireball");
            profile.setSkillBarSlot(2, "icebolt");

            profile.unlearnSpell("fireball");

            assertTrue(profile.hasSpell("icebolt"));
            assertEquals("icebolt", profile.getSkillInSlot(2));
        }
    }

    // ====== SPELL XP ======

    @Nested
    @DisplayName("SpellXP")
    class SpellXPTests {

        @Test
        @DisplayName("XP padrão é 0")
        void shouldDefaultToZero() {
            assertEquals(0.0, profile.getSpellXP("unknown"));
        }

        @Test
        @DisplayName("Deve adicionar XP (merge/soma)")
        void shouldAddXP() {
            profile.addSpellXP("fireball", 50.0);
            profile.addSpellXP("fireball", 30.0);
            assertEquals(80.0, profile.getSpellXP("fireball"), 0.001);
        }

        @Test
        @DisplayName("setSpellXP sobrescreve o valor")
        void shouldOverwriteXP() {
            profile.addSpellXP("fireball", 100.0);
            profile.setSpellXP("fireball", 25.0);
            assertEquals(25.0, profile.getSpellXP("fireball"), 0.001);
        }

        @Test
        @DisplayName("Deve normalizar spellId para lowercase")
        void shouldNormalizeSpellId() {
            profile.addSpellXP("FireBall", 10.0);
            assertEquals(10.0, profile.getSpellXP("fireball"), 0.001);
        }
    }

    // ====== MILESTONES ======

    @Nested
    @DisplayName("Milestones")
    class MilestoneTests {

        @Test
        @DisplayName("Milestone não alcançado retorna false")
        void shouldReturnFalse_forUnachievedMilestone() {
            assertFalse(profile.hasMilestone("fireball", 5));
        }

        @Test
        @DisplayName("Deve alcançar milestone")
        void shouldAchieveMilestone() {
            profile.achieveMilestone("fireball", 5);
            assertTrue(profile.hasMilestone("fireball", 5));
        }

        @Test
        @DisplayName("Milestones de levels diferentes são independentes")
        void shouldTrackMilestonesIndependently() {
            profile.achieveMilestone("fireball", 3);
            profile.achieveMilestone("fireball", 7);
            assertTrue(profile.hasMilestone("fireball", 3));
            assertTrue(profile.hasMilestone("fireball", 7));
            assertFalse(profile.hasMilestone("fireball", 5));
        }

        @Test
        @DisplayName("Deve normalizar spellId para lowercase")
        void shouldNormalizeSpellId() {
            profile.achieveMilestone("FireBall", 5);
            assertTrue(profile.hasMilestone("fireball", 5));
        }
    }

    // ====== MASTERY ======

    @Nested
    @DisplayName("Mastery")
    class MasteryTests {

        @Test
        @DisplayName("Spell não dominada retorna false")
        void shouldReturnFalse_forUnmasteredSpell() {
            assertFalse(profile.isMastered("fireball"));
        }

        @Test
        @DisplayName("Deve setar mastery")
        void shouldSetMastered() {
            profile.setMastered("fireball");
            assertTrue(profile.isMastered("fireball"));
        }

        @Test
        @DisplayName("getMasteredSpells retorna set imutável")
        void shouldReturnUnmodifiableMasteredSpells() {
            profile.setMastered("fireball");
            assertThrows(UnsupportedOperationException.class, () ->
                profile.getMasteredSpells().add("icebolt")
            );
        }

        @Test
        @DisplayName("Deve normalizar spellId para lowercase")
        void shouldNormalizeSpellId() {
            profile.setMastered("FireBall");
            assertTrue(profile.isMastered("fireball"));
        }
    }

    // ====== LOCKING ======

    @Nested
    @DisplayName("Locking")
    class LockingTests {

        @Test
        @DisplayName("Spell não travada por padrão")
        void shouldNotBeLocked_byDefault() {
            assertFalse(profile.isLocked("fireball"));
        }

        @Test
        @DisplayName("Deve travar spell")
        void shouldLockSpell() {
            profile.lockSpell("fireball");
            assertTrue(profile.isLocked("fireball"));
        }

        @Test
        @DisplayName("Deve destravar spell")
        void shouldUnlockSpellForRemoval() {
            profile.lockSpell("fireball");
            profile.unlockSpellForRemoval("fireball");
            assertFalse(profile.isLocked("fireball"));
        }

        @Test
        @DisplayName("Deve normalizar spellId para lowercase")
        void shouldNormalizeSpellId() {
            profile.lockSpell("FireBall");
            assertTrue(profile.isLocked("fireball"));
        }
    }

    // ====== SPELL MEMORY ======

    @Nested
    @DisplayName("SpellMemory")
    class SpellMemoryTests {

        @Test
        @DisplayName("Nível lembrado padrão é 0")
        void shouldDefaultToZero() {
            assertEquals(0, profile.getRememberedLevel("unknown"));
        }

        @Test
        @DisplayName("Deve lembrar e recuperar nível")
        void shouldRememberAndGetLevel() {
            profile.rememberSpell("fireball", 7);
            assertEquals(7, profile.getRememberedLevel("fireball"));
        }

        @Test
        @DisplayName("Deve esquecer spell")
        void shouldForgetSpell() {
            profile.rememberSpell("fireball", 5);
            profile.forgetSpell("fireball");
            assertEquals(0, profile.getRememberedLevel("fireball"));
        }

        @Test
        @DisplayName("Deve normalizar spellId para lowercase")
        void shouldNormalizeSpellId() {
            profile.rememberSpell("FireBall", 3);
            assertEquals(3, profile.getRememberedLevel("fireball"));
        }
    }

    // ====== STATISTICS ======

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("getSpellStats cria novo se não existir")
        void shouldCreateNewStats_ifNotExists() {
            var stats = profile.getSpellStats("fireball");
            assertNotNull(stats);
            assertEquals(0, stats.getCasts());
        }

        @Test
        @DisplayName("getSpellStats retorna mesmo objeto para mesma spell")
        void shouldReturnSameStats_forSameSpell() {
            var stats1 = profile.getSpellStats("fireball");
            stats1.incrementCasts();
            var stats2 = profile.getSpellStats("fireball");
            assertEquals(1, stats2.getCasts());
            assertSame(stats1, stats2);
        }

        @Test
        @DisplayName("getAllSpellStats retorna mapa imutável")
        void shouldReturnUnmodifiableAllStats() {
            profile.getSpellStats("fireball");
            assertThrows(UnsupportedOperationException.class, () ->
                profile.getAllSpellStats().put("icebolt", new SpellStats())
            );
        }

        @Test
        @DisplayName("Deve normalizar spellId para lowercase")
        void shouldNormalizeSpellId() {
            profile.getSpellStats("FireBall").incrementCasts();
            assertEquals(1, profile.getSpellStats("fireball").getCasts());
        }
    }
}

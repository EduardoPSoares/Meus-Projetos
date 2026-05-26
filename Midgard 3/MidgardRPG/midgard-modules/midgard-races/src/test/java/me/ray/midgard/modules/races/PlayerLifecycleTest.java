package me.ray.midgard.modules.races;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.profile.ProfileManager;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.RaceXpSource;
import me.ray.midgard.modules.races.api.TraitTrigger;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.event.PlayerChangeRaceEvent;
import me.ray.midgard.modules.races.event.PlayerRaceLevelUpEvent;
import me.ray.midgard.modules.races.manager.RaceLevelManager;
import me.ray.midgard.modules.races.manager.RaceManager;
import me.ray.midgard.modules.races.model.*;
import me.ray.midgard.modules.races.task.RaceTraitRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerLifecycleTest {

    @Test
    @DisplayName("Lifecycle completo: entrada → seleção de raça → XP → level up → traits → evolução → saída")
    void fullPlayerLifecycle() {
        try (
            MockedStatic<MidgardCore> coreMock = mockStatic(MidgardCore.class);
            MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class);
            MockedStatic<MessageUtils> msgMock = mockStatic(MessageUtils.class)
        ) {
            // ═══════════════════════════════════════════════════════════
            // SETUP: Infraestrutura de mocks
            // ═══════════════════════════════════════════════════════════
            UUID playerId = UUID.randomUUID();
            Player player = mock(Player.class);
            World world = mock(World.class);
            Location location = mock(Location.class);
            Block block = mock(Block.class);

            when(player.getUniqueId()).thenReturn(playerId);
            when(player.getName()).thenReturn("TestPlayer");
            when(player.isOnline()).thenReturn(true);
            when(player.getWorld()).thenReturn(world);
            when(player.getLocation()).thenReturn(location);
            when(location.getBlock()).thenReturn(block);
            when(location.getBlockY()).thenReturn(64);
            when(world.getName()).thenReturn("world");
            when(world.getTime()).thenReturn(6000L); // Dia (< 12610 default)
            when(world.hasStorm()).thenReturn(false);
            when(world.isThundering()).thenReturn(false);

            ProfileManager profileManager = mock(ProfileManager.class);
            MidgardProfile profile = new MidgardProfile(playerId, "TestPlayer");
            coreMock.when(MidgardCore::getProfileManager).thenReturn(profileManager);
            when(profileManager.getProfile(player)).thenReturn(profile);

            PluginManager pluginMgr = mock(PluginManager.class);
            bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginMgr);

            RacesModule module = mock(RacesModule.class);
            YamlConfiguration config = new YamlConfiguration();
            config.set("leveling.enabled", true);
            config.set("leveling.max-level", 10);
            config.set("leveling.base-xp", 100.0);
            config.set("leveling.multiplier", 1.5);
            when(module.getConfig()).thenReturn(config);
            when(module.getMessage("event.level_up")).thenReturn("Level %old% → %new%");

            RaceManager raceManager = mock(RaceManager.class);
            when(module.getRaceManager()).thenReturn(raceManager);

            // ═══════════════════════════════════════════════════════════
            // FASE 1: Player entra no jogo — dados iniciais vazios
            // ═══════════════════════════════════════════════════════════
            RaceData data = new RaceData();
            profile.setData(data);

            assertNull(data.getRaceId(), "Deve iniciar sem raça");
            assertFalse(data.hasRace());
            assertEquals(1, data.getLevel());
            assertEquals(0.0, data.getExperience(), 0.001);
            assertEquals(0, data.getTotalKills());
            assertTrue(data.getUnlockedMutations().isEmpty());
            assertTrue(data.getRaceHistory().isEmpty());

            // ═══════════════════════════════════════════════════════════
            // FASE 2: Sem raça → addExperience é no-op
            // ═══════════════════════════════════════════════════════════
            RaceLevelManager levelManager = new RaceLevelManager(module);

            levelManager.addExperience(player, 999.0, RaceXpSource.COMBAT);
            assertEquals(0.0, data.getExperience(), 0.001, "Sem raça, XP não deve ser adicionado");
            assertEquals(1, data.getLevel());

            // ═══════════════════════════════════════════════════════════
            // FASE 3: Criar raça completa com traits, multipliers, atributos
            // ═══════════════════════════════════════════════════════════
            RaceTrait mockTrait = mock(RaceTrait.class);

            // ON_EXP_GAIN: multiplier 1.5, ALWAYS, minLevel 1
            ConfiguredTrait expBoost = new ConfiguredTrait(
                    "exp_boost", mockTrait, TraitTrigger.ON_EXP_GAIN, 1,
                    Map.of("multiplier", 1.5));

            // ON_KILL: multiplier 2.0, só à NOITE, minLevel 1
            ConfiguredTrait killBoost = new ConfiguredTrait(
                    "kill_boost", mockTrait, TraitTrigger.ON_KILL, 1,
                    Map.of("multiplier", 2.0),
                    false, null, TraitCondition.fromString("NIGHT"));

            // PASSIVE_TICK: selectable, DAY, minLevel 3
            ConfiguredTrait selectableTrait = new ConfiguredTrait(
                    "sun_power", mockTrait, TraitTrigger.PASSIVE_TICK, 3,
                    Map.of("power", 5),
                    true, null, TraitCondition.fromString("DAY"));

            // ON_EXP_GAIN: multiplier 1.2, minLevel 5 (só ativa nível 5+)
            ConfiguredTrait highLevelTrait = new ConfiguredTrait(
                    "exp_mastery", mockTrait, TraitTrigger.ON_EXP_GAIN, 5,
                    Map.of("multiplier", 1.2));

            Race elf = new Race("elf", "&aElfo", null, 0, 10, 5, null,
                    List.of("Raça ancestral"),
                    Map.of("MAX_HEALTH", 2.0, "MOVEMENT_SPEED", 0.01),   // attributes
                    Map.of("MAX_HEALTH", 0.5),                            // perLevelAttributes
                    Map.of("ATTACK_DAMAGE", 1.0),                         // dayAttributes
                    Map.of("LUCK", 1.0),                                  // nightAttributes
                    Map.of(),                                              // dayPerLevelAttributes
                    Map.of("LUCK", 0.1),                                  // nightPerLevelAttributes
                    List.of(expBoost, killBoost, selectableTrait, highLevelTrait),
                    List.of("race.elf"),
                    List.of("/give %player% diamond 1"),
                    List.of("/effect clear %player%"),
                    List.of(
                            new EvolutionRequirement(EvolutionRequirement.RequirementType.LEVEL, null, 5, "Nível 5"),
                            new EvolutionRequirement(EvolutionRequirement.RequirementType.KILLS, null, 10, "10 kills")
                    ),
                    "elven_branch", true,
                    Map.of(RaceXpSource.COMBAT, 1.5, RaceXpSource.MINING, 0.8));

            Race highElf = new Race("high_elf", "&6Alto Elfo", "elf", 5, 11, 6, null,
                    List.of("Evolução dos elfos"),
                    Map.of("MAX_HEALTH", 4.0), Map.of("MAX_HEALTH", 1.0),
                    List.of(), List.of(), List.of(), List.of(),
                    List.of(), "elven_branch", true);

            when(raceManager.getRace("elf")).thenReturn(elf);
            when(raceManager.getRace("high_elf")).thenReturn(highElf);

            // ═══════════════════════════════════════════════════════════
            // FASE 4: Selecionar raça — evento + atualização de dados
            // ═══════════════════════════════════════════════════════════
            PlayerChangeRaceEvent selectEvent = new PlayerChangeRaceEvent(player, null, elf);
            assertNull(selectEvent.getOldRace(), "Primeira seleção: oldRace deve ser null");
            assertSame(elf, selectEvent.getNewRace());
            assertFalse(selectEvent.isCancelled());

            data.setRaceId("elf");
            data.setLastRaceChange(System.currentTimeMillis());
            assertTrue(data.hasRace());
            assertEquals("elf", data.getRaceId());
            assertTrue(data.getLastRaceChange() > 0);

            // ═══════════════════════════════════════════════════════════
            // FASE 5: Verificar propriedades da raça
            // ═══════════════════════════════════════════════════════════
            assertFalse(elf.isSubRace());
            assertTrue(elf.hasTimeAttributes());
            assertTrue(elf.hasEvolutionRequirements());

            // XP multipliers por fonte
            assertEquals(1.5, elf.getXpMultiplier(RaceXpSource.COMBAT), 0.001);
            assertEquals(0.8, elf.getXpMultiplier(RaceXpSource.MINING), 0.001);
            assertEquals(1.0, elf.getXpMultiplier(RaceXpSource.FISHING), 0.001); // default

            // Mapa imutável
            assertThrows(UnsupportedOperationException.class,
                    () -> elf.getXpMultipliers().put(RaceXpSource.FISHING, 2.0));

            // Atributos base
            assertEquals(2.0, elf.getAttributes().get("MAX_HEALTH"));
            assertEquals(0.5, elf.getPerLevelAttributes().get("MAX_HEALTH"));

            // Atributos por tempo
            assertEquals(1.0, elf.getDayAttributes().get("ATTACK_DAMAGE"));
            assertEquals(1.0, elf.getNightAttributes().get("LUCK"));
            assertEquals(0.1, elf.getNightPerLevelAttributes().get("LUCK"));

            // Cálculo manual: MAX_HEALTH no nível 5 = base 2.0 + perLevel 0.5 * (5-1) = 4.0
            double maxHealthLv5 = elf.getAttributes().get("MAX_HEALTH")
                    + elf.getPerLevelAttributes().get("MAX_HEALTH") * (5 - 1);
            assertEquals(4.0, maxHealthLv5, 0.001);

            // LUCK noturno no nível 5 = base 1.0 + perLevel 0.1 * (5-1) = 1.4
            double luckNightLv5 = elf.getNightAttributes().get("LUCK")
                    + elf.getNightPerLevelAttributes().get("LUCK") * (5 - 1);
            assertEquals(1.4, luckNightLv5, 0.001);

            // ═══════════════════════════════════════════════════════════
            // FASE 6: XP de COMBAT durante o DIA — race mult + trait
            // ═══════════════════════════════════════════════════════════
            // 10 XP COMBAT (dia, world time=6000):
            //   Race mult COMBAT = 1.5 → 15.0
            //   expBoost (ON_EXP_GAIN, ALWAYS, minLv1) → ×1.5 = 22.5
            //   killBoost (ON_KILL, NIGHT) → condição NIGHT falha (é dia) → skip
            //   highLevelTrait (ON_EXP_GAIN, minLv5) → nível 1 < 5 → skip
            levelManager.addExperience(player, 10.0, RaceXpSource.COMBAT);
            assertEquals(22.5, data.getExperience(), 0.001);
            assertEquals(1, data.getLevel(), "Precisa 100 XP para nível 2");

            // ═══════════════════════════════════════════════════════════
            // FASE 7: XP de MINING — fonte com penalidade
            // ═══════════════════════════════════════════════════════════
            // 20 XP MINING: 20 × 0.8 (race) × 1.5 (expBoost) = 24.0
            levelManager.addExperience(player, 20.0, RaceXpSource.MINING);
            assertEquals(46.5, data.getExperience(), 0.001); // 22.5 + 24.0

            // ═══════════════════════════════════════════════════════════
            // FASE 8: XP genérica (sem source) — chamada legada
            // ═══════════════════════════════════════════════════════════
            // 30 XP sem source → race mult não aplica (null), trait expBoost ×1.5 = 45.0
            levelManager.addExperience(player, 30.0);
            assertEquals(91.5, data.getExperience(), 0.001); // 46.5 + 45.0

            // ═══════════════════════════════════════════════════════════
            // FASE 9: LEVEL UP! (nível 1 → 2)
            // ═══════════════════════════════════════════════════════════
            // Fórmula: base=100, mult=1.5 → Lv1→2: 100 × 1.5^0 = 100.0
            assertEquals(100.0, RaceLevelManager.calculateRequiredXp(1, 100.0, 1.5), 0.01);
            assertEquals(150.0, RaceLevelManager.calculateRequiredXp(2, 100.0, 1.5), 0.01);
            assertEquals(225.0, RaceLevelManager.calculateRequiredXp(3, 100.0, 1.5), 0.01);

            // 6 XP FISHING (mult 1.0): 6 × 1.0 × 1.5 (expBoost) = 9.0
            // Total: 91.5 + 9.0 = 100.5 ≥ 100 → LEVEL UP! Residual: 0.5
            levelManager.addExperience(player, 6.0, RaceXpSource.FISHING);
            assertEquals(2, data.getLevel());
            assertEquals(0.5, data.getExperience(), 0.01);

            // Verificar que PlayerRaceLevelUpEvent foi disparado
            ArgumentCaptor<org.bukkit.event.Event> eventCaptor =
                    ArgumentCaptor.forClass(org.bukkit.event.Event.class);
            verify(pluginMgr, atLeastOnce()).callEvent(eventCaptor.capture());

            boolean foundLevelUp = eventCaptor.getAllValues().stream()
                    .filter(e -> e instanceof PlayerRaceLevelUpEvent)
                    .map(e -> (PlayerRaceLevelUpEvent) e)
                    .anyMatch(e -> e.getOldLevel() == 1 && e.getNewLevel() == 2);
            assertTrue(foundLevelUp, "Deve ter disparado evento 1→2");

            // ═══════════════════════════════════════════════════════════
            // FASE 10: Multi-level up (nível 2 → 4)
            // ═══════════════════════════════════════════════════════════
            // 300 XP COMBAT (dia): 300 × 1.5 (race) × 1.5 (expBoost) = 675.0
            // XP total: 0.5 + 675.0 = 675.5
            //   Lv2→3: req 150 → residual 525.5
            //   Lv3→4: req 225 → residual 300.5
            //   Lv4:   req 337.5, 300.5 < 337.5 → para
            levelManager.addExperience(player, 300.0, RaceXpSource.COMBAT);
            assertEquals(4, data.getLevel());
            assertEquals(300.5, data.getExperience(), 0.1);

            // ═══════════════════════════════════════════════════════════
            // FASE 11: XP de COMBAT à NOITE — kill trait ativa!
            // ═══════════════════════════════════════════════════════════
            when(world.getTime()).thenReturn(15000L); // Noite

            // 10 XP COMBAT (noite):
            //   Race mult = 1.5 → 15.0
            //   expBoost ×1.5 = 22.5
            //   killBoost (ON_KILL, NIGHT ok) ×2.0 = 45.0
            //   highLevelTrait minLv5, nível 4 < 5 → skip
            // Total: 300.5 + 45.0 = 345.5 ≥ 337.5 → LEVEL UP 4→5! Residual: 8.0
            levelManager.addExperience(player, 10.0, RaceXpSource.COMBAT);
            assertEquals(5, data.getLevel());
            assertEquals(8.0, data.getExperience(), 0.1);

            when(world.getTime()).thenReturn(6000L); // Volta pro dia

            // ═══════════════════════════════════════════════════════════
            // FASE 12: High-level trait ativa (nível 5+)
            // ═══════════════════════════════════════════════════════════
            // 10 XP COMBAT (dia):
            //   Race ×1.5 = 15.0
            //   expBoost ×1.5 = 22.5
            //   highLevelTrait (minLv5, nível 5 ≥ 5) ×1.2 = 27.0
            //   killBoost (NIGHT) → dia → skip
            double xpBefore = data.getExperience();
            levelManager.addExperience(player, 10.0, RaceXpSource.COMBAT);
            double gained = data.getExperience() - xpBefore;
            assertEquals(27.0, gained, 0.1);
            assertEquals(5, data.getLevel()); // req lv5 = 506.25, longe

            // ═══════════════════════════════════════════════════════════
            // FASE 13: Kill tracking
            // ═══════════════════════════════════════════════════════════
            assertEquals(0, data.getTotalKills());

            data.addKill("ZOMBIE");
            data.addKill("ZOMBIE");
            data.addKill("SKELETON");
            data.addKill("CREEPER");

            assertEquals(4, data.getTotalKills());
            assertEquals(2, data.getKillsOf("ZOMBIE"));
            assertEquals(1, data.getKillsOf("SKELETON"));
            assertEquals(1, data.getKillsOf("CREEPER"));
            assertEquals(0, data.getKillsOf("SPIDER"));

            // Case insensitive
            assertEquals(2, data.getKillsOf("zombie"));

            // ═══════════════════════════════════════════════════════════
            // FASE 14: TraitCondition — dia/noite, bioma, clima, altitude, mundo
            // ═══════════════════════════════════════════════════════════

            // Dia/Noite
            TraitCondition dayCond = TraitCondition.fromString("DAY");
            TraitCondition nightCond = TraitCondition.fromString("NIGHT");
            assertTrue(dayCond.isMet(player));   // world time = 6000 (dia)
            assertFalse(nightCond.isMet(player));

            when(world.getTime()).thenReturn(15000L);
            assertFalse(dayCond.isMet(player));
            assertTrue(nightCond.isMet(player));
            when(world.getTime()).thenReturn(6000L); // reset

            // ALWAYS
            assertTrue(TraitCondition.ALWAYS.isMet(player));
            assertTrue(TraitCondition.ALWAYS.isAlways());

            // Clima
            TraitCondition clearCond = new TraitCondition(
                    TraitCondition.TimeRule.ALWAYS, Set.of(), Set.of(),
                    TraitCondition.WeatherRule.CLEAR, Integer.MIN_VALUE, Integer.MAX_VALUE);
            assertTrue(clearCond.isMet(player)); // sem tempestade

            when(world.hasStorm()).thenReturn(true);
            TraitCondition rainCond = new TraitCondition(
                    TraitCondition.TimeRule.ALWAYS, Set.of(), Set.of(),
                    TraitCondition.WeatherRule.RAIN, Integer.MIN_VALUE, Integer.MAX_VALUE);
            assertTrue(rainCond.isMet(player)); // chovendo
            assertFalse(clearCond.isMet(player)); // não é limpo

            when(world.isThundering()).thenReturn(true);
            TraitCondition thunderCond = new TraitCondition(
                    TraitCondition.TimeRule.ALWAYS, Set.of(), Set.of(),
                    TraitCondition.WeatherRule.THUNDER, Integer.MIN_VALUE, Integer.MAX_VALUE);
            assertTrue(thunderCond.isMet(player));

            when(world.hasStorm()).thenReturn(false);
            when(world.isThundering()).thenReturn(false);

            // Altitude
            TraitCondition altCond = new TraitCondition(
                    TraitCondition.TimeRule.ALWAYS, Set.of(), Set.of(),
                    TraitCondition.WeatherRule.ANY, 50, 100);
            assertTrue(altCond.isMet(player)); // Y=64

            when(location.getBlockY()).thenReturn(30);
            assertFalse(altCond.isMet(player)); // Y=30, abaixo do mínimo
            when(location.getBlockY()).thenReturn(64); // reset

            // Mundo
            TraitCondition worldCond = new TraitCondition(
                    TraitCondition.TimeRule.ALWAYS, Set.of(), Set.of("world"),
                    TraitCondition.WeatherRule.ANY, Integer.MIN_VALUE, Integer.MAX_VALUE);
            assertTrue(worldCond.isMet(player));

            when(world.getName()).thenReturn("nether");
            assertFalse(worldCond.isMet(player));
            when(world.getName()).thenReturn("world"); // reset

            // fromSection null → ALWAYS
            TraitCondition fromNull = TraitCondition.fromSection(null);
            assertSame(TraitCondition.ALWAYS, fromNull);

            // fromString inválido → ALWAYS
            TraitCondition fromInvalid = TraitCondition.fromString("BANANA");
            assertSame(TraitCondition.ALWAYS, fromInvalid);

            // ═══════════════════════════════════════════════════════════
            // FASE 15: Selectable trait (sistema de mutations)
            // ═══════════════════════════════════════════════════════════
            assertTrue(selectableTrait.isSelectable());
            assertEquals(3, selectableTrait.getMinLevel());
            assertTrue(data.getLevel() >= selectableTrait.getMinLevel());

            // Sem mutation → trait bloqueada
            assertFalse(data.hasMutation("sun_power"));

            // Desbloquear
            data.unlockMutation("sun_power");
            assertTrue(data.hasMutation("sun_power"));
            assertEquals(1, data.getUnlockedMutations().size());

            // Múltiplas mutations
            data.unlockMutation("darkvision");
            assertEquals(2, data.getUnlockedMutations().size());
            assertTrue(data.hasMutation("darkvision"));

            // ═══════════════════════════════════════════════════════════
            // FASE 16: Evolution requirements
            // ═══════════════════════════════════════════════════════════
            List<EvolutionRequirement> reqs = elf.getEvolutionRequirements();
            assertEquals(2, reqs.size());

            EvolutionRequirement levelReq = reqs.get(0);
            EvolutionRequirement killsReq = reqs.get(1);

            // LEVEL 5: nível 5 → atende
            assertEquals(EvolutionRequirement.RequirementType.LEVEL, levelReq.type());
            assertTrue(levelReq.isMet(player, data));

            // KILLS 10: 4 kills → não atende
            assertEquals(EvolutionRequirement.RequirementType.KILLS, killsReq.type());
            assertFalse(killsReq.isMet(player, data));

            // Completar kills
            for (int i = 0; i < 6; i++) { data.addKill("ZOMBIE"); }
            assertEquals(10, data.getTotalKills());
            assertTrue(killsReq.isMet(player, data));

            // Todos os requisitos atendidos → evolução permitida
            assertTrue(reqs.stream().allMatch(r -> r.isMet(player, data)));

            // ═══════════════════════════════════════════════════════════
            // FASE 17: Evolução para sub-raça
            // ═══════════════════════════════════════════════════════════
            data.pushRaceHistory("elf");

            PlayerChangeRaceEvent evolveEvent = new PlayerChangeRaceEvent(player, elf, highElf);
            assertSame(elf, evolveEvent.getOldRace());
            assertSame(highElf, evolveEvent.getNewRace());

            data.setRaceId("high_elf");

            assertTrue(highElf.isSubRace());
            assertEquals("elf", highElf.getParentRace());
            assertEquals(5, highElf.getMinLevel());
            assertTrue(highElf.isAllowDevolution());
            assertEquals("elven_branch", highElf.getExclusionBranch());

            // Sem time attributes na sub-raça
            assertFalse(highElf.hasTimeAttributes());

            // Sem evolução requirements na sub-raça
            assertFalse(highElf.hasEvolutionRequirements());

            // XP multipliers default (1.0) para sub-raça
            assertEquals(1.0, highElf.getXpMultiplier(RaceXpSource.COMBAT), 0.001);

            // Histórico mantido
            assertEquals(1, data.getRaceHistory().size());
            assertEquals("elf", data.getPreviousRaceId());

            // ═══════════════════════════════════════════════════════════
            // FASE 18: Devolver (de-evolution)
            // ═══════════════════════════════════════════════════════════
            String previous = data.popRaceHistory();
            assertEquals("elf", previous);
            assertTrue(data.getRaceHistory().isEmpty());

            data.setRaceId(previous);
            assertEquals("elf", data.getRaceId());

            // ═══════════════════════════════════════════════════════════
            // FASE 19: Max level — não ultrapassa o teto
            // ═══════════════════════════════════════════════════════════
            data.setLevel(10); // maxLevel configurado
            data.setExperience(0);

            levelManager.addExperience(player, 10.0, RaceXpSource.COMBAT);
            assertEquals(10, data.getLevel(), "Não deve ultrapassar o nível máximo");
            assertTrue(data.getExperience() > 0, "XP deve acumular mesmo no max level");

            // ═══════════════════════════════════════════════════════════
            // FASE 20: ConfiguredTrait — edge cases
            // ═══════════════════════════════════════════════════════════
            // Config null → Map.of()
            ConfiguredTrait nullConfig = new ConfiguredTrait("test", mockTrait,
                    TraitTrigger.ON_EXP_GAIN, 1, null);
            assertNotNull(nullConfig.getConfig());
            assertTrue(nullConfig.getConfig().isEmpty());
            assertSame(TraitCondition.ALWAYS, nullConfig.getCondition());
            assertFalse(nullConfig.isSelectable());
            assertNull(nullConfig.getExclusionGroup());

            // ═══════════════════════════════════════════════════════════
            // FASE 21: RaceTraitRunnable — cleanup no quit
            // ═══════════════════════════════════════════════════════════
            RaceTraitRunnable runnable = new RaceTraitRunnable(module);
            runnable.removePlayer(playerId);
            runnable.removePlayer(playerId); // Idempotente, sem crash

            // ═══════════════════════════════════════════════════════════
            // FASE 22: Contagem total de level-up events
            // ═══════════════════════════════════════════════════════════
            ArgumentCaptor<org.bukkit.event.Event> allEvents =
                    ArgumentCaptor.forClass(org.bukkit.event.Event.class);
            verify(pluginMgr, atLeast(1)).callEvent(allEvents.capture());

            long levelUpCount = allEvents.getAllValues().stream()
                    .filter(e -> e instanceof PlayerRaceLevelUpEvent)
                    .count();
            // FASE 9(1→2) + FASE 10(2→3, 3→4) + FASE 11(4→5) = 4 level ups
            assertEquals(4, levelUpCount, "Devem ter ocorrido 4 level ups no total");

            // Verificar que último level up foi 4→5
            PlayerRaceLevelUpEvent lastLevelUp = allEvents.getAllValues().stream()
                    .filter(e -> e instanceof PlayerRaceLevelUpEvent)
                    .map(e -> (PlayerRaceLevelUpEvent) e)
                    .reduce((a, b) -> b)
                    .orElseThrow();
            assertEquals(4, lastLevelUp.getOldLevel());
            assertEquals(5, lastLevelUp.getNewLevel());

            // ═══════════════════════════════════════════════════════════
            // FASE 23: Estado final do player ao sair
            // ═══════════════════════════════════════════════════════════
            assertEquals("elf", data.getRaceId());
            assertEquals(10, data.getLevel());
            assertEquals(10, data.getTotalKills());
            assertEquals(8, data.getKillsOf("ZOMBIE")); // 2 + 6
            assertEquals(1, data.getKillsOf("SKELETON"));
            assertEquals(1, data.getKillsOf("CREEPER"));
            assertTrue(data.hasMutation("sun_power"));
            assertTrue(data.hasMutation("darkvision"));
            assertTrue(data.getRaceHistory().isEmpty());
        }
    }
}

package me.ray.midgard.modules.professions.blacksmith.forge;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.data.ForgeData;
import me.ray.midgard.modules.professions.blacksmith.forge.data.ForgeRepository;
import me.ray.midgard.modules.professions.blacksmith.forge.display.ForgeHologram;
import me.ray.midgard.modules.professions.blacksmith.forge.display.ForgeScoreboard;
import me.ray.midgard.modules.professions.blacksmith.forge.effect.ForgeEffectManager;
import me.ray.midgard.modules.professions.blacksmith.forge.fuel.FuelManager;
import me.ray.midgard.modules.professions.blacksmith.forge.minigame.HammeringMinigame;
import me.ray.midgard.modules.professions.blacksmith.forge.minigame.QuenchingMinigame;
import me.ray.midgard.modules.professions.blacksmith.forge.minigame.SharpeningMinigame;
import me.ray.midgard.modules.professions.blacksmith.forge.quality.ForgeQualityApplier;
import me.ray.midgard.modules.professions.blacksmith.forge.quality.QualityCalculator;
import me.ray.midgard.modules.professions.blacksmith.forge.quality.QualityTier;
import me.ray.midgard.modules.professions.blacksmith.forge.recipe.ForgeRecipe;
import me.ray.midgard.modules.professions.blacksmith.forge.session.ForgeSession;
import me.ray.midgard.modules.professions.blacksmith.forge.session.ForgeSessionManager;
import me.ray.midgard.modules.professions.blacksmith.forge.event.ForgeCompleteEvent;
import me.ray.midgard.modules.professions.blacksmith.forge.event.ForgeStartEvent;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.*;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia o fluxo de forjamento: fases (aquecimento, martelamento, têmpera,
 * afiação, finalização), minijogos ativos e efeitos por sessão.
 */
public class ForgeWorkflowService {

    private final MiniMessage mm = MiniMessage.miniMessage();

    // Dependencies (injected via constructor)
    private final ForgeSessionManager sessionManager;
    private final ForgeEffectManager effectManager;
    private final QualityCalculator qualityCalculator;
    private final FuelManager fuelManager;
    private final ForgeScoreboard forgeScoreboard;
    private final ForgeHologram forgeHologram;
    private final ForgeRegistry registry;
    private final ForgeRepository repository;
    private final ForgeManager forgeManager;

    // Active heating effects
    private final Map<UUID, BukkitTask> heatingEffects = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> heatingBossBars = new ConcurrentHashMap<>();

    // Active minigame instances
    private final Map<UUID, HammeringMinigame> activeHammeringGames = new ConcurrentHashMap<>();
    private final Map<UUID, QuenchingMinigame> activeQuenchingGames = new ConcurrentHashMap<>();
    private final Map<UUID, SharpeningMinigame> activeSharpeningGames = new ConcurrentHashMap<>();

    public ForgeWorkflowService(ForgeManager forgeManager,
                                ForgeSessionManager sessionManager,
                                ForgeEffectManager effectManager,
                                QualityCalculator qualityCalculator,
                                FuelManager fuelManager,
                                ForgeScoreboard forgeScoreboard,
                                ForgeHologram forgeHologram,
                                ForgeRegistry registry,
                                ForgeRepository repository) {
        this.forgeManager = forgeManager;
        this.sessionManager = sessionManager;
        this.effectManager = effectManager;
        this.qualityCalculator = qualityCalculator;
        this.fuelManager = fuelManager;
        this.forgeScoreboard = forgeScoreboard;
        this.forgeHologram = forgeHologram;
        this.registry = registry;
        this.repository = repository;
    }

    private String msg(String key) {
        return ProfessionsModule.getInstance().getMessage(key);
    }

    // ==================== Public Entry Points ====================

    /**
     * Inicia o processo de forjamento — valida fuel, consome materiais, inicia aquecimento.
     */
    public void startForging(Player player, ForgeStructure forge, ForgeRecipe recipe) {
        if (sessionManager.hasActiveSession(player.getUniqueId())) {
            player.sendMessage(mm.deserialize(msg("forge.session.already_active")));
            return;
        }

        if (sessionManager.isForgeInUse(forge.getForgeId())) {
            player.sendMessage(mm.deserialize(msg("forge.session.in_use")));
            return;
        }

        // Check fuel
        List<Location> fuelZone = forge.getFuelZoneLocations();
        if (!fuelManager.hasFuel(forge.getForgeId())) {
            ForgeData fuelCheckData = forgeManager.getOrLoadData(player.getUniqueId());
            if (!fuelManager.collectFuelFromZone(forge.getForgeId(), fuelZone, player, fuelCheckData.getLevel())) {
                player.sendMessage(mm.deserialize(msg("forge.session.no_fuel")));
                return;
            }
        }

        // Check materials
        Map<String, Integer> required = recipe.getAllMaterials();
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            String materialId = entry.getKey();
            int amount = entry.getValue();
            try {
                org.bukkit.Material mat = org.bukkit.Material.valueOf(materialId);
                if (!player.getInventory().containsAtLeast(new ItemStack(mat), amount)) {
                    player.sendMessage(mm.deserialize(msg("forge.session.missing_materials")
                            .replace("%amount%", String.valueOf(amount))
                            .replace("%material%", materialId.toLowerCase().replace("_", " "))));
                    return;
                }
            } catch (IllegalArgumentException e) {
                if (forgeManager.countMidgardItems(player, materialId) < amount) {
                    player.sendMessage(mm.deserialize(msg("forge.session.missing_materials")
                            .replace("%amount%", String.valueOf(amount))
                            .replace("%material%", materialId.toLowerCase().replace("_", " "))));
                    return;
                }
            }
        }

        // Fire cancellable ForgeStartEvent
        ForgeStartEvent startEvent = new ForgeStartEvent(player, forge, recipe);
        org.bukkit.Bukkit.getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) { return; }

        ForgeSession session = new ForgeSession(player.getUniqueId(), forge.getForgeId(), recipe);
        ForgeSession started = sessionManager.startSession(session);
        if (started == null) {
            player.sendMessage(mm.deserialize(msg("forge.session.start_failed")));
            return;
        }

        // Consume materials
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            try {
                org.bukkit.Material mat = org.bukkit.Material.valueOf(entry.getKey());
                ItemStack toRemove = new ItemStack(mat, entry.getValue());
                player.getInventory().removeItem(toRemove);
            } catch (IllegalArgumentException e) {
                forgeManager.removeMidgardItems(player, entry.getKey(), entry.getValue());
            }
        }

        session.setMaterialsConsumed(true);
        session.advanceToNextStage(); // SELECTING → PREPARING
        session.advanceToNextStage(); // PREPARING → HEATING

        player.sendMessage(mm.deserialize(""));
        player.sendMessage(mm.deserialize(msg("forge.session.started_title")));
        player.sendMessage(mm.deserialize(msg("forge.session.started_recipe").replace("%name%", recipe.getDisplayName())));
        player.sendMessage(mm.deserialize(msg("forge.session.started_fuel").replace("%fuel%",
                fuelManager.getDeposit(forge.getForgeId()) != null
                        ? fuelManager.getDeposit(forge.getForgeId()).getFuel().getDisplayName()
                        : "Nenhum")));
        player.sendMessage(mm.deserialize(""));

        forgeScoreboard.show(player, session);
        forgeHologram.showStageHologram(player.getUniqueId(), forge, ForgeStage.HEATING);

        startHeatingPhase(player, forge, session);
    }

    /**
     * Chamada quando o jogador clica na fornalha durante HEATING.
     */
    public void onRemoveFromFurnace(Player player, ForgeStructure forge) {
        ForgeSession session = sessionManager.getSession(player.getUniqueId());
        if (session == null || session.getCurrentStage() != ForgeStage.HEATING) { return; }

        double temp = session.getCurrentTemperature();
        session.calculateHeatingScore(temp);
        session.setMetalHeated(true);
        cancelHeatingEffect(player.getUniqueId());
        hideHeatingBossBar(player);

        if (session.getCurrentStage() == ForgeStage.FAILED) {
            effectManager.playFailureEffect(forge);
            sessionManager.failSession(player.getUniqueId());
            return;
        }

        player.sendMessage(mm.deserialize(msg("forge.heating.metal_removed")
                .replace("%temp%", String.format("%.0f", temp))
                .replace("%score%", String.format("%.0f%%", session.getHeatingScore() * 100))));

        session.advanceToNextStage(); // HEATING → HAMMERING
        forgeScoreboard.update(player, session);
        forgeHologram.showStageHologram(player.getUniqueId(), forge, ForgeStage.HAMMERING);
        startHammeringPhase(player, forge, session);
    }

    // ==================== Forging Phases ====================

    private void startHeatingPhase(Player player, ForgeStructure forge, ForgeSession session) {
        effectManager.playStageTransition(player, ForgeStage.HEATING);

        BukkitTask effect = effectManager.playHeatingEffect(forge);
        if (effect != null) { heatingEffects.put(player.getUniqueId(), effect); }

        double fuelPower = fuelManager.getHeatingPower(forge.getForgeId());

        BossBar bossBar = BossBar.bossBar(
                mm.deserialize(msg("forge.heating.bossbar_initial")),
                0f,
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_20
        );
        player.showBossBar(bossBar);
        heatingBossBars.put(player.getUniqueId(), bossBar);

        player.sendMessage(mm.deserialize(msg("forge.heating.instruction")));

        double idealMin = session.getRecipe().getIdealTempMin();
        double idealMax = session.getRecipe().getIdealTempMax();
        double maxTemp = idealMax + 300;

        final BukkitTask[] heatingTimer = new BukkitTask[1];
        heatingTimer[0] = Task.syncTimer(new Runnable() {
            double temp = 0;
            final double heatingRate = (maxTemp / (session.getRecipe().getHeatingTime() * 20.0)) * fuelPower;

            @Override
            public void run() {
                if (!session.isActive() || session.getCurrentStage() != ForgeStage.HEATING) {
                    if (heatingTimer[0] != null) { heatingTimer[0].cancel(); }
                    return;
                }

                temp += heatingRate * 2;
                session.setCurrentTemperature(temp);

                if (!fuelManager.consumeFuel(forge.getForgeId(), 2)) {
                    List<Location> fuelZoneLocs = forge.getFuelZoneLocations();
                    fuelManager.tryCollectMoreFuel(forge.getForgeId(), fuelZoneLocs);
                }

                forgeHologram.showTemperature(player.getUniqueId(), forge, temp, idealMin, idealMax);

                float progress = (float) Math.min(1.0, temp / maxTemp);
                bossBar.progress(progress);

                if (temp >= idealMin && temp <= idealMax) {
                    bossBar.color(BossBar.Color.GREEN);
                    bossBar.name(mm.deserialize(msg("forge.heating.bossbar_ideal")
                            .replace("%temp%", String.format("%.0f", temp))));
                } else if (temp < idealMin) {
                    bossBar.color(BossBar.Color.YELLOW);
                    bossBar.name(mm.deserialize(msg("forge.heating.bossbar_warming")
                            .replace("%temp%", String.format("%.0f", temp))));
                } else {
                    bossBar.color(BossBar.Color.RED);
                    bossBar.name(mm.deserialize(msg("forge.heating.bossbar_overheating")
                            .replace("%temp%", String.format("%.0f", temp))));
                }

                if (temp > maxTemp) {
                    player.hideBossBar(bossBar);
                    cancelHeatingEffect(player.getUniqueId());
                    session.calculateHeatingScore(temp);
                    if (session.getCurrentStage() == ForgeStage.FAILED) {
                        effectManager.playFailureEffect(registry.getById(session.getForgeId()));
                        sessionManager.failSession(player.getUniqueId());
                    }
                }
            }
        }, 1L, 2L);
    }

    private void startHammeringPhase(Player player, ForgeStructure forge, ForgeSession session) {
        effectManager.playStageTransition(player, ForgeStage.HAMMERING);

        HammeringMinigame minigame = new HammeringMinigame(session);
        Location anvilLoc = forge.getInteractiveLocations().get(ForgeBlock.ForgeBlockType.ANVIL);
        if (anvilLoc != null) { minigame.setAnvilLocation(anvilLoc); }

        minigame.setOnComplete((p, s) -> {
            activeHammeringGames.remove(p.getUniqueId());
            s.advanceToNextStage(); // HAMMERING → QUENCHING
            forgeScoreboard.update(p, s);
            forgeHologram.showStageHologram(p.getUniqueId(), forge, ForgeStage.QUENCHING);
            startQuenchingPhase(p, forge, s);
        });

        activeHammeringGames.put(player.getUniqueId(), minigame);
        minigame.start(player, session);
    }

    private void startQuenchingPhase(Player player, ForgeStructure forge, ForgeSession session) {
        effectManager.playStageTransition(player, ForgeStage.QUENCHING);
        effectManager.playQuenchEffect(forge);

        QuenchingMinigame minigame = new QuenchingMinigame(session);
        Location cauldronLoc = forge.getInteractiveLocations().get(ForgeBlock.ForgeBlockType.CAULDRON);
        if (cauldronLoc != null) { minigame.setCauldronLocation(cauldronLoc); }

        minigame.setOnComplete((p, s) -> {
            activeQuenchingGames.remove(p.getUniqueId());
            s.advanceToNextStage(); // QUENCHING → SHARPENING
            forgeScoreboard.update(p, s);
            forgeHologram.showStageHologram(p.getUniqueId(), forge, ForgeStage.SHARPENING);
            startSharpeningPhase(p, forge, s);
        });

        activeQuenchingGames.put(player.getUniqueId(), minigame);
        minigame.start(player, session);
    }

    private void startSharpeningPhase(Player player, ForgeStructure forge, ForgeSession session) {
        effectManager.playStageTransition(player, ForgeStage.SHARPENING);

        SharpeningMinigame minigame = new SharpeningMinigame(session);
        Location grindstoneLoc = forge.getInteractiveLocations().get(ForgeBlock.ForgeBlockType.GRINDSTONE);
        if (grindstoneLoc != null) { minigame.setGrindstoneLocation(grindstoneLoc); }

        minigame.setOnComplete((p, s) -> {
            activeSharpeningGames.remove(p.getUniqueId());
            s.advanceToNextStage(); // SHARPENING → FINALIZING
            forgeScoreboard.update(p, s);
            forgeHologram.showStageHologram(p.getUniqueId(), forge, ForgeStage.FINALIZING);
            finalizeForging(p, forge, s);
        });

        activeSharpeningGames.put(player.getUniqueId(), minigame);
        minigame.start(player, session);
    }

    private void finalizeForging(Player player, ForgeStructure forge, ForgeSession session) {
        effectManager.playStageTransition(player, ForgeStage.FINALIZING);

        ForgeData playerData = forgeManager.getOrLoadData(player.getUniqueId());
        double qualityScore = qualityCalculator.calculate(
                session.getMaterialQuality(),
                session.getHeatingScore(),
                session.getHammeringScore(),
                session.getQuenchingScore(),
                session.getSharpeningScore(),
                playerData.getLevel(),
                forge.getTier().getLevel()
        );

        QualityTier tier = QualityTier.fromScore(qualityScore);
        session.setFinalQualityScore(qualityScore);
        session.setQualityTier(tier);

        double xp = qualityCalculator.calculateXP(session.getRecipe().getBaseXP(), tier, false);
        session.setXpGained(xp);

        Task.syncLater(() -> {
            session.advanceToNextStage(); // FINALIZING → COMPLETED
            effectManager.playCompletionEffect(forge, tier);

            forgeScoreboard.update(player, session);
            forgeHologram.showCompletionResult(player.getUniqueId(), forge, tier, qualityScore);

            player.sendMessage(mm.deserialize(""));
            player.sendMessage(mm.deserialize(msg("forge.result.title")));
            player.sendMessage(mm.deserialize(msg("forge.result.item").replace("%name%", session.getRecipe().getDisplayName())));
            player.sendMessage(mm.deserialize(msg("forge.result.quality").replace("%tier%", tier.getFormattedName())));
            player.sendMessage(mm.deserialize(msg("forge.result.score").replace("%score%", String.format("%.1f%%", qualityScore * 100))));
            player.sendMessage(mm.deserialize(msg("forge.result.xp").replace("%xp%", String.format("%.0f", xp))));
            player.sendMessage(mm.deserialize(""));

            ForgeData data = playerData;
            int levelsGained = data.addXp(xp);
            data.incrementItemsForged();
            data.addPerfectStrikes(session.getPerfectStrikes());
            data.updateHighestQuality(qualityScore);
            if (tier == QualityTier.LEGENDARY) { data.incrementLegendaryForged(); }

            if (levelsGained > 0) {
                player.sendMessage(mm.deserialize(msg("forge.result.level_up").replace("%level%", String.valueOf(data.getLevel()))));
            }

            forge.incrementItemsForged();
            forge.setLastUsed(System.currentTimeMillis());

            if (repository != null) {
                repository.savePlayerData(player.getUniqueId(), data);
                repository.saveForge(forge);
            }

            String resultItemId = session.getRecipe().getResultItemId();
            ItemModule itemModule = ItemModule.getInstance();
            if (resultItemId != null && itemModule != null && itemModule.getItemManager() != null) {
                me.ray.midgard.modules.item.model.MidgardItem midgardItem = itemModule.getItemManager().getItem(resultItemId);
                if (midgardItem != null) {
                    ItemStack forgedItem = midgardItem.build();
                    if (forgedItem != null) {
                        ForgeQualityApplier.apply(forgedItem, tier, qualityScore);
                        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(forgedItem);
                        for (ItemStack leftover : overflow.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                        }
                        player.sendMessage(mm.deserialize(msg("forge.result.item_received")
                                .replace("%name%", session.getRecipe().getDisplayName())
                                .replace("%stars%", ForgeQualityApplier.getStarsDisplay(tier))));

                        // Fire ForgeCompleteEvent
                        org.bukkit.Bukkit.getPluginManager().callEvent(
                                new ForgeCompleteEvent(player, forge, session.getRecipe(),
                                        forgedItem, tier, qualityScore, xp));
                    }
                } else {
                    player.sendMessage(mm.deserialize(msg("forge.result.item_not_found").replace("%id%", resultItemId)));
                }
            }

            sessionManager.completeSession(player.getUniqueId());
        }, 40L);
    }

    // ==================== Session Callbacks ====================

    public void onSessionComplete(ForgeSession session) {
        cleanupMinigames(session.getPlayerId());

        Player completePlayer = org.bukkit.Bukkit.getPlayer(session.getPlayerId());
        if (completePlayer != null) {
            Task.syncLater(() -> forgeScoreboard.hide(completePlayer), 100L);
        }

        MidgardLogger.debug("Sessão de forja completada para " + session.getPlayerId() +
                " — Qualidade: " + (session.getQualityTier() != null ? session.getQualityTier().name() : "N/A"));
    }

    public void onSessionFail(ForgeSession session) {
        Player player = org.bukkit.Bukkit.getPlayer(session.getPlayerId());
        if (player != null) {
            effectManager.playStageTransition(player, ForgeStage.FAILED);
            hideHeatingBossBar(player);
            forgeScoreboard.hide(player);
        }
        forgeHologram.removeHologram(session.getPlayerId());
        cancelHeatingEffect(session.getPlayerId());
        cleanupMinigames(session.getPlayerId());
        MidgardLogger.debug("Sessão de forja falhou para " + session.getPlayerId());
    }

    // ==================== Minigame Dispatch ====================

    public void dispatchAnvilInteract(Player player) {
        HammeringMinigame game = activeHammeringGames.get(player.getUniqueId());
        if (game != null && game.isActive()) {
            ForgeSession session = sessionManager.getSession(player.getUniqueId());
            if (session != null) { game.onAction(player, session, 0); }
        }
    }

    public void dispatchCauldronInteract(Player player) {
        QuenchingMinigame game = activeQuenchingGames.get(player.getUniqueId());
        if (game != null && game.isActive()) {
            ForgeSession session = sessionManager.getSession(player.getUniqueId());
            if (session != null) { game.onAction(player, session, 0); }
        }
    }

    public void dispatchGrindstoneInteract(Player player) {
        SharpeningMinigame game = activeSharpeningGames.get(player.getUniqueId());
        if (game != null && game.isActive()) {
            ForgeSession session = sessionManager.getSession(player.getUniqueId());
            if (session != null) { game.onAction(player, session, 0); }
        }
    }

    // ==================== Cleanup ====================

    public void cleanupMinigames(UUID playerId) {
        Player player = org.bukkit.Bukkit.getPlayer(playerId);

        HammeringMinigame hammering = activeHammeringGames.remove(playerId);
        if (hammering != null && hammering.isActive() && player != null) { hammering.stop(player, null); }

        QuenchingMinigame quenching = activeQuenchingGames.remove(playerId);
        if (quenching != null && quenching.isActive() && player != null) { quenching.stop(player, null); }

        SharpeningMinigame sharpening = activeSharpeningGames.remove(playerId);
        if (sharpening != null && sharpening.isActive() && player != null) { sharpening.stop(player, null); }
    }

    public void cancelHeatingEffect(UUID playerId) {
        BukkitTask task = heatingEffects.remove(playerId);
        if (task != null) { task.cancel(); }
    }

    public void hideHeatingBossBar(Player player) {
        BossBar bar = heatingBossBars.remove(player.getUniqueId());
        if (bar != null) { player.hideBossBar(bar); }
    }

    public void shutdown() {
        heatingEffects.values().forEach(BukkitTask::cancel);
        heatingEffects.clear();

        for (var entry : heatingBossBars.entrySet()) {
            Player p = org.bukkit.Bukkit.getPlayer(entry.getKey());
            if (p != null) { p.hideBossBar(entry.getValue()); }
        }
        heatingBossBars.clear();

        for (UUID playerId : new HashSet<>(activeHammeringGames.keySet())) {
            cleanupMinigames(playerId);
        }
        for (UUID playerId : new HashSet<>(activeQuenchingGames.keySet())) {
            cleanupMinigames(playerId);
        }
        for (UUID playerId : new HashSet<>(activeSharpeningGames.keySet())) {
            cleanupMinigames(playerId);
        }
    }
}

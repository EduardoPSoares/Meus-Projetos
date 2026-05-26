package me.ray.midgard.modules.professions.blacksmith.forge;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.database.DatabaseManager;
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
import me.ray.midgard.modules.professions.blacksmith.forge.ghost.GhostBlockManager;
import me.ray.midgard.modules.professions.blacksmith.forge.gui.ForgeMainGui;
import me.ray.midgard.modules.professions.blacksmith.forge.gui.RecipeBookGui;
import me.ray.midgard.modules.professions.blacksmith.forge.listener.ForgeBuildListener;
import me.ray.midgard.modules.professions.blacksmith.forge.listener.ForgeInteractListener;
import me.ray.midgard.modules.professions.blacksmith.forge.listener.ForgePlayerListener;
import me.ray.midgard.modules.professions.blacksmith.forge.admin.ForgeCreationManager;
import me.ray.midgard.modules.professions.blacksmith.forge.quality.QualityCalculator;
import me.ray.midgard.modules.professions.blacksmith.forge.recipe.ForgeRecipe;
import me.ray.midgard.modules.professions.blacksmith.forge.recipe.ForgeRecipeManager;
import me.ray.midgard.modules.professions.blacksmith.forge.session.ForgeSessionManager;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central orchestrator for the entire forge system.
 * Coordinates all subsystems: structures, sessions, mini-games,
 * ghost blocks, recipes, effects, and persistence.
 */
public class ForgeManager {

    private final ProfessionsModule module;
    private final JavaPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Subsystems
    private final ForgeRegistry registry;
    private final ForgeSessionManager sessionManager;
    private final ForgeRecipeManager recipeManager;
    private final GhostBlockManager ghostBlockManager;
    private final ForgeEffectManager effectManager;
    private final QualityCalculator qualityCalculator;
    private final FuelManager fuelManager;
    private final ForgeScoreboard forgeScoreboard;
    private final ForgeHologram forgeHologram;

    // Admin forge creation
    private ForgeCreationManager creationManager;

    // Persistence
    private ForgeRepository repository;

    // Player data cache: UUID → ForgeData
    private final Map<UUID, ForgeData> playerDataCache = new ConcurrentHashMap<>();

    // Workflow service: handles all forging phases and minigames
    private ForgeWorkflowService workflowService;

    // Listeners (held for reference)
    private ForgeInteractListener interactListener;
    private ForgeBuildListener buildListener;
    private ForgePlayerListener playerListener;

    // Ambient task
    private BukkitTask ambientTask;

    // Recipe search: pending chat input callbacks
    private final Map<UUID, RecipeSearchCallback> pendingRecipeSearch = new ConcurrentHashMap<>();
    private record RecipeSearchCallback(ForgeStructure forge) {}

    // Schematic cache: forgeId → loaded schematic (avoids repeated DB queries)
    private final Map<UUID, ForgeSchematic> schematicCache = new ConcurrentHashMap<>();

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }

    public ForgeManager(ProfessionsModule module) {
        this.module = module;
        this.plugin = module.getPlugin();
        this.registry = new ForgeRegistry();
        this.sessionManager = new ForgeSessionManager();
        this.recipeManager = new ForgeRecipeManager();
        this.ghostBlockManager = new GhostBlockManager(plugin);
        this.effectManager = new ForgeEffectManager();
        this.qualityCalculator = new QualityCalculator();
        this.fuelManager = new FuelManager();
        this.forgeScoreboard = new ForgeScoreboard();
        this.forgeHologram = new ForgeHologram();
    }

    /**
     * Initializes the forge system — called from ProfessionsModule.onEnable().
     */
    public void initialize() {
        MidgardLogger.info("Inicializando sistema de Forja...");

        // Initialize database repository
        DatabaseManager dbManager = MidgardCore.getDatabaseManager();
        if (dbManager != null) {
            this.repository = new ForgeRepository(dbManager);
            loadForgesFromDb();
        }

        // Load recipes from config
        recipeManager.loadFromConfig(module.getConfig().getConfigurationSection("forge.recipes"));

        // Collect admin forge recipes from item module
        collectAndRegisterAdminRecipes();

        // Create workflow service (manages forging phases and minigames)
        this.workflowService = new ForgeWorkflowService(
                this, sessionManager, effectManager, qualityCalculator,
                fuelManager, forgeScoreboard, forgeHologram, registry, repository);

        // Start subsystems
        sessionManager.start();
        ghostBlockManager.start();

        // Session callbacks → delegate to workflow service
        sessionManager.setOnSessionComplete(workflowService::onSessionComplete);
        sessionManager.setOnSessionFail(workflowService::onSessionFail);

        // Register listeners
        registerListeners();

        // Initialize admin forge creation
        creationManager = new ForgeCreationManager(this);
        creationManager.loadTemplates();
        plugin.getServer().getPluginManager().registerEvents(creationManager, plugin);

        // Start ambient effect task (idle smoke from forges)
        ambientTask = Task.syncTimer(() -> {
            for (ForgeStructure forge : registry.getAll()) {
                if (forge.isActive() && !sessionManager.isForgeInUse(forge.getForgeId())) {
                    effectManager.playIdleSmoke(forge);
                }
            }
        }, 100L, 80L); // Every 4 seconds

        MidgardLogger.info("Sistema de Forja inicializado! " + registry.size() + " forjas carregadas, "
                + recipeManager.size() + " receitas.");
    }

    /**
     * Loads admin forge recipes from the item module and registers them.
     */
    private void collectAndRegisterAdminRecipes() {
        // Recipes from the item system's MidgardRecipe with type FORGE
        // are collected on-the-fly in openRecipeBook, so nothing extra needed here.
    }

    /**
     * Shuts down the forge system — called from ProfessionsModule.onDisable().
     */
    public void shutdown() {
        MidgardLogger.info("Desligando sistema de Forja...");

        if (ambientTask != null) { ambientTask.cancel(); }

        // Shutdown workflow service (cleans up heating effects, BossBars, minigames)
        if (workflowService != null) { workflowService.shutdown(); }

        // Remove all display elements
        forgeHologram.removeAll();

        sessionManager.shutdown();
        ghostBlockManager.shutdown();

        // Save all cached player data
        if (repository != null) {
            for (var entry : playerDataCache.entrySet()) {
                repository.savePlayerData(entry.getKey(), entry.getValue());
            }
        }

        playerDataCache.clear();
        registry.clear();
    }

    // ==================== Core Flow ====================

    /**
     * Opens the main forge GUI for a player interacting with a forge.
     */
    public void openForgeMenu(Player player, ForgeStructure forge) {
        ForgeData data = getOrLoadData(player.getUniqueId());
        ForgeMainGui gui = new ForgeMainGui(player, forge, data.getLevel());
        gui.setFuelInfo(fuelManager);

        gui.setOnOpenRecipeBook(p -> openRecipeBook(p, forge));
        gui.setOnStartForging((p, f) -> openRecipeBook(p, f));
        gui.setOnOpenProfessionInfo(p -> sendProfessionInfo(p));

        gui.open();
    }

    /**
     * Opens the recipe book for the player at a given forge.
     */
    public void openRecipeBook(Player player, ForgeStructure forge) {
        openRecipeBook(player, forge, null);
    }

    /**
     * Opens the recipe book with an optional search query pre-applied.
     */
    public void openRecipeBook(Player player, ForgeStructure forge, String searchQuery) {
        ForgeData data = getOrLoadData(player.getUniqueId());
        List<ForgeRecipe> available = new ArrayList<>(recipeManager.getAvailableRecipes(data.getLevel(), forge.getTier()));

        // Also include FORGE recipes from the admin item system (MidgardRecipe)
        available.addAll(collectAdminForgeRecipes(data.getLevel(), forge.getTier()));

        RecipeBookGui gui = new RecipeBookGui(player, available, data.getLevel(), forge.getTier());
        gui.setOnRecipeSelected((p, recipe) -> startForging(p, forge, recipe));
        gui.setOnSearchRequested(p -> {
            p.closeInventory();
            pendingRecipeSearch.put(p.getUniqueId(), new RecipeSearchCallback(forge));
            p.sendMessage(mm.deserialize(msg("forge.recipe_search.prompt")));
        });
        if (searchQuery != null && !searchQuery.isEmpty()) {
            gui.setSearchQuery(searchQuery);
        }
        gui.open();
    }

    /**
     * Handles chat input for recipe search. Called from ForgeInteractListener.
     */
    public boolean onChatForRecipeSearch(AsyncChatEvent event) {
        RecipeSearchCallback cb = pendingRecipeSearch.remove(event.getPlayer().getUniqueId());
        if (cb == null) { return false; }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        Task.sync(event.getPlayer(), () -> openRecipeBook(event.getPlayer(), cb.forge(), text));
        return true;
    }

    /**
     * Scans all MidgardItems for FORGE-type MidgardRecipe entries
     * and converts them to ForgeRecipe objects for the recipe book.
     */
    private List<ForgeRecipe> collectAdminForgeRecipes(int playerLevel, ForgeTier forgeTier) {
        List<ForgeRecipe> result = new ArrayList<>();
        ItemModule itemModule = ItemModule.getInstance();
        if (itemModule == null) { return result; }
        var itemManager = itemModule.getItemManager();
        if (itemManager == null) { return result; }

        for (String itemId : itemManager.getItemIds()) {
            me.ray.midgard.modules.item.model.MidgardItem midgardItem = itemManager.getItem(itemId);
            if (midgardItem == null) { continue; }

            for (me.ray.midgard.modules.item.model.MidgardRecipe mr : midgardItem.getRecipes()) {
                if (mr.getType() != me.ray.midgard.modules.item.model.MidgardRecipe.RecipeType.FORGE) { continue; }
                if (mr.isHiddenFromBook()) { continue; }

                // Convert MidgardRecipe to ForgeRecipe
                ForgeRecipe fr = new ForgeRecipe(itemId + "_" + mr.getId());
                fr.setDisplayName(midgardItem.getDisplayName() != null ? midgardItem.getDisplayName() : itemId);
                fr.setResultItemId(itemId);

                // Level / Tier
                int minLevel = mr.getForgeMinLevel() > 0 ? mr.getForgeMinLevel() : 1;
                fr.setRequiredLevel(minLevel);

                String tierStr = mr.getForgeTier();
                ForgeTier tier = ForgeTier.BASIC;
                if (tierStr != null) {
                    try { tier = ForgeTier.valueOf(tierStr); } catch (IllegalArgumentException ignored) { /* fallback to BASIC */ }
                }
                fr.setRequiredForgeTier(tier);

                // Difficulty
                int difficulty = mr.getForgeDifficulty() > 0 ? mr.getForgeDifficulty() : 1;
                fr.setDifficultyMultiplier(difficulty / 5.0);

                // Ingredients: slot 0 = primary metal, slots 1-4 = secondary
                Map<Integer, String> ingredients = mr.getIngredients();
                if (ingredients.containsKey(0)) {
                    fr.setPrimaryMetal(ingredients.get(0));
                    fr.setPrimaryMetalAmount(1);
                }
                for (int i = 1; i <= 4; i++) {
                    if (ingredients.containsKey(i)) {
                        fr.addSecondaryMaterial(ingredients.get(i), 1);
                    }
                }

                // Defaults
                fr.setBaseXP(50 + (difficulty * 10));
                fr.setHeatingTime(15 + (difficulty * 2));
                fr.setHammerStrikes(10 + (difficulty * 2));
                fr.setSharpeningPasses(2 + (difficulty / 3));
                fr.setIdealTempMin(800 + (difficulty * 50));
                fr.setIdealTempMax(1000 + (difficulty * 50));
                fr.setChapter(1);

                // Link forge recipe ID if set
                if (mr.getForgeRecipeId() != null && !mr.getForgeRecipeId().isEmpty()) {
                    // If it links to an existing ForgeRecipe, skip (already in recipeManager)
                    if (recipeManager.getRecipe(mr.getForgeRecipeId()) != null) { continue; }
                }

                result.add(fr);
            }
        }
        return result;
    }

    /**
     * Starts the forging process — delegates to ForgeWorkflowService.
     */
    public void startForging(Player player, ForgeStructure forge, ForgeRecipe recipe) {
        workflowService.startForging(player, forge, recipe);
    }

    /**
     * Called when player clicks furnace during heating.
     */
    public void onRemoveFromFurnace(Player player, ForgeStructure forge) {
        workflowService.onRemoveFromFurnace(player, forge);
    }

    // ==================== Blueprint / Building ====================

    /**
     * Validates a blueprint placement and starts the preview session.
     * Called by ForgeBuildListener when a player places a blueprint item.
     */
    public void onBlueprintPlace(Player player, ForgeTier tier, UUID templateId) {
        ForgeData data = getOrLoadData(player.getUniqueId());

        if (data.getLevel() < tier.getRequiredProfessionLevel()) {
            player.sendMessage(mm.deserialize(msg("forge.build.level_insufficient").replace("%level%", String.valueOf(tier.getRequiredProfessionLevel()))));
            return;
        }

        int owned = registry.countByOwner(player.getUniqueId());
        int maxForges = 3; // TODO: Configurable
        if (owned >= maxForges) {
            player.sendMessage(mm.deserialize(msg("forge.build.max_forges").replace("%max%", String.valueOf(maxForges))));
            return;
        }

        ForgeSchematic schematic = resolveSchematic(tier, templateId);
        Location anchor = player.getTargetBlockExact(5) != null
                ? player.getTargetBlockExact(5).getLocation().add(0, 1, 0)
                : player.getLocation().toBlockLocation();

        ghostBlockManager.startPreview(player, tier, schematic, anchor);
    }

    /**
     * Called when a forge build session is completed.
     * Creates and registers the forge structure.
     */
    private void onForgeBuildComplete(Player player, me.ray.midgard.modules.professions.blacksmith.forge.ghost.GhostBlockSession buildSession) {
        Location anchor = buildSession.getAnchor();
        ForgeStructure forge = new ForgeStructure(
                UUID.randomUUID(),
                player.getUniqueId(),
                anchor.getWorld().getName(),
                anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ(),
                buildSession.getTier(),
                buildSession.getRotation()
        );

        forge.initializeInteractiveLocations(buildSession.getSchematic());
        registry.register(forge);
        schematicCache.put(forge.getForgeId(), buildSession.getSchematic());

        if (repository != null) {
            repository.saveForge(forge).thenRun(() ->
                repository.saveSchematicData(forge.getForgeId(), buildSession.getSchematic())
            );
        }

        ForgeData data = getOrLoadData(player.getUniqueId());
        data.addForge(forge.getForgeId().toString());
        data.incrementForgesBuilt();
        if (repository != null) {
            repository.savePlayerData(player.getUniqueId(), data);
        }

        player.sendMessage(mm.deserialize(msg("forge.build.activated")));
    }

    /**
     * Resolves the schematic for a blueprint placement.
     * If a templateId is provided, loads the template's custom schematic.
     * Falls back to the basic forge schematic.
     */
    private ForgeSchematic resolveSchematic(ForgeTier tier, UUID templateId) {
        if (templateId != null && creationManager != null) {
            for (var template : creationManager.getTemplates()) {
                if (template.getTemplateId().equals(templateId) && template.getSchematic() != null) {
                    return template.getSchematic();
                }
            }
        }
        return ForgeSchematic.createBasicForge();
    }



    // ==================== Player Data ====================

    public ForgeData getOrLoadData(UUID playerId) {
        return playerDataCache.computeIfAbsent(playerId, id -> {
            if (repository != null) {
                try {
                    return repository.loadPlayerData(id).join();
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao carregar dados de forge do jogador " + id, e);
                }
            }
            return new ForgeData();
        });
    }

    public void unloadPlayerData(UUID playerId) {
        ForgeData data = playerDataCache.remove(playerId);
        if (data != null && repository != null) {
            repository.savePlayerData(playerId, data);
        }
        // Cleanup display elements
        forgeScoreboard.cleanup(playerId);
        forgeHologram.removeHologram(playerId);
    }

    // ==================== MidgardItem Helpers ====================

    public int countMidgardItems(Player player, String itemId) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is == null) { continue; }
            String id = me.ray.midgard.modules.item.utils.ItemPDC.getMidgardId(is);
            if (itemId.equals(id)) { count += is.getAmount(); }
        }
        return count;
    }

    public void removeMidgardItems(Player player, String itemId, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack is = contents[i];
            if (is == null) { continue; }
            String id = me.ray.midgard.modules.item.utils.ItemPDC.getMidgardId(is);
            if (itemId.equals(id)) {
                if (is.getAmount() <= remaining) {
                    remaining -= is.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    is.setAmount(is.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
    }

    // ==================== Helpers ====================

    private void registerListeners() {
        interactListener = new ForgeInteractListener(registry, sessionManager);
        interactListener.setOnSmithingTableInteract(this::openForgeMenu);
        interactListener.setOnFurnaceInteract(this::onRemoveFromFurnace);

        // Anvil: dispatch to active hammering minigame
        interactListener.setOnAnvilInteract((p, f) -> workflowService.dispatchAnvilInteract(p));

        // Cauldron: dispatch to active quenching minigame
        interactListener.setOnCauldronInteract((p, f) -> workflowService.dispatchCauldronInteract(p));

        // Grindstone: dispatch to active sharpening minigame
        interactListener.setOnGrindstoneInteract((p, f) -> workflowService.dispatchGrindstoneInteract(p));

        // Fuel deposit: right-click any forge block while holding fuel
        // Also collects dropped fuel items from the zone
        interactListener.setOnFuelZoneInteract((p, f) -> {
            ForgeData data = getOrLoadData(p.getUniqueId());
            int level = data.getLevel();
            // First try to deposit from hand
            boolean deposited = fuelManager.depositFuelFromHand(f.getForgeId(), p, level);
            // Also collect any dropped items in the fuel zone
            fuelManager.collectFuelFromZone(f.getForgeId(), f.getFuelZoneLocations(), p, level);
            return deposited;
        });

        buildListener = new ForgeBuildListener(ghostBlockManager, registry);
        buildListener.setOnBlueprintValidation((player, tier, templateId) -> onBlueprintPlace(player, tier, templateId));
        ghostBlockManager.setOnBuildComplete(this::onForgeBuildComplete);

        playerListener = new ForgePlayerListener(sessionManager, ghostBlockManager, this);

        plugin.getServer().getPluginManager().registerEvents(interactListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(buildListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(playerListener, plugin);
    }

    private void loadForgesFromDb() {
        if (repository == null) { return; }
        List<ForgeStructure> forges = repository.loadAllForges();
        for (ForgeStructure forge : forges) {
            // Try to load a custom schematic for this forge; fall back to the basic one
            ForgeSchematic schematic = repository.loadSchematicData(forge.getForgeId(), forge.getTier());
            if (schematic == null) {
                schematic = ForgeSchematic.createBasicForge();
            }
            schematicCache.put(forge.getForgeId(), schematic);
            forge.initializeInteractiveLocations(schematic);
            registry.register(forge);
        }
    }

    /**
     * Returns the cached schematic for a forge, loading from DB if not cached.
     * Falls back to the basic forge schematic if none exists.
     */
    public ForgeSchematic getSchematicFor(ForgeStructure forge) {
        return schematicCache.computeIfAbsent(forge.getForgeId(), id -> {
            if (repository != null) {
                ForgeSchematic loaded = repository.loadSchematicData(id, forge.getTier());
                if (loaded != null) { return loaded; }
            }
            return ForgeSchematic.createBasicForge();
        });
    }

    /**
     * Caches a schematic for a forge (call after building/saving).
     */
    public void cacheSchematic(UUID forgeId, ForgeSchematic schematic) {
        schematicCache.put(forgeId, schematic);
    }

    /**
     * Removes a schematic from cache (call after deleting a forge).
     */
    public void evictSchematic(UUID forgeId) {
        schematicCache.remove(forgeId);
    }

    private void sendProfessionInfo(Player player) {
        ForgeData data = getOrLoadData(player.getUniqueId());
        player.sendMessage(mm.deserialize(""));
        player.sendMessage(mm.deserialize(msg("forge.stats.title")));
        player.sendMessage(mm.deserialize(msg("forge.stats.level").replace("%level%", String.valueOf(data.getLevel()))));
        player.sendMessage(mm.deserialize(msg("forge.stats.xp")
                .replace("%current%", String.format("%.0f", data.getXp()))
                .replace("%max%", String.format("%.0f", data.getXpToNextLevel()))
                .replace("%percent%", String.format("%.1f%%", data.getProgressPercent()))));
        if (data.hasSpecialization()) {
            player.sendMessage(mm.deserialize(msg("forge.stats.specialization").replace("%spec%", data.getSpecialization())));
        }
        player.sendMessage(mm.deserialize(msg("forge.stats.items_forged").replace("%count%", String.valueOf(data.getTotalItemsForged()))));
        player.sendMessage(mm.deserialize(msg("forge.stats.legendary_items").replace("%count%", String.valueOf(data.getLegendaryItemsForged()))));
        player.sendMessage(mm.deserialize(msg("forge.stats.best_quality").replace("%value%", String.format("%.1f%%", data.getHighestQualityScore() * 100))));
        player.sendMessage(mm.deserialize(msg("forge.stats.forges_built").replace("%count%", String.valueOf(data.getTotalForgesBuilt()))));
        player.sendMessage(mm.deserialize(""));
    }

    // ==================== Getters ====================

    public ForgeRegistry getRegistry() { return registry; }
    public ForgeSessionManager getSessionManager() { return sessionManager; }
    public ForgeRecipeManager getRecipeManager() { return recipeManager; }
    public GhostBlockManager getGhostBlockManager() { return ghostBlockManager; }
    public ForgeEffectManager getEffectManager() { return effectManager; }
    public ForgeRepository getRepository() { return repository; }
    public FuelManager getFuelManager() { return fuelManager; }
    public ForgeScoreboard getForgeScoreboard() { return forgeScoreboard; }
    public ForgeHologram getForgeHologram() { return forgeHologram; }
    public ForgeCreationManager getCreationManager() { return creationManager; }
    public ForgeWorkflowService getWorkflowService() { return workflowService; }
}

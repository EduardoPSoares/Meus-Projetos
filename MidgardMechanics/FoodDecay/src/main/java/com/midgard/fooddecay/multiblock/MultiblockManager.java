package com.midgard.fooddecay.multiblock;

import com.midgard.core.MidgardCore;
import com.midgard.core.item.ItemBuilder;
import com.midgard.core.utils.MessageUtils;
import com.midgard.core.utils.TextUtils;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayManager;
import com.midgard.fooddecay.FoodDecayModule;
import com.midgard.fooddecay.FoodDecayPlugin;
import com.midgard.fooddecay.FoodTrait;
import com.midgard.fooddecay.NutritionManager;
import com.midgard.fooddecay.gui.MultiblockInspectionGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core manager for the multiblock system.
 * Coordinates building sessions, food processing, and periodic tasks.
 * Delegates animations, QTE events, quality, persistence, and GUI to helper classes.
 */
public class MultiblockManager {

    private static final long NIGHT_START_TICK = 12300L;
    private static final long NIGHT_END_TICK = 23850L;
    private static final int PROGRESS_BAR_SIZE = 20;

    private final FoodDecayConfig config;
    private final FoodDecayManager manager;
    private final NutritionManager nutritionManager;
    private final NamespacedKey blueprintKey;
    private final NamespacedKey blueprintTierKey;
    private final NamespacedKey controlKey;
    private final NamespacedKey entityTagKey;
    private final NamespacedKey discoveryKey;
    private final NamespacedKey lastRecipeKey;
    private final SessionInventoryBackupStore inventoryBackupStore;
    private final RecipeDiscoveryService recipeDiscoveryService;

    private final Map<UUID, BuildSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<Location, ProcessingMultiblock> activeMultiblocks = new ConcurrentHashMap<>();

    private int taskId = -1;
    private int autosaveCounter;

    public MultiblockManager(FoodDecayModule module) {
        this.config = module.getDecayConfig();
        this.manager = module.getManager();
        this.nutritionManager = module.getNutritionManager();
        this.blueprintKey = new NamespacedKey(MidgardCore.getInstance(), "blueprint_type");
        this.blueprintTierKey = new NamespacedKey(MidgardCore.getInstance(), "blueprint_tier");
        this.controlKey = new NamespacedKey(MidgardCore.getInstance(), "mb_control");
        this.entityTagKey = new NamespacedKey(MidgardCore.getInstance(), "mb_entity");
        this.discoveryKey = new NamespacedKey(MidgardCore.getInstance(), RecipeDiscoveryService.DISCOVERY_KEY_NAME);
        this.lastRecipeKey = new NamespacedKey(MidgardCore.getInstance(), "last_recipe");
        this.inventoryBackupStore = new SessionInventoryBackupStore(FoodDecayPlugin.getInstance(), config);
        this.recipeDiscoveryService = new RecipeDiscoveryService(config, discoveryKey);
    }

    public NamespacedKey getEntityTagKey() { return entityTagKey; }

    // =========================================================================
    //  Session Inventory Backup (crash-safe)
    // =========================================================================

    /**
     * Restores inventory from a crash backup if one exists for this player.
     * Called on PlayerJoinEvent. Returns true if inventory was restored.
     */
    public boolean restoreInventoryBackup(Player player) {
        return inventoryBackupStore.restore(player);
    }

    // =========================================================================
    //  Blueprint Item
    // =========================================================================

    public ItemStack createBlueprintItem(MultiblockType type) {
        return createBlueprintItem(type, 1);
    }

    public ItemStack createBlueprintItem(MultiblockType type, int tier) {
        tier = Math.max(1, Math.min(tier, type.getMaxTier()));
        String displayName = type.getDisplayName(tier);
        ItemBuilder builder = new ItemBuilder(Material.PAPER)
                .name(sc(config.msg("blueprint-item-name").replace("{name}", displayName)))
                .lore(type.getDescription(tier));

        List<MultiblockRecipe> recipes = config.getRecipes(type);
        int processingMin = type.getDefaultProcessingMinutes(tier);
        if (!recipes.isEmpty()) {
            builder.addLore("",
                    sc(config.msg("blueprint-recipes-label").replace("{count}", String.valueOf(recipes.size()))),
                    sc(config.msg("blueprint-time-default").replace("{time}", String.valueOf(processingMin))));
            for (MultiblockRecipe recipe : recipes) {
                builder.addLore(sc("&8  ▸ &7" + recipe.getInputDisplayName()
                        + " &8→ &f" + recipe.getOutputDisplayName()
                        + " &8(" + recipe.getTimeMinutes() + "ᴍ)"));
            }
        } else {
            builder.addLore("",
                    sc(config.msg("blueprint-time-simple").replace("{time}", String.valueOf(processingMin))),
                    sc(config.msg("blueprint-trait-label").replace("{trait}", config.getTraitDisplayName(type.getResultTrait()))));
        }

        builder.addLore("",
                sc("&7Tier: &f" + tier),
                sc(config.msg("blueprint-use-hint")));

        return builder.glow()
                .persistentData(blueprintKey, PersistentDataType.STRING, type.getConfigKey())
                .persistentData(blueprintTierKey, PersistentDataType.INTEGER, tier)
                .build();
    }

    public MultiblockType getBlueprintType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String key = item.getItemMeta().getPersistentDataContainer()
                .get(blueprintKey, PersistentDataType.STRING);
        return key != null ? MultiblockType.fromKey(key) : null;
    }

    public int getBlueprintTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 1;
        Integer tier = item.getItemMeta().getPersistentDataContainer()
                .get(blueprintTierKey, PersistentDataType.INTEGER);
        return tier != null ? tier : 1;
    }

    // =========================================================================
    //  Building Session
    // =========================================================================

    public void startSession(Player player, MultiblockType type, int tierOverride, Location groundBlock) {
        BuildSession existing = activeSessions.remove(player.getUniqueId());
        if (existing != null) {
            inventoryBackupStore.delete(player.getUniqueId());
            existing.restoreInventory(player);
            existing.cancel();
        }

        if (!config.isMultiblockEnabled() || !config.isMultiblockTypeEnabled(type)) {
            player.sendMessage(MessageUtils.toComponent(config.msg("blueprint-type-disabled")));
            return;
        }

        int tier = tierOverride > 0 ? tierOverride : config.getMachineTier(player);
        int minY = type.getMinY(tier);
        Location anchor = new Location(
                groundBlock.getWorld(),
                groundBlock.getBlockX(),
                groundBlock.getBlockY() + 1 + Math.abs(minY),
                groundBlock.getBlockZ()
        );

        ItemStack hand = player.getInventory().getItemInMainHand();
        hand.setAmount(hand.getAmount() - 1);

        BuildSession session = new BuildSession(player.getUniqueId(), type, tier, anchor, entityTagKey, config);
        session.startPositioning(player);
        activeSessions.put(player.getUniqueId(), session);

        // Crash-safe: backup original inventory to disk immediately
        inventoryBackupStore.save(player.getUniqueId(), session.getSavedInventory());

        setupControlHotbar(player);

        player.playSound(player.getLocation(),
                Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.8f, 1.2f);
        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("session-positioning")
                        .replace("{name}", type.getDisplayName(tier)))));
        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("session-positioning-hint"))));
    }

    // =========================================================================
    //  Control Items & Hotbar
    // =========================================================================

    private ItemStack makeControl(String action, String name, Material mat, String... lore) {
        return new ItemBuilder(mat)
                .name(name)
                .lore(lore)
                .persistentData(controlKey, PersistentDataType.STRING, action)
                .build();
    }

    private void setupControlHotbar(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(0, makeControl("rotate",
                sc(config.msg("hotbar-rotate-name")), Material.COMPASS,
                "", sc(config.msg("hotbar-rotate-lore-1")),
                "", sc(config.msg("hotbar-rotate-lore-2"))));
        player.getInventory().setItem(2, makeControl("x",
                sc(config.msg("hotbar-axis-x-name")), Material.BLUE_DYE,
                "", sc(config.msg("hotbar-axis-x-lore-1")),
                "", sc(config.msg("hotbar-axis-x-lore-2")),
                sc(config.msg("hotbar-axis-x-lore-3"))));
        player.getInventory().setItem(3, makeControl("y",
                sc(config.msg("hotbar-axis-y-name")), Material.LIME_DYE,
                "", sc(config.msg("hotbar-axis-y-lore-1")),
                "", sc(config.msg("hotbar-axis-y-lore-2")),
                sc(config.msg("hotbar-axis-y-lore-3"))));
        player.getInventory().setItem(4, makeControl("z",
                sc(config.msg("hotbar-axis-z-name")), Material.MAGENTA_DYE,
                "", sc(config.msg("hotbar-axis-z-lore-1")),
                "", sc(config.msg("hotbar-axis-z-lore-2")),
                sc(config.msg("hotbar-axis-z-lore-3"))));
        player.getInventory().setItem(6, makeControl("confirm",
                sc(config.msg("hotbar-confirm-name")), Material.EMERALD,
                "", sc(config.msg("hotbar-confirm-lore-1")),
                sc(config.msg("hotbar-confirm-lore-2")),
                "", sc(config.msg("hotbar-confirm-lore-3"))));
        player.getInventory().setItem(8, makeControl("cancel",
                sc(config.msg("hotbar-cancel-name")), Material.BARRIER,
                "", sc(config.msg("hotbar-cancel-lore-1")),
                sc(config.msg("hotbar-cancel-lore-2")),
                "", sc(config.msg("hotbar-cancel-lore-3"))));
    }

    public String getControlAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(controlKey, PersistentDataType.STRING);
    }

    public void handleControl(Player player, String action, boolean rightClick) {
        BuildSession session = activeSessions.get(player.getUniqueId());
        if (session == null || session.getPhase() != BuildSession.Phase.POSITIONING) return;

        int dir = rightClick ? 1 : -1;
        switch (action) {
            case "rotate" -> session.rotate(player);
            case "x" -> session.moveAnchor(player, dir, 0, 0);
            case "y" -> session.moveAnchor(player, 0, dir, 0);
            case "z" -> session.moveAnchor(player, 0, 0, dir);
            case "confirm" -> confirmPosition(player);
            case "cancel" -> cancelSession(player);
        }
    }

    private void confirmPosition(Player player) {
        BuildSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        if (!session.confirmPosition()) {
            player.playSound(player.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
            player.sendMessage(MessageUtils.toComponent(
                    sc(config.msg("session-area-blocked"))));
            return;
        }

        session.restoreInventory(player);
        inventoryBackupStore.delete(player.getUniqueId());

        player.playSound(player.getLocation(),
                Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.5f);
        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("session-confirmed"))));

        Component cancel = Component.text("[")
                .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
                .append(Component.text(config.msg("session-cancel-button"))
                        .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/fd blueprint cancel"))
                        .hoverEvent(HoverEvent.showText(
                                MessageUtils.toComponent(sc(config.msg("session-cancel-hover"))))))
                .append(Component.text("]")
                        .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));

        player.sendMessage(Component.empty()
                .append(MessageUtils.toComponent(sc(config.msg("session-cancel-hint"))))
                .append(cancel));
    }

    public boolean isInPositioningPhase(UUID playerId) {
        BuildSession session = activeSessions.get(playerId);
        return session != null && session.getPhase() == BuildSession.Phase.POSITIONING;
    }

    public void cancelSession(Player player) {
        BuildSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) {
            player.sendMessage(MessageUtils.toComponent(
                    sc(config.msg("blueprint-no-active"))));
            return;
        }

        if (session.getPhase() == BuildSession.Phase.POSITIONING) {
            session.restoreInventory(player);
        }
        session.cancel();
        inventoryBackupStore.delete(player.getUniqueId());

        ItemStack blueprint = createBlueprintItem(session.getType(), session.getTier());
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(blueprint);
        if (!overflow.isEmpty()) {
            overflow.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item));
        }

        player.playSound(player.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("session-cancelled")
                        .replace("{name}", session.getType().getDisplayName(session.getTier())))));
    }

    public boolean onSessionBlockPlace(Player player, Block block) {
        BuildSession session = activeSessions.get(player.getUniqueId());
        if (session == null || session.getPhase() != BuildSession.Phase.BUILDING) return false;

        int result = session.onBlockPlace(block);
        if (result == 0) return false;

        if (result == -1) {
            Material expected = session.getExpectedMaterial(block.getLocation());
            player.playSound(player.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
            player.sendMessage(MessageUtils.toComponent(
                    sc(config.msg("session-wrong-block")
                            .replace("{material}", TextUtils.formatEnum(expected)))));
            return true;
        }

        float pitch = 1.0f + (session.getPlacedCount() * 0.08f);
        player.playSound(player.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, Math.min(pitch, 2.0f));
        block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                block.getLocation().add(0.5, 0.5, 0.5),
                10, 0.3, 0.3, 0.3, 0.05);

        if (session.isComplete()) {
            completeSession(player, session);
        }

        return false;
    }

    public void onSessionBlockBreak(Player player, Block block) {
        BuildSession session = activeSessions.get(player.getUniqueId());
        if (session == null || session.getPhase() != BuildSession.Phase.BUILDING) return;

        if (session.isSessionBlock(block)) {
            cancelSession(player);
        }
    }

    private void completeSession(Player player, BuildSession session) {
        activeSessions.remove(player.getUniqueId());
        inventoryBackupStore.delete(player.getUniqueId());
        session.removeBossBar();

        List<Location> blocks = session.getAllBlockLocations();
        ProcessingMultiblock mb = new ProcessingMultiblock(
                session.getType(), blocks, session.getCurrentRotation());
        mb.tier = session.getTier();
        activeMultiblocks.put(blocks.getFirst(), mb);

        Location center = session.getAnchor().clone().add(0.5, 1.5, 0.5);
        center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                center, 50, 1, 1, 1, 0.1);
        center.getWorld().spawnParticle(Particle.FIREWORK,
                center, 20, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(),
                Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        player.playSound(player.getLocation(),
                Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);

        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("session-complete")
                        .replace("{name}", session.getType().getDisplayName(session.getTier())))));
        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("session-complete-hint"))));
    }

    // =========================================================================
    //  Active Multiblock Interaction
    // =========================================================================

    public boolean onInteract(Player player, Block block) {
        if (!config.isMultiblockEnabled()) return false;

        Location loc = block.getLocation();
        ProcessingMultiblock mb = activeMultiblocks.get(loc);
        if (mb == null) return false;

        // Right-click → open inspection GUI
        if (!player.isSneaking()) {
            new MultiblockInspectionGui(mb, config).open(player);
            return true;
        }

        // Shift+right-click → direct interaction (place food, resources, collect)
        return mb.hasFood()
                ? handleFoodInteraction(mb, player, loc)
                : handleEmptyInteraction(mb, player, loc);
    }

    private boolean handleFoodInteraction(ProcessingMultiblock mb, Player player, Location loc) {
        if (mb.eventActive) {
            MultiblockEventHandler.handleInteraction(mb, player, loc, config);
            return true;
        }
        if (mb.completedTime > 0) {
            collectCompleted(mb, player, loc);
            return true;
        }
        if (mb.isComplete(config)) {
            completeProcessing(mb, player, loc);
            return true;
        }

        for (ProcessingSlot slot : mb.extraSlots) {
            if (slot.completedTime > 0) {
                collectExtraSlot(mb, slot, player, loc);
                return true;
            }
            if (slot.isComplete(config, mb.type)) {
                autoCompleteExtraSlot(mb, slot, loc);
                collectExtraSlot(mb, slot, player, loc);
                return true;
            }
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (getBlueprintType(hand) != null) return false;
        if (handleResourceInput(mb, player, hand)) return true;

        if (!hand.getType().isAir() && hand.getType().isEdible()) {
            int maxSlots = config.getMaxSlots(player);
            if (mb.getActiveSlotCount() < maxSlots) {
                startExtraSlotProcessing(mb, player, hand, loc);
                return true;
            }
        }
        showProgress(mb, player);
        return true;
    }

    private boolean handleEmptyInteraction(ProcessingMultiblock mb, Player player, Location loc) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (getBlueprintType(hand) != null) return false;
        if (handleResourceInput(mb, player, hand)) return true;

        if (hand.getType().isAir() || !hand.getType().isEdible()) {
            player.sendMessage(MessageUtils.toComponent(sc(getResourceHint(mb))));
            return true;
        }
        startProcessing(mb, player, hand, loc);
        return true;
    }

    // =========================================================================
    //  Resource Input Handling
    // =========================================================================

    private boolean handleResourceInput(ProcessingMultiblock mb, Player player, ItemStack hand) {
        Material mat = hand.getType();

        if (mb.type == MultiblockType.SMOKEHOUSE) {
            if (config.getSmokehouseFuelMaterials().contains(mat)) {
                hand.setAmount(hand.getAmount() - 1);
                mb.fuel++;
                player.playSound(player.getLocation(),
                        Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.8f, 1.0f);
                player.sendMessage(MessageUtils.toComponent(
                        sc(config.msg("resource-fuel-added")
                                .replace("{count}", String.valueOf(mb.fuel))
                                .replace("{plural}", mb.fuel > 1 ? "s" : ""))));
                return true;
            }
        }

        if (mb.type == MultiblockType.SALT_BARREL) {
            if (mat == config.getSaltMaterial()) {
                hand.setAmount(hand.getAmount() - 1);
                mb.salt++;
                player.playSound(player.getLocation(),
                        Sound.BLOCK_SAND_PLACE, 0.8f, 1.2f);
                player.sendMessage(MessageUtils.toComponent(
                        sc(config.msg("resource-salt-added")
                                .replace("{count}", String.valueOf(mb.salt))
                                .replace("{max}", String.valueOf(config.getSaltRequired())))));
                return true;
            }
        }

        if (mb.type == MultiblockType.PICKLING_CAULDRON) {
            if (mat == config.getPicklingWaterMaterial() && !mb.hasWater) {
                player.getInventory().setItemInMainHand(new ItemStack(config.getPicklingWaterReturn()));
                mb.hasWater = true;
                player.playSound(player.getLocation(),
                        Sound.ITEM_BUCKET_EMPTY, 0.8f, 1.0f);
                player.sendMessage(MessageUtils.toComponent(
                        sc(config.msg("resource-water-added")
                                .replace("{status}", picklingStatus(mb)))));
                return true;
            }
            if (mat == config.getPicklingVinegarMaterial() && !mb.hasVinegar) {
                hand.setAmount(hand.getAmount() - 1);
                mb.hasVinegar = true;
                player.playSound(player.getLocation(),
                        Sound.ITEM_BOTTLE_FILL, 0.8f, 1.2f);
                player.sendMessage(MessageUtils.toComponent(
                        sc(config.msg("resource-vinegar-added")
                                .replace("{status}", picklingStatus(mb)))));
                return true;
            }
            if (config.getPicklingFuelMaterials().contains(mat) && mb.fuel <= 0) {
                hand.setAmount(hand.getAmount() - 1);
                mb.fuel++;
                player.playSound(player.getLocation(),
                        Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.8f, 1.0f);
                player.sendMessage(MessageUtils.toComponent(
                        sc(config.msg("resource-fuel-pickling-added")
                                .replace("{status}", picklingStatus(mb)))));
                return true;
            }
        }

        if (mb.type == MultiblockType.SEALING_PRESS) {
            if (mat == config.getWaxMaterial()) {
                hand.setAmount(hand.getAmount() - 1);
                mb.wax++;
                player.playSound(player.getLocation(),
                        Sound.BLOCK_BEEHIVE_SHEAR, 0.8f, 1.0f);
                player.sendMessage(MessageUtils.toComponent(
                        sc(config.msg("resource-wax-added")
                                .replace("{count}", String.valueOf(mb.wax))
                                .replace("{plural}", mb.wax > 1 ? "s" : ""))));
                return true;
            }
        }

        return false;
    }

    private String getResourceHint(ProcessingMultiblock mb) {
        int saltReq = config.getSaltRequired();
        return switch (mb.type) {
            case SMOKEHOUSE -> mb.fuel > 0
                    ? config.msg("resource-hint-smokehouse-ready")
                        .replace("{fuel}", String.valueOf(mb.fuel))
                    : config.msg("resource-hint-smokehouse-empty");
            case SALT_BARREL -> mb.salt >= saltReq
                    ? config.msg("resource-hint-salt-ready")
                        .replace("{salt}", String.valueOf(mb.salt))
                    : config.msg("resource-hint-salt-empty")
                        .replace("{salt}", String.valueOf(mb.salt))
                        .replace("{max}", String.valueOf(saltReq));
            case PICKLING_CAULDRON -> {
                boolean ready = mb.hasWater && mb.hasVinegar && mb.fuel > 0;
                yield ready
                        ? config.msg("resource-hint-pickling-ready")
                        : config.msg("resource-hint-pickling-empty")
                            .replace("{status}", picklingStatus(mb));
            }
            case SEALING_PRESS -> mb.wax > 0
                    ? config.msg("resource-hint-sealing-ready")
                        .replace("{wax}", String.valueOf(mb.wax))
                    : config.msg("resource-hint-sealing-empty");
            default -> config.msg("resource-hint-drying");
        };
    }

    // =========================================================================
    //  Food Processing
    // =========================================================================

    private record ProcessingValidation(MultiblockRecipe recipe, boolean valid) {}
    private record ExtraIngredientCheck(boolean valid, List<ItemStack> consumed, List<String> missing) {}

    private ProcessingValidation validateProcessingInput(ProcessingMultiblock mb, Player player, ItemStack food) {
        MultiblockRecipe recipe = config.findRecipe(mb.type, food);
        if (recipe == null && config.hasRecipes(mb.type)) {
            player.sendMessage(MessageUtils.toComponent(
                    sc(config.msg("processing-no-recipe")
                            .replace("{machine}", mb.type.getDisplayName(mb.tier)))));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
            return new ProcessingValidation(null, false);
        }
        if (recipe != null && recipe.getRequiresTrait() != null
                && !manager.hasTrait(food, recipe.getRequiresTrait())) {
            player.sendMessage(MessageUtils.toComponent(
                    sc(config.msg("processing-requires-trait")
                            .replace("{trait}", config.getTraitDisplayName(recipe.getRequiresTrait())))));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
            return new ProcessingValidation(null, false);
        }
        if (recipe != null && recipe.getRequiresRecipe() != null) {
            boolean passed = false;
            if (food.hasItemMeta()) {
                String last = food.getItemMeta().getPersistentDataContainer()
                        .get(lastRecipeKey, PersistentDataType.STRING);
                if (recipe.getRequiresRecipe().equals(last)) passed = true;
            }
            if (!passed) {
                player.sendMessage(MessageUtils.toComponent(
                        sc(config.msg("processing-requires-recipe")
                                .replace("{recipe}", recipe.getRequiresRecipe()))));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
                return new ProcessingValidation(null, false);
            }
        }
        if (recipe != null && recipe.getProfession() != null
                && recipe.getProfessionLevel() > 0 && MMOCoreHook.isAvailable()) {
            int level = MMOCoreHook.getProfessionLevel(player, recipe.getProfession());
            if (level < recipe.getProfessionLevel()) {
                player.sendMessage(MessageUtils.toComponent(
                        sc(config.msg("processing-requires-profession")
                                .replace("{profession}", recipe.getProfession())
                                .replace("{level}", String.valueOf(recipe.getProfessionLevel())))));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
                return new ProcessingValidation(null, false);
            }
        }
        if (!manager.isStamped(food)) manager.stampItem(food);
        FoodTrait trait = (recipe != null && recipe.getTrait() != null)
                ? recipe.getTrait()
                : (recipe == null ? mb.type.getResultTrait() : null);
        if (trait != null) {
            if (manager.hasTrait(food, trait)) {
                player.sendMessage(MessageUtils.toComponent(
                        sc(config.msg("processing-already-has-trait")
                                .replace("{trait}", config.getTraitDisplayName(trait)))));
                return new ProcessingValidation(null, false);
            }
            if (manager.getTraits(food).size() >= config.getMaxTraitsPerItem()) {
                player.sendMessage(MessageUtils.toComponent(
                        sc(config.msg("processing-max-traits")
                                .replace("{max}", String.valueOf(config.getMaxTraitsPerItem())))));
                return new ProcessingValidation(null, false);
            }
        }
        return new ProcessingValidation(recipe, true);
    }

    private void startProcessing(ProcessingMultiblock mb, Player player,
                                 ItemStack food, Location anchor) {
        var validation = validateProcessingInput(mb, player, food);
        if (!validation.valid()) return;
        MultiblockRecipe recipe = validation.recipe();
        ExtraIngredientCheck extraIngredients = checkExtraIngredients(player, recipe);
        if (!extraIngredients.valid()) {
            player.sendMessage(MessageUtils.toComponent(sc(
                    "&cFaltam ingredientes extras: &f" + String.join(", ", extraIngredients.missing()))));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
            return;
        }
        if (!validateAndConsumeResources(mb, player, anchor)) return;
        if (recipe != null && !recipe.getExtraIngredients().isEmpty()) {
            if (!consumeExtraIngredients(player, recipe)) {
                player.sendMessage(MessageUtils.toComponent(
                        sc("&cNao foi possivel consumir os ingredientes extras desta receita.")));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
                return;
            }
            food = player.getInventory().getItemInMainHand();
            if (food == null || food.getType().isAir()) {
                return;
            }
        }
        if (recipe != null) {
            registerRecipeAttempt(player, recipe);
        }

        ItemStack copy = food.clone();
        copy.setAmount(1);
        food.setAmount(food.getAmount() - 1);
        applyImplicitComplexNutrition(copy, recipe, extraIngredients.consumed());

        mb.processingFood = copy;
        mb.activeRecipe = recipe;
        mb.startTime = System.currentTimeMillis();
        mb.pausedMs = 0;
        mb.completedTime = 0;
        mb.animTick = 0;
        mb.eventActive = false;
        mb.eventsHandled = 0;
        mb.eventsMissed = 0;
        mb.qualityBonus = 0;
        mb.proximityTicks = 0;
        mb.ownerId = player.getUniqueId();

        spawnFoodDisplay(mb, anchor, copy);
        int minutes = mb.getProcessingMinutes(config);
        player.playSound(player.getLocation(),
                Sound.BLOCK_SMITHING_TABLE_USE, 0.6f, 1.0f);

        String processingName = recipe != null
                ? recipe.getOutputDisplayName()
                : config.getTraitDisplayName(mb.type.getResultTrait());
        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("processing-start")
                        .replace("{name}", processingName)
                        .replace("{minutes}", String.valueOf(minutes)))));

        if (mb.type == MultiblockType.DRYING_RACK) {
            player.sendMessage(MessageUtils.toComponent(
                    sc(config.msg("processing-drying-hint"))));
        }
    }

    private boolean validateAndConsumeResources(ProcessingMultiblock mb,
                                                 Player player, Location anchor) {
        switch (mb.type) {
            case DRYING_RACK -> {
                World world = anchor.getWorld();
                boolean skyOpen = world.getHighestBlockYAt(anchor) <= anchor.getBlockY();
                if (!skyOpen) {
                    player.sendMessage(MessageUtils.toComponent(
                            sc(config.msg("resource-need-sky"))));
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
                    return false;
                }
                long time = world.getTime();
                boolean isNight = time > NIGHT_START_TICK && time < NIGHT_END_TICK;
                if (isNight) {
                    player.sendMessage(MessageUtils.toComponent(
                            sc(config.msg("resource-need-day"))));
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
                    return false;
                }
                if (world.hasStorm()) {
                    player.sendMessage(MessageUtils.toComponent(
                            sc(config.msg("resource-need-no-rain"))));
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
                    return false;
                }
            }
            case SMOKEHOUSE -> {
                if (mb.fuel <= 0) {
                    player.sendMessage(MessageUtils.toComponent(
                            sc(config.msg("resource-need-fuel"))));
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
                    return false;
                }
                mb.fuel--;
            }
            case SALT_BARREL -> {
                if (mb.salt < config.getSaltRequired()) {
                    player.sendMessage(MessageUtils.toComponent(
                            sc(config.msg("resource-need-salt")
                                    .replace("{count}", String.valueOf(mb.salt))
                                    .replace("{max}", String.valueOf(config.getSaltRequired())))));
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
                    return false;
                }
                mb.salt -= config.getSaltRequired();
            }
            case PICKLING_CAULDRON -> {
                if (!mb.hasWater || !mb.hasVinegar || mb.fuel <= 0) {
                    player.sendMessage(MessageUtils.toComponent(
                            sc(config.msg("resource-need-pickling")
                                    .replace("{status}", picklingStatus(mb)))));
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
                    return false;
                }
                mb.hasWater = false;
                mb.hasVinegar = false;
                mb.fuel--;
            }
            case SEALING_PRESS -> {
                if (mb.wax <= 0) {
                    player.sendMessage(MessageUtils.toComponent(
                            sc(config.msg("resource-need-wax"))));
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
                    return false;
                }
                mb.wax--;
            }
        }
        return true;
    }

    private void spawnFoodDisplay(ProcessingMultiblock mb, Location anchor, ItemStack food) {
        MultiblockAnimations.DisplayPlacement dp =
                MultiblockAnimations.getDisplayPlacement(mb.type);
        Location displayLoc = anchor.clone().add(dp.offX(), dp.offY(), dp.offZ());
        ItemDisplay display = (ItemDisplay) anchor.getWorld().spawnEntity(
                displayLoc, EntityType.ITEM_DISPLAY);
        display.setItemStack(food);
        float hs = dp.scale() / 2f;
        display.setTransformation(new Transformation(
                new Vector3f(-hs, 0, -hs),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(dp.scale(), dp.scale(), dp.scale()),
                new AxisAngle4f(0, 0, 0, 1)
        ));
        display.setBillboard(dp.billboard());
        display.setBrightness(new Display.Brightness(15, 15));
        display.setInterpolationDuration(20);
        display.getPersistentDataContainer().set(entityTagKey, PersistentDataType.BYTE, (byte) 1);
        mb.foodDisplay = display;
    }

    private void showProgress(ProcessingMultiblock mb, Player player) {
        long elapsed = mb.getEffectiveElapsed();
        long total = mb.getProcessingMinutes(config) * 60_000L;
        int percent = (int) Math.min(99, Math.max(0, (elapsed * 100) / total));

        int bars = PROGRESS_BAR_SIZE;
        int filled = percent * bars / 100;
        StringBuilder bar = new StringBuilder("&a");
        bar.append("\u2588".repeat(filled));
        bar.append("&8");
        bar.append("\u2591".repeat(bars - filled));

        String status = "&f" + percent + "%";
        if (mb.type == MultiblockType.DRYING_RACK && mb.pausedMs > 0) {
            Location anchor = mb.blocks.getFirst();
            World world = anchor.getWorld();
            boolean skyOpen = world.getHighestBlockYAt(anchor) <= anchor.getBlockY();
            long time = world.getTime();
            boolean isNight = time > NIGHT_START_TICK && time < NIGHT_END_TICK;
            if (!skyOpen || world.hasStorm() || isNight) {
                status += " &8| " + config.msg("processing-paused");
                if (world.hasStorm()) status += " " + config.msg("processing-paused-rain");
                else if (isNight) status += " " + config.msg("processing-paused-night");
                else status += " " + config.msg("processing-paused-sky");
            }
        }

        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("processing-progress")
                        .replace("{machine}", mb.type.getDisplayName(mb.tier))
                        .replace("{bar}", bar.toString())
                        .replace("{status}", status))));
    }

    private void completeProcessing(ProcessingMultiblock mb,
                                    Player player, Location anchor) {
        ItemStack result = buildResult(mb);

        // Capture recipe before reset clears it
        MultiblockRecipe completedRecipe = mb.activeRecipe;

        Map<Integer, ItemStack> overflow =
                player.getInventory().addItem(result);
        if (!overflow.isEmpty()) {
            Location dropLoc = anchor.clone().add(0.5, 1.5, 0.5);
            overflow.values().forEach(item ->
                    dropLoc.getWorld().dropItemNaturally(dropLoc, item));
        }

        cleanupFoodDisplay(mb);

        String traitName = completedRecipe != null
                ? completedRecipe.getOutputDisplayName()
                : config.getTraitDisplayName(mb.type.getResultTrait());

        mb.resetProcessingState();

        Location center = anchor.clone().add(0.5, 1.5, 0.5);
        anchor.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                center, 25, 0.5, 0.5, 0.5, 0.05);
        anchor.getWorld().playSound(anchor,
                mb.type.getCompleteSound(), 0.8f, 1.2f);
        player.playSound(player.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.5f);

        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("processing-preserved")
                        .replace("{name}", traitName))));

        // MMOCore experience reward
        if (completedRecipe != null
                && completedRecipe.getExperienceProfession() != null
                && completedRecipe.getExperienceReward() > 0
                && MMOCoreHook.isAvailable()) {
            MMOCoreHook.giveExperience(player,
                    completedRecipe.getExperienceProfession(),
                    completedRecipe.getExperienceReward());
        }

        if (completedRecipe != null) {
            registerRecipeCollection(player, completedRecipe);
        }
    }

    private void collectCompleted(ProcessingMultiblock mb,
                                  Player player, Location anchor) {
        MultiblockRecipe completedRecipe = mb.activeRecipe;
        Map<Integer, ItemStack> overflow =
                player.getInventory().addItem(mb.processingFood);
        if (!overflow.isEmpty()) {
            Location dropLoc = anchor.clone().add(0.5, 1.5, 0.5);
            overflow.values().forEach(item ->
                    dropLoc.getWorld().dropItemNaturally(dropLoc, item));
        }

        cleanupFoodDisplay(mb);
        mb.resetProcessingState();

        Location center = anchor.clone().add(0.5, 1.5, 0.5);
        anchor.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                center, 25, 0.5, 0.5, 0.5, 0.05);
        player.playSound(player.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.5f);
        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("processing-collected"))));

        if (completedRecipe != null) {
            registerRecipeCollection(player, completedRecipe);
        }
    }

    private void autoComplete(ProcessingMultiblock mb, Location anchor) {
        MultiblockAnimations.retractSealingPressPiston(mb, anchor);

        ItemStack result = buildResult(mb);
        mb.processingFood = result;
        mb.completedTime = System.currentTimeMillis();

        if (mb.foodDisplay != null && mb.foodDisplay.isValid()
                && mb.foodDisplay instanceof ItemDisplay id) {
            id.setItemStack(result);
        }

        Location dropLoc = anchor.clone().add(0.5, 1.5, 0.5);
        anchor.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                dropLoc, 30, 0.5, 0.5, 0.5, 0.05);
        anchor.getWorld().playSound(anchor,
                mb.type.getCompleteSound(), 0.8f, 1.2f);
        anchor.getWorld().playSound(anchor,
                Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.5f);

        notifyNearbyPlayers(anchor, mb.type, mb.tier);
    }

    /**
     * Builds the result item with traits, quality, spoiled data, and recipe tag.
     */
    private ItemStack buildResult(ProcessingMultiblock mb) {
        ItemStack result;
        if (mb.activeRecipe != null) {
            result = mb.activeRecipe.createOutput(mb.processingFood);
            if (mb.activeRecipe.getTrait() != null) {
                manager.addTrait(result, mb.activeRecipe.getTrait());
            }
            if (mb.activeRecipe.getSpoiledCustomModelData() > 0) {
                manager.setSpoiledModelData(result,
                        mb.activeRecipe.getSpoiledCustomModelData(),
                        mb.activeRecipe.getSpoiledName());
            }
            if (!manager.isStamped(result)) {
                manager.stampItem(result);
            }
        } else {
            result = mb.processingFood;
            manager.addTrait(result, mb.type.getResultTrait());
        }

        // Apply quality
        result = MultiblockQuality.applyQuality(result, mb.qualityBonus, config);

        // Tag with recipe ID for multi-stage chain
        if (mb.activeRecipe != null) {
            var rmeta = result.getItemMeta();
            if (rmeta != null) {
                rmeta.getPersistentDataContainer().set(
                        lastRecipeKey,
                        PersistentDataType.STRING, mb.activeRecipe.getId());
                result.setItemMeta(rmeta);
            }
            applyRecipeNutritionProfile(result, mb.activeRecipe, mb.processingFood);
        }

        return result;
    }

    private void spoilAndDrop(ProcessingMultiblock mb, Location anchor) {
        var meta = mb.processingFood.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtils.toComponent(sc(config.msg("processing-spoiled-name"))));
            mb.processingFood.setItemMeta(meta);
        }

        Location dropLoc = anchor.clone().add(0.5, 1.5, 0.5);
        anchor.getWorld().dropItemNaturally(dropLoc, mb.processingFood);

        cleanupFoodDisplay(mb);
        mb.resetProcessingState();

        anchor.getWorld().spawnParticle(Particle.SMOKE,
                dropLoc, 30, 0.5, 0.5, 0.5, 0.05);
        anchor.getWorld().spawnParticle(Particle.ANGRY_VILLAGER,
                dropLoc, 5, 0.3, 0.3, 0.3, 0);
        anchor.getWorld().playSound(anchor,
                Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 0.5f);

        int radius = config.getNotificationRadius();
        if (radius > 0) {
            double rSq = radius * radius;
            for (Player p : anchor.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(anchor) <= rSq) {
                    p.sendActionBar(MessageUtils.toComponent(
                            sc(config.msg("processing-spoiled")
                                    .replace("{machine}", mb.type.getDisplayName(mb.tier)))));
                }
            }
        }
    }

    private void cleanupFoodDisplay(ProcessingMultiblock mb) {
        if (mb.foodDisplay != null && mb.foodDisplay.isValid()) {
            mb.foodDisplay.remove();
            mb.foodDisplay = null;
        }
    }

    // =========================================================================
    //  Extra Slot Processing (multi-slot support)
    // =========================================================================

    private void startExtraSlotProcessing(ProcessingMultiblock mb, Player player,
                                          ItemStack food, Location anchor) {
        var validation = validateProcessingInput(mb, player, food);
        if (!validation.valid()) return;
        MultiblockRecipe recipe = validation.recipe();
        ExtraIngredientCheck extraIngredients = checkExtraIngredients(player, recipe);
        if (!extraIngredients.valid()) {
            player.sendMessage(MessageUtils.toComponent(sc(
                    "&cFaltam ingredientes extras: &f" + String.join(", ", extraIngredients.missing()))));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
            return;
        }
        if (!validateAndConsumeResources(mb, player, anchor)) return;
        if (recipe != null && !recipe.getExtraIngredients().isEmpty()) {
            if (!consumeExtraIngredients(player, recipe)) {
                player.sendMessage(MessageUtils.toComponent(
                        sc("&cNao foi possivel consumir os ingredientes extras desta receita.")));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
                return;
            }
            food = player.getInventory().getItemInMainHand();
            if (food == null || food.getType().isAir()) {
                return;
            }
        }
        if (recipe != null) {
            registerRecipeAttempt(player, recipe);
        }

        ItemStack copy = food.clone();
        copy.setAmount(1);
        food.setAmount(food.getAmount() - 1);
        applyImplicitComplexNutrition(copy, recipe, extraIngredients.consumed());

        ProcessingSlot slot = new ProcessingSlot();
        slot.food = copy;
        slot.recipe = recipe;
        slot.startTime = System.currentTimeMillis();
        slot.ownerId = player.getUniqueId();
        mb.extraSlots.add(slot);

        spawnExtraSlotDisplay(slot, mb, anchor, copy);
        int minutes = slot.getProcessingMinutes(config, mb.type);
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 0.6f, 1.0f);

        String processingName = recipe != null
                ? recipe.getOutputDisplayName()
                : config.getTraitDisplayName(mb.type.getResultTrait());
        int slotNum = mb.getActiveSlotCount();
        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("extra-slot-started")
                        .replace("{name}", processingName)
                        .replace("{minutes}", String.valueOf(minutes))
                        .replace("{slot}", String.valueOf(slotNum))
                        .replace("{max}", String.valueOf(config.getMaxSlots(player))))));
    }

    private void spawnExtraSlotDisplay(ProcessingSlot slot, ProcessingMultiblock mb,
                                       Location anchor, ItemStack food) {
        MultiblockAnimations.DisplayPlacement dp =
                MultiblockAnimations.getDisplayPlacement(mb.type);
        int slotIndex = mb.extraSlots.indexOf(slot);
        double offsetX = (slotIndex + 1) * 0.25 - 0.125;
        Location displayLoc = anchor.clone().add(
                dp.offX() + offsetX, dp.offY(), dp.offZ());
        ItemDisplay display = (ItemDisplay) anchor.getWorld().spawnEntity(
                displayLoc, EntityType.ITEM_DISPLAY);
        display.setItemStack(food);
        float hs = dp.scale() / 2f;
        display.setTransformation(new Transformation(
                new Vector3f(-hs, 0, -hs),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(dp.scale(), dp.scale(), dp.scale()),
                new AxisAngle4f(0, 0, 0, 1)
        ));
        display.setBillboard(dp.billboard());
        display.setBrightness(new Display.Brightness(15, 15));
        display.setInterpolationDuration(20);
        display.getPersistentDataContainer().set(entityTagKey, PersistentDataType.BYTE, (byte) 1);
        slot.foodDisplay = display;
    }

    private void autoCompleteExtraSlot(ProcessingMultiblock mb, ProcessingSlot slot,
                                       Location anchor) {
        ItemStack result = buildExtraSlotResult(mb, slot);
        slot.food = result;
        slot.completedTime = System.currentTimeMillis();

        if (slot.foodDisplay != null && slot.foodDisplay.isValid()
                && slot.foodDisplay instanceof ItemDisplay id) {
            id.setItemStack(result);
        }

        Location dropLoc = anchor.clone().add(0.5, 1.5, 0.5);
        anchor.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                dropLoc, 15, 0.3, 0.3, 0.3, 0.05);
        anchor.getWorld().playSound(anchor, Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, 1.5f);
    }

    private void collectExtraSlot(ProcessingMultiblock mb, ProcessingSlot slot,
                                  Player player, Location anchor) {
        MultiblockRecipe completedRecipe = slot.recipe;
        Map<Integer, ItemStack> overflow =
                player.getInventory().addItem(slot.food);
        if (!overflow.isEmpty()) {
            Location dropLoc = anchor.clone().add(0.5, 1.5, 0.5);
            overflow.values().forEach(item ->
                    dropLoc.getWorld().dropItemNaturally(dropLoc, item));
        }

        cleanupSlotDisplay(slot);

        // MMOCore experience reward
        if (slot.recipe != null
                && slot.recipe.getExperienceProfession() != null
                && slot.recipe.getExperienceReward() > 0
                && MMOCoreHook.isAvailable()) {
            MMOCoreHook.giveExperience(player,
                    slot.recipe.getExperienceProfession(),
                    slot.recipe.getExperienceReward());
        }

        mb.extraSlots.remove(slot);

        Location center = anchor.clone().add(0.5, 1.5, 0.5);
        anchor.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                center, 15, 0.3, 0.3, 0.3, 0.05);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.5f);
        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("processing-collected"))));

        if (completedRecipe != null) {
            registerRecipeCollection(player, completedRecipe);
        }
    }

    private ItemStack buildExtraSlotResult(ProcessingMultiblock mb, ProcessingSlot slot) {
        ItemStack result;
        if (slot.recipe != null) {
            result = slot.recipe.createOutput(slot.food);
            if (slot.recipe.getTrait() != null) {
                manager.addTrait(result, slot.recipe.getTrait());
            }
            if (slot.recipe.getSpoiledCustomModelData() > 0) {
                manager.setSpoiledModelData(result,
                        slot.recipe.getSpoiledCustomModelData(),
                        slot.recipe.getSpoiledName());
            }
            if (!manager.isStamped(result)) manager.stampItem(result);
        } else {
            result = slot.food;
            manager.addTrait(result, mb.type.getResultTrait());
        }

        result = MultiblockQuality.applyQuality(result, slot.qualityBonus, config);

        if (slot.recipe != null) {
            var rmeta = result.getItemMeta();
            if (rmeta != null) {
                rmeta.getPersistentDataContainer().set(
                        lastRecipeKey, PersistentDataType.STRING, slot.recipe.getId());
                result.setItemMeta(rmeta);
            }
            applyRecipeNutritionProfile(result, slot.recipe, slot.food);
        }
        return result;
    }

    private void applyRecipeNutritionProfile(ItemStack result, MultiblockRecipe recipe, ItemStack processedInput) {
        if (result == null || recipe == null) {
            return;
        }

        Set<NutritionManager.FoodGroup> groups;
        if (!recipe.getNutritionGroups().isEmpty()) {
            groups = nutritionManager.parseFoodGroups(recipe.getNutritionGroups());
        } else if (!recipe.getExtraIngredients().isEmpty() && processedInput != null) {
            groups = nutritionManager.getFoodGroups(processedInput);
        } else {
            return;
        }

        if (!groups.isEmpty()) {
            nutritionManager.setFoodGroups(result, groups);
        }
    }

    private void applyImplicitComplexNutrition(ItemStack processingFood,
                                               MultiblockRecipe recipe,
                                               List<ItemStack> consumedIngredients) {
        if (processingFood == null || recipe == null
                || recipe.getExtraIngredients().isEmpty()
                || !recipe.getNutritionGroups().isEmpty()) {
            return;
        }

        Set<NutritionManager.FoodGroup> groups = EnumSet.noneOf(NutritionManager.FoodGroup.class);
        groups.addAll(nutritionManager.getFoodGroups(processingFood));
        for (ItemStack ingredient : consumedIngredients) {
            groups.addAll(nutritionManager.getFoodGroups(ingredient));
        }
        if (!groups.isEmpty()) {
            nutritionManager.setFoodGroups(processingFood, groups);
        }
    }

    private ExtraIngredientCheck checkExtraIngredients(Player player, MultiblockRecipe recipe) {
        if (recipe == null || recipe.getExtraIngredients().isEmpty()) {
            return new ExtraIngredientCheck(true, List.of(), List.of());
        }

        ItemStack[] simulated = cloneStorageContents(player.getInventory().getStorageContents());
        return consumeExtraIngredients(simulated, player.getInventory().getHeldItemSlot(), recipe.getExtraIngredients());
    }

    private boolean consumeExtraIngredients(Player player, MultiblockRecipe recipe) {
        if (recipe == null || recipe.getExtraIngredients().isEmpty()) {
            return true;
        }

        ItemStack[] contents = cloneStorageContents(player.getInventory().getStorageContents());
        ExtraIngredientCheck consumed =
                consumeExtraIngredients(contents, player.getInventory().getHeldItemSlot(), recipe.getExtraIngredients());
        if (!consumed.valid()) {
            return false;
        }
        player.getInventory().setStorageContents(contents);
        return true;
    }

    private ExtraIngredientCheck consumeExtraIngredients(ItemStack[] contents,
                                                         int heldSlot,
                                                         List<RecipeIngredient> ingredients) {
        List<ItemStack> consumed = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (RecipeIngredient ingredient : ingredients) {
            int remaining = ingredient.getAmount();
            for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
                ItemStack stack = contents[slot];
                if (stack == null || stack.getType().isAir() || !ingredient.matches(stack)) {
                    continue;
                }

                int reserved = slot == heldSlot ? 1 : 0;
                int available = stack.getAmount() - reserved;
                if (available <= 0) {
                    continue;
                }

                int taken = Math.min(available, remaining);
                ItemStack consumedStack = stack.clone();
                consumedStack.setAmount(taken);
                consumed.add(consumedStack);

                int newAmount = stack.getAmount() - taken;
                if (newAmount <= 0) {
                    contents[slot] = null;
                } else {
                    stack.setAmount(newAmount);
                    contents[slot] = stack;
                }

                remaining -= taken;
            }

            if (remaining > 0) {
                missing.add(ingredient.getReferenceLabel());
            }
        }

        return new ExtraIngredientCheck(missing.isEmpty(), consumed, missing);
    }

    private ItemStack[] cloneStorageContents(ItemStack[] source) {
        ItemStack[] cloned = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            cloned[i] = source[i] != null ? source[i].clone() : null;
        }
        return cloned;
    }

    private void cleanupSlotDisplay(ProcessingSlot slot) {
        if (slot.foodDisplay != null && slot.foodDisplay.isValid()) {
            slot.foodDisplay.remove();
            slot.foodDisplay = null;
        }
    }

    private void spoilAndDropExtraSlot(ProcessingMultiblock mb, ProcessingSlot slot,
                                       Location anchor) {
        var meta = slot.food.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtils.toComponent(sc(config.msg("processing-spoiled-name"))));
            slot.food.setItemMeta(meta);
        }

        Location dropLoc = anchor.clone().add(0.5, 1.5, 0.5);
        anchor.getWorld().dropItemNaturally(dropLoc, slot.food);
        cleanupSlotDisplay(slot);
        mb.extraSlots.remove(slot);
    }

    private void notifyNearbyPlayers(Location anchor, MultiblockType type, int tier) {
        int radius = config.getNotificationRadius();
        if (radius <= 0) return;
        double rSq = radius * radius;
        for (Player p : anchor.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(anchor) <= rSq) {
                p.playSound(p.getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.5f);
                p.sendActionBar(MessageUtils.toComponent(
                        sc(config.msg("notify-complete")
                                .replace("{machine}", type.getDisplayName(tier)))));
            }
        }
    }

    // =========================================================================
    //  Block Break
    // =========================================================================

    public void onMultiblockBreak(Player player, Block block) {
        Location loc = block.getLocation();

        ProcessingMultiblock mb = activeMultiblocks.remove(loc);
        if (mb != null) {
            deactivateMultiblock(mb, player);
            return;
        }

        Iterator<Map.Entry<Location, ProcessingMultiblock>> it =
                activeMultiblocks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, ProcessingMultiblock> entry = it.next();
            if (entry.getValue().containsBlock(loc)) {
                deactivateMultiblock(entry.getValue(), player);
                it.remove();
                return;
            }
        }
    }

    private void deactivateMultiblock(ProcessingMultiblock mb, Player player) {
        Location anchor = mb.blocks.isEmpty() ? null : mb.blocks.getFirst();
        MultiblockAnimations.retractSealingPressPiston(mb, anchor);
        MultiblockEventHandler.forceCleanup(mb);
        if (mb.hasFood() && anchor != null) {
            Location dropLoc = anchor.clone().add(0.5, 1.5, 0.5);
            dropLoc.getWorld().dropItemNaturally(dropLoc, mb.processingFood);
            mb.processingFood = null;
        }
        cleanupFoodDisplay(mb);

        // Drop food from extra slots
        if (anchor != null) {
            for (ProcessingSlot slot : mb.extraSlots) {
                if (slot.hasFood()) {
                    Location dropLoc = anchor.clone().add(0.5, 1.5, 0.5);
                    dropLoc.getWorld().dropItemNaturally(dropLoc, slot.food);
                }
                cleanupSlotDisplay(slot);
            }
        }
        mb.extraSlots.clear();

        if (player != null) {
            player.sendMessage(MessageUtils.toComponent(
                    sc(config.msg("session-dismantled")
                            .replace("{machine}", mb.type.getDisplayName(mb.tier)))));
        }
    }

    // =========================================================================
    //  Periodic Task
    // =========================================================================

    public void startTask() {
        if (!config.isMultiblockEnabled()) return;
        MultiblockPersistence.removeOrphanedEntities(entityTagKey);
        activeMultiblocks.putAll(MultiblockPersistence.loadData(config));
        taskId = Bukkit.getScheduler().runTaskTimer(
                MidgardCore.getInstance(), this::tick, 20L, 20L
        ).getTaskId();
    }

    public void stopTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        for (Map.Entry<UUID, BuildSession> entry : activeSessions.entrySet()) {
            BuildSession session = entry.getValue();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && session.getPhase() == BuildSession.Phase.POSITIONING) {
                session.restoreInventory(player);
            }
            session.cancel();
            inventoryBackupStore.delete(entry.getKey());
        }
        activeSessions.clear();

        MultiblockPersistence.saveData(activeMultiblocks, entityTagKey);

        for (ProcessingMultiblock mb : activeMultiblocks.values()) {
            MultiblockAnimations.retractSealingPressPiston(mb,
                    mb.blocks.isEmpty() ? null : mb.blocks.getFirst());
            MultiblockEventHandler.forceCleanup(mb);
            cleanupFoodDisplay(mb);
            for (ProcessingSlot slot : mb.extraSlots) {
                cleanupSlotDisplay(slot);
            }
        }
        activeMultiblocks.clear();
    }

    private void tick() {
        long now = System.currentTimeMillis();

        if (++autosaveCounter >= 20 && !activeMultiblocks.isEmpty()) {
            autosaveCounter = 0;
            MultiblockPersistence.saveData(activeMultiblocks, entityTagKey);
        }

        tickSessions(now);
        tickActiveMultiblocks();
    }

    private void tickSessions(long now) {
        Iterator<Map.Entry<UUID, BuildSession>> sessionIt =
                activeSessions.entrySet().iterator();
        while (sessionIt.hasNext()) {
            Map.Entry<UUID, BuildSession> entry = sessionIt.next();
            BuildSession session = entry.getValue();

            if (now - session.getStartTime() > config.getSessionTimeoutMinutes() * 60_000L) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && session.getPhase() == BuildSession.Phase.POSITIONING) {
                    session.restoreInventory(player);
                }
                session.cancel();
                inventoryBackupStore.delete(entry.getKey());
                sessionIt.remove();

                if (player != null) {
                    ItemStack blueprint = createBlueprintItem(session.getType(), session.getTier());
                    Map<Integer, ItemStack> overflow =
                            player.getInventory().addItem(blueprint);
                    if (!overflow.isEmpty()) {
                        overflow.values().forEach(item ->
                                player.getWorld().dropItemNaturally(
                                        player.getLocation(), item));
                    }
                    player.sendMessage(MessageUtils.toComponent(
                            sc(config.msg("session-timeout")
                                    .replace("{machine}", session.getType().getDisplayName(session.getTier())))));
                }
            }
        }
    }

    private void tickActiveMultiblocks() {
        Iterator<Map.Entry<Location, ProcessingMultiblock>> mbIt =
                activeMultiblocks.entrySet().iterator();
        while (mbIt.hasNext()) {
            Map.Entry<Location, ProcessingMultiblock> entry = mbIt.next();
            Location anchor = entry.getKey();
            ProcessingMultiblock mb = entry.getValue();

            if (!anchor.isWorldLoaded() || !anchor.getWorld().isChunkLoaded(
                    anchor.getBlockX() >> 4, anchor.getBlockZ() >> 4)) {
                continue;
            }

            if (!MultiblockPersistence.isStructureIntact(mb)) {
                deactivateMultiblock(mb, null);
                mbIt.remove();
                continue;
            }

            if (mb.hasFood()) {
                tickMultiblockProcessing(mb, anchor);
            }

            tickExtraSlots(mb, anchor);
        }
    }

    private void tickMultiblockProcessing(ProcessingMultiblock mb, Location anchor) {
        MultiblockPersistence.ensureFoodDisplay(mb, anchor, entityTagKey);
        MultiblockAnimations.tickFoodDisplay(mb);

        if (mb.completedTime > 0) {
            MultiblockAnimations.spawnCompletionGlow(mb, anchor);
            int abandonMin = config.getAbandonmentMinutes();
            if (abandonMin > 0) {
                long waitedMs = System.currentTimeMillis() - mb.completedTime;
                if (waitedMs >= abandonMin * 60_000L) {
                    spoilAndDrop(mb, anchor);
                }
            }
            return;
        }

        if (mb.type == MultiblockType.DRYING_RACK) {
            World world = anchor.getWorld();
            boolean skyOpen = world.getHighestBlockYAt(anchor) <= anchor.getBlockY();
            long time = world.getTime();
            boolean isNight = time > NIGHT_START_TICK && time < NIGHT_END_TICK;
            if (!skyOpen || world.hasStorm() || isNight) {
                mb.pausedMs += 1000;
                if (world.hasStorm() && skyOpen) {
                    Location center = anchor.clone().add(0.5, 1.2, 0.5);
                    world.spawnParticle(Particle.DRIPPING_WATER,
                            center, 3, 0.3, 0.1, 0.3, 0);
                }
                return;
            }
        }

        MultiblockAnimations.spawnProcessingAnimation(mb, anchor, config);
        MultiblockAnimations.spawnAmbientSmoke(mb, anchor, config);
        MultiblockEventHandler.tickEvent(mb, anchor, config);
        tickProximity(mb, anchor);

        if (mb.isComplete(config) && !mb.eventActive) {
            autoComplete(mb, anchor);
        }
    }

    private void tickExtraSlots(ProcessingMultiblock mb, Location anchor) {
        if (mb.extraSlots.isEmpty()) return;

        boolean dryingPaused = false;
        if (mb.type == MultiblockType.DRYING_RACK) {
            World world = anchor.getWorld();
            boolean skyOpen = world.getHighestBlockYAt(anchor) <= anchor.getBlockY();
            long time = world.getTime();
            boolean isNight = time > NIGHT_START_TICK && time < NIGHT_END_TICK;
            dryingPaused = !skyOpen || world.hasStorm() || isNight;
        }

        Iterator<ProcessingSlot> slotIt = mb.extraSlots.iterator();
        while (slotIt.hasNext()) {
            ProcessingSlot slot = slotIt.next();
            if (!slot.hasFood()) {
                cleanupSlotDisplay(slot);
                slotIt.remove();
                continue;
            }

            // Animate food display
            if (slot.foodDisplay != null && slot.foodDisplay.isValid()) {
                slot.animTick++;
            }

            if (slot.completedTime > 0) {
                int abandonMin = config.getAbandonmentMinutes();
                if (abandonMin > 0) {
                    long waitedMs = System.currentTimeMillis() - slot.completedTime;
                    if (waitedMs >= abandonMin * 60_000L) {
                        var meta = slot.food.getItemMeta();
                        if (meta != null) {
                            meta.displayName(MessageUtils.toComponent(
                                    sc(config.msg("processing-spoiled-name"))));
                            slot.food.setItemMeta(meta);
                        }
                        Location dropLoc = anchor.clone().add(0.5, 1.5, 0.5);
                        anchor.getWorld().dropItemNaturally(dropLoc, slot.food);
                        cleanupSlotDisplay(slot);
                        slotIt.remove();
                    }
                }
                continue;
            }

            if (dryingPaused) {
                slot.pausedMs += 1000;
                continue;
            }

            if (slot.isComplete(config, mb.type)) {
                autoCompleteExtraSlot(mb, slot, anchor);
            }
        }
    }

    // =========================================================================
    //  Proximity Bonus
    // =========================================================================

    private void tickProximity(ProcessingMultiblock mb, Location anchor) {
        int radius = config.getProximityRadius();
        if (radius <= 0) return;
        double bonusPerTick = config.getProximityBonusPerTick();
        if (bonusPerTick <= 0) return;

        double rSq = radius * radius;
        boolean nearby = false;
        for (Player p : anchor.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(anchor) <= rSq) {
                nearby = true;
                break;
            }
        }
        if (nearby) {
            mb.qualityBonus += (float) bonusPerTick;
            mb.proximityTicks++;
            if (mb.proximityTicks % 30 == 0) {
                Location center = anchor.clone().add(0.5, 1.5, 0.5);
                anchor.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                        center, 3, 0.3, 0.2, 0.3, 0.02);
            }
        }
    }

    // =========================================================================
    //  Public Queries
    // =========================================================================

    public boolean hasActiveSession(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    public boolean isActiveMultiblock(Location loc) {
        if (activeMultiblocks.containsKey(loc)) return true;
        for (ProcessingMultiblock mb : activeMultiblocks.values()) {
            if (mb.containsBlock(loc)) return true;
        }
        return false;
    }

    public NamespacedKey getBlueprintKey() {
        return blueprintKey;
    }

    // =========================================================================
    //  Recipe Discovery
    // =========================================================================

    public Set<String> getDiscoveredRecipes(Player player) {
        return recipeDiscoveryService.getDiscoveredRecipes(player);
    }

    public Map<String, RecipeDiscoveryProgress> getRecipeDiscoveryProgress(Player player) {
        return recipeDiscoveryService.getDiscoveryProgress(player);
    }

    public RecipeDiscoveryProgress getRecipeDiscoveryProgress(Player player, String recipeId) {
        return recipeDiscoveryService.getDiscoveryProgress(player, recipeId);
    }

    public RecipeDiscoveryStage getRecipeDiscoveryStage(Player player, MultiblockRecipe recipe) {
        if (recipe == null) {
            return RecipeDiscoveryStage.UNKNOWN;
        }
        return recipeDiscoveryService.getDiscoveryStage(player, recipe.getId());
    }

    public void registerRecipeAttempt(Player player, MultiblockRecipe recipe) {
        recipeDiscoveryService.registerAttempt(player, recipe);
    }

    public void registerRecipeCollection(Player player, MultiblockRecipe recipe) {
        recipeDiscoveryService.registerCollection(player, recipe);
    }

    public void cleanOrphanedInChunk(Chunk chunk) {
        MultiblockPersistence.cleanOrphanedInChunk(chunk, entityTagKey, activeMultiblocks);
    }

    // =========================================================================
    //  Utilities
    // =========================================================================

    private String picklingStatus(ProcessingMultiblock mb) {
        String water = mb.hasWater   ? " &a\u2714" : " &c\u2718";
        String vinegar = mb.hasVinegar ? " &a\u2714" : " &c\u2718";
        String fuel = mb.fuel > 0    ? " &a\u2714" : " &c\u2718";
        return " &8[" + config.msg("pickling-water-label") + water
                + " &8|" + config.msg("pickling-vinegar-label") + vinegar
                + " &8|" + config.msg("pickling-coal-label") + fuel + "&8]";
    }
}

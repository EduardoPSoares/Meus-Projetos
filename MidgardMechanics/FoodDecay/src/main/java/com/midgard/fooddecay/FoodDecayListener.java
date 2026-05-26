package com.midgard.fooddecay;

import com.midgard.core.MidgardCore;
import com.midgard.core.utils.MessageUtils;
import com.midgard.fooddecay.gui.FoodInspectionGui;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listeners for the TFC-style food decay system.
 * Handles stamping, expiration blocking, trait application, and inventory updates.
 */
public class FoodDecayListener implements Listener {

    private final FoodDecayModule module;
    private final FoodDecayManager manager;
    private final FoodDecayConfig config;

    /** Debounce map — tracks the scheduled task ID per player to avoid duplicate processing. */
    private final Map<UUID, Integer> pendingRefresh = new ConcurrentHashMap<>();

    public FoodDecayListener(FoodDecayModule module) {
        this.module = module;
        this.manager = module.getManager();
        this.config = module.getDecayConfig();
    }

    /**
     * Block eating expired food and handle food portions.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();

        // Block expired food consumption
        if (config.blockExpiredConsume() && manager.isExpired(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    MessageUtils.toComponent(config.getMsgExpiredConsume())
            );
            return;
        }

        // Handle portions — consume one portion instead of the whole item
        if (config.isPortionsEnabled()) {
            // Use event.getHand() to reliably determine the correct hand slot
            org.bukkit.inventory.EquipmentSlot hand = event.getHand();
            ItemStack handItem = hand == org.bukkit.inventory.EquipmentSlot.HAND
                    ? event.getPlayer().getInventory().getItemInMainHand()
                    : event.getPlayer().getInventory().getItemInOffHand();
            if (handItem.isSimilar(item) && manager.getPortionsRemaining(handItem) > 0) {
                event.setCancelled(true);
                Player player = event.getPlayer();

                // Split off 1 item from the stack to avoid modifying all items' meta
                ItemStack single;
                boolean wasStacked = handItem.getAmount() > 1;
                if (wasStacked) {
                    single = handItem.asOne();
                    handItem.setAmount(handItem.getAmount() - 1);
                } else {
                    single = handItem;
                }

                boolean hasMore = manager.consumePortion(single);

                player.setFoodLevel(Math.min(20, player.getFoodLevel() + 2));
                player.setSaturation(Math.min(player.getFoodLevel(), player.getSaturation() + 1.0f));
                player.playSound(player.getLocation(),
                        org.bukkit.Sound.ENTITY_GENERIC_EAT, 1f, 1f);

                // Track nutrition for partial portions too
                NutritionManager nutritionManager = module.getNutritionManager();
                if (nutritionManager != null) {
                    int totalPortions = manager.getPortionsTotal(single);
                    nutritionManager.onEat(player, single, totalPortions);
                }

                // Apply fermented drink effects for portioned drinks (e.g. HONEY_BOTTLE)
                FermentationManager fermentManager = module.getFermentationManager();
                if (fermentManager != null) {
                    fermentManager.onDrinkConsume(player, single);
                }

                if (hasMore) {
                    // Item still has portions — put it back in inventory
                    if (wasStacked) {
                        player.getInventory().addItem(single).values()
                                .forEach(o -> player.getWorld().dropItemNaturally(player.getLocation(), o));
                    }
                    int remaining = manager.getPortionsRemaining(single);
                    if (remaining > 0) {
                        String msg = config.msg("portion-consumed");
                        if (msg != null && !msg.isEmpty()) {
                            player.sendActionBar(MessageUtils.toComponent(
                                    msg.replace("{remaining}", String.valueOf(remaining))));
                        }
                    }
                } else {
                    // Last portion consumed — if it was split off, it's already gone (not added back)
                    // If it was the only item, remove it from hand
                    if (!wasStacked) {
                        handItem.setAmount(0);
                    }
                    // Return empty container (vanilla logic doesn't fire because event was cancelled)
                    Material container = getContainerReturn(single.getType());
                    if (container != null) {
                        player.getInventory().addItem(new ItemStack(container)).values()
                                .forEach(o -> player.getWorld().dropItemNaturally(player.getLocation(), o));
                    }
                }
                return;
            }
        }
    }

    /**
     * Stamp food when picked up from the ground.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!config.stampOnPickup()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        player.getServer().getScheduler().runTaskLater(
                MidgardCore.getInstance(),
                () -> manager.processInventory(player),
                1L
        );
    }

    /**
     * Stamp food when it spawns in the world (drops, natural spawns).
     * Ensures all food items enter the decay system immediately.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!config.isStampOnItemSpawn()) return;
        ItemStack item = event.getEntity().getItemStack();
        if (item.getType().isEdible() && !manager.isStamped(item)) {
            manager.stampItem(item);
            event.getEntity().setItemStack(item);
        }
        // Weight stamp on spawn
        WeightManager wm = module.getWeightManager();
        if (wm != null && config.isWeightEnabled() && item.getType().isEdible() && !wm.isStamped(item)) {
            wm.stampItem(item);
            event.getEntity().setItemStack(item);
        }
    }

    /**
     * Stamp food when crafted.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!config.stampOnCraft()) return;
        ItemStack result = event.getCurrentItem();
        if (result != null) {
            manager.stampItem(result);
            // Weight stamp on craft
            WeightManager wm = module.getWeightManager();
            if (wm != null && config.isWeightEnabled() && result.getType().isEdible()) {
                wm.stampItem(result);
            }
        }
    }

    /**
     * Stamp food when extracted from a furnace/smoker.
     * Auto-apply SMOKED trait if extracted from a Smoker block.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        if (!config.stampOnFurnace()) return;
        Player player = event.getPlayer();

        // Check if the block is a Smoker for auto-smoke trait
        Block block = event.getBlock();
        boolean isSmoker = block.getType() == Material.SMOKER;
        Material extractedType = event.getItemType();

        player.getServer().getScheduler().runTaskLater(
                MidgardCore.getInstance(),
                () -> {
                    // Process inventory to stamp new items
                    manager.processInventory(player);

                    // Auto-apply SMOKED trait to freshly cooked food from smoker
                    if (isSmoker && config.isTraitsEnabled() && config.isAutoSmokeFromSmoker()) {
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item == null || item.getType().isAir()) continue;
                            if (item.getType() != extractedType) continue;
                            if (!manager.isStamped(item)) continue;
                            if (manager.hasTrait(item, FoodTrait.SMOKED)) continue;
                            if (!item.getType().isEdible()) continue;
                            if (config.neverExpires(item.getType())) continue;

                            // Apply SMOKED trait
                            if (manager.addTrait(item, FoodTrait.SMOKED)) {
                                player.sendMessage(
                                        MessageUtils.toComponent(config.getMsgPreserveAutoSmoked())
                                );
                                break; // Only apply to one stack
                            }
                        }
                    }
                },
                1L
        );
    }

    /**
     * Process and stamp food when a player opens their inventory.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!config.stampOnInventoryOpen()) return;
        if (event.getPlayer() instanceof Player player) {
            manager.processInventory(player);

            // Process container items using the container's location (for depth conservation)
            Location containerLoc = event.getInventory().getLocation();
            if (containerLoc != null) {
                Material containerType = containerLoc.getBlock().getType();
                manager.processContainer(event.getInventory(), containerLoc, containerType);
                NmsHelper.syncInventory(player);
            }
        }
    }

    /**
     * Stamp food moved in inventory via clicks.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!config.stampOnInventoryClick()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        ItemStack current = event.getCurrentItem();
        if (current != null) {
            manager.stampItem(current);
            // Weight stamp on inventory click
            WeightManager wmCurrent = module.getWeightManager();
            if (wmCurrent != null && config.isWeightEnabled() && current.getType().isEdible()) {
                wmCurrent.stampItem(current);
            }
        }
        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            manager.stampItem(cursor);
            // Weight stamp cursor item
            WeightManager wmCursor = module.getWeightManager();
            if (wmCursor != null && config.isWeightEnabled() && cursor.getType().isEdible()) {
                wmCursor.stampItem(cursor);
            }
        }
    }

    /**
     * Refresh lore dynamically when items are moved between a container and the player inventory.
     * Debounced — multiple rapid clicks within 2 ticks are coalesced into a single refresh.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onContainerInteract(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Location containerLoc = event.getView().getTopInventory().getLocation();
        if (containerLoc == null) return;

        UUID uuid = player.getUniqueId();
        // If a refresh is already scheduled for this player, skip — it will cover this click too
        if (pendingRefresh.containsKey(uuid)) return;

        Material containerType = containerLoc.getBlock().getType();
        org.bukkit.inventory.Inventory topInv = event.getView().getTopInventory();
        int taskId = Bukkit.getScheduler().runTaskLater(
                MidgardCore.getInstance(),
                () -> {
                    pendingRefresh.remove(uuid);
                    if (!player.isOnline()) return;
                    manager.processInventory(player);
                    manager.processContainer(topInv, containerLoc, containerType);
                    NmsHelper.syncInventory(player);
                },
                2L // 2 ticks — coalesces rapid clicks
        ).getTaskId();
        pendingRefresh.put(uuid, taskId);
    }

    /**
     * Refresh lore when a container is closed — ensures both player inventory and container
     * items reflect the correct context (player environment vs. container storage).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Location containerLoc = event.getInventory().getLocation();
        if (containerLoc == null) return;
        Material containerType = containerLoc.getBlock().getType();
        manager.processInventory(player);
        manager.processContainer(event.getInventory(), containerLoc, containerType);
        NmsHelper.syncInventory(player);
    }

    /**
     * Process inventory when a player joins to stamp any unstamped food
     * and update decay on existing items. Also stamps liquid containers.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        manager.resumeInventoryDecay(event.getPlayer());
        if (!config.stampOnJoin()) return;
        Player player = event.getPlayer();
        player.getServer().getScheduler().runTaskLater(
                MidgardCore.getInstance(),
                () -> {
                    manager.processInventory(player);
                    // Stamp liquid containers in inventory
                    LiquidManager liquidManager = module.getLiquidManager();
                    if (liquidManager != null && config.isLiquidContainersEnabled()) {
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null && !item.getType().isAir()) {
                                liquidManager.stampContainer(item);
                            }
                        }
                    }
                },
                5L
        );
    }

    /**
     * Freeze decay only for the player's own inventory while they are offline.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.pauseInventoryDecay(event.getPlayer());
    }

    // =====================================================
    // Nutrition tracking
    // =====================================================
    // Liquid container stamping
    // =====================================================

    /**
     * Stamp liquid container items when they are clicked or moved in inventory.
     * This ensures container items receive their PDC capacity data.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLiquidContainerStamp(InventoryClickEvent event) {
        LiquidManager liquidManager = module.getLiquidManager();
        if (liquidManager == null) return;
        if (!config.isLiquidContainersEnabled()) return;

        ItemStack current = event.getCurrentItem();
        if (current != null) liquidManager.stampContainer(current);
        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) liquidManager.stampContainer(cursor);
    }

    /**
     * Stamp liquid containers when crafted.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLiquidContainerCraft(CraftItemEvent event) {
        LiquidManager liquidManager = module.getLiquidManager();
        if (liquidManager == null) return;
        if (!config.isLiquidContainersEnabled()) return;

        ItemStack result = event.getCurrentItem();
        if (result != null) liquidManager.stampContainer(result);
    }

    // =====================================================
    // Nutrition tracking
    // =====================================================

    /**
     * Track nutrition when food is successfully consumed.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsumeNutrition(PlayerItemConsumeEvent event) {
        NutritionManager nutritionManager = module.getNutritionManager();
        if (nutritionManager != null) {
            nutritionManager.onEat(event.getPlayer(), event.getItem());
        }
    }

    /**
     * Reset nutrition on death if configured.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (!config.isNutritionResetOnDeath()) return;
        NutritionManager nutritionManager = module.getNutritionManager();
        if (nutritionManager != null) {
            nutritionManager.resetNutrition(event.getEntity());
        }
    }

    // =====================================================
    // Cauldron recipes
    // =====================================================

    /**
     * Clear cauldron ingredient tracking when the block is broken.
     * Also clear fermentation barrel mark when a barrel is broken.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCauldronBreak(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        if (type == Material.WATER_CAULDRON) {
            CauldronManager cauldronManager = module.getCauldronManager();
            if (cauldronManager != null) {
                cauldronManager.clearCauldron(event.getBlock().getLocation());
            }
        }
        if (type == Material.BARREL) {
            FermentationManager fermentManager = module.getFermentationManager();
            if (fermentManager != null) {
                fermentManager.onBarrelBreak(event.getBlock());
            }
        }
        if (type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE) {
            CookingManager cookingManager = module.getCookingManager();
            if (cookingManager != null) {
                cookingManager.onCampfireRemoved(event.getBlock().getLocation());
            }
        }
    }

    /**
     * Prevent pistons from pushing dedicated fermentation barrels.
     * Without this, a piston displaces the barrel and orphans the tracking data.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        FermentationManager fermentManager = module.getFermentationManager();
        CookingManager cookingManager = module.getCookingManager();
        for (Block block : event.getBlocks()) {
            if (fermentManager != null && block.getType() == Material.BARREL
                    && fermentManager.isFermentBarrel(block)) {
                event.setCancelled(true);
                return;
            }
            if (cookingManager != null
                    && (block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE)
                    && cookingManager.hasActiveCooking(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Prevent pistons from pulling dedicated fermentation barrels
     * or campfires with active cooking.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        FermentationManager fermentManager = module.getFermentationManager();
        CookingManager cookingManager = module.getCookingManager();
        for (Block block : event.getBlocks()) {
            if (fermentManager != null && block.getType() == Material.BARREL
                    && fermentManager.isFermentBarrel(block)) {
                event.setCancelled(true);
                return;
            }
            if (cookingManager != null
                    && (block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE)
                    && cookingManager.hasActiveCooking(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Clean up tracked fermentation barrels and cauldrons destroyed by entity explosions
     * (TNT, creepers, etc.), since BlockBreakEvent does not fire for explosions.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosionBlocks(event.blockList());
    }

    /**
     * Clean up tracked blocks destroyed by block-sourced explosions
     * (beds in wrong dimension, respawn anchors).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosionBlocks(event.blockList());
    }

    private void handleExplosionBlocks(java.util.List<Block> blockList) {
        FermentationManager fermentManager = module.getFermentationManager();
        CauldronManager cauldronManager = module.getCauldronManager();
        CookingManager cookingManager = module.getCookingManager();

        for (Block block : blockList) {
            if (block.getType() == Material.BARREL && fermentManager != null
                    && fermentManager.isFermentBarrel(block)) {
                fermentManager.onBarrelBreak(block);
            }
            if (block.getType() == Material.WATER_CAULDRON && cauldronManager != null) {
                cauldronManager.clearCauldron(block.getLocation());
            }
            if (cookingManager != null
                    && (block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE)) {
                cookingManager.onCampfireRemoved(block.getLocation());
            }
        }
    }

    /**
     * Detect placement of fermentation barrel items and mark the block.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBarrelPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.BARREL) return;
        FermentationManager fermentManager = module.getFermentationManager();
        if (fermentManager == null) return;
        if (fermentManager.isFermentBarrelItem(event.getItemInHand())) {
            fermentManager.onBarrelPlace(event.getBlockPlaced(), null);
        }
    }

    /**
     * Handle right-clicking a water cauldron with food.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCauldronInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        CauldronManager cauldronManager = module.getCauldronManager();
        if (cauldronManager == null || !config.isCauldronRecipesEnabled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.WATER_CAULDRON) return;

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!item.getType().isEdible()) return;

        if (cauldronManager.onCauldronInteract(event.getPlayer(), block, item)) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle right-clicking a composter with spoiled food.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onComposterInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        CompostManager compostManager = module.getCompostManager();
        if (compostManager == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.COMPOSTER) return;

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (compostManager.onComposterInteract(event.getPlayer(), block, item)) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle right-clicking a campfire/soul campfire with raw food to cook it.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCampfireCookInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        CookingManager cookingManager = module.getCookingManager();
        if (cookingManager == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();
        if (type != Material.CAMPFIRE && type != Material.SOUL_CAMPFIRE) return;

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (cookingManager.onCampfireInteract(event.getPlayer(), block, item)) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle right-clicking water sources/cauldrons with a liquid container,
     * or sneaking to pour liquid. Auto-stamps recognized container materials.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLiquidContainerInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        LiquidManager liquidManager = module.getLiquidManager();
        if (liquidManager == null) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Auto-stamp recognized container materials on interaction
        if (!liquidManager.isContainer(item)) {
            if (config.isLiquidContainersEnabled()) {
                liquidManager.stampContainer(item);
            }
            if (!liquidManager.isContainer(item)) return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            // Skip barrel blocks — handled by fermentation
            if (block != null && block.getType() == Material.BARREL) return;
            if (liquidManager.onContainerInteract(player, block, item, player.isSneaking())) {
                event.setCancelled(true);
                return;
            }
        }

        // Sneak + right-click air → pour out (exclude barrel blocks)
        if (player.isSneaking()
                && (event.getAction() == Action.RIGHT_CLICK_AIR
                    || (event.getAction() == Action.RIGHT_CLICK_BLOCK
                        && (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.BARREL)))) {
            if (liquidManager.onContainerPour(player, item)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Handle barrel interactions for fermentation.
     * - Right-click with container → pours liquid / drains (sneaking).
     * - Right-click empty hand → opens FermentationBarrelGui.
     * - Active fermentation: any click checks progress/collects.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onFermentBarrelInteract(PlayerInteractEvent event) {
        FermentationManager fermentManager = module.getFermentationManager();
        if (fermentManager == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BARREL) return;

        boolean isFerment = fermentManager.isFermentBarrel(block);
        if (!isFerment) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Try manager interaction first (liquid pour/drain, progress, collect)
        if (fermentManager.onBarrelInteract(player, block, item, player.isSneaking())) {
            event.setCancelled(true);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            return;
        }

        // Always cancel to prevent vanilla barrel from opening
        event.setCancelled(true);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);

        // Only open GUI when hand is empty — not when holding an unrecognized item
        if (item == null || item.getType().isAir()) {
            new com.midgard.fooddecay.gui.FermentationBarrelGui(
                    block.getLocation(), fermentManager, module.getDecayConfig()).open(player);
        }
    }

    /**
     * Safety net: block vanilla barrel inventory if it is a dedicated fermentation barrel.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFermentBarrelOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (event.getInventory().getType() != org.bukkit.event.inventory.InventoryType.BARREL) return;
        FermentationManager fermentManager = module.getFermentationManager();
        if (fermentManager == null) return;

        org.bukkit.Location loc = event.getInventory().getLocation();
        if (loc == null) return;

        if (fermentManager.isFermentBarrel(loc.getBlock())) {
            event.setCancelled(true);
        }
    }

    /**
     * Apply fermentation drink effects when consumed.
     * Uses MONITOR to avoid double-application when onConsume(HIGH) cancels for portioned drinks.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrinkConsume(PlayerItemConsumeEvent event) {
        FermentationManager fermentManager = module.getFermentationManager();
        if (fermentManager == null) return;

        fermentManager.onDrinkConsume(event.getPlayer(), event.getItem());
    }

    // =====================================================
    // Shift+click food for inspection GUI
    // =====================================================

    /**
     * Shift + right-click with food to open the inspection GUI.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodInspect(PlayerInteractEvent event) {
        if (!config.isGuiEnabled() || !config.isInspectOnShiftClick()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;

        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        if (hand.getType().isAir() || !hand.getType().isEdible()) return;

        // Don't interfere with block interactions (containers, crafting tables, etc.)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null) {
                if (block.getType().isInteractable()) return;

                // Don't interfere with active multiblock structures
                if (module.getMultiblockManager().isActiveMultiblock(block.getLocation())) {
                    return;
                }
            }
        }

        event.setCancelled(true);
        new FoodInspectionGui(module).open(event.getPlayer());
    }

    // =====================================================
    // Weight/Size stack enforcement
    // =====================================================

    /**
     * Enforce stack limits and container size restrictions on inventory clicks.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeightStackEnforce(InventoryClickEvent event) {
        if (!config.isWeightEnabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        WeightManager wm = module.getWeightManager();
        if (wm == null) return;

        if (config.isWeightContainerRestrictionsEnabled()
                && checkContainerRestriction(event, player, wm)) {
            event.setCancelled(true);
            return;
        }

        player.getServer().getScheduler().runTaskLater(
                MidgardCore.getInstance(),
                () -> wm.enforceInventory(player),
                1L
        );
    }

    private boolean checkContainerRestriction(InventoryClickEvent event, Player player, WeightManager wm) {
        // Normal click: cursor into container
        if (event.getClickedInventory() != null
                && event.getClickedInventory() != player.getInventory()) {
            if (isBlockedBySize(wm, player, event.getCursor(), event.getClickedInventory())) return true;
        }

        // Shift-click from player into container
        if (event.isShiftClick() && event.getClickedInventory() == player.getInventory()) {
            org.bukkit.inventory.Inventory topInv = event.getView().getTopInventory();
            if (isBlockedBySize(wm, player, event.getCurrentItem(), topInv)) return true;
        }

        // Number key swap into container
        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY
                && event.getClickedInventory() != null
                && event.getClickedInventory() != player.getInventory()) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (isBlockedBySize(wm, player, hotbarItem, event.getClickedInventory())) return true;
        }

        // Offhand swap into container
        if (event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND
                && event.getClickedInventory() != null
                && event.getClickedInventory() != player.getInventory()) {
            if (isBlockedBySize(wm, player, player.getInventory().getItemInOffHand(),
                    event.getClickedInventory())) return true;
        }

        return false;
    }

    private boolean isBlockedBySize(WeightManager wm, Player player,
                                     ItemStack item, org.bukkit.inventory.Inventory inv) {
        if (item == null || item.getType().isAir() || !wm.isStamped(item)) return false;
        if (!(inv.getHolder() instanceof org.bukkit.block.Container container)) return false;
        return wm.blockContainerInsert(player, item, container.getBlock().getType());
    }

    /**
     * Enforce container size restrictions on drag events.
     * Prevents bypassing size limits by dragging items into containers.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeightDragEnforce(InventoryDragEvent event) {
        if (!config.isWeightEnabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        WeightManager wm = module.getWeightManager();
        if (wm == null) return;

        if (config.isWeightContainerRestrictionsEnabled()) {
            org.bukkit.inventory.Inventory topInv = event.getView().getTopInventory();
            if (topInv.getHolder() instanceof org.bukkit.block.Container container) {
                int topSize = topInv.getSize();
                boolean dragsIntoContainer = false;
                for (int rawSlot : event.getRawSlots()) {
                    if (rawSlot < topSize) {
                        dragsIntoContainer = true;
                        break;
                    }
                }
                if (dragsIntoContainer) {
                    ItemStack dragged = event.getOldCursor();
                    if (dragged != null && !dragged.getType().isAir() && wm.isStamped(dragged)) {
                        Material containerType = container.getBlock().getType();
                        if (wm.blockContainerInsert(player, dragged, containerType)) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }

        player.getServer().getScheduler().runTaskLater(
                MidgardCore.getInstance(),
                () -> wm.enforceInventory(player),
                1L
        );
    }

    /**
     * Enforce weight stack limits when picking up items.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWeightPickupEnforce(EntityPickupItemEvent event) {
        if (!config.isWeightEnabled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        WeightManager wm = module.getWeightManager();
        if (wm == null) return;

        player.getServer().getScheduler().runTaskLater(
                MidgardCore.getInstance(),
                () -> wm.enforceInventory(player),
                2L
        );
    }

    /**
     * Block hoppers/droppers from moving size-restricted items into containers.
     * Prevents bypassing weight/size restrictions via automation.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperMoveItem(InventoryMoveItemEvent event) {
        if (!config.isWeightEnabled()) return;
        if (!config.isWeightContainerRestrictionsEnabled()) return;

        WeightManager wm = module.getWeightManager();
        if (wm == null) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir() || !wm.isStamped(item)) return;

        org.bukkit.inventory.Inventory destination = event.getDestination();
        if (!(destination.getHolder() instanceof org.bukkit.block.Container container)) return;

        if (!wm.canFitInContainer(item, container.getBlock().getType())) {
            event.setCancelled(true);
        }
    }

    private static Material getContainerReturn(Material food) {
        return switch (food) {
            case MUSHROOM_STEW, RABBIT_STEW, BEETROOT_SOUP, SUSPICIOUS_STEW -> Material.BOWL;
            case HONEY_BOTTLE, POTION -> Material.GLASS_BOTTLE;
            case MILK_BUCKET -> Material.BUCKET;
            default -> null;
        };
    }
}

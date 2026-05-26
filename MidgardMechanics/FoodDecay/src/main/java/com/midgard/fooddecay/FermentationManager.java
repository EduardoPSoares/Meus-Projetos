package com.midgard.fooddecay;

import com.midgard.core.MidgardCore;
import com.midgard.core.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TFC-style fermentation system.
 * Players right-click a barrel block with a liquid container to start fermentation.
 * After time passes the liquid transforms into an alcoholic/fermented beverage.
 * Drinks apply potion effects when consumed.
 */
public class FermentationManager {

    private static final String DRINK_LORE_MARKER = "\u00A78\u00A7k\u00A7r";
    private static final int BARREL_LIQUID_CAPACITY = 1000;

    private final FoodDecayConfig config;
    private final LiquidManager liquidManager;
    private final NamespacedKey drinkIdKey;
    private final NamespacedKey fermentBarrelItemKey;

    // In-memory barrel tracking (replaces TileState PDC)
    private final Set<String> fermentBarrelLocations = ConcurrentHashMap.newKeySet();
    private final Map<String, BarrelLiquid> barrelLiquids = new ConcurrentHashMap<>();
    private final Map<Location, FermentEntry> activeBarrels = new ConcurrentHashMap<>();
    private final File dataFile;
    private int taskId = -1;
    private int saveTaskId = -1;

    /** Liquid stored inside a fermentation barrel. */
    private record BarrelLiquid(String type, int amount) {}

    /** Temporary conversion used when a player pours a vanilla filled container into the barrel. */
    private record VanillaContainerConversion(ItemStack container) {}

    public FermentationManager(FoodDecayConfig config, LiquidManager liquidManager) {
        this.config = config;
        this.liquidManager = liquidManager;
        MidgardCore core = MidgardCore.getInstance();
        this.drinkIdKey = new NamespacedKey(core, "drink_id");
        this.fermentBarrelItemKey = new NamespacedKey(core, "ferment_barrel_item");
        this.dataFile = new File(FoodDecayPlugin.getInstance().getDataFolder(), "ferment-barrels.yml");
        loadData();
    }

    // =====================================================
    // Fermentation Barrel Tracking (In-Memory + File)
    // =====================================================

    private static String locKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static String locKey(Block block) {
        return locKey(block.getLocation());
    }

    /**
     * Checks if a barrel block is a dedicated fermentation barrel.
     */
    public boolean isFermentBarrel(Block block) {
        if (block == null || block.getType() != Material.BARREL) return false;
        return fermentBarrelLocations.contains(locKey(block));
    }

    /**
     * Marks a barrel block as a dedicated fermentation barrel.
     */
    private void markFermentBarrel(Block block) {
        if (block == null || block.getType() != Material.BARREL) return;
        String key = locKey(block);
        fermentBarrelLocations.add(key);
        markDirty();
    }

    /**
     * Removes the fermentation barrel mark, releasing it back to normal use.
     */
    private void unmarkFermentBarrel(Block block) {
        if (block == null) return;
        String key = locKey(block);
        fermentBarrelLocations.remove(key);
        barrelLiquids.remove(key);
        markDirty();
    }

    /**
     * Returns the active fermentation entry for a location, or null if none.
     */
    public FermentEntry getActiveFermentation(Location loc) {
        return activeBarrels.get(loc);
    }

    // =====================================================
    // Fermentation Barrel Item
    // =====================================================

    /**
     * Creates a special fermentation barrel item.
     * When placed, the block becomes a dedicated fermentation barrel.
     */
    public ItemStack createFermentBarrelItem() {
        ItemStack item = new com.midgard.core.item.ItemBuilder(Material.BARREL)
                .name(MessageUtils.sc(config.msg("ferment-barrel-item-name")))
                .lore(MessageUtils.sc(config.msg("ferment-barrel-item-lore-1")),
                        MessageUtils.sc(config.msg("ferment-barrel-item-lore-2")),
                        "",
                        MessageUtils.sc(config.msg("ferment-barrel-item-lore-3")))
                .glow()
                .build();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(fermentBarrelItemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Checks if an ItemStack is a fermentation barrel item.
     */
    public boolean isFermentBarrelItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(fermentBarrelItemKey, PersistentDataType.BYTE);
    }

    /**
     * Called when a player places a block.
     * If the item placed is a fermentation barrel item, mark the block.
     * Returns true if it was a fermentation barrel placement.
     */
    public boolean onBarrelPlace(Block block, ItemStack itemInHand) {
        if (block == null || block.getType() != Material.BARREL) return false;
        markFermentBarrel(block);
        return true;
    }

    // =====================================================
    // Barrel Liquid Storage (In-Memory)
    // =====================================================

    /**
     * Gets the liquid type stored in a fermentation barrel, or null if empty.
     */
    public String getBarrelLiquidType(Block block) {
        BarrelLiquid bl = barrelLiquids.get(locKey(block));
        return bl != null ? bl.type() : null;
    }

    /**
     * Gets the liquid amount (mB) stored in a fermentation barrel.
     */
    public int getBarrelLiquidAmount(Block block) {
        BarrelLiquid bl = barrelLiquids.get(locKey(block));
        return bl != null ? bl.amount() : 0;
    }

    /**
     * Sets the liquid stored in a fermentation barrel.
     */
    private void setBarrelLiquid(Block block, String liquidType, int amount) {
        String key = locKey(block);
        if (amount <= 0) {
            barrelLiquids.remove(key);
        } else {
            barrelLiquids.put(key, new BarrelLiquid(liquidType.toUpperCase(), amount));
        }
        markDirty();
    }

    /**
     * Pours liquid from a container into the barrel.
     * Returns the amount poured, or 0 if incompatible/full.
     */
    public int pourLiquidIntoBarrel(Block block, ItemStack container) {
        if (!liquidManager.isContainer(container)) return 0;
        if (liquidManager.isEmpty(container)) return 0;

        String containerLiquid = liquidManager.getLiquidType(container);
        if (containerLiquid == null || containerLiquid.isEmpty()) return 0;

        String barrelLiquid = getBarrelLiquidType(block);
        int barrelAmount = getBarrelLiquidAmount(block);

        // Can't mix different liquids
        if (barrelLiquid != null && !barrelLiquid.isEmpty() && barrelAmount > 0
                && !barrelLiquid.equalsIgnoreCase(containerLiquid)) {
            return -1; // signal: incompatible liquid
        }

        int space = BARREL_LIQUID_CAPACITY - barrelAmount;
        if (space <= 0) return 0; // barrel full

        int available = liquidManager.getLiquidAmount(container);
        int toTransfer = Math.min(available, space);

        liquidManager.drain(container, toTransfer);
        setBarrelLiquid(block, containerLiquid, barrelAmount + toTransfer);
        return toTransfer;
    }

    /**
     * Drains liquid from the barrel into a container.
     * Returns the amount transferred, or 0 if nothing to drain / container full.
     */
    public int drainLiquidFromBarrel(Block block, ItemStack container) {
        if (!liquidManager.isContainer(container)) return 0;

        String barrelLiquid = getBarrelLiquidType(block);
        int barrelAmount = getBarrelLiquidAmount(block);
        if (barrelLiquid == null || barrelAmount <= 0) return 0;

        int filled = liquidManager.fill(container, barrelLiquid, barrelAmount);
        if (filled > 0) {
            int remaining = barrelAmount - filled;
            setBarrelLiquid(block, barrelLiquid, remaining);
        }
        return filled;
    }

    public int getBarrelLiquidCapacity() {
        return BARREL_LIQUID_CAPACITY;
    }

    public String getLiquidDisplayName(String liquidType) {
        return liquidManager.getLiquidDisplayName(liquidType);
    }

    // =====================================================
    // Data
    // =====================================================

    public record FermentEntry(
            String liquidType,
            int liquidAmount,
            Material containerMaterial,
            String recipeId,
            long startTime,
            long durationMs,
            UUID playerUuid,
            ItemDisplay display
    ) {
        public long elapsed() { return System.currentTimeMillis() - startTime; }
        public boolean isReady() { return elapsed() >= durationMs; }
        public float progress() {
            return Math.min(1f, (float) elapsed() / Math.max(1, durationMs));
        }
    }

    // =====================================================
    // Interaction
    // =====================================================

    /**
     * Handles a player interacting with a fermentation barrel.
     * - With liquid container: pours liquid into barrel, auto-starts fermentation if enough.
     * - With empty container (sneaking): drains liquid from barrel back into container.
     * - Active fermentation: checks progress or collects.
     * Returns true if the interaction was consumed.
     */
    public boolean onBarrelInteract(Player player, Block block, ItemStack handItem, boolean isSneaking) {
        if (!config.isFermentationEnabled()) return false;
        if (block == null || block.getType() != Material.BARREL) return false;
        if (!isFermentBarrel(block)) return false;

        Location loc = block.getLocation();

        // If fermentation is active — show progress or collect
        if (activeBarrels.containsKey(loc)) {
            FermentEntry entry = activeBarrels.get(loc);
            if (entry.isReady()) {
                collectDrink(player, loc);
                return true;
            }
            String msg = config.msg("ferment-progress");
            if (msg != null && !msg.isEmpty()) {
                float pct = entry.progress() * 100;
                player.sendActionBar(MessageUtils.toComponent(
                        msg.replace("{progress}", String.format("%.0f", pct))
                           .replace("{recipe}", entry.recipeId)
                ));
            }
            return true;
        }

        // No item in hand → open GUI (handled by listener, not here)
        if (handItem == null || handItem.getType().isAir()) return false;

        ItemStack interactionItem = handItem;
        VanillaContainerConversion vanillaConversion = null;

        // Handle vanilla filled containers (WATER_BUCKET, MILK_BUCKET, HONEY_BOTTLE)
        // Only convert if barrel has space and no active fermentation
        if (!isSneaking) {
            int barrelSpace = BARREL_LIQUID_CAPACITY - getBarrelLiquidAmount(block);
            if (barrelSpace > 0) {
                vanillaConversion = tryConvertVanillaContainer(handItem);
                if (vanillaConversion != null) {
                    interactionItem = vanillaConversion.container();
                }
            } else {
                // Barrel is full — check if it's a vanilla container and block it
                Material mat = handItem.getType();
                if (mat == Material.WATER_BUCKET || mat == Material.MILK_BUCKET || mat == Material.HONEY_BOTTLE) {
                    sendMsg(player, "ferment-barrel-full");
                    return true;
                }
            }
        }

        // Auto-stamp recognized container materials
        if (!liquidManager.isContainer(interactionItem)
                && config.getLiquidContainerCapacity(interactionItem.getType()) > 0) {
            liquidManager.stampContainer(interactionItem);
        }

        // Sneaking with container → drain liquid FROM barrel into container
        if (isSneaking && liquidManager.isContainer(interactionItem)) {
            // Capture liquid type before draining (drain may empty the barrel, losing the type)
            String drainLiquidType = getBarrelLiquidType(block);
            int drained = drainLiquidFromBarrel(block, interactionItem);
            if (drained > 0) {
                // Sync changes back to player's hand
                player.getInventory().setItemInMainHand(interactionItem);
                player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 0.7f, 1.1f);
                String msg = config.msg("ferment-barrel-drained");
                if (msg != null && !msg.isEmpty()) {
                    player.sendActionBar(MessageUtils.toComponent(
                            msg.replace("{amount}", String.valueOf(drained))
                               .replace("{liquid}", liquidManager.getLiquidDisplayName(drainLiquidType))
                    ));
                }
            } else {
                sendMsg(player, "ferment-barrel-empty");
            }
            return true;
        }

        // Not sneaking with container → pour liquid INTO barrel
        if (liquidManager.isContainer(interactionItem)) {
            if (liquidManager.isEmpty(interactionItem)) {
                sendMsg(player, "ferment-empty-container");
                return true;
            }

            int poured = pourLiquidIntoBarrel(block, interactionItem);
            if (poured == -1) {
                sendMsg(player, "ferment-barrel-liquid-mismatch");
                return true;
            }
            if (poured == 0) {
                sendMsg(player, "ferment-barrel-full");
                return true;
            }

            // Sync changes back to the player's inventory after draining the container.
            syncPlayerContainerAfterPour(player, handItem, interactionItem, vanillaConversion);

            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 0.7f, 1.1f);
            String barrelLiquid = getBarrelLiquidType(block);
            applyInvalidRecipePenaltyIfNeeded(player, block, barrelLiquid, poured);
            int barrelAmount = getBarrelLiquidAmount(block);
            String msg = config.msg("ferment-barrel-poured");
            if (msg != null && !msg.isEmpty()) {
                player.sendActionBar(MessageUtils.toComponent(
                        msg.replace("{amount}", String.valueOf(poured))
                           .replace("{liquid}", liquidManager.getLiquidDisplayName(barrelLiquid))
                           .replace("{total}", String.valueOf(barrelAmount))
                           .replace("{capacity}", String.valueOf(BARREL_LIQUID_CAPACITY))
                ));
            }

            return true;
        }

        return false;
    }

    /**
     * Converts vanilla filled containers (WATER_BUCKET, MILK_BUCKET, HONEY_BOTTLE)
     * into the liquid system format without mutating the player's inventory yet.
     * Returns a temporary converted container, or null if not a supported vanilla container.
     */
    private VanillaContainerConversion tryConvertVanillaContainer(ItemStack handItem) {
        if (handItem == null || handItem.getType().isAir()) return null;

        Material mat = handItem.getType();
        String liquidType;
        Material emptyMat;
        int fillAmount;

        switch (mat) {
            case WATER_BUCKET -> { liquidType = "WATER"; emptyMat = Material.BUCKET; fillAmount = 1000; }
            case MILK_BUCKET -> { liquidType = "MILK"; emptyMat = Material.BUCKET; fillAmount = 1000; }
            case HONEY_BOTTLE -> { liquidType = "HONEY"; emptyMat = Material.GLASS_BOTTLE; fillAmount = 250; }
            default -> { return null; }
        }

        int capacity = config.getLiquidContainerCapacity(emptyMat);
        if (capacity < fillAmount) return null;

        ItemStack convertedItem = new ItemStack(emptyMat);
        if (!liquidManager.stampContainer(convertedItem)) return null;

        int filled = liquidManager.fill(convertedItem, liquidType, fillAmount);
        if (filled != fillAmount) return null;

        return new VanillaContainerConversion(convertedItem);
    }

    private void syncPlayerContainerAfterPour(Player player, ItemStack originalHand, ItemStack resultContainer,
                                              VanillaContainerConversion vanillaConversion) {
        FermentationInventorySync.HandUpdate handUpdate = FermentationInventorySync.planAfterPour(
                originalHand,
                resultContainer,
                vanillaConversion != null
        );

        player.getInventory().setItemInMainHand(handUpdate.mainHand());
        if (handUpdate.extraItem() != null) {
            player.getInventory().addItem(handUpdate.extraItem()).values().forEach(overflow ->
                    player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        }
    }

    private void applyInvalidRecipePenaltyIfNeeded(Player player, Block block, String liquidType, int pouredAmount) {
        if (liquidType == null || liquidType.isEmpty() || pouredAmount <= 0) return;
        if (!config.findAllFermentRecipes(liquidType).isEmpty()) return;

        int barrelAmount = getBarrelLiquidAmount(block);
        FermentationInvalidRecipePenalty.Outcome outcome =
                FermentationInvalidRecipePenalty.apply(barrelAmount, pouredAmount);
        if (outcome.lostAmountMb() <= 0) return;

        setBarrelLiquid(block, liquidType, outcome.remainingAmountMb());
        playInvalidRecipeFeedback(player, block);

        String msg = config.msg("ferment-no-recipe-penalty");
        if (msg == null || msg.isEmpty()) return;

        player.sendMessage(MessageUtils.toComponent(
                msg.replace("{liquid}", liquidManager.getLiquidDisplayName(liquidType))
                   .replace("{lost}", String.valueOf(outcome.lostAmountMb()))
                   .replace("{remaining}", String.valueOf(outcome.remainingAmountMb()))
                   .replace("{penalty}", String.valueOf(getInvalidRecipePenaltyPercent()))
        ));
    }

    private void playInvalidRecipeFeedback(Player player, Block block) {
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 0.9f);

        World world = block.getWorld();
        Location effectLoc = block.getLocation().add(0.5, 1.0, 0.5);
        world.spawnParticle(Particle.SMOKE, effectLoc, 10, 0.2, 0.15, 0.2, 0.01);
        world.spawnParticle(Particle.ASH, effectLoc, 6, 0.18, 0.1, 0.18, 0.01);
    }

    /**
     * Starts fermentation with a specific recipe chosen by the player from the GUI.
     * Returns true if fermentation started successfully.
     */
    public boolean startFermentation(Player player, Location loc, String recipeId) {
        if (activeBarrels.containsKey(loc)) return false;

        Block block = loc.getBlock();
        if (block.getType() != Material.BARREL || !isFermentBarrel(block)) return false;

        FoodDecayConfig.FermentRecipe recipe = config.getFermentRecipe(recipeId);
        if (recipe == null) return false;

        String liquidType = getBarrelLiquidType(block);
        int liquidAmount = getBarrelLiquidAmount(block);
        if (liquidType == null || !liquidType.equalsIgnoreCase(recipe.inputLiquid())) return false;
        if (liquidAmount < recipe.requiredMb()) return false;

        // Consume liquid from barrel
        int remaining = liquidAmount - recipe.requiredMb();
        setBarrelLiquid(block, liquidType, remaining);

        long durationMs = recipe.timeMinutes() * 60_000L;
        ItemDisplay display = spawnBarrelDisplay(loc, recipe.resultMaterial());

        FermentEntry entry = new FermentEntry(
                liquidType, recipe.requiredMb(), Material.BARREL,
                recipe.id(), System.currentTimeMillis(), durationMs,
                player.getUniqueId(), display
        );
        activeBarrels.put(loc, entry);
        markDirty();

        block.getWorld().playSound(loc, Sound.BLOCK_BREWING_STAND_BREW, 1f, 0.8f);
        String msg = config.msg("ferment-started");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(MessageUtils.toComponent(
                    msg.replace("{drink}", recipe.displayName())
                       .replace("{time}", String.valueOf(recipe.timeMinutes()))
            ));
        }
        return true;
    }

    /**
     * Collects the fermented drink from a barrel.
     */
    private void collectDrink(Player player, Location loc) {
        FermentEntry entry = activeBarrels.remove(loc);
        if (entry == null) return;

        if (entry.display != null && entry.display.isValid()) {
            entry.display.remove();
        }
        markDirty();

        FoodDecayConfig.FermentRecipe recipe = config.getFermentRecipe(entry.recipeId);
        if (recipe == null) {
            // Recipe was removed from config — return liquid to barrel
            Block block = loc.getBlock();
            if (block.getType() == Material.BARREL && isFermentBarrel(block)) {
                setBarrelLiquid(block, entry.liquidType(), entry.liquidAmount());
            }
            sendMsg(player, "ferment-recipe-not-found");
            return;
        }

        // Build the drink item
        ItemStack drink = buildDrinkItem(recipe, entry.liquidAmount);

        player.getInventory().addItem(drink).values()
                .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.0f);
        String msg = config.msg("ferment-complete");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(MessageUtils.toComponent(
                    msg.replace("{drink}", recipe.displayName())
            ));
        }
    }

    /**
     * Builds a drink item from a fermentation recipe.
     * The drink is a POTION-like item with custom name, lore, and PDC drink ID.
     */
    private ItemStack buildDrinkItem(FoodDecayConfig.FermentRecipe recipe, int liquidAmount) {
        ItemStack item = new ItemStack(recipe.resultMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Name
        meta.displayName(MessageUtils.toComponent(recipe.displayName()));

        // Lore
        List<Component> lore = new ArrayList<>();
        lore.add(MessageUtils.toComponent(DRINK_LORE_MARKER + "&8&m              "));
        lore.add(MessageUtils.toComponent(DRINK_LORE_MARKER + " &6Bebida: &f" + recipe.displayName()));
        lore.add(MessageUtils.toComponent(DRINK_LORE_MARKER + " &7Fermentado de &f"
                + liquidManager.getLiquidDisplayName(recipe.inputLiquid())));
        if (!recipe.effects().isEmpty()) {
            lore.add(MessageUtils.toComponent(DRINK_LORE_MARKER + " &7Efeitos:"));
            for (FoodDecayConfig.DrinkEffect effect : recipe.effects()) {
                String effectName = formatEffectName(effect.type());
                int secs = effect.durationTicks() / 20;
                lore.add(MessageUtils.toComponent(DRINK_LORE_MARKER + "  &7- " + effectName
                        + " &8(" + secs + "s, nivel " + (effect.amplifier() + 1) + ")"));
            }
        }
        lore.add(MessageUtils.toComponent(DRINK_LORE_MARKER + "&8&m              "));
        meta.lore(lore);

        // PDC: mark as drink
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(drinkIdKey, PersistentDataType.STRING, recipe.id());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Called when a player consumes a drink item (POTION / HONEY_BOTTLE).
     * Applies fermentation effects. Returns true if it was a fermented drink.
     */
    public boolean onDrinkConsume(Player player, ItemStack item) {
        if (!config.isFermentationEnabled()) return false;
        if (item == null || !item.hasItemMeta()) return false;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String drinkId = pdc.get(drinkIdKey, PersistentDataType.STRING);
        if (drinkId == null) return false;

        FoodDecayConfig.FermentRecipe recipe = config.getFermentRecipe(drinkId);
        if (recipe == null) return false;

        // Apply effects
        for (FoodDecayConfig.DrinkEffect effect : recipe.effects()) {
            PotionEffectType effectType = effect.type();
            if (effectType != null) {
                player.addPotionEffect(new PotionEffect(
                        effectType, effect.durationTicks(), effect.amplifier(), true, true, true
                ));
            }
        }

        String msg = config.msg("ferment-consumed");
        if (msg != null && !msg.isEmpty()) {
            player.sendActionBar(MessageUtils.toComponent(
                    msg.replace("{drink}", recipe.displayName())
            ));
        }

        return true;
    }

    // =====================================================
    // Display Entity
    // =====================================================

    private ItemDisplay spawnBarrelDisplay(Location loc, Material material) {
        World world = loc.getWorld();
        if (world == null) return null;

        Location spawnLoc = loc.clone().add(0.5, 1.1, 0.5);
        return world.spawn(spawnLoc, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setBillboard(Display.Billboard.FIXED);
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(0.3f, 0.3f, 0.3f),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
            display.setPersistent(false);
        });
    }

    // =====================================================
    // Task
    // =====================================================

    public void startTask() {
        if (!config.isFermentationEnabled()) return;
        taskId = Bukkit.getScheduler().runTaskTimer(
                MidgardCore.getInstance(), this::tick, 20L, 40L
        ).getTaskId();
    }

    public void stopTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        // Remove display entities only — do NOT clear activeBarrels here.
        // Data must be saved by shutdown() before being discarded.
        for (FermentEntry entry : activeBarrels.values()) {
            if (entry.display != null && entry.display.isValid()) {
                entry.display.remove();
            }
        }
    }

    private void tick() {
        Iterator<Map.Entry<Location, FermentEntry>> it = activeBarrels.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, FermentEntry> mapEntry = it.next();
            Location loc = mapEntry.getKey();
            FermentEntry entry = mapEntry.getValue();

            // Verify barrel still exists
            if (!loc.isWorldLoaded()) {
                removeEntry(it, entry);
                continue;
            }
            Block block = loc.getBlock();
            if (block.getType() != Material.BARREL) {
                removeEntry(it, entry);
                continue;
            }

            // Spawn fermentation particles
            World world = loc.getWorld();
            double x = loc.getBlockX() + 0.5;
            double y = loc.getBlockY() + 1.0;
            double z = loc.getBlockZ() + 0.5;

            if (entry.isReady()) {
                // Ready — sparkle
                world.spawnParticle(Particle.HAPPY_VILLAGER, x, y + 0.3, z, 1, 0.2, 0.1, 0.2, 0);
            } else {
                // Fermenting — bubbles
                world.spawnParticle(Particle.BUBBLE_POP, x, y + 0.1, z, 1, 0.15, 0.05, 0.15, 0.01);
                if (entry.progress() > 0.5f) {
                    world.spawnParticle(Particle.EFFECT, x, y + 0.2, z, 1, 0.1, 0.02, 0.1, 0);
                }
            }
        }
    }

    private void removeEntry(Iterator<Map.Entry<Location, FermentEntry>> it, FermentEntry entry) {
        if (entry.display != null && entry.display.isValid()) {
            entry.display.remove();
        }
        it.remove();
    }

    // =====================================================
    // Utility
    // =====================================================

    /**
     * Called when a fermentation barrel is broken.
     * Removes the active fermentation entry and cleans up display entity.
     */
    public void onBarrelBreak(Block block) {
        if (block == null) return;
        Location loc = block.getLocation();
        World world = loc.getWorld();

        // Drop ready drink if fermentation is complete, or return liquid if in progress
        FermentEntry entry = activeBarrels.remove(loc);
        if (entry != null) {
            if (entry.display != null && entry.display.isValid()) {
                entry.display.remove();
            }
            if (entry.isReady()) {
                FoodDecayConfig.FermentRecipe recipe = config.getFermentRecipe(entry.recipeId);
                if (recipe != null && world != null) {
                    ItemStack drink = buildDrinkItem(recipe, entry.liquidAmount());
                    world.dropItemNaturally(loc, drink);
                }
            } else if (world != null) {
                // Fermentation in progress — return invested liquid as filled containers
                int remaining = entry.liquidAmount();
                while (remaining > 0) {
                    Material containerMat = remaining >= 1000 ? Material.BUCKET : Material.BOWL;
                    int capacity = config.getLiquidContainerCapacity(containerMat);
                    if (capacity <= 0) { containerMat = Material.BUCKET; capacity = 1000; }
                    int toFill = Math.min(remaining, capacity);
                    ItemStack container = new ItemStack(containerMat);
                    liquidManager.stampContainer(container);
                    liquidManager.fill(container, entry.liquidType(), toFill);
                    world.dropItemNaturally(loc, container);
                    remaining -= toFill;
                }
            }
        }

        // Drop stored liquid as a filled container
        String key = locKey(block);
        BarrelLiquid bl = barrelLiquids.get(key);
        if (bl != null && bl.amount() > 0 && world != null) {
            int remaining = bl.amount();
            while (remaining > 0) {
                Material containerMat = remaining >= 1000 ? Material.BUCKET : Material.BOWL;
                int capacity = config.getLiquidContainerCapacity(containerMat);
                if (capacity <= 0) { containerMat = Material.BUCKET; capacity = 1000; }
                int toFill = Math.min(remaining, capacity);
                ItemStack container = new ItemStack(containerMat);
                liquidManager.stampContainer(container);
                liquidManager.fill(container, bl.type(), toFill);
                world.dropItemNaturally(loc, container);
                remaining -= toFill;
            }
        }

        // Clean in-memory tracking
        fermentBarrelLocations.remove(key);
        barrelLiquids.remove(key);
        markDirty();
    }

    /**
     * Releases a barrel from fermentation dedication (called from GUI).
     * If there is an active fermentation, it is removed and the invested liquid is returned.
     */
    public void releaseBarrel(Location loc) {
        FermentEntry entry = activeBarrels.remove(loc);
        if (entry != null) {
            if (entry.display != null && entry.display.isValid()) {
                entry.display.remove();
            }
            // Return invested fermentation liquid as filled containers
            if (loc.getWorld() != null && entry.liquidAmount() > 0) {
                int remaining = entry.liquidAmount();
                while (remaining > 0) {
                    Material containerMat = remaining >= 1000 ? Material.BUCKET : Material.BOWL;
                    int capacity = config.getLiquidContainerCapacity(containerMat);
                    if (capacity <= 0) { containerMat = Material.BUCKET; capacity = 1000; }
                    int toFill = Math.min(remaining, capacity);
                    ItemStack container = new ItemStack(containerMat);
                    liquidManager.stampContainer(container);
                    liquidManager.fill(container, entry.liquidType(), toFill);
                    loc.getWorld().dropItemNaturally(loc, container);
                    remaining -= toFill;
                }
            }
        }
        Block block = loc.getBlock();
        // Drop stored liquid before unmarking
        String key = block.getType() == Material.BARREL ? locKey(block) : locKey(loc);
        BarrelLiquid bl = barrelLiquids.get(key);
        if (bl != null && bl.amount() > 0 && loc.getWorld() != null) {
            int remaining = bl.amount();
            while (remaining > 0) {
                Material containerMat = remaining >= 1000 ? Material.BUCKET : Material.BOWL;
                int capacity = config.getLiquidContainerCapacity(containerMat);
                if (capacity <= 0) { containerMat = Material.BUCKET; capacity = 1000; }
                int toFill = Math.min(remaining, capacity);
                ItemStack container = new ItemStack(containerMat);
                liquidManager.stampContainer(container);
                liquidManager.fill(container, bl.type(), toFill);
                loc.getWorld().dropItemNaturally(loc, container);
                remaining -= toFill;
            }
        }
        if (block.getType() == Material.BARREL) {
            unmarkFermentBarrel(block);
        } else {
            fermentBarrelLocations.remove(locKey(loc));
            barrelLiquids.remove(locKey(loc));
            markDirty();
        }
    }

    /**
     * Collects the completed drink from a barrel (called from GUI).
     */
    public void collectFromGui(Player player, Location loc) {
        collectDrink(player, loc);
    }

    private String formatEffectName(PotionEffectType type) {
        if (type == null) return "?";
        String name = type.getKey().getKey().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private void sendMsg(Player player, String key) {
        String msg = config.msg(key);
        if (msg != null && !msg.isEmpty()) {
            player.sendActionBar(MessageUtils.toComponent(msg));
        }
    }

    public int getInvalidRecipePenaltyPercent() {
        return FermentationInvalidRecipePenalty.getLossPercent();
    }

    public int getInvalidRecipeMinimumLossMb() {
        return FermentationInvalidRecipePenalty.getMinimumLossMb();
    }

    public NamespacedKey getDrinkIdKey() { return drinkIdKey; }
    public Map<Location, FermentEntry> getActiveBarrels() {
        return Collections.unmodifiableMap(activeBarrels);
    }

    // =====================================================
    // Lazy Save
    // =====================================================

    /**
     * Schedules a save for 1 second later, coalescing multiple rapid changes.
     * Prevents I/O stutter from saving on every single pour/drain operation.
     */
    private void markDirty() {
        if (saveTaskId != -1) return;
        saveTaskId = Bukkit.getScheduler().runTaskLater(
                MidgardCore.getInstance(),
                () -> {
                    saveTaskId = -1;
                    saveData();
                },
                20L
        ).getTaskId();
    }

    // =====================================================
    // Persistence (ferment-barrels.yml)
    // =====================================================

    private void saveData() {
        YamlConfiguration yaml = new YamlConfiguration();

        // Save barrel locations
        yaml.set("barrels", new ArrayList<>(fermentBarrelLocations));

        // Save liquid data
        ConfigurationSection liquidSection = yaml.createSection("liquids");
        for (Map.Entry<String, BarrelLiquid> e : barrelLiquids.entrySet()) {
            String safeKey = e.getKey().replace(",", "_");
            liquidSection.set(safeKey + ".key", e.getKey());
            liquidSection.set(safeKey + ".type", e.getValue().type());
            liquidSection.set(safeKey + ".amount", e.getValue().amount());
        }

        // Save active fermentations
        ConfigurationSection fermentSection = yaml.createSection("fermentations");
        int idx = 0;
        for (Map.Entry<Location, FermentEntry> e : activeBarrels.entrySet()) {
            Location loc = e.getKey();
            FermentEntry entry = e.getValue();
            if (loc.getWorld() == null) continue;
            String path = "f" + idx;
            fermentSection.set(path + ".world", loc.getWorld().getName());
            fermentSection.set(path + ".x", loc.getBlockX());
            fermentSection.set(path + ".y", loc.getBlockY());
            fermentSection.set(path + ".z", loc.getBlockZ());
            fermentSection.set(path + ".liquidType", entry.liquidType());
            fermentSection.set(path + ".liquidAmount", entry.liquidAmount());
            fermentSection.set(path + ".recipeId", entry.recipeId());
            fermentSection.set(path + ".startTime", entry.startTime());
            fermentSection.set(path + ".durationMs", entry.durationMs());
            fermentSection.set(path + ".playerUuid", entry.playerUuid().toString());
            idx++;
        }

        try {
            yaml.save(dataFile);
        } catch (IOException ex) {
            Bukkit.getLogger().warning("[FoodDecay] Failed to save ferment-barrels.yml: " + ex.getMessage());
        }
    }

    private void loadData() {
        if (!dataFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);

        // Load barrel locations
        List<String> barrels = yaml.getStringList("barrels");
        fermentBarrelLocations.addAll(barrels);

        // Load liquid data
        ConfigurationSection liquidSection = yaml.getConfigurationSection("liquids");
        if (liquidSection != null) {
            for (String safeKey : liquidSection.getKeys(false)) {
                String realKey = liquidSection.getString(safeKey + ".key", safeKey);
                String type = liquidSection.getString(safeKey + ".type");
                int amount = liquidSection.getInt(safeKey + ".amount", 0);
                if (type != null && amount > 0) {
                    barrelLiquids.put(realKey, new BarrelLiquid(type, amount));
                }
            }
        }

        // Load active fermentations
        ConfigurationSection fermentSection = yaml.getConfigurationSection("fermentations");
        if (fermentSection != null) {
            for (String key : fermentSection.getKeys(false)) {
                String worldName = fermentSection.getString(key + ".world");
                World world = worldName != null ? Bukkit.getWorld(worldName) : null;
                if (world == null) continue;
                int x = fermentSection.getInt(key + ".x");
                int y = fermentSection.getInt(key + ".y");
                int z = fermentSection.getInt(key + ".z");
                Location loc = new Location(world, x, y, z);

                String liquidType = fermentSection.getString(key + ".liquidType");
                int liquidAmount = fermentSection.getInt(key + ".liquidAmount");
                String recipeId = fermentSection.getString(key + ".recipeId");
                long startTime = fermentSection.getLong(key + ".startTime");
                long durationMs = fermentSection.getLong(key + ".durationMs");
                String uuidStr = fermentSection.getString(key + ".playerUuid");
                if (recipeId == null || uuidStr == null) continue;

                UUID playerUuid;
                try { playerUuid = UUID.fromString(uuidStr); }
                catch (IllegalArgumentException ignored) { continue; }

                FoodDecayConfig.FermentRecipe recipe = config.getFermentRecipe(recipeId);
                Material resultMat = recipe != null ? recipe.resultMaterial() : Material.POTION;
                ItemDisplay display = spawnBarrelDisplay(loc, resultMat);

                FermentEntry entry = new FermentEntry(
                        liquidType, liquidAmount, Material.BARREL,
                        recipeId, startTime, durationMs, playerUuid, display
                );
                activeBarrels.put(loc, entry);
            }
            if (!activeBarrels.isEmpty()) {
                Bukkit.getLogger().info("[FoodDecay] Restored " + activeBarrels.size() + " active fermentation(s).");
            }
        }

        Bukkit.getLogger().info("[FoodDecay] Loaded " + fermentBarrelLocations.size()
                + " fermentation barrels, " + barrelLiquids.size() + " with liquid.");
    }

    /** Saves data and cleans up (called on disable). */
    public void shutdown() {
        if (saveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(saveTaskId);
            saveTaskId = -1;
        }
        saveData();
        activeBarrels.clear();
    }
}

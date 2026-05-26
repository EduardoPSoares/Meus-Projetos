package com.midgard.fooddecay;

import com.midgard.fooddecay.multiblock.MMOCoreHook;
import com.midgard.fooddecay.multiblock.ItemsAdderHook;
import com.midgard.fooddecay.multiblock.MMOItemsHook;
import com.midgard.fooddecay.multiblock.MultiblockRecipe;
import com.midgard.fooddecay.multiblock.RecipeIngredient;
import com.midgard.fooddecay.multiblock.MultiblockType;
import static com.midgard.core.utils.MessageUtils.sc;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

/**
 * Fully configurable configuration for the TFC-style FoodDecay system.
 * Includes temperature zones, seasonal modifiers, preservation traits,
 * container modifiers, and all messages/formats.
 */
public class FoodDecayConfig {

    private final FoodDecayPlugin plugin;
    private final Map<Material, Long> expirationTimes = new HashMap<>();

    // --- General settings ---
    private long defaultExpiration;
    private long decayCheckInterval;
    private boolean showLoreTimer;
    private double warningThreshold;
    private boolean trackAllFood;

    // --- Colors ---
    private String freshColor;
    private String warningColor;
    private String expiredColor;

    // --- Lore ---
    private String loreFormat;
    private String loreExpiredStatus;

    // --- Time format ---
    private String timeFormatDays;
    private String timeFormatHours;
    private String timeFormatMinutes;
    private String timeFormatSeconds;
    private String timeFormatExpired;

    // --- Behavior toggles ---
    private boolean blockExpiredConsume;
    private boolean stampOnPickup;
    private boolean stampOnCraft;
    private boolean stampOnFurnace;
    private boolean stampOnInventoryClick;
    private boolean stampOnInventoryOpen;
    private boolean stampOnJoin;
    private boolean stampOnItemSpawn;

    // --- Temperature system ---
    private boolean temperatureEnabled;
    private final List<TemperatureZone> temperatureZones = new ArrayList<>();

    // --- Depth-temperature system ---
    private boolean depthTemperatureEnabled;
    private final List<DepthZone> depthZones = new ArrayList<>();

    // --- Season system ---
    private boolean seasonEnabled;
    private final Map<String, Double> seasonMultipliers = new HashMap<>();

    // --- Trait system ---
    private boolean traitsEnabled;
    private boolean autoSmokeFromSmoker;
    private final Map<FoodTrait, Double> traitMultipliers = new EnumMap<>(FoodTrait.class);
    private final Map<FoodTrait, Material> traitIngredients = new EnumMap<>(FoodTrait.class);
    private final Map<FoodTrait, String> traitDisplayNames = new EnumMap<>(FoodTrait.class);
    private final Map<FoodTrait, String> traitLoreLines = new EnumMap<>(FoodTrait.class);
    private int maxTraitsPerItem;

    // --- Container modifiers ---
    private boolean containerModifiersEnabled;
    private final Map<Material, Double> containerMultipliers = new HashMap<>();

    // --- Ice conservation ---
    private boolean iceConservationEnabled;
    private double iceMultiplier;
    private long iceWarmingDuration;
    private long iceFreezingDuration;
    private final Set<Material> iceBlocks = new HashSet<>();

    // --- Model stages ---
    private final Map<Material, int[]> decayModelStages = new HashMap<>();
    private final Map<Material, int[]> freezeModelStages = new HashMap<>();

    // --- Ambient smoke ---
    private boolean ambientSmokeEnabled;
    private int ambientSmokeInterval;
    private int ambientSmokeCount;
    private double ambientSmokeHeight;

    // --- Composting system ---
    private boolean compostingEnabled;
    private boolean compostPartialDecay;
    private double compostMinDecay;
    private String compostResultMaterial;
    private int compostResultAmount;

    // --- Cooking system ---
    private boolean cookingEnabled;
    private boolean cookingBurnEnabled;
    private long cookingBurnMinutes;
    private final Map<String, Double> cookingHeatMultipliers = new HashMap<>();
    private final Map<org.bukkit.Material, org.bukkit.Material> cookingRecipes = new EnumMap<>(org.bukkit.Material.class);
    private final Map<org.bukkit.Material, Long> cookingTimes = new EnumMap<>(org.bukkit.Material.class);
    private long cookingDefaultTime;

    // --- Portions system ---
    private boolean portionsEnabled;
    private int portionsDefault;
    private final Map<org.bukkit.Material, Integer> portionsPerFood = new EnumMap<>(org.bukkit.Material.class);

    // --- Liquid containers ---
    private boolean liquidContainersEnabled;
    private int liquidPourAmount;
    private int liquidMbPerCauldronLevel;
    private final Map<org.bukkit.Material, Integer> liquidContainerCapacities = new EnumMap<>(org.bukkit.Material.class);
    private final Map<String, String> liquidDisplayNames = new HashMap<>();
    private final Map<String, String> liquidColors = new HashMap<>();

    // --- Fermentation ---
    private boolean fermentationEnabled;
    private final Map<String, FermentRecipe> fermentRecipes = new LinkedHashMap<>();

    public record DrinkEffect(org.bukkit.potion.PotionEffectType type, int durationTicks, int amplifier) {}
    public record FermentRecipe(
            String id, String inputLiquid, int requiredMb, int timeMinutes,
            org.bukkit.Material resultMaterial, String displayName, List<DrinkEffect> effects
    ) {}
    public record GroupBonus(
            double healthBonus,
            Double activationThreshold,
            List<String> effects,
            Map<Attribute, Double> attributes,
            Map<String, Double> mmocoreStats
    ) {}
    public record MultiSlotTier(int level, int slots) {}
    public record TemperatureZone(String name, int maxTemp, double multiplier) {}
    public record DepthZone(String name, int maxY, int temperatureOffset) {}

    private static final double KG_PER_OUNCE = 0.028349523125D;

    // --- Weight/Size system ---
    private boolean weightEnabled;
    private boolean weightContainerRestrictionsEnabled;
    private double weightMaxKgPerStack;
    private String weightDefaultSize;
    private double weightDefaultKg;
    private final Map<Material, Double> weightPerFood = new EnumMap<>(Material.class);
    private final Map<Material, String> sizePerFood = new EnumMap<>(Material.class);
    private final Map<String, String> weightSizeDisplayNames = new HashMap<>();
    private final Map<String, String> weightSizeColors = new HashMap<>();
    private final Map<Material, Set<String>> containerSizeRestrictions = new EnumMap<>(Material.class);

    // --- Decay by weight ---
    private boolean decayByWeightEnabled;
    private double decayByWeightLossInterval;

    // --- Nutrition system ---
    private boolean nutritionEnabled;
    private double nutritionGainPerFood;
    private double nutritionDecayPerMinute;
    private double nutritionHealthBonusPerGroup;
    private boolean nutritionResetOnDeath;
    private double nutritionActivationThreshold;
    private final Map<String, GroupBonus> nutritionGroupBonuses = new HashMap<>();
    private final Set<String> nutritionConfiguredMmocoreStats = new LinkedHashSet<>();

    // --- Vinegar recipe ---
    private boolean vinegarRecipeEnabled;

    // --- Cauldron recipes ---
    private boolean cauldronRecipesEnabled;
    private int cauldronMaxIngredients;

    // --- Multiblock system ---
    private boolean multiblockEnabled;
    private int notificationRadius;
    private int abandonmentMinutes;
    private int proximityRadius;
    private double proximityBonusPerTick;
    private int qteIntervalSeconds;
    private int qteDurationSeconds;
    private double qteChance;
    private int qteMaxPerCycle;
    private float qteBonusPerEvent;
    private final Map<MultiblockType, Boolean> multiblockTypeEnabled = new EnumMap<>(MultiblockType.class);
    private final Map<MultiblockType, Integer> multiblockProcessingMinutes = new EnumMap<>(MultiblockType.class);
    private final Map<MultiblockType, List<MultiblockRecipe>> multiblockRecipes = new EnumMap<>(MultiblockType.class);
    private final Map<MultiblockType, String> multiblockDisplayNames = new EnumMap<>(MultiblockType.class);
    private final Map<MultiblockType, List<String>> multiblockDescriptions = new EnumMap<>(MultiblockType.class);

    // --- Multi-slot (level-based) ---
    private int multiSlotBaseSlots;
    private final List<MultiSlotTier> multiSlotTiers = new ArrayList<>();
    private String multiSlotProfession;

    // --- Machine tier (level-based structure complexity) ---
    private int machineTierT2Level;
    private int machineTierT3Level;

    // --- GUI ---
    private boolean guiEnabled;
    private boolean inspectOnShiftClick;
    private String guiTitleInspection;
    private String guiTitleNutrition;
    private String guiTitleCookbook;
    private String guiTitleMultiblockInspection;

    // --- Session & QTE ---
    private int sessionTimeoutMinutes;
    private float qteMissPenalty;

    // --- Quality tiers ---
    private float qualityTier1Threshold;
    private float qualityTier2Threshold;
    private String qualityTier0Prefix;
    private String qualityTier1Prefix;
    private String qualityTier2Prefix;
    private String qualityTier0Lore;
    private String qualityTier1Lore;
    private String qualityTier2Lore;

    // --- Cauldron result ---
    private Material cauldronResultMaterial;
    private String cauldronResultName;
    private List<String> cauldronResultLore;

    // --- Vinegar recipe config ---
    private Material vinegarResultMaterial;
    private String vinegarResultName;
    private List<String> vinegarResultLore;
    private List<Material> vinegarIngredients;

    // --- Machine-specific resources ---
    private List<Material> smokehouseFuelMaterials;
    private Material saltMaterial;
    private int saltRequired;
    private Material picklingWaterMaterial;
    private Material picklingWaterReturn;
    private Material picklingVinegarMaterial;
    private List<Material> picklingFuelMaterials;
    private Material waxMaterial;

    // --- Nutrition food group config ---
    private final Map<String, String> nutritionGroupDisplayNames = new HashMap<>();
    private final Map<String, String> nutritionGroupColors = new HashMap<>();
    private final Map<String, Material> nutritionGroupIcons = new HashMap<>();
    private final Map<Material, Set<String>> nutritionFoodGroupMap = new EnumMap<>(Material.class);

    // --- All messages (generic map) ---
    private final Map<String, String> messages = new HashMap<>();

    public FoodDecayConfig() {
        this.plugin = FoodDecayPlugin.getInstance();
        load();
    }

    public void load() {
        FileConfiguration cfg = plugin.getConfig();
        loadGeneralSettings(cfg);
        loadEnvironmentSettings(cfg);
        loadTraitSettings(cfg);
        loadStorageSettings(cfg);
        loadMessages();
        loadFoodProcessingSettings(cfg);
        loadCookingSettings(cfg);
        loadLiquidContainerSettings(cfg);
        loadFermentationSettings(cfg);
        loadWeightSettings(cfg);
        loadNutritionSettings(cfg);
        loadMultiblockSettings(cfg);
        loadRecipesFromFile();
        loadQualitySettings(cfg);
        loadExpirationTimes(cfg);
    }

    // =========================================================================
    //  Load Helpers
    // =========================================================================

    private void loadGeneralSettings(FileConfiguration cfg) {
        this.defaultExpiration = cfg.getLong("default-expiration-minutes", 60);
        this.decayCheckInterval = cfg.getLong("decay-check-interval-ticks", 1200);
        this.showLoreTimer = cfg.getBoolean("show-lore-timer", true);
        this.warningThreshold = cfg.getDouble("warning-threshold", 0.25);
        this.trackAllFood = cfg.getBoolean("track-all-food", true);
        this.freshColor = cfg.getString("colors.fresh", "&a");
        this.warningColor = cfg.getString("colors.warning", "&e");
        this.expiredColor = cfg.getString("colors.expired", "&c");
        this.loreFormat = cfg.getString("lore.format", "{color}⏳ Validade: {status}");
        this.loreExpiredStatus = cfg.getString("lore.expired-status", "EXPIRADO");
        this.timeFormatDays = cfg.getString("time-format.days", "{d}d {h}h");
        this.timeFormatHours = cfg.getString("time-format.hours", "{h}h {m}m");
        this.timeFormatMinutes = cfg.getString("time-format.minutes", "{m}m {s}s");
        this.timeFormatSeconds = cfg.getString("time-format.seconds", "{s}s");
        this.timeFormatExpired = cfg.getString("time-format.expired", "Expirado");
        this.blockExpiredConsume = cfg.getBoolean("behavior.block-expired-consume", true);
        this.stampOnPickup = cfg.getBoolean("behavior.stamp-on-pickup", true);
        this.stampOnCraft = cfg.getBoolean("behavior.stamp-on-craft", true);
        this.stampOnFurnace = cfg.getBoolean("behavior.stamp-on-furnace", true);
        this.stampOnInventoryClick = cfg.getBoolean("behavior.stamp-on-inventory-click", true);
        this.stampOnInventoryOpen = cfg.getBoolean("behavior.stamp-on-inventory-open", true);
        this.stampOnJoin = cfg.getBoolean("behavior.stamp-on-join", true);
        this.stampOnItemSpawn = cfg.getBoolean("behavior.stamp-on-item-spawn", true);
    }

    private void loadEnvironmentSettings(FileConfiguration cfg) {
        this.temperatureEnabled = cfg.getBoolean("temperature.enabled", true);
        temperatureZones.clear();
        ConfigurationSection tempSection = cfg.getConfigurationSection("temperature.zones");
        if (tempSection != null) {
            List<TemperatureZone> zones = new ArrayList<>();
            for (String key : tempSection.getKeys(false)) {
                int maxTemp = tempSection.getInt(key + ".max-temp", 30);
                double multiplier = tempSection.getDouble(key + ".multiplier", 1.0);
                zones.add(new TemperatureZone(key, maxTemp, multiplier));
            }
            zones.sort(Comparator.comparingInt(TemperatureZone::maxTemp));
            temperatureZones.addAll(zones);
        }
        this.depthTemperatureEnabled = cfg.getBoolean("depth-temperature.enabled", false);
        depthZones.clear();
        ConfigurationSection depthSection = cfg.getConfigurationSection("depth-temperature.zones");
        if (depthSection != null) {
            List<DepthZone> dZones = new ArrayList<>();
            for (String key : depthSection.getKeys(false)) {
                int maxY = depthSection.getInt(key + ".max-y", 64);
                int offset = depthSection.getInt(key + ".temperature-offset", 0);
                dZones.add(new DepthZone(key, maxY, offset));
            }
            dZones.sort(Comparator.comparingInt(DepthZone::maxY));
            depthZones.addAll(dZones);
        }
        this.seasonEnabled = cfg.getBoolean("season.enabled", true);
        seasonMultipliers.clear();
        ConfigurationSection seasonSection = cfg.getConfigurationSection("season.multipliers");
        if (seasonSection != null) {
            for (String key : seasonSection.getKeys(false)) {
                seasonMultipliers.put(key.toUpperCase(), seasonSection.getDouble(key, 1.0));
            }
        }
    }

    private void loadTraitSettings(FileConfiguration cfg) {
        this.traitsEnabled = cfg.getBoolean("traits.enabled", true);
        this.autoSmokeFromSmoker = cfg.getBoolean("traits.auto-smoke-from-smoker", true);
        this.maxTraitsPerItem = cfg.getInt("traits.max-per-item", 3);
        traitMultipliers.clear();
        traitIngredients.clear();
        traitDisplayNames.clear();
        traitLoreLines.clear();
        for (FoodTrait trait : FoodTrait.values()) {
            String path = "traits." + trait.name();
            traitMultipliers.put(trait, cfg.getDouble(path + ".multiplier", 0.5));
            String ingredientStr = cfg.getString(path + ".ingredient", "NONE");
            if (!"NONE".equalsIgnoreCase(ingredientStr)) {
                try {
                    traitIngredients.put(trait, Material.valueOf(ingredientStr.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    // Invalid material, no ingredient required
                }
            }
            traitDisplayNames.put(trait, cfg.getString(path + ".display-name", trait.getLoreLine()));
            traitLoreLines.put(trait, cfg.getString(path + ".lore", "&7" + trait.getDisplayName()));
        }
    }

    private void loadStorageSettings(FileConfiguration cfg) {
        this.containerModifiersEnabled = cfg.getBoolean("containers.enabled", true);
        containerMultipliers.clear();
        ConfigurationSection containerSection = cfg.getConfigurationSection("containers.multipliers");
        if (containerSection != null) {
            for (String key : containerSection.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    containerMultipliers.put(mat, containerSection.getDouble(key, 1.0));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        this.iceConservationEnabled = cfg.getBoolean("ice-conservation.enabled", true);
        this.iceMultiplier = cfg.getDouble("ice-conservation.multiplier", 0.3);
        this.iceWarmingDuration = cfg.getLong("ice-conservation.warming-duration", 10);
        this.iceFreezingDuration = cfg.getLong("ice-conservation.freezing-duration", 5);
        iceBlocks.clear();
        List<String> iceList = cfg.getStringList("ice-conservation.ice-blocks");
        for (String name : iceList) {
            try {
                iceBlocks.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (iceBlocks.isEmpty()) {
            iceBlocks.add(Material.ICE);
            iceBlocks.add(Material.PACKED_ICE);
            iceBlocks.add(Material.BLUE_ICE);
            iceBlocks.add(Material.SNOW_BLOCK);
        }
        decayModelStages.clear();
        freezeModelStages.clear();
        ConfigurationSection modelStagesSec = cfg.getConfigurationSection("model-stages");
        if (modelStagesSec != null) {
            for (String key : modelStagesSec.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    ConfigurationSection itemSec = modelStagesSec.getConfigurationSection(key);
                    if (itemSec != null) {
                        List<Integer> decayList = itemSec.getIntegerList("decay");
                        if (!decayList.isEmpty()) {
                            decayModelStages.put(material, decayList.stream().mapToInt(Integer::intValue).toArray());
                        }
                        List<Integer> freezeList = itemSec.getIntegerList("freeze");
                        if (!freezeList.isEmpty()) {
                            freezeModelStages.put(material, freezeList.stream().mapToInt(Integer::intValue).toArray());
                        }
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration msgCfg = loadYamlWithRecovery(messagesFile, "messages.yml");
        messages.clear();
        for (String key : msgCfg.getKeys(false)) {
            Object val = msgCfg.get(key);
            if (val instanceof String s) {
                messages.put(key, s);
            }
        }
    }

    private YamlConfiguration loadYamlWithRecovery(File targetFile, String resourceName) {
        try {
            return YamlConfiguration.loadConfiguration(targetFile);
        } catch (Exception firstFailure) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to load " + targetFile.getName() + ". Restoring bundled copy.", firstFailure);

            backupInvalidFile(targetFile);
            restoreBundledResource(targetFile, resourceName);

            try {
                return YamlConfiguration.loadConfiguration(targetFile);
            } catch (Exception secondFailure) {
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to recover " + targetFile.getName() + " after restoring defaults.", secondFailure);
                return new YamlConfiguration();
            }
        }
    }

    private void backupInvalidFile(File targetFile) {
        if (!targetFile.exists()) {
            return;
        }

        String backupName = targetFile.getName() + ".broken-" + System.currentTimeMillis();
        File backupFile = new File(targetFile.getParentFile(), backupName);
        try {
            Files.copy(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to back up invalid file " + targetFile.getName(), ex);
        }
    }

    private void restoreBundledResource(File targetFile, String resourceName) {
        File parent = targetFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        try (InputStream input = plugin.getResource(resourceName)) {
            if (input == null) {
                plugin.getLogger().warning("Bundled resource not found: " + resourceName);
                return;
            }
            Files.copy(input, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to restore bundled resource " + resourceName, ex);
        }
    }

    private void loadFoodProcessingSettings(FileConfiguration cfg) {
        this.decayByWeightEnabled = cfg.getBoolean("decay-by-weight.enabled", false);
        this.decayByWeightLossInterval = cfg.getDouble("decay-by-weight.loss-interval", 0.25);
        this.ambientSmokeEnabled = cfg.getBoolean("ambient-smoke.enabled", false);
        this.ambientSmokeInterval = Math.max(1, cfg.getInt("ambient-smoke.interval-ticks", 3));
        this.ambientSmokeCount = Math.max(0, cfg.getInt("ambient-smoke.particle-count", 2));
        this.ambientSmokeHeight = Math.max(0, cfg.getDouble("ambient-smoke.height", 6.0));
        this.compostingEnabled = cfg.getBoolean("composting.enabled", false);
        this.compostPartialDecay = cfg.getBoolean("composting.allow-partial-decay", false);
        this.compostMinDecay = Math.max(0, Math.min(1.0, cfg.getDouble("composting.partial-decay-threshold", 0.5)));
        this.compostResultMaterial = cfg.getString("composting.result-material", "BONE_MEAL");
        this.compostResultAmount = Math.max(1, cfg.getInt("composting.result-amount", 1));
        this.portionsEnabled = cfg.getBoolean("portions.enabled", false);
        this.portionsDefault = Math.max(1, cfg.getInt("portions.default-portions", 4));
        portionsPerFood.clear();
        ConfigurationSection portionsSection = cfg.getConfigurationSection("portions.per-food");
        if (portionsSection != null) {
            for (String key : portionsSection.getKeys(false)) {
                try {
                    org.bukkit.Material mat = org.bukkit.Material.valueOf(key.toUpperCase());
                    portionsPerFood.put(mat, Math.max(1, portionsSection.getInt(key, portionsDefault)));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void loadCookingSettings(FileConfiguration cfg) {
        this.cookingEnabled = cfg.getBoolean("cooking.enabled", false);
        this.cookingBurnEnabled = cfg.getBoolean("cooking.burn-enabled", true);
        this.cookingBurnMinutes = Math.max(1, cfg.getLong("cooking.burn-after-minutes", 5));
        this.cookingDefaultTime = Math.max(1, cfg.getLong("cooking.default-cook-minutes", 3));
        cookingHeatMultipliers.clear();
        ConfigurationSection heatSection = cfg.getConfigurationSection("cooking.heat-multipliers");
        if (heatSection != null) {
            for (String key : heatSection.getKeys(false)) {
                cookingHeatMultipliers.put(key.toLowerCase(), Math.max(0.01, heatSection.getDouble(key, 1.0)));
            }
        }
        cookingHeatMultipliers.putIfAbsent("campfire", 1.0);
        cookingHeatMultipliers.putIfAbsent("soul-campfire", 1.5);
        cookingRecipes.clear();
        cookingTimes.clear();
        ConfigurationSection cookingRecipesSection = cfg.getConfigurationSection("cooking.recipes");
        if (cookingRecipesSection != null) {
            for (String key : cookingRecipesSection.getKeys(false)) {
                try {
                    org.bukkit.Material raw = org.bukkit.Material.valueOf(key.toUpperCase());
                    String resultStr = cookingRecipesSection.getString(key + ".result", "");
                    org.bukkit.Material result = org.bukkit.Material.valueOf(resultStr.toUpperCase());
                    cookingRecipes.put(raw, result);
                    long time = cookingRecipesSection.getLong(key + ".minutes", cookingDefaultTime);
                    cookingTimes.put(raw, Math.max(1, time));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void loadLiquidContainerSettings(FileConfiguration cfg) {
        this.liquidContainersEnabled = cfg.getBoolean("liquid-containers.enabled", false);
        this.liquidPourAmount = Math.max(1, cfg.getInt("liquid-containers.pour-amount-mb", 250));
        this.liquidMbPerCauldronLevel = Math.max(1, cfg.getInt("liquid-containers.mb-per-cauldron-level", 333));
        liquidContainerCapacities.clear();
        ConfigurationSection containerCapSection = cfg.getConfigurationSection("liquid-containers.containers");
        if (containerCapSection != null) {
            for (String key : containerCapSection.getKeys(false)) {
                try {
                    org.bukkit.Material mat = org.bukkit.Material.valueOf(key.toUpperCase());
                    liquidContainerCapacities.put(mat, Math.max(1, containerCapSection.getInt(key, 1000)));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        liquidDisplayNames.clear();
        liquidColors.clear();
        ConfigurationSection liquidTypesSection = cfg.getConfigurationSection("liquid-containers.liquid-types");
        if (liquidTypesSection != null) {
            for (String key : liquidTypesSection.getKeys(false)) {
                String upper = key.toUpperCase();
                liquidDisplayNames.put(upper, liquidTypesSection.getString(key + ".name", key));
                liquidColors.put(upper, liquidTypesSection.getString(key + ".color", "&b"));
            }
        }
    }

    private void loadFermentationSettings(FileConfiguration cfg) {
        this.fermentationEnabled = cfg.getBoolean("fermentation.enabled", false);
        fermentRecipes.clear();
        ConfigurationSection fermentSection = cfg.getConfigurationSection("fermentation.recipes");
        if (fermentSection != null) {
            for (String recipeId : fermentSection.getKeys(false)) {
                try {
                    ConfigurationSection r = fermentSection.getConfigurationSection(recipeId);
                    if (r == null) continue;
                    String inputLiquid = r.getString("input-liquid", "").toUpperCase();
                    int requiredMb = Math.max(1, r.getInt("required-mb", 250));
                    int timeMinutes = Math.max(1, r.getInt("time-minutes", 30));
                    org.bukkit.Material resultMat;
                    try {
                        resultMat = org.bukkit.Material.valueOf(r.getString("result-material", "POTION").toUpperCase());
                    } catch (IllegalArgumentException e) {
                        resultMat = org.bukkit.Material.POTION;
                    }
                    String displayName = r.getString("display-name", recipeId);
                    List<DrinkEffect> effects = new ArrayList<>();
                    List<Map<?, ?>> effectList = r.getMapList("effects");
                    for (Map<?, ?> effectMap : effectList) {
                        try {
                            String effectName = String.valueOf(effectMap.get("type")).toUpperCase();
                            org.bukkit.potion.PotionEffectType effectType =
                                    org.bukkit.Registry.EFFECT.get(NamespacedKey.minecraft(effectName.toLowerCase()));
                            int duration = effectMap.containsKey("duration")
                                    ? ((Number) effectMap.get("duration")).intValue() : 600;
                            int amplifier = effectMap.containsKey("amplifier")
                                    ? ((Number) effectMap.get("amplifier")).intValue() : 0;
                            if (effectType != null) {
                                effects.add(new DrinkEffect(effectType, duration, amplifier));
                            }
                        } catch (Exception e) {
                            Bukkit.getLogger().warning("[FoodDecay] Failed to parse effect in ferment recipe '" + recipeId + "': " + e.getMessage());
                        }
                    }
                    fermentRecipes.put(recipeId, new FermentRecipe(
                            recipeId, inputLiquid, requiredMb, timeMinutes, resultMat, displayName, effects
                    ));
                } catch (Exception ignored) {}
            }
        }
    }

    private void loadWeightSettings(FileConfiguration cfg) {
        WeightSettingsLoader.LoadedWeightSettings settings = WeightSettingsLoader.load(cfg);
        this.weightEnabled = settings.enabled();
        this.weightContainerRestrictionsEnabled = settings.containerRestrictionsEnabled();
        this.weightMaxKgPerStack = settings.maxKgPerStack();
        this.weightDefaultSize = settings.defaultSize();
        this.weightDefaultKg = settings.defaultKg();

        weightPerFood.clear();
        weightPerFood.putAll(settings.weightPerFood());

        sizePerFood.clear();
        sizePerFood.putAll(settings.sizePerFood());

        weightSizeDisplayNames.clear();
        weightSizeDisplayNames.putAll(settings.sizeDisplayNames());

        weightSizeColors.clear();
        weightSizeColors.putAll(settings.sizeColors());

        containerSizeRestrictions.clear();
        containerSizeRestrictions.putAll(settings.containerRestrictions());
    }

    public static double ouncesToKg(double ounces) {
        return ounces * KG_PER_OUNCE;
    }

    private void loadNutritionSettings(FileConfiguration cfg) {
        this.nutritionEnabled = cfg.getBoolean("nutrition.enabled", false);
        this.nutritionGainPerFood = cfg.getDouble("nutrition.gain-per-food", 20.0);
        this.nutritionDecayPerMinute = cfg.getDouble("nutrition.decay-per-minute", 0.5);
        this.nutritionHealthBonusPerGroup = cfg.getDouble("nutrition.health-bonus-per-group", 2.0);
        this.nutritionResetOnDeath = cfg.getBoolean("nutrition.reset-on-death", true);
        this.nutritionActivationThreshold = cfg.getDouble("nutrition.activation-threshold", 25.0);
        nutritionFoodGroupMap.clear();
        nutritionGroupDisplayNames.clear();
        nutritionGroupColors.clear();
        nutritionGroupIcons.clear();
        nutritionGroupBonuses.clear();
        nutritionConfiguredMmocoreStats.clear();
        ConfigurationSection groupsSection = cfg.getConfigurationSection("nutrition.groups");
        if (groupsSection != null) {
            for (String groupKey : groupsSection.getKeys(false)) {
                ConfigurationSection gs = groupsSection.getConfigurationSection(groupKey);
                if (gs == null) continue;
                nutritionGroupDisplayNames.put(groupKey, gs.getString("display-name", groupKey));
                nutritionGroupColors.put(groupKey, gs.getString("color", "&7"));
                String iconStr = gs.getString("icon", "STONE");
                nutritionGroupIcons.put(groupKey, parseMaterial(iconStr, Material.STONE));
                for (String foodStr : gs.getStringList("foods")) {
                    Material food = parseMaterial(foodStr, null);
                    if (food != null) {
                        nutritionFoodGroupMap.computeIfAbsent(food, k -> new HashSet<>()).add(groupKey);
                    }
                }
                double healthBonus = gs.getDouble("health-bonus", nutritionHealthBonusPerGroup);
                Double activationThreshold = gs.contains("activation-threshold")
                        ? gs.getDouble("activation-threshold")
                        : null;
                List<String> effectStrings = gs.getStringList("effects");
                Map<Attribute, Double> attributeBonuses = new LinkedHashMap<>();
                ConfigurationSection attributesSection = gs.getConfigurationSection("attributes");
                if (attributesSection != null) {
                    for (String attributeKey : attributesSection.getKeys(false)) {
                        Attribute attribute = parseAttribute(attributeKey);
                        if (attribute != null) {
                            attributeBonuses.put(attribute, attributesSection.getDouble(attributeKey));
                        }
                    }
                }
                Map<String, Double> mmocoreStats = new HashMap<>();
                ConfigurationSection statsSection = gs.getConfigurationSection("mmocore-stats");
                if (statsSection != null) {
                    for (String stat : statsSection.getKeys(false)) {
                        String normalizedStat = stat.trim().toUpperCase(Locale.ROOT);
                        mmocoreStats.put(normalizedStat, statsSection.getDouble(stat));
                        nutritionConfiguredMmocoreStats.add(normalizedStat);
                    }
                }
                nutritionGroupBonuses.put(groupKey, new GroupBonus(
                        healthBonus,
                        activationThreshold,
                        List.copyOf(effectStrings),
                        Map.copyOf(attributeBonuses),
                        Map.copyOf(mmocoreStats)
                ));
            }
        }
    }

    private void loadMultiblockSettings(FileConfiguration cfg) {
        this.vinegarRecipeEnabled = cfg.getBoolean("recipes.vinegar.enabled", true);
        this.cauldronRecipesEnabled = cfg.getBoolean("cauldron-recipes.enabled", true);
        this.cauldronMaxIngredients = cfg.getInt("cauldron-recipes.max-ingredients", 3);
        this.multiblockEnabled = cfg.getBoolean("multiblock.enabled", true);
        this.notificationRadius = cfg.getInt("multiblock.notification-radius", 32);
        this.abandonmentMinutes = cfg.getInt("multiblock.abandonment-minutes", 30);
        this.proximityRadius = cfg.getInt("multiblock.proximity-radius", 8);
        this.proximityBonusPerTick = cfg.getDouble("multiblock.proximity-bonus-per-tick", 0.3);
        this.qteIntervalSeconds = cfg.getInt("multiblock.qte.interval-seconds", 180);
        this.qteDurationSeconds = cfg.getInt("multiblock.qte.duration-seconds", 120);
        this.qteChance = cfg.getDouble("multiblock.qte.chance", 0.5);
        this.qteMaxPerCycle = cfg.getInt("multiblock.qte.max-per-cycle", 3);
        this.qteBonusPerEvent = (float) cfg.getDouble("multiblock.qte.bonus-per-event", 10.0);
        this.sessionTimeoutMinutes = cfg.getInt("multiblock.session-timeout-minutes", 10);
        this.qteMissPenalty = (float) cfg.getDouble("multiblock.qte-miss-penalty", 5.0);
        this.multiSlotBaseSlots = cfg.getInt("multiblock.multi-slot.base-slots", 1);
        this.multiSlotProfession = cfg.getString("multiblock.multi-slot.profession", "cooking");
        multiSlotTiers.clear();
        ConfigurationSection slotTiersSection = cfg.getConfigurationSection("multiblock.multi-slot.tiers");
        if (slotTiersSection != null) {
            for (String key : slotTiersSection.getKeys(false)) {
                int level = slotTiersSection.getInt(key + ".level", 0);
                int slots = slotTiersSection.getInt(key + ".slots", 1);
                multiSlotTiers.add(new MultiSlotTier(level, slots));
            }
            multiSlotTiers.sort(java.util.Comparator.comparingInt(MultiSlotTier::level));
        }
        this.machineTierT2Level = cfg.getInt("multiblock.machine-tiers.tier2-level", 10);
        this.machineTierT3Level = cfg.getInt("multiblock.machine-tiers.tier3-level", 25);
        multiblockTypeEnabled.clear();
        multiblockProcessingMinutes.clear();
        multiblockRecipes.clear();
        multiblockDisplayNames.clear();
        multiblockDescriptions.clear();
        for (MultiblockType type : MultiblockType.values()) {
            String key = "multiblock.structures." + type.getConfigKey();
            multiblockTypeEnabled.put(type, cfg.getBoolean(key + ".enabled", true));
            multiblockProcessingMinutes.put(type,
                    cfg.getInt(key + ".processing-minutes", type.getDefaultProcessingMinutes()));
            multiblockDisplayNames.put(type,
                    cfg.getString(key + ".display-name", type.getDisplayName()));
            List<String> desc = cfg.getStringList(key + ".description");
            multiblockDescriptions.put(type, desc.isEmpty() ? type.getDescription() : desc);
        }
        this.smokehouseFuelMaterials = loadMaterialList(
                cfg.getStringList("multiblock.structures.defumeiro.fuel-materials"),
                List.of(Material.COAL, Material.CHARCOAL));
        this.saltMaterial = parseMaterial(
                cfg.getString("multiblock.structures.barril-de-sal.salt-material", "SUGAR"), Material.SUGAR);
        this.saltRequired = cfg.getInt("multiblock.structures.barril-de-sal.salt-required", 2);
        this.picklingWaterMaterial = parseMaterial(
                cfg.getString("multiblock.structures.tina-de-conserva.water-material", "WATER_BUCKET"), Material.WATER_BUCKET);
        this.picklingWaterReturn = parseMaterial(
                cfg.getString("multiblock.structures.tina-de-conserva.water-return", "BUCKET"), Material.BUCKET);
        this.picklingVinegarMaterial = parseMaterial(
                cfg.getString("multiblock.structures.tina-de-conserva.vinegar-material", "GLASS_BOTTLE"), Material.GLASS_BOTTLE);
        this.picklingFuelMaterials = loadMaterialList(
                cfg.getStringList("multiblock.structures.tina-de-conserva.fuel-materials"),
                List.of(Material.COAL, Material.CHARCOAL));
        this.waxMaterial = parseMaterial(
                cfg.getString("multiblock.structures.prensa-de-selagem.wax-material", "HONEYCOMB"), Material.HONEYCOMB);
    }

    private void loadRecipesFromFile() {
        multiblockRecipes.clear();
        multiblockRecipes.putAll(RecipeConfigurationStore.loadAll(getRecipesFile(), this::loadRecipe));
    }

    private void loadQualitySettings(FileConfiguration cfg) {
        this.qualityTier1Threshold = (float) cfg.getDouble("quality.tier1-threshold", 5.0);
        this.qualityTier2Threshold = (float) cfg.getDouble("quality.tier2-threshold", 20.0);
        this.qualityTier0Prefix = cfg.getString("quality.tier0-prefix", "&7Comum ");
        this.qualityTier1Prefix = cfg.getString("quality.tier1-prefix", "&a\u2714 Bom ");
        this.qualityTier2Prefix = cfg.getString("quality.tier2-prefix", "&6\u2b50 Excelente ");
        this.qualityTier0Lore = cfg.getString("quality.tier0-lore", "&7Qualidade Comum");
        this.qualityTier1Lore = cfg.getString("quality.tier1-lore", "&a\u2714 Boa Qualidade");
        this.qualityTier2Lore = cfg.getString("quality.tier2-lore", "&e\u2b50 Qualidade Excelente");
        this.cauldronResultMaterial = parseMaterial(
                cfg.getString("cauldron-recipes.result-material", "RABBIT_STEW"), Material.RABBIT_STEW);
        this.cauldronResultName = cfg.getString("cauldron-recipes.result-name", "&6Ensopado Artesanal");
        this.cauldronResultLore = cfg.getStringList("cauldron-recipes.result-lore");
        this.vinegarResultMaterial = parseMaterial(
                cfg.getString("vinegar.result-material", "HONEY_BOTTLE"), Material.HONEY_BOTTLE);
        this.vinegarResultName = cfg.getString("vinegar.result-name", "&eVinagre");
        this.vinegarResultLore = cfg.getStringList("vinegar.result-lore");
        this.vinegarIngredients = new ArrayList<>();
        for (String s : cfg.getStringList("vinegar.ingredients")) {
            Material m = parseMaterial(s, null);
            if (m != null) vinegarIngredients.add(m);
        }
        this.guiEnabled = cfg.getBoolean("gui.enabled", true);
        this.inspectOnShiftClick = cfg.getBoolean("gui.inspect-on-shift-click", true);
        this.guiTitleInspection = cfg.getString("gui.titles.inspection", "&f\uD83D\uDD0D Inspecao de Alimento");
        this.guiTitleNutrition = cfg.getString("gui.titles.nutrition", "&6&l\uD83C\uDF4E Nutricao");
        this.guiTitleCookbook = cfg.getString("gui.titles.cookbook", "&8&l\uD83D\uDCD6 Livro de Receitas");
        this.guiTitleMultiblockInspection = cfg.getString("gui.titles.multiblock-inspection", "&f\u2692 {machine}");
    }

    private void loadExpirationTimes(FileConfiguration cfg) {
        expirationTimes.clear();
        ConfigurationSection expirationSection = cfg.getConfigurationSection("expiration-minutes");
        if (expirationSection != null) {
            Set<String> keys = expirationSection.getKeys(false);
            for (String key : keys) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    long minutes = cfg.getLong("expiration-minutes." + key);
                    expirationTimes.put(material, minutes);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void reload() {
        plugin.reloadConfig();
        load();
    }

    /**
     * Saves a single config value to config.yml and reloads.
     */
    public void saveValue(String path, Object value) {
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
        reload();
    }

    // --- Expiration logic ---

    /**
     * Get the base expiration time in minutes for a material.
     * Returns -1 if the food never expires.
     * Returns 0 if the food is not configured (not tracked).
     */
    public long getBaseExpirationMinutes(Material material) {
        Long minutes = expirationTimes.get(material);
        if (minutes == null) {
            if (material.isEdible() && trackAllFood) {
                return defaultExpiration;
            }
            return 0;
        }
        if (minutes < 0) return -1;
        return minutes;
    }

    public long getExpirationMillis(Material material) {
        long minutes = getBaseExpirationMinutes(material);
        if (minutes <= 0) return minutes;
        return minutes * 60_000L;
    }

    public boolean isTracked(Material material) {
        return material.isEdible() && getBaseExpirationMinutes(material) > 0;
    }

    public boolean neverExpires(Material material) {
        return getBaseExpirationMinutes(material) == -1;
    }

    // --- Temperature ---

    public boolean isTemperatureEnabled() { return temperatureEnabled; }
    public List<TemperatureZone> getTemperatureZones() { return Collections.unmodifiableList(temperatureZones); }

    public double getTemperatureMultiplier(int temperature) {
        for (TemperatureZone zone : temperatureZones) {
            if (temperature <= zone.maxTemp()) {
                return zone.multiplier();
            }
        }
        // Above all zones — use the highest multiplier
        return temperatureZones.isEmpty() ? 1.0 : temperatureZones.getLast().multiplier();
    }

    // --- Depth-temperature ---

    public boolean isDepthTemperatureEnabled() { return depthTemperatureEnabled; }
    public List<DepthZone> getDepthZones() { return Collections.unmodifiableList(depthZones); }

    /**
     * Gets the temperature offset for a given Y-level.
     * Returns 0 if depth-temperature is disabled or Y is above all configured zones.
     */
    public int getDepthTemperatureOffset(int y) {
        if (!depthTemperatureEnabled) return 0;
        for (DepthZone zone : depthZones) {
            if (y <= zone.maxY()) {
                return zone.temperatureOffset();
            }
        }
        return 0; // Above all depth zones — no offset
    }

    /**
     * Gets the depth zone name for a given Y-level.
     * Returns null if depth-temperature is disabled or Y is above all configured zones.
     */
    public String getDepthZoneName(int y) {
        if (!depthTemperatureEnabled) return null;
        for (DepthZone zone : depthZones) {
            if (y <= zone.maxY()) {
                return zone.name();
            }
        }
        return null;
    }

    // --- Season ---

    public boolean isSeasonEnabled() { return seasonEnabled; }

    public double getSeasonMultiplier(String season) {
        return seasonMultipliers.getOrDefault(season.toUpperCase(), 1.0);
    }
    public Map<String, Double> getSeasonMultipliers() { return Collections.unmodifiableMap(seasonMultipliers); }

    // --- Traits ---

    public boolean isTraitsEnabled() { return traitsEnabled; }
    public boolean isAutoSmokeFromSmoker() { return autoSmokeFromSmoker; }
    public int getMaxTraitsPerItem() { return maxTraitsPerItem; }

    public double getTraitMultiplier(FoodTrait trait) {
        return traitMultipliers.getOrDefault(trait, 0.5);
    }

    public Material getTraitIngredient(FoodTrait trait) {
        return traitIngredients.get(trait);
    }

    public String getTraitDisplayName(FoodTrait trait) {
        return traitDisplayNames.getOrDefault(trait, trait.getDisplayName());
    }

    public String getTraitLoreLine(FoodTrait trait) {
        return traitLoreLines.getOrDefault(trait, trait.getLoreLine());
    }

    // --- Containers ---

    public boolean isContainerModifiersEnabled() { return containerModifiersEnabled; }

    public double getContainerMultiplier(Material containerType) {
        return containerMultipliers.getOrDefault(containerType, 1.0);
    }

    public Map<Material, Double> getContainerMultipliers() { return Collections.unmodifiableMap(containerMultipliers); }

    // --- Ice conservation ---

    public boolean isIceConservationEnabled() { return iceConservationEnabled; }
    public double getIceMultiplier() { return iceMultiplier; }
    public long getIceWarmingDuration() { return iceWarmingDuration; }
    public long getIceFreezingDuration() { return iceFreezingDuration; }
    public Set<Material> getIceBlocks() { return Collections.unmodifiableSet(iceBlocks); }

    // --- Model stages ---

    public int[] getDecayModelStages(Material material) { return decayModelStages.get(material); }
    public int[] getFreezeModelStages(Material material) { return freezeModelStages.get(material); }

    // --- General getters ---

    public long getDefaultExpiration() { return defaultExpiration; }
    public long getDecayCheckInterval() { return decayCheckInterval; }
    public boolean showLoreTimer() { return showLoreTimer; }
    public double getWarningThreshold() { return warningThreshold; }
    public boolean trackAllFood() { return trackAllFood; }

    // --- Color getters ---

    public String getFreshColor() { return freshColor; }
    public String getWarningColor() { return warningColor; }
    public String getExpiredColor() { return expiredColor; }

    // --- Lore getters ---

    public String getLoreFormat() { return loreFormat; }
    public String getLoreExpiredStatus() { return loreExpiredStatus; }

    // --- Time format getters ---

    public String getTimeFormatDays() { return timeFormatDays; }
    public String getTimeFormatHours() { return timeFormatHours; }
    public String getTimeFormatMinutes() { return timeFormatMinutes; }
    public String getTimeFormatSeconds() { return timeFormatSeconds; }
    public String getTimeFormatExpired() { return timeFormatExpired; }

    // --- Behavior getters ---

    public boolean blockExpiredConsume() { return blockExpiredConsume; }
    public boolean stampOnPickup() { return stampOnPickup; }
    public boolean stampOnCraft() { return stampOnCraft; }
    public boolean stampOnFurnace() { return stampOnFurnace; }
    public boolean stampOnInventoryClick() { return stampOnInventoryClick; }
    public boolean stampOnInventoryOpen() { return stampOnInventoryOpen; }
    public boolean stampOnJoin() { return stampOnJoin; }
    public boolean isStampOnItemSpawn() { return stampOnItemSpawn; }

    // --- Message getters (delegate to generic msg()) ---

    public String getMsgExpiredConsume() { return msg("expired-consume"); }
    public String getMsgCommandHeader() { return msg("command-header"); }
    public String getMsgReloadSuccess() { return msg("reload-success"); }
    public String getMsgPreserveAutoSmoked() { return msg("preserve-auto-smoked"); }

    // --- Decay by weight getters ---

    public boolean isDecayByWeightEnabled() { return decayByWeightEnabled; }
    public double getDecayByWeightLossInterval() { return decayByWeightLossInterval; }

    // --- Ambient smoke getters ---

    public boolean isAmbientSmokeEnabled() { return ambientSmokeEnabled; }
    public int getAmbientSmokeInterval() { return ambientSmokeInterval; }
    public int getAmbientSmokeCount() { return ambientSmokeCount; }
    public double getAmbientSmokeHeight() { return ambientSmokeHeight; }

    // --- Composting getters ---

    public boolean isCompostingEnabled() { return compostingEnabled; }
    public boolean isCompostPartialDecay() { return compostPartialDecay; }
    public double getCompostMinDecay() { return compostMinDecay; }
    public String getCompostResultMaterial() { return compostResultMaterial; }
    public int getCompostResultAmount() { return compostResultAmount; }

    // --- Cooking getters ---

    public boolean isCookingEnabled() { return cookingEnabled; }
    public boolean isCookingBurnEnabled() { return cookingBurnEnabled; }
    public long getCookingBurnMinutes() { return cookingBurnMinutes; }
    public double getCookingHeatMultiplier(String type) {
        return cookingHeatMultipliers.getOrDefault(type.toLowerCase(), 1.0);
    }
    public org.bukkit.Material getCookingResult(org.bukkit.Material raw) {
        return cookingRecipes.get(raw);
    }
    public long getCookingTime(org.bukkit.Material raw) {
        return cookingTimes.getOrDefault(raw, cookingDefaultTime);
    }
    public long getCookingDefaultTime() { return cookingDefaultTime; }
    public Map<String, Double> getCookingHeatMultipliers() { return Collections.unmodifiableMap(cookingHeatMultipliers); }
    public Map<org.bukkit.Material, org.bukkit.Material> getCookingRecipes() { return Collections.unmodifiableMap(cookingRecipes); }

    // --- Portions getters ---

    public boolean isPortionsEnabled() { return portionsEnabled; }
    public int getPortionsDefault() { return portionsDefault; }
    public Map<org.bukkit.Material, Integer> getPortionsPerFood() { return Collections.unmodifiableMap(portionsPerFood); }
    public int getPortions(org.bukkit.Material material) {
        return portionsPerFood.getOrDefault(material, portionsDefault);
    }

    // --- Liquid container getters ---

    public boolean isLiquidContainersEnabled() { return liquidContainersEnabled; }
    public int getLiquidPourAmount() { return liquidPourAmount; }
    public int getLiquidMbPerCauldronLevel() { return liquidMbPerCauldronLevel; }
    public int getLiquidContainerCapacity(org.bukkit.Material material) {
        return liquidContainerCapacities.getOrDefault(material, 0);
    }
    public String getLiquidDisplayName(String type) {
        return liquidDisplayNames.getOrDefault(type.toUpperCase(), type);
    }
    public String getLiquidColor(String type) {
        return liquidColors.getOrDefault(type.toUpperCase(), "&b");
    }
    public Map<org.bukkit.Material, Integer> getLiquidContainerCapacities() { return Collections.unmodifiableMap(liquidContainerCapacities); }
    public Map<String, String> getLiquidDisplayNames() { return Collections.unmodifiableMap(liquidDisplayNames); }

    // --- Weight/Size getters ---

    public boolean isWeightEnabled() { return weightEnabled; }
    public boolean isWeightContainerRestrictionsEnabled() { return weightContainerRestrictionsEnabled; }
    public double getWeightMaxKgPerStack() { return weightMaxKgPerStack; }
    public String getWeightDefaultSize() { return weightDefaultSize; }
    public double getWeightDefaultKg() { return weightDefaultKg; }
    public Map<Material, Double> getWeightPerFoodKg() { return Collections.unmodifiableMap(weightPerFood); }
    public double getFoodWeightKg(Material material) {
        return weightPerFood.getOrDefault(material, weightDefaultKg);
    }
    public String getFoodSize(Material material) {
        return sizePerFood.getOrDefault(material, weightDefaultSize);
    }
    public String getWeightSizeDisplayName(String size) {
        return weightSizeDisplayNames.get(size.toUpperCase());
    }
    public String getWeightSizeColor(String size) {
        return weightSizeColors.get(size.toUpperCase());
    }
    public Set<String> getContainerSizeRestrictions(Material containerType) {
        return containerSizeRestrictions.get(containerType);
    }

    // --- Fermentation getters ---

    public boolean isFermentationEnabled() { return fermentationEnabled; }
    public java.util.Collection<FermentRecipe> getFermentRecipes() { return fermentRecipes.values(); }
    public FermentRecipe getFermentRecipe(String id) { return fermentRecipes.get(id); }
    public FermentRecipe findFermentRecipe(String liquidType) {
        for (FermentRecipe recipe : fermentRecipes.values()) {
            if (recipe.inputLiquid().equalsIgnoreCase(liquidType)) return recipe;
        }
        return null;
    }

    public List<FermentRecipe> findAllFermentRecipes(String liquidType) {
        List<FermentRecipe> result = new ArrayList<>();
        for (FermentRecipe recipe : fermentRecipes.values()) {
            if (recipe.inputLiquid().equalsIgnoreCase(liquidType)) result.add(recipe);
        }
        return result;
    }

    // --- Nutrition getters ---

    public boolean isNutritionEnabled() { return nutritionEnabled; }
    public double getNutritionGainPerFood() { return nutritionGainPerFood; }
    public double getNutritionDecayPerMinute() { return nutritionDecayPerMinute; }
    public double getNutritionHealthBonusPerGroup() { return nutritionHealthBonusPerGroup; }
    public boolean isNutritionResetOnDeath() { return nutritionResetOnDeath; }
    public double getNutritionActivationThreshold() { return nutritionActivationThreshold; }
    public Map<String, GroupBonus> getNutritionGroupBonuses() { return Collections.unmodifiableMap(nutritionGroupBonuses); }
    public Set<String> getNutritionConfiguredMmocoreStats() { return Collections.unmodifiableSet(nutritionConfiguredMmocoreStats); }

    // --- Vinegar recipe getters ---

    public boolean isVinegarRecipeEnabled() { return vinegarRecipeEnabled; }

    // --- Cauldron recipes getters ---

    public boolean isCauldronRecipesEnabled() { return cauldronRecipesEnabled; }
    public int getCauldronMaxIngredients() { return cauldronMaxIngredients; }

    // --- Multiblock getters ---

    public boolean isMultiblockEnabled() { return multiblockEnabled; }
    public int getNotificationRadius() { return notificationRadius; }
    public int getAbandonmentMinutes() { return abandonmentMinutes; }
    public int getProximityRadius() { return proximityRadius; }
    public double getProximityBonusPerTick() { return proximityBonusPerTick; }
    public int getQteIntervalSeconds() { return qteIntervalSeconds; }
    public int getQteDurationSeconds() { return qteDurationSeconds; }
    public double getQteChance() { return qteChance; }
    public int getQteMaxPerCycle() { return qteMaxPerCycle; }
    public float getQteBonusPerEvent() { return qteBonusPerEvent; }

    /**
     * Returns the maximum number of slots a machine can have for the given player.
     * Based on MMOCore profession level.
     */
    public int getMaxSlots(org.bukkit.entity.Player player) {
        int slots = multiSlotBaseSlots;
        if (!multiSlotTiers.isEmpty() && multiSlotProfession != null) {
            int level = MMOCoreHook.getProfessionLevel(player, multiSlotProfession);
            for (MultiSlotTier tier : multiSlotTiers) {
                if (level >= tier.level()) slots = tier.slots();
            }
        }
        return Math.max(1, slots);
    }

    public int getMultiSlotBaseSlots() { return multiSlotBaseSlots; }
    public String getMultiSlotProfession() { return multiSlotProfession; }

    /**
     * Returns the machine tier (1–3) for the given player based on MMOCore profession level.
     */
    public int getMachineTier(org.bukkit.entity.Player player) {
        if (multiSlotProfession == null) return 1;
        int level = MMOCoreHook.getProfessionLevel(player, multiSlotProfession);
        if (level >= machineTierT3Level) return 3;
        if (level >= machineTierT2Level) return 2;
        return 1;
    }

    public boolean isMultiblockTypeEnabled(MultiblockType type) {
        return multiblockTypeEnabled.getOrDefault(type, true);
    }
    public int getMultiblockProcessingMinutes(MultiblockType type) {
        return multiblockProcessingMinutes.getOrDefault(type, type.getDefaultProcessingMinutes());
    }
    public String getMultiblockDisplayName(MultiblockType type) {
        return multiblockDisplayNames.getOrDefault(type, type.getDisplayName());
    }
    public List<String> getMultiblockDescription(MultiblockType type) {
        return multiblockDescriptions.getOrDefault(type, type.getDescription());
    }

    /**
     * Returns all configured recipes for a machine type.
     */
    public List<MultiblockRecipe> getRecipes(MultiblockType type) {
        return multiblockRecipes.getOrDefault(type, List.of());
    }

    /**
     * Finds a recipe that matches the given input item for a machine type.
     * Returns null if no recipe matches.
     */
    public MultiblockRecipe findRecipe(MultiblockType type, ItemStack input) {
        for (MultiblockRecipe recipe : getRecipes(type)) {
            if (recipe.matchesInput(input)) return recipe;
        }
        return null;
    }

    /**
     * Returns true if the machine type has custom recipes configured.
     */
    public boolean hasRecipes(MultiblockType type) {
        List<MultiblockRecipe> recipes = multiblockRecipes.get(type);
        return recipes != null && !recipes.isEmpty();
    }

    private MultiblockRecipe loadRecipe(String id, MultiblockType type,
                                        ConfigurationSection section) {
        if (section == null) return null;

        // Input
        Material inputMaterial = null;
        String inputMmoType = null;
        String inputMmoId = null;
        String inputItemsAdderId = null;
        int inputModelData = 0;

        ConfigurationSection inputSec = section.getConfigurationSection("input");
        if (inputSec != null) {
            String itemsAdderStr = inputSec.getString("itemsadder");
            if (itemsAdderStr != null && !itemsAdderStr.isBlank()) {
                inputItemsAdderId = itemsAdderStr.trim();
                ItemStack preview = ItemsAdderHook.createItem(inputItemsAdderId);
                if (preview != null) {
                    inputMaterial = preview.getType();
                }
            } else {
                String mmoStr = inputSec.getString("mmoitems");
                if (mmoStr != null && mmoStr.contains(":")) {
                    String[] parts = mmoStr.split(":", 2);
                    inputMmoType = parts[0];
                    inputMmoId = parts[1];
                    ItemStack preview = MMOItemsHook.createItem(inputMmoType, inputMmoId);
                    if (preview != null) {
                        inputMaterial = preview.getType();
                    }
                } else {
                    String matStr = inputSec.getString("material");
                    if (matStr != null) {
                        try {
                            inputMaterial = Material.valueOf(matStr.toUpperCase());
                        } catch (IllegalArgumentException ignored) {
                            // Invalid material name in recipe config
                        }
                    }
                }
            }
            inputModelData = inputSec.getInt("custom-model-data", 0);
        }

        if (inputMaterial == null && inputMmoType == null && inputItemsAdderId == null) return null;

        // Output
        Material outputMaterial = null;
        String outputMmoType = null;
        String outputMmoId = null;
        String outputItemsAdderId = null;
        String outputName = null;
        List<String> outputLore = null;
        int outputModelData = 0;

        ConfigurationSection outputSec = section.getConfigurationSection("output");
        if (outputSec != null) {
            String itemsAdderStr = outputSec.getString("itemsadder");
            if (itemsAdderStr != null && !itemsAdderStr.isBlank()) {
                outputItemsAdderId = itemsAdderStr.trim();
                ItemStack preview = ItemsAdderHook.createItem(outputItemsAdderId);
                if (preview != null) {
                    outputMaterial = preview.getType();
                }
            } else {
                String mmoStr = outputSec.getString("mmoitems");
                if (mmoStr != null && mmoStr.contains(":")) {
                    String[] parts = mmoStr.split(":", 2);
                    outputMmoType = parts[0];
                    outputMmoId = parts[1];
                    ItemStack preview = MMOItemsHook.createItem(outputMmoType, outputMmoId);
                    if (preview != null) {
                        outputMaterial = preview.getType();
                    }
                } else {
                    String matStr = outputSec.getString("material");
                    if (matStr != null) {
                        try {
                            outputMaterial = Material.valueOf(matStr.toUpperCase());
                        } catch (IllegalArgumentException ignored) {
                            // Invalid material name in recipe config
                        }
                    }
                }
            }
            outputName = outputSec.getString("name");
            outputLore = outputSec.getStringList("lore");
            outputModelData = outputSec.getInt("custom-model-data", 0);
        }

        // Spoiled variant
        int spoiledModelData = 0;
        String spoiledName = null;
        ConfigurationSection spoiledSec = section.getConfigurationSection("spoiled");
        if (spoiledSec != null) {
            spoiledModelData = spoiledSec.getInt("custom-model-data", 0);
            spoiledName = spoiledSec.getString("name");
        }

        // Processing
        int timeMinutes = section.getInt("time-minutes",
                multiblockProcessingMinutes.getOrDefault(type, type.getDefaultProcessingMinutes()));

        FoodTrait trait = null;
        String traitStr = section.getString("trait");
        if (traitStr != null) {
            trait = FoodTrait.fromString(traitStr);
        }

        // Multi-stage: optional required trait or previous recipe
        FoodTrait requiresTrait = null;
        String reqTraitStr = section.getString("requires-trait");
        if (reqTraitStr != null) {
            requiresTrait = FoodTrait.fromString(reqTraitStr);
        }
        String requiresRecipe = section.getString("requires-recipe");
        List<RecipeIngredient> extraIngredients = new ArrayList<>();
        for (Map<?, ?> rawMap : section.getMapList("extra-inputs")) {
            RecipeIngredient ingredient = RecipeIngredient.fromConfigMap(rawMap);
            if (ingredient != null) {
                extraIngredients.add(ingredient);
            }
        }
        List<String> nutritionGroups = section.getStringList("nutrition-groups").stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .toList();

        MultiblockRecipe recipe = new MultiblockRecipe(id, type,
                inputMaterial, inputMmoType, inputMmoId, inputItemsAdderId, inputModelData,
                outputMaterial, outputMmoType, outputMmoId, outputItemsAdderId,
                outputName, outputLore, outputModelData,
                spoiledModelData, spoiledName,
                timeMinutes, trait,
                requiresTrait, requiresRecipe, extraIngredients, nutritionGroups);

        // MMOCore optional fields
        String profession = section.getString("profession");
        if (profession != null) {
            recipe.setProfession(profession);
            recipe.setProfessionLevel(section.getInt("profession-level", 1));
        }
        String expProfession = section.getString("experience-profession");
        if (expProfession != null) {
            recipe.setExperienceProfession(expProfession);
            recipe.setExperienceReward(section.getDouble("experience-reward", 0));
        }

        return recipe;
    }

    // --- GUI getters ---

    public boolean isGuiEnabled() { return guiEnabled; }
    public boolean isInspectOnShiftClick() { return inspectOnShiftClick; }

    // --- Session & QTE getters ---

    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public float getQteMissPenalty() { return qteMissPenalty; }

    // --- Quality tier getters ---

    public float getQualityTier1Threshold() { return qualityTier1Threshold; }
    public float getQualityTier2Threshold() { return qualityTier2Threshold; }
    public String getQualityTier0Prefix() { return qualityTier0Prefix; }
    public String getQualityTier1Prefix() { return qualityTier1Prefix; }
    public String getQualityTier2Prefix() { return qualityTier2Prefix; }
    public String getQualityTier0Lore() { return qualityTier0Lore; }
    public String getQualityTier1Lore() { return qualityTier1Lore; }
    public String getQualityTier2Lore() { return qualityTier2Lore; }

    // --- Cauldron result getters ---

    public Material getCauldronResultMaterial() { return cauldronResultMaterial; }
    public String getCauldronResultName() { return cauldronResultName; }
    public List<String> getCauldronResultLore() { return Collections.unmodifiableList(cauldronResultLore); }

    // --- Vinegar recipe config getters ---

    public Material getVinegarResultMaterial() { return vinegarResultMaterial; }
    public String getVinegarResultName() { return vinegarResultName; }
    public List<String> getVinegarResultLore() { return Collections.unmodifiableList(vinegarResultLore); }
    public List<Material> getVinegarIngredients() { return Collections.unmodifiableList(vinegarIngredients); }

    // --- GUI title getters ---

    public String getGuiTitleInspection() { return guiTitleInspection; }
    public String getGuiTitleNutrition() { return guiTitleNutrition; }
    public String getGuiTitleCookbook() { return guiTitleCookbook; }
    public String getGuiTitleMultiblockInspection() { return guiTitleMultiblockInspection; }

    // --- Machine resource getters ---

    public List<Material> getSmokehouseFuelMaterials() { return Collections.unmodifiableList(smokehouseFuelMaterials); }
    public Material getSaltMaterial() { return saltMaterial; }
    public int getSaltRequired() { return saltRequired; }
    public Material getPicklingWaterMaterial() { return picklingWaterMaterial; }
    public Material getPicklingWaterReturn() { return picklingWaterReturn; }
    public Material getPicklingVinegarMaterial() { return picklingVinegarMaterial; }
    public List<Material> getPicklingFuelMaterials() { return Collections.unmodifiableList(picklingFuelMaterials); }
    public Material getWaxMaterial() { return waxMaterial; }

    // --- Nutrition food group config getters ---

    public Map<String, String> getNutritionGroupDisplayNames() { return Collections.unmodifiableMap(nutritionGroupDisplayNames); }
    public Map<String, String> getNutritionGroupColors() { return Collections.unmodifiableMap(nutritionGroupColors); }
    public Map<String, Material> getNutritionGroupIcons() { return Collections.unmodifiableMap(nutritionGroupIcons); }
    public Map<Material, Set<String>> getNutritionFoodGroupMap() { return Collections.unmodifiableMap(nutritionFoodGroupMap); }
    public Set<String> getNutritionFoodGroups(Material food) {
        return nutritionFoodGroupMap.getOrDefault(food, Set.of());
    }

    private Attribute parseAttribute(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return Attribute.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            if (!normalized.startsWith("GENERIC_")) {
                try {
                    return Attribute.valueOf("GENERIC_" + normalized);
                } catch (IllegalArgumentException ignoredAgain) {
                    return null;
                }
            }
            return null;
        }
    }

    // --- Generic message getter ---

    /**
     * Returns a pre-colorized message from messages.yml by key.
     * Falls back to a visible missing-key placeholder.
     */
    public String msg(String key) {
        return messages.getOrDefault(key, "§c[Missing: " + key + "]");
    }

    // --- Helper methods ---

    private static Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try { return Material.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return fallback; }
    }

    private static List<Material> loadMaterialList(List<String> names, List<Material> fallback) {
        if (names == null || names.isEmpty()) return new ArrayList<>(fallback);
        List<Material> result = new ArrayList<>();
        for (String s : names) {
            Material m = parseMaterial(s, null);
            if (m != null) result.add(m);
        }
        return result.isEmpty() ? new ArrayList<>(fallback) : result;
    }

    // --- Recipe save/delete ---

    /**
     * Saves a recipe to recipes.yml and reloads the recipe cache.
     */
    public void saveRecipe(MultiblockRecipe recipe) {
        try {
            RecipeConfigurationStore.saveRecipe(getRecipesFile(), recipe);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save recipes.yml", e);
        }

        reloadRecipes();
    }

    /**
     * Deletes a recipe from recipes.yml and reloads the recipe cache.
     */
    public void deleteRecipe(MultiblockType machineType, String recipeId) {
        try {
            RecipeConfigurationStore.deleteRecipe(getRecipesFile(), machineType, recipeId);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save recipes.yml", e);
        }

        reloadRecipes();
    }

    /**
     * Reloads only the recipe portion from recipes.yml.
     */
    public void reloadRecipes() {
        loadRecipesFromFile();
    }

    private File getRecipesFile() {
        return new File(plugin.getDataFolder(), "recipes.yml");
    }
}

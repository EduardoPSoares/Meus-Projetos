package com.midgard.fooddecay;

import com.midgard.core.MidgardCore;
import com.midgard.fooddecay.multiblock.MMOCoreStatHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * TFC-style nutrition system with 5 food groups.
 * Eating varied foods from different groups increases max health.
 * Nutrition decays over time, encouraging continuous varied eating.
 */
public class NutritionManager {

    public enum FoodGroup {
        GRAIN("Grãos", "&e"),
        FRUIT("Frutas", "&a"),
        VEGETABLE("Vegetais", "&2"),
        PROTEIN("Proteína", "&c"),
        DAIRY("Laticínios", "&f");

        private final String displayName;
        private final String color;

        FoodGroup(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }

    private final FoodDecayConfig config;
    private final NamespacedKey nutritionKey;
    private final NamespacedKey nutritionGroupsKey;
    private final Map<Attribute, NamespacedKey> nutritionAttributeKeys = new HashMap<>();
    private final Map<UUID, double[]> playerNutrition = new ConcurrentHashMap<>();
    private final Map<Material, Set<FoodGroup>> foodGroupMap = new EnumMap<>(Material.class);
    private int taskId = -1;
    private static final String NUTRITION_MMOCORE_PREFIX = "fooddecayNutrition:";

    public NutritionManager(FoodDecayConfig config) {
        this.config = config;
        this.nutritionKey = new NamespacedKey(MidgardCore.getInstance(), "nutrition_bonus");
        this.nutritionGroupsKey = new NamespacedKey(MidgardCore.getInstance(), "nutrition_groups");
        initFoodGroups();
        loadData();
    }

    /**
     * Returns the configurable display name for a food group.
     */
    public String getGroupDisplayName(FoodGroup group) {
        return config.getNutritionGroupDisplayNames().getOrDefault(group.name(), group.getDisplayName());
    }

    /**
     * Returns the configurable color code for a food group.
     */
    public String getGroupColor(FoodGroup group) {
        return config.getNutritionGroupColors().getOrDefault(group.name(), group.getColor());
    }

    /**
     * Returns the configurable icon material for a food group.
     */
    public Material getGroupIcon(FoodGroup group) {
        return config.getNutritionGroupIcons().getOrDefault(group.name(), Material.STONE);
    }

    public void reloadFoodGroups() {
        initFoodGroups();
    }

    private void initFoodGroups() {
        foodGroupMap.clear();
        Map<Material, Set<String>> configMap = config.getNutritionFoodGroupMap();
        if (!configMap.isEmpty()) {
            for (Map.Entry<Material, Set<String>> entry : configMap.entrySet()) {
                Set<FoodGroup> groups = EnumSet.noneOf(FoodGroup.class);
                for (String name : entry.getValue()) {
                    try { groups.add(FoodGroup.valueOf(name)); }
                    catch (IllegalArgumentException ignored) { /* invalid group name — skip */ }
                }
                if (!groups.isEmpty()) foodGroupMap.put(entry.getKey(), groups);
            }
            return;
        }
        initDefaultFoodGroups();
    }

    private void initDefaultFoodGroups() {
        mapFood(FoodGroup.GRAIN, Material.BREAD, Material.COOKIE, Material.CAKE,
                Material.PUMPKIN_PIE, Material.BAKED_POTATO);

        mapFood(FoodGroup.FRUIT, Material.APPLE, Material.MELON_SLICE,
                Material.SWEET_BERRIES, Material.GLOW_BERRIES, Material.CHORUS_FRUIT);

        mapFood(FoodGroup.VEGETABLE, Material.CARROT, Material.POTATO,
                Material.BEETROOT, Material.DRIED_KELP, Material.GOLDEN_CARROT);

        mapFood(FoodGroup.PROTEIN, Material.COOKED_BEEF, Material.COOKED_PORKCHOP,
                Material.COOKED_CHICKEN, Material.COOKED_MUTTON, Material.COOKED_COD,
                Material.COOKED_SALMON, Material.COOKED_RABBIT, Material.BEEF,
                Material.PORKCHOP, Material.CHICKEN, Material.MUTTON,
                Material.COD, Material.SALMON, Material.RABBIT);

        mapFood(FoodGroup.DAIRY, Material.MUSHROOM_STEW, Material.RABBIT_STEW,
                Material.BEETROOT_SOUP, Material.SUSPICIOUS_STEW, Material.MILK_BUCKET);
    }

    private void mapFood(FoodGroup group, Material... materials) {
        for (Material mat : materials) {
            foodGroupMap.computeIfAbsent(mat, k -> EnumSet.noneOf(FoodGroup.class)).add(group);
        }
    }

    /**
     * Called when a player eats food. Adds nutrition for the food's group(s).
     */
    public void onEat(Player player, Material food) {
        onEat(player, new ItemStack(food), 1);
    }

    /**
     * Called when a player eats food. Reads the actual food component from the item.
     */
    public void onEat(Player player, ItemStack food) {
        onEat(player, food, 1);
    }

    /**
     * Called when a player eats food, optionally dividing the gain by the item's total portions.
     */
    public void onEat(Player player, ItemStack food, int totalPortions) {
        if (!config.isNutritionEnabled()) return;

        if (food == null || food.getType().isAir()) return;

        Set<FoodGroup> groups = getFoodGroups(food);
        if (groups == null || groups.isEmpty()) return;

        double[] nutrition = playerNutrition.computeIfAbsent(
                player.getUniqueId(), k -> new double[FoodGroup.values().length]);

        FoodProfile profile = resolveFoodProfile(food);
        double gain = NutritionGainCalculator.calculate(
                config.getNutritionGainPerFood(),
                profile.nutrition(),
                profile.saturation(),
                totalPortions);
        for (FoodGroup group : groups) {
            nutrition[group.ordinal()] = Math.min(100.0, nutrition[group.ordinal()] + gain);
        }

        updateHealthBonus(player);
    }

    private FoodProfile resolveFoodProfile(ItemStack food) {
        FoodComponent component = resolveFoodComponent(food);
        if (component != null) {
            return new FoodProfile(component.getNutrition(), component.getSaturation());
        }

        return new FoodProfile(4, 2.4f);
    }

    private FoodComponent resolveFoodComponent(ItemStack food) {
        if (food == null || food.getType().isAir() || !food.getType().isEdible()) {
            return null;
        }

        ItemMeta meta = food.getItemMeta();
        if (meta != null && meta.hasFood()) {
            return meta.getFood();
        }

        ItemMeta defaultMeta = new ItemStack(food.getType()).getItemMeta();
        if (defaultMeta != null && defaultMeta.hasFood()) {
            return defaultMeta.getFood();
        }

        return null;
    }

    private record FoodProfile(int nutrition, float saturation) {}

    /**
     * Gets the nutrition values for a player (array indexed by FoodGroup.ordinal()).
     */
    public double[] getNutrition(Player player) {
        return playerNutrition.getOrDefault(
                player.getUniqueId(), new double[FoodGroup.values().length]);
    }

    /**
     * Resets all nutrition for a player (e.g., on death).
     */
    public void resetNutrition(Player player) {
        playerNutrition.remove(player.getUniqueId());
        removeHealthBonus(player);
    }

    private void updateHealthBonus(Player player) {
        NutritionRewardResolver.ResolvedRewards rewards = NutritionRewardResolver.resolve(
                getNutrition(player),
                config.getNutritionActivationThreshold(),
                config.getNutritionHealthBonusPerGroup(),
                config.getNutritionGroupBonuses()
        );

        applyAttributeBonuses(player, rewards);
        applyPotionEffects(player, rewards.effects());
        applyMmocoreBonuses(player, rewards.mmocoreStats());
    }

    private PotionEffect parseEffect(String effectStr) {
        String[] parts = effectStr.split(":");
        if (parts.length < 3) return null;
        PotionEffectType type = PotionEffectType.getByName(parts[0].trim());
        if (type == null) return null;
        try {
            int amplifier = Integer.parseInt(parts[1].trim());
            int duration = Integer.parseInt(parts[2].trim());
            return new PotionEffect(type, duration, amplifier, true, false, true);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void removeHealthBonus(Player player) {
        clearAttributeBonuses(player);
        clearMmocoreBonuses(player);
    }

    private void applyAttributeBonuses(Player player, NutritionRewardResolver.ResolvedRewards rewards) {
        Map<Attribute, Double> combined = new LinkedHashMap<>();
        if (Math.abs(rewards.healthBonus()) > 0.000001D) {
            combined.put(Attribute.MAX_HEALTH, rewards.healthBonus());
        }
        rewards.attributeBonuses().forEach((attribute, amount) ->
                combined.merge(attribute, amount, Double::sum));

        clearAttributeBonuses(player);

        for (Map.Entry<Attribute, Double> entry : combined.entrySet()) {
            double amount = entry.getValue();
            if (Math.abs(amount) < 0.000001D) {
                continue;
            }

            AttributeInstance attribute = player.getAttribute(entry.getKey());
            if (attribute == null) {
                continue;
            }

            attribute.addModifier(new AttributeModifier(
                    getNutritionAttributeKey(entry.getKey()),
                    amount,
                    AttributeModifier.Operation.ADD_NUMBER
            ));
        }
    }

    private void clearAttributeBonuses(Player player) {
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attribute.MAX_HEALTH);
        for (FoodDecayConfig.GroupBonus bonus : config.getNutritionGroupBonuses().values()) {
            attributes.addAll(bonus.attributes().keySet());
        }

        for (Attribute attribute : attributes) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }

            NamespacedKey key = getNutritionAttributeKey(attribute);
            for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
                if (modifier.getKey().equals(key)) {
                    instance.removeModifier(modifier);
                }
            }
        }
    }

    private NamespacedKey getNutritionAttributeKey(Attribute attribute) {
        if (attribute == Attribute.MAX_HEALTH) {
            return nutritionKey;
        }

        return nutritionAttributeKeys.computeIfAbsent(attribute, key ->
                new NamespacedKey(MidgardCore.getInstance(),
                        "nutrition_bonus_" + key.name().toLowerCase(Locale.ROOT)));
    }

    private void applyPotionEffects(Player player, List<String> effectStrings) {
        for (String effectStr : effectStrings) {
            PotionEffect effect = parseEffect(effectStr);
            if (effect != null) {
                player.addPotionEffect(effect);
            }
        }
    }

    private void applyMmocoreBonuses(Player player, Map<String, Double> statBonuses) {
        clearMmocoreBonuses(player);
        if (statBonuses.isEmpty()) {
            return;
        }

        MMOCoreStatHook.applyBonuses(player, statBonuses, NUTRITION_MMOCORE_PREFIX);
    }

    private void clearMmocoreBonuses(Player player) {
        MMOCoreStatHook.clearBonuses(
                player,
                config.getNutritionConfiguredMmocoreStats(),
                NUTRITION_MMOCORE_PREFIX
        );
    }

    /**
     * Starts the periodic nutrition decay task.
     */
    public void startDecayTask() {
        if (!config.isNutritionEnabled()) return;
        taskId = Bukkit.getScheduler().runTaskTimer(
                MidgardCore.getInstance(),
                this::decayAllNutrition,
                1200L, 1200L
        ).getTaskId();
    }

    public void stopDecayTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        saveData();
    }

    private void decayAllNutrition() {
        double decayRate = config.getNutritionDecayPerMinute();

        Iterator<Map.Entry<UUID, double[]>> it = playerNutrition.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, double[]> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());

            // Skip offline players — preserve their data for when they return
            if (player == null || !player.isOnline()) {
                continue;
            }

            double[] nutrition = entry.getValue();
            boolean anyActive = false;
            for (int i = 0; i < nutrition.length; i++) {
                if (nutrition[i] > 0) {
                    nutrition[i] = Math.max(0, nutrition[i] - decayRate);
                    if (nutrition[i] > 0) anyActive = true;
                }
            }

            updateHealthBonus(player);

            if (!anyActive) {
                it.remove();
                removeHealthBonus(player);
            }
        }
        saveData();
    }

    /**
     * Gets the food groups for a given material.
     */
    public Set<FoodGroup> getFoodGroups(Material material) {
        return foodGroupMap.getOrDefault(material, EnumSet.noneOf(FoodGroup.class));
    }

    /**
     * Gets the effective food groups for an item, preferring a custom item override when present.
     */
    public Set<FoodGroup> getFoodGroups(ItemStack item) {
        Set<FoodGroup> customGroups = getStoredFoodGroups(item);
        if (!customGroups.isEmpty()) {
            return customGroups;
        }
        if (item == null || item.getType().isAir()) {
            return EnumSet.noneOf(FoodGroup.class);
        }
        return getFoodGroups(item.getType());
    }

    /**
     * Stores a custom nutrition profile directly on the item.
     */
    public boolean setFoodGroups(ItemStack item, Set<FoodGroup> groups) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (groups == null || groups.isEmpty()) {
            pdc.remove(nutritionGroupsKey);
        } else {
            pdc.set(nutritionGroupsKey, PersistentDataType.STRING, serializeFoodGroups(groups));
        }
        item.setItemMeta(meta);
        return true;
    }

    public Set<FoodGroup> parseFoodGroups(Collection<String> groupNames) {
        Set<FoodGroup> groups = EnumSet.noneOf(FoodGroup.class);
        if (groupNames == null) {
            return groups;
        }

        for (String raw : groupNames) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                groups.add(FoodGroup.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid nutrition group IDs from config/editor input.
            }
        }
        return groups;
    }

    /**
     * Returns the configured materials that belong to the given nutrition group.
     * Useful for GUI hints and player-facing examples.
     */
    public List<Material> getFoodsForGroup(FoodGroup group) {
        List<Material> foods = new ArrayList<>();
        for (Map.Entry<Material, Set<FoodGroup>> entry : foodGroupMap.entrySet()) {
            if (entry.getValue().contains(group)) {
                foods.add(entry.getKey());
            }
        }
        foods.sort(Comparator.comparing(Material::name));
        return foods;
    }

    private Set<FoodGroup> getStoredFoodGroups(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return EnumSet.noneOf(FoodGroup.class);
        }

        String serialized = item.getItemMeta().getPersistentDataContainer()
                .get(nutritionGroupsKey, PersistentDataType.STRING);
        if (serialized == null || serialized.isBlank()) {
            return EnumSet.noneOf(FoodGroup.class);
        }

        return parseFoodGroups(Arrays.asList(serialized.split(",")));
    }

    private String serializeFoodGroups(Set<FoodGroup> groups) {
        return groups.stream()
                .map(FoodGroup::name)
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    // =====================================================
    //  Persistence
    // =====================================================

    private File getDataFile() {
        return new File(FoodDecayPlugin.getInstance().getDataFolder(), "nutrition.yml");
    }

    public void saveData() {
        File file = getDataFile();
        if (playerNutrition.isEmpty()) {
            file.delete();
            return;
        }
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, double[]> entry : playerNutrition.entrySet()) {
            String path = entry.getKey().toString();
            double[] vals = entry.getValue();
            List<Double> list = new ArrayList<>(vals.length);
            for (double v : vals) list.add(v);
            data.set(path, list);
        }
        try {
            file.getParentFile().mkdirs();
            data.save(file);
        } catch (IOException e) {
            MidgardCore.getInstance().getLogger().log(Level.WARNING,
                    "[FoodDecay] Failed to save nutrition data", e);
        }
    }

    private void loadData() {
        File file = getDataFile();
        if (!file.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        for (String key : data.getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(key); }
            catch (IllegalArgumentException ignored) { continue; }
            List<Double> list = data.getDoubleList(key);
            if (list.isEmpty()) continue;
            double[] vals = new double[FoodGroup.values().length];
            for (int i = 0; i < Math.min(vals.length, list.size()); i++) {
                vals[i] = list.get(i);
            }
            playerNutrition.put(uuid, vals);
        }
        MidgardCore.getInstance().getLogger().info(
                "[FoodDecay] Loaded nutrition data for " + playerNutrition.size() + " player(s).");
    }

    /**
     * Restores health bonuses for all online players who have stored nutrition.
     * Called after plugin enable once the server is fully ready.
     */
    public void restoreOnlineBonuses() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerNutrition.containsKey(player.getUniqueId())) {
                updateHealthBonus(player);
            }
        }
    }
}

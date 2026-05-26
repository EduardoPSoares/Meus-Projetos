package com.midgard.fooddecay;

import com.midgard.core.MidgardCore;
import com.midgard.core.module.MidgardModule;
import com.midgard.core.utils.MessageUtils;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.gui.ChatInput;
import com.midgard.fooddecay.multiblock.MultiblockListener;
import com.midgard.fooddecay.multiblock.MultiblockManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * FoodDecay module — TFC-style food expiration system.
 * Features dynamic decay, multiblock preservation structures,
 * temperature, seasons, nutrition, cauldron recipes.
 */
public class FoodDecayModule extends MidgardModule {

    private FoodDecayConfig config;
    private SeasonHook seasonHook;
    private EnvironmentManager environmentManager;
    private FoodDecayManager manager;
    private NutritionManager nutritionManager;
    private CauldronManager cauldronManager;
    private CompostManager compostManager;
    private CookingManager cookingManager;
    private LiquidManager liquidManager;
    private FermentationManager fermentationManager;
    private WeightManager weightManager;
    private MultiblockManager multiblockManager;
    private NamespacedKey vinegarRecipeKey;

    public FoodDecayModule() {
        super("MidgardCooking", "1.0.0");
    }

    @Override
    public void onEnable() {
        this.config = new FoodDecayConfig();
        this.seasonHook = new SeasonHook();
        this.environmentManager = new EnvironmentManager(config, seasonHook);
        this.manager = new FoodDecayManager(this, environmentManager);
        this.nutritionManager = new NutritionManager(config);
        this.cauldronManager = new CauldronManager(config, nutritionManager, manager);
        this.compostManager = new CompostManager(config, manager);
        this.cookingManager = new CookingManager(config, manager);
        this.liquidManager = new LiquidManager(config);
        this.fermentationManager = new FermentationManager(config, liquidManager);
        this.weightManager = new WeightManager(config);
        this.multiblockManager = new MultiblockManager(this);

        registerListener(new FoodDecayListener(this));
        registerListener(new MultiblockListener(multiblockManager));

        MidgardCore.getInstance().getCommandRegistry()
                .registerCommand(new FoodDecayCommand(this));

        manager.startDecayTask();
        nutritionManager.startDecayTask();
        nutritionManager.restoreOnlineBonuses();
        multiblockManager.startTask();
        cookingManager.startTask();
        fermentationManager.startTask();

        if (config.isVinegarRecipeEnabled()) {
            registerVinegarRecipe();
        }

        ChatInput.register();
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.stopDecayTask();
        if (nutritionManager != null) nutritionManager.stopDecayTask();
        if (multiblockManager != null) multiblockManager.stopTask();
        if (cookingManager != null) cookingManager.stopTask();
        if (fermentationManager != null) {
            fermentationManager.stopTask();
            fermentationManager.shutdown();
        }
        if (cauldronManager != null) cauldronManager.clearAll();
        ChatInput.unregister();

        if (vinegarRecipeKey != null) {
            Bukkit.removeRecipe(vinegarRecipeKey);
        }
    }

    private void registerVinegarRecipe() {
        MidgardCore core = MidgardCore.getInstance();
        vinegarRecipeKey = new NamespacedKey(core, "vinegar");

        ItemStack vinegar = new ItemStack(config.getVinegarResultMaterial());
        ItemMeta meta = vinegar.getItemMeta();
        if (meta == null) return;
        meta.displayName(MessageUtils.toComponent(sc(config.getVinegarResultName())));
        List<String> loreLines = config.getVinegarResultLore();
        if (loreLines != null && !loreLines.isEmpty()) {
            meta.lore(loreLines.stream()
                    .map(l -> MessageUtils.toComponent(sc(l)))
                    .toList());
        }
        vinegar.setItemMeta(meta);

        ShapelessRecipe recipe = new ShapelessRecipe(vinegarRecipeKey, vinegar);
        List<Material> ingredients = config.getVinegarIngredients();
        if (ingredients.isEmpty()) {
            recipe.addIngredient(Material.GLASS_BOTTLE);
            recipe.addIngredient(Material.SUGAR);
        } else {
            for (Material ing : ingredients) {
                recipe.addIngredient(ing);
            }
        }

        Bukkit.addRecipe(recipe);
    }

    public FoodDecayConfig getDecayConfig() { return config; }
    public FoodDecayManager getManager() { return manager; }
    public SeasonHook getSeasonHook() { return seasonHook; }
    public EnvironmentManager getEnvironmentManager() { return environmentManager; }
    public NutritionManager getNutritionManager() { return nutritionManager; }
    public CauldronManager getCauldronManager() { return cauldronManager; }
    public CompostManager getCompostManager() { return compostManager; }
    public CookingManager getCookingManager() { return cookingManager; }
    public LiquidManager getLiquidManager() { return liquidManager; }
    public FermentationManager getFermentationManager() { return fermentationManager; }
    public WeightManager getWeightManager() { return weightManager; }
    public MultiblockManager getMultiblockManager() { return multiblockManager; }
}

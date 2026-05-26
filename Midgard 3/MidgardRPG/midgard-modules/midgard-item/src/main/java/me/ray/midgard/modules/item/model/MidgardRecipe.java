package me.ray.midgard.modules.item.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MidgardRecipe {

    private final String id;
    private RecipeType type;
    private Map<Integer, String> ingredients;
    private int outputAmount;
    private boolean hiddenFromBook;
    
    // Shaped Specific
    private boolean shaped; // true by default

    // Furnace/Cooking Specific
    private double experience;
    private int cookTime;

    public MidgardRecipe(RecipeType type) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.ingredients = new HashMap<>();
        this.outputAmount = 1;
        this.shaped = true;
        this.cookTime = 200;
        this.experience = 0.0;
    }

    public MidgardRecipe(String id, RecipeType type) {
        this.id = id;
        this.type = type;
        this.ingredients = new HashMap<>();
        this.outputAmount = 1;
        this.shaped = true;
        this.cookTime = 200;
        this.experience = 0.0;
    }

    public String getId() { return id; }
    public RecipeType getType() { return type; }
    public void setType(RecipeType type) { this.type = type; }

    public Map<Integer, String> getIngredients() { return ingredients; }
    public void setIngredients(Map<Integer, String> ingredients) { this.ingredients = ingredients; }
    public void setIngredient(int slot, String ingredient) {
        if (ingredient == null) {
            ingredients.remove(slot);
        } else {
            ingredients.put(slot, ingredient);
        }
    }

    public int getOutputAmount() { return outputAmount; }
    public void setOutputAmount(int outputAmount) { this.outputAmount = outputAmount; }

    public boolean isHiddenFromBook() { return hiddenFromBook; }
    public void setHiddenFromBook(boolean hiddenFromBook) { this.hiddenFromBook = hiddenFromBook; }

    public boolean isShaped() { return shaped; }
    public void setShaped(boolean shaped) { this.shaped = shaped; }

    public double getExperience() { return experience; }
    public void setExperience(double experience) { this.experience = experience; }

    public int getCookTime() { return cookTime; }
    public void setCookTime(int cookTime) { this.cookTime = cookTime; }

    // Forge-specific fields
    private String forgeRecipeId;       // Links to ForgeRecipe ID in forge system
    private int forgeDifficulty;         // 1-10 difficulty scale
    private int forgeMinLevel;           // Minimum forge profession level
    private String forgeTier;            // Required forge tier (BASIC, ADVANCED, etc.)

    public String getForgeRecipeId() { return forgeRecipeId; }
    public void setForgeRecipeId(String forgeRecipeId) { this.forgeRecipeId = forgeRecipeId; }
    public int getForgeDifficulty() { return forgeDifficulty; }
    public void setForgeDifficulty(int forgeDifficulty) { this.forgeDifficulty = forgeDifficulty; }
    public int getForgeMinLevel() { return forgeMinLevel; }
    public void setForgeMinLevel(int forgeMinLevel) { this.forgeMinLevel = forgeMinLevel; }
    public String getForgeTier() { return forgeTier; }
    public void setForgeTier(String forgeTier) { this.forgeTier = forgeTier; }

    // Smelting-specific fields (Smeltery crafting)
    private Map<String, Integer> smeltingMetals;  // MoltenMetal name -> mb required
    private int smeltingMinTemperature;

    public Map<String, Integer> getSmeltingMetals() { return smeltingMetals != null ? smeltingMetals : new HashMap<>(); }
    public void setSmeltingMetals(Map<String, Integer> metals) { this.smeltingMetals = metals; }
    public void setSmeltingMetal(String metal, int amount) {
        if (smeltingMetals == null) { smeltingMetals = new HashMap<>(); }
        if (amount <= 0) { smeltingMetals.remove(metal); }
        else { smeltingMetals.put(metal, amount); }
    }
    public int getSmeltingMinTemperature() { return smeltingMinTemperature; }
    public void setSmeltingMinTemperature(int temp) { this.smeltingMinTemperature = temp; }

    public enum RecipeType {
        SHAPED,
        SHAPELESS,
        FURNACE,
        BLAST_FURNACE,
        SMOKER,
        CAMPFIRE,
        SMITHING,
        STONE_CUTTING,
        MEGA_SHAPED,   // 5x5
        SUPER_SHAPED,  // Custom
        FORGE,         // Forge crafting system
        SMELTING        // Smeltery crafting system
    }
}

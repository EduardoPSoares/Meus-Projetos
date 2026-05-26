package me.ray.midgard.modules.item.model;

import me.ray.midgard.modules.item.model.MidgardRecipe.RecipeType;
import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gerencia as configurações de crafting de um item.
 * <p>
 * Suporta múltiplas receitas por item (Sistema v2) e mantém compatibilidade
 * com o sistema legado (Sistema v1 - Single Recipe per Type).
 */
public class MidgardItemCrafting {

    private final MidgardItemImpl item;
    private final ConfigurationSection base;
    
    // Cache em memória das receitas
    private final List<MidgardRecipe> recipes = new ArrayList<>();

    public MidgardItemCrafting(MidgardItemImpl item, ConfigurationSection base) {
        this.item = item;
        this.base = base;
        loadRecipes();
    }

    private void loadRecipes() {
        recipes.clear();

        // 1. Carregar Formato Novo (v2) - crafting.recipes.<id>
        if (base.isConfigurationSection("crafting.recipes")) {
            ConfigurationSection recipesSection = base.getConfigurationSection("crafting.recipes");
            for (String key : recipesSection.getKeys(false)) {
                ConfigurationSection recipeSection = recipesSection.getConfigurationSection(key);
                if (recipeSection == null) { continue; }

                String typeStr = recipeSection.getString("type");
                RecipeType type;
                try {
                    type = RecipeType.valueOf(typeStr);
                } catch (IllegalArgumentException | NullPointerException e) {
                    continue;
                }

                MidgardRecipe recipe = new MidgardRecipe(key, type);
                recipe.setOutputAmount(recipeSection.getInt("output", 1));
                recipe.setHiddenFromBook(recipeSection.getBoolean("hide-book", false));
                recipe.setShaped(recipeSection.getBoolean("shaped", true));
                recipe.setExperience(recipeSection.getDouble("experience", 0.0));
                recipe.setCookTime(recipeSection.getInt("duration", 200));

                if (recipeSection.isConfigurationSection("ingredients")) {
                    ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
                    for (String slotKey : ingredientsSection.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotKey);
                            recipe.setIngredient(slot, ingredientsSection.getString(slotKey));
                        } catch (NumberFormatException ignored) { /* Invalid slot key */ }
                    }
                }

                // Forge-specific fields
                if (type == RecipeType.FORGE) {
                    recipe.setForgeDifficulty(recipeSection.getInt("forge-difficulty", 1));
                    recipe.setForgeMinLevel(recipeSection.getInt("forge-min-level", 1));
                    recipe.setForgeTier(recipeSection.getString("forge-tier", "BASIC"));
                    recipe.setForgeRecipeId(recipeSection.getString("forge-recipe-id", null));
                }

                // Smelting-specific fields
                if (type == RecipeType.SMELTING) {
                    recipe.setSmeltingMinTemperature(recipeSection.getInt("smelting-min-temp", 800));
                    if (recipeSection.isConfigurationSection("smelting-metals")) {
                        ConfigurationSection metalsSection = recipeSection.getConfigurationSection("smelting-metals");
                        Map<String, Integer> metals = new HashMap<>();
                        for (String metalKey : metalsSection.getKeys(false)) {
                            metals.put(metalKey, metalsSection.getInt(metalKey));
                        }
                        recipe.setSmeltingMetals(metals);
                    }
                }

                recipes.add(recipe);
            }
        }

        // 2. Carregar Formato Legado (v1) e converter para memória (Migração Implícita)
        // Se já existem receitas v2, não ignoramos v1, mas o ideal é que o save converta tudo.
        // Vamos checar os tipos padrões.
        for (RecipeType type : RecipeType.values()) {
            String path = "crafting." + type.name().toLowerCase();
            
            // Verifica se existe configuração legada habilitada
            if (base.getBoolean(path + ".enabled", false)) {
                // Verifica se já não importamos essa receita (evitar duplicação se o save falhou no meio)
                // Assumimos que se tem ID, é v2. Se estamos lendo do path legado, é v1.
                // Criamos um ID baseado no tipo para ser determinístico na migração
                String legacyId = "legacy_" + type.name().toLowerCase();
                
                // Se já carregamos uma receita com esse ID (migração anterior), ignoramos o path legado
                if (recipes.stream().anyMatch(r -> r.getId().equals(legacyId))) {
                    continue;
                }

                MidgardRecipe legacyRecipe = new MidgardRecipe(legacyId, type);
                legacyRecipe.setOutputAmount(base.getInt(path + ".output", 1));
                legacyRecipe.setHiddenFromBook(base.getBoolean(path + ".hide-book", false));
                legacyRecipe.setShaped(base.getBoolean(path + ".shaped", true));
                legacyRecipe.setExperience(base.getDouble(path + ".experience", 0.0));
                legacyRecipe.setCookTime(base.getInt(path + ".duration", 200));

                if (base.isConfigurationSection(path + ".ingredients")) {
                    ConfigurationSection ingSection = base.getConfigurationSection(path + ".ingredients");
                    for (String key : ingSection.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(key);
                            legacyRecipe.setIngredient(slot, ingSection.getString(key));
                        } catch (NumberFormatException ignored) { /* Invalid slot key */ }
                    }
                }
                
                recipes.add(legacyRecipe);
            }
        }
    }

    private void saveRecipes() {
        // Limpar seção de receitas v2
        base.set("crafting.recipes", null);
        
        // Salvar todas as receitas no formato v2
        for (MidgardRecipe recipe : recipes) {
            String path = "crafting.recipes." + recipe.getId();
            base.set(path + ".type", recipe.getType().name());
            base.set(path + ".output", recipe.getOutputAmount());
            base.set(path + ".hide-book", recipe.isHiddenFromBook());
            
            // Salvar propriedades condicionais para manter o YAML limpo
            if (isFurnaceType(recipe.getType())) {
                base.set(path + ".experience", recipe.getExperience());
                base.set(path + ".duration", recipe.getCookTime());
            }
            
            if (recipe.getType() == RecipeType.SHAPED || recipe.getType() == RecipeType.SUPER_SHAPED || recipe.getType() == RecipeType.MEGA_SHAPED) {
                base.set(path + ".shaped", recipe.isShaped());
            }

            // Forge-specific fields
            if (recipe.getType() == RecipeType.FORGE) {
                base.set(path + ".forge-difficulty", recipe.getForgeDifficulty());
                base.set(path + ".forge-min-level", recipe.getForgeMinLevel());
                if (recipe.getForgeTier() != null) {
                    base.set(path + ".forge-tier", recipe.getForgeTier());
                }
                if (recipe.getForgeRecipeId() != null) {
                    base.set(path + ".forge-recipe-id", recipe.getForgeRecipeId());
                }
            }

            // Smelting-specific fields
            if (recipe.getType() == RecipeType.SMELTING) {
                base.set(path + ".smelting-min-temp", recipe.getSmeltingMinTemperature());
                base.set(path + ".smelting-metals", null);
                for (Map.Entry<String, Integer> metalEntry : recipe.getSmeltingMetals().entrySet()) {
                    base.set(path + ".smelting-metals." + metalEntry.getKey(), metalEntry.getValue());
                }
            }

            // Ingredientes
            base.set(path + ".ingredients", null); // Limpar
            for (Map.Entry<Integer, String> entry : recipe.getIngredients().entrySet()) {
                base.set(path + ".ingredients." + entry.getKey(), entry.getValue());
            }
            
            // Limpar entrada legada se existir (Migração completa)
            String legacyPath = "crafting." + recipe.getType().name().toLowerCase();
            if (base.contains(legacyPath)) {
                base.set(legacyPath, null);
            }
        }
        
        // Habilitar sistema global se houver receitas
        base.set("crafting.enabled", !recipes.isEmpty());
        
        item.save();
    }
    
    private boolean isFurnaceType(RecipeType type) {
        return type == RecipeType.FURNACE || type == RecipeType.BLAST_FURNACE || type == RecipeType.SMOKER || type == RecipeType.CAMPFIRE;
    }

    // --- API v2 (Multi-Recipe) ---

    public List<MidgardRecipe> getRecipes() {
        return new ArrayList<>(recipes);
    }

    public void addRecipe(MidgardRecipe recipe) {
        recipes.add(recipe);
        saveRecipes();
    }

    public void updateRecipe(MidgardRecipe recipe) {
        // Recipe object is likely already in the list if obtained via getRecipe
        // But if it's a copy or new instance with same ID, we might need to replace
        // For now assuming reference manipulation, so just save
        saveRecipes();
    }

    public void removeRecipe(String recipeId) {
        recipes.removeIf(r -> r.getId().equals(recipeId));
        saveRecipes();
    }

    public MidgardRecipe getRecipe(String recipeId) {
        return recipes.stream().filter(r -> r.getId().equals(recipeId)).findFirst().orElse(null);
    }
    
    // --- API v1 (Compatibilidade Legada) ---
    // Estes métodos operam na PRIMEIRA receita encontrada do tipo especificado.
    // Se não existir, criam uma nova receita.

    private MidgardRecipe getOrCreateFirst(String typeStr) {
        try {
            RecipeType type = RecipeType.valueOf(typeStr.toUpperCase());
            return recipes.stream()
                    .filter(r -> r.getType() == type)
                    .findFirst()
                    .orElseGet(() -> {
                        MidgardRecipe newRecipe = new MidgardRecipe(type);
                        recipes.add(newRecipe);
                        return newRecipe;
                    });
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    private MidgardRecipe getFirst(String typeStr) {
        try {
            RecipeType type = RecipeType.valueOf(typeStr.toUpperCase());
            return recipes.stream()
                    .filter(r -> r.getType() == type)
                    .findFirst()
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isCraftingEnabled() { return !recipes.isEmpty(); }
    public void setCraftingEnabled(boolean val) { 
        if (!val) {
            recipes.clear();
            saveRecipes();
        }
        // Se true, não faz nada pois precisa adicionar receita específica
    }

    public boolean isCraftingEnabled(String type) { 
        return getFirst(type) != null; 
    }
    
    public void setCraftingEnabled(String type, boolean val) { 
        if (val) {
            getOrCreateFirst(type); // Cria se não existir
            saveRecipes();
        } else {
            // Remover todas as receitas desse tipo (comportamento legado era binário por tipo)
            try {
                RecipeType rType = RecipeType.valueOf(type.toUpperCase());
                recipes.removeIf(r -> r.getType() == rType);
                saveRecipes();
            } catch (IllegalArgumentException ignored) { /* Invalid recipe type */ }
        }
    }

    public boolean isCraftingShaped() { 
        MidgardRecipe r = getFirst("SHAPED");
        return r != null && r.isShaped(); 
    }
    
    public void setCraftingShaped(boolean val) { 
        MidgardRecipe r = getOrCreateFirst("SHAPED");
        if (r != null) {
            r.setShaped(val);
            saveRecipes();
        }
    }

    public boolean isCraftingShaped(String type) { 
        MidgardRecipe r = getFirst(type);
        return r != null && r.isShaped();
    }
    
    public void setCraftingShaped(String type, boolean val) {
        MidgardRecipe r = getOrCreateFirst(type);
        if (r != null) {
            r.setShaped(val);
            saveRecipes();
        }
    }

    public int getCraftingOutputAmount() { 
        MidgardRecipe r = getFirst("SHAPED");
        return r != null ? r.getOutputAmount() : 1;
    }
    
    public void setCraftingOutputAmount(int val) {
        MidgardRecipe r = getOrCreateFirst("SHAPED");
        if (r != null) {
            r.setOutputAmount(val);
            saveRecipes();
        }
    }

    public int getCraftingOutputAmount(String type) {
        MidgardRecipe r = getFirst(type);
        return r != null ? r.getOutputAmount() : 1;
    }
    
    public void setCraftingOutputAmount(String type, int val) {
        MidgardRecipe r = getOrCreateFirst(type);
        if (r != null) {
            r.setOutputAmount(val);
            saveRecipes();
        }
    }

    public Map<Integer, String> getCraftingIngredients() {
        MidgardRecipe r = getFirst("SHAPED");
        return r != null ? r.getIngredients() : new HashMap<>();
    }

    public Map<Integer, String> getCraftingIngredients(String type) {
        MidgardRecipe r = getFirst(type);
        return r != null ? r.getIngredients() : new HashMap<>();
    }

    public void setCraftingIngredient(int slot, String ingredient) {
        MidgardRecipe r = getOrCreateFirst("SHAPED");
        if (r != null) {
            r.setIngredient(slot, ingredient);
            saveRecipes();
        }
    }

    public void setCraftingIngredient(String type, int slot, String ingredient) {
        MidgardRecipe r = getOrCreateFirst(type);
        if (r != null) {
            r.setIngredient(slot, ingredient);
            saveRecipes();
        }
    }

    public double getCraftingExperience(String type) {
        MidgardRecipe r = getFirst(type);
        return r != null ? r.getExperience() : 0.0;
    }
    
    public void setCraftingExperience(String type, double val) {
        MidgardRecipe r = getOrCreateFirst(type);
        if (r != null) {
            r.setExperience(val);
            saveRecipes();
        }
    }

    public int getCraftingDuration(String type) {
        MidgardRecipe r = getFirst(type);
        return r != null ? r.getCookTime() : 200;
    }
    
    public void setCraftingDuration(String type, int val) {
        MidgardRecipe r = getOrCreateFirst(type);
        if (r != null) {
            r.setCookTime(val);
            saveRecipes();
        }
    }

    public boolean isCraftingHiddenFromBook(String type) {
        MidgardRecipe r = getFirst(type);
        return r != null && r.isHiddenFromBook();
    }
    
    public void setCraftingHiddenFromBook(String type, boolean val) {
        MidgardRecipe r = getOrCreateFirst(type);
        if (r != null) {
            r.setHiddenFromBook(val);
            saveRecipes();
        }
    }
}

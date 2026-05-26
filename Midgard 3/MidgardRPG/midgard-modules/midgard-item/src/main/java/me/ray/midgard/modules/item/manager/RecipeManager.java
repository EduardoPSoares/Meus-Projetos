package me.ray.midgard.modules.item.manager;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.model.MidgardRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.bukkit.inventory.RecipeChoice.ExactChoice;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RecipeManager {

    private final ItemModule module;

    public RecipeManager(ItemModule module) {
        this.module = module;
    }

    public void reload() {
        // Collect recipe keys to remove (iterator.remove() is not supported on all server implementations)
        java.util.List<NamespacedKey> toRemove = new java.util.ArrayList<>();
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe instanceof Keyed) {
                NamespacedKey key = ((Keyed) recipe).getKey();
                if (key.getNamespace().equalsIgnoreCase("midgard_item")) {
                    toRemove.add(key);
                }
            }
        }

        for (NamespacedKey key : toRemove) {
            Bukkit.removeRecipe(key);
        }

        registerRecipes();
    }

    public void registerRecipes() {
        int count = 0;
        for (String itemId : module.getItemManager().getItemIds()) {
            MidgardItem item = module.getItemManager().getItem(itemId);
            if (item == null) { continue; }

            if (registerItemRecipes(item)) {
                count++;
            }
        }
        MidgardLogger.info("Registradas receitas para " + count + " itens.");
    }

    public void updateItemRecipes(MidgardItem item) {
        unregisterItemRecipes(item);
        if (registerItemRecipes(item)) {
            MidgardLogger.info("Receitas atualizadas para o item: " + item.getId());
        }
    }

    private void unregisterItemRecipes(MidgardItem item) {
        String[] types = {"shaped", "shapeless", "furnace", "smoker", "blast_furnace", "campfire", "smithing"};
        for (String type : types) {
            NamespacedKey key = getKey(item, type);
            Bukkit.removeRecipe(key);
        }
    }

    private boolean registerItemRecipes(MidgardItem item) {
        // Removed global check to allow specific crafting types to work even if global 'enabled' is false (or not set)
        // if (!item.isCraftingEnabled()) return false;

        boolean registered = false;

        // Shaped
        if (hasRecipeType(item, "SHAPED")) {
            if (registerShapedRecipe(item)) {
                registered = true;
            }
        }

        // Shapeless
        if (hasRecipeType(item, "SHAPELESS")) {
            if (registerShapelessRecipe(item)) {
                registered = true;
            }
        }

        // Furnace
        if (hasRecipeType(item, "FURNACE")) {
            if (registerCookingRecipe(item, "FURNACE", FurnaceRecipe.class)) {
                registered = true;
            }
        }

        // Smoker
        if (hasRecipeType(item, "SMOKER")) {
            if (registerCookingRecipe(item, "SMOKER", SmokingRecipe.class)) {
                registered = true;
            }
        }

        // Blast Furnace
        if (hasRecipeType(item, "BLAST_FURNACE")) {
            if (registerCookingRecipe(item, "BLAST_FURNACE", BlastingRecipe.class)) {
                registered = true;
            }
        }

        // Campfire
        if (hasRecipeType(item, "CAMPFIRE")) {
            if (registerCookingRecipe(item, "CAMPFIRE", CampfireRecipe.class)) {
                registered = true;
            }
        }

        // Smithing
        if (hasRecipeType(item, "SMITHING")) {
            if (registerSmithingRecipe(item)) {
                registered = true;
            }
        }

        return registered;
    }

    private boolean hasRecipeType(MidgardItem item, String type) {
        return item.getRecipes().stream().anyMatch(recipe -> recipe.getType().name().equalsIgnoreCase(type));
    }

    private NamespacedKey getKey(MidgardItem item, String suffix) {
        // Usar "midgard_item" como namespace para facilitar a identificação e remoção
        // Formato: midgard_item:item_id_type
        return new NamespacedKey("midgard_item", item.getId().toLowerCase() + "_" + suffix.toLowerCase());
    }

    private RecipeChoice getIngredient(String ingredientStr) {
        if (ingredientStr == null || ingredientStr.isEmpty()) { return null; }

        // Strip midgard: namespace prefix for custom item lookups
        String lookup = ingredientStr;
        if (lookup.toLowerCase().startsWith("midgard:")) {
            lookup = lookup.substring("midgard:".length());
        }

        // Tentar pegar como item do Midgard primeiro
        MidgardItem mItem = module.getItemManager().getItem(lookup);
        if (mItem != null) {
            return new ExactChoice(mItem.build());
        }

        // Tentar como Material Vanilla
        try {
            Material mat = Material.valueOf(ingredientStr.toUpperCase());
            return new MaterialChoice(mat);
        } catch (IllegalArgumentException e) {
            MidgardLogger.warn("Ingrediente inválido na receita: " + ingredientStr);
            return null;
        }
    }

    private ItemStack getStackFromChoice(RecipeChoice choice) {
        if (choice instanceof ExactChoice) {
            List<ItemStack> stacks = ((ExactChoice) choice).getChoices();
            return stacks.isEmpty() ? new ItemStack(Material.AIR) : stacks.get(0);
        } else if (choice instanceof MaterialChoice) {
            List<Material> materials = ((MaterialChoice) choice).getChoices();
            return materials.isEmpty() ? new ItemStack(Material.AIR) : new ItemStack(materials.get(0));
        }
        return new ItemStack(Material.AIR);
    }

    private boolean registerShapedRecipe(MidgardItem item) {
        MidgardRecipe recipeData = getRecipeByType(item, "SHAPED");
        if (recipeData == null) { return false; }

        Map<Integer, String> ingredients = recipeData.getIngredients();
        if (ingredients.isEmpty()) { return false; }

        NamespacedKey key = getKey(item, "shaped");
        ItemStack result = item.build();
        result.setAmount(recipeData.getOutputAmount());

        ShapedRecipe recipe = new ShapedRecipe(key, result);

        // Mapear slots (1-9) para shape (ABC, DEF, GHI)
        // Slot 1 -> A, 2 -> B, 3 -> C
        // Slot 4 -> D, 5 -> E, 6 -> F
        // Slot 7 -> G, 8 -> H, 9 -> I
        
        // Determinar shape dinâmico ou fixo 3x3? Vamos usar fixo 3x3 por simplicidade.
        recipe.shape("ABC", "DEF", "GHI");

        char[] chars = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
        boolean hasIngredient = false;

        for (int i = 0; i < 9; i++) {
            // RecipeConfigurationGui usa slots 1-9 na lógica de mapa, mas vamos verificar
            // CraftingEditionGui usa 0-8 no array gridSlots.
            // RecipeConfigurationGui usa slotMap 30->1...
            // O modelo MidgardItemCrafting armazena o que foi passado.
            // Assumindo que a GUI passa 1-9 ou 0-8?
            // CraftingEditionGui (antigo) passava 0-8.
            // Editores novos (ShapedRecipeEditorGui) mapeiam 1-9.
            // Vamos assumir 1-9 para SHAPED baseado no ShapedRecipeEditorGui.
            
            String ing = ingredients.get(i + 1); // 1-based index from ShapedRecipeEditorGui
            if (ing != null) {
                RecipeChoice choice = getIngredient(ing);
                if (choice != null) {
                    recipe.setIngredient(chars[i], choice);
                    hasIngredient = true;
                }
            }
        }

        if (!hasIngredient) { return false; }

        try {
            Bukkit.addRecipe(recipe);
            return true;
        } catch (Exception e) {
            MidgardLogger.error("Falha ao registrar receita SHAPED para " + item.getId(), e);
            return false;
        }
    }

    private boolean registerShapelessRecipe(MidgardItem item) {
        MidgardRecipe recipeData = getRecipeByType(item, "SHAPELESS");
        if (recipeData == null) { return false; }

        Map<Integer, String> ingredients = recipeData.getIngredients();
        if (ingredients.isEmpty()) { return false; }

        NamespacedKey key = getKey(item, "shapeless");
        ItemStack result = item.build();
        result.setAmount(recipeData.getOutputAmount());

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        boolean hasIngredient = false;

        for (String ing : ingredients.values()) {
            RecipeChoice choice = getIngredient(ing);
            if (choice != null) {
                recipe.addIngredient(choice);
                hasIngredient = true;
            }
        }

        if (!hasIngredient) { return false; }

        try {
            Bukkit.addRecipe(recipe);
            return true;
        } catch (Exception e) {
            MidgardLogger.error("Falha ao registrar receita SHAPELESS para " + item.getId(), e);
            return false;
        }
    }

    private <T extends CookingRecipe<?>> boolean registerCookingRecipe(MidgardItem item, String type, Class<T> recipeClass) {
        MidgardRecipe recipeData = getRecipeByType(item, type);
        if (recipeData == null) { return false; }

        Map<Integer, String> ingredients = recipeData.getIngredients();
        // Furnace/Cooking geralmente usa slot 0 (input)
        String inputStr = ingredients.get(0); 
        if (inputStr == null) { return false; }

        RecipeChoice input = getIngredient(inputStr);
        if (input == null) { return false; }

        NamespacedKey key = getKey(item, type.toLowerCase());
        ItemStack result = item.build();
        result.setAmount(recipeData.getOutputAmount());
        
        float exp = (float) recipeData.getExperience();
        int duration = recipeData.getCookTime();

        try {
            // Construtor: NamespacedKey key, ItemStack result, RecipeChoice input, float experience, int cookingTime
            T recipe = recipeClass.getConstructor(NamespacedKey.class, ItemStack.class, RecipeChoice.class, float.class, int.class)
                    .newInstance(key, result, input, exp, duration);
            
            Bukkit.addRecipe(recipe);
            return true;
        } catch (Exception e) {
            MidgardLogger.error("Falha ao registrar receita " + type + " para " + item.getId(), e);
            return false;
        }
    }

    private boolean registerSmithingRecipe(MidgardItem item) {
        // Smithing recipes (1.20+) requerem Template, Base, Addition
        // MidgardItemCrafting armazena em slots 0, 1, 2?
        // SmithingRecipeEditorGui:
        // Slot 40 (Base) -> index 0
        // Slot 41 (Addition) -> index 1
        // Slot 39 (Template) -> index 2
        
        MidgardRecipe recipeData = getRecipeByType(item, "SMITHING");
        if (recipeData == null) { return false; }

        Map<Integer, String> ingredients = recipeData.getIngredients();
        String baseStr = ingredients.get(0);
        String addStr = ingredients.get(1);
        String templStr = ingredients.get(2);

        if (baseStr == null || addStr == null) { return false; } // Base e Addition são obrigatórios

        RecipeChoice base = getIngredient(baseStr);
        RecipeChoice addition = getIngredient(addStr);
        RecipeChoice template = templStr != null ? getIngredient(templStr) : null;

        if (base == null || addition == null) { return false; }

        NamespacedKey key = getKey(item, "smithing");
        ItemStack result = item.build();
        result.setAmount(recipeData.getOutputAmount());

        // SmithingTransformRecipe (pre-1.20 style) vs SmithingTrimRecipe vs SmithingTransformRecipe (modern)
        // Bukkit 1.20+ usa SmithingTransformRecipe(key, template, base, addition, result)
        // Se template for null, pode falhar em versões novas.
        
        // Para compatibilidade, vamos tentar instanciar SmithingTransformRecipe.
        try {
            
            // Verifica se existe construtor com template (1.20+)
            // public SmithingTransformRecipe(@NotNull NamespacedKey key, @Nullable RecipeChoice template, @NotNull RecipeChoice base, @NotNull RecipeChoice addition, @NotNull ItemStack result)
            
            if (template == null) {
                // Use PAPER as a safe template placeholder (AIR throws on some server implementations)
                template = new MaterialChoice(Material.PAPER);
            }

            // Assumindo Paper 1.20+
            // Usando Reflection para evitar erros de compilação com diferentes versões da API
            try {
                Class<?> clazz = Class.forName("org.bukkit.inventory.SmithingTransformRecipe");
                java.lang.reflect.Constructor<?>[] constructors = clazz.getConstructors();
                
                boolean registeredSmithing = false;
                for (java.lang.reflect.Constructor<?> c : constructors) {
                    if (c.getParameterCount() == 5) {
                        Class<?>[] types = c.getParameterTypes();
                        // Verificar se corresponde a (NamespacedKey, ?, ?, ?, ItemStack)
                        if (types[0] == NamespacedKey.class && types[4] == ItemStack.class) {
                            Object arg2 = types[1] == ItemStack.class ? getStackFromChoice(template) : template;
                            Object arg3 = types[2] == ItemStack.class ? getStackFromChoice(base) : base;
                            Object arg4 = types[3] == ItemStack.class ? getStackFromChoice(addition) : addition;
                            
                            SmithingRecipe recipeObj = (SmithingRecipe) c.newInstance(key, arg2, arg3, arg4, result);
                            Bukkit.addRecipe(recipeObj);
                            registeredSmithing = true;
                            break;
                        }
                    }
                }
                
                if (registeredSmithing) { return true; }
                
                MidgardLogger.error("Não foi possível encontrar um construtor compatível para SmithingTransformRecipe.");
                return false;
            } catch (Exception e) {
                MidgardLogger.error("Erro ao registrar receita Smithing via reflection", e);
                return false;
            }
        } catch (Exception e) {
            // Tentar fallback para versão antiga (sem template) se existir essa classe/construtor
            // SmithingRecipe(NamespacedKey key, ItemStack result, RecipeChoice base, RecipeChoice addition) - Depreciado/Removido
            try {
                // Reflection ou SmithingRecipe antigo
                // Ignorando complexidade de versão por enquanto, assumindo moderno.
                MidgardLogger.error("Falha ao registrar receita SMITHING para " + item.getId() + " (Verifique compatibilidade de versão)", e);
                return false;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    private MidgardRecipe getRecipeByType(MidgardItem item, String type) {
        return item.getRecipes().stream()
                .filter(recipe -> recipe.getType().name().equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove todas as receitas de um item pelo ID (usado pelo sync quando o item é deletado).
     */
    public void removeItemRecipesById(String itemId) {
        String[] types = {"shaped", "shapeless", "furnace", "smoker", "blast_furnace", "campfire", "smithing"};
        for (String type : types) {
            NamespacedKey key = new NamespacedKey("midgard_item", itemId.toLowerCase() + "_" + type);
            Bukkit.removeRecipe(key);
        }
    }
}

package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import java.util.*;

/**
 * Receita de liga (alloy): combina dois ou mais metais fundidos para criar um novo.
 * Ligas se formam automaticamente no tanque quando os componentes estão presentes
 * na proporção correta, como no Tinkers' Construct.
 * Recipes are loaded from config via {@link AlloyRecipeManager}.
 */
public class AlloyRecipe {

    private final String id;
    private final MoltenMetal result;
    private final int resultAmount; // mb produzido
    private final Map<MoltenMetal, Integer> ingredients; // metal → mb necessário
    private final int minSmelteryTemperature;

    public AlloyRecipe(String id, MoltenMetal result, int resultAmount,
                       Map<MoltenMetal, Integer> ingredients, int minTemp) {
        this.id = id;
        this.result = result;
        this.resultAmount = resultAmount;
        this.ingredients = Collections.unmodifiableMap(new LinkedHashMap<>(ingredients));
        this.minSmelteryTemperature = minTemp;
    }

    public String getId() { return id; }
    public MoltenMetal getResult() { return result; }
    public int getResultAmount() { return resultAmount; }
    public Map<MoltenMetal, Integer> getIngredients() { return ingredients; }
    public int getMinSmelteryTemperature() { return minSmelteryTemperature; }

    /**
     * Quantas vezes essa liga pode ser criada com os metais disponíveis no tanque.
     */
    public int getMaxCrafts(Map<MoltenMetal, Integer> tankContents) {
        int max = Integer.MAX_VALUE;
        for (var entry : ingredients.entrySet()) {
            int available = tankContents.getOrDefault(entry.getKey(), 0);
            int canMake = available / entry.getValue();
            max = Math.min(max, canMake);
        }
        return max == Integer.MAX_VALUE ? 0 : max;
    }

    /**
     * Verifica se a liga pode ser formada com os metais disponíveis.
     */
    public boolean canForm(Map<MoltenMetal, Integer> tankContents, int currentTemp) {
        if (currentTemp < minSmelteryTemperature) { return false; }
        return getMaxCrafts(tankContents) > 0;
    }
}

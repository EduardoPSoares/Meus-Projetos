package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import org.bukkit.Material;

/**
 * Receita de fundição: item sólido → metal fundido.
 * Cada receita define qual Material (item) produz qual MoltenMetal.
 * Recipes are loaded from config via {@link SmeltingRecipeManager}.
 */
public class SmeltingRecipe {

    private final String id;
    private final Material input;
    private final MoltenMetal output;
    private final int outputAmount; // em millibuckets (mb)
    private final int smeltTime; // em ticks
    private final int minTemperature; // temperatura mínima da smeltery

    public SmeltingRecipe(String id, Material input, MoltenMetal output,
                          int outputAmount, int smeltTime, int minTemperature) {
        this.id = id;
        this.input = input;
        this.output = output;
        this.outputAmount = outputAmount;
        this.smeltTime = smeltTime;
        this.minTemperature = minTemperature;
    }

    public String getId() { return id; }
    public Material getInput() { return input; }
    public MoltenMetal getOutput() { return output; }
    public int getOutputAmount() { return outputAmount; }
    public int getSmeltTime() { return smeltTime; }
    public int getMinTemperature() { return minTemperature; }
}

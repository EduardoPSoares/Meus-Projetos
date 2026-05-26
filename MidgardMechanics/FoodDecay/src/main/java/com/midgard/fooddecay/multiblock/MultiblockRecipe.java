package com.midgard.fooddecay.multiblock;

import com.midgard.fooddecay.FoodTrait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Represents a configurable recipe for a multiblock machine.
 * Supports vanilla items, MMOItems and ItemsAdder as input/output.
 */
public class MultiblockRecipe {

    private final String id;
    private final MultiblockType machineType;

    private final Material inputMaterial;
    private final String inputMmoType;
    private final String inputMmoId;
    private final String inputItemsAdderId;
    private final int inputCustomModelData;

    private final Material outputMaterial;
    private final String outputMmoType;
    private final String outputMmoId;
    private final String outputItemsAdderId;
    private final String outputName;
    private final List<String> outputLore;
    private final int outputCustomModelData;

    private final int spoiledCustomModelData;
    private final String spoiledName;

    private final int timeMinutes;
    private final FoodTrait trait;
    private final FoodTrait requiresTrait;
    private final String requiresRecipe;
    private final List<RecipeIngredient> extraIngredients;
    private final List<String> nutritionGroups;

    private String profession;
    private int professionLevel;
    private String experienceProfession;
    private double experienceReward;

    public MultiblockRecipe(String id, MultiblockType machineType,
                            Material inputMaterial, String inputMmoType, String inputMmoId,
                            String inputItemsAdderId, int inputCustomModelData,
                            Material outputMaterial, String outputMmoType, String outputMmoId,
                            String outputItemsAdderId,
                            String outputName, List<String> outputLore,
                            int outputCustomModelData,
                            int spoiledCustomModelData, String spoiledName,
                            int timeMinutes, FoodTrait trait,
                            FoodTrait requiresTrait, String requiresRecipe,
                            List<String> nutritionGroups) {
        this(id, machineType,
                inputMaterial, inputMmoType, inputMmoId, inputItemsAdderId, inputCustomModelData,
                outputMaterial, outputMmoType, outputMmoId, outputItemsAdderId,
                outputName, outputLore, outputCustomModelData,
                spoiledCustomModelData, spoiledName,
                timeMinutes, trait, requiresTrait, requiresRecipe,
                List.of(), nutritionGroups);
    }

    public MultiblockRecipe(String id, MultiblockType machineType,
                            Material inputMaterial, String inputMmoType, String inputMmoId,
                            String inputItemsAdderId, int inputCustomModelData,
                            Material outputMaterial, String outputMmoType, String outputMmoId,
                            String outputItemsAdderId,
                            String outputName, List<String> outputLore,
                            int outputCustomModelData,
                            int spoiledCustomModelData, String spoiledName,
                            int timeMinutes, FoodTrait trait,
                            FoodTrait requiresTrait, String requiresRecipe,
                            List<RecipeIngredient> extraIngredients,
                            List<String> nutritionGroups) {
        this.id = id;
        this.machineType = machineType;
        this.inputMaterial = inputMaterial;
        this.inputMmoType = inputMmoType;
        this.inputMmoId = inputMmoId;
        this.inputItemsAdderId = inputItemsAdderId;
        this.inputCustomModelData = inputCustomModelData;
        this.outputMaterial = outputMaterial;
        this.outputMmoType = outputMmoType;
        this.outputMmoId = outputMmoId;
        this.outputItemsAdderId = outputItemsAdderId;
        this.outputName = outputName;
        this.outputLore = outputLore != null ? outputLore : List.of();
        this.outputCustomModelData = outputCustomModelData;
        this.spoiledCustomModelData = spoiledCustomModelData;
        this.spoiledName = spoiledName;
        this.timeMinutes = timeMinutes;
        this.trait = trait;
        this.requiresTrait = requiresTrait;
        this.requiresRecipe = requiresRecipe;
        this.extraIngredients = extraIngredients != null ? List.copyOf(extraIngredients) : List.of();
        this.nutritionGroups = nutritionGroups != null ? List.copyOf(nutritionGroups) : List.of();
    }

    public boolean matchesInput(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        if (inputItemsAdderId != null && !inputItemsAdderId.isBlank()) {
            return ItemsAdderHook.matchesItem(item, inputItemsAdderId);
        }

        if (inputMmoType != null && inputMmoId != null) {
            return MMOItemsHook.matchesItem(item, inputMmoType, inputMmoId);
        }

        if (inputMaterial == null || item.getType() != inputMaterial) {
            return false;
        }

        if (inputCustomModelData > 0) {
            if (!item.hasItemMeta()) {
                return false;
            }
            ItemMeta meta = item.getItemMeta();
            return meta.hasCustomModelData() && meta.getCustomModelData() == inputCustomModelData;
        }

        return true;
    }

    public ItemStack createOutput(ItemStack input) {
        ItemStack output = createBaseOutput(input);
        applyOutputOverrides(output);
        return output;
    }

    public ItemStack createInputPreview() {
        return createPreviewItem(inputMaterial, inputMmoType, inputMmoId, inputItemsAdderId, Material.CHEST);
    }

    public ItemStack createOutputPreview() {
        ItemStack preview = createBaseOutput(null);
        applyOutputOverrides(preview);
        return preview;
    }

    private ItemStack createBaseOutput(ItemStack input) {
        if (outputItemsAdderId != null && !outputItemsAdderId.isBlank()) {
            ItemStack output = ItemsAdderHook.createItem(outputItemsAdderId);
            if (output != null) {
                return output;
            }
        }

        if (outputMmoType != null && outputMmoId != null) {
            ItemStack output = MMOItemsHook.createItem(outputMmoType, outputMmoId);
            if (output != null) {
                return output;
            }
        }

        if (outputMaterial != null) {
            return new ItemStack(outputMaterial);
        }

        if (input != null) {
            ItemStack clone = input.clone();
            clone.setAmount(1);
            return clone;
        }

        return new ItemStack(Material.STONE);
    }

    private static ItemStack createPreviewItem(Material material, String mmoType, String mmoId,
                                               String itemsAdderId, Material fallback) {
        if (itemsAdderId != null && !itemsAdderId.isBlank()) {
            ItemStack preview = ItemsAdderHook.createItem(itemsAdderId);
            if (preview != null) {
                return preview.clone();
            }
        }

        if (mmoType != null && mmoId != null) {
            ItemStack preview = MMOItemsHook.createItem(mmoType, mmoId);
            if (preview != null) {
                return preview.clone();
            }
        }

        if (material != null) {
            return new ItemStack(material);
        }

        return new ItemStack(fallback);
    }

    private void applyOutputOverrides(ItemStack output) {
        ItemMeta meta = output.getItemMeta();
        if (meta == null) {
            return;
        }

        if (outputCustomModelData > 0) {
            meta.setCustomModelData(outputCustomModelData);
        }
        if (outputName != null && !outputName.isEmpty()) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(outputName));
        }
        if (!outputLore.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : outputLore) {
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
            }
            meta.lore(lore);
        }
        output.setItemMeta(meta);
    }

    public String getId() {
        return id;
    }

    public MultiblockType getMachineType() {
        return machineType;
    }

    public int getTimeMinutes() {
        return timeMinutes;
    }

    public FoodTrait getTrait() {
        return trait;
    }

    public int getSpoiledCustomModelData() {
        return spoiledCustomModelData;
    }

    public String getSpoiledName() {
        return spoiledName;
    }

    public int getOutputCustomModelData() {
        return outputCustomModelData;
    }

    public String getOutputName() {
        return outputName;
    }

    public List<String> getOutputLore() {
        return outputLore;
    }

    public Material getInputMaterial() {
        return inputMaterial;
    }

    public String getInputMmoType() {
        return inputMmoType;
    }

    public String getInputMmoId() {
        return inputMmoId;
    }

    public String getInputItemsAdderId() {
        return inputItemsAdderId;
    }

    public int getInputCustomModelData() {
        return inputCustomModelData;
    }

    public Material getOutputMaterial() {
        return outputMaterial;
    }

    public String getOutputMmoType() {
        return outputMmoType;
    }

    public String getOutputMmoId() {
        return outputMmoId;
    }

    public String getOutputItemsAdderId() {
        return outputItemsAdderId;
    }

    public FoodTrait getRequiresTrait() {
        return requiresTrait;
    }

    public String getRequiresRecipe() {
        return requiresRecipe;
    }

    public List<RecipeIngredient> getExtraIngredients() {
        return extraIngredients;
    }

    public List<String> getNutritionGroups() {
        return nutritionGroups;
    }

    public String getProfession() {
        return profession;
    }

    public int getProfessionLevel() {
        return professionLevel;
    }

    public String getExperienceProfession() {
        return experienceProfession;
    }

    public double getExperienceReward() {
        return experienceReward;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public void setProfessionLevel(int professionLevel) {
        this.professionLevel = professionLevel;
    }

    public void setExperienceProfession(String experienceProfession) {
        this.experienceProfession = experienceProfession;
    }

    public void setExperienceReward(double experienceReward) {
        this.experienceReward = experienceReward;
    }

    public String getInputDisplayName() {
        if (inputItemsAdderId != null && !inputItemsAdderId.isBlank()) {
            return ItemsAdderHook.getItemDisplayName(inputItemsAdderId);
        }
        if (inputMmoType != null && inputMmoId != null) {
            return MMOItemsHook.getItemDisplayName(inputMmoType, inputMmoId);
        }
        if (inputMaterial != null) {
            return formatMaterial(inputMaterial.name());
        }
        return "???";
    }

    public String getOutputDisplayName() {
        if (outputName != null && !outputName.isEmpty()) {
            return outputName;
        }
        if (outputItemsAdderId != null && !outputItemsAdderId.isBlank()) {
            return ItemsAdderHook.getItemDisplayName(outputItemsAdderId);
        }
        if (outputMmoType != null && outputMmoId != null) {
            return MMOItemsHook.getItemDisplayName(outputMmoType, outputMmoId);
        }
        if (outputMaterial != null) {
            return formatMaterial(outputMaterial.name());
        }
        return "???";
    }

    public String getInputReferenceLabel() {
        if (inputItemsAdderId != null && !inputItemsAdderId.isBlank()) {
            return ItemsAdderHook.getItemReferenceLabel(inputItemsAdderId);
        }
        if (inputMmoType != null && inputMmoId != null) {
            return MMOItemsHook.getItemReferenceLabel(inputMmoType, inputMmoId);
        }
        return getInputDisplayName();
    }

    public String getOutputReferenceLabel() {
        if (outputItemsAdderId != null && !outputItemsAdderId.isBlank()) {
            String base = ItemsAdderHook.getItemReferenceLabel(outputItemsAdderId);
            if (outputName != null && !outputName.isBlank()) {
                return stripLegacy(outputName) + " (base: " + base + ")";
            }
            return base;
        }
        if (outputMmoType != null && outputMmoId != null) {
            String base = MMOItemsHook.getItemReferenceLabel(outputMmoType, outputMmoId);
            if (outputName != null && !outputName.isBlank()) {
                return stripLegacy(outputName) + " (base: " + base + ")";
            }
            return base;
        }
        return outputName != null && !outputName.isBlank()
                ? stripLegacy(outputName)
                : getOutputDisplayName();
    }

    private static String formatMaterial(String name) {
        String[] parts = name.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.charAt(0))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private static String stripLegacy(String value) {
        return value.replaceAll("(?i)&[0-9A-FK-OR]", "").trim();
    }
}

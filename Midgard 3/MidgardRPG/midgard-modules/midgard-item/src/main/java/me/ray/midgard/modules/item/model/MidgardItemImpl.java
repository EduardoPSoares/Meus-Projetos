package me.ray.midgard.modules.item.model;

import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.repository.ItemRepository;
import me.ray.midgard.modules.item.repository.ItemSyncManager;
import me.ray.midgard.modules.item.utils.StatRange;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Implementação padrão de um item do MidgardRPG.
 * <p>
 * Esta classe encapsula todas as propriedades e comportamentos de um item customizado,
 * delegando responsabilidades específicas para subcomponentes (Crafting, Display, Mechanics, etc.).
 */
public class MidgardItemImpl implements MidgardItem {

    private final String id;
    private ConfigurationSection config;
    private ConfigurationSection base;
    private File file;
    
    private MidgardItemCrafting crafting;
    private MidgardItemUpdaterOptions updaterOptions;
    private MidgardItemGeneral general;
    private MidgardItemDisplay display;
    private MidgardItemDurability durability;
    private MidgardItemRestrictions restrictions;
    private MidgardItemRequirements requirements;
    private MidgardItemMechanics mechanics;

    private String categoryId;

    /**
     * Construtor do MidgardItemImpl.
     *
     * @param id ID único do item.
     * @param config Seção de configuração do item.
     * @param defaultCategory Categoria padrão caso não especificada.
     * @param file Arquivo de origem do item.
     */
    public MidgardItemImpl(String id, ConfigurationSection config, String defaultCategory, File file) {
        this.id = id;
        this.config = config;
        this.file = file;
        
        if (config.isConfigurationSection("base")) {
            this.base = config.getConfigurationSection("base");
        } else {
            this.base = config;
        }

        // Look for 'type' at top level first (GUI-created items),
        // then fall back to 'base.type' (resource YAML items),
        // then fall back to folder-inferred defaultCategory.
        String resolvedCategory = config.getString("type",
                this.base.getString("type", defaultCategory));
        this.categoryId = (resolvedCategory != null ? resolvedCategory : "UNCATEGORIZED").toUpperCase();

        this.crafting = new MidgardItemCrafting(this, base);
        this.updaterOptions = new MidgardItemUpdaterOptions(this, base);
        this.general = new MidgardItemGeneral(this, base);
        this.display = new MidgardItemDisplay(this, base);
        this.durability = new MidgardItemDurability(this, base);
        this.restrictions = new MidgardItemRestrictions(this, base);
        this.requirements = new MidgardItemRequirements(this, base);
        this.mechanics = new MidgardItemMechanics(this, base);
    }

    /**
     * Construtor para itens carregados do banco de dados (sem arquivo local).
     */
    public MidgardItemImpl(String id, ConfigurationSection config, String defaultCategory) {
        this(id, config, defaultCategory, null);
    }

    /**
     * Cria um MidgardItemImpl a partir dos dados do banco de dados.
     *
     * @param id         ID do item
     * @param categoryId Categoria do item
     * @param yamlData   Dados YAML serializados
     * @return instância do item, ou null se falhar
     */
    public static MidgardItemImpl fromDatabase(String id, String categoryId, String yamlData) {
        try {
            YamlConfiguration root = new YamlConfiguration();
            YamlConfiguration temp = new YamlConfiguration();
            temp.loadFromString(yamlData);
            ConfigurationSection section = root.createSection(id);
            for (String key : temp.getKeys(true)) {
                if (!temp.isConfigurationSection(key)) {
                    section.set(key, temp.get(key));
                }
            }
            return new MidgardItemImpl(id, section, categoryId, null);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao carregar item " + id + " do banco de dados", e);
            return null;
        }
    }

    /**
     * Constrói um ItemStack do Bukkit representando este item.
     *
     * @return ItemStack gerado.
     */
    public ItemStack build() {
        return new MidgardItemBuilder(this).build();
    }


    public String getId() { return id; }
    @Override
    public File getFile() { return file; }
    public String getCategoryId() { return categoryId; }

    @Override
    public void setCategoryId(String categoryId) {
        this.categoryId = (categoryId != null ? categoryId : "UNCATEGORIZED").toUpperCase();
        base.set("type", this.categoryId);
        save();
    }
    public Map<ItemStat, StatRange> getStats() { return mechanics.getStats(); }

    public String getNexoId() { return general.getNexoId(); }
    public void setNexoId(String id) { general.setNexoId(id); }

    public String getDisplayName() { return general.getName(); }
    public void setDisplayName(String name) { general.setName(name); }
    public int getRevisionId() { return general.getRevisionId(); }
    public void setRevisionId(int revisionId) { general.setRevisionId(revisionId); }
    public Material getMaterial() { return general.getMaterial(); }
    public void setMaterial(Material material) { general.setMaterial(material); }
    public int getBaseItemDamage() { return mechanics.getBaseItemDamage(); }
    public void setBaseItemDamage(int baseItemDamage) { mechanics.setBaseItemDamage(baseItemDamage); }
    public String getDisplayedType() { return display.getDisplayedType(); }
    public void setDisplayedType(String displayedType) { display.setDisplayedType(displayedType); }
    public String getItemModel() { return display.getItemModel(); }
    public void setItemModel(String itemModel) { display.setItemModel(itemModel); }
    public String getEquippableSlot() { return mechanics.getEquippableSlot(); }
    public void setEquippableSlot(String equippableSlot) { mechanics.setEquippableSlot(equippableSlot); }
    public String getEquippableModel() { return mechanics.getEquippableModel(); }
    public void setEquippableModel(String equippableModel) { mechanics.setEquippableModel(equippableModel); }
    public int getMaxCustomDurability() { return durability.getMaxCustomDurability(); }
    public void setMaxCustomDurability(int val) { durability.setMaxCustomDurability(val); }
    public int getMaxVanillaDurability() { return durability.getMaxVanillaDurability(); }
    public void setMaxVanillaDurability(int val) { durability.setMaxVanillaDurability(val); }
    public boolean isLostWhenBroken() { return durability.isLostWhenBroken(); }
    public void setLostWhenBroken(boolean val) { durability.setLostWhenBroken(val); }
    @Override
    public boolean isUnbreakable() { return durability.isUnbreakable(); }
    public void setUnbreakable(boolean val) { durability.setUnbreakable(val); }
    public String getNbtTags() { return mechanics.getNbtTags(); }
    public void setNbtTags(String val) { mechanics.setNbtTags(val); }
    public String getCustomModelDataStrings() { return display.getCustomModelDataStrings(); }
    public void setCustomModelDataStrings(String val) { display.setCustomModelDataStrings(val); }
    public String getCustomModelDataFloats() { return display.getCustomModelDataFloats(); }
    public void setCustomModelDataFloats(String val) { display.setCustomModelDataFloats(val); }
    public String getLoreFormat() { return display.getLoreFormat(); }
    public void setLoreFormat(String val) { display.setLoreFormat(val); }
    public int getMaxStackSize() { return restrictions.getMaxStackSize(); }
    public void setMaxStackSize(int val) { restrictions.setMaxStackSize(val); }
    public String getCustomTooltip() { return display.getCustomTooltip(); }
    public void setCustomTooltip(String val) { display.setCustomTooltip(val); }
    public String getVanillaTooltipStyle() { return display.getVanillaTooltipStyle(); }
    public void setVanillaTooltipStyle(String val) { display.setVanillaTooltipStyle(val); }
    public String getEnchantments() { return mechanics.getEnchantments(); }
    public void setEnchantments(String val) { mechanics.setEnchantments(val); }
    public boolean isHideEnchantments() { return mechanics.isHideEnchantments(); }
    public void setHideEnchantments(boolean val) { mechanics.setHideEnchantments(val); }
    public boolean isHideTooltip() { return display.isHideTooltip(); }
    public void setHideTooltip(boolean val) { display.setHideTooltip(val); }
    public String getPermission() { return requirements.getPermission(); }
    public void setPermission(String val) { requirements.setPermission(val); }
    public String getItemParticles() { return display.getItemParticles(); }
    public void setItemParticles(String val) { display.setItemParticles(val); }
    public boolean isDisableInteraction() { return restrictions.isDisableInteraction(); }
    public void setDisableInteraction(boolean val) { restrictions.setDisableInteraction(val); }
    public boolean isDisableCrafting() { return restrictions.isDisableCrafting(); }
    public void setDisableCrafting(boolean val) { restrictions.setDisableCrafting(val); }
    public boolean isDisableSmelting() { return restrictions.isDisableSmelting(); }
    public void setDisableSmelting(boolean val) { restrictions.setDisableSmelting(val); }
    public boolean isDisableRepairing() { return restrictions.isDisableRepairing(); }
    public void setDisableRepairing(boolean val) { restrictions.setDisableRepairing(val); }
    public boolean isDisableEnchanting() { return restrictions.isDisableEnchanting(); }
    public void setDisableEnchanting(boolean val) { restrictions.setDisableEnchanting(val); }
    public boolean isDisableSmithing() { return restrictions.isDisableSmithing(); }
    public void setDisableSmithing(boolean val) { restrictions.setDisableSmithing(val); }
    public boolean isDisableItemDropping() { return restrictions.isDisableItemDropping(); }
    public void setDisableItemDropping(boolean val) { restrictions.setDisableItemDropping(val); }
    public List<String> getRequiredClasses() { return requirements.getRequiredClasses(); }
    public void setRequiredClasses(List<String> list) { requirements.setRequiredClasses(list); }
    public int getRequiredLevel() { return requirements.getRequiredLevel(); }
    public void setRequiredLevel(int val) { requirements.setRequiredLevel(val); }
    public int getCustomModelData() { return display.getCustomModelData(); }
    public void setCustomModelData(int val) { display.setCustomModelData(val); }
    public List<String> getLore() { return general.getLore(); }
    public void setLore(List<String> lore) { general.setLore(lore); }
    public List<String> getPermanentEffects() { return mechanics.getPermanentEffects(); }
    public void setPermanentEffects(List<String> list) { mechanics.setPermanentEffects(list); }
    public List<String> getGrantedPermissions() { return mechanics.getGrantedPermissions(); }
    public void setGrantedPermissions(List<String> list) { mechanics.setGrantedPermissions(list); }
    public List<String> getRequiredBiomes() { return requirements.getRequiredBiomes(); }
    public void setRequiredBiomes(List<String> list) { requirements.setRequiredBiomes(list); }
    public List<String> getCompatibleTypes() { return requirements.getCompatibleTypes(); }
    public void setCompatibleTypes(List<String> list) { requirements.setCompatibleTypes(list); }
    public List<String> getCompatibleIds() { return requirements.getCompatibleIds(); }
    public void setCompatibleIds(List<String> list) { requirements.setCompatibleIds(list); }
    public List<String> getCompatibleMaterials() { return requirements.getCompatibleMaterials(); }
    public void setCompatibleMaterials(List<String> list) { requirements.setCompatibleMaterials(list); }
    public List<String> getCustomSounds() { return mechanics.getCustomSounds(); }
    public void setCustomSounds(List<String> list) { mechanics.setCustomSounds(list); }
    public List<String> getCommands() { return mechanics.getCommands(); }
    public void setCommands(List<String> list) { mechanics.setCommands(list); }
    public List<String> getItemAbilities() { return mechanics.getItemAbilities(); }
    public void setItemAbilities(List<String> list) { mechanics.setItemAbilities(list); }
    public List<String> getGemSockets() { return mechanics.getGemSockets(); }
    public void setGemSockets(List<String> list) { mechanics.setGemSockets(list); }
    
    // Spell Bindings (Item Abilities)
    public java.util.List<me.ray.midgard.modules.item.ability.SpellBinding> getSpellBindings() { 
        return mechanics.getSpellBindings(); 
    }
    public java.util.List<me.ray.midgard.modules.item.ability.SpellBinding> getSpellBindingsForTrigger(
            me.ray.midgard.modules.item.ability.AbilityTrigger trigger) { 
        return mechanics.getSpellBindingsForTrigger(trigger); 
    }
    public boolean hasSpellBindings() { 
        return mechanics.hasSpellBindings(); 
    }
    
    public String getItemSet() { return mechanics.getItemSet(); }
    public void setItemSet(String val) { mechanics.setItemSet(val); }
    public boolean isDisableDropOnDeath() { return restrictions.isDisableDropOnDeath(); }
    public void setDisableDropOnDeath(boolean val) { restrictions.setDisableDropOnDeath(val); }
    public String getCameraOverlay() { return display.getCameraOverlay(); }
    public void setCameraOverlay(String val) { display.setCameraOverlay(val); }
    public boolean isHideDurabilityBar() { return durability.isHideDurabilityBar(); }
    public void setHideDurabilityBar(boolean val) { durability.setHideDurabilityBar(val); }
    public boolean isUnstackable() { return restrictions.isUnstackable(); }
    public void setUnstackable(boolean val) { restrictions.setUnstackable(val); }
    public String getCooldownReference() { return mechanics.getCooldownReference(); }
    public void setCooldownReference(String val) { mechanics.setCooldownReference(val); }
    public String getCraftingRecipePermission() { return mechanics.getCraftingRecipePermission(); }
    public void setCraftingRecipePermission(String val) { mechanics.setCraftingRecipePermission(val); }
    public String getRepairReference() { return durability.getRepairReference(); }
    public void setRepairReference(String val) { durability.setRepairReference(val); }
    public boolean isAmphibian() { return mechanics.isAmphibian(); }
    public void setAmphibian(boolean val) { mechanics.setAmphibian(val); }
    public String getTrimMaterial() { return display.getTrimMaterial(); }
    public void setTrimMaterial(String val) { display.setTrimMaterial(val); }
    public String getTrimPattern() { return display.getTrimPattern(); }
    public void setTrimPattern(String val) { display.setTrimPattern(val); }
    public boolean isHideArmorTrim() { return display.isHideArmorTrim(); }
    public void setHideArmorTrim(boolean val) { display.setHideArmorTrim(val); }
    public boolean isDisableAdvancedEnchants() { return mechanics.isDisableAdvancedEnchants(); }
    public void setDisableAdvancedEnchants(boolean val) { mechanics.setDisableAdvancedEnchants(val); }
    public int getBrowserIndex() { return general.getBrowserIndex(); }
    public void setBrowserIndex(int val) { general.setBrowserIndex(val); }
    public String getTier() { return general.getTier(); }
    public void setTier(String val) { general.setTier(val); }

    // Crafting
    public java.util.List<MidgardRecipe> getRecipes() { return crafting.getRecipes(); }
    public MidgardRecipe getRecipe(String recipeId) { return crafting.getRecipe(recipeId); }
    public boolean isCraftingEnabled() { return crafting.isCraftingEnabled(); }
    public void setCraftingEnabled(boolean val) { crafting.setCraftingEnabled(val); }
    public boolean isCraftingEnabled(String type) { return crafting.isCraftingEnabled(type); }
    public void setCraftingEnabled(String type, boolean val) { crafting.setCraftingEnabled(type, val); }
    public boolean isCraftingShaped() { return crafting.isCraftingShaped(); }
    public void setCraftingShaped(boolean val) { crafting.setCraftingShaped(val); }
    public boolean isCraftingShaped(String type) { return crafting.isCraftingShaped(type); }
    public void setCraftingShaped(String type, boolean val) { crafting.setCraftingShaped(type, val); }
    public int getCraftingOutputAmount() { return crafting.getCraftingOutputAmount(); }
    public void setCraftingOutputAmount(int val) { crafting.setCraftingOutputAmount(val); }
    public int getCraftingOutputAmount(String type) { return crafting.getCraftingOutputAmount(type); }
    public void setCraftingOutputAmount(String type, int val) { crafting.setCraftingOutputAmount(type, val); }
    public Map<Integer, String> getCraftingIngredients() { return crafting.getCraftingIngredients(); }
    public Map<Integer, String> getCraftingIngredients(String type) { return crafting.getCraftingIngredients(type); }
    public void setCraftingIngredient(int slot, String ingredient) { crafting.setCraftingIngredient(slot, ingredient); }
    public void setCraftingIngredient(String type, int slot, String ingredient) { crafting.setCraftingIngredient(type, slot, ingredient); }
    public double getCraftingExperience(String type) { return crafting.getCraftingExperience(type); }
    public void setCraftingExperience(String type, double val) { crafting.setCraftingExperience(type, val); }
    public int getCraftingDuration(String type) { return crafting.getCraftingDuration(type); }
    public void setCraftingDuration(String type, int val) { crafting.setCraftingDuration(type, val); }
    public boolean isCraftingHiddenFromBook(String type) { return crafting.isCraftingHiddenFromBook(type); }
    public void setCraftingHiddenFromBook(String type, boolean val) { crafting.setCraftingHiddenFromBook(type, val); }

    public void addRecipe(MidgardRecipe recipe) { crafting.addRecipe(recipe); }
    public void updateRecipe(MidgardRecipe recipe) { crafting.updateRecipe(recipe); }
    public void removeRecipe(String recipeId) { crafting.removeRecipe(recipeId); }

    public void setStat(ItemStat stat, double value) { mechanics.setStat(stat, value); }
    public void setStat(ItemStat stat, String value) { mechanics.setStat(stat, value); }
    public double getStat(ItemStat stat) { return mechanics.getStat(stat); }
    public StatRange getStatRange(ItemStat stat) { return mechanics.getStatRange(stat); }

    // Updater Options
    public boolean isKeepLore() { return updaterOptions.isKeepLore(); }
    public void setKeepLore(boolean val) { updaterOptions.setKeepLore(val); }
    public boolean isKeepName() { return updaterOptions.isKeepName(); }
    public void setKeepName(boolean val) { updaterOptions.setKeepName(val); }
    public boolean isKeepEnchantments() { return updaterOptions.isKeepEnchantments(); }
    public void setKeepEnchantments(boolean val) { updaterOptions.setKeepEnchantments(val); }
    public boolean isKeepExternalSH() { return updaterOptions.isKeepExternalSH(); }
    public void setKeepExternalSH(boolean val) { updaterOptions.setKeepExternalSH(val); }
    public boolean isKeepUpgrades() { return updaterOptions.isKeepUpgrades(); }
    public void setKeepUpgrades(boolean val) { updaterOptions.setKeepUpgrades(val); }
    public boolean isKeepGemStones() { return updaterOptions.isKeepGemStones(); }
    public void setKeepGemStones(boolean val) { updaterOptions.setKeepGemStones(val); }
    public boolean isKeepSoulbind() { return updaterOptions.isKeepSoulbind(); }
    public void setKeepSoulbind(boolean val) { updaterOptions.setKeepSoulbind(val); }

    public void save() {
        config.set("type", categoryId);

        ItemModule moduleInstance = ItemModule.getInstance();
        if (moduleInstance != null) {
            ItemRepository repository = moduleInstance.getItemRepository();
            if (repository != null) {
                String yamlData = serializeConfig();
                repository.saveItem(id, categoryId, yamlData, null);

                ItemSyncManager syncManager = moduleInstance.getItemSyncManager();
                if (syncManager != null) {
                    syncManager.notifyUpdate(id);
                }
            }

            if (moduleInstance.getRecipeManager() != null) {
                moduleInstance.getRecipeManager().updateItemRecipes(this);
            }
        }
    }

    /**
     * Serializa a ConfigurationSection do item para YAML string.
     */
    public String serializeConfig() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (String key : config.getKeys(true)) {
            if (!config.isConfigurationSection(key)) {
                yaml.set(key, config.get(key));
            }
        }
        return yaml.saveToString();
    }

    private void reloadComponents() {
        this.crafting = new MidgardItemCrafting(this, base);
        this.updaterOptions = new MidgardItemUpdaterOptions(this, base);
        this.general = new MidgardItemGeneral(this, base);
        this.display = new MidgardItemDisplay(this, base);
        this.durability = new MidgardItemDurability(this, base);
        this.restrictions = new MidgardItemRestrictions(this, base);
        this.requirements = new MidgardItemRequirements(this, base);
        this.mechanics = new MidgardItemMechanics(this, base);
    }
}

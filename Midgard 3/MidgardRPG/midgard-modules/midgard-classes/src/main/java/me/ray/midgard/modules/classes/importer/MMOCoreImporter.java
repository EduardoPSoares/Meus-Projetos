package me.ray.midgard.modules.classes.importer;

import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Importador de pacotes do MMOCore para o MidgardRPG.
 * 
 * Suporta importação de:
 * - Classes (MMOCore/classes/*.yml)
 * - Skills (MMOCore/skills/*.yml + MythicLib/skill/*.yml)
 * - MythicMobs Skills (já são compatíveis nativamente)
 * 
 * Estrutura esperada do pacote:
 * /pacote/
 *   /MMOCore/
 *     /classes/
 *     /skills/
 *   /MythicLib/
 *     /skill/
 *   /MythicMobs/
 *     /skills/
 *   /Nexo/ (opcional)
 *     /items/
 */
public class MMOCoreImporter {

    private final JavaPlugin plugin;
    private final File outputClassesFolder;
    private final File outputSpellsFolder;
    private final File outputItemsFolder;
    private final File mythicMobsSkillsFolder;
    
    private int importedClasses = 0;
    private int importedSpells = 0;
    private int importedItems = 0;
    private int copiedMythicSkills = 0;
    private List<String> warnings = new ArrayList<>();

    public MMOCoreImporter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.outputClassesFolder = new File(plugin.getDataFolder(), "modules/classes/classes");
        this.outputSpellsFolder = new File(plugin.getDataFolder(), "modules/spells/spells");
        this.outputItemsFolder = new File(plugin.getDataFolder(), "modules/item/imported");
        this.mythicMobsSkillsFolder = new File(plugin.getDataFolder().getParentFile(), "MythicMobs/Skills");
    }

    /**
     * Importa um pacote completo do MMOCore.
     * 
     * @param packageFolder Pasta raiz do pacote
     * @return Resultado da importação
     */
    public ImportResult importPackage(File packageFolder) {
        importedClasses = 0;
        importedSpells = 0;
        importedItems = 0;
        copiedMythicSkills = 0;
        warnings.clear();

        if (!packageFolder.exists() || !packageFolder.isDirectory()) {
            return new ImportResult(false, "Pasta não encontrada: " + packageFolder.getPath(), 0, 0, 0, 0, warnings);
        }

        // Criar pastas de saída se necessário
        outputClassesFolder.mkdirs();
        outputSpellsFolder.mkdirs();
        outputItemsFolder.mkdirs();

        // Importar classes
        File mmocoreClasses = new File(packageFolder, "MMOCore/classes");
        if (mmocoreClasses.exists()) {
            importClasses(mmocoreClasses);
        }

        // Importar skills (combina MMOCore/skills + MythicLib/skill)
        File mmocoreSkills = new File(packageFolder, "MMOCore/skills");
        File mythicLibSkills = new File(packageFolder, "MythicLib/skill");
        
        // Primeiro carrega o mapeamento do MythicLib
        Map<String, MythicLibMapping> mythicMappings = new HashMap<>();
        if (mythicLibSkills.exists()) {
            mythicMappings = loadMythicLibMappings(mythicLibSkills);
        }
        
        // Depois importa as skills com o mapeamento
        if (mmocoreSkills.exists()) {
            importSkills(mmocoreSkills, mythicMappings);
        }
        
        // Importar itens do MMOItems
        File mmoItemsFolder = new File(packageFolder, "MMOItems/item");
        if (mmoItemsFolder.exists()) {
            importItems(mmoItemsFolder, mythicMappings);
            
            // Registrar categoria IMPORTED no item-types.yml
            if (importedItems > 0) {
                registerImportedCategory();
            }
        }
        
        // Copiar skills do MythicMobs para a pasta do plugin
        File mythicMobsSkills = new File(packageFolder, "MythicMobs/skills");
        if (mythicMobsSkills.exists()) {
            copyMythicMobsSkills(mythicMobsSkills);
        }
        
        // Copiar mobs do MythicMobs (VFX, etc)
        File mythicMobsMobs = new File(packageFolder, "MythicMobs/mobs");
        if (mythicMobsMobs.exists()) {
            copyMythicMobsMobs(mythicMobsMobs);
        }
        
        // ==================== IMPORTAR ARQUIVOS DE CONFIGURAÇÃO EXTRAS ====================
        
        // Importar tiers.yml do MMOItems
        File mmoItemsTiers = new File(packageFolder, "MMOItems/item-tiers.yml");
        if (mmoItemsTiers.exists()) {
            copyConfigFile(mmoItemsTiers, new File(plugin.getDataFolder(), "modules/item/imported-tiers.yml"));
            warnings.add("INFO: item-tiers.yml importado para modules/item/imported-tiers.yml - revisar e mesclar manualmente");
        }
        
        // Importar item-sets.yml do MMOItems
        File mmoItemsSets = new File(packageFolder, "MMOItems/item-sets.yml");
        if (mmoItemsSets.exists()) {
            copyConfigFile(mmoItemsSets, new File(plugin.getDataFolder(), "modules/item/imported-sets.yml"));
            warnings.add("INFO: item-sets.yml importado para modules/item/imported-sets.yml - revisar e mesclar manualmente");
        }
        
        // Importar upgrade-templates.yml do MMOItems
        File mmoItemsUpgrade = new File(packageFolder, "MMOItems/upgrade-templates.yml");
        if (mmoItemsUpgrade.exists()) {
            copyConfigFile(mmoItemsUpgrade, new File(plugin.getDataFolder(), "modules/item/imported-upgrade-templates.yml"));
            warnings.add("INFO: upgrade-templates.yml importado para modules/item/imported-upgrade-templates.yml - revisar e mesclar manualmente");
        }
        
        // Importar stats.yml (configuração de display de stats)
        File mmoItemsStats = new File(packageFolder, "MMOItems/stats.yml");
        if (mmoItemsStats.exists()) {
            copyConfigFile(mmoItemsStats, new File(plugin.getDataFolder(), "modules/item/imported-stats-display.yml"));
            warnings.add("INFO: stats.yml importado para modules/item/imported-stats-display.yml - revisar e mesclar manualmente");
        }
        
        // Importar lore-format.yml
        File mmoItemsLore = new File(packageFolder, "MMOItems/lore-format.yml");
        if (mmoItemsLore.exists()) {
            copyConfigFile(mmoItemsLore, new File(plugin.getDataFolder(), "modules/item/imported-lore-format.yml"));
            warnings.add("INFO: lore-format.yml importado para modules/item/imported-lore-format.yml - revisar e mesclar manualmente");
        }

        String message = String.format("Importação concluída! Classes: %d, Spells: %d, Items: %d, MythicMobs Skills: %d", 
            importedClasses, importedSpells, importedItems, copiedMythicSkills);
        return new ImportResult(true, message, importedClasses, importedSpells, importedItems, copiedMythicSkills, warnings);
    }
    
    /**
     * Copia um arquivo de configuração para o destino.
     */
    private void copyConfigFile(File source, File destination) {
        try {
            destination.getParentFile().mkdirs();
            java.nio.file.Files.copy(source.toPath(), destination.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            MidgardLogger.info("Arquivo de configuração copiado: " + source.getName() + " -> " + destination.getName());
        } catch (IOException e) {
            warnings.add("Erro ao copiar " + source.getName() + ": " + e.getMessage());
        }
    }

    private void importClasses(File classesFolder) {
        File[] files = classesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                importClass(file);
                importedClasses++;
            } catch (Exception e) {
                warnings.add("Erro ao importar classe " + file.getName() + ": " + e.getMessage());
                MidgardLogger.error("Erro ao importar classe MMOCore: " + file.getName(), e);
            }
        }
    }

    private void importClass(File mmocoreFile) throws IOException {
        YamlConfiguration mmocore = YamlConfiguration.loadConfiguration(mmocoreFile);
        String classId = mmocoreFile.getName().replace(".yml", "");
        
        // Criar configuração no formato MidgardRPG
        YamlConfiguration midgard = new YamlConfiguration();
        
        // Display
        ConfigurationSection display = mmocore.getConfigurationSection("display");
        if (display != null) {
            String displayName = display.getString("name", classId);
            // Remove color codes antigos e converte para MiniMessage
            displayName = convertLegacyColors(displayName);
            midgard.set("display-name", displayName);
            
            // Item/Icon - formato: MATERIAL:CUSTOM_MODEL_DATA
            String item = display.getString("item", "BARRIER");
            parseItemToMidgard(midgard, item);
            
            // Lore
            List<String> lore = display.getStringList("lore");
            List<String> convertedLore = new ArrayList<>();
            for (String line : lore) {
                convertedLore.add(convertLegacyColors(line));
            }
            midgard.set("lore", convertedLore);
        }
        
        // Max Level
        midgard.set("max-level", mmocore.getInt("max-level", 100));
        
        // Skills -> Spells mapping
        ConfigurationSection skills = mmocore.getConfigurationSection("skills");
        if (skills != null) {
            List<Map<String, Object>> spellsList = new ArrayList<>();
            for (String skillId : skills.getKeys(false)) {
                Map<String, Object> spellEntry = new HashMap<>();
                String spellIdLower = skillId.toLowerCase();
                spellEntry.put("id", spellIdLower);
                spellEntry.put("unlock-level", skills.getInt(skillId + ".level", 1));
                spellEntry.put("max-level", skills.getInt(skillId + ".max-level", 30));
                spellsList.add(spellEntry);
            }
            midgard.set("spells", spellsList);
        }
        
        // Skill slots -> pode ser usado para configurar skillbar
        ConfigurationSection skillSlots = mmocore.getConfigurationSection("skill-slots");
        if (skillSlots != null) {
            midgard.set("skill-slots", skillSlots.getKeys(false).size());
        }
        
        // Atributos base (valores padrão se não especificados)
        midgard.set("base-attributes.health", 100);
        midgard.set("base-attributes.mana", 50);
        midgard.set("base-attributes.strength", 10);
        midgard.set("base-attributes.defense", 10);
        
        midgard.set("per-level.health", 10);
        midgard.set("per-level.mana", 5);
        
        // Salvar
        File outputFile = new File(outputClassesFolder, classId + ".yml");
        midgard.save(outputFile);
        
        MidgardLogger.info("Classe importada: " + classId);
    }

    private Map<String, MythicLibMapping> loadMythicLibMappings(File mythicLibFolder) {
        Map<String, MythicLibMapping> mappings = new HashMap<>();
        
        File[] files = mythicLibFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return mappings;
        }
        
        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            for (String skillId : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(skillId);
                if (section == null) {
                    continue;
                }
                
                String mythicMobsSkillId = section.getString("mythicmobs-skill-id");
                List<String> modifiers = section.getStringList("modifiers");
                String passiveType = section.getString("passive-type", null);
                
                mappings.put(skillId, new MythicLibMapping(mythicMobsSkillId, modifiers, passiveType));
            }
        }
        
        return mappings;
    }

    private void importSkills(File skillsFolder, Map<String, MythicLibMapping> mythicMappings) {
        File[] files = skillsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            // Ignorar arquivos de hash/cache do MMOCore
            if (file.getName().matches("[0-9a-f]{16}")) {
                continue;
            }
            
            try {
                importSkill(file, mythicMappings);
                importedSpells++;
            } catch (Exception e) {
                warnings.add("Erro ao importar skill " + file.getName() + ": " + e.getMessage());
                MidgardLogger.error("Erro ao importar skill MMOCore: " + file.getName(), e);
            }
        }
    }

    private void importSkill(File mmocoreFile, Map<String, MythicLibMapping> mythicMappings) throws IOException {
        YamlConfiguration mmocore = YamlConfiguration.loadConfiguration(mmocoreFile);
        String skillId = mmocoreFile.getName().replace(".yml", "").toLowerCase();
        String skillIdUpper = skillId.toUpperCase();
        
        // Criar configuração no formato MidgardRPG Spells
        YamlConfiguration midgard = new YamlConfiguration();
        
        // Nome e descrição
        String name = mmocore.getString("name", skillId);
        midgard.set("name", convertLegacyColors(name));
        
        // Lore/Description
        List<String> lore = mmocore.getStringList("lore");
        List<String> convertedLore = new ArrayList<>();
        for (String line : lore) {
            convertedLore.add(convertLegacyColors(line));
        }
        midgard.set("description", convertedLore);
        
        // Material/Icon - formato: MATERIAL:CUSTOM_MODEL_DATA
        String material = mmocore.getString("material", "BLAZE_POWDER");
        parseIconToMidgard(midgard, material);
        
        // MythicMobs skill ID
        MythicLibMapping mapping = mythicMappings.get(skillIdUpper);
        if (mapping != null && mapping.mythicMobsSkillId != null) {
            midgard.set("mythic-skill", mapping.mythicMobsSkillId);
            
            // Se for passivo
            if (mapping.passiveType != null) {
                midgard.set("passive", true);
                midgard.set("passive-type", mapping.passiveType);
            }
        } else {
            // Fallback: usar o ID da skill como nome do mythic skill
            midgard.set("mythic-skill", skillId);
            warnings.add("Skill " + skillId + " não tem mapeamento MythicLib. Usando ID como mythic-skill.");
        }
        
        // Atributos escaláveis
        parseScalableAttribute(mmocore, midgard, "cooldown");
        parseScalableAttribute(mmocore, midgard, "mana");
        parseScalableAttribute(mmocore, midgard, "stamina");
        
        // Damage como variável
        if (mmocore.isConfigurationSection("damage")) {
            ConfigurationSection dmg = mmocore.getConfigurationSection("damage");
            midgard.set("variables.damage.base", dmg.getDouble("base", 0));
            midgard.set("variables.damage.per-level", dmg.getDouble("per-level", 0));
        }
        
        // Timer (para skills passivas)
        if (mmocore.isConfigurationSection("timer")) {
            ConfigurationSection timer = mmocore.getConfigurationSection("timer");
            double timerBase = timer.getDouble("base", 0);
            if (timerBase > 0) {
                midgard.set("variables.timer.base", timerBase);
                midgard.set("variables.timer.per-level", timer.getDouble("per-level", 0));
            }
        }
        
        // Cast time (não existe no MMOCore, mas podemos definir padrão)
        midgard.set("cast-time", 0.0);
        midgard.set("interruptible", true);
        
        // Salvar
        File outputFile = new File(outputSpellsFolder, skillId + ".yml");
        midgard.save(outputFile);
        
        MidgardLogger.info("Spell importada: " + skillId);
    }

    private void parseScalableAttribute(YamlConfiguration source, YamlConfiguration target, String key) {
        if (source.isConfigurationSection(key)) {
            ConfigurationSection section = source.getConfigurationSection(key);
            target.set(key + ".base", section.getDouble("base", 0));
            target.set(key + ".per-level", section.getDouble("per-level", 0));
        } else if (source.contains(key)) {
            target.set(key, source.getDouble(key, 0));
        }
    }

    private void parseItemToMidgard(YamlConfiguration config, String itemString) {
        // Formato MMOCore: STONE_SWORD:4577 (material:customModelData)
        if (itemString.contains(":")) {
            String[] parts = itemString.split(":");
            config.set("icon", parts[0]);
            try {
                config.set("model-data", Integer.parseInt(parts[1]));
            } catch (NumberFormatException e) {
                config.set("icon", parts[0]);
            }
        } else {
            config.set("icon", itemString);
        }
    }

    private void parseIconToMidgard(YamlConfiguration config, String materialString) {
        // Formato MMOCore: COAL:1920
        if (materialString.contains(":")) {
            String[] parts = materialString.split(":");
            config.set("icon.material", parts[0]);
            try {
                config.set("icon.model-data", Integer.parseInt(parts[1]));
            } catch (NumberFormatException e) {
                config.set("icon.material", parts[0]);
            }
        } else {
            config.set("icon.material", materialString);
        }
    }

    // ==================== MMOITEMS IMPORT ====================
    
    private void importItems(File itemsFolder, Map<String, MythicLibMapping> mythicMappings) {
        File[] files = itemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                importItemFile(file, mythicMappings);
            } catch (Exception e) {
                warnings.add("Erro ao importar item " + file.getName() + ": " + e.getMessage());
                MidgardLogger.error("Erro ao importar item MMOItems: " + file.getName(), e);
            }
        }
    }

    private void importItemFile(File mmocoreFile, Map<String, MythicLibMapping> mythicMappings) throws IOException {
        YamlConfiguration mmoItems = YamlConfiguration.loadConfiguration(mmocoreFile);
        String category = mmocoreFile.getName().replace(".yml", "").toLowerCase();
        
        for (String itemId : mmoItems.getKeys(false)) {
            ConfigurationSection itemSection = mmoItems.getConfigurationSection(itemId);
            if (itemSection == null) {
                continue;
            }
            
            ConfigurationSection base = itemSection.getConfigurationSection("base");
            if (base == null) {
                continue;
            }
            
            YamlConfiguration midgard = new YamlConfiguration();
            
            // ==================== DISPLAY ====================
            
            // Material
            String material = base.getString("material", "STONE_SWORD");
            midgard.set("base.material", material);
            
            // Model data - verificar várias variações de nome do MMOItems
            int modelData = getIntFromVariations(base, "custom-model-data", "CustomModelData", "model-data", "custom_model_data");
            if (modelData > 0) {
                midgard.set("base.custom-model-data", modelData);
            }
            
            // Nome
            String name = base.getString("name", itemId);
            midgard.set("base.name", convertLegacyColors(name));
            
            // Lore
            List<String> lore = base.getStringList("lore");
            List<String> convertedLore = new ArrayList<>();
            for (String line : lore) {
                convertedLore.add(convertLegacyColors(line));
            }
            if (!convertedLore.isEmpty()) {
                midgard.set("base.lore", convertedLore);
            }
            
            // Tipo e tier
            midgard.set("base.type", category.toUpperCase());
            String tier = base.getString("tier", "COMMON");
            midgard.set("base.tier", tier.toUpperCase());
            
            // Dye color para leather armor
            if (base.contains("dye-color")) {
                midgard.set("base.dye-color", base.getString("dye-color"));
            }
            
            // Skull texture
            if (base.contains("skull-texture")) {
                midgard.set("base.skull-texture", base.getString("skull-texture"));
            }
            
            // Item Particles
            if (base.isConfigurationSection("item-particles")) {
                ConfigurationSection particles = base.getConfigurationSection("item-particles");
                midgard.set("base.item-particles", particles.getString("type", "") + ":" + 
                    particles.getString("particle", "") + ":" + 
                    particles.getDouble("radius", 1.0));
            }
            
            // ==================== STATS NUMÉRICOS ====================
            
            // Stats de combate
            importDoubleStat(base, midgard, "attack-damage");
            importDoubleStat(base, midgard, "attack-speed");
            importDoubleStat(base, midgard, "critical-strike-chance");
            importDoubleStat(base, midgard, "critical-strike-power");
            importDoubleStat(base, midgard, "block-power");
            importDoubleStat(base, midgard, "block-rating");
            importDoubleStat(base, midgard, "dodge-rating");
            importDoubleStat(base, midgard, "parry-rating");
            
            // Stats de defesa
            importDoubleStat(base, midgard, "armor");
            importDoubleStat(base, midgard, "armor-toughness");
            importDoubleStat(base, midgard, "defense");
            importDoubleStat(base, midgard, "knockback-resistance");
            
            // Stats de vida/mana/stamina
            importDoubleStat(base, midgard, "max-health");
            importDoubleStat(base, midgard, "max-mana");
            importDoubleStat(base, midgard, "max-stamina");
            importDoubleStat(base, midgard, "health-regeneration");
            importDoubleStat(base, midgard, "mana-regeneration");
            importDoubleStat(base, midgard, "stamina-regeneration");
            
            // Stats de movimento
            importDoubleStat(base, midgard, "movement-speed");
            
            // Stats de dano
            importDoubleStat(base, midgard, "weapon-damage");
            importDoubleStat(base, midgard, "skill-damage");
            importDoubleStat(base, midgard, "projectile-damage");
            importDoubleStat(base, midgard, "physical-damage");
            importDoubleStat(base, midgard, "magic-damage");
            importDoubleStat(base, midgard, "undead-damage");
            
            // Stats de redução de dano
            importDoubleStat(base, midgard, "damage-reduction");
            importDoubleStat(base, midgard, "fall-damage-reduction");
            importDoubleStat(base, midgard, "projectile-damage-reduction");
            importDoubleStat(base, midgard, "physical-damage-reduction");
            importDoubleStat(base, midgard, "magic-damage-reduction");
            importDoubleStat(base, midgard, "fire-damage-reduction");
            importDoubleStat(base, midgard, "pve-damage-reduction");
            importDoubleStat(base, midgard, "pvp-damage-reduction");
            
            // Stats especiais
            importDoubleStat(base, midgard, "lifesteal");
            importDoubleStat(base, midgard, "spell-vampirism");
            importDoubleStat(base, midgard, "cooldown-reduction");
            importDoubleStat(base, midgard, "additional-experience");
            importDoubleStat(base, midgard, "item-cooldown");
            
            // ==================== DANO ELEMENTAL ====================
            
            if (base.isConfigurationSection("element")) {
                ConfigurationSection elements = base.getConfigurationSection("element");
                for (String element : elements.getKeys(false)) {
                    ConfigurationSection elemSection = elements.getConfigurationSection(element);
                    if (elemSection != null) {
                        // Dano elemental
                        if (elemSection.contains("damage")) {
                            String midgardKey = element.toLowerCase() + "-damage";
                            midgard.set("base." + midgardKey, getNumericValue(elemSection, "damage"));
                        }
                        // Defesa elemental
                        if (elemSection.contains("defense")) {
                            String midgardKey = element.toLowerCase() + "-damage-reduction";
                            midgard.set("base." + midgardKey, getNumericValue(elemSection, "defense"));
                        }
                    }
                }
            }
            
            // ==================== BOOLEANS ====================
            
            if (base.getBoolean("unbreakable", false)) {
                midgard.set("base.unbreakable", true);
            }
            if (base.getBoolean("two-handed", false)) {
                midgard.set("base.two-handed", true);
            }
            if (base.getBoolean("unstackable", false)) {
                midgard.set("base.unstackable", true);
            }
            if (base.getBoolean("disable-interaction", false)) {
                midgard.set("base.disable-interaction", true);
            }
            if (base.getBoolean("disable-crafting", false)) {
                midgard.set("base.disable-crafting", true);
            }
            if (base.getBoolean("disable-enchanting", false)) {
                midgard.set("base.disable-enchanting", true);
            }
            if (base.getBoolean("disable-repairing", false)) {
                midgard.set("base.disable-repairing", true);
            }
            if (base.getBoolean("disable-smelting", false)) {
                midgard.set("base.disable-smelting", true);
            }
            if (base.getBoolean("hide-enchants", false) || base.getBoolean("hide-enchantments", false)) {
                midgard.set("base.hide-enchantments", true);
            }
            
            // ==================== REQUIREMENTS ====================
            
            // Required level
            int requiredLevel = getIntFromVariations(base, "required-level", "level-requirement");
            if (requiredLevel > 0) {
                midgard.set("base.required-level", requiredLevel);
            }
            
            // Required class
            String requiredClass = base.getString("required-class", "");
            if (requiredClass.isEmpty()) {
                requiredClass = base.getString("class-requirement", "");
            }
            if (!requiredClass.isEmpty()) {
                midgard.set("base.required-class", requiredClass);
            }
            
            // Permission
            String permission = base.getString("permission", "");
            if (!permission.isEmpty()) {
                midgard.set("base.permission", permission);
            }
            
            // ==================== ENCHANTMENTS ====================
            
            if (base.isConfigurationSection("enchants")) {
                ConfigurationSection enchants = base.getConfigurationSection("enchants");
                StringBuilder enchantStr = new StringBuilder();
                for (String enchant : enchants.getKeys(false)) {
                    int level;
                    if (enchants.isConfigurationSection(enchant)) {
                        level = enchants.getConfigurationSection(enchant).getInt("base", 1);
                    } else {
                        level = enchants.getInt(enchant, 1);
                    }
                    if (!enchantStr.isEmpty()) {
                        enchantStr.append(";");
                    }
                    enchantStr.append(enchant.toUpperCase()).append(":").append(level);
                }
                if (!enchantStr.isEmpty()) {
                    midgard.set("base.enchantments", enchantStr.toString());
                }
            }
            
            // ==================== PERMANENT EFFECTS ====================
            
            if (base.isConfigurationSection("perm-effects")) {
                ConfigurationSection effects = base.getConfigurationSection("perm-effects");
                List<String> permEffects = new ArrayList<>();
                for (String effect : effects.getKeys(false)) {
                    int level;
                    if (effects.isConfigurationSection(effect)) {
                        level = effects.getConfigurationSection(effect).getInt("base", 1);
                    } else {
                        level = effects.getInt(effect, 1);
                    }
                    permEffects.add(effect.toUpperCase() + ":" + level);
                }
                if (!permEffects.isEmpty()) {
                    midgard.set("base.permanent-effects", permEffects);
                }
            }
            
            // ==================== GEM SOCKETS ====================
            
            if (base.isList("gem-sockets")) {
                List<String> gemSockets = base.getStringList("gem-sockets");
                if (!gemSockets.isEmpty()) {
                    midgard.set("base.gem-sockets", gemSockets);
                }
            }
            
            // ==================== COMMANDS ====================
            
            if (base.isConfigurationSection("commands")) {
                ConfigurationSection commands = base.getConfigurationSection("commands");
                List<String> commandList = new ArrayList<>();
                for (String cmdKey : commands.getKeys(false)) {
                    ConfigurationSection cmd = commands.getConfigurationSection(cmdKey);
                    if (cmd != null) {
                        String command = cmd.getString("command", "");
                        if (!command.isEmpty()) {
                            double cooldown = cmd.getDouble("cooldown", 0);
                            double delay = cmd.getDouble("delay", 0);
                            // Formato: comando|cooldown|delay
                            commandList.add(command + "|" + cooldown + "|" + delay);
                        }
                    }
                }
                if (!commandList.isEmpty()) {
                    midgard.set("base.commands", commandList);
                }
            }
            
            // ==================== ABILITIES / SPELLS ====================
            
            ConfigurationSection abilities = base.getConfigurationSection("ability");
            if (abilities != null) {
                List<Map<String, Object>> spellBindings = new ArrayList<>();
                
                for (String abilityKey : abilities.getKeys(false)) {
                    ConfigurationSection ability = abilities.getConfigurationSection(abilityKey);
                    if (ability == null) {
                        continue;
                    }
                    
                    String type = ability.getString("type", "");
                    String mode = ability.getString("mode", "LEFT_CLICK");
                    
                    // Converter para formato Midgard
                    Map<String, Object> binding = new HashMap<>();
                    binding.put("spell", type.toLowerCase());
                    binding.put("trigger", convertAbilityMode(mode));
                    
                    // Overrides de cooldown/damage
                    if (ability.contains("cooldown")) {
                        binding.put("cooldown-override", getNumericValue(ability, "cooldown"));
                    }
                    if (ability.contains("damage")) {
                        binding.put("damage-override", getNumericValue(ability, "damage"));
                    }
                    if (ability.contains("duration")) {
                        binding.put("duration-override", getNumericValue(ability, "duration"));
                    }
                    if (ability.contains("radius")) {
                        binding.put("radius-override", getNumericValue(ability, "radius"));
                    }
                    
                    spellBindings.add(binding);
                }
                
                if (!spellBindings.isEmpty()) {
                    midgard.set("base.spell-bindings", spellBindings);
                }
            }
            
            // ==================== DURABILITY ====================
            
            int maxDurability = getIntFromVariations(base, "max-durability", "custom-durability", "durability");
            if (maxDurability > 0) {
                midgard.set("base.max-custom-durability", maxDurability);
            }
            
            if (base.getBoolean("lost-when-broken", false)) {
                midgard.set("base.lost-when-broken", true);
            }
            
            // ==================== ITEM SET ====================
            
            String itemSet = base.getString("item-set", "");
            if (itemSet.isEmpty()) {
                itemSet = base.getString("set", "");
            }
            if (!itemSet.isEmpty()) {
                midgard.set("base.item-set", itemSet.toUpperCase());
            }
            
            // ==================== UPGRADE SYSTEM ====================
            
            if (base.isConfigurationSection("upgrade")) {
                ConfigurationSection upgrade = base.getConfigurationSection("upgrade");
                String template = upgrade.getString("template", "");
                if (!template.isEmpty()) {
                    midgard.set("base.upgrade-template", template);
                }
                int maxUpgrade = upgrade.getInt("max", 0);
                if (maxUpgrade > 0) {
                    midgard.set("base.max-upgrade", maxUpgrade);
                }
                double successChance = upgrade.getDouble("success", 100.0);
                if (successChance < 100.0) {
                    midgard.set("base.upgrade-success-chance", successChance);
                }
                if (upgrade.getBoolean("destroy", false)) {
                    midgard.set("base.destroy-on-upgrade-fail", true);
                }
                if (upgrade.getBoolean("workbench", false)) {
                    midgard.set("base.upgrade-workbench-only", true);
                }
            }
            
            // ==================== BLUNT / WEAPON MECHANICS ====================
            
            importDoubleStat(base, midgard, "blunt-power");
            importDoubleStat(base, midgard, "blunt-rating");
            importDoubleStat(base, midgard, "range");
            importDoubleStat(base, midgard, "arrow-velocity");
            importDoubleStat(base, midgard, "knockback");
            importDoubleStat(base, midgard, "recoil");
            
            // ==================== ARROW PARTICLES ====================
            
            if (base.isConfigurationSection("arrow-particles")) {
                ConfigurationSection particles = base.getConfigurationSection("arrow-particles");
                String particleType = particles.getString("particle", "");
                if (!particleType.isEmpty()) {
                    int amount = particles.getInt("amount", 1);
                    double speed = particles.getDouble("speed", 0.1);
                    midgard.set("base.arrow-particles", particleType + ":" + amount + ":" + speed);
                }
            }
            
            // ==================== CONSUMABLE RESTORE ====================
            
            if (base.isConfigurationSection("restore")) {
                ConfigurationSection restore = base.getConfigurationSection("restore");
                double health = getNumericValue(restore, "health");
                double food = getNumericValue(restore, "food");
                double saturation = getNumericValue(restore, "saturation");
                double mana = getNumericValue(restore, "mana");
                double stamina = getNumericValue(restore, "stamina");
                
                if (health > 0) {
                    midgard.set("base.restore-health", health);
                }
                if (food > 0) {
                    midgard.set("base.restore-food", food);
                }
                if (saturation > 0) {
                    midgard.set("base.restore-saturation", saturation);
                }
                if (mana > 0) {
                    midgard.set("base.restore-mana", mana);
                }
                if (stamina > 0) {
                    midgard.set("base.restore-stamina", stamina);
                }
            }
            
            // ==================== SOULBINDING ====================
            
            double soulbindChance = base.getDouble("soulbinding-chance", 0);
            if (soulbindChance > 0) {
                midgard.set("base.soulbinding-chance", soulbindChance);
            }
            int soulbindLevel = base.getInt("soulbound-level", 0);
            if (soulbindLevel > 0) {
                midgard.set("base.soulbound-level", soulbindLevel);
            }
            double soulbindBreakChance = base.getDouble("soulbound-break-chance", 0);
            if (soulbindBreakChance > 0) {
                midgard.set("base.soulbound-break-chance", soulbindBreakChance);
            }
            
            // ==================== GEM STONE OPTIONS ====================
            
            String gemColor = base.getString("gem-color", "");
            if (!gemColor.isEmpty()) {
                midgard.set("base.gem-color", gemColor);
            }
            double gemSuccessRate = base.getDouble("success-rate", 0);
            if (gemSuccessRate > 0) {
                midgard.set("base.gem-success-rate", gemSuccessRate);
            }
            
            // ==================== TOOL OPTIONS ====================
            
            if (base.getBoolean("autosmelt", false)) {
                midgard.set("base.autosmelt", true);
            }
            if (base.getBoolean("bouncing-crack", false)) {
                midgard.set("base.bouncing-crack", true);
            }
            
            // ==================== EQUIP PRIORITY ====================
            
            double equipPriority = base.getDouble("equip-priority", 0);
            if (equipPriority > 0) {
                midgard.set("base.equip-priority", equipPriority);
            }
            
            // ==================== CUSTOM SOUNDS ====================
            
            if (base.isConfigurationSection("sounds") || base.isConfigurationSection("custom-sounds")) {
                ConfigurationSection sounds = base.getConfigurationSection("sounds");
                if (sounds == null) {
                    sounds = base.getConfigurationSection("custom-sounds");
                }
                if (sounds != null) {
                    List<String> soundList = new ArrayList<>();
                    for (String key : sounds.getKeys(false)) {
                        String sound = sounds.getString(key + ".sound", sounds.getString(key, ""));
                        if (!sound.isEmpty()) {
                            soundList.add(key + ":" + sound);
                        }
                    }
                    if (!soundList.isEmpty()) {
                        midgard.set("base.custom-sounds", soundList);
                    }
                }
            }
            
            // ==================== OUTPUT ====================
            
            // Criar configuração com a estrutura correta (ID como raiz)
            YamlConfiguration outputConfig = new YamlConfiguration();
            String normalizedId = itemId.toUpperCase().replace("-", "_").replace(" ", "_");
            
            // Copiar todos os valores para dentro da seção do ID
            for (String key : midgard.getKeys(true)) {
                if (!midgard.isConfigurationSection(key)) {
                    outputConfig.set(normalizedId + "." + key, midgard.get(key));
                }
            }
            
            // Definir a categoria como IMPORTED
            outputConfig.set(normalizedId + ".base.type", "IMPORTED");
            
            // Salvar
            File outputFile = new File(outputItemsFolder, normalizedId.toLowerCase() + ".yml");
            outputConfig.save(outputFile);
            importedItems++;
            
            MidgardLogger.info("Item importado: " + normalizedId);
        }
    }
    
    /**
     * Importa um stat numérico do MMOItems para o Midgard.
     * Suporta tanto valores simples quanto o formato base/scale do MMOItems.
     */
    private void importDoubleStat(ConfigurationSection base, YamlConfiguration midgard, String key) {
        if (!base.contains(key)) {
            return;
        }
        
        double value = getNumericValue(base, key);
        if (value != 0) {
            midgard.set("base." + key, value);
        }
    }
    
    /**
     * Obtém um valor numérico que pode ser simples ou no formato base/scale do MMOItems.
     */
    private double getNumericValue(ConfigurationSection section, String key) {
        if (section.isConfigurationSection(key)) {
            ConfigurationSection statSection = section.getConfigurationSection(key);
            // Formato MMOItems: base + scale * level (usamos só base para itens fixos)
            return statSection.getDouble("base", 0);
        } else {
            return section.getDouble(key, 0);
        }
    }
    
    /**
     * Tenta obter um int de várias possíveis chaves.
     */
    private int getIntFromVariations(ConfigurationSection section, String... keys) {
        for (String key : keys) {
            if (section.contains(key)) {
                if (section.isConfigurationSection(key)) {
                    return section.getConfigurationSection(key).getInt("base", 0);
                }
                return section.getInt(key, 0);
            }
        }
        return 0;
    }

    private String convertAbilityMode(String mmoMode) {
        // Converte modos de ability do MMOItems para triggers do Midgard
        return switch (mmoMode.toUpperCase()) {
            case "LEFT_CLICK" -> "LEFT_CLICK";
            case "RIGHT_CLICK" -> "RIGHT_CLICK";
            case "SHIFT_LEFT_CLICK" -> "SHIFT_LEFT_CLICK";
            case "SHIFT_RIGHT_CLICK" -> "SHIFT_RIGHT_CLICK";
            case "SNEAK" -> "SNEAK";
            case "TIMER" -> "PASSIVE_TIMER";
            default -> "LEFT_CLICK";
        };
    }

    // ==================== MYTHICMOBS COPY ====================
    
    private void copyMythicMobsSkills(File skillsFolder) {
        if (!mythicMobsSkillsFolder.exists()) {
            mythicMobsSkillsFolder.mkdirs();
        }
        
        File[] files = skillsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            try {
                File dest = new File(mythicMobsSkillsFolder, file.getName());
                java.nio.file.Files.copy(file.toPath(), dest.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                copiedMythicSkills++;
                MidgardLogger.info("MythicMobs skill copiada: " + file.getName());
            } catch (IOException e) {
                warnings.add("Erro ao copiar skill MythicMobs " + file.getName() + ": " + e.getMessage());
            }
        }
    }
    
    private void copyMythicMobsMobs(File mobsFolder) {
        File mythicMobsMobsFolder = new File(plugin.getDataFolder().getParentFile(), "MythicMobs/Mobs");
        if (!mythicMobsMobsFolder.exists()) {
            mythicMobsMobsFolder.mkdirs();
        }
        
        File[] files = mobsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            try {
                File dest = new File(mythicMobsMobsFolder, file.getName());
                java.nio.file.Files.copy(file.toPath(), dest.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                MidgardLogger.info("MythicMobs mob copiado: " + file.getName());
            } catch (IOException e) {
                warnings.add("Erro ao copiar mob MythicMobs " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Converte códigos de cor legados (&a, &b, etc) para MiniMessage.
     */
    private String convertLegacyColors(String text) {
        if (text == null) {
            return "";
        }
        
        // Mapeamento de códigos legacy para MiniMessage
        Map<String, String> colorMap = new LinkedHashMap<>();
        colorMap.put("&0", "<black>");
        colorMap.put("&1", "<dark_blue>");
        colorMap.put("&2", "<dark_green>");
        colorMap.put("&3", "<dark_aqua>");
        colorMap.put("&4", "<dark_red>");
        colorMap.put("&5", "<dark_purple>");
        colorMap.put("&6", "<gold>");
        colorMap.put("&7", "<gray>");
        colorMap.put("&8", "<dark_gray>");
        colorMap.put("&9", "<blue>");
        colorMap.put("&a", "<green>");
        colorMap.put("&b", "<aqua>");
        colorMap.put("&c", "<red>");
        colorMap.put("&d", "<light_purple>");
        colorMap.put("&e", "<yellow>");
        colorMap.put("&f", "<white>");
        colorMap.put("&l", "<bold>");
        colorMap.put("&n", "<underlined>");
        colorMap.put("&o", "<italic>");
        colorMap.put("&m", "<strikethrough>");
        colorMap.put("&k", "<obfuscated>");
        colorMap.put("&r", "<reset>");
        
        String result = text;
        for (Map.Entry<String, String> entry : colorMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
            result = result.replace(entry.getKey().toUpperCase(), entry.getValue());
        }
        
        return result;
    }

    /**
     * Registra a categoria IMPORTED no arquivo item-types.yml se ainda não existir.
     */
    private void registerImportedCategory() {
        try {
            File itemTypesFile = new File(plugin.getDataFolder(), "modules/item/item-types.yml");
            if (!itemTypesFile.exists()) {
                warnings.add("Arquivo item-types.yml não encontrado. Categoria IMPORTED não foi registrada.");
                return;
            }
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(itemTypesFile);
            
            // Verifica se a categoria IMPORTED já existe
            if (config.contains("IMPORTED")) {
                return; // Já existe, não precisa adicionar
            }
            
            // Encontra o próximo slot disponível na última página
            int maxPage = 1;
            int maxSlot = 10;
            
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    int page = section.getInt("page", 1);
                    int slot = section.getInt("slot", 10);
                    if (page > maxPage || (page == maxPage && slot > maxSlot)) {
                        maxPage = page;
                        maxSlot = slot;
                    }
                }
            }
            
            // Calcula próximo slot (slots válidos: 10-16, 19-25, 28-34, 37-43)
            int nextSlot = getNextSlot(maxSlot);
            int nextPage = maxPage;
            if (nextSlot == -1) {
                // Precisa de nova página
                nextPage++;
                nextSlot = 10;
            }
            
            // Adiciona a categoria IMPORTED
            config.set("IMPORTED.name", "<gradient:#fbbf24:#f59e0b>Importados</gradient>");
            config.set("IMPORTED.icon", "CHEST");
            config.set("IMPORTED.model-data", 0);
            config.set("IMPORTED.slot", nextSlot);
            config.set("IMPORTED.page", nextPage);
            
            config.save(itemTypesFile);
            MidgardLogger.info("Categoria IMPORTED registrada no item-types.yml (página " + nextPage + ", slot " + nextSlot + ")");
            
        } catch (Exception e) {
            warnings.add("Erro ao registrar categoria IMPORTED: " + e.getMessage());
            MidgardLogger.error("Erro ao registrar categoria IMPORTED", e);
        }
    }
    
    /**
     * Calcula o próximo slot disponível para a GUI.
     * Slots válidos por linha: 10-16, 19-25, 28-34, 37-43
     */
    private int getNextSlot(int currentSlot) {
        int[] validSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        
        for (int i = 0; i < validSlots.length; i++) {
            if (validSlots[i] == currentSlot) {
                if (i + 1 < validSlots.length) {
                    return validSlots[i + 1];
                } else {
                    return -1; // Precisa de nova página
                }
            }
        }
        return 10; // Fallback
    }

    /**
     * Resultado da importação.
     */
    public static class ImportResult {
        public final boolean success;
        public final String message;
        public final int classesImported;
        public final int spellsImported;
        public final int itemsImported;
        public final int mythicSkillsCopied;
        public final List<String> warnings;

        public ImportResult(boolean success, String message, int classesImported, int spellsImported, int itemsImported, int mythicSkillsCopied, List<String> warnings) {
            this.success = success;
            this.message = message;
            this.classesImported = classesImported;
            this.spellsImported = spellsImported;
            this.itemsImported = itemsImported;
            this.mythicSkillsCopied = mythicSkillsCopied;
            this.warnings = warnings;
        }
    }

    /**
     * Mapeamento do MythicLib skill.
     */
    private static class MythicLibMapping {
        final String mythicMobsSkillId;
        final List<String> modifiers;
        final String passiveType;

        MythicLibMapping(String mythicMobsSkillId, List<String> modifiers, String passiveType) {
            this.mythicMobsSkillId = mythicMobsSkillId;
            this.modifiers = modifiers;
            this.passiveType = passiveType;
        }
    }
}

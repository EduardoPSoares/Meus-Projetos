package me.ray.midgard.modules.professions.penalty;

import me.ray.midgard.core.config.ConfigWrapper;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.professions.ProfessionType;
import me.ray.midgard.modules.professions.ProfessionsModule;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Configuração de penalidades por profissão.
 * Carregado da seção 'penalties' de cada profissão YML.
 *
 * Cada profissão define penalidades para não-profissionais:
 * - drop-chance: chance de dropar o item (0.0-1.0) ao quebrar blocos
 * - mining-fatigue: aplica Mining Fatigue ao minerar
 * - craft-fail-chance: chance de falhar craft (perdendo materiais)
 * - smelt-fail-chance: chance de queimar comida (produzir carvão)
 * - fish-junk-multiplier: multiplica chance de lixo na pesca
 * - brew-fail-chance: chance de poção falhar
 * - enchant-extra-cost: custo extra de XP ao encantar
 * - durability-multiplier: multiplicador de desgaste de ferramenta
 * - slowness: aplica Slowness ao executar ação
 */
public final class ProfessionPenaltyConfig {

    private static volatile Map<ProfessionType, ProfessionPenaltyConfig> CONFIGS = Collections.emptyMap();

    private final ProfessionType type;
    private final boolean enabled;
    private final Set<Material> affectedMaterials;

    // Penalidades de bloco
    private final double dropChance;
    private final PotionEffectType miningEffect;
    private final int miningEffectDuration;
    private final int miningEffectAmplifier;
    private final double durabilityMultiplier;

    // Penalidades de craft
    private final double craftFailChance;
    private final Material craftFailResult;

    // Penalidades de fornalha
    private final double smeltFailChance;
    private final Material smeltFailResult;

    // Penalidades de pesca
    private final double fishJunkMultiplier;

    // Penalidades de brew
    private final double brewFailChance;

    // Penalidades de encantamento
    private final int enchantExtraCost;
    private final double enchantFailChance;

    // Slowness ao executar ação
    private final PotionEffectType actionEffect;
    private final int actionEffectDuration;
    private final int actionEffectAmplifier;

    // Mensagem de penalidade
    private final boolean showMessage;

    private ProfessionPenaltyConfig(ProfessionType type, boolean enabled, Set<Material> affectedMaterials,
                                     double dropChance, PotionEffectType miningEffect,
                                     int miningEffectDuration, int miningEffectAmplifier,
                                     double durabilityMultiplier,
                                     double craftFailChance, Material craftFailResult,
                                     double smeltFailChance, Material smeltFailResult,
                                     double fishJunkMultiplier,
                                     double brewFailChance,
                                     int enchantExtraCost, double enchantFailChance,
                                     PotionEffectType actionEffect, int actionEffectDuration,
                                     int actionEffectAmplifier, boolean showMessage) {
        this.type = type;
        this.enabled = enabled;
        this.affectedMaterials = affectedMaterials;
        this.dropChance = dropChance;
        this.miningEffect = miningEffect;
        this.miningEffectDuration = miningEffectDuration;
        this.miningEffectAmplifier = miningEffectAmplifier;
        this.durabilityMultiplier = durabilityMultiplier;
        this.craftFailChance = craftFailChance;
        this.craftFailResult = craftFailResult;
        this.smeltFailChance = smeltFailChance;
        this.smeltFailResult = smeltFailResult;
        this.fishJunkMultiplier = fishJunkMultiplier;
        this.brewFailChance = brewFailChance;
        this.enchantExtraCost = enchantExtraCost;
        this.enchantFailChance = enchantFailChance;
        this.actionEffect = actionEffect;
        this.actionEffectDuration = actionEffectDuration;
        this.actionEffectAmplifier = actionEffectAmplifier;
        this.showMessage = showMessage;
    }

    // ==========================================
    // Getters
    // ==========================================

    public ProfessionType type() { return type; }
    public boolean isEnabled() { return enabled; }
    public Set<Material> affectedMaterials() { return affectedMaterials; }
    public double dropChance() { return dropChance; }
    public PotionEffectType miningEffect() { return miningEffect; }
    public int miningEffectDuration() { return miningEffectDuration; }
    public int miningEffectAmplifier() { return miningEffectAmplifier; }
    public double durabilityMultiplier() { return durabilityMultiplier; }
    public double craftFailChance() { return craftFailChance; }
    public Material craftFailResult() { return craftFailResult; }
    public double smeltFailChance() { return smeltFailChance; }
    public Material smeltFailResult() { return smeltFailResult; }
    public double fishJunkMultiplier() { return fishJunkMultiplier; }
    public double brewFailChance() { return brewFailChance; }
    public int enchantExtraCost() { return enchantExtraCost; }
    public double enchantFailChance() { return enchantFailChance; }
    public PotionEffectType actionEffect() { return actionEffect; }
    public int actionEffectDuration() { return actionEffectDuration; }
    public int actionEffectAmplifier() { return actionEffectAmplifier; }
    public boolean showMessage() { return showMessage; }

    /**
     * Verifica se um material está na lista de materiais afetados.
     * Se a lista estiver vazia, todos os materiais da profissão são afetados.
     */
    public boolean affectsMaterial(Material material) {
        return affectedMaterials.isEmpty() || affectedMaterials.contains(material);
    }

    // ==========================================
    // Loading
    // ==========================================

    public static void loadAll() {
        ProfessionsModule module = ProfessionsModule.getInstance();
        if (module == null || module.getPlugin() == null) {
            MidgardLogger.warn("ProfessionPenaltyConfig.loadAll() chamado antes do módulo inicializar.");
            return;
        }

        Map<ProfessionType, ProfessionPenaltyConfig> newConfigs = new EnumMap<>(ProfessionType.class);
        int loaded = 0;
        for (ProfessionType type : ProfessionType.values()) {
            String path = "modules/professions/professions/" + type.getId() + ".yml";
            try {
                ConfigWrapper wrapper = new ConfigWrapper(module.getPlugin(), path);
                FileConfiguration config = wrapper.getConfig();
                ConfigurationSection penaltySection = config.getConfigurationSection("penalties");

                if (penaltySection == null) {
                    MidgardLogger.debug("Sem 'penalties' em %s, penalidades desabilitadas para %s.",
                            path, type.getDisplayName());
                    continue;
                }

                boolean enabled = penaltySection.getBoolean("enabled", false);
                if (!enabled) {
                    MidgardLogger.debug("Penalidades desabilitadas para %s.", type.getDisplayName());
                    continue;
                }

                // Materiais afetados
                Set<Material> materials = new HashSet<>();
                List<String> matList = penaltySection.getStringList("affected-materials");
                for (String matName : matList) {
                    try {
                        materials.add(Material.valueOf(matName.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        MidgardLogger.warn("Material inválido em penalties de %s: %s", type.getId(), matName);
                    }
                }

                // Block penalties
                ConfigurationSection blockSec = penaltySection.getConfigurationSection("block");
                double dropChance = blockSec != null ? blockSec.getDouble("drop-chance", 1.0) : 1.0;
                double durabilityMult = blockSec != null ? blockSec.getDouble("durability-multiplier", 1.0) : 1.0;

                PotionEffectType miningEffect = null;
                int miningDuration = 0;
                int miningAmplifier = 0;
                if (blockSec != null) {
                    ConfigurationSection effectSec = blockSec.getConfigurationSection("effect");
                    if (effectSec != null) {
                        miningEffect = parsePotionEffect(effectSec.getString("type"));
                        miningDuration = effectSec.getInt("duration", 60);
                        miningAmplifier = effectSec.getInt("amplifier", 0);
                    }
                }

                // Craft penalties
                ConfigurationSection craftSec = penaltySection.getConfigurationSection("craft");
                double craftFail = craftSec != null ? craftSec.getDouble("fail-chance", 0.0) : 0.0;
                Material craftFailRes = craftSec != null ? parseMaterial(craftSec.getString("fail-result")) : null;

                // Smelt penalties
                ConfigurationSection smeltSec = penaltySection.getConfigurationSection("smelt");
                double smeltFail = smeltSec != null ? smeltSec.getDouble("fail-chance", 0.0) : 0.0;
                Material smeltFailRes = smeltSec != null ? parseMaterial(smeltSec.getString("fail-result")) : null;

                // Fish penalties
                ConfigurationSection fishSec = penaltySection.getConfigurationSection("fish");
                double fishJunk = fishSec != null ? fishSec.getDouble("junk-multiplier", 1.0) : 1.0;

                // Brew penalties
                ConfigurationSection brewSec = penaltySection.getConfigurationSection("brew");
                double brewFail = brewSec != null ? brewSec.getDouble("fail-chance", 0.0) : 0.0;

                // Enchant penalties
                ConfigurationSection enchantSec = penaltySection.getConfigurationSection("enchant");
                int enchantExtra = enchantSec != null ? enchantSec.getInt("extra-cost", 0) : 0;
                double enchantFail = enchantSec != null ? enchantSec.getDouble("fail-chance", 0.0) : 0.0;

                // Action effect (generic slowness etc.)
                PotionEffectType actionEff = null;
                int actionDuration = 0;
                int actionAmplifier = 0;
                ConfigurationSection actionSec = penaltySection.getConfigurationSection("action-effect");
                if (actionSec != null) {
                    actionEff = parsePotionEffect(actionSec.getString("type"));
                    actionDuration = actionSec.getInt("duration", 40);
                    actionAmplifier = actionSec.getInt("amplifier", 0);
                }

                boolean showMsg = penaltySection.getBoolean("show-message", true);

                newConfigs.put(type, new ProfessionPenaltyConfig(
                        type, enabled, Set.copyOf(materials),
                        dropChance, miningEffect, miningDuration, miningAmplifier,
                        durabilityMult,
                        craftFail, craftFailRes,
                        smeltFail, smeltFailRes,
                        fishJunk,
                        brewFail,
                        enchantExtra, enchantFail,
                        actionEff, actionDuration, actionAmplifier,
                        showMsg
                ));
                loaded++;
            } catch (Exception e) {
                MidgardLogger.error("Erro ao carregar penalties de '%s'", type.getId(), e);
            }
        }

        // Swap atômico — readers sempre veem um mapa completo e consistente
        CONFIGS = Collections.unmodifiableMap(newConfigs);
        MidgardLogger.info("Carregadas %d configurações de penalidades de profissões.", loaded);
    }

    private static PotionEffectType parsePotionEffect(String name) {
        if (name == null || name.isBlank()) { return null; }
        try {
            return PotionEffectType.getByName(name.toUpperCase());
        } catch (Exception e) {
            MidgardLogger.warn("PotionEffectType inválido: %s", name);
            return null;
        }
    }

    private static Material parseMaterial(String name) {
        if (name == null || name.isBlank()) { return null; }
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            MidgardLogger.warn("Material inválido para fail-result: %s", name);
            return null;
        }
    }

    // ==========================================
    // Static access
    // ==========================================

    public static ProfessionPenaltyConfig get(ProfessionType type) {
        return CONFIGS.get(type);
    }

    public static Map<ProfessionType, ProfessionPenaltyConfig> all() {
        return CONFIGS;
    }
}

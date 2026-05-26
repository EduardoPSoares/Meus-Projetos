package me.ray.midgard.modules.combat;

import me.ray.midgard.core.config.ConfigWrapper;
import me.ray.midgard.core.database.DefinitionRepository;
import me.ray.midgard.core.database.DefinitionMigrationTool;
import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe de configuração para o módulo de combate.
 * <p>
 * Carrega e armazena as configurações definidas no arquivo 'modules/combat.yml'.
 * Inclui configurações para indicadores de dano, combat tag, stamina e sistema de níveis.
 */
public class CombatConfig {

    private final JavaPlugin plugin;
    private final ConfigWrapper configWrapper;
    private FileConfiguration config;
    private DefinitionRepository repository;

    // Construtor vazio para testes
    public CombatConfig() {
        this.plugin = null;
        this.configWrapper = null;
        // Inicializa mapas para evitar NullPointerException em testes
        this.elementalMultipliers = new HashMap<>();
        this.elementalFormats = new HashMap<>();
        this.elementalIcons = new HashMap<>();
        // Safe defaults to prevent division by zero and other issues
        this.defenseDivisor = 100.0;
        this.defenseScalingEnabled = true;
        this.defenseScalingBase = 20.0;
        this.maxMitigation = 0.80;
        this.staminaCheckInterval = 5;
        this.baseHandDamage = 1.0;
        this.maxLevel = 100;
        this.combatTagDuration = 10000L;
        this.xpRequirementsBase = 150.0;
        this.xpRequirementsLinear = 25.0;
        this.xpRequirementsExponential = 1.08;
        this.xpGainDefaultBase = 20.0;
        this.strengthMultiplier = 0.01;
        this.intelligenceMultiplier = 0.01;
        // New system defaults
        this.absorptionEnabled = true;
        this.absorptionDecayPerSecond = 0.0;
        this.absorptionMaxPercent = 50.0;
        this.dotReductionEnabled = true;
        this.skillDamageBonusEnabled = true;
        this.skillReductionEnabled = true;
        this.minionDamageBonusEnabled = true;
        this.minionReductionEnabled = true;
        this.minionOwnerLifeSteal = false;
        this.dodgeEnabled = true;
        this.parryEnabled = true;
        this.blockEnabled = true;
        this.thornsEnabled = true;
        this.lifeStealEnabled = true;
        this.criticalEnabled = true;
        this.staminaSprintDrain = 2.0;
    }

    public CombatConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configWrapper = new ConfigWrapper(plugin, "modules/combat.yml");
        
        // Initialize DB repository + migrate config if needed
        me.ray.midgard.core.database.DatabaseManager dbManager = me.ray.midgard.core.MidgardCore.getDatabaseManager();
        if (dbManager != null) {
            this.repository = new DefinitionRepository(dbManager, "midgard_combat_config");
            java.io.File configFile = new java.io.File(plugin.getDataFolder(), "modules/combat.yml");
            new DefinitionMigrationTool(repository, "combat_config")
                .migrateWholeConfig(configFile, "combat_config");
        }
        
        reload();
    }
    
    public void reload() {
         if (configWrapper != null) {
            // Try loading from DB first
            if (repository != null && repository.count() > 0) {
                DefinitionRepository.DefinitionData data = repository.loadAll().get("combat_config");
                if (data != null) {
                    config = DefinitionMigrationTool.deserializeToConfig(data.yamlData());
                    loadDefaults();
                    loadSettings();
                    return;
                }
            }
            // Fallback: load from YAML file
            configWrapper.reloadConfig();
            config = configWrapper.getConfig();
            loadDefaults(); // Ensure defaults are loaded
            loadSettings();
         }
    }

    public DefinitionRepository getRepository() {
        return repository;
    }
    
    // Método para carregar settings
    private void loadSettings() {
        this.indicatorEnabled = config.getBoolean("damage-indicator.enabled");
        this.indicatorDuration = config.getInt("damage-indicator.duration-ticks");
        this.indicatorFormatNormal = config.getString("damage-indicator.format.normal", "<gray>");
        this.indicatorFormatPhysical = config.getString("damage-indicator.format.physical", "<red>");
        this.indicatorFormatCritical = config.getString("damage-indicator.format.critical", "<red><bold>");
        this.indicatorIconCritical = config.getString("damage-indicator.icon.critical", "");
        this.indicatorIconWeapon = config.getString("damage-indicator.icon.weapon", "🗡");
        this.indicatorIconPhysical = config.getString("damage-indicator.icon.physical", "✘");
        this.indicatorIconProjectile = config.getString("damage-indicator.icon.projectile", "➶");
        this.indicatorIconMagical = config.getString("damage-indicator.icon.magical", "✩");
        this.indicatorIconEnvironment = config.getString("damage-indicator.icon.environment", "🌲");
        this.indicatorIconTrue = config.getString("damage-indicator.icon.true", "🛡");
        this.indicatorIconAoE = config.getString("damage-indicator.icon.aoe", "☀");
        
        this.indicatorFormatMagical = config.getString("damage-indicator.format.magical", "<blue>");
        this.indicatorFormatProjectile = config.getString("damage-indicator.format.projectile", "<green>");
        this.indicatorFormatEnvironment = config.getString("damage-indicator.format.environment", "<dark_green>");
        this.indicatorFormatTrue = config.getString("damage-indicator.format.true", "<white>");
        this.indicatorFormatAoE = config.getString("damage-indicator.format.aoe", "<yellow>");

        // DOT / Skill / Minion indicators
        this.indicatorFormatDot = config.getString("damage-indicator.format.dot", "<green>");
        this.indicatorIconDot = config.getString("damage-indicator.icon.dot", "☣ ");
        this.indicatorFormatSkill = config.getString("damage-indicator.format.skill", "<light_purple>");
        this.indicatorIconSkill = config.getString("damage-indicator.icon.skill", "✦ ");
        this.indicatorFormatMinion = config.getString("damage-indicator.format.minion", "<dark_aqua>");
        this.indicatorIconMinion = config.getString("damage-indicator.icon.minion", "🐾 ");

        this.indicatorBackgroundColor = config.getString("damage-indicator.background-color", "#80000000");
        this.indicatorTemplate = config.getString("damage-indicator.template", "%icons%   %color%%damage%");
        this.indicatorDecimals = config.getInt("damage-indicator.decimals", 1);
        this.indicatorGravity = config.getDouble("damage-indicator.gravity", 0.04);
        this.indicatorInitialVelocity = config.getDouble("damage-indicator.initial-velocity", 0.25);
        
        elementalFormats.put("fire", config.getString("damage-indicator.format.fire"));
        elementalFormats.put("ice", config.getString("damage-indicator.format.ice"));
        elementalFormats.put("light", config.getString("damage-indicator.format.light"));
        elementalFormats.put("darkness", config.getString("damage-indicator.format.darkness"));
        elementalFormats.put("divine", config.getString("damage-indicator.format.divine"));
        
        elementalIcons.put("fire", config.getString("damage-indicator.icon.fire"));
        elementalIcons.put("ice", config.getString("damage-indicator.icon.ice"));
        elementalIcons.put("light", config.getString("damage-indicator.icon.light"));
        elementalIcons.put("darkness", config.getString("damage-indicator.icon.darkness"));
        elementalIcons.put("divine", config.getString("damage-indicator.icon.divine"));
        
        this.combatTagDuration = config.getLong("combat-tag.duration-seconds") * 1000L;
        
        this.staminaSprintDrain = config.getDouble("stamina.sprint-drain-per-check");
        this.staminaCheckInterval = config.getLong("stamina.check-interval-ticks");
        if (this.staminaCheckInterval <= 0) {
            this.staminaCheckInterval = 5;
        }
        
        this.defenseDivisor = config.getDouble("formulas.defense-divisor");
        if (this.defenseDivisor <= 0) {
            this.defenseDivisor = 100.0;
        }
        this.defenseScalingEnabled = config.getBoolean("formulas.defense-scaling.enabled", true);
        this.defenseScalingBase = config.getDouble("formulas.defense-scaling.base-divisor", 20.0);
        this.maxMitigation = config.getDouble("formulas.max-mitigation", 0.80);

        this.maxLevel = config.getInt("level.max-level", 100);
        
        // Requirements
        this.xpRequirementsBase = config.getDouble("level.requirements.base-xp", 150.0);
        this.xpRequirementsLinear = config.getDouble("level.requirements.linear-growth", 25.0);
        this.xpRequirementsExponential = config.getDouble("level.requirements.exponential-growth", 1.08);
        
        // XP Gain Core
        this.xpGainDefaultBase = config.getDouble("level.xp-gain.default-base", 20.0);
        
        // XP Gain Variance
        this.xpVarianceEnabled = config.getBoolean("level.xp-gain.variance.enabled", true);
        this.xpVarianceMin = config.getDouble("level.xp-gain.variance.min-factor", 0.8);
        this.xpVarianceMax = config.getDouble("level.xp-gain.variance.max-factor", 1.3);
        
        // XP Gain Scaling
        this.xpScalingMobLevelImpact = config.getBoolean("level.xp-gain.scaling.mob-level-impact", true);
        this.xpScalingMobLevelMultiplier = config.getDouble("level.xp-gain.scaling.mob-level-multiplier", 5.0);
        
        // XP Gain Disparity
        this.xpDisparityEnabled = config.getBoolean("level.xp-gain.disparity.enabled", true);
        
        this.xpPenaltyThreshold = config.getInt("level.xp-gain.disparity.penalty.threshold", 5);
        this.xpPenaltyReduction = config.getDouble("level.xp-gain.disparity.penalty.reduction", 0.15);
        this.xpPenaltyMinCap = config.getDouble("level.xp-gain.disparity.penalty.min-cap", 0.05);
        
        this.xpBonusThreshold = config.getInt("level.xp-gain.disparity.bonus.threshold", 0);
        this.xpBonusIncrement = config.getDouble("level.xp-gain.disparity.bonus.increment", 0.08);
        this.xpBonusMaxCap = config.getDouble("level.xp-gain.disparity.bonus.max-cap", 2.5);
        
        // Point Allocation Loading
        this.pointAllocation.clear();
        if (config.isConfigurationSection("point-allocation")) {
            for (String spentAttr : config.getConfigurationSection("point-allocation").getKeys(false)) {
                Map<String, Double> bonuses = new HashMap<>();
                if (config.isConfigurationSection("point-allocation." + spentAttr)) {
                    for (String targetAttr : config.getConfigurationSection("point-allocation." + spentAttr).getKeys(false)) {
                        bonuses.put(targetAttr.toLowerCase(), config.getDouble("point-allocation." + spentAttr + "." + targetAttr, 1.0));
                    }
                }
                pointAllocation.put(spentAttr.toLowerCase(), bonuses);
            }
        }

        // Attribute Scaling Loading
        this.strengthToPhysicalDamage = config.getDouble("scaling.strength.physical-damage", 0.5);
        this.intelligenceToMana = config.getDouble("scaling.intelligence.mana", 1.0);
        this.intelligenceToMagicDamage = config.getDouble("scaling.intelligence.magic-damage", 1.0);
        this.intelligenceToManaRegen = config.getDouble("scaling.intelligence.mana-regen", 0.1);
        this.dexterityToCritChance = config.getDouble("scaling.dexterity.crit-chance", 0.2);
        this.agilityToSpeed = config.getDouble("scaling.agility.speed", 0.1);
        this.agilityToDodge = config.getDouble("scaling.agility.dodge", 0.2);
        this.vitalityToHealth = config.getDouble("scaling.vitality.health", 5.0);
        this.vitalityToDefense = config.getDouble("scaling.vitality.defense", 1.0);
        
        // Formula Loading
        try {
            this.damageFormulaMode = ScalingMode.valueOf(config.getString("formulas.damage-mode", "ADDITIVE").toUpperCase());
        } catch (IllegalArgumentException e) {
            this.damageFormulaMode = ScalingMode.ADDITIVE;
            MidgardLogger.warn("Modo de fórmula de dano inválido na config. Usando ADDITIVE.");
        }
        this.baseHandDamage = config.getDouble("formulas.base-hand-damage", 1.0);
        this.strengthMultiplier = config.getDouble("formulas.multipliers.strength", 0.01);
        this.intelligenceMultiplier = config.getDouble("formulas.multipliers.intelligence", 0.01);

        // Absorption config
        this.absorptionEnabled = config.getBoolean("absorption.enabled", true);
        this.absorptionDecayPerSecond = config.getDouble("absorption.decay-per-second", 0.0);
        this.absorptionMaxPercent = config.getDouble("absorption.max-percent-of-health", 50.0);

        // DOT config
        this.dotReductionEnabled = config.getBoolean("dot.reduction-enabled", true);

        // Skill config
        this.skillDamageBonusEnabled = config.getBoolean("skill.damage-bonus-enabled", true);
        this.skillReductionEnabled = config.getBoolean("skill.reduction-enabled", true);

        // Minion config
        this.minionDamageBonusEnabled = config.getBoolean("minion.damage-bonus-enabled", true);
        this.minionReductionEnabled = config.getBoolean("minion.reduction-enabled", true);
        this.minionOwnerLifeSteal = config.getBoolean("minion.owner-life-steal", false);

        // Mechanics toggles
        this.dodgeEnabled = config.getBoolean("mechanics.dodge.enabled", true);
        this.parryEnabled = config.getBoolean("mechanics.parry.enabled", true);
        this.blockEnabled = config.getBoolean("mechanics.block.enabled", true);
        this.thornsEnabled = config.getBoolean("mechanics.thorns.enabled", true);
        this.lifeStealEnabled = config.getBoolean("mechanics.life-steal.enabled", true);
        this.criticalEnabled = config.getBoolean("mechanics.critical.enabled", true);

        // Formula configurability
        this.physicalFormula = config.getString("formulas.physical", "(base + weapon) * (1 + strength * multiplier)");
        this.magicalFormula = config.getString("formulas.magical", "(base + magic_damage) * (1 + intelligence * multiplier)");
        this.defenseFormula = config.getString("formulas.defense", "defense / (defense + divisor)");
        this.elementalFormula = config.getString("formulas.elemental", "defense / (defense + divisor)");

        this.elementalInteractionsEnabled = config.getBoolean("elemental-interactions.enabled", true);

        // Load Elemental Multipliers
        elementalMultipliers.clear();
        if (config.isConfigurationSection("elemental-interactions")) {
            for (String attackerElement : config.getConfigurationSection("elemental-interactions").getKeys(false)) {
                if (attackerElement.equals("enabled")) {
                    continue;
                }
                Map<String, Double> victims = new HashMap<>();
                for (String victimElement : config.getConfigurationSection("elemental-interactions." + attackerElement).getKeys(false)) {
                    victims.put(victimElement.toLowerCase(), config.getDouble("elemental-interactions." + attackerElement + "." + victimElement));
                }
                elementalMultipliers.put(attackerElement.toLowerCase(), victims);
            }
        }
    }

    public enum ScalingMode { ADDITIVE, MULTIPLICATIVE }
    public ScalingMode damageFormulaMode = ScalingMode.ADDITIVE; // Default
    public double baseHandDamage = 1.0;
    public double intelligenceMultiplier = 0.01;
    public double strengthMultiplier = 0.01;
    
    // Settings
    /** Se os indicadores de dano (hologramas) estão ativados. */
    public boolean indicatorEnabled;
    /** Duração em ticks que o indicador de dano permanece visível. */
    public int indicatorDuration;
    /** Formato de cor/estilo para dano normal. */
    public String indicatorFormatNormal;
    /** Formato de cor/estilo para dano físico. */
    public String indicatorFormatPhysical; // Novo
    /** Formato de cor/estilo para dano crítico. */
    public String indicatorFormatCritical;
    /** Ícone exibido para dano crítico. */
    public String indicatorIconCritical;
    /** Ícone exibido para quando o jogador usa uma arma. */
    public String indicatorIconWeapon;
    /** Ícone exibido para dano físico. */
    public String indicatorIconPhysical; // Novo
    /** Ícone exibido para dano de projétil. */
    public String indicatorIconProjectile;
    /** Ícone exibido para dano mágico. */
    public String indicatorIconMagical;
    /** Ícone exibido para dano ambiental. */
    public String indicatorIconEnvironment;
    /** Ícone exibido para dano verdadeiro. */
    public String indicatorIconTrue;
    /** Ícone exibido para dano em área (AoE). */
    public String indicatorIconAoE;
    
    /** Formato de cor para dano mágico. */
    public String indicatorFormatMagical;
    /** Formato de cor para dano de projétil. */
    public String indicatorFormatProjectile;
    /** Formato de cor para dano ambiental. */
    public String indicatorFormatEnvironment;
    /** Formato de cor para dano verdadeiro. */
    public String indicatorFormatTrue;
    /** Formato de cor para dano em área (AoE). */
    public String indicatorFormatAoE;

    // DOT / Skill / Minion indicator fields
    public String indicatorFormatDot;
    public String indicatorIconDot;
    public String indicatorFormatSkill;
    public String indicatorIconSkill;
    public String indicatorFormatMinion;
    public String indicatorIconMinion;

    /** Cor de fundo do indicador de dano (formato ARGB Hex). */
    public String indicatorBackgroundColor;
    /** Template da mensagem do indicador. Suporta %icons%, %color%, %damage%. */
    public String indicatorTemplate; // Novo: "%icons% %color%%damage%"
    /** Número de casas decimais para exibir no valor do dano. */
    public int indicatorDecimals; // Novo
    /** Gravidade aplicada ao movimento do indicador. */
    public double indicatorGravity; // Novo
    /** Velocidade inicial vertical do indicador. */
    public double indicatorInitialVelocity; // Novo
    
    /** Mapas de formatos e ícones para danos elementais. */
    public Map<String, String> elementalFormats = new HashMap<>();
    public Map<String, String> elementalIcons = new HashMap<>();
    public Map<String, Map<String, Double>> elementalMultipliers = new HashMap<>();
    public boolean elementalInteractionsEnabled;
    
    /** Duração do estado de combate em segundos. */
    public long combatTagDuration;
    
    /** Quantidade de stamina drenada por verificação ao correr. */
    public double staminaSprintDrain;
    /** Intervalo em ticks entre verificações de stamina ao correr. */
    public long staminaCheckInterval;
    
    /** Divisor usado na fórmula de defesa para cálculo de redução de dano. */
    public double defenseDivisor;
    /** Se a defesa escala com o nível do atacante. */
    public boolean defenseScalingEnabled;
    /** Divisor base para o escalonamento de defesa. */
    public double defenseScalingBase;
    /** Limite máximo de mitigação de dano (0.0 a 1.0). */
    public double maxMitigation;

    // Level System (UPDATED)
    /** Nível máximo alcançável. */
    public int maxLevel;
    
    // Requirements
    public double xpRequirementsBase;
    public double xpRequirementsLinear;
    public double xpRequirementsExponential;
    
    // XP Gain - Core
    public double xpGainDefaultBase;
    
    // XP Gain - Variance (RNG)
    public boolean xpVarianceEnabled;
    public double xpVarianceMin;
    public double xpVarianceMax;
    
    // XP Gain - Scaling
    public boolean xpScalingMobLevelImpact;
    public double xpScalingMobLevelMultiplier;
    
    // XP Gain - Disparity
    public boolean xpDisparityEnabled;
    
    public int xpPenaltyThreshold;
    public double xpPenaltyReduction;
    public double xpPenaltyMinCap;
    
    public int xpBonusThreshold;
    public double xpBonusIncrement;
    public double xpBonusMaxCap;

    // Point Allocation: maps spent attribute -> { affected attribute -> value per point }
    public Map<String, Map<String, Double>> pointAllocation = new HashMap<>();

    // Attribute Scaling
    public double strengthToPhysicalDamage;
    public double intelligenceToMana;
    public double intelligenceToMagicDamage;
    public double intelligenceToManaRegen;
    public double dexterityToCritChance;
    public double agilityToSpeed;
    public double agilityToDodge;
    public double vitalityToHealth;
    public double vitalityToDefense;

    // Absorption System
    public boolean absorptionEnabled;
    public double absorptionDecayPerSecond;
    public double absorptionMaxPercent;

    // DOT System
    public boolean dotReductionEnabled;

    // Skill System
    public boolean skillDamageBonusEnabled;
    public boolean skillReductionEnabled;

    // Minion System
    public boolean minionDamageBonusEnabled;
    public boolean minionReductionEnabled;
    public boolean minionOwnerLifeSteal;

    // Mechanics On/Off Toggles
    public boolean dodgeEnabled;
    public boolean parryEnabled;
    public boolean blockEnabled;
    public boolean thornsEnabled;
    public boolean lifeStealEnabled;
    public boolean criticalEnabled;

    // Formula text (for reference / advanced config)
    public String physicalFormula;
    public String magicalFormula;
    public String defenseFormula;
    public String elementalFormula;

    /**
     * Define os valores padrão para a configuração caso não existam.
     */
    private void loadDefaults() {
        // Padrões correspondem ao arquivo resources/modules/combat.yml
        config.addDefault("damage-indicator.enabled", true);
        config.addDefault("damage-indicator.duration-ticks", 18);
        config.addDefault("damage-indicator.format.normal", "<gray>");
        config.addDefault("damage-indicator.format.physical", "<red>");
        config.addDefault("damage-indicator.format.critical", "<red><bold>");
        config.addDefault("damage-indicator.icon.critical", "⚔"); // Apenas a espada
        config.addDefault("damage-indicator.icon.weapon", "🗡 "); // Ícone de arma
        config.addDefault("damage-indicator.icon.physical", "✘ "); // Ícone físico (soco/impacto)
        config.addDefault("damage-indicator.icon.projectile", "🏹 ");
        config.addDefault("damage-indicator.icon.magical", "✩ ");
        config.addDefault("damage-indicator.icon.environment", "🌲 ");
        config.addDefault("damage-indicator.icon.true", "🛡 ");
        config.addDefault("damage-indicator.icon.aoe", "☀ "); // Ícone de sol para AoE
        
        config.addDefault("damage-indicator.format.magical", "<blue>");
        config.addDefault("damage-indicator.format.projectile", "<green>");
        config.addDefault("damage-indicator.format.environment", "<dark_green>");
        config.addDefault("damage-indicator.format.true", "<white>");
        config.addDefault("damage-indicator.format.aoe", "<yellow>");

        config.addDefault("damage-indicator.background-color", "#80000000"); // 50% Preto
        config.addDefault("damage-indicator.template", "%icons% %color%%damage%");
        config.addDefault("damage-indicator.decimals", 1);
        config.addDefault("damage-indicator.gravity", 0.04);
        config.addDefault("damage-indicator.initial-velocity", 0.25);
        
        config.addDefault("damage-indicator.format.fire", "<red>");
        config.addDefault("damage-indicator.format.ice", "<aqua>");
        config.addDefault("damage-indicator.format.light", "<yellow>");
        config.addDefault("damage-indicator.format.darkness", "<dark_purple>");
        config.addDefault("damage-indicator.format.divine", "<gold>");

        // DOT / Skill / Minion indicator defaults
        config.addDefault("damage-indicator.format.dot", "<green>");
        config.addDefault("damage-indicator.icon.dot", "☣ ");
        config.addDefault("damage-indicator.format.skill", "<light_purple>");
        config.addDefault("damage-indicator.icon.skill", "✦ ");
        config.addDefault("damage-indicator.format.minion", "<dark_aqua>");
        config.addDefault("damage-indicator.icon.minion", "🐾 ");
        
        config.addDefault("damage-indicator.icon.fire", "🔥 ");
        config.addDefault("damage-indicator.icon.ice", "❄ ");
        config.addDefault("damage-indicator.icon.light", "⚡ ");
        config.addDefault("damage-indicator.icon.darkness", "☠ ");
        config.addDefault("damage-indicator.icon.divine", "☀ ");
        
        config.addDefault("combat-tag.duration-seconds", 10);
        
        config.addDefault("stamina.sprint-drain-per-check", 2.0);
        config.addDefault("stamina.check-interval-ticks", 5);
        
        config.addDefault("formulas.defense-divisor", 100.0);
        config.addDefault("formulas.defense-scaling.enabled", true);
        config.addDefault("formulas.defense-scaling.base-divisor", 20.0);
        config.addDefault("formulas.max-mitigation", 0.80);

        // Padrões de Nível (paths must match loadSettings keys)
        config.addDefault("level.max-level", 100);
        config.addDefault("level.requirements.base-xp", 150.0);
        config.addDefault("level.requirements.linear-growth", 25.0);
        config.addDefault("level.requirements.exponential-growth", 1.08);
        config.addDefault("level.xp-gain.default-base", 20.0);
        
        // Attribute Scaling Defaults
        config.addDefault("scaling.strength.physical-damage", 0.5); // 10 STR = +5 DMG
        config.addDefault("scaling.intelligence.mana", 1.0);
        config.addDefault("scaling.intelligence.magic-damage", 1.0);
        config.addDefault("scaling.intelligence.mana-regen", 0.1);
        config.addDefault("scaling.dexterity.crit-chance", 0.2); // 10 DEX = +2% Crit
        config.addDefault("scaling.agility.speed", 0.1); // 10 AGI = +1 Speed
        config.addDefault("scaling.agility.dodge", 0.2); // 10 AGI = +2% Dodge
        config.addDefault("scaling.vitality.health", 5.0); // 10 VIT = +50 HP
        config.addDefault("scaling.vitality.defense", 1.0); // 10 VIT = +10 DEF

        // Formula Defaults
        config.addDefault("formulas.damage-mode", "ADDITIVE"); // ADDITIVE or MULTIPLICATIVE
        config.addDefault("formulas.base-hand-damage", 1.0);
        config.addDefault("formulas.multipliers.strength", 0.01); // 1% per STR
        config.addDefault("formulas.multipliers.intelligence", 0.01); // 1% per INT
        
        // Formula text (reference for admins — not evaluated, just documentation in-config)
        config.addDefault("formulas.physical", "(base + weapon) * (1 + strength * multiplier)");
        config.addDefault("formulas.magical", "(base + magic_damage) * (1 + intelligence * multiplier)");
        config.addDefault("formulas.defense", "defense / (defense + divisor)");
        config.addDefault("formulas.elemental", "defense / (defense + divisor)");

        // Absorption Defaults
        config.addDefault("absorption.enabled", true);
        config.addDefault("absorption.decay-per-second", 0.0);
        config.addDefault("absorption.max-percent-of-health", 50.0);

        // DOT Defaults
        config.addDefault("dot.reduction-enabled", true);

        // Skill Defaults
        config.addDefault("skill.damage-bonus-enabled", true);
        config.addDefault("skill.reduction-enabled", true);

        // Minion Defaults
        config.addDefault("minion.damage-bonus-enabled", true);
        config.addDefault("minion.reduction-enabled", true);
        config.addDefault("minion.owner-life-steal", false);

        // Mechanics Toggles
        config.addDefault("mechanics.dodge.enabled", true);
        config.addDefault("mechanics.parry.enabled", true);
        config.addDefault("mechanics.block.enabled", true);
        config.addDefault("mechanics.thorns.enabled", true);
        config.addDefault("mechanics.life-steal.enabled", true);
        config.addDefault("mechanics.critical.enabled", true);

        // Elemental Relationships (Attacker -> Victim)
        // Fire > Ice (1.5x)
        // Ice > Fire (1.5x) - Mutual weakness or maybe Water > Fire
        // Light > Darkness (1.5x)
        // Darkness > Light (1.5x)
        // Divine > Undead (2.0x) - Special case
        
        config.addDefault("elemental-interactions.enabled", true);

        if (!config.contains("elemental-interactions.fire")) {
            config.set("elemental-interactions.fire.ice", 1.5);
            config.set("elemental-interactions.ice.fire", 1.5); // Melting?
            config.set("elemental-interactions.light.darkness", 1.5);
            config.set("elemental-interactions.darkness.light", 1.5);
            config.set("elemental-interactions.divine.undead", 2.0);
            config.set("elemental-interactions.divine.darkness", 1.5);
            // Only save to disk if loading from file (not DB)
            if (repository == null || repository.count() == 0) {
                configWrapper.saveConfig();
            }
        }
        
        config.options().copyDefaults(true);
    }

    /**
     * Obtém a XP base configurada para um tipo de entidade específico.
     * Procura em 'level.xp-gain.mobs.<ENTITY_TYPE>'.
     *
     * @param type O tipo de entidade.
     * @return O valor de XP base ou o padrão se não configurado.
     */
    public double getMobExperience(org.bukkit.entity.EntityType type) {
        if (type == null) {
            return xpGainDefaultBase;
        }
        // Permite configurar XP específico por mob, ex: level.xp-gain.mobs.ZOMBIE: 50.0
        return config.getDouble("level.xp-gain.mobs." + type.name(), xpGainDefaultBase);
    }
}

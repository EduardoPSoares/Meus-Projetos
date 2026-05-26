package me.ray.midgard.modules.spells.manager;

import me.ray.midgard.core.database.DefinitionRepository;
import me.ray.midgard.core.database.DefinitionMigrationTool;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.api.skills.SkillTrigger;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.AttributeModifier;
import me.ray.midgard.core.attribute.AttributeOperation;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.data.SpellMilestone;
import me.ray.midgard.modules.spells.data.SpellProfile;
import me.ray.midgard.modules.spells.data.SpellSound;
import me.ray.midgard.modules.spells.obj.ScalableAttribute;
import me.ray.midgard.modules.spells.obj.Spell;
import me.ray.midgard.modules.spells.obj.SpellType;
import me.ray.midgard.modules.spells.requirement.*;
import me.ray.midgard.modules.spells.task.ChannelingTask;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpellManager {

    private final SpellsModule module;
    private final Map<String, Spell> loadedSpells = new ConcurrentHashMap<>();
    private final Set<UUID> castingModePlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> comboActivePlayers = ConcurrentHashMap.newKeySet();

    private final Map<UUID, Integer> castingAnchors = new ConcurrentHashMap<>();

    private final Map<Integer, String> defaultCombos = new ConcurrentHashMap<>();
    private final Map<UUID, ChannelingTask> channelingTasks = new ConcurrentHashMap<>();

    private SpellXPManager xpManager;

    private static final Pattern ATTR_PATTERN = Pattern.compile("%attr:([a-zA-Z_]+)%");

    private static final String[] DEFAULT_SPELL_FILES = {
        "agregar.yml", "alma_corrompida.yml", "apocalipse_arcano.yml", "armadilha.yml",
        "assassinato.yml", "aura_sagrada.yml", "barreira_absoluta.yml", "bastiao_imortal.yml",
        "bencao_divina.yml", "bloquear.yml", "bola_de_fogo.yml", "ceifador_de_almas.yml",
        "chute_giratorio.yml", "chuva_de_flechas.yml", "contra_ataque.yml", "corpo_de_ferro.yml",
        "corte_giratorio.yml", "corte_profundo.yml", "curar.yml", "danca_das_laminas.yml",
        "despertar_do_chi.yml", "drenar_vida.yml", "emboscada.yml", "enxame_de_almas.yml",
        "escudo_de_fe.yml", "escudo_divino.yml", "escudo_magico.yml", "escudo_sagrado.yml",
        "esmagar.yml", "evasao.yml", "exercito_dos_mortos.yml", "explosao_arcana.yml",
        "explosao_necrotica.yml", "flecha_certeira.yml", "flecha_do_destino.yml",
        "flecha_envenenada.yml", "flecha_explosiva.yml", "fluxo_de_mana.yml",
        "fluxo_interior.yml", "fortaleza_interior.yml", "furia_berserker.yml",
        "furia_guerreira.yml", "golpe_brutal.yml", "golpe_critico.yml", "golpe_devastador.yml",
        "golpe_do_dragao.yml", "grito_de_guerra.yml", "investida.yml", "invocar_esqueleto.yml",
        "julgamento_divino.yml", "luz_sagrada.yml", "maldicao.yml", "meteoro.yml",
        "mil_punhos.yml", "muralha_de_aco.yml", "olho_de_aguia.yml", "pacto_sombrio.yml",
        "palma_de_buda.yml", "passo_do_vento.yml", "passo_sombrio.yml", "prece_protetora.yml",
        "proteger_aliado.yml", "provocar.yml", "punhalada.yml", "purificar.yml",
        "raio_glacial.yml", "rajada_de_golpes.yml", "reflexos_rapidos.yml", "relampago.yml",
        "renovar.yml", "resistencia_marcial.yml", "ressurreicao.yml", "sabedoria_arcana.yml",
        "soco_trovao.yml", "sombras_furtivas.yml", "tempestade_de_flechas.yml",
        "tempestade_elemental.yml", "tiro_perfurante.yml", "toque_da_morte.yml",
        "veneno_mortal.yml"
    };

    public SpellManager(SpellsModule module) {
        this.module = module;
        this.xpManager = new SpellXPManager(module);
        loadDefaultCombos();
    }

    // ==================== XP MANAGER ====================

    public SpellXPManager getXPManager() {
        return xpManager;
    }

    // ==================== COMBOS ====================

    private void loadDefaultCombos() {
        defaultCombos.put(1, "RRR");
        defaultCombos.put(2, "RLR");
        defaultCombos.put(3, "RRL");
        defaultCombos.put(4, "RLL");

        if (module.getConfig() != null) {
            ConfigurationSection section = module.getConfig().getConfigurationSection("combos");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(key);
                        String seq = section.getString(key);
                        if (seq != null) {
                            defaultCombos.put(slot, seq.toUpperCase());
                        }
                    } catch (NumberFormatException e) {
                        MidgardLogger.warn("Invalid combo slot in config: " + key);
                    }
                }
            }
        }
    }

    public void reloadCombos() {
        defaultCombos.clear();
        loadDefaultCombos();
    }

    public String getDefaultCombo(int slot) {
        return defaultCombos.getOrDefault(slot, "NONE");
    }

    public SpellsModule getModule() {
        return module;
    }

    // ==================== CASTING MODE ====================

    public void toggleCastingMode(Player player) {
        if (castingModePlayers.contains(player.getUniqueId())) {
            disableCastingMode(player);
        } else {
            enableCastingMode(player);
        }
    }

    public void enableCastingMode(Player player) {
        castingModePlayers.add(player.getUniqueId());

        int anchor = player.getInventory().getHeldItemSlot();
        castingAnchors.put(player.getUniqueId(), anchor);

        // Sinaliza para CombatOverlay parar de enviar action bar
        player.setMetadata("midgard_casting_mode", new FixedMetadataValue(module.getPlugin(), true));

        player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1f, 1.5f);

        String enableMsg = module.getMessage("casting.mode_enabled");
        MessageUtils.send(player, enableMsg);
    }

    public void disableCastingMode(Player player) {
        castingModePlayers.remove(player.getUniqueId());
        castingAnchors.remove(player.getUniqueId());
        comboActivePlayers.remove(player.getUniqueId());

        // Remove sinalizações para CombatOverlay voltar a enviar action bar
        player.removeMetadata("midgard_casting_mode", module.getPlugin());
        player.removeMetadata("midgard_combo_active", module.getPlugin());

        player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1f, 0.5f);
        MessageUtils.sendActionBar(player, "");

        String disableMsg = module.getMessage("casting.mode_disabled");
        MessageUtils.send(player, disableMsg);
    }

    public boolean isCastingMode(Player player) {
        return castingModePlayers.contains(player.getUniqueId());
    }

    public Set<UUID> getCastingPlayers() {
        return Collections.unmodifiableSet(castingModePlayers);
    }

    public boolean isComboActive(Player player) {
        return comboActivePlayers.contains(player.getUniqueId());
    }

    public void setComboActive(UUID uuid, boolean active) {
        if (active) {
            comboActivePlayers.add(uuid);
        } else {
            comboActivePlayers.remove(uuid);
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            if (active) {
                player.setMetadata("midgard_combo_active", new FixedMetadataValue(module.getPlugin(), true));
            } else {
                player.removeMetadata("midgard_combo_active", module.getPlugin());
            }
        }
    }

    // ==================== SKILL BAR ====================

    public boolean isAnchorSlot(Player player, int visualSlotIndex) {
        Integer anchor = castingAnchors.get(player.getUniqueId());
        return anchor != null && anchor == visualSlotIndex;
    }

    public void castSkillBar(Player player, int slot) {
        SpellProfile profile = getProfile(player);
        if (profile == null) { return; }

        String spellId = profile.getSkillInSlot(slot);
        if (spellId == null) { return; }

        castSpell(player, spellId);
    }

    public String getSkillInVirtualSlot(Player player, int visualSlotIndex) {
        SpellProfile profile = getProfile(player);
        if (profile == null) { return null; }

        Integer anchor = castingAnchors.get(player.getUniqueId());
        int logicalSlot;

        if (anchor == null) {
            logicalSlot = visualSlotIndex + 1;
        } else {
            if (visualSlotIndex == anchor) { return null; }
            
            // Adjust for anchor
            if (visualSlotIndex > anchor) {
                logicalSlot = visualSlotIndex; 
            } else {
                logicalSlot = visualSlotIndex + 1;
            }
        }

        // Slot 5 reservado para Ultimate
        if (logicalSlot == 5) {
             return profile.getEquippedUltimate();
        }

        return profile.getSkillInSlot(logicalSlot);
    }

    // ==================== SPELL LOADING ====================

    public void loadSpells() {
        loadedSpells.clear();

        // Try loading from database
        DefinitionRepository repo = module.getRepository();
        if (repo != null) {
            // Seed any missing default spells into DB from JAR
            seedMissingDefaultSpells(repo);
            // Load all spells from DB
            loadSpellsFromDatabase(repo);
            return;
        }

        // Fallback: load from YAML files (no database available)
        loadSpellsFromYaml();
    }

    private void loadSpellsFromDatabase(DefinitionRepository repo) {
        // Load default sounds from config
        SpellSound defaultCastStart = parseSound(module.getConfig(), "defaults.sounds.cast-start", SpellSound.DEFAULT_CAST_START);
        SpellSound defaultCastFinish = parseSound(module.getConfig(), "defaults.sounds.cast-finish", SpellSound.DEFAULT_CAST_FINISH);
        SpellSound defaultCastFail = parseSound(module.getConfig(), "defaults.sounds.cast-fail", SpellSound.DEFAULT_CAST_FAIL);
        double defaultInterruptThreshold = module.getConfig() != null
                ? module.getConfig().getDouble("defaults.interrupt-threshold", 0.0)
                : 0.0;

        Map<String, DefinitionRepository.DefinitionData> dbSpells = repo.loadAll();
        for (Map.Entry<String, DefinitionRepository.DefinitionData> entry : dbSpells.entrySet()) {
            try {
                org.bukkit.configuration.file.YamlConfiguration config =
                    (org.bukkit.configuration.file.YamlConfiguration) DefinitionMigrationTool.deserializeToConfig(entry.getValue().yamlData());
                if (config == null) { continue; }
                Spell spell = parseSpellFromConfig(entry.getKey(), config,
                    defaultCastStart, defaultCastFinish, defaultCastFail, defaultInterruptThreshold);
                if (spell != null) { loadedSpells.put(entry.getKey(), spell); }
            } catch (Exception e) {
                MidgardLogger.error("Erro ao carregar spell " + entry.getKey() + " do banco", e);
            }
        }
        MidgardLogger.info("Loaded " + loadedSpells.size() + " spells from database.");
    }

    private void loadSpellsFromYaml() {
        File moduleFolder = new File(module.getPlugin().getDataFolder(), "modules/spells");
        File spellsFolder = new File(moduleFolder, "spells");

        if (!spellsFolder.exists()) {
            spellsFolder.mkdirs();
        }

        // Extract all default spells from resources
        for (String spell : DEFAULT_SPELL_FILES) {
            createDefaultSpell(spellsFolder, spell);
        }

        List<File> files = new ArrayList<>();
        collectYamlFiles(spellsFolder, files);
        if (files.isEmpty()) { return; }

        // Load default sounds from config
        SpellSound defaultCastStart = parseSound(module.getConfig(), "defaults.sounds.cast-start", SpellSound.DEFAULT_CAST_START);
        SpellSound defaultCastFinish = parseSound(module.getConfig(), "defaults.sounds.cast-finish", SpellSound.DEFAULT_CAST_FINISH);
        SpellSound defaultCastFail = parseSound(module.getConfig(), "defaults.sounds.cast-fail", SpellSound.DEFAULT_CAST_FAIL);
        double defaultInterruptThreshold = module.getConfig() != null
                ? module.getConfig().getDouble("defaults.interrupt-threshold", 0.0)
                : 0.0;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = file.getName().replace(".yml", "").toLowerCase();
            Spell spell = parseSpellFromConfig(id, config,
                defaultCastStart, defaultCastFinish, defaultCastFail, defaultInterruptThreshold);
            if (spell != null) { loadedSpells.put(id, spell); }
        }

        MidgardLogger.info("Loaded " + loadedSpells.size() + " spells from " + spellsFolder.getPath());
    }

    /**
     * Parses a single Spell from a config section.
     */
    private Spell parseSpellFromConfig(String id, ConfigurationSection config,
            SpellSound defaultCastStart, SpellSound defaultCastFinish,
            SpellSound defaultCastFail, double defaultInterruptThreshold) {
        String name = config.getString("name", id);
        String mythicSkill = config.getString("mythic-skill");

        if (mythicSkill == null || mythicSkill.isEmpty()) {
            MidgardLogger.warn("Spell '" + id + "' sem mythic-skill definida. Ignorando.");
            return null;
        }

        boolean mythicSkillValid = false;
        try {
            mythicSkillValid = MythicProvider.get().getSkillManager().getSkill(mythicSkill).isPresent();
        } catch (Throwable e) {
            MidgardLogger.error("Erro ao verificar spell '" + id + "': MythicMobs nao disponivel. " + e.getMessage());
            return null;
        }

        if (!mythicSkillValid) {
            MidgardLogger.error("Erro critico ao carregar spell '" + id + "': A skill MythicMobs '" + mythicSkill + "' nao foi encontrada.");
            return null;
        }

        SpellType spellType = SpellType.fromString(config.getString("spell-type", "common"));

        double castTime = config.getDouble("cast-time", 0.0);
        boolean interruptible = config.getBoolean("interruptible", true);
        int maxLevel = config.getInt("max-level", 10);

        List<String> lore = config.contains("description")
                ? config.getStringList("description")
                : config.getStringList("lore");

        List<String> lockedLore = config.contains("description-locked")
                ? config.getStringList("description-locked")
                : config.getStringList("lore-locked");

        List<SpellRequirement> requirements = parseRequirements(config);

        // Icon
        String iconMaterial = null;
        String iconMaterialLocked = null;
        int iconModelData = 0;
        int iconModelDataLocked = 0;

        if (config.isConfigurationSection("icon")) {
            ConfigurationSection iconSec = config.getConfigurationSection("icon");
            iconMaterial = iconSec.getString("material", null);
            iconMaterialLocked = iconSec.getString("material-locked", null);
            iconModelData = iconSec.getInt("model-data", 0);
            iconModelDataLocked = iconSec.getInt("model-data-locked", 0);
        } else if (config.contains("icon")) {
            iconMaterial = config.getString("icon", null);
        }

        // New features parsing
        List<SpellMilestone> milestones = parseMilestones(config);
        Map<String, Double> masteryBonuses = parseMasteryBonuses(config);
        double interruptThreshold = config.getDouble("interrupt-threshold", defaultInterruptThreshold);
        SpellSound castStart = parseSound(config, "sounds.cast-start", defaultCastStart);
        SpellSound castFinish = parseSound(config, "sounds.cast-finish", defaultCastFinish);
        SpellSound castFail = parseSound(config, "sounds.cast-fail", defaultCastFail);

        return new Spell(id, mythicSkill, name, spellType, lore, lockedLore,
                parseAttribute(config, "cooldown"),
                parseAttribute(config, "mana"),
                parseAttribute(config, "stamina"),
                parseVariables(config),
                requirements, castTime, interruptible,
                iconMaterial, iconMaterialLocked, iconModelData, iconModelDataLocked,
                maxLevel,
                milestones, masteryBonuses, interruptThreshold,
                castStart, castFinish, castFail);
    }

    /**
     * Recarrega um spell específico a partir de dados do banco.
     */
    public void reloadSpellFromDb(String spellId, DefinitionRepository.DefinitionData data) {
        try {
            org.bukkit.configuration.file.YamlConfiguration config =
                (org.bukkit.configuration.file.YamlConfiguration) DefinitionMigrationTool.deserializeToConfig(data.yamlData());
            if (config == null) { return; }
            SpellSound defaultCastStart = parseSound(module.getConfig(), "defaults.sounds.cast-start", SpellSound.DEFAULT_CAST_START);
            SpellSound defaultCastFinish = parseSound(module.getConfig(), "defaults.sounds.cast-finish", SpellSound.DEFAULT_CAST_FINISH);
            SpellSound defaultCastFail = parseSound(module.getConfig(), "defaults.sounds.cast-fail", SpellSound.DEFAULT_CAST_FAIL);
            double defaultInterruptThreshold = module.getConfig() != null
                    ? module.getConfig().getDouble("defaults.interrupt-threshold", 0.0) : 0.0;
            Spell spell = parseSpellFromConfig(spellId, config,
                defaultCastStart, defaultCastFinish, defaultCastFail, defaultInterruptThreshold);
            if (spell != null) { loadedSpells.put(spellId, spell); }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao recarregar spell " + spellId + " do banco", e);
        }
    }

    /**
     * Remove um spell do registro em memória.
     */
    public void unregisterSpell(String spellId) {
        loadedSpells.remove(spellId);
    }

    /**
     * Seeds any missing default spells from JAR resources into the database.
     */
    private void seedMissingDefaultSpells(DefinitionRepository repo) {
        Map<String, DefinitionRepository.DefinitionData> existing = repo.loadAll();
        Set<String> existingIds = existing.keySet();

        int count = 0;
        for (String fileName : DEFAULT_SPELL_FILES) {
            String id = fileName.replace(".yml", "").toLowerCase();
            if (existingIds.contains(id)) { continue; }

            String resourcePath = "modules/spells/spells/" + fileName;
            try (java.io.InputStream is = module.getPlugin().getResource(resourcePath)) {
                if (is == null) { continue; }
                YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
                String yamlData = config.saveToString();
                repo.save(id, "spell", yamlData, "default-seed").join();
                count++;
            } catch (Exception e) {
                MidgardLogger.warn("Erro ao inserir spell padrão: " + id + " - " + e.getMessage());
            }
        }

        if (count > 0) {
            MidgardLogger.info("[Spells] " + count + " spells padrão inseridas no banco.");
        }
    }

    private void collectYamlFiles(File folder, List<File> result) {
        File[] entries = folder.listFiles();
        if (entries == null) { return; }
        for (File entry : entries) {
            if (entry.isDirectory()) {
                collectYamlFiles(entry, result);
            } else if (entry.getName().endsWith(".yml")) {
                result.add(entry);
            }
        }
    }

    private ScalableAttribute parseAttribute(ConfigurationSection config, String path) {
        if (config.isConfigurationSection(path)) {
            ConfigurationSection section = config.getConfigurationSection(path);
            double base = section.getDouble("base", 0.0);
            double perLevel = section.getDouble("per-level", 0.0);
            return new ScalableAttribute(base, perLevel);
        } else {
            return ScalableAttribute.of(config.getDouble(path, 0.0));
        }
    }

    private Map<String, Object> parseVariables(ConfigurationSection config) {
        Map<String, Object> variables = new HashMap<>();
        ConfigurationSection varSection = config.getConfigurationSection("variables");
        if (varSection != null) {
            for (String key : varSection.getKeys(false)) {
                if (varSection.isConfigurationSection(key)) {
                    ConfigurationSection sub = varSection.getConfigurationSection(key);
                    double base = sub.getDouble("base", 0.0);
                    double perLevel = sub.getDouble("per-level", 0.0);
                    variables.put(key, new ScalableAttribute(base, perLevel));
                } else {
                    Object val = varSection.get(key);
                    if (val instanceof Number) {
                        variables.put(key, ScalableAttribute.of(((Number) val).doubleValue()));
                    } else if (val != null) {
                        variables.put(key, val.toString());
                    }
                }
            }
        }
        return variables;
    }

    private List<SpellRequirement> parseRequirements(ConfigurationSection config) {
        List<SpellRequirement> requirements = new ArrayList<>();
        if (config.isConfigurationSection("requirements")) {
            ConfigurationSection reqSec = config.getConfigurationSection("requirements");
            if (reqSec.contains("level")) {
                requirements.add(new LevelRequirement(reqSec.getInt("level")));
            }
            if (reqSec.contains("class")) {
                if (reqSec.isList("class")) {
                    requirements.add(new ClassRequirement(reqSec.getStringList("class")));
                } else {
                    requirements.add(new ClassRequirement(reqSec.getString("class")));
                }
            }
            if (reqSec.contains("permission")) {
                requirements.add(new PermissionRequirement(reqSec.getString("permission")));
            }
            if (reqSec.isConfigurationSection("attribute")) {
                for (String attr : reqSec.getConfigurationSection("attribute").getKeys(false)) {
                    requirements.add(new AttributeRequirement(attr, reqSec.getInt("attribute." + attr)));
                }
            }
        }
        return requirements;
    }

    private List<SpellMilestone> parseMilestones(ConfigurationSection config) {
        List<SpellMilestone> milestones = new ArrayList<>();
        ConfigurationSection milestonesSection = config.getConfigurationSection("milestones");
        if (milestonesSection == null) { return milestones; }

        for (String key : milestonesSection.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                ConfigurationSection ms = milestonesSection.getConfigurationSection(key);
                if (ms == null) { continue; }

                String visual = ms.getString("visual", null);
                Map<String, Double> statBonuses = new HashMap<>();
                if (ms.isConfigurationSection("stat-bonus")) {
                    ConfigurationSection statSec = ms.getConfigurationSection("stat-bonus");
                    for (String stat : statSec.getKeys(false)) {
                        statBonuses.put(stat, statSec.getDouble(stat));
                    }
                }
                String mechanic = ms.getString("mechanic-override", null);

                milestones.add(new SpellMilestone(level, visual, statBonuses, mechanic));
            } catch (NumberFormatException e) {
                MidgardLogger.warn("Invalid milestone level key: " + key);
            }
        }

        // Sort by level ascending
        milestones.sort(Comparator.comparingInt(SpellMilestone::level));
        return milestones;
    }

    private Map<String, Double> parseMasteryBonuses(ConfigurationSection config) {
        Map<String, Double> bonuses = new HashMap<>();
        ConfigurationSection mastery = config.getConfigurationSection("mastery");
        if (mastery == null) { return bonuses; }

        ConfigurationSection attrBonus = mastery.getConfigurationSection("attribute-bonus");
        if (attrBonus != null) {
            for (String attr : attrBonus.getKeys(false)) {
                bonuses.put(attr, attrBonus.getDouble(attr));
            }
        }
        return bonuses;
    }

    private SpellSound parseSound(ConfigurationSection config, String path, SpellSound defaultSound) {
        if (config == null) { return defaultSound; }
        ConfigurationSection soundSection = config.getConfigurationSection(path);
        if (soundSection == null) { return defaultSound; }

        String sound = soundSection.getString("sound", defaultSound.sound());
        float volume = (float) soundSection.getDouble("volume", defaultSound.volume());
        float pitch = (float) soundSection.getDouble("pitch", defaultSound.pitch());
        return new SpellSound(sound, volume, pitch);
    }

    private void createDefaultSpell(File folder, String fileName) {
        File file = new File(folder, fileName);
        if (!file.exists()) {
            try {
                module.getPlugin().saveResource("modules/spells/spells/" + fileName, false);
            } catch (Exception e) {
                MidgardLogger.warn("Could not save default spell " + fileName + ": " + e.getMessage());
            }
        }
    }

    // ==================== SPELL GETTERS ====================

    public Spell getSpell(String id) {
        if (id == null) { return null; }
        return loadedSpells.get(id.toLowerCase());
    }

    public Collection<Spell> getSpells() {
        return Collections.unmodifiableCollection(loadedSpells.values());
    }

    public Set<String> getLoadedSpellIds() {
        return Collections.unmodifiableSet(loadedSpells.keySet());
    }

    public List<Spell> getSpellsByType(SpellType type) {
        return loadedSpells.values().stream()
                .filter(s -> s.getSpellType() == type)
                .collect(Collectors.toList());
    }

    // ==================== PROFILE ====================

    public SpellProfile getProfile(Player player) {
        MidgardProfile coreProfile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (coreProfile == null) { return null; }
        return coreProfile.getOrCreateData(SpellProfile.class);
    }

    // ==================== CHANNELING ====================

    public boolean isChanneling(Player player) {
        return channelingTasks.containsKey(player.getUniqueId());
    }

    public ChannelingTask getChannelingTask(Player player) {
        return channelingTasks.get(player.getUniqueId());
    }

    public void cancelChanneling(Player player, String reason) {
        if (channelingTasks.containsKey(player.getUniqueId())) {
            ChannelingTask task = channelingTasks.remove(player.getUniqueId());
            Spell spell = task.getSpell();
            task.cancel();

            // No resource refund needed — mana/stamina is consumed only on finishChanneling()

            String interruptMsg = module.getMessage("casting.interrupted");
            if (interruptMsg != null) {
                interruptMsg = interruptMsg.replace("%reason%", reason);
                MessageUtils.send(player, interruptMsg);
            }

            // Play fail sound
            playSpellSound(player, spell.getCastFailSound());
        }
    }

    public void finishChanneling(Player player, Spell spell) {
        channelingTasks.remove(player.getUniqueId());

        SpellProfile profile = getProfile(player);
        int level = profile != null ? profile.getSpellLevel(spell.getId()) : 1;

        // Consume resources after channeling completes
        double manaCost = spell.getManaCost().calculate(level);
        if (manaCost > 0 && !module.getResourceProvider().consumeMana(player, manaCost)) {
            MessageUtils.send(player, module.getMessage("casting.no_mana"));
            playSpellSound(player, spell.getCastFailSound());
            return;
        }
        double staminaCost = spell.getStaminaCost().calculate(level);
        if (staminaCost > 0 && !module.getResourceProvider().consumeStamina(player, staminaCost)) {
            // Refund mana that was already consumed
            if (manaCost > 0) {
                module.getResourceProvider().consumeMana(player, -manaCost);
            }
            MessageUtils.send(player, module.getMessage("casting.no_stamina"));
            playSpellSound(player, spell.getCastFailSound());
            return;
        }

        executeSpellEffect(player, spell, level);
    }

    // ==================== CASTING ====================

    public boolean castSpell(Player player, String spellId) {
        Spell spell = getSpell(spellId);
        if (spell == null) { return false; }

        if (spell.isPassive()) {
            MessageUtils.send(player, module.getMessage("casting.passive_spell"));
            return false;
        }

        if (isChanneling(player)) {
            MessageUtils.send(player, module.getMessage("casting.already_casting"));
            return false;
        }

        SpellProfile profile = getProfile(player);
        if (profile == null) { return false; }

        if (!profile.hasSpell(spellId)) {
            return false;
        }

        int level = profile.getSpellLevel(spellId);

        if (profile.isOnCooldown(spellId)) {
            long remaining = profile.getCooldownRemainingKey(spellId) / 1000;
            String cooldownMsg = module.getMessage("casting.on_cooldown")
                    .replace("%spell%", spell.getDisplayName())
                    .replace("%time%", String.valueOf(remaining));
            MessageUtils.send(player, cooldownMsg);
            return false;
        }

        for (SpellRequirement req : spell.getRequirements()) {
            if (!req.check(player)) {
                MessageUtils.send(player, req.getFailureMessage());
                return false;
            }
        }

        // Pre-check Mana
        double manaCost = spell.getManaCost().calculate(level);
        if (manaCost > 0 && module.getResourceProvider().getMana(player) < manaCost) {
            MessageUtils.send(player, module.getMessage("casting.no_mana"));
            return false;
        }

        // Pre-check Stamina
        double staminaCost = spell.getStaminaCost().calculate(level);
        if (staminaCost > 0 && module.getResourceProvider().getStamina(player) < staminaCost) {
            MessageUtils.send(player, module.getMessage("casting.no_stamina"));
            return false;
        }

        // Channeling — resources consumed after finish
        if (spell.getCastTime() > 0) {
            startChanneling(player, spell);
            return true;
        }

        // Instant cast: consume now
        if (manaCost > 0 && !module.getResourceProvider().consumeMana(player, manaCost)) {
            MessageUtils.send(player, module.getMessage("casting.no_mana"));
            return false;
        }
        if (staminaCost > 0 && !module.getResourceProvider().consumeStamina(player, staminaCost)) {
            // Refund mana that was already consumed
            if (manaCost > 0) {
                module.getResourceProvider().consumeMana(player, -manaCost);
            }
            MessageUtils.send(player, module.getMessage("casting.no_stamina"));
            return false;
        }

        return executeSpellEffect(player, spell, level);
    }

    private void startChanneling(Player player, Spell spell) {
        ChannelingTask task = new ChannelingTask(module, player, spell);
        channelingTasks.put(player.getUniqueId(), task);
        task.start();

        // Play cast start sound
        playSpellSound(player, spell.getCastStartSound());
    }

    private boolean executeSpellEffect(Player player, Spell spell, int level) {
        String skillName = spell.getEffectiveSkillName(level);
        String spellId = spell.getId();
        SpellProfile profile = getProfile(player);

        Optional<Skill> mythicSkill;
        try {
            mythicSkill = MythicProvider.get().getSkillManager().getSkill(skillName);
        } catch (Throwable e) {
            MidgardLogger.error("MythicMobs nao disponivel ao conjurar " + spellId + ": " + e.getMessage());
            return false;
        }
        if (mythicSkill.isEmpty()) {
            String errorMsg = module.getMessage("errors.config_error")
                    .replace("%skill%", skillName);
            MessageUtils.send(player, errorMsg);
            return false;
        }

        boolean success = false;
        Skill skill = mythicSkill.get();

        try {
            io.lumine.mythic.api.skills.SkillMetadata meta = new io.lumine.mythic.core.skills.SkillMetadataImpl(
                    SkillTrigger.get("API"),
                    MythicProvider.get().getSkillManager().getCaster(io.lumine.mythic.bukkit.BukkitAdapter.adapt(player)),
                    io.lumine.mythic.bukkit.BukkitAdapter.adapt(player),
                    io.lumine.mythic.bukkit.BukkitAdapter.adapt(player.getEyeLocation()),
                    new HashSet<>(),
                    new HashSet<>(),
                    1.0f
            );

            // Calculate variables with milestone bonuses
            Map<String, Object> effectiveVariables = calculateEffectiveVariables(spell, level, profile);

            // Inject variables
            for (Map.Entry<String, Object> entry : effectiveVariables.entrySet()) {
                String key = entry.getKey();
                Object valObject = entry.getValue();

                if (valObject instanceof ScalableAttribute sa) {
                    double val = sa.calculate(level);
                    meta.getVariables().putFloat(key, (float) val);
                } else {
                    String valueResult = valObject.toString();

                    // Resolve %attr:xxx% placeholders
                    valueResult = resolveAttributePlaceholders(player, valueResult);

                    // Resolve PAPI placeholders
                    if (valueResult.contains("%")) {
                        try {
                            if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                                valueResult = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, valueResult);
                            }
                        } catch (Throwable ignored) {
                            // PlaceholderAPI not available on classpath
                        }
                    }

                    meta.getVariables().putString(key, valueResult);
                }
            }

            if (skill.isUsable(meta)) {
                skill.execute(meta);
                success = true;
            } else {
                MidgardLogger.warn("MythicMobs skill '" + skillName + "' isUsable() returned false for " + player.getName());
            }

        } catch (Exception e) {
            MidgardLogger.error("Falha ao conjurar magia " + spellId, e);
        }

        if (success) {
            double cooldown = spell.getCooldown().calculate(level);
            profile.setCooldown(spellId, cooldown);

            // Play finish sound
            playSpellSound(player, spell.getCastFinishSound());

            // Update statistics
            profile.getSpellStats(spellId).incrementCasts();

            // Grant XP (mob/pvp bonus handled separately via SpellDamageListener)
            xpManager.grantCastXP(player, spell, false, false);

            // Track cast for damage listener
            if (module.getDamageListener() != null) {
                module.getDamageListener().trackCast(player, spellId);
            }

            if (module.getConfig().getBoolean("general.show_cast_messages", true)) {
                String castMsg = module.getMessage("casting.spell_cast")
                        .replace("%spell%", spell.getDisplayName());
                MessageUtils.send(player, castMsg);
            }
        }

        return success;
    }

    // ==================== VARIABLE CALCULATION ====================

    private Map<String, Object> calculateEffectiveVariables(Spell spell, int level, SpellProfile profile) {
        Map<String, Object> result = new HashMap<>(spell.getVariables());

        // Apply milestone stat bonuses
        for (SpellMilestone milestone : spell.getMilestones()) {
            if (level >= milestone.level()) {
                for (Map.Entry<String, Double> bonus : milestone.statBonuses().entrySet()) {
                    String key = bonus.getKey();
                    Object existing = result.get(key);
                    if (existing instanceof ScalableAttribute sa) {
                        double current = sa.calculate(level);
                        double bonusValue = bonus.getValue();
                        // Replace with flat value (base + milestone bonus)
                        result.put(key, ScalableAttribute.of(current + bonusValue));
                    }
                }
            }
        }

        return result;
    }

    private String resolveAttributePlaceholders(Player player, String value) {
        if (!value.contains("%attr:")) { return value; }

        MidgardProfile coreProfile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (coreProfile == null) { return value; }

        CoreAttributeData attrData = coreProfile.getOrCreateData(CoreAttributeData.class);

        Matcher matcher = ATTR_PATTERN.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String attrId = matcher.group(1);
            AttributeInstance instance = attrData.getInstance(attrId);
            double attrValue = instance != null ? instance.getValue() : 0;
            matcher.appendReplacement(result, String.valueOf(attrValue));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    // ==================== MILESTONES & MASTERY ====================

    public void checkMilestone(Player player, Spell spell, int newLevel) {
        SpellProfile profile = getProfile(player);
        if (profile == null) { return; }

        SpellMilestone milestone = spell.getMilestoneForLevel(newLevel);
        if (milestone == null) { return; }
        if (profile.hasMilestone(spell.getId(), newLevel)) { return; }

        profile.achieveMilestone(spell.getId(), newLevel);

        String msg = module.getMessage("milestones.achieved")
                .replace("%spell%", spell.getDisplayName())
                .replace("%level%", String.valueOf(newLevel));
        MessageUtils.send(player, msg);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    public void grantMastery(Player player, Spell spell) {
        SpellProfile profile = getProfile(player);
        if (profile == null) { return; }
        if (profile.isMastered(spell.getId())) { return; }

        profile.setMastered(spell.getId());

        // Apply permanent attribute bonuses
        Map<String, Double> bonuses = spell.getMasteryBonuses();
        if (!bonuses.isEmpty()) {
            MidgardProfile coreProfile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (coreProfile != null) {
                CoreAttributeData attrData = coreProfile.getOrCreateData(CoreAttributeData.class);
                for (Map.Entry<String, Double> entry : bonuses.entrySet()) {
                    AttributeInstance instance = attrData.getInstance(entry.getKey());
                    if (instance != null) {
                        AttributeModifier modifier = new AttributeModifier(
                                "mastery_" + spell.getId() + "_" + entry.getKey(),
                                entry.getValue(),
                                AttributeOperation.ADD_NUMBER
                        );
                        instance.addModifier(modifier);
                    }
                }
            }
        }

        String msg = module.getMessage("mastery.achieved")
                .replace("%spell%", spell.getDisplayName());
        MessageUtils.send(player, msg);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.5f);
    }

    /**
     * Remove mastery attribute modifiers for a spell before unlearning.
     */
    public void removeSpellMasteryModifiers(Player player, String spellId) {
        Spell spell = getSpell(spellId);
        if (spell == null) { return; }

        SpellProfile profile = getProfile(player);
        if (profile == null || !profile.isMastered(spellId)) { return; }

        Map<String, Double> bonuses = spell.getMasteryBonuses();
        if (bonuses.isEmpty()) { return; }

        MidgardProfile coreProfile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (coreProfile == null) { return; }

        CoreAttributeData attrData = coreProfile.getOrCreateData(CoreAttributeData.class);
        for (String attrKey : bonuses.keySet()) {
            AttributeInstance instance = attrData.getInstance(attrKey);
            if (instance != null) {
                instance.removeModifier("mastery_" + spellId + "_" + attrKey);
            }
        }
    }

    // ==================== SOUND HELPER ====================

    public void playSpellSound(Player player, SpellSound spellSound) {
        if (spellSound == null) { return; }
        try {
            String soundName = spellSound.sound().toUpperCase().replace('.', '_').replace(' ', '_');
            Sound bukkitSound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), bukkitSound, spellSound.volume(), spellSound.pitch());
        } catch (IllegalArgumentException e) {
            MidgardLogger.warn("Invalid sound: " + spellSound.sound());
        }
    }

    // ==================== SHUTDOWN ====================

    /**
     * Limpa todos os estados em memória ao desligar o módulo.
     * Cancela channelings ativos, limpa modos de casting e buffers.
     */
    public void shutdown() {
        // Cancel all active channeling tasks
        for (ChannelingTask task : channelingTasks.values()) {
            try {
                task.cancel();
            } catch (Throwable ignored) { /* Cancel pode falhar se task já finalizada */ }
        }
        channelingTasks.clear();

        // Disable casting mode for all players - schedule on each player's entity thread for Folia
        for (UUID uuid : new HashSet<>(castingModePlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                me.ray.midgard.core.utils.Task.sync(player, () -> {
                    try {
                        player.removeMetadata("midgard_casting_mode", module.getPlugin());
                        player.removeMetadata("midgard_combo_active", module.getPlugin());
                    } catch (Throwable ignored) { /* Metadata removal pode falhar se jogador desconectou */ }
                });
            }
        }
        castingModePlayers.clear();
        comboActivePlayers.clear();
        castingAnchors.clear();
        loadedSpells.clear();
    }

    // ==================== MASTERY REAPPLICATION ====================

    /**
     * Reaplica bônus de mastery para um jogador (usado no login).
     * Necessário porque AttributeModifiers são transientes e perdidos no relog.
     */
    public void reapplyMasteryBonuses(Player player) {
        SpellProfile spellProfile = getProfile(player);
        if (spellProfile == null) { return; }

        MidgardProfile coreProfile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (coreProfile == null) { return; }

        CoreAttributeData attrData = coreProfile.getOrCreateData(CoreAttributeData.class);

        for (String spellId : spellProfile.getMasteredSpells()) {
            Spell spell = getSpell(spellId);
            if (spell == null) { continue; }

            for (Map.Entry<String, Double> entry : spell.getMasteryBonuses().entrySet()) {
                AttributeInstance instance = attrData.getInstance(entry.getKey());
                if (instance == null) { continue; }

                String modName = "mastery_" + spellId + "_" + entry.getKey();
                // Remove first to avoid duplicates
                instance.removeModifier(modName);
                instance.addModifier(new AttributeModifier(
                        modName,
                        entry.getValue(),
                        AttributeOperation.ADD_NUMBER
                ));
            }
        }
    }
}

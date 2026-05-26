package me.ray.midgard.modules.races.manager;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ConsoleUtils;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.event.PlayerChangeRaceEvent;
import me.ray.midgard.modules.races.model.ConfiguredTrait;
import me.ray.midgard.modules.races.model.EvolutionRequirement;
import me.ray.midgard.modules.races.model.Race;
import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import me.ray.midgard.modules.races.registry.TraitRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import me.ray.midgard.core.database.DefinitionRepository;
import me.ray.midgard.core.database.DefinitionMigrationTool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RaceManager {

    private final RacesModule module;
    private volatile Map<String, Race> races;

    public RaceManager(RacesModule module) {
        this.module = module;
        this.races = new ConcurrentHashMap<>();
    }

    public void loadRaces() {
        Map<String, Race> newRaces = new HashMap<>();

        DefinitionRepository repo = module.getRepository();
        if (repo != null && repo.count() > 0) {
            // Carregar do banco de dados
            Map<String, DefinitionRepository.DefinitionData> dbRaces = repo.loadAll();
            for (Map.Entry<String, DefinitionRepository.DefinitionData> entry : dbRaces.entrySet()) {
                try {
                    ConfigurationSection section = DefinitionMigrationTool.deserializeToSection(
                        entry.getKey(), entry.getValue().yamlData());
                    if (section != null) {
                        Race race = loadRaceFromSection(entry.getKey(), section);
                        newRaces.put(entry.getKey(), race);
                    }
                } catch (Exception e) {
                    MidgardLogger.warn("Erro ao carregar raça " + entry.getKey() + " do banco", e);
                }
            }
        } else {
            // Fallback: carregar do YAML local
            File file = new File(module.getDataFolder(), "races.yml");
            if (!file.exists()) {
                module.saveResource("modules/races/races.yml", false);
            }
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection section = config.getConfigurationSection("races");
            if (section == null) {
                ConsoleUtils.warn("[MidgardRaces] Nenhuma raça encontrada!");
                return;
            }
            for (String key : section.getKeys(false)) {
                try {
                    ConfigurationSection raceSection = section.getConfigurationSection(key);
                    if (raceSection != null) {
                        Race race = loadRaceFromSection(key, raceSection);
                        newRaces.put(key, race);
                    }
                } catch (Exception e) {
                    MidgardLogger.warn("Erro ao carregar raça " + key, e);
                }
            }
        }

        this.races = new ConcurrentHashMap<>(newRaces);
        ConsoleUtils.info("[MidgardRaces] Carregadas " + races.size() + " raças.");
    }

    public void reloadRaceFromDb(String raceId, DefinitionRepository.DefinitionData data) {
        try {
            ConfigurationSection section = DefinitionMigrationTool.deserializeToSection(raceId, data.yamlData());
            if (section != null) {
                Race race = loadRaceFromSection(raceId, section);
                races.put(raceId, race);
            }
        } catch (Exception e) {
            MidgardLogger.warn("Erro ao recarregar raça " + raceId + " do banco", e);
        }
    }

    public void unregisterRace(String raceId) {
        races.remove(raceId);
    }

    private Race loadRaceFromSection(String id, ConfigurationSection section) {
        String displayName = section.getString("display-name", id);
        String parentRace = section.getString("parent-race", null);
        int minLevel = section.getInt("min-level", 0);
        int slot = section.getInt("slot", -1);
        int treeSlot = section.getInt("tree-slot", -1);
        
        org.bukkit.inventory.ItemStack icon = null;
        if (section.contains("nexo") && me.ray.midgard.core.MidgardCore.getInstance().getServer().getPluginManager().isPluginEnabled("Nexo")) {
             try {
                icon = me.ray.midgard.core.integration.NexoUtils.getCustomItem(section.getString("nexo"));
             } catch (Throwable ignored) { /* Nexo pode não estar disponível */ }
        }
        
        if (icon == null) {
            Material mat = Material.matchMaterial(section.getString("icon", "PLAYER_HEAD"));
            if (mat == null) { mat = Material.PLAYER_HEAD; }
            icon = new org.bukkit.inventory.ItemStack(mat);
        }

        if (section.contains("model-data")) {
            me.ray.midgard.core.utils.ItemBuilder ib = new me.ray.midgard.core.utils.ItemBuilder(icon);
            ib.customModelData(section.getInt("model-data"));
            icon = ib.build();
        }
        
        List<String> description = section.getStringList("description");
        List<String> onSelectCommands = section.getStringList("on-select-commands");
        List<String> onRemoveCommands = section.getStringList("on-remove-commands");
        List<String> permissions = section.getStringList("permissions");
        
        Map<String, Double> attributes = new HashMap<>();
        if (section.isConfigurationSection("attributes")) {
            ConfigurationSection attrSection = section.getConfigurationSection("attributes");
            for (String attr : attrSection.getKeys(false)) {
                attributes.put(attr, attrSection.getDouble(attr));
            }
        }

        Map<String, Double> perLevelAttributes = new HashMap<>();
        if (section.isConfigurationSection("per-level-attributes")) {
            ConfigurationSection attrSection = section.getConfigurationSection("per-level-attributes");
            for (String attr : attrSection.getKeys(false)) {
                perLevelAttributes.put(attr, attrSection.getDouble(attr));
            }
        }

        Map<String, Double> dayAttributes = parseAttributeSection(section, "day-attributes");
        Map<String, Double> nightAttributes = parseAttributeSection(section, "night-attributes");
        Map<String, Double> dayPerLevelAttributes = parseAttributeSection(section, "day-per-level-attributes");
        Map<String, Double> nightPerLevelAttributes = parseAttributeSection(section, "night-per-level-attributes");

        List<ConfiguredTrait> traits = new ArrayList<>();
        if (section.isConfigurationSection("traits")) {
            ConfigurationSection traitsSection = section.getConfigurationSection("traits");
            for (String traitKey : traitsSection.getKeys(false)) {
                ConfigurationSection traitConfig = traitsSection.getConfigurationSection(traitKey);
                if (traitConfig == null) { continue; }

                String type = traitConfig.getString("type");
                if (type == null || type.isEmpty()) {
                    ConsoleUtils.warn("[MidgardRaces] Trait 'type' not configured for trait " + traitKey + " in race " + id);
                    continue;
                }
                RaceTrait trait = TraitRegistry.getInstance().getTrait(type);
                if (trait == null) {
                    ConsoleUtils.warn("[MidgardRaces] Trait type '" + type + "' not found for race " + id);
                    continue;
                }

                String triggerName = traitConfig.getString("trigger", "PASSIVE_TICK");
                TraitTrigger trigger;
                try {
                    trigger = TraitTrigger.valueOf(triggerName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    ConsoleUtils.warn("[MidgardRaces] Invalid trigger '" + triggerName + "' for trait " + traitKey);
                    continue;
                }

                Map<String, Object> configMap = new HashMap<>();
                for (String configKey : traitConfig.getKeys(false)) {
                    configMap.put(configKey, traitConfig.get(configKey));
                }
                configMap.put("trait_id", traitKey); // Inject trait ID for identification

                int traitMinLevel = traitConfig.getInt("min-level", 1);
                boolean selectable = traitConfig.getBoolean("selectable", false);
                String exclusionGroup = traitConfig.getString("exclusion-group", null);
                me.ray.midgard.modules.races.model.TraitCondition condition;
                if (traitConfig.isConfigurationSection("condition")) {
                    condition = me.ray.midgard.modules.races.model.TraitCondition.fromSection(traitConfig.getConfigurationSection("condition"));
                } else {
                    condition = me.ray.midgard.modules.races.model.TraitCondition.fromString(traitConfig.getString("active-at", "ALWAYS"));
                }

                traits.add(new ConfiguredTrait(traitKey, trait, trigger, traitMinLevel, configMap, selectable, exclusionGroup, condition));
            }
        }

        // XP multipliers
        Map<me.ray.midgard.modules.races.api.RaceXpSource, Double> xpMultipliers = new HashMap<>();
        if (section.isConfigurationSection("xp-multipliers")) {
            ConfigurationSection xpSection = section.getConfigurationSection("xp-multipliers");
            for (String key : xpSection.getKeys(false)) {
                try {
                    me.ray.midgard.modules.races.api.RaceXpSource source =
                            me.ray.midgard.modules.races.api.RaceXpSource.valueOf(key.toUpperCase());
                    xpMultipliers.put(source, xpSection.getDouble(key));
                } catch (IllegalArgumentException e) {
                    ConsoleUtils.warn("[MidgardRaces] XP source '" + key + "' invalida na raca " + id);
                }
            }
        }

        return new Race(id, displayName, parentRace, minLevel, slot, treeSlot, icon, description,
                attributes, perLevelAttributes, dayAttributes, nightAttributes,
                dayPerLevelAttributes, nightPerLevelAttributes,
                traits, permissions, onSelectCommands, onRemoveCommands,
                parseEvolutionRequirements(section), section.getString("exclusion-branch", null),
                section.getBoolean("allow-devolution", true), xpMultipliers);
    }

    private Map<String, Double> parseAttributeSection(ConfigurationSection parent, String key) {
        if (!parent.isConfigurationSection(key)) { return Map.of(); }
        ConfigurationSection section = parent.getConfigurationSection(key);
        Map<String, Double> result = new HashMap<>();
        for (String attr : section.getKeys(false)) {
            result.put(attr, section.getDouble(attr));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<EvolutionRequirement> parseEvolutionRequirements(ConfigurationSection section) {
        if (!section.isList("evolution-requirements")) { return List.of(); }
        List<Map<?, ?>> raw = section.getMapList("evolution-requirements");
        return EvolutionRequirement.fromConfigList(raw);
    }

    public Race getRace(String id) {
        if (id == null) { return null; }
        return races.get(id);
    }

    public Collection<Race> getRaces() {
        return Collections.unmodifiableCollection(races.values());
    }
    
    public void setRace(Player player, Race newRace) {
        setRace(player, newRace, false);
    }

    /**
     * Define a raça do jogador, com opção de forçar (admin bypass).
     * @param forced true para ignorar validação de raça pai (uso admin)
     */
    public void setRace(Player player, Race newRace, boolean forced) {
        if (newRace == null) { return; }
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return; }
        
        RaceData data = profile.getOrCreateData(RaceData.class);
        Race oldRace = getRace(data.getRaceId());

        // Validação de sub-raça: exige que o jogador tenha a raça pai (bypass para admin)
        if (!forced && newRace.isSubRace()) {
            if (oldRace == null || !newRace.getParentRace().equals(oldRace.getId())) {
                MessageUtils.send(player, module.getMessage("command.missing_parent_race")
                        .replace("%race%", newRace.getDisplayName()));
                return;
            }
        }
        
        PlayerChangeRaceEvent event = new PlayerChangeRaceEvent(player, oldRace, newRace);
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) { return; }

        // 1. Trigger ON_REMOVE for old race
        if (oldRace != null) {
            processTrigger(player, TraitTrigger.ON_REMOVE, new HashMap<>());
            
            // Execute On Remove Commands on global thread (dispatchCommand requires global tick thread in Folia)
            if (oldRace.getOnRemoveCommands() != null) {
                for (String cmd : oldRace.getOnRemoveCommands()) {
                    String finalCmd = cmd.replace("{player}", player.getName());
                    Task.sync(() -> { if (player.isOnline()) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd); } });
                }
            }
        }
        
        data.setRaceId(newRace.getId());
        data.setLastRaceChange(System.currentTimeMillis());
        data.setLevel(1);
        data.setExperience(0);
        data.getUnlockedMutations().clear(); // Reset mutations on race change

        // 2. Trigger ON_SELECT for new race
        processTrigger(player, TraitTrigger.ON_SELECT, new HashMap<>());
        
        // Execute On Select Commands on global thread (dispatchCommand requires global tick thread in Folia)
        if (newRace.getOnSelectCommands() != null) {
            for (String cmd : newRace.getOnSelectCommands()) {
                String finalCmd = cmd.replace("{player}", player.getName());
                Task.sync(() -> { if (player.isOnline()) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd); } });
            }
        }
        
        MessageUtils.send(player, module.getMessage("event.race_change").replace("%race%", newRace.getDisplayName()));

        // Persistir imediatamente
        MidgardProfile profileToSave = MidgardCore.getProfileManager().getProfile(player);
        if (profileToSave != null) {
            MidgardCore.getProfileManager().saveProfile(profileToSave);
        }
    }

    public void processTrigger(Player player, TraitTrigger trigger, Map<String, Object> context) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return; }

        RaceData data = profile.getData(RaceData.class);
        if (data == null || !data.hasRace()) { return; }

        Race race = getRace(data.getRaceId());
        if (race == null || race.getTraits() == null) { return; }

        for (ConfiguredTrait ct : race.getTraits()) {
            if (ct.getTrigger() == trigger) {
                if (data.getLevel() >= ct.getMinLevel()) {
                    // Check mutation unlock for selectable traits
                    if (ct.isSelectable() && !data.hasMutation(ct.getId())) {
                        continue;
                    }
                    // Check condition (time, biome, world, weather, altitude)
                    if (!ct.getCondition().isMet(player)) { continue; }
                    try {
                        ct.getTrait().execute(player, trigger, context, ct.getConfig());
                    } catch (Exception e) {
                        me.ray.midgard.core.debug.MidgardLogger.error("Erro ao executar trait %s para %s", ct.getId(), player.getName(), e);
                    }
                }
            }
        }
    }
    
    public RaceData getRaceData(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return null; }
        return profile.getData(RaceData.class);
    }
    
    public boolean canUnlockMutation(Player player, Race race, ConfiguredTrait trait) {
        if (!trait.isSelectable()) { return true; } // Non-selectable are auto-unlocked by level
        
        RaceData data = getRaceData(player);
        if (data == null || !data.hasRace() || !data.getRaceId().equals(race.getId())) { return false; }
        
        if (data.getLevel() < trait.getMinLevel()) { return false; }
        if (data.hasMutation(trait.getId())) { return false; } // Already unlocked
        
        // Check exclusion group
        if (trait.getExclusionGroup() != null) {
            for (ConfiguredTrait other : race.getTraits()) {
                if (other.getId().equals(trait.getId())) { continue; }
                if (trait.getExclusionGroup().equals(other.getExclusionGroup())) {
                    if (data.hasMutation(other.getId())) {
                        return false; // Already has a mutation from this group
                    }
                }
            }
        }
        
        return true;
    }
    
    public void unlockMutation(Player player, Race race, ConfiguredTrait trait) {
        if (!canUnlockMutation(player, race, trait)) { return; }
        
        RaceData data = getRaceData(player);
        if (data == null) { return; }
        data.unlockMutation(trait.getId());
        MessageUtils.send(player, module.getMessage("event.mutation_unlocked").replace("%mutation%", trait.getId()));

        // Persistir imediatamente — mutation é decisão permanente do jogador
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile != null) {
            MidgardCore.getProfileManager().saveProfile(profile);
        }
    }
    
    public void resetRace(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return; }
        
        RaceData data = profile.getData(RaceData.class);
        if (data == null || !data.hasRace()) { return; } // Nothing to reset
        
        Race oldRace = getRace(data.getRaceId());
        
        // Call event with newRace = null
        PlayerChangeRaceEvent event = new PlayerChangeRaceEvent(player, oldRace, null);
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) { return; }

        // Trigger ON_REMOVE for old race
        if (oldRace != null) {
            processTrigger(player, TraitTrigger.ON_REMOVE, new HashMap<>());
            
            // Execute On Remove Commands on global thread (dispatchCommand requires global tick thread in Folia)
            if (oldRace.getOnRemoveCommands() != null) {
                for (String cmd : oldRace.getOnRemoveCommands()) {
                    String finalCmd = cmd.replace("{player}", player.getName());
                    Task.sync(() -> { if (player.isOnline()) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd); } });
                }
            }
        }
        data.setRaceId(null);
        data.setLevel(1);
        data.setExperience(0);
        data.getUnlockedMutations().clear();
        
        MessageUtils.send(player, module.getMessage("event.race_reset"));

        // Persistir imediatamente
        MidgardCore.getProfileManager().saveProfile(profile);
    }

    // ─── Evolução ────────────────────────────────────────────────────

    /**
     * Verifica se o jogador cumpre todos os requisitos de evolução.
     */
    public boolean checkEvolutionRequirements(Player player, Race evolution) {
        if (!evolution.hasEvolutionRequirements()) { return true; }
        RaceData data = getRaceData(player);
        if (data == null) { return false; }
        for (EvolutionRequirement req : evolution.getEvolutionRequirements()) {
            if (!req.isMet(player, data)) { return false; }
        }
        return true;
    }

    /**
     * Tenta evoluir o jogador para uma sub-raça, validando requisitos e branch exclusivo.
     * Retorna true se a evolução foi bem-sucedida.
     */
    public boolean evolve(Player player, Race evolution) {
        if (evolution == null || !evolution.isSubRace()) { return false; }
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return false; }

        RaceData data = profile.getOrCreateData(RaceData.class);
        Race currentRace = getRace(data.getRaceId());
        if (currentRace == null) { return false; }

        // Verificar que a evolução é filha da raça atual
        if (!evolution.getParentRace().equals(currentRace.getId())) {
            MessageUtils.send(player, module.getMessage("command.missing_parent_race")
                    .replace("%race%", evolution.getDisplayName()));
            return false;
        }

        // Verificar nível mínimo
        if (data.getLevel() < evolution.getMinLevel()) {
            MessageUtils.send(player, module.getMessage("evolution.requirements_not_met")
                    .replace("%race%", evolution.getDisplayName()));
            return false;
        }

        // Verificar branch exclusivo
        if (evolution.getExclusionBranch() != null) {
            for (Race sibling : getRaces()) {
                if (sibling.getId().equals(evolution.getId())) { continue; }
                if (!sibling.isSubRace() || !sibling.getParentRace().equals(currentRace.getId())) { continue; }
                if (evolution.getExclusionBranch().equals(sibling.getExclusionBranch())) {
                    // Verificar se o jogador já evoluiu nesse branch (histórico)
                    if (data.getRaceHistory().contains(sibling.getId())) {
                        MessageUtils.send(player, module.getMessage("evolution.branch_locked")
                                .replace("%race%", evolution.getDisplayName())
                                .replace("%branch%", evolution.getExclusionBranch()));
                        return false;
                    }
                }
            }
        }

        // Verificar requisitos
        if (!checkEvolutionRequirements(player, evolution)) {
            MessageUtils.send(player, module.getMessage("evolution.requirements_not_met")
                    .replace("%race%", evolution.getDisplayName()));
            return false;
        }

        // Salvar raça atual no histórico antes de mudar
        data.pushRaceHistory(currentRace.getId());

        // Evoluir via setRace (que trata ON_REMOVE, ON_SELECT, eventos)
        setRace(player, evolution);

        // Verificar se a evolução realmente aconteceu (evento pode ter sido cancelado)
        if (!evolution.getId().equals(data.getRaceId())) {
            // Evolução cancelada por evento — restaurar histórico
            data.popRaceHistory();
            return false;
        }

        // Consumir requisitos somente após sucesso confirmado (itens, dinheiro)
        for (EvolutionRequirement req : evolution.getEvolutionRequirements()) {
            req.consume(player);
        }

        return true;
    }

    /**
     * De-evolui o jogador, voltando para a raça anterior no histórico.
     * Retorna true se a de-evolução foi bem-sucedida.
     */
    public boolean devolve(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return false; }

        RaceData data = profile.getOrCreateData(RaceData.class);
        if (!data.hasRace()) { return false; }

        Race currentRace = getRace(data.getRaceId());
        if (currentRace == null) { return false; }

        // Verificar se de-evolução é permitida na config
        boolean devolutionEnabled = module.getConfig().getBoolean("settings.devolution.enabled", true);
        if (!devolutionEnabled) {
            MessageUtils.send(player, module.getMessage("evolution.devolution_disabled"));
            return false;
        }

        // Verificar se a raça atual permite de-evolução
        if (!currentRace.isAllowDevolution()) {
            MessageUtils.send(player, module.getMessage("evolution.devolution_blocked")
                    .replace("%race%", currentRace.getDisplayName()));
            return false;
        }

        // Buscar raça anterior do histórico (peek primeiro, pop só após validação)
        String previousRaceId = data.getPreviousRaceId();
        if (previousRaceId == null) {
            MessageUtils.send(player, module.getMessage("evolution.no_previous_race"));
            return false;
        }

        Race previousRace = getRace(previousRaceId);
        if (previousRace == null) {
            MessageUtils.send(player, module.getMessage("evolution.previous_race_invalid"));
            return false;
        }

        // Custo de de-evolução
        double devolutionCost = module.getConfig().getDouble("settings.devolution.cost", 0);
        if (devolutionCost > 0) {
            var eco = MidgardCore.getEconomyProvider();
            if (eco != null && !eco.has(player.getUniqueId(), "default", devolutionCost)) {
                MessageUtils.send(player, module.getMessage("evolution.devolution_no_money")
                        .replace("%cost%", String.format("%.0f", devolutionCost)));
                return false;
            }
            if (eco != null) {
                eco.withdraw(player.getUniqueId(), "default", devolutionCost);
            }
        }

        // Todas validações passaram — remover do histórico e executar de-evolução
        data.popRaceHistory();

        // Executar de-evolução via setRace (forced para bypassar validação de parent)
        setRace(player, previousRace, true);

        // Verificar se a de-evolução realmente aconteceu (evento pode ter sido cancelado)
        if (!data.getRaceId().equals(previousRace.getId())) {
            // Evento cancelado — restaurar histórico e reembolsar
            data.pushRaceHistory(previousRaceId);
            if (devolutionCost > 0) {
                var ecoRefund = MidgardCore.getEconomyProvider();
                if (ecoRefund != null) {
                    ecoRefund.deposit(player.getUniqueId(), "default", devolutionCost);
                }
            }
            return false;
        }

        MessageUtils.send(player, module.getMessage("evolution.devolved")
                .replace("%race%", previousRace.getDisplayName()));
        return true;
    }

    /**
     * Registra uma kill para o jogador (usado para requisitos de evolução KILLS/KILL_TYPE).
     */
    public void addKill(Player player, String entityType) {
        RaceData data = getRaceData(player);
        if (data == null || !data.hasRace()) { return; }
        data.addKill(entityType);
    }

}

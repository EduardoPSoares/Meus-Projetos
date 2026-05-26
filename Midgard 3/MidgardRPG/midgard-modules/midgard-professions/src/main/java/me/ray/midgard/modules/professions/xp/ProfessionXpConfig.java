package me.ray.midgard.modules.professions.xp;

import me.ray.midgard.core.config.ConfigWrapper;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.professions.ProfessionType;
import me.ray.midgard.modules.professions.ProfessionsModule;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuração de fontes de XP por profissão.
 * Carregado da seção 'xp-config' de cada profissão YML.
 *
 * Suporta dois tipos de ação:
 * - Material-based: chave é Material do Bukkit → XP (ex: block-break, craft, smelt, fish)
 * - Param-based: chave é parâmetro com nome descritivo → valor (ex: enchant.base-xp)
 */
public final class ProfessionXpConfig {

    private static volatile Map<ProfessionType, ProfessionXpConfig> CONFIGS = Collections.emptyMap();

    private final ProfessionType type;
    private final Map<String, Map<Material, Double>> materialSources;
    private final Map<String, Map<String, Double>> paramSources;

    private ProfessionXpConfig(ProfessionType type,
                               Map<String, Map<Material, Double>> materialSources,
                               Map<String, Map<String, Double>> paramSources) {
        this.type = type;
        this.materialSources = materialSources;
        this.paramSources = paramSources;
    }

    public ProfessionType type() { return type; }

    /**
     * Retorna o XP para uma ação material-based (block-break, craft, smelt, fish).
     * Retorna 0 se não configurado.
     */
    public double getMaterialXp(String action, Material material) {
        var map = materialSources.get(action);
        return map != null ? map.getOrDefault(material, 0.0) : 0.0;
    }

    /**
     * Retorna o valor de um parâmetro para uma ação param-based (enchant, brew).
     * Retorna 0 se não configurado.
     */
    public double getParam(String action, String param) {
        var map = paramSources.get(action);
        return map != null ? map.getOrDefault(param, 0.0) : 0.0;
    }

    /**
     * Verifica se esta profissão tem uma ação configurada.
     */
    public boolean hasAction(String action) {
        return materialSources.containsKey(action) || paramSources.containsKey(action);
    }

    /**
     * Carrega xp-config de todas as profissões.
     */
    public static void loadAll() {
        ProfessionsModule module = ProfessionsModule.getInstance();
        if (module == null || module.getPlugin() == null) {
            MidgardLogger.warn("ProfessionXpConfig.loadAll() chamado antes do módulo inicializar.");
            return;
        }

        Map<ProfessionType, ProfessionXpConfig> newConfigs = new EnumMap<>(ProfessionType.class);
        int loaded = 0;
        for (ProfessionType type : ProfessionType.values()) {
            String path = "modules/professions/professions/" + type.getId() + ".yml";
            try {
                ConfigWrapper wrapper = new ConfigWrapper(module.getPlugin(), path);
                FileConfiguration config = wrapper.getConfig();
                ConfigurationSection xpSection = config.getConfigurationSection("xp-config");

                if (xpSection == null) {
                    MidgardLogger.debug("Sem 'xp-config' em %s, ignorando.", path);
                    continue;
                }

                Map<String, Map<Material, Double>> materialSources = new HashMap<>();
                Map<String, Map<String, Double>> paramSources = new HashMap<>();

                for (String actionKey : xpSection.getKeys(false)) {
                    ConfigurationSection actionSec = xpSection.getConfigurationSection(actionKey);
                    if (actionSec == null) { continue; }

                    Map<Material, Double> materials = new HashMap<>();
                    Map<String, Double> params = new HashMap<>();

                    for (String key : actionSec.getKeys(false)) {
                        double value = actionSec.getDouble(key, 0);
                        if (value <= 0) { continue; }

                        try {
                            Material mat = Material.valueOf(key.toUpperCase());
                            materials.put(mat, value);
                        } catch (IllegalArgumentException e) {
                            params.put(key, value);
                        }
                    }

                    if (!materials.isEmpty()) {
                        materialSources.put(actionKey, Map.copyOf(materials));
                    }
                    if (!params.isEmpty()) {
                        paramSources.put(actionKey, Map.copyOf(params));
                    }
                }

                newConfigs.put(type, new ProfessionXpConfig(type, Map.copyOf(materialSources), Map.copyOf(paramSources)));
                loaded++;
            } catch (Exception e) {
                MidgardLogger.error("Erro ao carregar xp-config de '%s'", type.getId(), e);
            }
        }

        // Swap atômico — readers sempre veem um mapa completo e consistente
        CONFIGS = Collections.unmodifiableMap(newConfigs);
        MidgardLogger.info("Carregadas %d configurações de XP de profissões.", loaded);
    }

    public static ProfessionXpConfig get(ProfessionType type) {
        return CONFIGS.get(type);
    }

    public static Map<ProfessionType, ProfessionXpConfig> all() {
        return CONFIGS;
    }
}

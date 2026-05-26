package me.ray.midgard.modules.professions.gui;

import me.ray.midgard.core.config.ConfigWrapper;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.professions.ProfessionType;
import me.ray.midgard.modules.professions.ProfessionsModule;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Dados visuais e temáticos de cada profissão para renderização no menu de progressão.
 * Imutável — carregado de definitions.yml na inicialização.
 */
public record ProfessionDefinition(
        ProfessionType type,
        Material menuIcon,
        String gradient,
        String description,
        List<String> xpSources,
        List<String> bonuses,
        List<String> abilities,
        Material xpSourceIcon,
        Material extraButtonIcon,
        String extraButtonName,
        List<String> extraButtonLore
) {

    private static final Map<ProfessionType, ProfessionDefinition> DEFINITIONS = new EnumMap<>(ProfessionType.class);

    /**
     * Carrega todas as definições dos arquivos individuais em professions/<id>.yml.
     * Chamado durante inicialização do módulo.
     */
    public static void loadAll() {
        DEFINITIONS.clear();

        ProfessionsModule module = ProfessionsModule.getInstance();
        if (module == null || module.getPlugin() == null) {
            MidgardLogger.warn("ProfessionDefinition.loadAll() chamado antes do módulo inicializar.");
            return;
        }

        int loaded = 0;
        for (ProfessionType type : ProfessionType.values()) {
            String path = "modules/professions/professions/" + type.getId() + ".yml";
            try {
                ConfigWrapper wrapper = new ConfigWrapper(module.getPlugin(), path);
                FileConfiguration config = wrapper.getConfig();
                ConfigurationSection sec = config.getConfigurationSection("definition");

                if (sec == null) {
                    MidgardLogger.warn("Seção 'definition' não encontrada em %s, ignorando.", path);
                    continue;
                }

                Material menuIcon = parseMaterial(sec.getString("menu-icon"), type.getIcon());
                String gradient = sec.getString("gradient", "<gold>");
                String description = sec.getString("description", "");
                List<String> xpSources = sec.getStringList("xp-sources");
                List<String> bonuses = sec.getStringList("bonuses");
                List<String> abilities = sec.getStringList("abilities");
                Material xpSourceIcon = parseMaterial(sec.getString("xp-source-icon"), Material.EXPERIENCE_BOTTLE);
                Material extraButtonIcon = parseMaterial(sec.getString("extra-button-icon"), Material.BOOK);
                String extraButtonName = sec.getString("extra-button-name", "");
                List<String> extraButtonLore = sec.getStringList("extra-button-lore");

                DEFINITIONS.put(type, new ProfessionDefinition(
                        type, menuIcon, gradient, description,
                        List.copyOf(xpSources), List.copyOf(bonuses), List.copyOf(abilities),
                        xpSourceIcon, extraButtonIcon, extraButtonName, List.copyOf(extraButtonLore)
                ));
                loaded++;
            } catch (Exception e) {
                MidgardLogger.error("Erro ao carregar definição da profissão '%s' de %s", type.getId(), path, e);
            }
        }

        MidgardLogger.info("Carregadas %d definições de profissão.", loaded);
    }

    private static Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) { return fallback; }
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            MidgardLogger.warn("Material inválido em definitions.yml: '%s', usando fallback.", name);
            return fallback;
        }
    }

    /**
     * Retorna a definição visual de uma profissão.
     */
    public static ProfessionDefinition get(ProfessionType type) {
        return DEFINITIONS.get(type);
    }

    /**
     * Retorna todas as definições registradas (imutável).
     */
    public static Map<ProfessionType, ProfessionDefinition> all() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }
}

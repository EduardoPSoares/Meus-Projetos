package me.ray.midgard.modules.professions.gui;

import me.ray.midgard.core.config.ConfigWrapper;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.professions.ProfessionType;
import me.ray.midgard.modules.professions.ProfessionsModule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de recompensas por nível para cada profissão.
 * Carregado de rewards.yml na inicialização.
 * Níveis sem registro retornam {@link ProfessionLevelReward#empty(int)}.
 */
public final class ProfessionRewardRegistry {

    private static final Map<ProfessionType, Map<Integer, ProfessionLevelReward>> REWARDS = new EnumMap<>(ProfessionType.class);

    private ProfessionRewardRegistry() {}

    /**
     * Carrega todas as recompensas dos arquivos individuais em professions/<id>.yml.
     * Chamado durante inicialização do módulo.
     */
    public static void loadAll() {
        REWARDS.clear();

        ProfessionsModule module = ProfessionsModule.getInstance();
        if (module == null || module.getPlugin() == null) {
            MidgardLogger.warn("ProfessionRewardRegistry.loadAll() chamado antes do módulo inicializar.");
            return;
        }

        int totalRewards = 0;
        for (ProfessionType type : ProfessionType.values()) {
            String path = "modules/professions/professions/" + type.getId() + ".yml";
            try {
                ConfigWrapper wrapper = new ConfigWrapper(module.getPlugin(), path);
                FileConfiguration config = wrapper.getConfig();
                ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");

                if (rewardsSection == null) {
                    MidgardLogger.warn("Seção 'rewards' não encontrada em %s, ignorando.", path);
                    continue;
                }

                Map<Integer, ProfessionLevelReward> map = new HashMap<>();
                for (String levelKey : rewardsSection.getKeys(false)) {
                    int level;
                    try {
                        level = Integer.parseInt(levelKey);
                    } catch (NumberFormatException e) {
                        MidgardLogger.warn("Nível inválido '%s' em %s, ignorando.", levelKey, path);
                        continue;
                    }

                    try {
                        ConfigurationSection sec = rewardsSection.getConfigurationSection(levelKey);
                        if (sec == null) { continue; }

                        String name = sec.getString("name", null);
                        List<String> bonuses = sec.getStringList("bonuses");
                        String ability = sec.getString("ability", null);
                        String abilityDesc = sec.getString("ability-desc", null);
                        List<String> perks = sec.getStringList("perks");

                        map.put(level, new ProfessionLevelReward(
                                level, name,
                                bonuses.isEmpty() ? List.of() : List.copyOf(bonuses),
                                ability, abilityDesc,
                                perks.isEmpty() ? List.of() : List.copyOf(perks)
                        ));
                        totalRewards++;
                    } catch (Exception e) {
                        MidgardLogger.error("Erro ao carregar recompensa nível %d de '%s'", level, type.getId(), e);
                    }
                }

                REWARDS.put(type, map);
            } catch (Exception e) {
                MidgardLogger.error("Erro ao carregar recompensas de '%s' em %s", type.getId(), path, e);
            }
        }

        MidgardLogger.info("Carregadas %d recompensas de nível.", totalRewards);
    }

    /**
     * Retorna a recompensa de um nível de uma profissão.
     * Retorna reward vazio se nenhum marco definido para o nível.
     */
    public static ProfessionLevelReward getReward(ProfessionType type, int level) {
        Map<Integer, ProfessionLevelReward> map = REWARDS.get(type);
        if (map == null) {
            return ProfessionLevelReward.empty(level);
        }
        return map.getOrDefault(level, ProfessionLevelReward.empty(level));
    }
}

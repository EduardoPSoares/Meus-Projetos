package me.ray.midgard.modules.combat.level;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.event.PlayerLevelUpEvent;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatConfig;
import me.ray.midgard.modules.combat.CombatData;
import me.ray.midgard.modules.combat.CombatModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gerenciador do sistema de níveis e experiência.
 * <p>
 * Responsável por adicionar experiência aos jogadores, calcular requisitos de nível
 * e processar a evolução de nível (Level Up).
 */
public class LevelManager {

    private final CombatConfig config;

    /**
     * Construtor do LevelManager.
     *
     * @param config Configuração do módulo de combate.
     */
    public LevelManager(CombatConfig config) {
        this.config = config;
    }

    /**
     * Calcula o XP que um jogador deve ganhar ao matar um mob.
     * Aplica escalonamento, penalidades de nível e variação RNG.
     * 
     * @param playerLevel O nível atual do jogador.
     * @param mobLevel O nível do mob morto.
     * @param baseMobXp O XP base do mob (se <= 0, usa o padrão do config).
     * @return A quantidade final de XP calculado.
     */
    public double calculateKillXp(int playerLevel, int mobLevel, double baseMobXp) {
        // 1. Base XP
        double finalXp = baseMobXp > 0 ? baseMobXp : config.xpGainDefaultBase;

        // 2. Scaling (Impacto do Nível do Mob)
        if (config.xpScalingMobLevelImpact) {
            // Ex: Mob Lv 10 * 5.0 = +50 XP
            finalXp += (mobLevel * config.xpScalingMobLevelMultiplier);
        }

        // 3. Disparity System (Risco vs Recompensa)
        if (config.xpDisparityEnabled) {
            int diff = playerLevel - mobLevel;

            // Penalidade (Jogador muito forte para o mob)
            if (diff > config.xpPenaltyThreshold) {
                int penaltyLevels = diff - config.xpPenaltyThreshold;
                double reduction = penaltyLevels * config.xpPenaltyReduction; // Ex: 0.15 * 2 = 0.30 (30%)
                
                double multiplier = Math.max(config.xpPenaltyMinCap, 1.0 - reduction); // MinCap (ex: 0.05)
                finalXp *= multiplier;
            }
            // Bônus (Mob mais forte que o jogador)
            else if ((-diff) > config.xpBonusThreshold) {
                int bonusLevels = (-diff) - config.xpBonusThreshold;
                double bonus = bonusLevels * config.xpBonusIncrement; // Ex: 0.08 * 5 = 0.40 (40%)
                
                double multiplier = Math.min(config.xpBonusMaxCap, 1.0 + bonus); // MaxCap (ex: 2.5)
                finalXp *= multiplier;
            }
        }

        // 4. Variance (RNG - Imprevisibilidade)
        if (config.xpVarianceEnabled) {
            double min = config.xpVarianceMin;
            double max = config.xpVarianceMax;
            double randomFactor = ThreadLocalRandom.current().nextDouble(min, max);
            finalXp *= randomFactor;
        }

        // Sanity Check: XP não pode ser negativo
        return Math.max(0, finalXp);
    }

    /**
     * Adiciona experiência a um jogador e verifica se ele subiu de nível.
     *
     * @param player O jogador que receberá a experiência.
     * @param amount A quantidade de experiência a ser adicionada.
     */
    public void addExperience(Player player, double amount) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }

        CombatData data = profile.getOrCreateData(CombatData.class);
        double currentXp = data.getExperience();
        int currentLevel = data.getLevel();

        if (currentLevel >= config.maxLevel) {
            updateVanillaExperience(player, currentLevel, 0); // Max level visual
            return;
        }

        currentXp += amount;
        
        // Verifica se subiu de nível
        while (currentXp >= getRequiredXp(currentLevel)) {
            double required = getRequiredXp(currentLevel);
            if (required <= 0) {
                break; // Prevent infinite loop if config produces zero/negative XP requirements
            }
            currentXp -= required;
            int oldLevel = currentLevel;
            currentLevel++;
            
            // Send level up message from messages.yml
            if (CombatModule.getInstance() != null) {
                String levelUpMsg = CombatModule.getInstance().getMessage("progression.combat_level_up")
                    .replace("%level%", String.valueOf(currentLevel))
                    .replace("%old_level%", String.valueOf(oldLevel));
                MessageUtils.send(player, levelUpMsg);
            }
            
            // Fire event
            PlayerLevelUpEvent event = new PlayerLevelUpEvent(player, oldLevel, currentLevel);
            Bukkit.getPluginManager().callEvent(event);
            
            if (currentLevel >= config.maxLevel) {
                currentXp = 0;
                break;
            }
        }

        data.setExperience(currentXp);
        data.setLevel(currentLevel);
        
        updateVanillaExperience(player, currentLevel, currentXp);
    }
    
    /**
     * Atualiza a barra de experiência e nível do Minecraft vanilla.
     * 
     * @param player O jogador.
     * @param level O nível atual do RPG.
     * @param currentXp A experiência atual no nível.
     */
    public void updateVanillaExperience(Player player, int level, double currentXp) {
        if (player == null) {
            return;
        }
        
        // Set Level
        player.setLevel(level);
        
        // Set Bar Progress
        double required = getRequiredXp(level);
        float progress = 0.0f;
        
        if (required > 0) {
            progress = (float) (currentXp / required);
        }
        
        // Clamp between 0 and 1
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        
        player.setExp(progress);
    }

    /**
     * Calcula a experiência necessária para passar do nível atual para o próximo.
     * Utiliza uma fórmula baseada em configuração (Base + Linear + Exponencial).
     *
     * @param level O nível atual.
     * @return A quantidade de XP necessária para o próximo nível.
     */
    public double getRequiredXp(int level) {
        // Fórmula: Base + (Nível * Linear) * (Exponencial ^ Nível)
        return config.xpRequirementsBase + (level * config.xpRequirementsLinear) * Math.pow(config.xpRequirementsExponential, level);
    }
}

package me.ray.midgard.modules.professions.penalty;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.professions.ProfessionData;
import me.ray.midgard.modules.professions.ProfessionType;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.xp.ProfessionXpConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gerenciador central de penalidades para não-profissionais.
 * Verifica se um jogador possui a profissão necessária e aplica penalidades quando não possui.
 *
 * Fluxo:
 * 1. Listener intercepta evento (block-break, craft, etc.)
 * 2. Listener chama este manager para verificar se há penalidade
 * 3. Manager verifica profissão ativa vs profissão requerida pelo material
 * 4. Se não-profissional, aplica penalidade configurada e retorna true
 */
public final class ProfessionPenaltyManager {

    private final ProfessionsModule module;
    private volatile boolean globalEnabled;
    private volatile long messageCooldownMs;

    /**
     * Mapa de cooldown: UUID+messageKey → timestamp da última mensagem.
     * Evita spam ao minerar múltiplos blocos ou realizar ações repetidas rapidamente.
     */
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();

    public ProfessionPenaltyManager(ProfessionsModule module) {
        this.module = Objects.requireNonNull(module);
        reloadConfig();
    }

    public void reloadConfig() {
        this.globalEnabled = module.getConfig().getBoolean("professions.penalties.enabled", true);
        this.messageCooldownMs = module.getConfig().getLong("professions.penalties.message-cooldown-ms", 3000L);
        messageCooldowns.clear();
    }

    /**
     * Limpa dados ao desligar o módulo. Evita memory leak entre reloads.
     */
    public void shutdown() {
        messageCooldowns.clear();
    }

    /**
     * Verifica se o jogador é profissional de um dado tipo.
     * Retorna true se o jogador tem essa profissão ativa.
     */
    public boolean isProfessional(Player player, ProfessionType type) {
        if (!globalEnabled) { return true; }
        if (hasExplicitBypass(player)) { return true; }

        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return true; } // Perfil não carregado — segurança, não penalizar

        ProfessionData data = profile.getData(ProfessionData.class);
        if (data == null) { return false; } // Sem dados de profissão — não é profissional

        return data.getActiveProfession() == type;
    }

    /**
     * Encontra a profissão que protege um material para uma ação específica.
     * Retorna null se nenhuma profissão protege esse material.
     */
    public ProfessionType findProfessionForMaterial(String action, Material material) {
        for (var entry : ProfessionXpConfig.all().entrySet()) {
            double xp = entry.getValue().getMaterialXp(action, material);
            if (xp > 0) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Verifica se o jogador está isento de penalidade para um material.
     * Diferente de isProfessional(), verifica TODAS as profissões que dão XP para o material,
     * não apenas uma. Necessário porque materiais como GOLDEN_APPLE pertencem a mais de uma profissão.
     */
    public boolean isPlayerExemptForMaterial(Player player, String action, Material material) {
        if (!globalEnabled) { return true; }
        if (hasExplicitBypass(player)) { return true; }

        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return true; }

        ProfessionData data = profile.getData(ProfessionData.class);
        if (data == null) { return false; }

        ProfessionType activeProfession = data.getActiveProfession();
        if (activeProfession == null) { return false; }

        for (var entry : ProfessionXpConfig.all().entrySet()) {
            double xp = entry.getValue().getMaterialXp(action, material);
            if (xp > 0 && entry.getKey() == activeProfession) {
                return true;
            }
        }
        return false;
    }

    /**
     * Encontra a profissão para ações param-based (enchant, brew).
     */
    public ProfessionType findProfessionForAction(String action) {
        for (var entry : ProfessionXpConfig.all().entrySet()) {
            if (entry.getValue().hasAction(action)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Verifica se a penalidade de uma profissão é aplicável ao material.
     */
    public ProfessionPenaltyConfig getPenaltyFor(ProfessionType type) {
        if (!globalEnabled) { return null; }
        ProfessionPenaltyConfig config = ProfessionPenaltyConfig.get(type);
        if (config == null || !config.isEnabled()) { return null; }
        return config;
    }

    /**
     * Rola chance — retorna true se a penalidade deve ser aplicada.
     * chance = 0.3 → 30% de chance de penalidade
     */
    public boolean rollChance(double chance) {
        if (chance <= 0.0) { return false; }
        if (chance >= 1.0) { return true; }
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    /**
     * Aplica um efeito de poção ao jogador (Mining Fatigue, Slowness, etc.).
     */
    public void applyEffect(Player player, PotionEffectType effectType, int durationTicks, int amplifier) {
        if (effectType == null || !player.isOnline()) { return; }
        try {
            player.addPotionEffect(new PotionEffect(effectType, durationTicks, amplifier, false, false, true));
        } catch (Exception e) {
            MidgardLogger.error("Erro ao aplicar efeito %s em %s", effectType.getName(), player.getName(), e);
        }
    }

    /**
     * Envia mensagem de penalidade ao jogador, respeitando cooldown para evitar spam.
     */
    public void sendPenaltyMessage(Player player, String messageKey) {
        if (!player.isOnline()) { return; }
        if (!checkMessageCooldown(player.getUniqueId(), messageKey)) { return; }
        try {
            String msg = module.getMessage("professions.penalties." + messageKey);
            if (msg == null || msg.isEmpty()) { return; }
            player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
        } catch (Exception e) {
            MidgardLogger.debug("Chave de mensagem de penalidade não encontrada: penalties.%s", messageKey);
        }
    }

    /**
     * Envia mensagem de penalidade com placeholder de profissão, respeitando cooldown.
     */
    public void sendPenaltyMessage(Player player, String messageKey, ProfessionType type) {
        if (!player.isOnline()) { return; }
        if (!checkMessageCooldown(player.getUniqueId(), messageKey)) { return; }
        try {
            String msg = module.getMessage("professions.penalties." + messageKey);
            if (msg == null || msg.isEmpty()) { return; }
            msg = msg.replace("%profession%", type.getDisplayName())
                     .replace("%symbol%", type.getSymbol());
            player.sendMessage(MiniMessage.miniMessage().deserialize(msg));
        } catch (Exception e) {
            MidgardLogger.debug("Chave de mensagem de penalidade não encontrada: penalties.%s", messageKey);
        }
    }

    /**
     * Verifica se a mensagem pode ser enviada (cooldown expirado).
     * Retorna true se pode enviar, false se ainda em cooldown.
     */
    private boolean checkMessageCooldown(UUID playerId, String messageKey) {
        String key = playerId.toString() + ":" + messageKey;
        long now = System.currentTimeMillis();
        // Operação atômica: verifica cooldown e atualiza timestamp de uma só vez
        boolean[] allowed = {false};
        messageCooldowns.compute(key, (k, lastSent) -> {
            if (lastSent == null || (now - lastSent) >= messageCooldownMs) {
                allowed[0] = true;
                return now;
            }
            return lastSent;
        });
        return allowed[0];
    }

    public boolean isGlobalEnabled() {
        return globalEnabled;
    }

    /**
     * Verifica se o jogador tem a permissão de bypass explicitamente definida.
     * Não herda do status OP — apenas permissões setadas via plugin de permissões ou diretamente.
     */
    private boolean hasExplicitBypass(Player player) {
        return player.isPermissionSet("midgard.profession.bypass")
                && player.hasPermission("midgard.profession.bypass");
    }
}

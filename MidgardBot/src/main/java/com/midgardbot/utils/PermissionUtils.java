package com.midgardbot.utils;

import com.midgardbot.config.BotConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilitário centralizado para verificação de permissões.
 * Elimina duplicação de lógica entre CommandManager e InteractionManager.
 */
public final class PermissionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionUtils.class);

    // Rate limiting: userId -> Map<commandName, lastUsageTimestamp>
    private static final Map<String, Map<String, Long>> commandCooldowns = new ConcurrentHashMap<>();
    private static final long DEFAULT_COOLDOWN_MS = 3000; // 3 segundos entre usos do mesmo comando

    private PermissionUtils() {}

    /**
     * Verifica se um membro tem permissão para executar um comando.
     *
     * @param member      Membro do Discord
     * @param permKey     Chave de permissão (ex: "PERM_CMD_BAN") ou null se público
     * @param commandName Nome do comando (para logging)
     * @return true se tem permissão, false se não
     */
    public static boolean hasPermission(Member member, String permKey, String commandName) {
        if (permKey == null) return true;

        List<String> allowedRoles = BotConfig.getAuthorizedRoles(permKey);
        if (allowedRoles.isEmpty()) return true;

        boolean hasRole = member.getRoles().stream()
                .map(Role::getId)
                .anyMatch(allowedRoles::contains);

        if (hasRole) return true;

        // Admin bypass
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            LOGGER.info("🛡️ Admin '{}' usou comando '{}' via bypass de permissão.",
                    member.getUser().getName(), commandName);
            return true;
        }

        LOGGER.warn("⛔ Acesso negado ao comando '{}' para o usuário '{}' (ID: {}).",
                commandName, member.getUser().getName(), member.getUser().getId());
        LOGGER.warn("   Cargos Exigidos (Config): {}", allowedRoles);
        LOGGER.warn("   Cargos do Usuário: {}",
                member.getRoles().stream().map(Role::getId).toList());
        return false;
    }

    /**
     * Verifica se o uso de um comando está em cooldown para o usuário (rate limiting).
     *
     * @param userId      ID do usuário
     * @param commandName Nome do comando
     * @return true se está em cooldown (deve bloquear), false se pode executar
     */
    public static boolean isRateLimited(String userId, String commandName) {
        long now = System.currentTimeMillis();
        Map<String, Long> userCooldowns = commandCooldowns.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        Long lastUsage = userCooldowns.get(commandName);

        if (lastUsage != null && now - lastUsage < DEFAULT_COOLDOWN_MS) {
            return true;
        }

        userCooldowns.put(commandName, now);
        return false;
    }

    /**
     * Limpa cooldowns expirados para evitar vazamento de memória.
     * Deve ser chamado periodicamente.
     */
    public static void cleanupCooldowns() {
        long now = System.currentTimeMillis();
        commandCooldowns.forEach((userId, userCooldowns) -> {
            userCooldowns.entrySet().removeIf(entry -> now - entry.getValue() > DEFAULT_COOLDOWN_MS * 10);
        });
        commandCooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}

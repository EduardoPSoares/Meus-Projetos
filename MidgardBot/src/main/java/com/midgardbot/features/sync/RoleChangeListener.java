package com.midgardbot.features.sync;

import com.midgardbot.features.link.LinkManager;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Listener de Mudança de Cargos.
 * Monitora eventos de adição e remoção de cargos no Discord.
 * Se o cargo estiver mapeado na configuração, inicia o processo de sincronização com o Minecraft.
 */
public class RoleChangeListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleChangeListener.class);

    @Override
    public void onReady(@NotNull net.dv8tion.jda.api.events.session.ReadyEvent event) {
        LOGGER.info("RoleChangeListener registrado e pronto! Monitorando eventos de cargo...");
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        try {
            LOGGER.debug("Evento de cargo detectado (ADD) para: {}", event.getMember().getEffectiveName());
            handleRoleChange(event.getMember().getId(), event.getRoles(), "add", event);
        } catch (Exception e) {
            LOGGER.error("Erro ao processar adição de cargo", e);
        }
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        try {
            handleRoleChange(event.getMember().getId(), event.getRoles(), "remove", event);
        } catch (Exception e) {
            LOGGER.error("Erro ao processar remoção de cargo", e);
        }
    }

    private void handleRoleChange(String targetId, java.util.List<Role> roles, String action, net.dv8tion.jda.api.events.guild.GenericGuildEvent event) {
        try {
            LOGGER.info("RoleChangeListener acionado. Target: " + targetId + ", Action: " + action);
            
            // 1. Verificar se o cargo adicionado/removido está no mapeamento
            for (Role role : roles) {
                LOGGER.info("Verificando role: " + role.getId() + " (" + role.getName() + ")");
                if (RoleSyncConfig.isSyncRole(role.getId())) {
                    String group = RoleSyncConfig.getGroupForRole(role.getId());
                    LOGGER.info("Role configurada encontrada! Grupo LuckPerms: " + group);
                    
                    // 2. Verificar quem executou a ação (Audit Log)
                    event.getGuild().retrieveAuditLogs()
                        .type(net.dv8tion.jda.api.audit.ActionType.MEMBER_ROLE_UPDATE)
                        .limit(1)
                        .queue(logs -> {
                            try {
                                if (logs.isEmpty()) {
                                    LOGGER.warn("AuditLog vazio.");
                                    return;
                                }
                                net.dv8tion.jda.api.audit.AuditLogEntry entry = logs.get(0);
                                
                                LOGGER.info("AuditLog Entry - Target: " + entry.getTargetId() + ", Executor: " + entry.getUser().getName());
                                
                                // Verifica se o log corresponde ao alvo
                                if (!entry.getTargetId().equals(targetId)) {
                                    LOGGER.warn("AuditLog target mismatch. Esperado: " + targetId + ", Encontrado: " + entry.getTargetId());
                                    return;
                                }
                                
                                // Verifica se o executor tem o cargo de admin necessario
                                String adminRoleId = RoleSyncConfig.getAdminRoleId();
                                
                                event.getGuild().retrieveMember(entry.getUser()).queue(executor -> {
                                    try {
                                        boolean hasAdminRole = executor.getRoles().stream()
                                            .anyMatch(r -> r.getId().equals(adminRoleId));
                                        
                                        LOGGER.info("Executor: " + executor.getEffectiveName() + ", HasAdminRole: " + hasAdminRole + " (Required: " + adminRoleId + ")");
                                            
                                        if (hasAdminRole) {
                                            processSync(targetId, action, group);
                                        } else {
                                            LOGGER.info("Sync ignorado: Executor " + executor.getEffectiveName() + " nao tem o cargo de admin necessario.");
                                        }
                                    } catch (Exception e) {
                                        LOGGER.error("Erro ao processar executor do sync", e);
                                    }
                                }, error -> LOGGER.error("Erro ao recuperar executor do sync", error));
                            } catch (Exception e) {
                                LOGGER.error("Erro ao processar audit logs", e);
                            }
                        }, error -> LOGGER.error("Erro ao recuperar audit logs", error));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro no handleRoleChange", e);
        }
    }

    private void processSync(String discordId, String action, String group) {
        UUID uuid = LinkManager.getUUID(discordId);
        LOGGER.info("Processando sync para DiscordID: " + discordId + " -> UUID: " + (uuid != null ? uuid : "Nao vinculado"));
        
        if (uuid != null) {
            // Usuario vinculado -> Enfileirar para o plugin
            RoleSyncManager.queueSync(uuid.toString(), action, group);
        } else {
            // Usuario nao vinculado -> Armazenar pendencia
            RoleSyncManager.addPending(discordId, action, group);
        }
    }
}

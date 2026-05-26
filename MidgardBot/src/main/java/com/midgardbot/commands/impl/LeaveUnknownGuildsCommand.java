package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.utils.EmbedUtils; // Adicionado import
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comando de Segurança (Leave Others).
 * Faz o bot sair de todos os servidores que não sejam o servidor principal configurado.
 * Medida de segurança para evitar que o bot seja adicionado em servidores não autorizados.
 */
public class LeaveUnknownGuildsCommand implements ISlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaveUnknownGuildsCommand.class);
    private static final String SAFE_GUILD_ID = "1443614176722288794";
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public String getName() {
        return "leaveothers";
    }

    @Override
    public String getDescription() {
        return "Executa o protocolo de isolamento (Sair de servidores não autorizados)";
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_LEAVEGUILDS";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Fallback para ADMINISTRATOR se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_LEAVEGUILDS").isEmpty() && 
            !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError("⛔ Acesso Negado", "Sem permissão.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        List<Guild> guildsToLeave = new ArrayList<>();
        for (Guild guild : event.getJDA().getGuilds()) {
            if (!guild.getId().equals(SAFE_GUILD_ID)) {
                guildsToLeave.add(guild);
            }
        }

        if (guildsToLeave.isEmpty()) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "✅ Ambiente Seguro", 
                "O bot já está isolado na instância principal.", 
                event.getJDA().getSelfUser()).build()).queue();
            return;
        }

        event.getHook().sendMessageEmbeds(EmbedUtils.createWarning(
            "☣️ Protocolo de Isolamento Iniciado",
            "Detectadas **" + guildsToLeave.size() + "** instâncias não autorizadas.\n" +
            "Iniciando rotina de saída sequencial (Rate-Limit Protection: 15s).",
            event.getJDA().getSelfUser()
        ).build()).queue();

        AtomicInteger index = new AtomicInteger(0);
        final java.util.concurrent.ScheduledFuture<?>[] future = new java.util.concurrent.ScheduledFuture<?>[1];
        
        future[0] = scheduler.scheduleAtFixedRate(() -> {
            if (index.get() >= guildsToLeave.size()) {
                if (future[0] != null) {
                    future[0].cancel(false);
                    LOGGER.info("[SYSTEM] Protocolo de isolamento finalizado.");
                }
                return;
            }

            Guild guild = guildsToLeave.get(index.getAndIncrement());
            String guildName = guild.getName();

            try {
                guild.leave().queue(
                    success -> LOGGER.info("🗑️ [AUTO-LEAVE] Desconectado de: " + guildName),
                    error -> LOGGER.error("❌ [AUTO-LEAVE] Falha ao desconectar de: " + guildName, error)
                );
            } catch (Exception e) {
                LOGGER.error("Erro crítico em leaveothers", e);
            }

        }, 0, 15, TimeUnit.SECONDS);
    }
}
package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

/**
 * Comando de Bypass.
 * Permite resetar o limite de tentativas de whitelist de um usuário específico.
 * Útil quando um jogador erra por pouco e merece outra chance imediata.
 */
public class BypassCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "bypass";
    }

    @Override
    public String getDescription() {
        return "Reseta manualmente o ciclo de tentativas e temporizadores de whitelist (Admin)";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "O usuário que receberá o reset", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_BYPASS";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Validação de Permissão Administrativa
        // Fallback para ADMINISTRATOR se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_BYPASS").isEmpty() && 
            !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError(
                "⛔ Acesso Negado",
                "Você não possui as credenciais administrativas necessárias para sobrescrever restrições do sistema.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        OptionMapping userOption = event.getOption("usuario");
        
        // Validação de Segurança (Defensiva)
        if (userOption == null) {
            event.replyEmbeds(EmbedUtils.createError(
                "⚠️ Parâmetro Inválido",
                "É necessário especificar um usuário alvo para executar o bypass.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        User target = userOption.getAsUser();
        String userId = target.getId();

        // Execução do Reset no Banco de Dados
        DataManager.resetAttempts(userId);
        // DataManager.removeCooldown(userId); // Mantido comentado conforme original (lógica integrada)

        // Resposta de Sucesso detalhada (Log de Auditoria Visual)
        event.replyEmbeds(EmbedUtils.createSuccess(
            "🔓 Override de Restrições Executado",
            "As restrições temporais e de tentativas foram removidas manualmente para este usuário.\n\n" +
            "👤 **Usuário Alvo:** " + target.getAsMention() + "\n" +
            "🆔 **ID do Registro:** `" + userId + "`\n" +
            "📉 **Ações Realizadas:**\n" +
            "• Zeramento do contador de falhas.\n" +
            "• Remoção imediata do Cooldown (Tempo de espera).\n\n" +
            "✅ **Status:** O usuário está liberado para iniciar uma nova submissão de whitelist imediatamente.",
            event.getJDA().getSelfUser()
        ).build()).queue();
    }
}
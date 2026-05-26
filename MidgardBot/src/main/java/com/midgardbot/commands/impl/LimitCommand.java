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
 * Comando de Limite.
 * Gerencia o sistema de limites de tentativas de whitelist.
 * Permite ativar/desativar o limite globalmente ou ver o status de um usuário.
 */
public class LimitCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "limit";
    }

    @Override
    public String getDescription() {
        return "Gerencia as políticas de cota para envio de whitelists";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "acao", "Operação de Gerenciamento", true)
                .addChoice("Alternar Status Global (Toggle)", "toggle")
                .addChoice("Incrementar Cota (Add)", "add")
                .addChoice("Reduzir Cota (Remove)", "remove"),
            new OptionData(OptionType.USER, "usuario", "Usuário alvo", false),
            new OptionData(OptionType.INTEGER, "quantidade", "Valor numérico", false)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_LIMIT";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_LIMIT").isEmpty() && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError("⛔ Acesso Negado", "Permissão insuficiente.", event.getJDA().getSelfUser()).build())
                .setEphemeral(true).queue();
            return;
        }

        String action = event.getOption("acao").getAsString();

        switch (action) {
            case "toggle": handleToggle(event); break;
            case "add": handleModify(event, true); break;
            case "remove": handleModify(event, false); break;
        }
    }

    private void handleToggle(SlashCommandInteractionEvent event) {
        boolean newState = !DataManager.isLimitEnabled();
        DataManager.setLimitEnabled(newState);

        if (newState) {
            event.replyEmbeds(EmbedUtils.createSuccess(
                "🔒 Sistema de Cotas: ATIVADO",
                "O limitador global de requisições foi habilitado.\nUsuários agora estão sujeitos ao teto diário de envios.",
                event.getJDA().getSelfUser()
            ).build()).queue();
        } else {
            event.replyEmbeds(EmbedUtils.createWarning(
                "🔓 Sistema de Cotas: DESATIVADO",
                "O limitador global foi suspenso.\nO envio de whitelists está **ilimitado** para todos os usuários.",
                event.getJDA().getSelfUser()
            ).build()).queue();
        }
    }

    private void handleModify(SlashCommandInteractionEvent event, boolean isAdd) {
        OptionMapping userOption = event.getOption("usuario");
        OptionMapping amountOption = event.getOption("quantidade");

        if (userOption == null || amountOption == null) {
            event.replyEmbeds(EmbedUtils.createError(
                "⚠️ Erro de Sintaxe",
                "Parâmetros obrigatórios ausentes:\n• `usuario`\n• `quantidade`",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        User target = userOption.getAsUser();
        int amount = amountOption.getAsInt();

        if (amount <= 0) {
            event.replyEmbeds(EmbedUtils.createError("⚠️ Valor Inválido", "A quantidade deve ser um inteiro positivo.", event.getJDA().getSelfUser()).build())
                .setEphemeral(true).queue();
            return;
        }

        if (isAdd) {
            DataManager.addAttempts(target.getId(), amount);
            event.replyEmbeds(EmbedUtils.createSuccess(
                "📈 Cota Expandida",
                "Adicionado **+" + amount + "** tentativas ao saldo do usuário.\n\n" +
                "👤 **Usuário:** " + target.getAsMention() + "\n" +
                "📊 **Saldo Atual:** `" + DataManager.getRemainingAttempts(target.getId()) + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();
        } else {
            DataManager.removeAttempts(target.getId(), amount);
            event.replyEmbeds(EmbedUtils.createSuccess(
                "📉 Cota Reduzida",
                "Removido **-" + amount + "** tentativas do saldo do usuário.\n\n" +
                "👤 **Usuário:** " + target.getAsMention() + "\n" +
                "📊 **Saldo Atual:** `" + DataManager.getRemainingAttempts(target.getId()) + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();
        }
    }
}
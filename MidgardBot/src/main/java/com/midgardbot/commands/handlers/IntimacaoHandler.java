package com.midgardbot.commands.handlers;

import com.midgardbot.config.BotConfig;
import com.midgardbot.features.intimacao.IntimacaoManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

/**
 * Handler para interações de botões do sistema de intimações.
 */
public final class IntimacaoHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntimacaoHandler.class);

    private IntimacaoHandler() {}

    /**
     * Processa interações de botão relacionadas a intimações.
     * @return true se o botão foi tratado, false caso contrário
     */
    public static boolean handleButton(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.startsWith("intimacao_confirmar:")) {
            handleConfirmar(event);
            return true;
        }

        if (id.startsWith("intimacao_retirar:")) {
            handleRetirar(event);
            return true;
        }

        return false;
    }

    private static void handleConfirmar(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        String userId = componentId.substring("intimacao_confirmar:".length());

        // Verificar se o usuário que clicou é o intimado
        if (!event.getUser().getId().equals(userId)) {
            event.replyEmbeds(
                EmbedUtils.createError("Acesso Negado", "Apenas o usuário intimado pode confirmar o recebimento.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        // Verificar se existe intimação ativa
        IntimacaoManager.IntimacaoData data = IntimacaoManager.getIntimacao(userId);
        if (data == null) {
            event.replyEmbeds(
                EmbedUtils.createWarning("Intimação não encontrada", "Não foi encontrada uma intimação ativa para você.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        if (data.confirmado) {
            event.replyEmbeds(
                EmbedUtils.createInfo("Já Confirmado", "Você já confirmou o recebimento desta intimação.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        // Confirmar recebimento
        boolean success = IntimacaoManager.confirmarRecebimento(userId, event.getJDA());

        if (success) {
            // Atualizar a mensagem removendo os botões
            event.editMessageEmbeds(
                new EmbedBuilder()
                    .setTitle("✅ Recebimento Confirmado — Midgard RPG")
                    .setDescription("Você confirmou o recebimento da intimação com sucesso.\n"
                        + "Compareça na data e horário da audiência.")
                    .setColor(Color.decode("#2ECC71"))
                    .addField("📋 Motivo", "> " + data.motivo, false)
                    .addField("📅 Data da Audiência", "> " + data.dataAudiencia, false)
                    .setFooter("MidgardBOT • Sistema de Intimações", event.getJDA().getSelfUser().getAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build()
            ).setComponents().queue(); // Remove action rows (buttons)
        } else {
            event.replyEmbeds(
                EmbedUtils.createError("Erro", "Não foi possível confirmar o recebimento. Tente novamente.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
        }
    }

    private static void handleRetirar(ButtonInteractionEvent event) {
        String userId = event.getComponentId().substring("intimacao_retirar:".length());

        // Verificar se quem clicou é staff (tem permissão)
        List<String> allowedRoles = BotConfig.getAuthorizedRoles("PERM_CMD_INTIMAR");
        boolean isStaff = event.getMember() != null && (
            event.getMember().getRoles().stream().anyMatch(role -> allowedRoles.contains(role.getId()))
            || event.getMember().hasPermission(Permission.ADMINISTRATOR)
        );

        if (!isStaff) {
            event.replyEmbeds(
                EmbedUtils.createError("Acesso Negado", "Apenas membros da staff podem retirar uma intimação.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        IntimacaoManager.IntimacaoData data = IntimacaoManager.getIntimacao(userId);
        if (data == null) {
            event.replyEmbeds(
                EmbedUtils.createWarning("Intimação não encontrada", "Não foi encontrada uma intimação ativa para este usuário.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        // Confirmar ação com resposta efêmera antes de deletar o canal
        event.replyEmbeds(
            EmbedUtils.createSuccess("Intimação Retirada",
                "A intimação de <@" + userId + "> foi retirada com sucesso.\nOs canais serão deletados em instantes.",
                event.getJDA().getSelfUser()).build()
        ).setEphemeral(true).queue();

        // Retirar a intimação (deleta canais, notifica usuário, restaura whitelist)
        IntimacaoManager.retirarIntimacao(userId, event.getUser(), event.getJDA());
    }
}

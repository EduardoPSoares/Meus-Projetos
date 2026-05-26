package com.midgardbot.commands.handlers;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.midgardbot.commands.impl.PlayerControlCommand;
import com.midgardbot.utils.EmbedUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

/**
 * Handler de interações do Painel de Controle de Jogador.
 * Processa botões, modais e select menus gerados pelo /player.
 */
public class PlayerControlHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerControlHandler.class);

    // Comandos RCON permitidos no painel (previne execução arbitrária)
    private static final Set<String> ALLOWED_RCON_COMMANDS = Set.of(
        "give", "tp", "effect", "gamemode", "tell", "msg", "kick",
        "xp", "experience", "clear", "time", "weather", "title",
        "playsound", "particle", "teleport", "spawnpoint", "setworldspawn"
    );

    private static final Set<String> ALLOWED_GAMEMODES = Set.of(
        "survival", "creative", "adventure", "spectator"
    );

    // ========================
    //    BUTTON INTERACTIONS
    // ========================

    public static boolean handleButton(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("pc_")) return false;

        // Verifica se a sessão existe
        String messageId = event.getMessageId();
        String targetNick = PlayerControlCommand.activeSessions.get(messageId);
        if (targetNick == null) {
            event.reply("⚠️ Sessão expirada. Use `/player` novamente.").setEphemeral(true).queue();
            return true;
        }

        // Verifica permissão (só o admin que abriu pode interagir)
        String ownerId = PlayerControlCommand.sessionOwners.get(messageId);
        if (ownerId != null && !ownerId.equals(event.getUser().getId()) 
            && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("⛔ Apenas quem abriu este painel pode usá-lo.").setEphemeral(true).queue();
            return true;
        }

        switch (id) {
            case "pc_tp":       handleTeleport(event, targetNick, messageId); break;
            case "pc_kick":     handleKickModal(event, targetNick, messageId); break;
            case "pc_gamemode": handleGamemodeMenu(event, targetNick, messageId); break;
            case "pc_effect":   handleEffectModal(event, targetNick, messageId); break;
            case "pc_msg":      handleMsgModal(event, targetNick, messageId); break;
            case "pc_execute":  handleExecuteModal(event, targetNick, messageId); break;
            case "pc_refresh":  handleRefresh(event, targetNick, messageId); break;
            case "pc_close":    handleClose(event, messageId); break;
            default: return false;
        }
        return true;
    }

    // ========================
    //   SELECT MENU INTERACTIONS
    // ========================

    public static boolean handleSelectMenu(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("pc_gm_select:")) return false;

        String messageId = id.split(":")[1];
        String targetNick = PlayerControlCommand.activeSessions.get(messageId);
        if (targetNick == null) {
            event.reply("⚠️ Sessão expirada.").setEphemeral(true).queue();
            return true;
        }

        String mode = event.getValues().get(0);
        if (!ALLOWED_GAMEMODES.contains(mode.toLowerCase())) {
            event.reply("⚠️ Gamemode inválido.").setEphemeral(true).queue();
            return true;
        }
        event.deferReply(true).queue();

        String result = PlayerControlCommand.executeRcon("gamemode " + mode + " " + targetNick);
        if (result != null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "Gamemode Alterado",
                "O gamemode de **" + targetNick + "** foi alterado para **" + mode.toUpperCase() + "**.\n\n" +
                "📡 `" + PlayerControlCommand.formatRcon(result) + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();
            LOGGER.info("Gamemode de {} alterado para {} por {}", targetNick, mode, event.getUser().getName());
        } else {
            sendRconError(event);
        }
        return true;
    }

    // ========================
    //    MODAL INTERACTIONS
    // ========================

    public static boolean handleModal(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        if (!modalId.startsWith("pc_modal_")) return false;

        // Extrai o messageId do modal ID: pc_modal_<tipo>:<messageId>
        String[] parts = modalId.split(":");
        if (parts.length < 2) return false;
        String messageId = parts[1];

        String targetNick = PlayerControlCommand.activeSessions.get(messageId);
        if (targetNick == null) {
            event.reply("⚠️ Sessão expirada. Use `/player` novamente.").setEphemeral(true).queue();
            return true;
        }

        event.deferReply(true).queue();

        if (modalId.startsWith("pc_modal_kick:")) {
            handleKickSubmit(event, targetNick);
        } else if (modalId.startsWith("pc_modal_effect:")) {
            handleEffectSubmit(event, targetNick);
        } else if (modalId.startsWith("pc_modal_msg:")) {
            handleMsgSubmit(event, targetNick);
        } else if (modalId.startsWith("pc_modal_execute:")) {
            handleExecuteSubmit(event, targetNick);
        } else {
            return false;
        }
        return true;
    }

    // ========================
    //    BUTTON HANDLERS
    // ========================

    private static void handleTeleport(ButtonInteractionEvent event, String targetNick, String messageId) {
        String adminNick = PlayerControlCommand.resolveAdminNick(event.getUser().getId());
        if (adminNick == null) {
            event.reply("❌ Sua conta Discord não está vinculada ao Minecraft.\nVincule sua conta para se teleportar.")
                .setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        String result = PlayerControlCommand.executeRcon("tp " + adminNick + " " + targetNick);
        if (result != null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "Teleporte Realizado",
                "Você (**" + adminNick + "**) foi teleportado até **" + targetNick + "**.\n\n" +
                "📡 `" + PlayerControlCommand.formatRcon(result) + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();
            LOGGER.info("{} se teleportou até {} via painel", adminNick, targetNick);
        } else {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(
                "Falha no Teleporte",
                "Não foi possível executar o teleporte. Verifique se ambos os jogadores estão online e o RCON está funcionando.",
                event.getJDA().getSelfUser()
            ).build()).queue();
        }
    }

    private static void handleKickModal(ButtonInteractionEvent event, String targetNick, String messageId) {
        TextInput reason = TextInput.create("kick_reason", "Motivo da expulsão", TextInputStyle.SHORT)
            .setPlaceholder("Ex: Comportamento inadequado")
            .setRequired(false)
            .setMaxLength(200)
            .build();

        Modal modal = Modal.create("pc_modal_kick:" + messageId, "Kick — " + targetNick)
            .addActionRow(reason)
            .build();

        event.replyModal(modal).queue();
    }

    private static void handleGamemodeMenu(ButtonInteractionEvent event, String targetNick, String messageId) {
        StringSelectMenu menu = StringSelectMenu.create("pc_gm_select:" + messageId)
            .setPlaceholder("Selecione o gamemode para " + targetNick)
            .addOption("🏕️ Survival", "survival", "Modo sobrevivência padrão")
            .addOption("🏗️ Creative", "creative", "Modo criativo com acesso total")
            .addOption("🗺️ Adventure", "adventure", "Modo aventura sem quebrar blocos")
            .addOption("👻 Spectator", "spectator", "Modo espectador invisível")
            .build();

        event.reply("🎮 **Alterar Gamemode de " + targetNick + ":**")
            .addActionRow(menu)
            .setEphemeral(true)
            .queue();
    }

    private static void handleEffectModal(ButtonInteractionEvent event, String targetNick, String messageId) {
        TextInput effect = TextInput.create("effect_name", "Efeito (ex: speed, strength, clear)", TextInputStyle.SHORT)
            .setPlaceholder("speed, strength, invisibility, clear...")
            .setRequired(true)
            .setMaxLength(50)
            .build();

        TextInput duration = TextInput.create("effect_duration", "Duração em segundos", TextInputStyle.SHORT)
            .setPlaceholder("30")
            .setRequired(false)
            .setMaxLength(10)
            .build();

        TextInput level = TextInput.create("effect_level", "Nível do efeito", TextInputStyle.SHORT)
            .setPlaceholder("1")
            .setRequired(false)
            .setMaxLength(3)
            .build();

        Modal modal = Modal.create("pc_modal_effect:" + messageId, "Efeitos — " + targetNick)
            .addActionRow(effect)
            .addActionRow(duration)
            .addActionRow(level)
            .build();

        event.replyModal(modal).queue();
    }

    private static void handleMsgModal(ButtonInteractionEvent event, String targetNick, String messageId) {
        TextInput msg = TextInput.create("msg_content", "Mensagem para " + targetNick, TextInputStyle.PARAGRAPH)
            .setPlaceholder("Digite a mensagem que será enviada ao jogador...")
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(500)
            .build();

        Modal modal = Modal.create("pc_modal_msg:" + messageId, "Mensagem — " + targetNick)
            .addActionRow(msg)
            .build();

        event.replyModal(modal).queue();
    }

    private static void handleExecuteModal(ButtonInteractionEvent event, String targetNick, String messageId) {
        TextInput cmd = TextInput.create("exec_command", "Comando (use {player} para o nick)", TextInputStyle.PARAGRAPH)
            .setPlaceholder("Ex: give {player} diamond 64")
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(500)
            .build();

        Modal modal = Modal.create("pc_modal_execute:" + messageId, "Comando — " + targetNick)
            .addActionRow(cmd)
            .build();

        event.replyModal(modal).queue();
    }

    private static void handleRefresh(ButtonInteractionEvent event, String targetNick, String messageId) {
        event.deferEdit().queue();

        EmbedBuilder embed = PlayerControlCommand.buildPlayerPanel(targetNick);

        List<ActionRow> actionRows = List.of(
            ActionRow.of(
                Button.primary("pc_tp", "📍 Teleportar"),
                Button.danger("pc_kick", "🦶 Kick"),
                Button.secondary("pc_gamemode", "🎮 Gamemode"),
                Button.secondary("pc_effect", "✨ Efeitos")
            ),
            ActionRow.of(
                Button.primary("pc_msg", "💬 Mensagem"),
                Button.secondary("pc_execute", "⚡ Comando"),
                Button.success("pc_refresh", "🔄 Atualizar"),
                Button.danger("pc_close", "❌ Fechar")
            )
        );

        event.getHook().editOriginalEmbeds(embed.build()).setComponents(actionRows).queue();
    }

    private static void handleClose(ButtonInteractionEvent event, String messageId) {
        PlayerControlCommand.activeSessions.remove(messageId);
        PlayerControlCommand.sessionOwners.remove(messageId);

        event.editMessageEmbeds(
            EmbedUtils.createInfo("Painel Fechado", "O painel de controle foi encerrado.", event.getJDA().getSelfUser()).build()
        ).setComponents().queue();
    }

    // ========================
    //    MODAL SUBMIT HANDLERS
    // ========================

    private static void handleKickSubmit(ModalInteractionEvent event, String targetNick) {
        String reason = event.getValue("kick_reason") != null 
            ? event.getValue("kick_reason").getAsString() 
            : "";
        if (reason.isEmpty()) reason = "Expulso por um administrador via Discord";

        String result = PlayerControlCommand.executeRcon("kick " + targetNick + " " + reason);
        if (result != null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "Jogador Expulso",
                "**" + targetNick + "** foi expulso do servidor.\n\n" +
                "📝 **Motivo:** " + reason + "\n" +
                "📡 `" + PlayerControlCommand.formatRcon(result) + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();
            LOGGER.info("Jogador {} kickado via painel por {}: {}", targetNick, event.getUser().getName(), reason);
        } else {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(
                "Falha ao Expulsar",
                "Não foi possível expulsar o jogador. Verifique se ele está online e o RCON está funcionando.",
                event.getJDA().getSelfUser()
            ).build()).queue();
        }
    }

    private static void handleEffectSubmit(ModalInteractionEvent event, String targetNick) {
        String effect = event.getValue("effect_name").getAsString().trim();
        // Valida nome do efeito: apenas letras, números, underscore e dois-pontos (namespace)
        if (!effect.matches("[a-zA-Z0-9_:]{1,50}")) {
            event.getHook().sendMessage("⚠️ Nome de efeito inválido. Use apenas letras, números e underscore.").queue();
            return;
        }

        int duration = 30;
        int level = 1;
        try {
            String durStr = event.getValue("effect_duration") != null ? event.getValue("effect_duration").getAsString().trim() : "";
            if (!durStr.isEmpty()) duration = Integer.parseInt(durStr);
        } catch (NumberFormatException ignored) {}
        try {
            String lvlStr = event.getValue("effect_level") != null ? event.getValue("effect_level").getAsString().trim() : "";
            if (!lvlStr.isEmpty()) level = Integer.parseInt(lvlStr);
        } catch (NumberFormatException ignored) {}

        String command;
        if (effect.equalsIgnoreCase("clear")) {
            command = "effect clear " + targetNick;
        } else {
            command = "effect give " + targetNick + " " + effect + " " + duration + " " + (level - 1);
        }

        String result = PlayerControlCommand.executeRcon(command);
        if (result != null) {
            String desc = effect.equalsIgnoreCase("clear")
                ? "Todos os efeitos de **" + targetNick + "** foram removidos."
                : "Efeito **" + effect + "** (Nível " + level + ", " + duration + "s) aplicado em **" + targetNick + "**.";

            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "Efeito Aplicado",
                desc + "\n\n📡 `" + PlayerControlCommand.formatRcon(result) + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();
            LOGGER.info("Efeito {} aplicado em {} por {}", effect, targetNick, event.getUser().getName());
        } else {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(
                "Falha ao Aplicar Efeito",
                "Não foi possível aplicar o efeito. Verifique o nome do efeito e se o jogador está online.",
                event.getJDA().getSelfUser()
            ).build()).queue();
        }
    }

    private static void handleMsgSubmit(ModalInteractionEvent event, String targetNick) {
        String message = event.getValue("msg_content").getAsString();

        String result = PlayerControlCommand.executeRcon("msg " + targetNick + " [Discord Staff] " + message);
        if (result != null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "Mensagem Enviada",
                "Mensagem enviada para **" + targetNick + "**:\n\n> " + message + "\n\n" +
                "📡 `" + PlayerControlCommand.formatRcon(result) + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();
        } else {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(
                "Falha ao Enviar",
                "Não foi possível enviar a mensagem. Verifique se o jogador está online.",
                event.getJDA().getSelfUser()
            ).build()).queue();
        }
    }

    private static void handleExecuteSubmit(ModalInteractionEvent event, String targetNick) {
        String command = event.getValue("exec_command").getAsString();
        String resolvedCommand = command.replace("{player}", targetNick);

        // Valida comando contra allowlist para prevenir execução arbitrária
        String baseCommand = resolvedCommand.trim().split("\\s+")[0].toLowerCase();
        // Remove namespace (ex: minecraft:give -> give)
        if (baseCommand.contains(":")) baseCommand = baseCommand.substring(baseCommand.indexOf(':') + 1);
        if (!ALLOWED_RCON_COMMANDS.contains(baseCommand)) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(
                "Comando Bloqueado",
                "O comando `" + baseCommand + "` não é permitido pelo painel de controle.\n" +
                "Comandos permitidos: " + String.join(", ", ALLOWED_RCON_COMMANDS),
                event.getJDA().getSelfUser()
            ).build()).queue();
            LOGGER.warn("Comando RCON bloqueado '{}' tentado por {} (alvo: {})", resolvedCommand, event.getUser().getName(), targetNick);
            return;
        }

        String result = PlayerControlCommand.executeRcon(resolvedCommand);
        if (result != null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "Comando Executado",
                "Comando executado com sucesso!\n\n" +
                "📝 **Comando:** `" + resolvedCommand + "`\n" +
                "📡 **Resposta:** `" + PlayerControlCommand.formatRcon(result) + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();
            LOGGER.info("Comando RCON '{}' executado por {} (alvo: {})", resolvedCommand, event.getUser().getName(), targetNick);
        } else {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(
                "Falha ao Executar",
                "Não foi possível executar o comando. Verifique a sintaxe e se o RCON está funcionando.\n\n" +
                "📝 **Comando:** `" + resolvedCommand + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();
        }
    }

    // ========================
    //    UTILITÁRIOS
    // ========================

    private static void sendRconError(StringSelectInteractionEvent event) {
        event.getHook().sendMessageEmbeds(EmbedUtils.createError(
            "Erro de Conexão RCON",
            "Não foi possível conectar ao servidor. Verifique se o servidor está online e o RCON está configurado.",
            event.getJDA().getSelfUser()
        ).build()).queue();
    }
}

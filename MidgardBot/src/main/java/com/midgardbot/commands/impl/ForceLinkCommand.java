package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.BotConfig;
import com.midgardbot.features.link.LinkManager;
import com.midgardbot.utils.EmbedUtils;
import com.midgardbot.utils.MojangUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.UUID;

public class ForceLinkCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "forcelink";
    }

    @Override
    public String getDescription() {
        return "Vincula forçadamente um usuário do Discord a um nick do Minecraft.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "O usuário do Discord a ser vinculado", true),
            new OptionData(OptionType.STRING, "nickname", "O nickname do Minecraft", true),
            new OptionData(OptionType.BOOLEAN, "offline", "Usar UUID offline (para pirata/cracked)", false)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_FORCELINK";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Verificação de permissão (padrão para comandos administrativos)
        boolean isConfigured = !BotConfig.getAuthorizedRoles("PERM_CMD_FORCELINK").isEmpty();
        if (!isConfigured && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
             event.replyEmbeds(
                EmbedUtils.createError("Permissão Negada", "Você não tem permissão para usar este comando.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        User targetUser = event.getOption("usuario").getAsUser();
        String nickname = event.getOption("nickname").getAsString();
        boolean offline = event.getOption("offline") != null && event.getOption("offline").getAsBoolean();

        event.deferReply().queue();

        // 1. Verificar se já está vinculado
        if (LinkManager.isLinked(targetUser.getId())) {
            event.getHook().sendMessageEmbeds(
                EmbedUtils.createError("Já Vinculado", "Este usuário já possui uma conta vinculada.", event.getJDA().getSelfUser()).build()
            ).queue();
            return;
        }

        // 2. Buscar UUID
        UUID uuid = null;
        
        if (offline) {
            uuid = MojangUtils.getOfflineUUID(nickname);
        } else {
            String uuidStr = MojangUtils.getUUID(nickname);
            if (uuidStr != null) {
                uuid = UUID.fromString(uuidStr);
            }
        }

        if (uuid == null) {
            event.getHook().sendMessageEmbeds(
                EmbedUtils.createError("Jogador Não Encontrado", 
                    "Não foi possível encontrar o UUID Premium para o nick: **" + nickname + "**.\n\n" +
                    "Se este for um jogador pirata/offline, use a opção `offline: True`.", 
                    event.getJDA().getSelfUser()).build()
            ).queue();
            return;
        }

        // 3. Verificar se o UUID já está vinculado a outra conta
        String existingDiscordId = LinkManager.getDiscordId(uuid);
        if (existingDiscordId != null) {
             event.getHook().sendMessageEmbeds(
                EmbedUtils.createError("Conflito", "Esta conta Minecraft já está vinculada ao usuário <@" + existingDiscordId + ">.", event.getJDA().getSelfUser()).build()
            ).queue();
            return;
        }

        // 4. Realizar o vínculo
        boolean success = LinkManager.forceLink(targetUser.getId(), uuid);

        if (success) {
            event.getHook().sendMessageEmbeds(
                EmbedUtils.createSuccess("Vínculo Realizado", 
                    "A conta **" + nickname + "** (" + (offline ? "Offline" : "Premium") + ") foi vinculada com sucesso a " + targetUser.getAsMention() + ".", 
                    event.getJDA().getSelfUser()).build()
            ).queue();
        } else {
            event.getHook().sendMessageEmbeds(
                EmbedUtils.createError("Erro", "Ocorreu um erro ao tentar salvar o vínculo.", event.getJDA().getSelfUser()).build()
            ).queue();
        }
    }
}

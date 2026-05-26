package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatus;
import com.midgardbot.data.WhitelistStatusInfo;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.List;
import java.util.Map;

public class FindUserCommand implements ISlashCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(FindUserCommand.class);

    @Override
    public String getName() {
        return "finduser";
    }

    @Override
    public String getDescription() {
        return "Encontra um usuário do Discord pelo nickname do Minecraft";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "nickname", "O nickname do jogador", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_FINDUSER";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_FINDUSER").isEmpty() && !event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER)) {
             event.replyEmbeds(EmbedUtils.createError("⛔ Acesso Negado", "Você precisa de permissão de Gerenciar Servidor.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
             return;
        }
        String nickname = event.getOption("nickname").getAsString().trim();
        
        // Primeiro tenta buscar no cache
        Map<String, WhitelistStatusInfo> allStatus = DataManager.getAllStatus();
        String foundUserId = null;
        WhitelistStatusInfo foundInfo = null;

        for (Map.Entry<String, WhitelistStatusInfo> entry : allStatus.entrySet()) {
            if (entry.getValue().nickname != null && entry.getValue().nickname.trim().equalsIgnoreCase(nickname)) {
                foundUserId = entry.getKey();
                foundInfo = entry.getValue();
                break;
            }
        }

        // Se não achou no cache, busca no banco de dados
        if (foundUserId == null) {
            Map.Entry<String, WhitelistStatusInfo> dbResult = com.midgardbot.data.DatabaseManager.getWhitelistByNickname(nickname);
            if (dbResult != null) {
                foundUserId = dbResult.getKey();
                foundInfo = dbResult.getValue();
            }
        }

        // Se ainda não achou, tenta buscar via API da Mojang + Tabela de Links
        if (foundUserId == null) {
            // Executa em thread separada para não bloquear o gateway, mas como é comando slash, temos 3s.
            // Idealmente deferReply, mas vamos tentar rápido.
            String uuidStr = com.midgardbot.utils.MojangUtils.getUUID(nickname);
            if (uuidStr != null) {
                try {
                    java.util.UUID realUUID = java.util.UUID.fromString(uuidStr);
                    String linkedDiscordId = com.midgardbot.features.link.LinkManager.getDiscordId(realUUID);
                    
                    if (linkedDiscordId != null) {
                        foundUserId = linkedDiscordId;
                        foundInfo = new WhitelistStatusInfo(
                            WhitelistStatus.APPROVED, 
                            "Encontrado via Link de Conta (Mojang API)", 
                            nickname, 
                            "{}", 
                            false
                        );
                    }
                } catch (Exception e) { LOGGER.debug("Erro ao buscar usuário por nickname no banco de dados", e); }
            }
        }

        // Se ainda não achou, tenta buscar membros do Discord com esse apelido/nome
        if (foundUserId == null) {
            List<net.dv8tion.jda.api.entities.Member> members = event.getGuild().getMembersByNickname(nickname, true);
            if (members.isEmpty()) {
                members = event.getGuild().getMembersByName(nickname, true);
            }
            
            if (!members.isEmpty()) {
                net.dv8tion.jda.api.entities.Member member = members.get(0);
                foundUserId = member.getId();
                foundInfo = new WhitelistStatusInfo(
                    WhitelistStatus.PENDING, 
                    "Encontrado via Discord (Não registrado no DB)", 
                    nickname, 
                    "{}", 
                    false
                );
            }
        }

        if (foundUserId != null) {
            User user = event.getJDA().getUserById(foundUserId);
            if (user == null) {
                // Tenta recuperar o usuário via API se não estiver em cache
                try {
                    user = event.getJDA().retrieveUserById(foundUserId).complete();
                } catch (Exception e) { LOGGER.debug("Erro ao recuperar usuário via API: {}", foundUserId, e); }
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🔍 Usuário Encontrado");
            embed.setColor(Color.GREEN);
            embed.addField("Nickname", foundInfo.nickname, true);
            embed.addField("Discord", user != null ? user.getAsMention() : foundUserId, true);
            embed.addField("ID", foundUserId, true);
            embed.addField("Status", foundInfo.status.label, true);
            
            event.replyEmbeds(embed.build()).queue();
        } else {
            event.replyEmbeds(EmbedUtils.createError(
                "Não Encontrado",
                "Nenhum usuário encontrado com o nickname: " + nickname,
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
        }
    }
}

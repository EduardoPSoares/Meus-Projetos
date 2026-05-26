package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatusInfo;
import com.midgardbot.features.link.LinkManager;
import com.midgardbot.utils.EmbedUtils;
import com.midgardbot.utils.MojangUtils;
import com.midgardbot.utils.RconClient;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comando /player - Abre painel interativo de controle total do jogador.
 * Exibe embed com todas as informações e botões de ação (TP, Kick, Gamemode, Efeitos, Msg, Comando).
 */
public class PlayerControlCommand implements ISlashCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerControlCommand.class);

    /** messageId -> nick do jogador alvo */
    public static final ConcurrentHashMap<String, String> activeSessions = new ConcurrentHashMap<>();
    /** messageId -> discordId do admin que abriu o painel */
    public static final ConcurrentHashMap<String, String> sessionOwners = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "player";
    }

    @Override
    public String getDescription() {
        return "Abre o painel de controle total de um jogador (Admin).";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "Usuário do Discord vinculado", false),
            new OptionData(OptionType.STRING, "nick", "Nickname do Minecraft", false)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_PLAYER";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        boolean isConfigured = !BotConfig.getAuthorizedRoles("PERM_CMD_PLAYER").isEmpty();
        if (!isConfigured && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError(
                "Acesso Negado",
                "Você não tem permissão para usar este comando.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        String targetNick = resolveNickname(event);
        if (targetNick == null) {
            event.replyEmbeds(EmbedUtils.createError(
                "Jogador Não Encontrado",
                "Informe um **usuário do Discord vinculado** ou um **nickname do Minecraft**.\n\n" +
                "**Uso:**\n" +
                "`/player usuario:@User`\n" +
                "`/player nick:Steve`",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();

        EmbedBuilder embed = buildPlayerPanel(targetNick);

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

        event.getHook().sendMessageEmbeds(embed.build()).setComponents(actionRows).queue(msg -> {
            activeSessions.put(msg.getId(), targetNick);
            sessionOwners.put(msg.getId(), event.getUser().getId());
        });

        LOGGER.info("Painel de controle aberto para jogador {} por {}", targetNick, event.getUser().getName());
    }

    /**
     * Constrói o embed do painel com todas as informações do jogador.
     */
    public static EmbedBuilder buildPlayerPanel(String nick) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🎮 Painel de Controle — " + nick);
        embed.setColor(EmbedUtils.COLOR_PRIMARY);
        embed.setThumbnail("https://mc-heads.net/avatar/" + nick + "/128");
        embed.setTimestamp(Instant.now());

        // --- Informações de Conta ---
        String linkedDiscordId = null;
        String uuidStr = MojangUtils.getUUID(nick);

        if (uuidStr != null) {
            embed.addField("🆔 UUID (Premium)", "`" + uuidStr + "`", false);
            try {
                UUID uuid = UUID.fromString(uuidStr);
                linkedDiscordId = LinkManager.getDiscordId(uuid);
            } catch (Exception e) { LOGGER.debug("Erro ao obter Discord ID vinculado ao UUID", e); }
        } else {
            UUID offlineUuid = MojangUtils.getOfflineUUID(nick);
            embed.addField("🆔 UUID (Offline)", "`" + offlineUuid.toString() + "`", false);
            linkedDiscordId = LinkManager.getDiscordId(offlineUuid);
        }

        if (linkedDiscordId != null) {
            embed.addField("🔗 Discord", "<@" + linkedDiscordId + ">", true);

            WhitelistStatusInfo status = DataManager.getStatus(linkedDiscordId);
            if (status != null) {
                String statusText;
                switch (status.status) {
                    case APPROVED: statusText = "✅ Aprovado"; break;
                    case REJECTED: statusText = "❌ Reprovado"; break;
                    default: statusText = "🕒 Pendente"; break;
                }
                embed.addField("📋 Whitelist", statusText, true);
            }
        } else {
            embed.addField("🔗 Discord", "Não vinculado", true);
            embed.addField("📋 Whitelist", "N/A", true);
        }

        // --- Dados In-Game via RCON ---
        String rconHost = BotConfig.getRconHost();
        String rconPass = BotConfig.getRconPassword();

        if (rconHost != null && !rconHost.isEmpty() && rconPass != null && !rconPass.isEmpty()) {
            try (RconClient rcon = new RconClient(rconHost, BotConfig.getRconPort(), rconPass)) {
                rcon.connect();

                String listResponse = rcon.sendCommand("list");
                boolean isOnline = listResponse.toLowerCase().contains(nick.toLowerCase());
                embed.addField("🟢 Status", isOnline ? "🟢 **Online**" : "🔴 **Offline**", true);

                if (isOnline) {
                    String posResponse = rcon.sendCommand("data get entity " + nick + " Pos");
                    if (isValidResponse(posResponse)) {
                        embed.addField("📍 Posição", formatRcon(posResponse), false);
                    }

                    String healthResponse = rcon.sendCommand("data get entity " + nick + " Health");
                    if (isValidResponse(healthResponse)) {
                        embed.addField("❤️ Vida", formatRcon(healthResponse), true);
                    }

                    String levelResponse = rcon.sendCommand("data get entity " + nick + " XpLevel");
                    if (isValidResponse(levelResponse)) {
                        embed.addField("⭐ Nível XP", formatRcon(levelResponse), true);
                    }

                    String gmResponse = rcon.sendCommand("data get entity " + nick + " playerGameType");
                    if (isValidResponse(gmResponse)) {
                        embed.addField("🎯 Gamemode", parseGamemode(formatRcon(gmResponse)), true);
                    }

                    String dimResponse = rcon.sendCommand("data get entity " + nick + " Dimension");
                    if (isValidResponse(dimResponse)) {
                        embed.addField("🌍 Dimensão", formatRcon(dimResponse), true);
                    }

                    String playtimeResponse = rcon.sendCommand("playtime " + nick);
                    if (isValidResponse(playtimeResponse) && !playtimeResponse.toLowerCase().contains("unknown")) {
                        embed.addField("⏱️ Tempo de Jogo", formatRcon(playtimeResponse), true);
                    }
                } else {
                    embed.setColor(EmbedUtils.COLOR_WARNING);
                    embed.addField("ℹ️ Info", "O jogador está **offline**. Algumas ações podem não funcionar.", false);
                }
            } catch (IOException e) {
                embed.addField("⚠️ RCON", "Falha ao conectar: `" + e.getMessage() + "`", false);
                embed.setColor(EmbedUtils.COLOR_ERROR);
            }
        } else {
            embed.addField("⚠️ RCON", "Não configurado no `config.env`", false);
            embed.setColor(EmbedUtils.COLOR_ERROR);
        }

        embed.setFooter("Use os botões abaixo para controlar o jogador");
        return embed;
    }

    // ========== Utilitários ==========

    private String resolveNickname(SlashCommandInteractionEvent event) {
        if (event.getOption("nick") != null) {
            return event.getOption("nick").getAsString().trim();
        }
        if (event.getOption("usuario") != null) {
            User targetUser = event.getOption("usuario").getAsUser();
            String discordId = targetUser.getId();

            UUID uuid = LinkManager.getUUID(discordId);
            if (uuid != null) {
                String nick = MojangUtils.getNickname(uuid.toString().replace("-", ""));
                if (nick != null) return nick;
            }

            WhitelistStatusInfo status = DataManager.getStatus(discordId);
            if (status != null && status.nickname != null && !status.nickname.isEmpty()) {
                return status.nickname;
            }
        }
        return null;
    }

    static boolean isValidResponse(String response) {
        return response != null && !response.isEmpty()
            && !response.toLowerCase().contains("no entity")
            && !response.toLowerCase().contains("not found");
    }

    public static String formatRcon(String response) {
        if (response == null) return "N/A";
        String cleaned = response.replaceAll("§[0-9a-fk-or]", "").trim();
        return cleaned.isEmpty() ? "N/A" : cleaned;
    }

    static String parseGamemode(String raw) {
        if (raw.contains("0")) return "🏕️ Survival";
        if (raw.contains("1")) return "🏗️ Creative";
        if (raw.contains("2")) return "🗺️ Adventure";
        if (raw.contains("3")) return "👻 Spectator";
        return raw;
    }

    /**
     * Executa um comando RCON e retorna a resposta ou null em caso de erro.
     */
    public static String executeRcon(String command) {
        String host = BotConfig.getRconHost();
        String pass = BotConfig.getRconPassword();
        if (host == null || host.isEmpty() || pass == null || pass.isEmpty()) return null;

        try (RconClient rcon = new RconClient(host, BotConfig.getRconPort(), pass)) {
            rcon.connect();
            return rcon.sendCommand(command);
        } catch (IOException e) {
            LoggerFactory.getLogger(PlayerControlCommand.class).error("Erro RCON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Resolve o nickname do admin a partir do seu Discord ID.
     */
    public static String resolveAdminNick(String adminDiscordId) {
        UUID adminUuid = LinkManager.getUUID(adminDiscordId);
        if (adminUuid != null) {
            String nick = MojangUtils.getNickname(adminUuid.toString().replace("-", ""));
            if (nick != null) return nick;
        }
        WhitelistStatusInfo status = DataManager.getStatus(adminDiscordId);
        if (status != null && status.nickname != null && !status.nickname.isEmpty()) {
            return status.nickname;
        }
        return null;
    }
}

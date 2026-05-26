package com.midgardbot.commands.handlers;

import com.midgardbot.commands.impl.PermaDeathCommand;
import com.midgardbot.commands.impl.PlayerControlCommand;
import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DatabaseManager;
import com.midgardbot.utils.EmbedUtils;
import com.midgardbot.utils.RconClient;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler de interações do painel PermaDeath.
 * Gerencia botões, modais e select menus para o controle de morte permanente via Discord.
 */
public class PermaDeathHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermaDeathHandler.class);
    private static final int PLAYERS_PER_PAGE = 8;

    // ========================
    //    BUTTON INTERACTIONS
    // ========================

    public static boolean handleButton(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("pd_")) return false;

        // Verifica permissão admin
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("⛔ Apenas administradores podem usar este painel.").setEphemeral(true).queue();
            return true;
        }

        String messageId = event.getMessageId();

        switch (id) {
            case "pd_list":     handleListDead(event, messageId, 0); break;
            case "pd_search":   handleSearchModal(event, messageId); break;
            case "pd_revive":   handleReviveModal(event, messageId); break;
            case "pd_delete":   handleDeleteModal(event, messageId); break;
            case "pd_toggle":   handleToggle(event, messageId); break;
            case "pd_reload":   handleReload(event, messageId); break;
            case "pd_stats":    handleStats(event, messageId); break;
            case "pd_close":    handleClose(event, messageId); break;
            case "pd_back":     handleBack(event, messageId); break;
            default:
                if (id.startsWith("pd_page_next:")) {
                    String msgId = id.split(":")[1];
                    int currentPage = PermaDeathCommand.sessionPages.getOrDefault(msgId, 0);
                    handleListDead(event, msgId, currentPage + 1);
                } else if (id.startsWith("pd_page_prev:")) {
                    String msgId = id.split(":")[1];
                    int currentPage = PermaDeathCommand.sessionPages.getOrDefault(msgId, 0);
                    handleListDead(event, msgId, Math.max(0, currentPage - 1));
                } else {
                    return false;
                }
        }
        return true;
    }

    // ========================
    //    MODAL INTERACTIONS
    // ========================

    public static boolean handleModal(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        if (!modalId.startsWith("pd_modal_")) return false;

        event.deferReply(true).queue();

        if (modalId.startsWith("pd_modal_search:")) {
            handleSearchSubmit(event);
        } else if (modalId.startsWith("pd_modal_revive:")) {
            handleReviveSubmit(event);
        } else if (modalId.startsWith("pd_modal_delete:")) {
            handleDeleteSubmit(event);
        } else {
            return false;
        }
        return true;
    }

    // ========================
    //    SELECT MENU (unused for now but ready)
    // ========================

    public static boolean handleSelectMenu(StringSelectInteractionEvent event) {
        return false; // No select menus used in this handler yet
    }

    // ========================
    //    BUTTON HANDLERS
    // ========================

    private static void handleListDead(ButtonInteractionEvent event, String messageId, int page) {
        event.deferEdit().queue();

        List<DeathRecord> allDeaths = queryAllDeaths();
        int totalPages = Math.max(1, (int) Math.ceil(allDeaths.size() / (double) PLAYERS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        PermaDeathCommand.sessionPages.put(messageId, page);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 Jogadores Mortos — Página " + (page + 1) + "/" + totalPages);
        embed.setColor(EmbedUtils.COLOR_ERROR);
        embed.setTimestamp(Instant.now());

        if (allDeaths.isEmpty()) {
            embed.setDescription("Nenhum jogador morto encontrado no sistema.\n\n*O Valhalla está vazio... por enquanto.*");
        } else {
            StringBuilder sb = new StringBuilder();
            int start = page * PLAYERS_PER_PAGE;
            int end = Math.min(start + PLAYERS_PER_PAGE, allDeaths.size());

            for (int i = start; i < end; i++) {
                DeathRecord r = allDeaths.get(i);
                String stateEmoji = getStateEmoji(r.deathState);
                sb.append(String.format("**%d.** %s `%s` — Morto por **%s**\n",
                    i + 1, stateEmoji, r.playerName, r.killerName));
                sb.append(String.format("   📅 `%s` • 📍 `%s: %d, %d, %d`\n\n",
                    r.deathTime != null ? r.deathTime : "N/A",
                    r.world != null ? r.world : "?",
                    (int) r.x, (int) r.y, (int) r.z));
            }

            embed.setDescription(sb.toString());
            embed.setFooter("Total: " + allDeaths.size() + " mortos • Use 🔍 Buscar para detalhes");
        }

        List<Button> navButtons = new ArrayList<>();
        navButtons.add(Button.secondary("pd_page_prev:" + messageId, "◀️ Anterior").withDisabled(page <= 0));
        navButtons.add(Button.secondary("pd_page_next:" + messageId, "▶️ Próximo").withDisabled(page >= totalPages - 1));
        navButtons.add(Button.primary("pd_back", "🔙 Voltar"));

        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(navButtons))
            .queue();
    }

    private static void handleSearchModal(ButtonInteractionEvent event, String messageId) {
        TextInput playerName = TextInput.create("pd_player_name", "Nome do Jogador", TextInputStyle.SHORT)
            .setPlaceholder("Ex: Steve")
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(16)
            .build();

        Modal modal = Modal.create("pd_modal_search:" + messageId, "🔍 Buscar Jogador Morto")
            .addActionRow(playerName)
            .build();

        event.replyModal(modal).queue();
    }

    private static void handleReviveModal(ButtonInteractionEvent event, String messageId) {
        TextInput playerName = TextInput.create("pd_player_name", "Nome do Jogador para Ressuscitar", TextInputStyle.SHORT)
            .setPlaceholder("Ex: Steve")
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(16)
            .build();

        Modal modal = Modal.create("pd_modal_revive:" + messageId, "♻️ Ressuscitar Jogador")
            .addActionRow(playerName)
            .build();

        event.replyModal(modal).queue();
    }

    private static void handleDeleteModal(ButtonInteractionEvent event, String messageId) {
        TextInput playerName = TextInput.create("pd_player_name", "Nome do Jogador", TextInputStyle.SHORT)
            .setPlaceholder("Ex: Steve")
            .setRequired(true)
            .setMinLength(1)
            .setMaxLength(16)
            .build();

        TextInput confirm = TextInput.create("pd_confirm", "Digite CONFIRMAR para prosseguir", TextInputStyle.SHORT)
            .setPlaceholder("CONFIRMAR")
            .setRequired(true)
            .setMinLength(9)
            .setMaxLength(9)
            .build();

        Modal modal = Modal.create("pd_modal_delete:" + messageId, "🗑️ Deletar Registro de Morte")
            .addActionRow(playerName)
            .addActionRow(confirm)
            .build();

        event.replyModal(modal).queue();
    }

    private static void handleToggle(ButtonInteractionEvent event, String messageId) {
        event.deferReply(true).queue();

        String result = executeRcon("rdeath toggle");
        if (result != null) {
            String cleaned = PlayerControlCommand.formatRcon(result);
            boolean enabled = cleaned.toLowerCase().contains("ativado") || cleaned.toLowerCase().contains("enabled");

            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "PermaDeath Toggle",
                "O sistema PermaDeath foi **" + (enabled ? "ATIVADO ✅" : "DESATIVADO ❌") + "**.\n\n" +
                "📡 `" + cleaned + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();

            LOGGER.info("PermaDeath toggle por {} — {}", event.getUser().getName(), cleaned);
        } else {
            sendRconError(event, "Não foi possível alterar o estado do PermaDeath.");
        }
    }

    private static void handleReload(ButtonInteractionEvent event, String messageId) {
        event.deferReply(true).queue();

        String result = executeRcon("rdeath reload");
        if (result != null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "Plugin Recarregado",
                "O plugin **RPermadeath** foi recarregado com sucesso!\n\n" +
                "📡 `" + PlayerControlCommand.formatRcon(result) + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();

            LOGGER.info("PermaDeath reload executado por {}", event.getUser().getName());
        } else {
            sendRconError(event, "Não foi possível recarregar o plugin.");
        }
    }

    private static void handleStats(ButtonInteractionEvent event, String messageId) {
        event.deferEdit().queue();

        List<DeathRecord> allDeaths = queryAllDeaths();

        int total = allDeaths.size();
        int valhalla = 0, ghost = 0, spectator = 0;
        for (DeathRecord r : allDeaths) {
            if ("VALHALLA".equalsIgnoreCase(r.deathState)) valhalla++;
            else if ("GHOST".equalsIgnoreCase(r.deathState)) ghost++;
            else if ("SPECTATOR".equalsIgnoreCase(r.deathState)) spectator++;
        }

        // Top killers
        java.util.Map<String, Integer> killerCount = new java.util.LinkedHashMap<>();
        for (DeathRecord r : allDeaths) {
            if (r.killerName != null && !r.killerName.equals("Desconhecido")) {
                killerCount.merge(r.killerName, 1, Integer::sum);
            }
        }
        List<java.util.Map.Entry<String, Integer>> topKillers = killerCount.entrySet().stream()
            .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .collect(java.util.stream.Collectors.toList());

        // Recent deaths
        List<DeathRecord> recent = allDeaths.subList(0, Math.min(5, allDeaths.size()));

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📊 Estatísticas do PermaDeath");
        embed.setColor(EmbedUtils.COLOR_GOLD);
        embed.setTimestamp(Instant.now());

        embed.addField("💀 Total de Mortos", "**" + total + "**", true);
        embed.addField("🏛️ Valhalla", "**" + valhalla + "**", true);
        embed.addField("👻 Ghost", "**" + ghost + "**", true);
        embed.addField("👁️ Spectator", "**" + spectator + "**", true);
        embed.addField("⚙️ Status", getPermaDeathStatus(), true);
        embed.addField("\u200B", "\u200B", true);

        // Top Killers
        if (!topKillers.isEmpty()) {
            StringBuilder topSb = new StringBuilder();
            int rank = 1;
            for (java.util.Map.Entry<String, Integer> entry : topKillers) {
                String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "  " + rank + ".";
                topSb.append(medal).append(" **").append(entry.getKey()).append("** — ").append(entry.getValue()).append(" kills\n");
                rank++;
            }
            embed.addField("🗡️ Top Assassinos", topSb.toString(), false);
        }

        // Recent deaths
        if (!recent.isEmpty()) {
            StringBuilder recentSb = new StringBuilder();
            for (DeathRecord r : recent) {
                recentSb.append(getStateEmoji(r.deathState)).append(" **").append(r.playerName)
                    .append("** — por *").append(r.killerName).append("*");
                if (r.deathTime != null) {
                    recentSb.append(" (").append(r.deathTime).append(")");
                }
                recentSb.append("\n");
            }
            embed.addField("🕐 Mortes Recentes", recentSb.toString(), false);
        }

        embed.setFooter("MidgardBOT • PermaDeath Stats");

        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(Button.primary("pd_back", "🔙 Voltar ao Painel")))
            .queue();
    }

    private static void handleBack(ButtonInteractionEvent event, String messageId) {
        event.deferEdit().queue();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("☠️ Painel PermaDeath — Controle Admin");
        embed.setColor(EmbedUtils.COLOR_ERROR);
        embed.setThumbnail("https://mc-heads.net/avatar/MHF_Skeleton/128");
        embed.setTimestamp(Instant.now());

        int totalDead = countDeadPlayers();
        String toggleStatus = getPermaDeathStatus();

        embed.setDescription(
            "Controle completo do sistema **PermaDeath** direto pelo Discord.\n" +
            "Use os botões abaixo para gerenciar mortes, ressuscitar jogadores e mais.\n\n" +
            EmbedUtils.SEPARATOR
        );

        embed.addField("💀 Jogadores Mortos", "**" + totalDead + "**", true);
        embed.addField("⚙️ PermaDeath", toggleStatus, true);
        embed.addField("\u200B", "\u200B", true);

        embed.addField("📋 Ações Disponíveis",
            "**📋 Listar Mortos** — Ver todos os jogadores mortos com detalhes\n" +
            "**🔍 Buscar Jogador** — Buscar informações de morte de um jogador\n" +
            "**♻️ Ressuscitar** — Trazer um jogador de volta à vida\n" +
            "**🗑️ Deletar Registro** — Remover registro de morte e resetar dados\n" +
            "**⚙️ Toggle** — Ativar/Desativar o sistema PermaDeath\n" +
            "**🔄 Reload** — Recarregar configurações do plugin\n" +
            "**📊 Estatísticas** — Informações detalhadas do sistema",
            false
        );

        embed.setFooter("MidgardBOT • PermaDeath Admin Panel", event.getJDA().getSelfUser().getAvatarUrl());

        List<ActionRow> actionRows = List.of(
            ActionRow.of(
                Button.primary("pd_list", "📋 Listar Mortos"),
                Button.primary("pd_search", "🔍 Buscar Jogador"),
                Button.success("pd_revive", "♻️ Ressuscitar"),
                Button.danger("pd_delete", "🗑️ Deletar Registro")
            ),
            ActionRow.of(
                Button.secondary("pd_toggle", "⚙️ Toggle PermaDeath"),
                Button.secondary("pd_reload", "🔄 Reload Plugin"),
                Button.primary("pd_stats", "📊 Estatísticas"),
                Button.danger("pd_close", "❌ Fechar")
            )
        );

        event.getHook().editOriginalEmbeds(embed.build()).setComponents(actionRows).queue();
    }

    private static void handleClose(ButtonInteractionEvent event, String messageId) {
        PermaDeathCommand.sessionOwners.remove(messageId);
        PermaDeathCommand.sessionPages.remove(messageId);

        event.editMessageEmbeds(
            EmbedUtils.createInfo("Painel Fechado", "O painel PermaDeath foi encerrado.", event.getJDA().getSelfUser()).build()
        ).setComponents().queue();
    }

    // ========================
    //    MODAL SUBMIT HANDLERS
    // ========================

    private static void handleSearchSubmit(ModalInteractionEvent event) {
        String playerName = event.getValue("pd_player_name").getAsString().trim();

        DeathRecord record = queryDeathByName(playerName);
        if (record == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createWarning(
                "Jogador Não Encontrado",
                "O jogador **" + playerName + "** não possui registro de morte no sistema.\n\n" +
                "*Ele pode estar vivo, ou o nome pode estar incorreto.*",
                event.getJDA().getSelfUser()
            ).build()).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("☠️ Registro de Morte — " + record.playerName);
        embed.setColor(EmbedUtils.COLOR_ERROR);
        embed.setThumbnail("https://mc-heads.net/avatar/" + record.playerName + "/128");
        embed.setTimestamp(Instant.now());

        embed.addField("👤 Jogador", "`" + record.playerName + "`", true);
        embed.addField("🆔 UUID", "`" + record.playerUuid + "`", true);
        embed.addField("\u200B", "\u200B", true);

        embed.addField("🗡️ Assassino", record.killerName != null ? "**" + record.killerName + "**" : "Desconhecido", true);
        if (record.killerUuid != null) {
            embed.addField("🆔 UUID Killer", "`" + record.killerUuid + "`", true);
        }
        embed.addField("\u200B", "\u200B", true);

        embed.addField("📅 Data da Morte", record.deathTime != null ? "`" + record.deathTime + "`" : "Desconhecida", true);
        embed.addField(getStateEmoji(record.deathState) + " Estado", "**" + formatState(record.deathState) + "**", true);
        embed.addField("\u200B", "\u200B", true);

        String locStr = String.format("`%s: %.0f, %.0f, %.0f`",
            record.world != null ? record.world : "?", record.x, record.y, record.z);
        embed.addField("📍 Local da Morte", locStr, false);

        embed.setFooter("MidgardBOT • PermaDeath Info", event.getJDA().getSelfUser().getAvatarUrl());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    private static void handleReviveSubmit(ModalInteractionEvent event) {
        String playerName = event.getValue("pd_player_name").getAsString().trim();

        // Verifica se o jogador está morto antes
        DeathRecord record = queryDeathByName(playerName);
        if (record == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createWarning(
                "Jogador Não Encontrado",
                "O jogador **" + playerName + "** não possui registro de morte.\n" +
                "Ele pode já estar vivo ou o nome está incorreto.",
                event.getJDA().getSelfUser()
            ).build()).queue();
            return;
        }

        // Executa via RCON
        String result = executeRcon("rdeath ressucitar " + record.playerName);
        if (result != null) {
            String cleaned = PlayerControlCommand.formatRcon(result);

            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "Jogador Ressuscitado!",
                "**" + record.playerName + "** foi trazido de volta à vida!\n\n" +
                "🗡️ **Assassino:** " + record.killerName + "\n" +
                "📅 **Morte em:** " + (record.deathTime != null ? record.deathTime : "N/A") + "\n" +
                "👻 **Estado anterior:** " + formatState(record.deathState) + "\n\n" +
                "📡 `" + cleaned + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();

            LOGGER.info("Jogador {} ressuscitado via painel por {}", record.playerName, event.getUser().getName());
        } else {
            sendRconError(event, "Não foi possível ressuscitar **" + playerName + "**.\nVerifique se o servidor está online e o RCON configurado.");
        }
    }

    private static void handleDeleteSubmit(ModalInteractionEvent event) {
        String playerName = event.getValue("pd_player_name").getAsString().trim();
        String confirm = event.getValue("pd_confirm").getAsString().trim();

        if (!confirm.equals("CONFIRMAR")) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(
                "Confirmação Inválida",
                "Você precisa digitar **CONFIRMAR** para deletar o registro.\n" +
                "Esta ação é irreversível!",
                event.getJDA().getSelfUser()
            ).build()).queue();
            return;
        }

        // Verifica se existe
        DeathRecord record = queryDeathByName(playerName);
        if (record == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createWarning(
                "Registro Não Encontrado",
                "Não há registro de morte para **" + playerName + "**.",
                event.getJDA().getSelfUser()
            ).build()).queue();
            return;
        }

        // Executa reset via RCON (isso faz revive + wipe de dados)
        String result = executeRcon("rdeath reset " + record.playerName);
        if (result != null) {
            String cleaned = PlayerControlCommand.formatRcon(result);

            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "Registro Deletado",
                "O registro de morte de **" + record.playerName + "** foi removido e seus dados resetados!\n\n" +
                "⚠️ **Atenção:** Os dados do jogador (inventário, XP, etc.) foram apagados.\n\n" +
                "📡 `" + cleaned + "`",
                event.getJDA().getSelfUser()
            ).build()).queue();

            LOGGER.info("Registro de morte de {} deletado/resetado via painel por {}", record.playerName, event.getUser().getName());
        } else {
            sendRconError(event, "Não foi possível deletar o registro de **" + playerName + "**.");
        }
    }

    // ========================
    //    DATABASE QUERIES
    // ========================

    /**
     * Conta o total de jogadores mortos na tabela midgard_deaths.
     */
    public static int countDeadPlayers() {
        if (!DatabaseManager.isConnected()) return 0;
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM midgard_deaths");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao contar mortos na DB", e);
        }
        return 0;
    }

    /**
     * Busca todos os registros de morte ordenados por data (mais recente primeiro).
     */
    private static List<DeathRecord> queryAllDeaths() {
        List<DeathRecord> deaths = new ArrayList<>();
        if (!DatabaseManager.isConnected()) return deaths;

        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return deaths;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT player_uuid, player_name, death_time, killer_uuid, killer_name, " +
                    "world, x, y, z, yaw, pitch, death_state FROM midgard_deaths ORDER BY death_time DESC");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    deaths.add(mapDeathRecord(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao consultar mortes na DB", e);
        }
        return deaths;
    }

    /**
     * Busca um registro de morte por nome de jogador (case-insensitive).
     */
    private static DeathRecord queryDeathByName(String playerName) {
        if (!DatabaseManager.isConnected()) return null;

        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT player_uuid, player_name, death_time, killer_uuid, killer_name, " +
                    "world, x, y, z, yaw, pitch, death_state FROM midgard_deaths WHERE LOWER(player_name) = LOWER(?)")) {
                ps.setString(1, playerName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapDeathRecord(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao buscar morte por nome na DB", e);
        }
        return null;
    }

    private static DeathRecord mapDeathRecord(ResultSet rs) throws SQLException {
        DeathRecord r = new DeathRecord();
        r.playerUuid = rs.getString("player_uuid");
        r.playerName = rs.getString("player_name");
        java.sql.Timestamp ts = rs.getTimestamp("death_time");
        r.deathTime = ts != null ? ts.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : null;
        r.killerUuid = rs.getString("killer_uuid");
        r.killerName = rs.getString("killer_name");
        r.world = rs.getString("world");
        r.x = rs.getDouble("x");
        r.y = rs.getDouble("y");
        r.z = rs.getDouble("z");
        r.deathState = rs.getString("death_state");
        return r;
    }

    // ========================
    //    RCON HELPERS
    // ========================

    /**
     * Retorna o status do sistema PermaDeath baseado na presença de registros no DB.
     * O estado exato (ativado/desativado) é controlado pela config do plugin no servidor.
     */
    public static String getPermaDeathStatus() {
        int count = countDeadPlayers();
        if (count >= 0) {
            return "📊 **" + count + " mortos** registrados";
        }
        return "⚠️ **DB Offline**";
    }

    private static String executeRcon(String command) {
        String host = BotConfig.getRconHost();
        String pass = BotConfig.getRconPassword();
        if (host == null || host.isEmpty() || pass == null || pass.isEmpty()) return null;

        try (RconClient rcon = new RconClient(host, BotConfig.getRconPort(), pass)) {
            rcon.connect();
            return rcon.sendCommand(command);
        } catch (IOException e) {
            LOGGER.error("Erro RCON ao executar '{}': {}", command, e.getMessage());
            return null;
        }
    }

    // ========================
    //    UTILITY
    // ========================

    private static String getStateEmoji(String state) {
        if (state == null) return "💀";
        switch (state.toUpperCase()) {
            case "VALHALLA": return "🏛️";
            case "GHOST": return "👻";
            case "SPECTATOR": return "👁️";
            default: return "💀";
        }
    }

    private static String formatState(String state) {
        if (state == null) return "Desconhecido";
        switch (state.toUpperCase()) {
            case "VALHALLA": return "Valhalla";
            case "GHOST": return "Fantasma";
            case "SPECTATOR": return "Espectador";
            case "ALIVE": return "Vivo";
            default: return state;
        }
    }

    private static void sendRconError(ButtonInteractionEvent event, String detail) {
        event.getHook().sendMessageEmbeds(EmbedUtils.createError(
            "Erro de Conexão RCON",
            detail + "\n\nVerifique se o servidor Minecraft está online e o RCON está configurado no `config.env`.",
            event.getJDA().getSelfUser()
        ).build()).queue();
    }

    private static void sendRconError(ModalInteractionEvent event, String detail) {
        event.getHook().sendMessageEmbeds(EmbedUtils.createError(
            "Erro de Conexão RCON",
            detail + "\n\nVerifique se o servidor Minecraft está online e o RCON está configurado no `config.env`.",
            event.getJDA().getSelfUser()
        ).build()).queue();
    }

    // ========================
    //    DATA CLASS
    // ========================

    static class DeathRecord {
        String playerUuid;
        String playerName;
        String deathTime;
        String killerUuid;
        String killerName;
        String world;
        double x, y, z;
        String deathState;
    }
}

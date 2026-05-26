package com.midgardbot.features;

import com.midgardbot.data.DatabaseManager;
import com.midgardbot.data.LogService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Quando um membro entra no servidor, verifica se ele possui advertências ativas (source='panel').
 * Se possuir, registra no sistema de logs do painel para alertar a staff.
 */
public class PunishmentRejoinListener extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PunishmentRejoinListener.class);

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Member member = event.getMember();
        String memberId = member.getId();

        Thread.ofVirtual().start(() -> {
            try {
                List<PunishmentInfo> activePunishments = getActivePanelPunishments(memberId);
                if (activePunishments.isEmpty()) return;

                LOGGER.info("[PUNISHMENT-REJOIN] Membro {} ({}) entrou com {} advertência(s) ativa(s)",
                        member.getEffectiveName(), memberId, activePunishments.size());

                StringBuilder msg = new StringBuilder();
                msg.append("Voltou ao servidor com ").append(activePunishments.size()).append(" advertência(s) ativa(s):\n");

                for (PunishmentInfo p : activePunishments) {
                    String severity = parseSeverity(p.reason);
                    String cleanReason = parseCleanReason(p.reason);
                    String date = java.time.LocalDate.ofInstant(
                            Instant.ofEpochMilli(p.startTime), java.time.ZoneId.systemDefault()
                    ).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                    msg.append("• Advertência ").append(severity.isEmpty() ? "" : severity)
                            .append(" (").append(date).append(") — ")
                            .append(cleanReason.isEmpty() ? "Sem motivo" : cleanReason)
                            .append(" (por ").append(p.moderatorName).append(")\n");
                }

                LogService.log(
                        "punishment",
                        "Jogador com punições retornou",
                        msg.toString(),
                        "⚠️",
                        memberId,
                        member.getEffectiveName(),
                        member.getEffectiveAvatarUrl()
                );
            } catch (Exception e) {
                LOGGER.error("[PUNISHMENT-REJOIN] Erro ao verificar punições do membro {}", memberId, e);
            }
        });
    }

    private List<PunishmentInfo> getActivePanelPunishments(String discordId) {
        List<PunishmentInfo> punishments = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return punishments;

            String sql = "SELECT id, type, reason, moderator_name, start_time FROM midgard_punishments " +
                    "WHERE target_discord_id = ? AND active = 1 AND source = 'panel' ORDER BY start_time DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, discordId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PunishmentInfo p = new PunishmentInfo();
                        p.id = rs.getInt("id");
                        p.type = rs.getString("type");
                        p.reason = rs.getString("reason");
                        p.moderatorName = rs.getString("moderator_name");
                        p.startTime = rs.getLong("start_time");
                        punishments.add(p);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("[PUNISHMENT-REJOIN] Erro ao buscar punições ativas", e);
        }
        return punishments;
    }

    private String parseSeverity(String reason) {
        if (reason == null) return "";
        var m = java.util.regex.Pattern.compile("^\\[(leve|média|pesada)]\\s*", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(reason);
        if (m.find()) return m.group(1);
        return "";
    }

    private String parseCleanReason(String reason) {
        if (reason == null) return "";
        return reason.replaceFirst("^\\[(leve|média|pesada)]\\s*", "");
    }

    private static class PunishmentInfo {
        int id;
        String type;
        String reason;
        String moderatorName;
        long startTime;
    }
}

package com.midgardbot.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Serviço para inserir logs no painel administrativo.
 * Logs são persistidos por dia e nunca apagados automaticamente.
 */
public class LogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogService.class);

    /**
     * Insere um log no banco de dados.
     *
     * @param category  Categoria do log (ex: "punishment", "ticket", "whitelist", "moderation")
     * @param title     Título curto do log
     * @param message   Mensagem detalhada
     * @param icon      Emoji/ícone do log
     * @param targetId  ID do Discord do alvo (opcional)
     * @param targetName Nome do alvo (opcional)
     * @param targetAvatar URL do avatar do alvo (opcional)
     */
    public static void log(String category, String title, String message, String icon,
                           String targetId, String targetName, String targetAvatar) {
        if (!DatabaseManager.isConnected()) return;

        String sql = "INSERT INTO midgard_logs (category, title, message, icon, target_id, target_name, target_avatar) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, category);
                ps.setString(2, title);
                ps.setString(3, message);
                ps.setString(4, icon);
                ps.setString(5, targetId);
                ps.setString(6, targetName);
                ps.setString(7, targetAvatar);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            LOGGER.error("[LOG] Erro ao inserir log: {}", title, e);
        }
    }

    /** Atalho sem avatar */
    public static void log(String category, String title, String message, String icon) {
        log(category, title, message, icon, null, null, null);
    }
}

package com.midgardbot.web;

import com.midgardbot.config.BotConfig;
import com.midgardbot.web.auth.AuthController;
import com.midgardbot.web.auth.JwtUtils;
import com.midgardbot.web.security.SecurityMiddleware;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import com.midgardbot.web.routes.DashboardRoutes;
import com.midgardbot.web.routes.ModerationRoutes;
import com.midgardbot.web.routes.WhitelistRoutes;
import com.midgardbot.web.routes.TicketRoutes;
import com.midgardbot.web.routes.StaffRoutes;
import com.midgardbot.web.routes.PlayerRoutes;
import com.midgardbot.web.routes.PresenceRoutes;
import com.midgardbot.web.routes.ReportRoutes;
import com.midgardbot.web.routes.LogRoutes;
import com.midgardbot.web.routes.MeetingRoutes;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import net.dv8tion.jda.api.JDA;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servidor web embutido no bot para o painel administrativo.
 * Usa Javalin (Jetty embarcado) servindo a SPA React + API REST.
 */
public class WebServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);
    private Javalin app;
    private final JDA jda;

    public WebServer(JDA jda) {
        this.jda = jda;
    }

    public void start() {
        String enabled = BotConfig.get("WEB_ENABLED");
        if (!"true".equalsIgnoreCase(enabled)) {
            LOGGER.info("[WEB] Painel web desativado (WEB_ENABLED != true)");
            return;
        }

        int port = 7070;
        try {
            String portStr = BotConfig.get("WEB_PORT");
            if (portStr != null && !portStr.isEmpty()) {
                port = Integer.parseInt(portStr);
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("[WEB] WEB_PORT inválido, usando porta padrão 7070");
        }

        String secret = BotConfig.get("WEB_SECRET");
        if (secret == null || secret.isBlank()
                || "midgard-web-default-secret-change-me".equals(secret)
                || "troque-para-uma-chave-segura-aleatoria".equals(secret)) {
            LOGGER.error("[WEB] WEB_SECRET não configurado ou usando valor padrão! Configure uma chave segura no config.env.");
            LOGGER.error("[WEB] O painel NÃO será iniciado por segurança.");
            return;
        }
        if (secret.length() < 32) {
            LOGGER.warn("[WEB] WEB_SECRET curto demais ({}). Recomendado: pelo menos 32 caracteres.", secret.length());
        }
        JwtUtils.init(secret);

        app = Javalin.create(config -> {
            // Configurar thread pool do Jetty para suportar múltiplos usuários simultâneos
            config.jetty.threadPool = new QueuedThreadPool(200, 10, 60000);

            // Servir arquivos estáticos do React (build)
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.hostedPath = "/";
                staticFileConfig.directory = "/webapp";
                staticFileConfig.location = Location.CLASSPATH;
            });

            // SPA fallback — qualquer rota que não seja /api retorna index.html
            config.spaRoot.addFile("/", "/webapp/index.html", Location.CLASSPATH);

            // Limitar tamanho do body (prevenir payloads gigantes)
            config.http.maxRequestSize = 1_048_576L; // 1 MB max
        });

        // Middleware de segurança (rate limiting, headers, etc.)
        SecurityMiddleware.register(app);

        // Middleware de autenticação para rotas /api (exceto /api/auth)
        app.before("/api/*", ctx -> {
            String path = ctx.path();
            if (path.equals("/api/auth/login") || path.equals("/api/auth/callback")) return;
            if (path.startsWith("/api/reports/uploads/")) return;

            // Streaming de áudio: aceitar token via query param (pois <audio> não envia headers)
            if (path.startsWith("/api/meetings/recordings/")) {
                String queryToken = ctx.queryParam("token");
                if (queryToken != null && !queryToken.isEmpty()) {
                    var decoded = JwtUtils.verify(queryToken);
                    if (decoded != null && AuthController.isStaff(jda, decoded.getSubject())) {
                        ctx.attribute("userId", decoded.getSubject());
                        return;
                    }
                }
            }

            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(401).json(new ErrorResponse("Token não fornecido"));
                ctx.skipRemainingHandlers();
                return;
            }

            String token = authHeader.substring(7);
            var decoded = JwtUtils.verify(token);
            if (decoded == null) {
                ctx.status(401).json(new ErrorResponse("Token inválido ou expirado"));
                ctx.skipRemainingHandlers();
                return;
            }

            String userId = decoded.getSubject();

            // Verificar se o usuário ainda tem cargo de staff
            if (!AuthController.isStaff(jda, userId)) {
                ctx.status(403).json(new ErrorResponse("Acesso revogado — você não possui mais cargo de staff"));
                ctx.skipRemainingHandlers();
                return;
            }

            ctx.attribute("userId", userId);
            ctx.attribute("username", decoded.getClaim("username").asString());
        });

        // Pré-carregar membros no cache do JDA para buscas
        try {
            var guild = AuthController.getMainGuild(jda);
            if (guild != null) {
                guild.loadMembers().get();
                LOGGER.info("[WEB] {} membros carregados no cache ({})", guild.getMemberCount(), guild.getName());
            }
        } catch (Exception e) {
            LOGGER.warn("[WEB] Falha ao pré-carregar membros: {}", e.getMessage());
        }

        // Registrar rotas
        AuthController.register(app, jda);
        DashboardRoutes.register(app, jda);
        WhitelistRoutes.register(app, jda);
        ModerationRoutes.register(app, jda);
        TicketRoutes.register(app, jda);
        StaffRoutes.register(app, jda);
        PlayerRoutes.register(app, jda);
        PresenceRoutes.register(app, jda);
        ReportRoutes.register(app, jda);
        MeetingRoutes.register(app, jda);
        LogRoutes.register(app, jda);

        app.start(port);
        LOGGER.info("[WEB] Painel administrativo iniciado na porta {}", port);
    }

    public void stop() {
        if (app != null) {
            app.stop();
            LOGGER.info("[WEB] Painel administrativo encerrado.");
        }
    }

    /** Resposta JSON padronizada para erros. */
    public record ErrorResponse(String error) {}
}

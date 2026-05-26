package com.midgardbot.web.security;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.midgardbot.config.BotConfig;

import io.javalin.Javalin;

/**
 * Middleware de segurança centralizado — proteção contra DoS, XSS, Clickjacking, etc.
 */
public class SecurityMiddleware {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityMiddleware.class);

    // Rate limiting: IP → (windowStart, requestCount)
    private static final ConcurrentHashMap<String, RateWindow> rateLimits = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, RateWindow> authRateLimits = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, RateWindow> searchRateLimits = new ConcurrentHashMap<>();

    private static final int GENERAL_LIMIT = 120;          // 120 req/min por IP (rotas gerais)
    private static final int AUTH_LIMIT = 10;               // 10 req/min por IP (login/callback)
    private static final int SEARCH_LIMIT = 30;             // 30 req/min (buscas)
    private static final long WINDOW_MS = 60_000;           // 1 minuto

    public static void register(Javalin app) {

        // ── Security Headers (antes de TODA resposta) ──
        app.after(ctx -> {
            // XSS Protection
            ctx.header("X-Content-Type-Options", "nosniff");
            ctx.header("X-XSS-Protection", "1; mode=block");

            // Clickjacking Protection
            ctx.header("X-Frame-Options", "DENY");

            // MIME sniffing
            ctx.header("Referrer-Policy", "strict-origin-when-cross-origin");

            // Permissions Policy (desabilitar APIs desnecessárias)
            ctx.header("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");

            // Content Security Policy (permite apenas recursos do próprio domínio e Discord CDN)
            if (!ctx.path().startsWith("/api/")) {
                ctx.header("Content-Security-Policy",
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-eval' 'sha256-nsegoMZGTp5YIuseFELHBDBdh0/XjGGNtk9DNP0qb0o='; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' https://cdn.discordapp.com https://mc-heads.net data: blob:; " +
                        "media-src 'self' blob:; " +
                        "connect-src 'self'; " +
                        "font-src 'self'; " +
                        "frame-ancestors 'none'; " +
                        "base-uri 'self'; " +
                        "form-action 'self'"
                );
            }

            // Cache control para API (não cachear dados sensíveis, exceto uploads de arquivos)
            if (ctx.path().startsWith("/api/") && !ctx.path().startsWith("/api/reports/uploads/")) {
                ctx.header("Cache-Control", "no-store, no-cache, must-revalidate, private");
                ctx.header("Pragma", "no-cache");
            }
        });

        // ── CORS manual (apenas para /api/*, não afeta static files) ──
        Set<String> allowedOrigins = buildAllowedOrigins();
        LOGGER.info("[SECURITY] CORS origins permitidas: {}", allowedOrigins);

        app.before("/api/*", ctx -> {
            String origin = ctx.header("Origin");
            if (origin != null && allowedOrigins.contains(origin)) {
                ctx.header("Access-Control-Allow-Origin", origin);
                ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
                ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
                ctx.header("Access-Control-Allow-Credentials", "true");
                ctx.header("Access-Control-Max-Age", "3600");
            }
            if ("OPTIONS".equalsIgnoreCase(ctx.method().name())) {
                ctx.status(204);
                ctx.skipRemainingHandlers();
            }
        });

        // ── Rate Limiting Global ──
        app.before("/api/*", ctx -> {
            String ip = getClientIp(ctx);
            String path = ctx.path();

            // Rate limit mais restritivo para auth
            if (path.startsWith("/api/auth")) {
                if (isRateLimited(ip, authRateLimits, AUTH_LIMIT)) {
                    LOGGER.warn("[SECURITY] Rate limit auth excedido: {}", ip);
                    ctx.status(429).json(Map.of("error", "Muitas tentativas. Aguarde 1 minuto."));
                    ctx.skipRemainingHandlers();
                    return;
                }
            }

            // Rate limit para buscas (mais pesadas)
            if (path.contains("/search") || path.contains("/players")) {
                if (isRateLimited(ip, searchRateLimits, SEARCH_LIMIT)) {
                    LOGGER.warn("[SECURITY] Rate limit de busca excedido: {}", ip);
                    ctx.status(429).json(Map.of("error", "Muitas buscas. Aguarde 1 minuto."));
                    ctx.skipRemainingHandlers();
                    return;
                }
            }

            // Rate limit geral
            if (isRateLimited(ip, rateLimits, GENERAL_LIMIT)) {
                LOGGER.warn("[SECURITY] Rate limit geral excedido: {}", ip);
                ctx.status(429).json(Map.of("error", "Muitas requisições. Aguarde 1 minuto."));
                ctx.skipRemainingHandlers();
            }
        });
    }

    /**
     * Valida que um ID do Discord contém apenas dígitos (previne injection).
     */
    public static boolean isValidDiscordId(String id) {
        return id != null && !id.isEmpty() && id.length() <= 20 && id.chars().allMatch(Character::isDigit);
    }

    /**
     * Sanitiza uma string de query removendo caracteres perigosos.
     */
    public static String sanitizeQuery(String input) {
        if (input == null) return null;
        // Limita tamanho e remove caracteres que poderiam ser usados em injection
        String sanitized = input.length() > 100 ? input.substring(0, 100) : input;
        return sanitized.replaceAll("[<>\"';&|`$\\\\]", "");
    }

    /**
     * Limita um valor de paginação a um range seguro.
     */
    public static int clampLimit(int limit, int max) {
        return Math.max(1, Math.min(limit, max));
    }

    public static int clampPage(int page) {
        return Math.max(1, page);
    }

    // ── Internals ──

    private static Set<String> buildAllowedOrigins() {
        Set<String> origins = new java.util.HashSet<>();
        // Localhost só em modo dev (WEB_DEV_MODE=true)
        String devMode = BotConfig.get("WEB_DEV_MODE");
        if ("true".equalsIgnoreCase(devMode)) {
            origins.add("http://localhost:5173");
            origins.add("http://localhost:7070");
        }

        String allowedOrigin = BotConfig.get("WEB_ALLOWED_ORIGIN");
        if (allowedOrigin != null && !allowedOrigin.isBlank()) {
            String host = allowedOrigin.replace("https://", "").replace("http://", "");
            origins.add("http://" + host);
            origins.add("https://" + host);
        } else {
            String redirectUri = BotConfig.get("WEB_REDIRECT_URI");
            if (redirectUri != null && !redirectUri.isBlank()) {
                try {
                    var uri = java.net.URI.create(redirectUri);
                    String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
                    String host = uri.getPort() > 0
                            ? uri.getHost() + ":" + uri.getPort()
                            : uri.getHost();
                    origins.add(scheme + "://" + host);
                    // Adicionar ambos os schemes
                    origins.add("http://" + host);
                    origins.add("https://" + host);
                } catch (Exception e) {
                    LOGGER.warn("[SECURITY] Falha ao derivar origin do WEB_REDIRECT_URI");
                }
            }
        }
        return origins;
    }

    private static String getClientIp(io.javalin.http.Context ctx) {
        // Só confia em X-Forwarded-For se WEB_BEHIND_PROXY estiver habilitado
        String behindProxy = com.midgardbot.config.BotConfig.get("WEB_BEHIND_PROXY");
        if ("true".equalsIgnoreCase(behindProxy)) {
            String forwarded = ctx.header("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return ctx.ip();
    }

    private static boolean isRateLimited(String ip, ConcurrentHashMap<String, RateWindow> store, int maxRequests) {
        long now = System.currentTimeMillis();
        RateWindow window = store.compute(ip, (key, existing) -> {
            if (existing == null || (now - existing.windowStart) > WINDOW_MS) {
                return new RateWindow(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return window.count.get() > maxRequests;
    }

    /** Limpa entradas expiradas do rate limiter (chamar periodicamente). */
    public static void cleanup() {
        long now = System.currentTimeMillis();
        rateLimits.entrySet().removeIf(e -> (now - e.getValue().windowStart) > WINDOW_MS * 2);
        authRateLimits.entrySet().removeIf(e -> (now - e.getValue().windowStart) > WINDOW_MS * 2);
        searchRateLimits.entrySet().removeIf(e -> (now - e.getValue().windowStart) > WINDOW_MS * 2);
    }

    private static class RateWindow {
        final long windowStart;
        final AtomicInteger count;

        RateWindow(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}

package com.midgardbot.web.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.midgardbot.config.BotConfig;

import io.javalin.Javalin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Controller de autenticação via Discord OAuth2.
 * Fluxo: Frontend redireciona → Discord → Callback → JWT.
 * Apenas membros com cargos em WEB_ALLOWED_ROLES podem acessar.
 */
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
    private static final Gson GSON = new Gson();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final String DISCORD_API = "https://discord.com/api/v10";

    // Armazena states OAuth2 válidos com timestamp de criação (expira em 5 min)
    private static final java.util.concurrent.ConcurrentHashMap<String, Long> pendingOAuthStates = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long STATE_EXPIRY_MS = 300_000; // 5 minutos

    /**
     * IDs de cargos autorizados a acessar o painel.
     * Carregados do config: WEB_ALLOWED_ROLES
     */
    public static Set<String> getAllowedRoles() {
        String roles = BotConfig.get("WEB_ALLOWED_ROLES");
        if (roles == null || roles.isBlank()) return Set.of();
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Verifica se um membro do Discord tem um dos cargos permitidos.
     */
    public static boolean isStaff(JDA jda, String userId) {
        Set<String> allowedRoles = getAllowedRoles();
        if (allowedRoles.isEmpty()) return false; // Sem roles configuradas = negar acesso por segurança

        try {
            Guild guild = getMainGuild(jda);
            if (guild == null) return false;
            Member member = guild.getMemberById(userId);
            if (member == null) return false;
            return member.getRoles().stream()
                    .anyMatch(r -> allowedRoles.contains(r.getId()));
        } catch (Exception e) {
            LOGGER.warn("[AUTH] Erro ao verificar cargos de {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Retorna o servidor principal pelo MAIN_GUILD_ID.
     */
    public static Guild getMainGuild(JDA jda) {
        String mainGuildId = BotConfig.get("MAIN_GUILD_ID");
        if (mainGuildId != null && !mainGuildId.isEmpty()) {
            Guild guild = jda.getGuildById(mainGuildId);
            if (guild != null) return guild;
        }
        // Fallback: retorna o primeiro que não seja o de staffs
        String staffGuildId = BotConfig.get("STAFF_GUILD_ID");
        for (Guild guild : jda.getGuilds()) {
            if (staffGuildId != null && !staffGuildId.isEmpty() && guild.getId().equals(staffGuildId)) {
                continue;
            }
            return guild;
        }
        return jda.getGuilds().isEmpty() ? null : jda.getGuilds().get(0);
    }

    public static void register(Javalin app, JDA jda) {
        // Retorna a URL de login do Discord OAuth2
        app.get("/api/auth/login", ctx -> {
            String clientId = BotConfig.get("DISCORD_CLIENT_ID");
            String redirectUri = BotConfig.get("WEB_REDIRECT_URI");
            if (clientId == null || redirectUri == null) {
                ctx.status(500).json(new ErrorMsg("OAuth2 não configurado"));
                return;
            }

            String state = java.util.UUID.randomUUID().toString();
            pendingOAuthStates.put(state, System.currentTimeMillis());
            // Limpa states expirados
            long now = System.currentTimeMillis();
            pendingOAuthStates.entrySet().removeIf(e -> (now - e.getValue()) > STATE_EXPIRY_MS);

            String url = DISCORD_API + "/oauth2/authorize"
                    + "?client_id=" + clientId
                    + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, "UTF-8")
                    + "&response_type=code"
                    + "&scope=identify+guilds.members.read"
                    + "&state=" + state;

            ctx.json(new LoginUrl(url));
        });

        // Callback do Discord OAuth2 — troca code por token
        app.post("/api/auth/callback", ctx -> {
            var body = GSON.fromJson(ctx.body(), CallbackBody.class);
            if (body == null || body.code == null) {
                ctx.status(400).json(new ErrorMsg("Código não fornecido"));
                return;
            }

            // Validar state para prevenir CSRF no fluxo OAuth
            if (body.state == null || body.state.isBlank()) {
                ctx.status(400).json(new ErrorMsg("Parâmetro state ausente"));
                return;
            }
            Long stateTimestamp = pendingOAuthStates.remove(body.state);
            if (stateTimestamp == null || (System.currentTimeMillis() - stateTimestamp) > STATE_EXPIRY_MS) {
                ctx.status(400).json(new ErrorMsg("State inválido ou expirado"));
                return;
            }

            String clientId = BotConfig.get("DISCORD_CLIENT_ID");
            String clientSecret = BotConfig.get("DISCORD_CLIENT_SECRET");
            String redirectUri = BotConfig.get("WEB_REDIRECT_URI");

            // Trocar o code por access_token
            RequestBody formBody = new FormBody.Builder()
                    .add("client_id", clientId)
                    .add("client_secret", clientSecret)
                    .add("grant_type", "authorization_code")
                    .add("code", body.code)
                    .add("redirect_uri", redirectUri)
                    .build();

            Request tokenReq = new Request.Builder()
                    .url(DISCORD_API + "/oauth2/token")
                    .post(formBody)
                    .build();

            try (Response resp = HTTP_CLIENT.newCall(tokenReq).execute()) {
                if (!resp.isSuccessful()) {
                    LOGGER.warn("[AUTH] Falha ao trocar code: {}", resp.code());
                    ctx.status(401).json(new ErrorMsg("Falha na autenticação com Discord"));
                    return;
                }

                JsonObject tokenJson = GSON.fromJson(resp.body().string(), JsonObject.class);
                if (tokenJson == null || !tokenJson.has("access_token") || tokenJson.get("access_token").isJsonNull()) {
                    LOGGER.warn("[AUTH] Resposta de token inválida do Discord");
                    ctx.status(401).json(new ErrorMsg("Resposta inválida do Discord"));
                    return;
                }
                String accessToken = tokenJson.get("access_token").getAsString();

                // Buscar dados do usuário
                Request userReq = new Request.Builder()
                        .url(DISCORD_API + "/users/@me")
                        .header("Authorization", "Bearer " + accessToken)
                        .build();

                try (Response userResp = HTTP_CLIENT.newCall(userReq).execute()) {
                    if (!userResp.isSuccessful()) {
                        ctx.status(401).json(new ErrorMsg("Falha ao obter dados do usuário"));
                        return;
                    }

                    JsonObject user = GSON.fromJson(userResp.body().string(), JsonObject.class);
                    if (user == null || !user.has("id") || user.get("id").isJsonNull()) {
                        ctx.status(401).json(new ErrorMsg("Dados do usuário inválidos"));
                        return;
                    }
                    String userId = user.get("id").getAsString();
                    String username = user.has("username") && !user.get("username").isJsonNull()
                            ? user.get("username").getAsString() : "Desconhecido";
                    String avatar = user.has("avatar") && !user.get("avatar").isJsonNull()
                            ? user.get("avatar").getAsString() : null;

                    // Verificar se o usuário é staff (tem cargo permitido)
                    if (!isStaff(jda, userId)) {
                        LOGGER.warn("[AUTH] Acesso negado para {} ({}) — sem cargo de staff", username, userId);
                        ctx.status(403).json(new ErrorMsg("Acesso restrito à equipe do servidor"));
                        return;
                    }

                    LOGGER.info("[AUTH] Login autorizado: {} ({})", username, userId);
                    String jwt = JwtUtils.generateToken(userId, username, avatar);

                    // Resolver role keys do usuário
                    java.util.List<String> roleKeys = new java.util.ArrayList<>();
                    try {
                        Guild guild = getMainGuild(jda);
                        if (guild != null) {
                            Member member = guild.getMemberById(userId);
                            if (member != null) {
                                String[] ROLE_KEYS = {"FUNDADOR", "CEOO", "ADMIN", "DEV", "DEV_JR", "MODERADOR", "LOREMAKER", "AJUDANTE", "BUILDER", "CINEGRAFISTA", "INTERPRETE", "STAFF"};
                                for (String key : ROLE_KEYS) {
                                    String val = BotConfig.get(key);
                                    if (val != null && !val.isEmpty()) {
                                        for (String rid : val.split(",")) {
                                            if (member.getRoles().stream().anyMatch(r -> r.getId().equals(rid.trim()))) {
                                                roleKeys.add(key);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    String avatarUrl = avatar != null
                            ? "https://cdn.discordapp.com/avatars/" + userId + "/" + avatar + ".png"
                            : "https://cdn.discordapp.com/embed/avatars/0.png";

                    ctx.json(Map.of(
                            "token", jwt,
                            "userId", userId,
                            "username", username,
                            "avatarUrl", avatarUrl,
                            "roleKeys", roleKeys
                    ));
                }
            } catch (IOException e) {
                LOGGER.error("[AUTH] Erro na autenticação", e);
                ctx.status(500).json(new ErrorMsg("Erro interno de autenticação"));
            }
        });

        // Verificar sessão atual
        app.get("/api/auth/me", ctx -> {
            String userId = ctx.attribute("userId");
            String username = ctx.attribute("username");
            if (userId == null) {
                ctx.status(401).json(new ErrorMsg("Não autenticado"));
                return;
            }

            // Resolver role keys e avatar do usuário
            java.util.List<String> roleKeys = new java.util.ArrayList<>();
            String avatarUrl = null;
            try {
                Guild guild = getMainGuild(jda);
                if (guild != null) {
                    Member member = guild.getMemberById(userId);
                    if (member != null) {
                        avatarUrl = member.getUser().getAvatarUrl();
                        String[] ROLE_KEYS = {"FUNDADOR", "CEOO", "ADMIN", "DEV", "DEV_JR", "MODERADOR", "LOREMAKER", "AJUDANTE", "BUILDER", "CINEGRAFISTA", "INTERPRETE", "STAFF"};
                        for (String key : ROLE_KEYS) {
                            String val = BotConfig.get(key);
                            if (val != null && !val.isEmpty()) {
                                for (String rid : val.split(",")) {
                                    if (member.getRoles().stream().anyMatch(r -> r.getId().equals(rid.trim()))) {
                                        roleKeys.add(key);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[AUTH] Erro ao resolver roles de {}: {}", userId, e.getMessage());
            }

            Map<String, Object> resp = new java.util.LinkedHashMap<>();
            resp.put("userId", userId);
            resp.put("username", username);
            resp.put("avatarUrl", avatarUrl);
            resp.put("roleKeys", roleKeys);
            ctx.json(resp);
        });
    }

    // DTOs
    private record LoginUrl(String url) {}
    private record CallbackBody(String code, String state) {}
    private record AuthResponse(String token, String userId, String username, String avatarUrl) {}
    private record UserInfo(String userId, String username) {}
    private record ErrorMsg(String error) {}
}

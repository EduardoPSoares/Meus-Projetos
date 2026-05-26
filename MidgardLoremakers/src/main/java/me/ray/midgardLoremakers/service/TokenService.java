package me.ray.midgardLoremakers.service;

import me.ray.midgardLoremakers.config.PluginConfiguration;
import me.ray.midgardLoremakers.data.DatabaseManager;
import me.ray.midgardLoremakers.model.AuthenticatedSession;
import me.ray.midgardLoremakers.model.IssuedAccessToken;
import me.ray.midgardLoremakers.model.IssuedWebPanelSession;
import me.ray.midgardLoremakers.model.ReusableAccessToken;
import me.ray.midgardLoremakers.model.WebPanelSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TokenService {

    private final DatabaseManager databaseManager;
    private final PluginConfiguration.SecurityConfiguration securityConfiguration;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<UUID, ReusableAccessToken> reusableTokens = new ConcurrentHashMap<>();

    public TokenService(DatabaseManager databaseManager, PluginConfiguration.SecurityConfiguration securityConfiguration) {
        this.databaseManager = databaseManager;
        this.securityConfiguration = securityConfiguration;
    }

    public IssuedAccessToken issueToken(UUID playerUuid, String playerName) {
        long now = System.currentTimeMillis();
        databaseManager.purgeExpiredTokens(now);

        ReusableAccessToken reusableAccessToken = reusableTokens.get(playerUuid);
        if (reusableAccessToken != null && reusableAccessToken.expiresAt() >= now) {
            return new IssuedAccessToken(
                    reusableAccessToken.rawToken(),
                    new AuthenticatedSession(playerUuid, playerName, reusableAccessToken.expiresAt())
            );
        }

        if (reusableAccessToken != null) {
            reusableTokens.remove(playerUuid, reusableAccessToken);
        }

        long expiresAt = now + (securityConfiguration.entryTokenValidityMinutes() * 60L * 1000L);
        String rawToken = generateToken();
        String tokenHash = hash(rawToken);
        databaseManager.storeAccessToken(playerUuid, playerName, tokenHash, now, expiresAt);
        reusableTokens.put(playerUuid, new ReusableAccessToken(rawToken, expiresAt));

        return new IssuedAccessToken(rawToken, new AuthenticatedSession(playerUuid, playerName, expiresAt));
    }

    public Optional<IssuedWebPanelSession> openWebSession(String rawAccessToken) {
        if (rawAccessToken == null || rawAccessToken.isBlank()) {
            return Optional.empty();
        }

        long now = System.currentTimeMillis();
        databaseManager.purgeExpiredTokens(now);
        databaseManager.purgeIdleWebSessions(now - webSessionIdleMillis());

        return databaseManager.consumeAccessToken(hash(rawAccessToken), now)
                .map(authenticatedSession -> {
                reusableTokens.computeIfPresent(authenticatedSession.playerUuid(), (uuid, cachedToken) ->
                    cachedToken.rawToken().equals(rawAccessToken) ? null : cachedToken);
                    String rawSessionId = generateToken();
                    String sessionHash = hash(rawSessionId);
                    databaseManager.storeWebSession(
                            authenticatedSession.playerUuid(),
                            authenticatedSession.playerName(),
                            sessionHash,
                            now
                    );
                    return new IssuedWebPanelSession(
                            rawSessionId,
                            new WebPanelSession(
                                    authenticatedSession.playerUuid(),
                                    authenticatedSession.playerName(),
                                    now,
                                    now
                            )
                    );
                });
    }

    public Optional<WebPanelSession> authenticateWebSession(String rawSessionId) {
        if (rawSessionId == null || rawSessionId.isBlank()) {
            return Optional.empty();
        }

        long now = System.currentTimeMillis();
        long minimumLastSeenAt = now - webSessionIdleMillis();
        databaseManager.purgeIdleWebSessions(minimumLastSeenAt);
        return databaseManager.findWebSession(hash(rawSessionId), now, minimumLastSeenAt);
    }

    private long webSessionIdleMillis() {
        return securityConfiguration.webSessionIdleMinutes() * 60L * 1000L;
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao assinar token", exception);
        }
    }
}

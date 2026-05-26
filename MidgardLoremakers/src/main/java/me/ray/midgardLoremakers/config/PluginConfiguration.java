package me.ray.midgardLoremakers.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public record PluginConfiguration(String databaseFileName, WebConfiguration web, SecurityConfiguration security, BookLimits bookLimits) {

    private static final String PUBLIC_URL_PLACEHOLDER = "SEU_DOMINIO_OU_IP";

    public static PluginConfiguration from(FileConfiguration config) {
        return new PluginConfiguration(
                config.getString("database.file", "loremakers.db"),
                new WebConfiguration(
                        config.getString("web.bind-host", "0.0.0.0"),
                        config.getInt("web.port", 8806),
                        config.getString("web.public-url", ""),
                        config.getString("web.public-scheme", "http"),
                        config.getInt("web.public-port", 0)
                ),
                new SecurityConfiguration(
                        config.getLong("security.entry-token-validity-minutes", 15L),
                        config.getLong("security.web-session-idle-minutes", 720L)
                ),
                new BookLimits(
                        config.getInt("books.max-books-per-player", 64),
                        config.getInt("books.max-pages-per-book", 50),
                        config.getInt("books.max-characters-per-page", 1200),
                        config.getInt("books.max-title-length", 48),
                        config.getInt("books.max-category-length", 32)
                )
        );
    }

    public boolean hasValidPublicRouting() {
        return publicRoutingValidationError() == null;
    }

    public boolean hasExplicitPublicUrlOverride() {
        return web.publicUrl() != null
                && !web.publicUrl().isBlank()
                && !web.publicUrl().contains(PUBLIC_URL_PLACEHOLDER);
    }

    public String publicRoutingValidationError() {
        if (hasExplicitPublicUrlOverride()) {
            return explicitPublicUrlValidationError();
        }

        if (web.publicScheme() == null
                || (!web.publicScheme().equalsIgnoreCase("http") && !web.publicScheme().equalsIgnoreCase("https"))) {
            return "web.public-scheme deve usar http ou https.";
        }
        if (resolvedPublicPort() <= 0 || resolvedPublicPort() > 65535) {
            return "web.public-port deve ser 0 ou uma porta entre 1 e 65535.";
        }

        return null;
    }

    public String buildPanelUrl(String rawToken, String connectionHost) {
        String baseUrl = hasExplicitPublicUrlOverride()
                ? web.normalizedPublicUrl()
                : buildAutomaticBaseUrl(connectionHost);
        return baseUrl + "/panel/dashboard?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    public String automaticHostValidationError(String connectionHost) {
        if (connectionHost == null || connectionHost.isBlank()) {
            return "Nao foi possivel detectar o host usado pelo jogador para entrar no servidor.";
        }

        String normalizedHost = connectionHost.trim().toLowerCase(Locale.ROOT);
        if (normalizedHost.equals("0.0.0.0") || normalizedHost.equals("::")) {
            return "O host detectado para o jogador e invalido. Use um IP, dominio, localhost ou configure web.public-url.";
        }
        return null;
    }

    public int resolvedPublicPort() {
        return web.publicPort() > 0 ? web.publicPort() : web.port();
    }

    public String publicRoutingDescription() {
        if (hasExplicitPublicUrlOverride()) {
            return web.normalizedPublicUrl();
        }
        return web.publicScheme().toLowerCase(Locale.ROOT) + "://<host-do-jogador>:" + resolvedPublicPort();
    }

    private String explicitPublicUrlValidationError() {
        try {
            URI uri = URI.create(web.publicUrl());
            if (uri.getScheme() == null || (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https"))) {
                return "web.public-url deve usar http:// ou https://.";
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return "web.public-url precisa de um host valido.";
            }

            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if (host.equals("0.0.0.0") || host.equals("::")) {
                return "web.public-url nao pode usar 0.0.0.0 ou ::. Use o IP publico, dominio ou localhost.";
            }
        } catch (IllegalArgumentException exception) {
            return "web.public-url esta em formato invalido.";
        }

        return null;
    }

    private String buildAutomaticBaseUrl(String connectionHost) {
        String host = formatHostForUrl(connectionHost.trim());
        int publicPort = resolvedPublicPort();
        if (("http".equalsIgnoreCase(web.publicScheme()) && publicPort == 80)
                || ("https".equalsIgnoreCase(web.publicScheme()) && publicPort == 443)) {
            return web.publicScheme().toLowerCase(Locale.ROOT) + "://" + host;
        }
        return web.publicScheme().toLowerCase(Locale.ROOT) + "://" + host + ":" + publicPort;
    }

    private String formatHostForUrl(String host) {
        if (host.contains(":") && !host.startsWith("[") && !host.endsWith("]")) {
            return "[" + host + "]";
        }
        return host;
    }

    public record WebConfiguration(String bindHost, int port, String publicUrl, String publicScheme, int publicPort) {
        public String normalizedPublicUrl() {
            if (publicUrl.endsWith("/")) {
                return publicUrl.substring(0, publicUrl.length() - 1);
            }
            return publicUrl;
        }
    }

    public record SecurityConfiguration(long entryTokenValidityMinutes, long webSessionIdleMinutes) {
    }

    public record BookLimits(int maxBooksPerPlayer,
                             int maxPagesPerBook,
                             int maxCharactersPerPage,
                             int maxTitleLength,
                             int maxCategoryLength) {
    }
}

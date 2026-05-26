package me.ray.midgardLoremakers.web;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.ray.midgardLoremakers.MidgardLoremakers;
import me.ray.midgardLoremakers.config.PluginConfiguration;
import me.ray.midgardLoremakers.model.IssuedWebPanelSession;
import me.ray.midgardLoremakers.model.BookCollaborator;
import me.ray.midgardLoremakers.model.BookSnapshot;
import me.ray.midgardLoremakers.model.BookSnapshotSummary;
import me.ray.midgardLoremakers.model.LoreBook;
import me.ray.midgardLoremakers.model.LoreBookSummary;
import me.ray.midgardLoremakers.model.WebPanelSession;
import me.ray.midgardLoremakers.service.LoreBookService;
import me.ray.midgardLoremakers.service.NotFoundException;
import me.ray.midgardLoremakers.service.TokenService;
import me.ray.midgardLoremakers.service.ValidationException;
import me.ray.midgardLoremakers.util.BookTextFormatter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public final class LoreMakerWebServer {

    private static final String SESSION_COOKIE_NAME = "midgard_loremaker_session";
    private static final int MAX_REQUEST_BODY_BYTES = 512 * 1024;
    private static final int MAX_LORE_LINES = 10;

    private final MidgardLoremakers plugin;
    private final PluginConfiguration pluginConfiguration;
    private final TokenService tokenService;
    private final LoreBookService loreBookService;
    private final Gson gson;
    private final Executor executor;

    private HttpServer httpServer;

    public LoreMakerWebServer(MidgardLoremakers plugin,
                              PluginConfiguration pluginConfiguration,
                              TokenService tokenService,
                              LoreBookService loreBookService,
                              Gson gson,
                              Executor executor) {
        this.plugin = plugin;
        this.pluginConfiguration = pluginConfiguration;
        this.tokenService = tokenService;
        this.loreBookService = loreBookService;
        this.gson = gson;
        this.executor = executor;
    }

    public void start() throws IOException {
        InetSocketAddress socketAddress;
        if (pluginConfiguration.web().bindHost() == null || pluginConfiguration.web().bindHost().isBlank()) {
            socketAddress = new InetSocketAddress(pluginConfiguration.web().port());
        } else {
            socketAddress = new InetSocketAddress(pluginConfiguration.web().bindHost(), pluginConfiguration.web().port());
        }

        httpServer = HttpServer.create(socketAddress, 0);
        httpServer.setExecutor(executor);
        httpServer.createContext("/", withErrorHandling(this::handleRoot));
        httpServer.createContext("/panel", withErrorHandling(this::handlePanelPage));
        httpServer.createContext("/assets/panel.css", withErrorHandling(exchange -> serveResource(exchange, "web/panel.css", "text/css; charset=UTF-8")));
        httpServer.createContext("/assets/panel.js", withErrorHandling(exchange -> serveResource(exchange, "web/panel.js", "application/javascript; charset=UTF-8")));
        httpServer.createContext("/api/session", withErrorHandling(this::handleSession));
        httpServer.createContext("/api/books", withErrorHandling(this::handleBooksRoot));
        httpServer.createContext("/api/books/", withErrorHandling(this::handleBookById));
        httpServer.createContext("/api/categories", withErrorHandling(this::handleCategories));
        httpServer.createContext("/api/categories/", withErrorHandling(this::handleCategoryByName));
        httpServer.start();
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        if (!"/".equals(exchange.getRequestURI().getPath())) {
            sendJson(exchange, 404, Map.of("error", "Rota nao encontrada."));
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, List.of("GET"));
            return;
        }

        Headers headers = exchange.getResponseHeaders();
        String query = exchange.getRequestURI().getRawQuery();
        headers.set("Location", query == null || query.isBlank() ? "/panel/dashboard" : "/panel/dashboard?" + query);
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private void handlePanelPage(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, List.of("GET"));
            return;
        }

        PanelBootstrap bootstrap = resolvePanelBootstrap(exchange);
        servePanelPage(exchange, bootstrap);
    }

    private void handleSession(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, List.of("GET"));
            return;
        }

        PanelBootstrap bootstrap = resolvePanelBootstrap(exchange);
        if (bootstrap.session().isEmpty()) {
            exchange.getResponseHeaders().add("Set-Cookie", clearSessionCookie());
            sendJson(exchange, 401, Map.of("error", "Sessao invalida ou ausente. Volte ao jogo e use /loremaker."));
            return;
        }

        Map<String, Object> payload = buildSessionPayload(bootstrap);
        sendJson(exchange, 200, payload);
    }

    private void handleBooksRoot(HttpExchange exchange) throws IOException {
        Optional<WebPanelSession> authenticatedSession = authenticateWebSession(exchange);
        if (authenticatedSession.isEmpty()) {
            exchange.getResponseHeaders().add("Set-Cookie", clearSessionCookie());
            sendJson(exchange, 401, Map.of("error", "Sessao invalida ou ausente."));
            return;
        }

        try {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<LoreBookSummary> books = loreBookService.listBooks(authenticatedSession.get().playerUuid());
                sendJson(exchange, 200, books);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                SaveBookRequest request = readJsonBody(exchange, SaveBookRequest.class);
                LoreBook savedBook = loreBookService.saveBook(
                        authenticatedSession.get().playerUuid(),
                        request.id,
                        request.title,
                        request.category,
                        request.tags,
                        request.pages
                );
                sendJson(exchange, 200, savedBook);
                return;
            }

            sendMethodNotAllowed(exchange, List.of("GET", "POST"));
        } catch (ValidationException exception) {
            sendJson(exchange, 400, Map.of("error", exception.getMessage()));
        } catch (JsonParseException exception) {
            sendJson(exchange, 400, Map.of("error", "Corpo JSON invalido."));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao processar /api/books: " + exception.getMessage());
            sendJson(exchange, 500, Map.of("error", "Falha interna ao salvar o livro."));
        }
    }

    private void handleCategories(HttpExchange exchange) throws IOException {
        Optional<WebPanelSession> authenticatedSession = authenticateWebSession(exchange);
        if (authenticatedSession.isEmpty()) {
            exchange.getResponseHeaders().add("Set-Cookie", clearSessionCookie());
            sendJson(exchange, 401, Map.of("error", "Sessao invalida ou ausente."));
            return;
        }

        try {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<String> categories = loreBookService.listCategories(authenticatedSession.get().playerUuid());
                sendJson(exchange, 200, categories);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                CategoryRequest request = readJsonBody(exchange, CategoryRequest.class);
                String category = loreBookService.createCategory(authenticatedSession.get().playerUuid(), request.name);
                sendJson(exchange, 200, Map.of("name", category));
                return;
            }

            sendMethodNotAllowed(exchange, List.of("GET", "POST"));
        } catch (ValidationException exception) {
            sendJson(exchange, 400, Map.of("error", exception.getMessage()));
        } catch (JsonParseException exception) {
            sendJson(exchange, 400, Map.of("error", "Corpo JSON invalido."));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao processar /api/categories: " + exception.getMessage());
            sendJson(exchange, 500, Map.of("error", "Falha interna ao processar categorias."));
        }
    }

    private void handleCategoryByName(HttpExchange exchange) throws IOException {
        Optional<WebPanelSession> authenticatedSession = authenticateWebSession(exchange);
        if (authenticatedSession.isEmpty()) {
            exchange.getResponseHeaders().add("Set-Cookie", clearSessionCookie());
            sendJson(exchange, 401, Map.of("error", "Sessao invalida ou ausente."));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String rawCategory = path.substring("/api/categories/".length()).trim();
        if (rawCategory.isEmpty()) {
            sendJson(exchange, 400, Map.of("error", "Categoria nao informada."));
            return;
        }

        String categoryName = URLDecoder.decode(rawCategory, StandardCharsets.UTF_8);

        try {
            if ("PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
                RenameCategoryRequest request = readJsonBody(exchange, RenameCategoryRequest.class);
                String renamed = loreBookService.renameCategory(authenticatedSession.get().playerUuid(), categoryName, request.newName);
                sendJson(exchange, 200, Map.of("name", renamed));
                return;
            }

            if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> parameters = queryParameters(exchange);
                String targetCategory = parameters.getOrDefault("target", "Sem categoria");
                loreBookService.deleteCategory(authenticatedSession.get().playerUuid(), categoryName, targetCategory);
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            sendMethodNotAllowed(exchange, List.of("PATCH", "DELETE"));
        } catch (ValidationException exception) {
            sendJson(exchange, 400, Map.of("error", exception.getMessage()));
        } catch (NotFoundException exception) {
            sendJson(exchange, 404, Map.of("error", exception.getMessage()));
        } catch (JsonParseException exception) {
            sendJson(exchange, 400, Map.of("error", "Corpo JSON invalido."));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao processar /api/categories/{name}: " + exception.getMessage());
            sendJson(exchange, 500, Map.of("error", "Falha interna ao processar categoria."));
        }
    }

    private void handleBookById(HttpExchange exchange) throws IOException {
        Optional<WebPanelSession> authenticatedSession = authenticateWebSession(exchange);
        if (authenticatedSession.isEmpty()) {
            exchange.getResponseHeaders().add("Set-Cookie", clearSessionCookie());
            sendJson(exchange, 401, Map.of("error", "Sessao invalida ou ausente."));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String rawSuffix = path.substring("/api/books/".length()).trim();

        if (rawSuffix.isEmpty()) {
            sendJson(exchange, 404, Map.of("error", "Livro nao informado."));
            return;
        }

        if (rawSuffix.matches("\\d+/export")) {
            String rawId = rawSuffix.substring(0, rawSuffix.indexOf('/'));
            handleBookExport(exchange, authenticatedSession.get(), Long.parseLong(rawId));
            return;
        }

        if (rawSuffix.matches("\\d+/duplicate")) {
            String rawId = rawSuffix.substring(0, rawSuffix.indexOf('/'));
            handleBookDuplicate(exchange, authenticatedSession.get(), Long.parseLong(rawId));
            return;
        }

        if (rawSuffix.matches("\\d+/history/\\d+")) {
            String[] histParts = rawSuffix.split("/");
            long histBookId = Long.parseLong(histParts[0]);
            long histSnapshotId = Long.parseLong(histParts[2]);
            handleSnapshotPreview(exchange, authenticatedSession.get(), histBookId, histSnapshotId);
            return;
        }

        if (rawSuffix.matches("\\d+/history")) {
            String rawId = rawSuffix.substring(0, rawSuffix.indexOf('/'));
            handleBookHistory(exchange, authenticatedSession.get(), Long.parseLong(rawId));
            return;
        }

        if (rawSuffix.matches("\\d+/restore/\\d+")) {
            String[] parts = rawSuffix.split("/");
            long bookId = Long.parseLong(parts[0]);
            long snapshotId = Long.parseLong(parts[2]);
            handleBookRestore(exchange, authenticatedSession.get(), bookId, snapshotId);
            return;
        }

        if (rawSuffix.matches("\\d+/collaborators")) {
            String rawId = rawSuffix.substring(0, rawSuffix.indexOf('/'));
            handleBookCollaborators(exchange, authenticatedSession.get(), Long.parseLong(rawId));
            return;
        }

        if (rawSuffix.matches("\\d+/collaborators/[0-9a-fA-F-]+")) {
            String[] parts = rawSuffix.split("/");
            long bookId = Long.parseLong(parts[0]);
            UUID collaboratorUuid = UUID.fromString(parts[2]);
            handleRemoveCollaborator(exchange, authenticatedSession.get(), bookId, collaboratorUuid);
            return;
        }

        if (rawSuffix.matches("\\d+/favorite")) {
            String rawId = rawSuffix.substring(0, rawSuffix.indexOf('/'));
            handleBookFavorite(exchange, authenticatedSession.get(), Long.parseLong(rawId));
            return;
        }

        try {
            long bookId = Long.parseLong(rawSuffix);

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                LoreBook book = loreBookService.findBook(authenticatedSession.get().playerUuid(), bookId)
                        .orElseThrow(() -> new NotFoundException("Livro nao encontrado."));
                sendJson(exchange, 200, book);
                return;
            }

            if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                loreBookService.deleteBook(authenticatedSession.get().playerUuid(), bookId);
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            sendMethodNotAllowed(exchange, List.of("GET", "DELETE"));
        } catch (NumberFormatException exception) {
            sendJson(exchange, 400, Map.of("error", "Identificador de livro invalido."));
        } catch (NotFoundException exception) {
            sendJson(exchange, 404, Map.of("error", exception.getMessage()));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao processar " + path + ": " + exception.getMessage());
            sendJson(exchange, 500, Map.of("error", "Falha interna ao consultar o livro."));
        }
    }

    private void handleSnapshotPreview(HttpExchange exchange, WebPanelSession session, long bookId, long snapshotId) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, List.of("GET"));
            return;
        }

        try {
            BookSnapshot snapshot = loreBookService.previewSnapshot(session.playerUuid(), bookId, snapshotId);
            sendJson(exchange, 200, snapshot);
        } catch (NotFoundException exception) {
            sendJson(exchange, 404, Map.of("error", exception.getMessage()));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao carregar preview de snapshot " + snapshotId + ": " + exception.getMessage());
            sendJson(exchange, 500, Map.of("error", "Falha ao carregar preview da versao."));
        }
    }

    private void handleBookHistory(HttpExchange exchange, WebPanelSession session, long bookId) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, List.of("GET"));
            return;
        }

        try {
            List<BookSnapshotSummary> snapshots = loreBookService.listSnapshots(session.playerUuid(), bookId);
            sendJson(exchange, 200, snapshots);
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao listar historico do livro " + bookId + ": " + exception.getMessage());
            sendJson(exchange, 500, Map.of("error", "Falha ao listar historico."));
        }
    }

    private void handleBookRestore(HttpExchange exchange, WebPanelSession session, long bookId, long snapshotId) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, List.of("POST"));
            return;
        }

        try {
            LoreBook restored = loreBookService.restoreSnapshot(session.playerUuid(), snapshotId);
            sendJson(exchange, 200, restored);
        } catch (NotFoundException exception) {
            sendJson(exchange, 404, Map.of("error", exception.getMessage()));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao restaurar snapshot " + snapshotId + ": " + exception.getMessage());
            sendJson(exchange, 500, Map.of("error", "Falha ao restaurar versao."));
        }
    }

    private void handleBookDuplicate(HttpExchange exchange, WebPanelSession session, long bookId) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, List.of("POST"));
            return;
        }

        try {
            LoreBook duplicated = loreBookService.duplicateBook(session.playerUuid(), bookId);
            sendJson(exchange, 200, duplicated);
        } catch (ValidationException exception) {
            sendJson(exchange, 400, Map.of("error", exception.getMessage()));
        } catch (NotFoundException exception) {
            sendJson(exchange, 404, Map.of("error", exception.getMessage()));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao duplicar livro " + bookId + ": " + exception.getMessage());
            sendJson(exchange, 500, Map.of("error", "Falha ao duplicar o livro."));
        }
    }

    private void handleBookCollaborators(HttpExchange exchange, WebPanelSession session, long bookId) throws IOException {
        try {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                boolean isOwner = loreBookService.isOwner(session.playerUuid(), bookId);
                var collaborators = isOwner
                        ? loreBookService.listCollaborators(session.playerUuid(), bookId)
                        : List.of();
                sendJson(exchange, 200, Map.of("collaborators", collaborators, "isOwner", isOwner));
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                CollaboratorRequest request = readJsonBody(exchange, CollaboratorRequest.class);
                if (request.playerName == null || request.playerName.isBlank()) {
                    sendJson(exchange, 400, Map.of("error", "Nome do jogador e obrigatorio."));
                    return;
                }

                String playerName = request.playerName.trim();
                CompletableFuture<UUID> uuidFuture = new CompletableFuture<>();

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player onlinePlayer = plugin.getServer().getPlayerExact(playerName);
                    if (onlinePlayer != null) {
                        uuidFuture.complete(onlinePlayer.getUniqueId());
                    } else {
                        uuidFuture.complete(null);
                    }
                });

                UUID collaboratorUuid = uuidFuture.get(5, TimeUnit.SECONDS);
                if (collaboratorUuid == null) {
                    sendJson(exchange, 400, Map.of("error", "Jogador \"" + playerName + "\" nao esta online."));
                    return;
                }

                loreBookService.addCollaborator(session.playerUuid(), bookId, collaboratorUuid, playerName);
                sendJson(exchange, 200, Map.of("message", "Colaborador adicionado com sucesso!"));
                return;
            }

            sendMethodNotAllowed(exchange, List.of("GET", "POST"));
        } catch (ValidationException exception) {
            sendJson(exchange, 400, Map.of("error", exception.getMessage()));
        } catch (NotFoundException exception) {
            sendJson(exchange, 404, Map.of("error", exception.getMessage()));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao gerenciar colaboradores do livro " + bookId + ": " + exception.getMessage());
            sendJson(exchange, 500, Map.of("error", "Falha ao gerenciar colaboradores."));
        }
    }

    private void handleRemoveCollaborator(HttpExchange exchange, WebPanelSession session, long bookId, UUID collaboratorUuid) throws IOException {
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, List.of("DELETE"));
            return;
        }

        try {
            loreBookService.removeCollaborator(session.playerUuid(), bookId, collaboratorUuid);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        } catch (NotFoundException exception) {
            sendJson(exchange, 404, Map.of("error", exception.getMessage()));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao remover colaborador de livro " + bookId + ": " + exception.getMessage());
            sendJson(exchange, 500, Map.of("error", "Falha ao remover colaborador."));
        }
    }

    private void handleBookFavorite(HttpExchange exchange, WebPanelSession session, long bookId) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, List.of("POST"));
            return;
        }

        try {
            boolean favorite = loreBookService.toggleFavorite(session.playerUuid(), bookId);
            sendJson(exchange, 200, Map.of("favorite", favorite));
        } catch (NotFoundException exception) {
            sendJson(exchange, 404, Map.of("error", exception.getMessage()));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao alternar favorito do livro " + bookId + ": " + exception.getMessage());
            sendJson(exchange, 500, Map.of("error", "Falha ao alternar favorito."));
        }
    }

    private void handleBookExport(HttpExchange exchange, WebPanelSession session, long bookId) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, List.of("POST"));
            return;
        }

        try {
            ExportBookRequest exportRequest;
            try {
                exportRequest = readJsonBody(exchange, ExportBookRequest.class);
            } catch (Exception ignored) {
                exportRequest = new ExportBookRequest();
            }

            String authorName = (exportRequest.author != null && !exportRequest.author.isBlank())
                    ? exportRequest.author.trim()
                    : session.playerName();

            LoreBook book = loreBookService.findBook(session.playerUuid(), bookId)
                    .orElseThrow(() -> new NotFoundException("Livro nao encontrado."));

            final String finalAuthor = authorName;
            final boolean glow = exportRequest.glow;
            final String lore = (exportRequest.lore != null && !exportRequest.lore.isBlank())
                    ? exportRequest.lore.trim() : null;
            final String displayColor = (exportRequest.displayColor != null && !exportRequest.displayColor.isBlank())
                    ? exportRequest.displayColor.trim() : null;
            CompletableFuture<Boolean> future = new CompletableFuture<>();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    Player player = plugin.getServer().getPlayer(session.playerUuid());
                    if (player == null || !player.isOnline()) {
                        future.complete(false);
                        return;
                    }

                    ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
                    BookMeta meta = (BookMeta) bookItem.getItemMeta();
                    meta.setTitle(book.title());
                    meta.setAuthor(finalAuthor);

                    for (String page : book.pages()) {
                        meta.addPages(BookTextFormatter.parseFormattedText(page));
                    }

                    if (glow) {
                        meta.setEnchantmentGlintOverride(true);
                    }

                    if (displayColor != null) {
                        NamedTextColor color = NamedTextColor.NAMES.value(displayColor);
                        if (color != null) {
                            meta.displayName(Component.text(book.title()).color(color)
                                    .decoration(TextDecoration.ITALIC, false));
                        }
                    }

                    if (lore != null) {
                        String[] parts = lore.split("\\|");
                        List<Component> loreLines = new java.util.ArrayList<>();
                        int lineLimit = Math.min(parts.length, MAX_LORE_LINES);
                        for (int li = 0; li < lineLimit; li++) {
                            loreLines.add(Component.text(parts[li].trim()).color(NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false));
                        }
                        meta.lore(loreLines);
                    }

                    bookItem.setItemMeta(meta);
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(bookItem);
                    if (!overflow.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), overflow.values().iterator().next());
                    }
                    future.complete(true);
                } catch (Exception exception) {
                    future.completeExceptionally(exception);
                }
            });

            boolean given = future.get(5, TimeUnit.SECONDS);
            if (given) {
                sendJson(exchange, 200, Map.of("message", "Livro exportado para o seu inventario!"));
            } else {
                sendJson(exchange, 400, Map.of("error", "Voce precisa estar online no servidor para receber o livro."));
            }
        } catch (NotFoundException exception) {
            sendJson(exchange, 404, Map.of("error", exception.getMessage()));
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao exportar livro " + bookId + ": " + exception.getMessage());
            sendJson(exchange, 500, Map.of("error", "Falha ao exportar o livro para o jogo."));
        }
    }

    private Optional<WebPanelSession> authenticateWebSession(HttpExchange exchange) {
        String headerSessionId = exchange.getRequestHeaders().getFirst("X-Midgard-Session");
        Optional<WebPanelSession> headerSession = tokenService.authenticateWebSession(headerSessionId);
        if (headerSession.isPresent()) {
            return headerSession;
        }

        return tokenService.authenticateWebSession(extractCookie(exchange, SESSION_COOKIE_NAME));
    }

    private PanelBootstrap resolvePanelBootstrap(HttpExchange exchange) {
        Optional<WebPanelSession> authenticatedSession = authenticateWebSession(exchange);
        if (authenticatedSession.isPresent()) {
            return new PanelBootstrap(authenticatedSession, null);
        }

        String entryToken = queryParameters(exchange).get("token");
        if (entryToken == null || entryToken.isBlank()) {
            return new PanelBootstrap(Optional.empty(), null);
        }

        Optional<IssuedWebPanelSession> openedSession = tokenService.openWebSession(entryToken);
        if (openedSession.isEmpty()) {
            exchange.getResponseHeaders().add("Set-Cookie", clearSessionCookie());
            return new PanelBootstrap(Optional.empty(), null);
        }

        exchange.getResponseHeaders().add("Set-Cookie", buildSessionCookie(openedSession.get().rawSessionId()));
        return new PanelBootstrap(Optional.of(openedSession.get().session()), openedSession.get().rawSessionId());
    }

    private Map<String, Object> buildSessionPayload(PanelBootstrap bootstrap) {
        PluginConfiguration.BookLimits limits = loreBookService.bookLimits();
        WebPanelSession session = bootstrap.session().orElseThrow();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerUuid", session.playerUuid());
        payload.put("playerName", session.playerName());
        payload.put("sessionActive", true);
        payload.put("sessionId", bootstrap.rawSessionId());
        payload.put("limits", Map.of(
                "maxBooksPerPlayer", limits.maxBooksPerPlayer(),
                "maxPagesPerBook", limits.maxPagesPerBook(),
                "maxCharactersPerPage", limits.maxCharactersPerPage(),
                "maxTitleLength", limits.maxTitleLength(),
                "maxCategoryLength", limits.maxCategoryLength()
        ));
        return payload;
    }

    private void servePanelPage(HttpExchange exchange, PanelBootstrap bootstrap) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, List.of("GET"));
            return;
        }

        try (InputStream inputStream = plugin.getResource("web/panel.html")) {
            if (inputStream == null) {
                sendJson(exchange, 404, Map.of("error", "Recurso nao encontrado."));
                return;
            }

            String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> bootstrapPayload = new LinkedHashMap<>();
            bootstrapPayload.put("session", bootstrap.session().map(session -> buildSessionPayload(bootstrap)).orElse(null));
            String bootstrapJson = gson.toJson(bootstrapPayload);
            String safeJson = bootstrapJson.replace("</", "<\\/");
            byte[] bytes = html.replace("__LOREMAKER_BOOTSTRAP_JSON__", safeJson).getBytes(StandardCharsets.UTF_8);

            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "text/html; charset=UTF-8");
            headers.set("Cache-Control", "no-store");
            headers.set("X-Content-Type-Options", "nosniff");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }
    }

    private void serveResource(HttpExchange exchange, String resourcePath, String contentType) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, List.of("GET"));
            return;
        }

        try (InputStream inputStream = plugin.getResource(resourcePath)) {
            if (inputStream == null) {
                sendJson(exchange, 404, Map.of("error", "Recurso nao encontrado."));
                return;
            }

            byte[] bytes = inputStream.readAllBytes();
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", contentType);
            headers.set("Cache-Control", "no-store");
            headers.set("X-Content-Type-Options", "nosniff");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }
    }

    private <T> T readJsonBody(HttpExchange exchange, Class<T> type) throws IOException {
        byte[] bodyBytes;
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int read;
            int total = 0;
            while ((read = is.read(chunk)) != -1) {
                total += read;
                if (total > MAX_REQUEST_BODY_BYTES) {
                    throw new JsonParseException("Corpo da requisicao excede o limite de " + (MAX_REQUEST_BODY_BYTES / 1024) + " KB.");
                }
                buffer.write(chunk, 0, read);
            }
            bodyBytes = buffer.toByteArray();
        }
        try (StringReader reader = new StringReader(new String(bodyBytes, StandardCharsets.UTF_8))) {
            T parsed = gson.fromJson(reader, type);
            if (parsed == null) {
                throw new JsonParseException("Corpo vazio");
            }
            return parsed;
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] bytes = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Cache-Control", "no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private void sendMethodNotAllowed(HttpExchange exchange, List<String> allowedMethods) throws IOException {
        exchange.getResponseHeaders().set("Allow", String.join(", ", allowedMethods));
        sendJson(exchange, 405, Map.of("error", "Metodo nao permitido."));
    }

    private HttpHandler withErrorHandling(CheckedHandler checkedHandler) {
        return exchange -> {
            try {
                checkedHandler.handle(exchange);
            } catch (Exception exception) {
                plugin.getLogger().warning("Falha HTTP em " + exchange.getRequestURI() + ": " + exception.getMessage());
                if (!exchange.getResponseHeaders().containsKey("Content-Type")) {
                    sendJson(exchange, 500, Map.of("error", "Falha interna ao atender o painel web."));
                } else {
                    exchange.close();
                }
            }
        };
    }

    private String extractCookie(HttpExchange exchange, String cookieName) {
        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");
        if (cookieHeaders == null || cookieHeaders.isEmpty()) {
            return null;
        }

        for (String header : cookieHeaders) {
            for (String cookie : header.split(";")) {
                String trimmed = cookie.trim();
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String name = trimmed.substring(0, separator).trim();
                if (!name.equals(cookieName)) {
                    continue;
                }

                return trimmed.substring(separator + 1).trim();
            }
        }

        return null;
    }

    private String buildSessionCookie(String rawSessionId) {
        String cookie = SESSION_COOKIE_NAME + "=" + rawSessionId + "; Path=/; HttpOnly; SameSite=Lax";
        if ("https".equalsIgnoreCase(pluginConfiguration.web().publicScheme())) {
            cookie += "; Secure";
        }
        return cookie;
    }

    private String clearSessionCookie() {
        String cookie = SESSION_COOKIE_NAME + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax";
        if ("https".equalsIgnoreCase(pluginConfiguration.web().publicScheme())) {
            cookie += "; Secure";
        }
        return cookie;
    }

    private Map<String, String> queryParameters(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        Map<String, String> parameters = new LinkedHashMap<>();
        if (query == null || query.isBlank()) {
            return parameters;
        }

        for (String pair : query.split("&")) {
            String[] segments = pair.split("=", 2);
            String key = URLDecoder.decode(segments[0], StandardCharsets.UTF_8);
            String value = segments.length > 1
                    ? URLDecoder.decode(segments[1], StandardCharsets.UTF_8)
                    : "";
            parameters.put(key, value);
        }

        return parameters;
    }

    private static final class SaveBookRequest {
        private Long id;
        private String title;
        private String category;
        private List<String> tags;
        private List<String> pages;
    }

    private static final class ExportBookRequest {
        private String author;
        private String lore;
        private String displayColor;
        private boolean glow;
    }

    private static final class CollaboratorRequest {
        private String playerName;
    }

    private static final class CategoryRequest {
        private String name;
    }

    private static final class RenameCategoryRequest {
        private String newName;
    }

    private record PanelBootstrap(Optional<WebPanelSession> session, String rawSessionId) {
    }

    @FunctionalInterface
    private interface CheckedHandler {
        void handle(HttpExchange exchange) throws Exception;
    }
}

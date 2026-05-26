package com.midgardbot.integrations;

import com.google.gson.*;
import com.midgardbot.config.BotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Gerencia notícias do Launcher salvando um news.json no repositório GitHub.
 * O Launcher lê esse arquivo via raw.githubusercontent.com.
 * Suporta categorias: noticias, lore, changelogs.
 */
public class NewsManager {

    private static final Logger logger = LoggerFactory.getLogger(NewsManager.class);
    private static final String LAUNCHER_REPO = "MidgardNetwork/MidgardLauncher";
    private static final String NEWS_FILE_PATH = "news.json";
    private static final String API_BASE = "https://api.github.com/repos/" + LAUNCHER_REPO + "/contents/" + NEWS_FILE_PATH;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy, HH:mm", Locale.of("pt", "BR"))
            .withZone(ZoneId.of("America/Sao_Paulo"));

    /** Categorias válidas para notícias do Launcher. */
    public static final List<String> VALID_CATEGORIES = List.of("noticias", "lore", "changelogs");

    /** Nomes bonitos para exibição das categorias. */
    public static final Map<String, String> CATEGORY_LABELS = Map.of(
        "noticias", "📰 Notícias",
        "lore", "📜 Lore",
        "changelogs", "📋 Changelogs"
    );

    /** Emojis para cada categoria. */
    public static final Map<String, String> CATEGORY_EMOJIS = Map.of(
        "noticias", "📰",
        "lore", "📜",
        "changelogs", "📋"
    );

    /**
     * Busca o news.json atual do GitHub.
     */
    private static JsonObject fetchCurrentFile() throws Exception {
        String token = BotConfig.getGithubToken();
        HttpURLConnection conn = (HttpURLConnection) new URI(API_BASE).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

        int code = conn.getResponseCode();
        if (code == 200) {
            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } else if (code == 404) {
            return null;
        } else {
            throw new RuntimeException("GitHub API retornou código " + code);
        }
    }

    /**
     * Salva o news.json no GitHub (cria ou atualiza).
     */
    private static void saveFile(JsonArray articles, String sha) throws Exception {
        String token = BotConfig.getGithubToken();

        JsonObject wrapper = new JsonObject();
        wrapper.add("articles", articles);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonContent = gson.toJson(wrapper);
        String encoded = Base64.getEncoder().encodeToString(jsonContent.getBytes(StandardCharsets.UTF_8));

        JsonObject body = new JsonObject();
        body.addProperty("message", "Atualizar notícias do Launcher");
        body.addProperty("content", encoded);
        if (sha != null) {
            body.addProperty("sha", sha);
        }

        HttpURLConnection conn = (HttpURLConnection) new URI(API_BASE).toURL().openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200 && code != 201) {
            String error = "";
            try (InputStreamReader reader = new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)) {
                error = JsonParser.parseReader(reader).toString();
            }
            throw new RuntimeException("Falha ao salvar no GitHub (código " + code + "): " + error);
        }

        logger.info("news.json atualizado no GitHub com sucesso.");
    }

    /**
     * Lê o array de artigos atual do GitHub.
     */
    private static JsonArray readArticles(JsonObject fileData) {
        if (fileData == null) return new JsonArray();

        String content = fileData.get("content").getAsString().replaceAll("\\s", "");
        String decoded = new String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8);
        JsonObject wrapper = JsonParser.parseString(decoded).getAsJsonObject();
        return wrapper.has("articles") ? wrapper.getAsJsonArray("articles") : new JsonArray();
    }

    /**
     * Adiciona uma notícia com categoria.
     */
    public static void addNews(String title, String content, String author, String category, String imageUrl) throws Exception {
        JsonObject fileData = fetchCurrentFile();
        JsonArray articles = readArticles(fileData);
        String sha = fileData != null ? fileData.get("sha").getAsString() : null;

        JsonObject article = new JsonObject();
        article.addProperty("title", title);
        article.addProperty("content", content);
        article.addProperty("author", author);
        article.addProperty("category", category != null ? category : "noticias");
        article.addProperty("date", DATE_FMT.format(Instant.now()));
        if (imageUrl != null && !imageUrl.isEmpty()) {
            article.addProperty("image", imageUrl);
        }

        JsonArray newArticles = new JsonArray();
        newArticles.add(article);
        for (JsonElement el : articles) {
            newArticles.add(el);
        }

        saveFile(newArticles, sha);
    }

    /**
     * Lista todas as notícias.
     */
    public static JsonArray getNews() throws Exception {
        JsonObject fileData = fetchCurrentFile();
        return readArticles(fileData);
    }

    /**
     * Lista notícias filtradas por categoria.
     */
    public static JsonArray getNewsByCategory(String category) throws Exception {
        JsonArray all = getNews();
        JsonArray filtered = new JsonArray();
        for (JsonElement el : all) {
            JsonObject art = el.getAsJsonObject();
            String cat = art.has("category") ? art.get("category").getAsString() : "noticias";
            if (cat.equals(category)) {
                filtered.add(art);
            }
        }
        return filtered;
    }

    /**
     * Remove uma notícia pelo índice global.
     * @return O título da notícia removida.
     */
    public static String removeNews(int index) throws Exception {
        JsonObject fileData = fetchCurrentFile();
        JsonArray articles = readArticles(fileData);
        String sha = fileData != null ? fileData.get("sha").getAsString() : null;

        if (index < 0 || index >= articles.size()) {
            throw new IndexOutOfBoundsException("Índice inválido: " + (index + 1));
        }

        String removedTitle = articles.get(index).getAsJsonObject().get("title").getAsString();
        articles.remove(index);

        saveFile(articles, sha);
        return removedTitle;
    }

    /**
     * Edita uma notícia existente pelo índice global.
     * Apenas os campos não-nulos serão atualizados.
     */
    public static void editNews(int index, String newTitle, String newContent, String newCategory, String newImageUrl) throws Exception {
        JsonObject fileData = fetchCurrentFile();
        JsonArray articles = readArticles(fileData);
        String sha = fileData != null ? fileData.get("sha").getAsString() : null;

        if (index < 0 || index >= articles.size()) {
            throw new IndexOutOfBoundsException("Índice inválido: " + (index + 1));
        }

        JsonObject article = articles.get(index).getAsJsonObject();
        if (newTitle != null) article.addProperty("title", newTitle);
        if (newContent != null) article.addProperty("content", newContent);
        if (newCategory != null) article.addProperty("category", newCategory);
        if (newImageUrl != null) {
            if (newImageUrl.isEmpty()) {
                article.remove("image");
            } else {
                article.addProperty("image", newImageUrl);
            }
        }

        saveFile(articles, sha);
    }

    /**
     * Conta quantas notícias existem por categoria.
     */
    public static Map<String, Integer> countByCategory() throws Exception {
        JsonArray all = getNews();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String cat : VALID_CATEGORIES) {
            counts.put(cat, 0);
        }
        for (JsonElement el : all) {
            JsonObject art = el.getAsJsonObject();
            String cat = art.has("category") ? art.get("category").getAsString() : "noticias";
            counts.merge(cat, 1, Integer::sum);
        }
        return counts;
    }
}

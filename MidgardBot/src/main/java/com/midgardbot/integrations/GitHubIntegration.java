package com.midgardbot.integrations;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.midgardbot.config.BotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GitHubIntegration {

    private static final Logger logger = LoggerFactory.getLogger(GitHubIntegration.class);
    private static final String API_URL = "https://api.github.com/repos/";

    public static String createIssue(String title, String body, List<String> labels) {
        String token = BotConfig.getGithubToken();
        String repo = BotConfig.getGithubRepo();

        if (token == null || token.isEmpty()) {
            logger.warn("GITHUB_TOKEN não configurado. Não é possível criar issues.");
            return null;
        }

        try {
            URL url = new java.net.URI(API_URL + repo + "/issues").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            JsonObject json = new JsonObject();
            json.addProperty("title", title);
            json.addProperty("body", body);
            
            if (labels != null && !labels.isEmpty()) {
                com.google.gson.JsonArray jsonLabels = new com.google.gson.JsonArray();
                for (String label : labels) {
                    jsonLabels.add(label);
                }
                json.add("labels", jsonLabels);
            }

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 201) {
                InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                return response.get("html_url").getAsString();
            } else {
                logger.error("Falha ao criar issue no GitHub. Código: " + responseCode);
                try (InputStreamReader reader = new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)) {
                    logger.error("Resposta: " + JsonParser.parseReader(reader).toString());
                }
            }

        } catch (Exception e) {
            logger.error("Erro ao conectar com GitHub: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}

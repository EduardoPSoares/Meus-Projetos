package me.ray.midgardspigot.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;

public class AutoUpdater {

    private final JavaPlugin plugin;
    private static final String REPO = "MidgardNetwork/MidgardProjects";
    private static final String STATUS_FILE = "updater_status.json";
    private boolean allowPrereleases = false;

    public AutoUpdater(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setAllowPrereleases(boolean allowPrereleases) {
        this.allowPrereleases = allowPrereleases;
    }

    public void checkForUpdate() {
        plugin.getLogger().info("Verificando atualizações no GitHub...");
        try {
            URL url = new URL("https://api.github.com/repos/" + REPO + "/releases/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "MidgardMaintenance-Updater");
            
            String token = plugin.getConfig().getString("update.github-token");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (conn.getResponseCode() == 404) {
                plugin.getLogger().info("Nenhuma release encontrada (404).");
                return;
            }

            if (conn.getResponseCode() != 200) {
                plugin.getLogger().warning("Falha ao verificar atualizações. HTTP: " + conn.getResponseCode());
                conn.disconnect();
                return;
            }

            JsonObject json;
            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            } finally {
                conn.disconnect();
            }

            boolean isPrerelease = json.has("prerelease") && json.get("prerelease").getAsBoolean();
            String latestVersion = json.get("tag_name").getAsString().replace("v", "");
            String currentVersion = plugin.getDescription().getVersion();
            String changelog = json.has("body") ? json.get("body").getAsString() : "Sem changelog.";

            if (isPrerelease && !allowPrereleases) {
                plugin.getLogger().info("Versão pre-release encontrada (" + latestVersion + "), mas atualizações beta estão desativadas.");
                return;
            }

            if (isNewer(latestVersion, currentVersion)) {
                if (isLoopDetected(latestVersion)) {
                    plugin.getLogger().warning("Loop de atualização detectado! O plugin tentou atualizar para a versão " + latestVersion + " recentemente.");
                    return;
                }

                plugin.getLogger().info("Nova versão encontrada: " + latestVersion + " (Atual: " + currentVersion + ")");
                
                String downloadUrl = null;
                long expectedSize = 0;
                boolean isApiUrl = false;

                for (var element : json.get("assets").getAsJsonArray()) {
                    JsonObject asset = element.getAsJsonObject();
                    if (asset.get("name").getAsString().equalsIgnoreCase("MidgardMaintenance.jar")) {
                        expectedSize = asset.get("size").getAsLong();
                        if (token != null && !token.isEmpty()) {
                            downloadUrl = asset.get("url").getAsString();
                            isApiUrl = true;
                        } else {
                            downloadUrl = asset.get("browser_download_url").getAsString();
                            isApiUrl = false;
                        }
                        break;
                    }
                }

                if (downloadUrl != null) {
                    updatePlugin(downloadUrl, latestVersion, changelog, expectedSize, isApiUrl);
                } else {
                    plugin.getLogger().warning("Asset 'MidgardMaintenance.jar' não encontrado na release.");
                }
            } else {
                plugin.getLogger().info("O plugin já está atualizado (Versão: " + currentVersion + ").");
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao verificar atualização: " + e.getMessage(), e);
        }
    }

    private void updatePlugin(String downloadUrl, String version, String changelog, long expectedSize, boolean isApiUrl) {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();
            
            File tempFile = new File(dataFolder, "MidgardMaintenance.jar.tmp");
            boolean success = false;
            int maxRetries = 3;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    plugin.getLogger().info("Baixando atualização (v" + version + ") - Tentativa " + attempt + "/" + maxRetries + "...");
                    
                    HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
                    conn.setRequestProperty("User-Agent", "MidgardMaintenance-Updater");
                    
                    String token = plugin.getConfig().getString("update.github-token");
                    if (token != null && !token.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                        if (isApiUrl) {
                            conn.setRequestProperty("Accept", "application/octet-stream");
                        }
                    }

                    conn.setInstanceFollowRedirects(false);
                    int status = conn.getResponseCode();
                    
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 302) {
                        String newUrl = conn.getHeaderField("Location");
                        conn.disconnect();
                        conn = (HttpURLConnection) new URL(newUrl).openConnection();
                        conn.setRequestProperty("User-Agent", "MidgardMaintenance-Updater");
                    } else if (status != 200) {
                        throw new RuntimeException("HTTP " + status);
                    }
                    
                    try (InputStream in = conn.getInputStream()) {
                        Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    if (expectedSize > 0 && tempFile.length() != expectedSize) {
                        plugin.getLogger().warning("Tamanho incorreto na tentativa " + attempt + ". Esperado: " + expectedSize + ", Baixado: " + tempFile.length());
                        continue;
                    }

                    // Verify JAR version
                    if (!verifyJarVersion(tempFile, version)) {
                        plugin.getLogger().warning("Abortando atualização: A versão no MANIFEST.MF do arquivo baixado não corresponde à versão esperada (" + version + ").");
                        tempFile.delete();
                        return; 
                    }

                    success = true;
                    break;

                } catch (Exception e) {
                    plugin.getLogger().warning("Erro na tentativa " + attempt + ": " + e.getMessage());
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            }

            if (!success) {
                tempFile.delete();
                plugin.getLogger().severe("Falha no download após " + maxRetries + " tentativas.");
                return;
            }

            String fileHash = calculateFileHash(tempFile);
            plugin.getLogger().info("SHA-256 do arquivo baixado: " + fileHash);

            saveUpdateAttempt(version);
            
            // Spigot Update Mechanism: Move to plugins/update/
            File updateFolder = new File(plugin.getDataFolder().getParentFile(), "update");
            if (!updateFolder.exists()) updateFolder.mkdirs();
            
            File updateFile = new File(updateFolder, "MidgardMaintenance.jar");
            
            try {
                Files.move(tempFile.toPath(), updateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("ATUALIZAÇÃO CONCLUÍDA COM SUCESSO (" + version + ")");
                plugin.getLogger().info("O arquivo foi salvo em 'plugins/update/'. Reinicie o servidor para aplicar.");
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao mover arquivo para pasta de update: " + e.getMessage());
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar: " + e.getMessage(), e);
        }
    }

    private String calculateFileHash(File file) {
        try (InputStream fis = Files.newInputStream(file.toPath())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            plugin.getLogger().warning("Não foi possível calcular o hash do arquivo: " + e.getMessage());
            return "Unknown";
        }
    }

    private boolean isNewer(String latest, String current) {
        if (current.equals("Unknown")) return true;
        try {
            String[] latParts = latest.split("\\.");
            String[] curParts = current.split("\\.");
            int length = Math.max(latParts.length, curParts.length);
            for (int i = 0; i < length; i++) {
                int latVer = i < latParts.length ? Integer.parseInt(latParts[i]) : 0;
                int curVer = i < curParts.length ? Integer.parseInt(curParts[i]) : 0;
                if (latVer > curVer) return true;
                if (latVer < curVer) return false;
            }
            return false;
        } catch (NumberFormatException e) {
            return !latest.equalsIgnoreCase(current);
        }
    }

    private boolean verifyJarVersion(File file, String expectedVersion) {
        try (JarFile jar = new JarFile(file)) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) return false;
            
            Attributes attr = manifest.getMainAttributes();
            String version = attr.getValue("Implementation-Version");
            
            if (version == null) return false;
            
            String cleanVersion = version.replace("v", "").trim();
            String cleanExpected = expectedVersion.replace("v", "").trim();
            
            return cleanVersion.equals(cleanExpected);
        } catch (Exception e) {
            return false;
        }
    }

    private void saveUpdateAttempt(String version) {
        try (FileWriter writer = new FileWriter(new File(plugin.getDataFolder(), STATUS_FILE))) {
            JsonObject json = new JsonObject();
            json.addProperty("targetVersion", version);
            json.addProperty("timestamp", System.currentTimeMillis());
            writer.write(json.toString());
        } catch (IOException e) {
            plugin.getLogger().warning("Não foi possível salvar o status da atualização: " + e.getMessage());
        }
    }

    private boolean isLoopDetected(String targetVersion) {
        File statusFile = new File(plugin.getDataFolder(), STATUS_FILE);
        if (!statusFile.exists()) return false;

        try (FileReader reader = new FileReader(statusFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String lastTarget = json.has("targetVersion") ? json.get("targetVersion").getAsString() : "";
            long timestamp = json.has("timestamp") ? json.get("timestamp").getAsLong() : 0;

            if (lastTarget.equals(targetVersion) && (System.currentTimeMillis() - timestamp) < 15 * 60 * 1000) {
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao ler status da atualização: " + e.getMessage());
        }
        return false;
    }
}

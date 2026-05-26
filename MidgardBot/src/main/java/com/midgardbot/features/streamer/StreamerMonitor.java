package com.midgardbot.features.streamer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.midgardbot.config.BotConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StreamerMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamerMonitor.class);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final OkHttpClient client = new OkHttpClient();
    private static JDA jda;
    
    private static String twitchAccessToken;
    private static long twitchTokenExpiry;

    public static void start(JDA jdaInstance) {
        jda = jdaInstance;
        scheduler.scheduleAtFixedRate(StreamerMonitor::checkStreamers, 0, 2, TimeUnit.MINUTES);
        LOGGER.info("StreamerMonitor iniciado.");
    }

    private static void checkStreamers() {
        String channelId = BotConfig.getStreamAnnounceChannelId();
        if (channelId == null || channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            LOGGER.warn("Canal de anúncios de stream não encontrado: " + channelId);
            return;
        }

        List<Streamer> streamers = StreamerManager.getStreamers();
        for (Streamer streamer : streamers) {
            try {
                boolean isLive = false;
                String title = "";
                String game = "";
                String thumbnail = "";
                String url = "";

                switch (streamer.getPlatform().toLowerCase()) {
                    case "twitch":
                        StreamInfo twitchInfo = checkTwitch(streamer.getChannelName());
                        if (twitchInfo != null) {
                            isLive = true;
                            title = twitchInfo.title;
                            game = twitchInfo.game;
                            thumbnail = twitchInfo.thumbnail;
                            url = "https://twitch.tv/" + streamer.getChannelName();
                        }
                        break;
                    case "youtube":
                        StreamInfo youtubeInfo = checkYoutube(streamer.getChannelName()); // Expects Channel ID
                        if (youtubeInfo != null) {
                            isLive = true;
                            title = youtubeInfo.title;
                            url = "https://youtube.com/channel/" + streamer.getChannelName();
                            thumbnail = youtubeInfo.thumbnail;
                        }
                        break;
                    case "kick":
                        StreamInfo kickInfo = checkKick(streamer.getChannelName());
                        if (kickInfo != null) {
                            isLive = true;
                            title = kickInfo.title;
                            game = kickInfo.game;
                            thumbnail = kickInfo.thumbnail;
                            url = "https://kick.com/" + streamer.getChannelName();
                        }
                        break;
                }

                if (isLive && !streamer.isLastStatus()) {
                    // Went live
                    announceStream(channel, streamer, title, game, thumbnail, url);
                    StreamerManager.updateStatus(streamer.getId(), true);
                } else if (!isLive && streamer.isLastStatus()) {
                    // Went offline
                    StreamerManager.updateStatus(streamer.getId(), false);
                }

            } catch (Exception e) {
                LOGGER.error("Erro ao verificar streamer " + streamer.getChannelName(), e);
            }
        }
    }

    private static void announceStream(TextChannel channel, Streamer streamer, String title, String game, String thumbnail, String url) {
        String roleId = BotConfig.getStreamNotificationRoleId();
        String mention = (roleId != null && !roleId.isEmpty()) ? "<@&" + roleId + ">" : "@everyone";
        
        String msg = mention + " " + streamer.getChannelName() + " está ao vivo na " + streamer.getPlatform() + "!";
        channel.sendMessage(msg)
            .setEmbeds(EmbedUtils.createEmbed(
                "🔴 Ao Vivo: " + streamer.getChannelName(),
                "**" + title + "**\n\n" + (game.isEmpty() ? "" : "Jogando: " + game + "\n") + "[Clique para assistir](" + url + ")",
                Color.RED
            ).setImage(thumbnail.replace("{width}", "1280").replace("{height}", "720")).build())
            .queue();
    }

    private static class StreamInfo {
        String title;
        String game;
        String thumbnail;
    }

    private static StreamInfo checkTwitch(String userLogin) {
        ensureTwitchToken();
        if (twitchAccessToken == null) {
            LOGGER.warn("Twitch Access Token é nulo. Verifique as credenciais.");
            return null;
        }

        Request request = new Request.Builder()
            .url("https://api.twitch.tv/helix/streams?user_login=" + userLogin)
            .addHeader("Client-ID", BotConfig.getTwitchClientId())
            .addHeader("Authorization", "Bearer " + twitchAccessToken)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOGGER.warn("Erro na API da Twitch: " + response.code() + " - " + response.body().string());
                // Se for erro de autenticação (401) ou requisição inválida (400 - Token inválido), força renovação
                if (response.code() == 401 || response.code() == 400) {
                     twitchAccessToken = null;
                     LOGGER.info("Token Twitch invalidado. Forçando renovação na próxima execução.");
                }
                return null;
            }
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            var data = json.getAsJsonArray("data");
            if (data.size() > 0) {
                JsonObject stream = data.get(0).getAsJsonObject();
                StreamInfo info = new StreamInfo();
                info.title = stream.get("title").getAsString();
                info.game = stream.get("game_name").getAsString();
                info.thumbnail = stream.get("thumbnail_url").getAsString();
                return info;
            }
        } catch (Exception e) {
            LOGGER.error("Erro Twitch API", e);
        }
        return null;
    }

    private static void ensureTwitchToken() {
        if (twitchAccessToken != null && System.currentTimeMillis() < twitchTokenExpiry) return;
        
        String clientId = BotConfig.getTwitchClientId();
        String clientSecret = BotConfig.getTwitchClientSecret();
        if (clientId == null || clientSecret == null) {
            LOGGER.warn("Twitch Client ID ou Secret não configurados.");
            return;
        }

        RequestBody body = RequestBody.create(
            "client_id=" + clientId + "&client_secret=" + clientSecret + "&grant_type=client_credentials",
            MediaType.parse("application/x-www-form-urlencoded")
        );

        Request request = new Request.Builder()
            .url("https://id.twitch.tv/oauth2/token")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
                twitchAccessToken = json.get("access_token").getAsString();
                twitchTokenExpiry = System.currentTimeMillis() + (json.get("expires_in").getAsLong() * 1000) - 60000;
                LOGGER.info("Token da Twitch atualizado com sucesso.");
            } else {
                LOGGER.error("Falha ao obter token da Twitch: " + response.code() + " - " + response.body().string());
            }
        } catch (Exception e) {
            LOGGER.error("Erro Twitch Auth", e);
        }
    }

    private static StreamInfo checkYoutube(String channelId) {
        String apiKey = BotConfig.getYoutubeApiKey();
        if (apiKey == null) return null;

        Request request = new Request.Builder()
            .url("https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=" + channelId + "&eventType=live&type=video&key=" + apiKey)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            var items = json.getAsJsonArray("items");
            if (items.size() > 0) {
                JsonObject video = items.get(0).getAsJsonObject();
                JsonObject snippet = video.getAsJsonObject("snippet");
                StreamInfo info = new StreamInfo();
                info.title = snippet.get("title").getAsString();
                info.game = "";
                info.thumbnail = snippet.getAsJsonObject("thumbnails").getAsJsonObject("high").get("url").getAsString();
                return info;
            }
        } catch (Exception e) {
            LOGGER.error("Erro YouTube API", e);
        }
        return null;
    }

    private static StreamInfo checkKick(String slug) {
        Request request = new Request.Builder()
            .url("https://kick.com/api/v1/channels/" + slug)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            if (!json.get("livestream").isJsonNull()) {
                JsonObject livestream = json.getAsJsonObject("livestream");
                StreamInfo info = new StreamInfo();
                info.title = livestream.get("session_title").getAsString();
                
                // Categories is an array in Kick API
                if (livestream.has("categories") && livestream.get("categories").isJsonArray()) {
                    var categories = livestream.getAsJsonArray("categories");
                    if (categories.size() > 0) {
                        info.game = categories.get(0).getAsJsonObject().get("name").getAsString();
                    } else {
                        info.game = "Variedades";
                    }
                } else {
                    info.game = "Variedades";
                }

                if (livestream.has("thumbnail") && !livestream.get("thumbnail").isJsonNull()) {
                    JsonObject thumb = livestream.getAsJsonObject("thumbnail");
                    if (thumb.has("url")) {
                        info.thumbnail = thumb.get("url").getAsString();
                    } else if (thumb.has("src")) {
                        info.thumbnail = thumb.get("src").getAsString();
                    } else {
                        info.thumbnail = "";
                    }
                } else {
                    info.thumbnail = ""; // Fallback or default image
                }
                
                return info;
            }
        } catch (Exception e) {
            LOGGER.error("Erro Kick API", e);
        }
        return null;
    }
}

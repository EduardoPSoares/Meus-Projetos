package me.ray.midgard.bot;

public class BotConfig {

    private final String token;
    private String ownerId = "";
    private long devGuildId = 0;
    private String prefix = "!";

    public BotConfig(String token) {
        this.token = token;
    }

    public String getToken() { return token; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public long getDevGuildId() { return devGuildId; }
    public void setDevGuildId(long devGuildId) { this.devGuildId = devGuildId; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
}

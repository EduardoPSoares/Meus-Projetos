package com.midgardbot.features.streamer;

public class Streamer {
    private int id;
    private String userId;
    private String platform;
    private String channelName;
    private boolean lastStatus;
    private long lastCheck;

    public Streamer(int id, String userId, String platform, String channelName, boolean lastStatus, long lastCheck) {
        this.id = id;
        this.userId = userId;
        this.platform = platform;
        this.channelName = channelName;
        this.lastStatus = lastStatus;
        this.lastCheck = lastCheck;
    }

    public int getId() { return id; }
    public String getUserId() { return userId; }
    public String getPlatform() { return platform; }
    public String getChannelName() { return channelName; }
    public boolean isLastStatus() { return lastStatus; }
    public long getLastCheck() { return lastCheck; }

    public void setLastStatus(boolean lastStatus) { this.lastStatus = lastStatus; }
    public void setLastCheck(long lastCheck) { this.lastCheck = lastCheck; }
}

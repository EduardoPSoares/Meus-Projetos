package com.midgardbot.data;

public class MaintenanceInfo {
    public boolean enabled;
    public long timestamp;
    public String user;

    public MaintenanceInfo(boolean enabled, String user) {
        this.enabled = enabled;
        this.user = user;
        this.timestamp = System.currentTimeMillis();
    }
}

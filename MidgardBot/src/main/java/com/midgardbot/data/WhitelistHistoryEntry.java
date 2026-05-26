package com.midgardbot.data;

public class WhitelistHistoryEntry {
    public String userId;
    public String staffId;
    public String staffName;
    public String action;
    public String details;
    public long timestamp;

    public WhitelistHistoryEntry(String userId, String staffId, String staffName, String action, String details) {
        this.userId = userId;
        this.staffId = staffId;
        this.staffName = staffName;
        this.action = action;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
    }
}

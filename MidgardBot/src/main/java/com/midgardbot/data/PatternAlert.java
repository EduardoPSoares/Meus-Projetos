package com.midgardbot.data;

public class PatternAlert {
    public String userId;
    public String type;
    public String severity;
    public String message;
    public String relatedUserId;
    public long timestamp;

    public PatternAlert(String userId, String type, String severity, String message, String relatedUserId) {
        this.userId = userId;
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.relatedUserId = relatedUserId;
        this.timestamp = System.currentTimeMillis();
    }
}

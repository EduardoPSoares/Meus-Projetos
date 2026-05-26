package com.midgardbot.data;

public class StaffFeedback {
    public String staffId;
    public String userId;
    public int rating;
    public String comment;
    public long timestamp;

    public StaffFeedback(String staffId, String userId, int rating, String comment) {
        this.staffId = staffId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = System.currentTimeMillis();
    }
}

package com.midgardbot.data;

public class StaffStats {
    public int approved = 0;
    public int rejected = 0;
    public int claimed = 0;
    public long totalReviewTime = 0;
    public int ticketsClosed = 0;
    public int ticketsClaimed = 0;

    public int getTotal() {
        return approved + rejected;
    }

    public double getAverageReviewTime() {
        int total = getTotal();
        return total > 0 ? (double) totalReviewTime / total / 1000.0 : 0;
    }

    public double getApprovalRate() {
        int total = getTotal();
        return total > 0 ? (double) approved / total * 100.0 : 0;
    }
}

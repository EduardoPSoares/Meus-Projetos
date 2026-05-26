package me.ray.midgard.bot.modules.whitelist;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class WhitelistApplication {

    public enum Status {
        IN_PROGRESS,
        PENDING,
        APPROVED,
        REJECTED
    }

    private final String odlUserId;
    private String odlUsername;
    private Status status;
    private int currentPart;
    private final Map<String, String> answers;
    private Instant createdAt;
    private Instant updatedAt;
    private String reviewedBy;
    private String reviewNote;
    private boolean forced;

    public WhitelistApplication(String userId) {
        this.odlUserId = userId;
        this.status = Status.IN_PROGRESS;
        this.currentPart = 0;
        this.answers = new LinkedHashMap<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public WhitelistApplication(String userId, String username, Status status, int currentPart,
                                 Map<String, String> answers, Instant createdAt, Instant updatedAt,
                                 String reviewedBy, String reviewNote, boolean forced) {
        this.odlUserId = userId;
        this.odlUsername = username;
        this.status = status;
        this.currentPart = currentPart;
        this.answers = answers != null ? new LinkedHashMap<>(answers) : new LinkedHashMap<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.reviewedBy = reviewedBy;
        this.reviewNote = reviewNote;
        this.forced = forced;
    }

    // ==================== Answer Management ====================

    public void setAnswer(String questionId, String answer) {
        answers.put(questionId, answer);
        this.updatedAt = Instant.now();
    }

    public void setAnswers(Map<String, String> newAnswers) {
        answers.putAll(newAnswers);
        this.updatedAt = Instant.now();
    }

    public String getAnswer(String questionId) {
        return answers.get(questionId);
    }

    public boolean hasAnswer(String questionId) {
        return answers.containsKey(questionId);
    }

    // ==================== Part Management ====================

    public void advancePart() {
        this.currentPart++;
        this.updatedAt = Instant.now();
    }

    public void completeParts(int totalParts) {
        if (this.currentPart >= totalParts - 1) {
            this.status = Status.PENDING;
        }
        advancePart();
    }

    // ==================== Review ====================

    public void approve(String reviewerId, String note) {
        this.status = Status.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewNote = note;
        this.updatedAt = Instant.now();
    }

    public void reject(String reviewerId, String note) {
        this.status = Status.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewNote = note;
        this.updatedAt = Instant.now();
    }

    // ==================== Accessors ====================

    public String getUserId() { return odlUserId; }
    public String getUsername() { return odlUsername; }
    public void setUsername(String username) { this.odlUsername = username; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; this.updatedAt = Instant.now(); }
    public int getCurrentPart() { return currentPart; }
    public void setCurrentPart(int currentPart) { this.currentPart = currentPart; }
    public Map<String, String> getAnswers() { return answers; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public boolean isForced() { return forced; }
    public void setForced(boolean forced) { this.forced = forced; }
}

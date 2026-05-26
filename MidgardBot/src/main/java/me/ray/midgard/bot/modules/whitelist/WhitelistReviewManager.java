package me.ray.midgard.bot.modules.whitelist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the review queue so multiple staff members can analyze
 * whitelist applications simultaneously without conflicts.
 * Each staff gets a unique application from the queue.
 */
public class WhitelistReviewManager {

    private static final Logger logger = LoggerFactory.getLogger(WhitelistReviewManager.class);

    private final WhitelistRepository repository;

    // staffId -> applicationUserId (which app the staff is currently reviewing)
    private final Map<String, String> activeReviews = new ConcurrentHashMap<>();

    // Panel message tracking: channelId -> messageId
    private String panelChannelId;
    private String panelMessageId;

    public WhitelistReviewManager(WhitelistRepository repository) {
        this.repository = repository;
    }

    /**
     * Assigns the next pending application to a staff member.
     * Skips applications already being reviewed by other staff.
     * Returns null if no pending applications are available.
     */
    public synchronized WhitelistApplication claimNext(String staffId) {
        // If staff already has an active review, return that
        String existingClaim = activeReviews.get(staffId);
        if (existingClaim != null) {
            var app = repository.findById(existingClaim);
            if (app.isPresent() && app.get().getStatus() == WhitelistApplication.Status.PENDING) {
                return app.get();
            }
            // Old claim is stale, remove it
            activeReviews.remove(staffId);
        }

        // Get all claimed application IDs
        Set<String> claimedIds = Set.copyOf(activeReviews.values());

        // Find next pending application not currently being reviewed
        var pending = repository.findPendingOrderedByDate();
        for (WhitelistApplication app : pending) {
            if (!claimedIds.contains(app.getUserId())) {
                activeReviews.put(staffId, app.getUserId());
                logger.info("Staff {} claimed application of user {}", staffId, app.getUserId());
                return app;
            }
        }

        return null; // No available applications
    }

    /**
     * Releases the current review assignment for a staff member.
     */
    public void release(String staffId) {
        String removed = activeReviews.remove(staffId);
        if (removed != null) {
            logger.info("Staff {} released review of user {}", staffId, removed);
        }
    }

    /**
     * Completes a review (approve/reject) and releases the assignment.
     */
    public void complete(String staffId) {
        release(staffId);
    }

    /**
     * Returns the application ID currently being reviewed by a staff member.
     */
    public String getActiveReview(String staffId) {
        return activeReviews.get(staffId);
    }

    /**
     * Checks if a staff member is currently reviewing an application.
     */
    public boolean isReviewing(String staffId) {
        return activeReviews.containsKey(staffId);
    }

    /**
     * Returns a set of staff IDs currently in review mode.
     */
    public Set<String> getActiveStaff() {
        return Set.copyOf(activeReviews.keySet());
    }

    /**
     * Returns the number of staff members actively reviewing.
     */
    public int getActiveStaffCount() {
        return activeReviews.size();
    }

    // ==================== Panel Tracking ====================

    public void setPanelMessage(String channelId, String messageId) {
        this.panelChannelId = channelId;
        this.panelMessageId = messageId;
    }

    public String getPanelChannelId() { return panelChannelId; }
    public String getPanelMessageId() { return panelMessageId; }
    public boolean hasPanelMessage() { return panelChannelId != null && panelMessageId != null; }
}

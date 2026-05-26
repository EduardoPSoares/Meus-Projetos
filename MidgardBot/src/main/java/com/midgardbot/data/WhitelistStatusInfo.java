package com.midgardbot.data;

import java.time.LocalDate;

public class WhitelistStatusInfo {
    public WhitelistStatus status;
    public String timestamp;
    public String reason;
    public String nickname;
    public String answers;
    public boolean termsAccepted;
    public String staffId;

    public WhitelistStatusInfo(WhitelistStatus status, String reason, String nickname, String answers, boolean termsAccepted, String staffId) {
        this.status = status;
        this.timestamp = LocalDate.now().toString();
        this.reason = reason;
        this.nickname = nickname;
        this.answers = answers;
        this.termsAccepted = termsAccepted;
        this.staffId = staffId;
    }

    public WhitelistStatusInfo(WhitelistStatus status, String reason, String nickname, String answers, boolean termsAccepted) {
        this(status, reason, nickname, answers, termsAccepted, null);
    }

    public WhitelistStatusInfo(WhitelistStatus status, String reason, String nickname, String answers) {
        this(status, reason, nickname, answers, false, null);
    }

    public WhitelistStatusInfo(WhitelistStatus status, String reason, String nickname) {
        this(status, reason, nickname, null, false);
    }
}

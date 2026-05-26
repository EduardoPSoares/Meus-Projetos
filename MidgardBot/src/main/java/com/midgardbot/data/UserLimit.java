package com.midgardbot.data;

public class UserLimit {
    public int attempts;
    public long resetTime;

    public UserLimit(int attempts, long resetTime) {
        this.attempts = attempts;
        this.resetTime = resetTime;
    }
}

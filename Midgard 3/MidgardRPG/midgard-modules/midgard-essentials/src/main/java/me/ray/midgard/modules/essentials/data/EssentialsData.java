package me.ray.midgard.modules.essentials.data;

import me.ray.midgard.core.profile.ModuleData;

public class EssentialsData implements ModuleData {

    private boolean vanished;
    private long lastLogin;
    private String lastKnownName;

    public EssentialsData() {
        this.vanished = false;
        this.lastLogin = System.currentTimeMillis();
        this.lastKnownName = "";
    }

    public boolean isVanished() {
        return vanished;
    }

    public void setVanished(boolean vanished) {
        this.vanished = vanished;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }
}

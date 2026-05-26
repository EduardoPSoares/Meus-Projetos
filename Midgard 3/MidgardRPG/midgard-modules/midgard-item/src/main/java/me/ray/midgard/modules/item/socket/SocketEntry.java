package me.ray.midgard.modules.item.socket;

public class SocketEntry {
    private final String type;
    private String gemId;

    public SocketEntry(String type, String gemId) {
        this.type = type;
        this.gemId = gemId;
    }

    public String getType() {
        return type;
    }

    public String getGemId() {
        return gemId;
    }

    public void setGemId(String gemId) {
        this.gemId = gemId;
    }

    public boolean isEmpty() {
        return gemId == null;
    }
}

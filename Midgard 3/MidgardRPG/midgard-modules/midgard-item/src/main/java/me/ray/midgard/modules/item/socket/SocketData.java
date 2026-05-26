package me.ray.midgard.modules.item.socket;

import me.ray.midgard.modules.item.utils.ItemPDC;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SocketData {

    private static final String KEY = "midgard_sockets_data";
    private final List<SocketEntry> sockets;

    public SocketData(List<String> socketTypes) {
        this.sockets = new ArrayList<>();
        for (String type : socketTypes) {
            sockets.add(new SocketEntry(type, null));
        }
    }

    public SocketData(List<SocketEntry> sockets, boolean dummy) {
        this.sockets = sockets;
    }

    public List<SocketEntry> getSockets() {
        return sockets;
    }

    public boolean hasEmptySocket(String type) {
        for (SocketEntry entry : sockets) {
            if (entry.getType().equalsIgnoreCase(type) && entry.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasAnyEmptySocket() {
        for (SocketEntry entry : sockets) {
            if (entry.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean applyGem(String type, String gemId) {
        // First try exact type match
        for (SocketEntry entry : sockets) {
            if (entry.getType().equalsIgnoreCase(type) && entry.isEmpty()) {
                entry.setGemId(gemId);
                return true;
            }
        }
        // Fallback: try universal "ANY" socket
        for (SocketEntry entry : sockets) {
            if (entry.getType().equalsIgnoreCase("ANY") && entry.isEmpty()) {
                entry.setGemId(gemId);
                return true;
            }
        }
        return false;
    }

    public static SocketData fromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return new SocketData(new ArrayList<>());
        }
        String data = ItemPDC.getString(item.getItemMeta(), KEY);
        if (data == null || data.isEmpty()) {
            return new SocketData(new ArrayList<>());
        }
        
        List<SocketEntry> entries = new ArrayList<>();
        String[] parts = data.split(";");
        for (String part : parts) {
            String[] split = part.split(":");
            if (split.length >= 1) {
                String type = split[0];
                String gemId = split.length > 1 && !split[1].equals("null") && !split[1].isEmpty() ? split[1] : null;
                entries.add(new SocketEntry(type, gemId));
            }
        }
        return new SocketData(entries, true);
    }

    public void save(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        String data = sockets.stream()
                .map(e -> e.getType() + ":" + (e.getGemId() == null ? "null" : e.getGemId()))
                .collect(Collectors.joining(";"));
        ItemPDC.setString(meta, KEY, data);
        item.setItemMeta(meta);
    }
}

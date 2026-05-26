package me.ray.midgard.core.loot;

import java.util.ArrayList;
import java.util.List;

public class LootTable {

    private final String id;
    private final List<LootEntry> entries = new ArrayList<>();
    private int minRolls = 1;
    private int maxRolls = 1;

    public LootTable(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void addEntry(LootEntry entry) {
        entries.add(entry);
    }

    public List<LootEntry> getEntries() {
        return entries;
    }

    public int getMinRolls() {
        return minRolls;
    }

    public void setMinRolls(int minRolls) {
        this.minRolls = minRolls;
    }

    public int getMaxRolls() {
        return maxRolls;
    }

    public void setMaxRolls(int maxRolls) {
        this.maxRolls = maxRolls;
    }
}

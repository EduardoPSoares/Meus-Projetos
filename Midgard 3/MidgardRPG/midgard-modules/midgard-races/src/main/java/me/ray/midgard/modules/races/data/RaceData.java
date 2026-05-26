package me.ray.midgard.modules.races.data;

import me.ray.midgard.core.profile.ModuleData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RaceData implements ModuleData {

    private String raceId;
    private int level;
    private double experience;
    private long lastRaceChange;
    private Set<String> unlockedMutations;
    private List<String> raceHistory;
    private int totalKills;
    private Map<String, Integer> killsByType;
    
    public RaceData() {
        this.raceId = null;
        this.level = 1;
        this.experience = 0;
        this.lastRaceChange = 0;
        this.unlockedMutations = ConcurrentHashMap.newKeySet();
        this.raceHistory = new ArrayList<>();
        this.totalKills = 0;
        this.killsByType = new ConcurrentHashMap<>();
    }

    public String getRaceId() {
        return raceId;
    }

    public void setRaceId(String raceId) {
        this.raceId = raceId;
    }

    public boolean hasRace() {
        return raceId != null && !raceId.isEmpty();
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getExperience() {
        return experience;
    }

    public void setExperience(double experience) {
        this.experience = experience;
    }
    
    public void addExperience(double amount) {
        this.experience += amount;
    }

    public long getLastRaceChange() {
        return lastRaceChange;
    }

    public void setLastRaceChange(long lastRaceChange) {
        this.lastRaceChange = lastRaceChange;
    }

    public Set<String> getUnlockedMutations() {
        if (unlockedMutations == null) { unlockedMutations = ConcurrentHashMap.newKeySet(); }
        return unlockedMutations;
    }

    public void setUnlockedMutations(Set<String> unlockedMutations) {
        this.unlockedMutations = unlockedMutations;
    }
    
    public void unlockMutation(String mutationId) {
        getUnlockedMutations().add(mutationId);
    }
    
    public boolean hasMutation(String mutationId) {
        return getUnlockedMutations().contains(mutationId);
    }

    // ─── Histórico de Raças ──────────────────────────────────────────

    public List<String> getRaceHistory() {
        if (raceHistory == null) { raceHistory = new ArrayList<>(); }
        return raceHistory;
    }

    public void pushRaceHistory(String raceId) {
        if (raceId == null || raceId.isEmpty()) { return; }
        getRaceHistory().add(raceId);
    }

    public String getPreviousRaceId() {
        var history = getRaceHistory();
        if (history.isEmpty()) { return null; }
        return history.getLast();
    }

    public String popRaceHistory() {
        var history = getRaceHistory();
        if (history.isEmpty()) { return null; }
        return history.removeLast();
    }

    // ─── Estatísticas de Kills ───────────────────────────────────────

    public int getTotalKills() {
        return totalKills;
    }

    public void setTotalKills(int totalKills) {
        this.totalKills = totalKills;
    }

    public Map<String, Integer> getKillsByType() {
        if (killsByType == null) { killsByType = new ConcurrentHashMap<>(); }
        return killsByType;
    }

    public int getKillsOf(String type) {
        if (type == null) { return 0; }
        return getKillsByType().getOrDefault(type.toUpperCase(), 0);
    }

    public void addKill(String entityType) {
        this.totalKills++;
        if (entityType != null) {
            String key = entityType.toUpperCase();
            getKillsByType().merge(key, 1, Integer::sum);
        }
    }
}

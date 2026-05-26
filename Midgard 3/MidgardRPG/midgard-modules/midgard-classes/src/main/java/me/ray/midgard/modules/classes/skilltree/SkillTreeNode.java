package me.ray.midgard.modules.classes.skilltree;

import java.util.List;
import java.util.Map;
import java.util.Collections;

public class SkillTreeNode {

    private final String id;
    private final String name;
    private final Map<Integer, List<String>> lorePerLevel;
    private final int x;
    private final int y;
    
    // Logic
    private final Map<String, Integer> parents;
    
    private boolean root = false;
    private int maxLevel = 1;

    public SkillTreeNode(String id, String name, Map<Integer, List<String>> lorePerLevel, int maxLevel, int x, int y, boolean root, Map<String, Integer> parents) {
        this.id = id;
        this.name = name;
        this.lorePerLevel = lorePerLevel;
        this.maxLevel = maxLevel;
        this.x = x;
        this.y = y;
        this.root = root;
        this.parents = parents;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    
    public List<String> getLore(int level) {
         return lorePerLevel.getOrDefault(level, Collections.emptyList());
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    
    public boolean isRoot() {
        return root;
    }
    
    public int getMaxLevel() {
        return maxLevel;
    }
    
    public Map<String, Integer> getParents() {
        return parents;
    }
    
    // Alias for compatibility
    public String getDisplayName() {
        return name;
    }
}

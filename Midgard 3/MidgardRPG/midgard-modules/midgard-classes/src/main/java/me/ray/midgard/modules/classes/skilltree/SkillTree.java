package me.ray.midgard.modules.classes.skilltree;

import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillTree {

    private final String id;
    private final String name;
    private final List<String> lore;
    private final ItemStack icon;
    private final int maxPoints;
    private final Map<String, SkillTreeNode> nodes;

    public SkillTree(String id, String name, List<String> lore, ItemStack icon, int maxPoints) {
        this.id = id;
        this.name = name;
        this.lore = lore;
        this.icon = icon;
        this.maxPoints = maxPoints;
        this.nodes = new HashMap<>(); // Store by Node ID
    }

    public void addNode(SkillTreeNode node) {
        nodes.put(node.getId(), node);
    }

    public List<String> getLore() {
        return lore;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public SkillTreeNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }
    
    public Map<String, SkillTreeNode> getNodes() {
        return nodes;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ItemStack getIcon() {
        return icon;
    }
}

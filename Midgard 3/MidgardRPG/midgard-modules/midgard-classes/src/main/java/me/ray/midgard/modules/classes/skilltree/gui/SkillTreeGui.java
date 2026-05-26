package me.ray.midgard.modules.classes.skilltree.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.modules.classes.ClassesModule;
import me.ray.midgard.modules.classes.skilltree.SkillTree;
import me.ray.midgard.modules.classes.skilltree.SkillTreeNode;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.core.text.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.ray.midgard.modules.classes.ClassData;
import me.ray.midgard.core.skill.SkillProvider;
import me.ray.midgard.core.profile.MidgardProfile;

public class SkillTreeGui extends BaseGui {

    private final SkillTree skillTree;
    private final ClassesModule module;
    private int offsetX = 0;
    private int offsetY = 0;

    private static final int CENTER_X = 4;
    private static final int CENTER_Y = 2; // Middle row roughly

    public SkillTreeGui(Player player, SkillTree skillTree) {
        super(player, 6, buildTitle(skillTree));
        this.skillTree = skillTree;
        this.module = ClassesModule.getInstance();
    }

    private static String buildTitle(SkillTree skillTree) {
        ClassesModule mod = ClassesModule.getInstance();
        if (mod == null) {
            return "Árvore: " + (skillTree != null ? skillTree.getName() : "Inválida");
        }
        String name = skillTree != null ? skillTree.getName() : mod.getMessage("gui.skill_tree_invalid");
        return mod.getMessage("gui.skill_tree_title").replace("%name%", name);
    }

    @Override
    public void initializeItems() {
        if (skillTree == null) {
            return;
        }

        // Clear only content slots, keep borders if any (implement border logic later)
        inventory.clear();

        // 1. Render Nodes based on Offset
        for (SkillTreeNode node : skillTree.getNodes().values()) {
            int relX = node.getX() - offsetX;
            int relY = node.getY() - offsetY; // Inverted Y in Minecraft GUI? Usually +Y is down in GUIs, but Cartesian is up.
                                              // MMOCore: +Y is UP. GUI Row 0 is Top.
                                              // So GUI Row = CenterY - relY
            
            int guiX = CENTER_X + relX;
            int guiY = CENTER_Y - relY;

            if (guiX >= 0 && guiX < 9 && guiY >= 0 && guiY < 5) { // Leave bottom row for controls
                int slot = guiY * 9 + guiX;
                ItemStack icon = createNodeIcon(node);
                inventory.setItem(slot, icon);
            }
        }

        // 2. Render Navigation Controls (Bottom Row)
        // 45 46 47 48 49 50 51 52 53
        
        // Up (Y+) -> Move View Up -> OffsetY increases
        inventory.setItem(50, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(module.getMessage("gui.nav_up"))
                .texture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTk5YWFmMjQ1NmE2MTIyZGU4ZjZiNjI2ODNmMmJjMmVlZDlhYmI4MWZkNWJlYTFiNGMyM2E1ODE1NmI2NjkifX19")
                .build());

        // Down (Y-) -> Move View Down -> OffsetY decreases
        inventory.setItem(48, new ItemBuilder(Material.PLAYER_HEAD) // Was 49 in reference, spread them out for D-Pad feel maybe? Reference has 49.
                .setName(module.getMessage("gui.nav_down"))
                .texture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzkxMmQ0NWIxYzc4Y2MyMjQ1MjcyM2VlNjZiYTJkMTU3NzdjYzI4ODU2OGQ2YzFiNjJhNTQ1YjI5YzcxODcifX19")
                .build());

        // Left (X-) -> Move View Left -> OffsetX decreases
        inventory.setItem(47, new ItemBuilder(Material.PLAYER_HEAD) // Reference 48
                 .setName(module.getMessage("gui.nav_left"))
                 .texture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTNmYzUyMjY0ZDhhZDllNjU0ZjQxNWJlZjAxYTIzOTQ3ZWRiY2NjY2Y2NDkzNzMyODliZWE0ZDE0OTU0MWY3MCJ9fX0=")
                 .build());

        // Right (X+) -> Move View Right -> OffsetX increases
        inventory.setItem(51, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(module.getMessage("gui.nav_right"))
                .texture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWYxMzNlOTE5MTlkYjBhY2VmZGMyNzJkNjdmZDg3YjRiZTg4ZGM0NGE5NTg5NTg4MjQ0NzRlMjFlMDZkNTNlNiJ9fX0=")
                .build());
        
        // Back Button
        inventory.setItem(45, new ItemBuilder(Material.ARROW).setName(module.getMessage("gui.nav_back")).build());
    }

    private ItemStack createNodeIcon(SkillTreeNode node) {
        // Read actual player skill level from SkillProvider
        SkillProvider skillProvider = MidgardCore.getSkillProvider();
        MidgardProfile profile = MidgardCore.getProfileManager() != null ? MidgardCore.getProfileManager().getProfile(player) : null;
        int playerLevel = 0;
        if (skillProvider != null && profile != null) {
            playerLevel = skillProvider.getSkillLevel(profile, node.getId());
        }
        
        // Visual state based on level
        Material mat;
        if (playerLevel >= node.getMaxLevel()) {
            mat = Material.ORANGE_STAINED_GLASS_PANE; // Maxed
        } else if (playerLevel > 0) {
            mat = Material.LIME_STAINED_GLASS_PANE; // Partially invested
        } else {
            mat = Material.GRAY_STAINED_GLASS_PANE; // Locked/not invested
        }
        if (node.isRoot()) {
            mat = Material.BEACON;
        }
        
        ItemBuilder builder = new ItemBuilder(mat);
        
        builder.setName("<green>" + node.getDisplayName());
        
        List<String> lore = new ArrayList<>();
        lore.addAll(node.getLore(playerLevel));
        lore.add("");
        lore.add(module.getMessage("gui.node_level").replace("%current%", String.valueOf(playerLevel)).replace("%max%", String.valueOf(node.getMaxLevel())));
        lore.add(module.getMessage("gui.node_position").replace("%x%", String.valueOf(node.getX())).replace("%y%", String.valueOf(node.getY())));
        
        builder.lore(lore);
        
        return builder.build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return;
        }
        
        int slot = event.getRawSlot();
        
        // Navigation
        if (slot == 50) { offsetY++; initializeItems(); return; } // Up
        if (slot == 48) { offsetY--; initializeItems(); return; } // Down
        if (slot == 47) { offsetX--; initializeItems(); return; } // Left
        if (slot == 51) { offsetX++; initializeItems(); return; } // Right
        if (slot == 45) { /* Close or Back */ player.closeInventory(); return; }

        // Node Interaction
        // Reverse calculation: Slot -> Node
        int guiY = slot / 9;
        int guiX = slot % 9;
        
        int relX = guiX - CENTER_X;
        int relY = CENTER_Y - guiY;
        
        int paramX = relX + offsetX;
        int paramY = relY + offsetY;
        
        // Find node at (paramX, paramY)
        SkillTreeNode clickedNode = null;
        for (SkillTreeNode node : skillTree.getNodes().values()) {
            if (node.getX() == paramX && node.getY() == paramY) {
                clickedNode = node;
                break;
            }
        }
        
        if (clickedNode != null) {
            handleNodeClick(clickedNode);
        }
    }
    
    private void handleNodeClick(SkillTreeNode node) {
        // Obter Perfil e Dados
        if (MidgardCore.getProfileManager() == null) {
            return;
        }
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) {
            return;
        }
        
        ClassData data = profile.getData(ClassData.class);
        if (data == null) {
            MessageUtils.send(player, module.getMessage("skills.data_error"));
            return;
        }

        // Provedor de Skill para verificar/definir níveis
        SkillProvider skillProvider = MidgardCore.getSkillProvider();
        if (skillProvider == null) {
            MessageUtils.send(player, module.getMessage("skills.system_error"));
            return;
        }

        String skillId = node.getId();
        int currentLevel = skillProvider.getSkillLevel(profile, skillId);

        // 1. Verificar Nível Máximo
        if (currentLevel >= node.getMaxLevel()) {
            MessageUtils.send(player, module.getMessage("skills.max_level_reached"));
            return;
        }

        // 1.5. Verificar Limite de Pontos da Árvore
        if (skillTree.getMaxPoints() > 0) {
            int totalInvested = 0;
            for (SkillTreeNode treeNode : skillTree.getNodes().values()) {
                totalInvested += skillProvider.getSkillLevel(profile, treeNode.getId());
            }
            if (totalInvested >= skillTree.getMaxPoints()) {
                MessageUtils.send(player, module.getMessage("skills.tree_max_points"));
                return;
            }
        }

        // 2. Verificar Pontos de Habilidade
        // Custo Fixo de 1 ponto
        int cost = 1; 
        if (data.getSkillPoints() < cost) {
            String msg = module.getMessage("skills.points_needed")
                .replace("%cost%", String.valueOf(cost));
            MessageUtils.send(player, msg);
            return;
        }

        // 3. Verificar Pré-requisitos (Parents)
        if (node.getParents() != null) {
            for (Map.Entry<String, Integer> entry : node.getParents().entrySet()) {
                String parentId = entry.getKey();
                int requiredLevel = entry.getValue();
                int parentLevel = skillProvider.getSkillLevel(profile, parentId);
                
                if (parentLevel < requiredLevel) {
                    String parentName = skillProvider.getSkillName(parentId);
                    String requirement = module.getMessage("skills.requirement_format")
                        .replace("%skill%", parentName)
                        .replace("%level%", String.valueOf(requiredLevel));
                    String msg = module.getMessage("skills.skill_required")
                        .replace("%skill_name%", node.getDisplayName())
                        .replace("%requirement%", requirement);
                    MessageUtils.send(player, msg);
                    return;
                }
            }
        }

        // 4. Executar Upgrade
        data.setSkillPoints(data.getSkillPoints() - cost);
        skillProvider.setSkillLevel(profile, skillId, currentLevel + 1);

        // Persistir imediatamente
        MidgardCore.getProfileManager().saveProfile(profile);
        
        // Efeitos e Feedback
        String msg = module.getMessage("skills.upgraded")
            .replace("%skill_name%", node.getDisplayName())
            .replace("%level%", String.valueOf(currentLevel + 1));
        MessageUtils.send(player, msg);
            
        // Atualizar GUI
        initializeItems();
    }
}

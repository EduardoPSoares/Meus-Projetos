package me.ray.midgard.modules.essentials.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.gui.GuiUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InvseeGui extends BaseGui {

    private final Player target;
    private final EssentialsManager manager;

    public InvseeGui(Player viewer, Player target, EssentialsManager manager) {
        super(viewer, 6, manager.getMessage("invsee.gui.title").replace("%player%", target.getName()));
        this.target = target;
        this.manager = manager;
    }

    @Override
    public void initializeItems() {
        // Copiar inventário principal (0-35)
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < 36; i++) {
            if (i < contents.length && contents[i] != null) {
                inventory.setItem(i, contents[i].clone());
            }
        }

        // Divisória
        GuiUtils.fillRow(inventory, 4, GuiUtils.createBorderItem());

        // Armadura (Slots 36-39 no Bukkit, vamos por na linha 5 do GUI)
        ItemStack[] armor = target.getInventory().getArmorContents();
        // Helmet
        if (armor.length > 3 && armor[3] != null) {
            inventory.setItem(45, armor[3].clone());
        } else {
            inventory.setItem(45, new ItemBuilder(Material.BARRIER).setName(manager.getMessage("invsee.gui.empty_helmet")).build());
        }
        
        // Chestplate
        if (armor.length > 2 && armor[2] != null) {
            inventory.setItem(46, armor[2].clone());
        } else {
            inventory.setItem(46, new ItemBuilder(Material.BARRIER).setName(manager.getMessage("invsee.gui.empty_chestplate")).build());
        }

        // Leggings
        if (armor.length > 1 && armor[1] != null) {
            inventory.setItem(47, armor[1].clone());
        } else {
            inventory.setItem(47, new ItemBuilder(Material.BARRIER).setName(manager.getMessage("invsee.gui.empty_leggings")).build());
        }

        // Boots
        if (armor.length > 0 && armor[0] != null) {
            inventory.setItem(48, armor[0].clone());
        } else {
            inventory.setItem(48, new ItemBuilder(Material.BARRIER).setName(manager.getMessage("invsee.gui.empty_boots")).build());
        }

        // Offhand
        ItemStack offhand = target.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() != Material.AIR) {
            inventory.setItem(53, offhand.clone());
        } else {
            inventory.setItem(53, new ItemBuilder(Material.BARRIER).setName(manager.getMessage("invsee.gui.empty_offhand")).build());
        }
        
        // Info
        double maxHealth = 20.0;
        if (target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
            maxHealth = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        }

        inventory.setItem(49, new ItemBuilder(Material.PAPER)
                .setName(manager.getMessage("invsee.gui.info_name"))
                .addLore(manager.getMessage("invsee.gui.info_health")
                        .replace("%current%", String.format("%.1f", target.getHealth()))
                        .replace("%max%", String.format("%.1f", maxHealth)))
                .addLore(manager.getMessage("invsee.gui.info_food").replace("%food%", String.valueOf(target.getFoodLevel())))
                .addLore(manager.getMessage("invsee.gui.info_xp").replace("%xp%", String.valueOf(target.getLevel())))
                .addLore("")
                .addLore(manager.getMessage("invsee.gui.readonly"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true); // Bloquear qualquer interação
    }
}

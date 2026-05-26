package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SocketConfigurationGui extends BaseGui {

    private final MidgardItem item;
    private final ItemEditionGui parent;

    public SocketConfigurationGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, 6, MidgardCore.getLanguageManager().getRawMessage("item.gui.socket_configuration.title"));
        this.item = item;
        this.parent = parent;
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        
        List<String> sockets = item.getGemSockets();
        if (sockets == null) {
            sockets = new ArrayList<>();
        }
        
        for (int i = 0; i < sockets.size(); i++) {
            if (i >= 45) {
                break;
            }
            
            String socketData = sockets.get(i);
            // socketData might be "redWEAPON" or "red:WEAPON"
            String display = socketData;
            
            ItemBuilder builder = new ItemBuilder(Material.EMERALD)
                    .name(MidgardCore.getLanguageManager().getMessage("item.gui.socket_configuration.item.name", "%d", String.valueOf(i + 1)));
            
            List<String> loreLines = MidgardCore.getLanguageManager().getStringList("item.gui.socket_configuration.item.lore");
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(MessageUtils.parse(line.replace("%type%", display)));
            }
            builder.lore(lore);
            
            inventory.setItem(i, builder.build());
        }
        
        // Add Button
        ItemStack addBtn = new ItemBuilder(Material.LIME_DYE)
                .name(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.socket_configuration.buttons.add.name")))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.socket_configuration.buttons.add.lore").stream().map(MessageUtils::parse).toList())
                .build();
        inventory.setItem(49, addBtn);
        
        // Back Button
        ItemStack backBtn = new ItemBuilder(Material.BARRIER)
                .name(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.socket_configuration.buttons.back")))
                .build();
        inventory.setItem(53, backBtn);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        List<String> sockets = new ArrayList<>(item.getGemSockets() != null ? item.getGemSockets() : new ArrayList<>());

        if (slot < 45 && slot < sockets.size()) {
            if (event.isRightClick()) {
                sockets.remove(slot);
                item.setGemSockets(sockets);
                item.save();
                initializeItems();
            }
        } else if (slot == 49) {
            new SocketTypeSelectionGui(player, item, this).open();
        } else if (slot == 53) {
            parent.open();
        }
    }
}

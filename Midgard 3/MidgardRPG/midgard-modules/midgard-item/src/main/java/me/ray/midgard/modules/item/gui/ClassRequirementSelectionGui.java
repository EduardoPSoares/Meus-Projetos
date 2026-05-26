package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.classes.ClassesModule;
import me.ray.midgard.modules.classes.RPGClass;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassRequirementSelectionGui extends PaginatedGui<RPGClass> {

    private final MidgardItem item;
    private final ItemEditionGui parent;

    public ClassRequirementSelectionGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, MidgardCore.getLanguageManager().getRawMessage("item.gui.class_requirement_selection.title"), new ArrayList<>());
        this.item = item;
        this.parent = parent;

        if (ClassesModule.getInstance() != null) {
            this.items = new ArrayList<>(ClassesModule.getInstance().getClassManager().getClasses().values());
        } else {
            this.items = new ArrayList<>();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.class_requirement_selection.error.module_disabled"));
        }
    }

    private boolean isSelected(RPGClass rpgClass) {
        return item.getRequiredClasses().contains(rpgClass.getId());
    }

    @Override
    public ItemStack createItem(RPGClass rpgClass) {
        boolean selected = isSelected(rpgClass);
        Material mat = selected ? Material.ENCHANTED_BOOK : Material.BOOK;

        String loreKey = selected
                ? "item.gui.class_requirement_selection.item.lore_selected"
                : "item.gui.class_requirement_selection.item.lore";
        List<String> loreLines = MidgardCore.getLanguageManager().getStringList(loreKey);
        List<Component> lore = loreLines.stream()
                .map(line -> line.replace("%s", rpgClass.getId()))
                .map(MessageUtils::parse)
                .collect(Collectors.toList());

        return new ItemBuilder(mat)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.class_requirement_selection.item.name", "%s", rpgClass.getDisplayName()))
                .lore(lore)
                .build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Handle navigation and close
        if (slot == 45 && page > 0) {
            page--;
            initializeItems();
            return;
        } else if (slot == 53 && (page + 1) * 45 < items.size()) {
            page++;
            initializeItems();
            return;
        } else if (slot == 49) {
            parent.open();
            return;
        } else if (slot == 48) {
            // Clear all classes
            item.setRequiredClasses(new ArrayList<>());
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.class_requirement_selection.messages.removed"));
            initializeItems();
            return;
        }

        // Handle item click - toggle class
        if (slot < 45) {
            int index = page * 45 + slot;
            if (index < items.size()) {
                RPGClass selectedClass = items.get(index);
                List<String> classes = new ArrayList<>(item.getRequiredClasses());

                if (classes.contains(selectedClass.getId())) {
                    // Remove
                    classes.remove(selectedClass.getId());
                    item.setRequiredClasses(classes);
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.class_requirement_selection.messages.removed_class", "%s", selectedClass.getDisplayName()));
                } else {
                    // Add
                    classes.add(selectedClass.getId());
                    item.setRequiredClasses(classes);
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.class_requirement_selection.messages.set", "%s", selectedClass.getDisplayName()));
                }
                initializeItems();
            }
        }
    }

    @Override
    public void addMenuBorder() {
        // Navigation buttons
        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.gui.class_requirement_selection.buttons.previous_page")).build());
        }

        if ((page + 1) * 45 < items.size()) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.gui.class_requirement_selection.buttons.next_page")).build());
        }

        // Back button
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name(MidgardCore.getLanguageManager().getMessage("item.gui.class_requirement_selection.buttons.back")).build());

        // Clear option
        List<Component> clearLore = MidgardCore.getLanguageManager().getStringList("item.gui.class_requirement_selection.buttons.clear.lore").stream()
                .map(MessageUtils::parse)
                .toList();

        inventory.setItem(48, new ItemBuilder(Material.MILK_BUCKET)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.class_requirement_selection.buttons.clear.name"))
                .lore(clearLore)
                .build());
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        addMenuBorder();

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            inventory.setItem(i - startIndex, createItem(items.get(i)));
        }
    }
}

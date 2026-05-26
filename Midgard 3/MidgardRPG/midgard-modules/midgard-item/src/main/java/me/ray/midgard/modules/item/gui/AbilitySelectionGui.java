package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.skill.Skill;
import me.ray.midgard.core.skill.SkillRegistry;
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

public class AbilitySelectionGui extends PaginatedGui<Skill> {

    private final MidgardItem item;
    private final ItemEditionGui parent;

    public AbilitySelectionGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, MidgardCore.getLanguageManager().getRawMessage("item.gui.ability_selection.title"), new ArrayList<>());
        this.item = item;
        this.parent = parent;
        
        // Load skills from registry
        if (SkillRegistry.getInstance() != null) {
            this.items = new ArrayList<>(SkillRegistry.getInstance().getAll());
        } else {
            this.items = new ArrayList<>();
        }
    }

    @Override
    public ItemStack createItem(Skill skill) {
        boolean hasAbility = item.getItemAbilities().contains(skill.getId());
        Material mat = hasAbility ? Material.ENCHANTED_BOOK : Material.BOOK;
        
        ItemBuilder builder = new ItemBuilder(mat)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.ability_selection.item.name", "%s", skill.getName()));

        List<String> loreLines = MidgardCore.getLanguageManager().getStringList("item.gui.ability_selection.item.lore");
        List<Component> lore = new ArrayList<>();
        
        for (String line : loreLines) {
            lore.add(MessageUtils.parse(line
                    .replace("%id%", skill.getId())
                    .replace("%type%", skill.getType().name())
                    .replace("%cooldown%", String.valueOf(skill.getCooldown() / 1000.0))
            ));
        }
        
        if (hasAbility) {
            lore.add(MessageUtils.parse(""));
            lore.add(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.ability_selection.active")));
        }
        
        builder.lore(lore);
        return builder.build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Handle navigation
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
        }

        // Handle item click
        if (slot < 45) {
            int index = page * 45 + slot;
            if (index < items.size()) {
                Skill skill = items.get(index);
                List<String> currentAbilities = new ArrayList<>(item.getItemAbilities());
                
                if (currentAbilities.contains(skill.getId())) {
                    currentAbilities.remove(skill.getId());
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.ability_selection.messages.removed", "%s", skill.getName()));
                } else {
                    currentAbilities.add(skill.getId());
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.ability_selection.messages.added", "%s", skill.getName()));
                }
                
                item.setItemAbilities(currentAbilities);
                item.save();
                initializeItems();
            }
        }
    }

    @Override
    public void addMenuBorder() {
        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.gui.ability_selection.buttons.previous_page")).build());
        }
        
        if ((page + 1) * 45 < items.size()) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.gui.ability_selection.buttons.next_page")).build());
        }

        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name(MidgardCore.getLanguageManager().getMessage("item.gui.ability_selection.buttons.back")).build());
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

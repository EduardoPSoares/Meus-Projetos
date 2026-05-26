package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.manager.TierManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TierSelectionGui extends PaginatedGui<String> {

    private final MidgardItem item;
    private final ItemEditionGui parent;
    private final TierManager tierManager;

    public TierSelectionGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, MidgardCore.getLanguageManager().getRawMessage("item.gui.tier_selection.title"), new ArrayList<>());
        this.item = item;
        this.parent = parent;
        this.tierManager = module.getTierManager();
        
        // Load real tiers from TierManager
        List<String> tiers = new ArrayList<>();
        if (tierManager != null) {
            tiers.addAll(tierManager.getTiers().stream()
                    .map(TierManager.Tier::getId)
                    .collect(Collectors.toList()));
        }
        
        // Fallback if empty
        if (tiers.isEmpty()) {
            tiers.add("COMMON");
            tiers.add("UNCOMMON");
            tiers.add("RARE");
            tiers.add("EPIC");
            tiers.add("LEGENDARY");
            tiers.add("MYTHIC");
            tiers.add("ARTIFACT");
        }
        
        this.items = tiers;
    }

    @Override
    public ItemStack createItem(String tierId) {
        boolean selected = tierId.equalsIgnoreCase(item.getTier());
        Material mat = selected ? Material.DIAMOND : Material.COAL;
        
        String displayName = tierId;
        String resolvedDisplay = tierId;
        
        if (tierManager != null) {
            TierManager.Tier tier = tierManager.getTier(tierId);
            if (tier != null) {
                displayName = tier.getDisplayName();
                resolvedDisplay = tier.resolveDisplay();
            }
        }
        
        // Format: [Icon] Name — uses glyph tag only if Nexo glyph is valid, otherwise display name
        String format = (selected ? "<green>✔ " : "<gray>") + resolvedDisplay;
        
        final String finalDisplayName = displayName;
        return new ItemBuilder(mat)
                .name(MessageUtils.parse(format))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.tier_selection.item.lore").stream()
                        .map(line -> MessageUtils.parse(line.replace("%tier%", finalDisplayName)))
                        .toList())
                .build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 45 && page > 0) {
            page--;
            initializeItems();
        } else if (slot == 53 && (page + 1) * 45 < items.size()) {
            page++;
            initializeItems();
        } else if (slot == 49) {
            parent.open();
        } else if (slot < 45) {
            int index = page * 45 + slot;
            if (index < items.size()) {
                String tier = items.get(index);
                item.setTier(tier);
                item.save();
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.tier_selection.messages.updated", "%tier%", tier));
                parent.open();
            }
        }
    }

    @Override
    public void addMenuBorder() {
        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.common.previous_page")).build());
        }
        if ((page + 1) * 45 < items.size()) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.common.next_page")).build());
        }
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name(MidgardCore.getLanguageManager().getMessage("item.gui.tier_selection.buttons.back")).build());
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

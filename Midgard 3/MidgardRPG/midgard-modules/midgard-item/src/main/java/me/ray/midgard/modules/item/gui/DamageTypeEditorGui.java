package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.ItemStat;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class DamageTypeEditorGui extends PaginatedGui<ItemStat> {

    private final MidgardItem item;
    private final ItemEditionGui parent;

    private static final List<ItemStat> DAMAGE_STATS = Arrays.asList(
            ItemStat.ATTACK_DAMAGE,
            ItemStat.MAGIC_DAMAGE,
            ItemStat.PROJECTILE_DAMAGE,
            ItemStat.SKILL_DAMAGE,
            ItemStat.UNDEAD_DAMAGE,
            ItemStat.FIRE_DAMAGE,
            ItemStat.ICE_DAMAGE,
            ItemStat.LIGHT_DAMAGE,
            ItemStat.DARKNESS_DAMAGE,
            ItemStat.DIVINE_DAMAGE
    );

    public DamageTypeEditorGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, MidgardCore.getLanguageManager().getRawMessage("item.gui.damage_type_editor.title"), DAMAGE_STATS);
        this.item = item;
        this.parent = parent;
    }

    @Override
    public ItemStack createItem(ItemStat stat) {
        me.ray.midgard.modules.item.utils.StatRange range = item.getStatRange(stat);
        String valueStr;
        if (range != null) {
            valueStr = range.toString();
        } else {
            valueStr = "0";
        }
        String baseKey = "item.gui.damage_type_editor.stats." + stat.getPath();

        String name = MidgardCore.getLanguageManager().getRawMessage(baseKey + ".name");
        if (name == null || name.startsWith("<red>")) {
            name = stat.getName();
        }

        List<String> description = MidgardCore.getLanguageManager().getStringList(baseKey + ".description");

        ItemBuilder builder = new ItemBuilder(getIconForStat(stat))
                .name(MessageUtils.parse("<green>" + name));

        if (description != null && !description.isEmpty()) {
            for (String line : description) {
                builder.addLore(line);
            }
            builder.addLore("");
        }

        builder.addLore(MidgardCore.getLanguageManager().getRawMessage("item.gui.damage_type_editor.lore.current_value").replace("%value%", valueStr));
        builder.addLore("");
        builder.addLore(MidgardCore.getLanguageManager().getRawMessage("item.gui.damage_type_editor.lore.click_edit"));
        builder.addLore(MidgardCore.getLanguageManager().getRawMessage("item.gui.damage_type_editor.lore.right_click_reset"));

        return builder.build();
    }

    private Material getIconForStat(ItemStat stat) {
        switch (stat) {
            case ATTACK_DAMAGE: return Material.IRON_SWORD;
            case MAGIC_DAMAGE: return Material.BLAZE_ROD;
            case PROJECTILE_DAMAGE: return Material.ARROW;
            case SKILL_DAMAGE: return Material.EXPERIENCE_BOTTLE;
            case UNDEAD_DAMAGE: return Material.ROTTEN_FLESH;
            case FIRE_DAMAGE: return Material.FLINT_AND_STEEL;
            case ICE_DAMAGE: return Material.SNOWBALL;
            case LIGHT_DAMAGE: return Material.GLOWSTONE_DUST;
            case DARKNESS_DAMAGE: return Material.COAL;
            case DIVINE_DAMAGE: return Material.NETHER_STAR;
            default: return Material.PAPER;
        }
    }

    @Override
    public void addMenuBorder() {
        super.addMenuBorder();
        // Back button at 49
        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
                .name(MidgardCore.getLanguageManager().getMessage("item.common.back"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 49) {
            parent.open();
            return;
        }

        // Handle item clicks based on PaginatedGui layout (Rows 1-3, Cols 1-7)
        int row = slot / 9;
        int col = slot % 9;

        if (row >= 1 && row <= 3 && col >= 1 && col <= 7) {
            int relativeIndex = (row - 1) * 7 + (col - 1);
            int index = relativeIndex + (page * maxItemsPerPage);

            if (index < items.size()) {
                ItemStat stat = items.get(index);
                
                if (event.getClick().isRightClick()) {
                    item.setStat(stat, 0.0);
                    item.save();
                    player.sendMessage(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.damage_type_editor.messages.reset").replace("%stat%", stat.getName())));
                    initializeItems();
                } else {
                    player.closeInventory();
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.enter-prompt", "%s", stat.getName()));
                    player.sendMessage(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.editors.stat_range.examples")));

                    me.ray.midgard.modules.item.listener.ChatInputListener.requestInput(player, (input) -> {
                        try {
                            me.ray.midgard.modules.item.utils.StatRange.parse(input);
                            item.setStat(stat, input);
                            item.save();
                            player.sendMessage(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.damage_type_editor.messages.updated").replace("%value%", input)));
                        } catch (Exception e) {
                            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.invalid-number"));
                        }
                        this.open();
                    });
                }
                return;
            }
        }
        
        super.onClick(event); // Handles pagination clicks
    }
}

package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.admin;

import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelteryTemplate;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import me.ray.midgard.modules.professions.ProfessionsModule;

/**
 * Painel admin principal listando todos os templates de smeltery com paginação.
 */
public class SmelteryAdminGui extends PaginatedGui<SmelteryTemplate> {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage("gui.smeltery_admin." + key); }

    private BiConsumer<Player, SmelteryTemplate> onTemplateClick;
    private Consumer<Player> onCreateNew;
    private final List<SmelteryTemplate> sourceTemplates;

    public SmelteryAdminGui(Player player, List<SmelteryTemplate> templates) {
        super(player, ProfessionsModule.getInstance().getMessage("gui.smeltery_admin.title"), templates);
        this.sourceTemplates = templates;
    }

    @Override
    public ItemStack createItem(SmelteryTemplate template) {
        String shortId = template.getTemplateId().toString().substring(0, 8);
        String date = new SimpleDateFormat("dd/MM/yyyy").format(new Date(template.getCreatedAt()));
        String status = template.isActive() ? msg("status.active") : msg("status.inactive");

        Material icon = switch (template.getTier()) {
            case SMALL -> Material.NETHER_BRICKS;
            case MEDIUM -> Material.RED_NETHER_BRICKS;
            case LARGE -> Material.CHISELED_NETHER_BRICKS;
            case MASTER -> Material.POLISHED_BLACKSTONE_BRICKS;
            case LEGENDARY -> Material.CRYING_OBSIDIAN;
        };

        var builder = new ItemBuilder(icon)
                .setName("<gold>" + template.getName())
                .addLore("<dark_gray>" + shortId)
                .addLore("")
                .addLore(msg("lore.type") + template.getTier().getFormattedName())
                .addLore(msg("lore.required_level") + template.getRequiredLevel())
                .addLore(msg("lore.created_at") + date)
                .addLore(msg("lore.status") + status);

        if (template.getSchematic() != null) {
            var sch = template.getSchematic();
            builder.addLore(msg("lore.dimensions") + sch.getWidth() + "x" + sch.getHeight() + "x" + sch.getDepth());
            builder.addLore(msg("lore.blocks") + sch.getSolidBlocks().size());
        }

        builder.addLore("")
                .addLore(msg("lore.click_edit"))
                .glowIf(template.isActive());

        return builder.build();
    }

    @Override
    public void onItemClick(Player player, int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (row < 1 || row > 3 || col < 1 || col > 7) { return; }

        int itemIndex = page * maxItemsPerPage + (row - 1) * 7 + (col - 1);
        if (itemIndex < 0 || itemIndex >= items.size()) { return; }

        SmelteryTemplate template = items.get(itemIndex);
        if (onTemplateClick != null) {
            player.closeInventory();
            onTemplateClick.accept(player, template);
        }
    }

    @Override
    public void addMenuBorder() {
        super.addMenuBorder();

        int total = items.size();
        long active = items.stream().filter(SmelteryTemplate::isActive).count();
        inventory.setItem(4, new ItemBuilder(Material.BLAST_FURNACE)
                .setName(msg("info.title"))
                .addLore(msg("info.total") + total)
                .addLore(msg("info.active_count") + active)
                .addLore(msg("info.inactive_count") + (total - active))
                .addLore("")
                .addLore(msg("info.desc_1"))
                .addLore(msg("info.desc_2"))
                .build());

        inventory.setItem(45, new ItemBuilder(Material.EMERALD)
                .setName(msg("create.title"))
                .addLore(msg("create.desc_1"))
                .addLore(msg("create.desc_2"))
                .addLore("")
                .addLore(msg("create.click"))
                .glow().build());

        inventory.setItem(53, new ItemBuilder(Material.SUNFLOWER)
                .setName(msg("refresh.title"))
                .addLore(msg("refresh.desc"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 45) {
            if (onCreateNew != null) {
                player.closeInventory();
                onCreateNew.accept(player);
            }
            return;
        }

        if (slot == 53) {
            this.items = sourceTemplates;
            this.page = 0;
            initializeItems();
            return;
        }

        super.onClick(event);
    }

    public void setOnTemplateClick(BiConsumer<Player, SmelteryTemplate> cb) { this.onTemplateClick = cb; }
    public void setOnCreateNew(Consumer<Player> cb) { this.onCreateNew = cb; }
}

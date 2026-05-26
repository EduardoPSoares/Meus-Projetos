package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.admin;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryTier;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelteryTemplate;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

/**
 * GUI para editar propriedades de um template de smeltery existente.
 * Permite renomear, alterar tier, nível, ativar/desativar e excluir.
 */
public class SmelteryEditGui extends BaseGui {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage("gui.smeltery_edit." + key); }

    private static final int SLOT_INFO = 4;
    private static final int SLOT_NAME = 20;
    private static final int SLOT_TIER = 22;
    private static final int SLOT_LEVEL = 24;
    private static final int SLOT_TOGGLE_ACTIVE = 30;
    private static final int SLOT_DELETE = 32;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_CLOSE = 49;

    private final SmelteryTemplate template;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private Consumer<Player> onBack;
    private Consumer<Player> onDelete;

    public SmelteryEditGui(Player player, SmelteryTemplate template) {
        super(player, 6, ProfessionsModule.getInstance().getMessage("gui.smeltery_edit.title") + template.getName());
        this.template = template;
    }

    @Override
    public void initializeItems() {
        var border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        String shortId = template.getTemplateId().toString().substring(0, 8);
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(template.getCreatedAt()));

        var infoBuilder = new ItemBuilder(Material.BLAST_FURNACE)
                .setName("<gold><bold>⚗ " + template.getName() + "</bold>")
                .addLore("<dark_gray>ID: " + shortId)
                .addLore("")
                .addLore(msg("info.lore.type") + template.getTier().getFormattedName())
                .addLore(msg("info.lore.required_level") + template.getRequiredLevel())
                .addLore(msg("info.lore.created_at") + date)
                .addLore(msg("info.lore.status") + (template.isActive() ? msg("status.active") : msg("status.inactive")));

        if (template.getSchematic() != null) {
            var sch = template.getSchematic();
            infoBuilder.addLore(msg("info.lore.dimensions") + sch.getWidth() + "x" + sch.getHeight() + "x" + sch.getDepth());
            infoBuilder.addLore(msg("info.lore.blocks") + sch.getSolidBlocks().size());
        }
        inventory.setItem(SLOT_INFO, infoBuilder.build());

        inventory.setItem(SLOT_NAME, new ItemBuilder(Material.NAME_TAG)
                .setName(msg("name.title"))
                .addLore(msg("name.current") + template.getName())
                .addLore("")
                .addLore(msg("name.click_rename"))
                .addLore(msg("name.type_in_chat"))
                .build());

        inventory.setItem(SLOT_TIER, new ItemBuilder(Material.NETHER_STAR)
                .setName(msg("tier.title"))
                .addLore(msg("tier.current") + template.getTier().getFormattedName())
                .addLore("")
                .addLore(msg("tier.click_next"))
                .addLore(msg("tier.click_prev"))
                .glow().build());

        inventory.setItem(SLOT_LEVEL, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(msg("level.title"))
                .addLore(msg("level.required") + template.getRequiredLevel())
                .addLore("")
                .addLore(msg("level.click_add"))
                .addLore(msg("level.click_sub"))
                .addLore(msg("level.click_shift"))
                .build());

        boolean active = template.isActive();
        inventory.setItem(SLOT_TOGGLE_ACTIVE, new ItemBuilder(active ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(active ? msg("toggle.active_title") : msg("toggle.inactive_title"))
                .addLore(active
                        ? msg("toggle.active_desc")
                        : msg("toggle.inactive_desc"))
                .addLore("")
                .addLore(active ? msg("toggle.click_deactivate") : msg("toggle.click_activate"))
                .build());

        inventory.setItem(SLOT_DELETE, new ItemBuilder(Material.TNT)
                .setName(msg("delete.title"))
                .addLore(msg("delete.desc"))
                .addLore("")
                .addLore(msg("delete.warning"))
                .addLore(msg("delete.click"))
                .build());

        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.DARK_OAK_DOOR)
                .setName(msg("back.title"))
                .addLore(msg("back.desc"))
                .build());

        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .setName(msg("close.title"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker)) { return; }

        int slot = event.getRawSlot();

        switch (slot) {
            case SLOT_NAME -> {
                clicker.closeInventory();
                clicker.sendMessage(mm.deserialize(ProfessionsModule.getInstance().getMessage("smeltery.admin.enter_new_name")));
                SmelteryCreationManager manager = SmelteryCreationManager.getInstance();
                if (manager != null) {
                    manager.setAwaitingNameInput(clicker.getUniqueId(), template, p -> {
                        openRefreshed(p);
                    });
                }
            }
            case SLOT_TIER -> {
                SmelteryTier[] tiers = SmelteryTier.values();
                int idx = template.getTier().ordinal();
                if (event.isLeftClick()) {
                    idx = (idx + 1) % tiers.length;
                } else if (event.isRightClick()) {
                    idx = (idx - 1 + tiers.length) % tiers.length;
                }
                template.setTier(tiers[idx]);
                initializeItems();
            }
            case SLOT_LEVEL -> {
                int delta = event.isShiftClick() ? 10 : 1;
                if (event.isRightClick()) { delta = -delta; }
                int newLevel = Math.max(1, Math.min(100, template.getRequiredLevel() + delta));
                template.setRequiredLevel(newLevel);
                initializeItems();
            }
            case SLOT_TOGGLE_ACTIVE -> {
                template.setActive(!template.isActive());
                clicker.sendMessage(mm.deserialize(template.isActive()
                        ? ProfessionsModule.getInstance().getMessage("smeltery.admin.template_toggled_on")
                        : ProfessionsModule.getInstance().getMessage("smeltery.admin.template_toggled_off")));
                initializeItems();
            }
            case SLOT_DELETE -> {
                if (!event.isShiftClick()) {
                    clicker.sendMessage(mm.deserialize(ProfessionsModule.getInstance().getMessage("smeltery.admin.confirm_delete")));
                    return;
                }
                clicker.closeInventory();
                clicker.sendMessage(mm.deserialize(ProfessionsModule.getInstance().getMessage("smeltery.admin.template_deleted").replace("%name%", template.getName())));
                if (onDelete != null) { onDelete.accept(clicker); }
            }
            case SLOT_BACK -> {
                clicker.closeInventory();
                if (onBack != null) { onBack.accept(clicker); }
            }
            case SLOT_CLOSE -> clicker.closeInventory();
        }
    }

    private void openRefreshed(Player p) {
        SmelteryEditGui refreshed = new SmelteryEditGui(p, template);
        refreshed.setOnBack(this.onBack);
        refreshed.setOnDelete(this.onDelete);
        refreshed.open();
    }

    public void setOnBack(Consumer<Player> cb) { this.onBack = cb; }
    public void setOnDelete(Consumer<Player> cb) { this.onDelete = cb; }
}

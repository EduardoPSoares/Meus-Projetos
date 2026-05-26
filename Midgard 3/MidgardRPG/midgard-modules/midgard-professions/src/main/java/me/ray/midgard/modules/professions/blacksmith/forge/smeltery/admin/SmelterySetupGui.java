package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.admin;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryBlockType;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/**
 * GUI principal de criação de template de smeltery.
 * Fluxo: definir nome/tipo/nível → selecionar área → escanear → validar → confirmar.
 */
public class SmelterySetupGui extends BaseGui {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage("gui.smeltery_setup." + key); }

    private static final int SLOT_INFO = 4;
    private static final int SLOT_NAME = 11;
    private static final int SLOT_TIER = 13;
    private static final int SLOT_LEVEL = 15;
    private static final int SLOT_POS1 = 20;
    private static final int SLOT_SCAN = 22;
    private static final int SLOT_POS2 = 24;
    private static final int SLOT_BLOCKS_SUMMARY = 29;
    private static final int SLOT_ASSIGN_BLOCKS = 31;
    private static final int SLOT_VALIDATION = 33;
    private static final int SLOT_CANCEL = 45;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_CONFIRM = 53;

    private final SmelteryCreationSession session;

    private Consumer<Player> onOpenBlockAssign;
    private Consumer<Player> onConfirm;
    private Consumer<Player> onCancel;
    private Consumer<Player> onSetName;

    public SmelterySetupGui(Player player, SmelteryCreationSession session) {
        super(player, 6, ProfessionsModule.getInstance().getMessage("gui.smeltery_setup.title"));
        this.session = session;
    }

    @Override
    public void initializeItems() {
        var border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        boolean hasPos1 = session.getPos1() != null;
        boolean hasPos2 = session.getPos2() != null;
        boolean scanned = session.isScanned();

        // --- Info header ---
        if (scanned) {
            Map<SmelteryBlockType, Integer> roles = session.getRoleCounts();
            int interactiveCount = roles.values().stream().mapToInt(Integer::intValue).sum();
            inventory.setItem(SLOT_INFO, new ItemBuilder(Material.BLAST_FURNACE)
                    .setName("<gold><bold>⚗ " + session.getName() + "</bold>")
                    .addLore(msg("info.lore.type") + session.getTier().getFormattedName())
                    .addLore(msg("info.lore.required_level") + session.getRequiredLevel())
                    .addLore(msg("info.lore.dimensions") + session.getWidth() + "x" + session.getHeight() + "x" + session.getDepth())
                    .addLore(msg("info.lore.blocks") + session.getScannedBlocks().size())
                    .addLore(msg("info.lore.interactive_blocks") + interactiveCount)
                    .build());
        } else {
            inventory.setItem(SLOT_INFO, new ItemBuilder(Material.PAPER)
                    .setName(msg("info.how_to_title"))
                    .addLore(msg("info.step_1"))
                    .addLore(msg("info.step_2"))
                    .addLore(msg("info.step_3"))
                    .addLore(msg("info.step_4"))
                    .addLore(msg("info.step_5"))
                    .build());
        }

        // --- Row 1: Name, Tier, Level ---

        inventory.setItem(SLOT_NAME, new ItemBuilder(Material.NAME_TAG)
                .setName(msg("name.title"))
                .addLore(msg("name.current") + session.getName())
                .addLore("")
                .addLore(msg("name.click_change"))
                .addLore(msg("name.type_in_chat"))
                .build());

        SmelteryTier tier = session.getTier();
        inventory.setItem(SLOT_TIER, new ItemBuilder(Material.NETHER_STAR)
                .setName(msg("tier.title"))
                .addLore(msg("tier.current") + tier.getFormattedName())
                .addLore("")
                .addLore(msg("tier.click_next"))
                .addLore(msg("tier.click_prev"))
                .glow().build());

        inventory.setItem(SLOT_LEVEL, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(msg("level.title"))
                .addLore(msg("level.required") + session.getRequiredLevel())
                .addLore("")
                .addLore(msg("level.click_add"))
                .addLore(msg("level.click_sub"))
                .addLore(msg("level.click_shift"))
                .build());

        // --- Row 2: Positions + Scan ---

        if (hasPos1) {
            inventory.setItem(SLOT_POS1, new ItemBuilder(Material.LIME_CONCRETE)
                    .setName(msg("pos1.defined"))
                    .addLore("<white>" + formatLoc(session.getPos1()))
                    .addLore("")
                    .addLore(msg("pos1.click_redefine"))
                    .build());
        } else {
            inventory.setItem(SLOT_POS1, new ItemBuilder(Material.RED_CONCRETE)
                    .setName(msg("pos1.undefined"))
                    .addLore(msg("pos1.desc_1"))
                    .addLore(msg("pos1.desc_2"))
                    .addLore("")
                    .addLore(msg("pos1.click_capture"))
                    .build());
        }

        if (scanned) {
            inventory.setItem(SLOT_SCAN, new ItemBuilder(Material.ENDER_EYE)
                    .setName(msg("scan.done_title"))
                    .addLore(msg("scan.blocks_detected") + session.getScannedBlocks().size())
                    .addLore("")
                    .addLore(msg("scan.click_rescan"))
                    .glow().build());
        } else if (hasPos1 && hasPos2) {
            inventory.setItem(SLOT_SCAN, new ItemBuilder(Material.ENDER_EYE)
                    .setName(msg("scan.ready_title"))
                    .addLore(msg("scan.ready_desc_1"))
                    .addLore(msg("scan.ready_desc_2"))
                    .addLore("")
                    .addLore(msg("scan.click_scan"))
                    .glow().build());
        } else {
            inventory.setItem(SLOT_SCAN, new ItemBuilder(Material.GRAY_DYE)
                    .setName(msg("scan.disabled_title"))
                    .addLore(msg("scan.disabled_desc"))
                    .build());
        }

        if (hasPos2) {
            inventory.setItem(SLOT_POS2, new ItemBuilder(Material.LIME_CONCRETE)
                    .setName(msg("pos2.defined"))
                    .addLore("<white>" + formatLoc(session.getPos2()))
                    .addLore("")
                    .addLore(msg("pos2.click_redefine"))
                    .build());
        } else {
            inventory.setItem(SLOT_POS2, new ItemBuilder(Material.RED_CONCRETE)
                    .setName(msg("pos2.undefined"))
                    .addLore(msg("pos2.desc_1"))
                    .addLore(msg("pos2.desc_2"))
                    .addLore("")
                    .addLore(msg("pos2.click_capture"))
                    .build());
        }

        // --- Row 3: Blocks + Validation (only after scan) ---
        if (scanned) {
            Map<SmelteryBlockType, Integer> roles = session.getRoleCounts();

            var summaryBuilder = new ItemBuilder(Material.BOOKSHELF)
                    .setName(msg("blocks_summary.title"))
                    .addLore("");
            for (SmelteryBlockType type : SmelteryBlockType.values()) {
                if (type == SmelteryBlockType.AIR || type == SmelteryBlockType.WALL) { continue; }
                int count = roles.getOrDefault(type, 0);
                String color = count > 0 ? "<green>" : "<red>";
                summaryBuilder.addLore(color + getTypeName(type) + ": <white>" + count);
            }
            inventory.setItem(SLOT_BLOCKS_SUMMARY, summaryBuilder.build());

            inventory.setItem(SLOT_ASSIGN_BLOCKS, new ItemBuilder(Material.WRITABLE_BOOK)
                    .setName(msg("assign_blocks.title"))
                    .addLore(msg("assign_blocks.desc_1"))
                    .addLore(msg("assign_blocks.desc_2"))
                    .addLore("")
                    .addLore(msg("assign_blocks.click"))
                    .glow().build());

            boolean valid = session.hasRequiredRoles();
            if (valid) {
                inventory.setItem(SLOT_VALIDATION, new ItemBuilder(Material.LIME_DYE)
                        .setName(msg("validation.ok_title"))
                        .addLore(msg("validation.ok_desc_1"))
                        .addLore(msg("validation.ok_desc_2"))
                        .build());
            } else {
                var vb = new ItemBuilder(Material.RED_DYE)
                        .setName(msg("validation.fail_title"))
                        .addLore(msg("validation.fail_missing"));
                for (SmelteryBlockType missing : session.getMissingRoles()) {
                    vb.addLore("<red> • " + getTypeName(missing));
                }
                vb.addLore("");
                vb.addLore(msg("validation.click_receive"));
                inventory.setItem(SLOT_VALIDATION, vb.build());
            }
        }

        boolean canConfirm = scanned && session.hasRequiredRoles();
        inventory.setItem(SLOT_CONFIRM, new ItemBuilder(canConfirm ? Material.LIME_WOOL : Material.GRAY_WOOL)
                .setName(canConfirm ? msg("confirm.ready") : msg("confirm.not_ready"))
                .addLore(canConfirm ? msg("confirm.ready_desc") : msg("confirm.not_ready_desc"))
                .build());

        inventory.setItem(SLOT_CANCEL, new ItemBuilder(Material.RED_WOOL)
                .setName(msg("cancel.title"))
                .addLore(msg("cancel.desc"))
                .build());

        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .setName(msg("close.title"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker)) { return; }

        var mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
        int slot = event.getRawSlot();

        switch (slot) {
            case SLOT_NAME -> {
                clicker.closeInventory();
                clicker.sendMessage(mm.deserialize(ProfessionsModule.getInstance().getMessage("smeltery.admin.enter_template_name")));
                if (onSetName != null) { onSetName.accept(clicker); }
            }
            case SLOT_TIER -> {
                SmelteryTier current = session.getTier();
                SmelteryTier[] tiers = SmelteryTier.values();
                int idx = current.ordinal();
                if (event.isLeftClick()) {
                    idx = (idx + 1) % tiers.length;
                } else if (event.isRightClick()) {
                    idx = (idx - 1 + tiers.length) % tiers.length;
                }
                session.setTier(tiers[idx]);
                initializeItems();
            }
            case SLOT_LEVEL -> {
                int delta = event.isShiftClick() ? 10 : 1;
                if (event.isRightClick()) { delta = -delta; }
                int newLevel = Math.max(1, Math.min(100, session.getRequiredLevel() + delta));
                session.setRequiredLevel(newLevel);
                initializeItems();
            }
            case SLOT_POS1 -> {
                session.setPos1(clicker.getLocation().toBlockLocation());
                clicker.sendMessage(mm.deserialize(
                        ProfessionsModule.getInstance().getMessage("smeltery.admin.pos1_set").replace("%loc%", formatLoc(session.getPos1()))));
                session.startVisualization(clicker);
                initializeItems();
            }
            case SLOT_POS2 -> {
                if (session.getPos1() != null && !clicker.getWorld().equals(session.getPos1().getWorld())) {
                    clicker.sendMessage(mm.deserialize(
                            ProfessionsModule.getInstance().getMessage("smeltery.admin.different_worlds")));
                    return;
                }
                session.setPos2(clicker.getLocation().toBlockLocation());
                clicker.sendMessage(mm.deserialize(
                        ProfessionsModule.getInstance().getMessage("smeltery.admin.pos2_set").replace("%loc%", formatLoc(session.getPos2()))));
                session.startVisualization(clicker);
                initializeItems();
            }
            case SLOT_SCAN -> {
                if (session.hasBothPositions()) {
                    int count = session.scanArea();
                    clicker.sendMessage(mm.deserialize(
                            ProfessionsModule.getInstance().getMessage("smeltery.admin.area_scanned").replace("%count%", String.valueOf(count))));
                    session.startVisualization(clicker);
                    initializeItems();
                }
            }
            case SLOT_ASSIGN_BLOCKS -> {
                if (!session.isScanned()) { return; }
                clicker.closeInventory();
                if (onOpenBlockAssign != null) { onOpenBlockAssign.accept(clicker); }
            }
            case SLOT_VALIDATION -> {
                if (!session.isScanned() || session.hasRequiredRoles()) { return; }
                var missing = session.getMissingRoles();
                for (SmelteryBlockType type : missing) {
                    Material mat = getRepresentativeMaterial(type);
                    if (mat != null && !mat.isAir()) {
                        clicker.getInventory().addItem(new ItemStack(mat, 1));
                    }
                }
                clicker.sendMessage(mm.deserialize(
                        ProfessionsModule.getInstance().getMessage("smeltery.admin.missing_blocks_given").replace("%count%", String.valueOf(missing.size()))));
            }
            case SLOT_CONFIRM -> {
                if (session.isScanned() && session.hasRequiredRoles()) {
                    clicker.closeInventory();
                    if (onConfirm != null) { onConfirm.accept(clicker); }
                }
            }
            case SLOT_CANCEL -> {
                clicker.closeInventory();
                if (onCancel != null) { onCancel.accept(clicker); }
            }
            case SLOT_CLOSE -> clicker.closeInventory();
        }
    }

    // === Callbacks ===

    public void setOnOpenBlockAssign(Consumer<Player> cb) { this.onOpenBlockAssign = cb; }
    public void setOnConfirm(Consumer<Player> cb) { this.onConfirm = cb; }
    public void setOnCancel(Consumer<Player> cb) { this.onCancel = cb; }
    public void setOnSetName(Consumer<Player> cb) { this.onSetName = cb; }

    // === Helpers ===

    private String formatLoc(org.bukkit.Location loc) {
        if (loc == null) { return msg("location.undefined"); }
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    static String getTypeName(SmelteryBlockType type) {
        ProfessionsModule mod = ProfessionsModule.getInstance();
        return switch (type) {
            case WALL -> mod.getMessage("gui.smeltery_setup.type.wall");
            case CONTROLLER -> mod.getMessage("gui.smeltery_setup.type.controller");
            case DRAIN -> mod.getMessage("gui.smeltery_setup.type.drain");
            case TANK_WINDOW -> mod.getMessage("gui.smeltery_setup.type.tank_window");
            case ITEM_INPUT -> mod.getMessage("gui.smeltery_setup.type.item_input");
            case FUEL_INPUT -> mod.getMessage("gui.smeltery_setup.type.fuel_input");
            case AIR -> mod.getMessage("gui.smeltery_setup.type.air");
            case CASTING_TABLE -> mod.getMessage("gui.smeltery_setup.type.casting_table");
            case CASTING_BASIN -> mod.getMessage("gui.smeltery_setup.type.casting_basin");
        };
    }

    private static Material getRepresentativeMaterial(SmelteryBlockType type) {
        return switch (type) {
            case CONTROLLER -> Material.BLAST_FURNACE;
            case DRAIN -> Material.HOPPER;
            case TANK_WINDOW -> Material.TINTED_GLASS;
            case ITEM_INPUT -> Material.DROPPER;
            case FUEL_INPUT -> Material.BARREL;
            case CASTING_TABLE -> Material.SMOOTH_STONE_SLAB;
            case CASTING_BASIN -> Material.CAULDRON;
            case WALL -> Material.NETHER_BRICKS;
            default -> null;
        };
    }
}

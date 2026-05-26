package me.ray.midgard.modules.races.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.model.Race;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * GUI Admin de Jogador Específico
 * Permite setar raça, level, XP e resetar
 * Layout: 6 linhas (54 slots) com separadores visuais
 *
 * Linha 0: Cabeça do alvo (centro)
 * Linha 1: Raça info | Definir Raça | Resetar Raça
 * Linha 2: ──── Separador roxo ────
 * Linha 3: [Level -] [Level Info] [Level +]  ║  [XP -] [XP Info] [XP +]
 * Linha 4: ──── Separador roxo ────
 * Linha 5: Voltar | | | | | | | Fechar
 */
public class RaceAdminPlayerGui extends BaseGui {

    private final RacesModule module;
    private final Player target;
    private final BaseGui parentGui;

    // Linha 0: Header
    private static final int SLOT_TARGET_HEAD = 4;

    // Linha 1: Ações de raça (espaçadas)
    private static final int SLOT_RACE_ICON = 10;
    private static final int SLOT_SET_RACE = 13;
    private static final int SLOT_RESET_RACE = 16;

    // Linha 2: Separador (18-26) — preenchido automaticamente

    // Linha 3: Controles Level (esquerda) e XP (direita)
    private static final int SLOT_LEVEL_DOWN = 28;
    private static final int SLOT_LEVEL_INFO = 29;
    private static final int SLOT_LEVEL_UP = 30;
    private static final int SLOT_XP_DOWN = 32;
    private static final int SLOT_XP_INFO = 33;
    private static final int SLOT_XP_UP = 34;

    // Linha 4: Separador (36-44) — preenchido automaticamente

    // Linha 5: Navegação
    private static final int SLOT_BACK = 45;
    private static final int SLOT_CLOSE = 53;

    public RaceAdminPlayerGui(Player admin, Player target, BaseGui parentGui) {
        super(admin, 6, getTitle(target));
        this.module = RacesModule.getInstance();
        this.target = target;
        this.parentGui = parentGui;
    }

    private static String getTitle(Player target) {
        return RacesModule.getInstance().getGuiMessage("admin_player.title")
                .replace("%player%", target.getName());
    }

    private String gui(String key) {
        return module.getGuiMessage("admin_player." + key);
    }

    private RaceData getTargetData() {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(target);
        if (profile == null) { return null; }
        return profile.getData(RaceData.class);
    }

    @Override
    public void initializeItems() {
        inventory.clear();

        // Decoração base
        var darkPane = RaceGuiTheme.createDarkPane();
        var separator = RaceGuiTheme.createPane(Material.PURPLE_STAINED_GLASS_PANE, " ");

        // Linha 0: panes + cabeça
        RaceGuiTheme.fillSlots(inventory, darkPane, 0, 1, 2, 3, 5, 6, 7, 8);
        // Linha 1: panes entre itens
        RaceGuiTheme.fillSlots(inventory, darkPane, 9, 11, 12, 14, 15, 17);
        // Linha 2: separador roxo completo
        for (int i = 18; i <= 26; i++) {
            inventory.setItem(i, separator);
        }
        // Linha 3: panes entre controles
        RaceGuiTheme.fillSlots(inventory, darkPane, 27, 31, 35);
        // Linha 4: separador roxo completo
        for (int i = 36; i <= 44; i++) {
            inventory.setItem(i, separator);
        }
        // Linha 5: panes
        RaceGuiTheme.fillSlots(inventory, darkPane, 46, 47, 48, 49, 50, 51, 52);

        // ── Header ──
        inventory.setItem(SLOT_TARGET_HEAD, createTargetHead());

        RaceData data = getTargetData();
        boolean hasRace = data != null && data.hasRace();

        // ── Ações de Raça ──
        inventory.setItem(SLOT_RACE_ICON, createRaceIcon(data));
        inventory.setItem(SLOT_SET_RACE, createSetRaceItem());
        inventory.setItem(SLOT_RESET_RACE, createResetRaceItem(hasRace));

        // ── Controles de Level ──
        inventory.setItem(SLOT_LEVEL_DOWN, createLevelButton(hasRace, false));
        inventory.setItem(SLOT_LEVEL_INFO, createLevelInfoItem(data));
        inventory.setItem(SLOT_LEVEL_UP, createLevelButton(hasRace, true));

        // ── Controles de XP ──
        inventory.setItem(SLOT_XP_DOWN, createXpButton(hasRace, false));
        inventory.setItem(SLOT_XP_INFO, createXpInfoItem(data));
        inventory.setItem(SLOT_XP_UP, createXpButton(hasRace, true));

        // ── Navegação ──
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .setName(module.getGuiMessage("general.back"))
                .build());
        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .setName(module.getGuiMessage("general.close"))
                .build());
    }

    // ═══════════════════════════════════════════
    //  CRIAÇÃO DE ITENS
    // ═══════════════════════════════════════════

    private ItemStack createTargetHead() {
        RaceData data = getTargetData();
        String raceName = module.getGuiMessage("general.no_race_name");
        int level = 0;
        double xp = 0;

        if (data != null && data.hasRace()) {
            Race race = module.getRaceManager().getRace(data.getRaceId());
            raceName = (race != null) ? race.getDisplayName() : data.getRaceId();
            level = data.getLevel();
            xp = data.getExperience();
        }

        return new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(target)
                .setName(gui("target.name").replace("%player%", target.getName()))
                .setLoreMultiline(gui("target.lore")
                        .replace("%race%", raceName)
                        .replace("%level%", String.valueOf(level))
                        .replace("%xp%", String.format("%.0f", xp)))
                .build();
    }

    private ItemStack createRaceIcon(RaceData data) {
        if (data == null || !data.hasRace()) {
            return new ItemBuilder(Material.GRAY_DYE)
                    .setName(gui("race_icon.no_race"))
                    .setLoreMultiline(gui("race_icon.no_race_lore"))
                    .build();
        }

        Race race = module.getRaceManager().getRace(data.getRaceId());
        if (race == null) {
            return new ItemBuilder(Material.BARRIER)
                    .setName("<red>" + data.getRaceId())
                    .build();
        }

        return new ItemBuilder(race.getIcon())
                .setName(gui("race_icon.name").replace("%race%", race.getDisplayName()))
                .setLoreMultiline(gui("race_icon.lore")
                        .replace("%id%", race.getId())
                        .replace("%level%", String.valueOf(data.getLevel()))
                        .replace("%xp%", String.format("%.0f", data.getExperience())))
                .build();
    }

    private ItemStack createSetRaceItem() {
        return new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(gui("set_race.name"))
                .setLoreMultiline(gui("set_race.lore"))
                .build();
    }

    private ItemStack createResetRaceItem(boolean hasRace) {
        Material mat = hasRace ? Material.TNT : Material.GRAY_DYE;
        return new ItemBuilder(mat)
                .setName(gui("reset_race.name"))
                .setLoreMultiline(hasRace ? gui("reset_race.lore") : gui("reset_race.lore_disabled"))
                .build();
    }

    private ItemStack createLevelButton(boolean hasRace, boolean isUp) {
        if (isUp) {
            Material mat = hasRace ? Material.LIME_DYE : Material.GRAY_DYE;
            return new ItemBuilder(mat)
                    .setName(gui("level_up.name"))
                    .setLoreMultiline(gui("level_up.lore"))
                    .build();
        } else {
            Material mat = hasRace ? Material.RED_DYE : Material.GRAY_DYE;
            return new ItemBuilder(mat)
                    .setName(gui("level_down.name"))
                    .setLoreMultiline(gui("level_down.lore"))
                    .build();
        }
    }

    private ItemStack createLevelInfoItem(RaceData data) {
        int level = (data != null && data.hasRace()) ? data.getLevel() : 0;
        int maxLevel = module.getLevelManager().getMaxLevel();
        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(gui("level_info.name")
                        .replace("%level%", String.valueOf(level))
                        .replace("%max%", String.valueOf(maxLevel)))
                .setLoreMultiline(gui("level_info.lore")
                        .replace("%level%", String.valueOf(level))
                        .replace("%max%", String.valueOf(maxLevel)))
                .glow()
                .build();
    }

    private ItemStack createXpButton(boolean hasRace, boolean isUp) {
        if (isUp) {
            Material mat = hasRace ? Material.LIME_DYE : Material.GRAY_DYE;
            return new ItemBuilder(mat)
                    .setName(gui("xp_up.name"))
                    .setLoreMultiline(gui("xp_up.lore"))
                    .build();
        } else {
            Material mat = hasRace ? Material.RED_DYE : Material.GRAY_DYE;
            return new ItemBuilder(mat)
                    .setName(gui("xp_down.name"))
                    .setLoreMultiline(gui("xp_down.lore"))
                    .build();
        }
    }

    private ItemStack createXpInfoItem(RaceData data) {
        double xp = 0;
        double required = 100;
        int level = 1;

        if (data != null && data.hasRace()) {
            xp = data.getExperience();
            level = data.getLevel();
            required = module.getLevelManager().getRequiredExperience(level);
        }

        String bar = RaceGuiTheme.progressBar((required > 0 ? (xp / required) * 100 : 0), 10);

        return new ItemBuilder(Material.GOLD_INGOT)
                .setName(gui("xp_info.name")
                        .replace("%xp%", String.format("%.0f", xp))
                        .replace("%required%", String.format("%.0f", required)))
                .setLoreMultiline(gui("xp_info.lore")
                        .replace("%xp%", String.format("%.0f", xp))
                        .replace("%required%", String.format("%.0f", required))
                        .replace("%bar%", bar))
                .glow()
                .build();
    }

    // ═══════════════════════════════════════════
    //  MANIPULAÇÃO DIRETA DE DADOS
    // ═══════════════════════════════════════════

    private void adminSetLevel(RaceData data, int newLevel) {
        int clamped = Math.max(1, Math.min(newLevel, module.getLevelManager().getMaxLevel()));
        data.setLevel(clamped);
        saveTargetProfile();
    }

    private void adminSetXp(RaceData data, double amount) {
        double safe = Math.max(0, amount);
        if (Double.isNaN(safe) || Double.isInfinite(safe)) { return; }
        data.setExperience(safe);
        module.getLevelManager().checkLevelUp(target, data);
        saveTargetProfile();
    }

    private void saveTargetProfile() {
        me.ray.midgard.core.profile.MidgardProfile profile = MidgardCore.getProfileManager().getProfile(target);
        if (profile != null) {
            MidgardCore.getProfileManager().saveProfile(profile);
        }
    }

    private void refresh() {
        initializeItems();
    }

    // ═══════════════════════════════════════════
    //  EVENT HANDLER
    // ═══════════════════════════════════════════

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event == null) { return; }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) { return; }
        if (!player.equals(event.getWhoClicked())) { return; }

        if (!player.hasPermission("midgard.admin.race")) {
            MessageUtils.send(player, module.getMessage("command.no_permission"));
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) { return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) { return; }

        if (!target.isOnline()) {
            MessageUtils.send(player, module.getMessage("command.player_not_found")
                    .replace("%player%", target.getName()));
            player.closeInventory();
            return;
        }

        RaceData data = getTargetData();
        if (data == null) { return; }

        boolean hasRace = data.hasRace();
        ClickType click = event.getClick();

        try {
            switch (slot) {
                // ── Ações de Raça ──
                case SLOT_SET_RACE -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new RaceAdminSetRaceGui(player, target, this).open();
                }
                case SLOT_RESET_RACE -> {
                    if (hasRace) {
                        module.getRaceManager().resetRace(target);
                        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);
                        MessageUtils.send(player, module.getMessage("command.reset_success")
                                .replace("%player%", target.getName()));
                        refresh();
                    }
                }

                // ── Controles de Level ──
                case SLOT_LEVEL_DOWN -> {
                    if (hasRace) {
                        int amount = click.isShiftClick() ? 5 : 1;
                        adminSetLevel(data, data.getLevel() - amount);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                        refresh();
                    }
                }
                case SLOT_LEVEL_INFO -> {
                    if (hasRace) {
                        if (click.isRightClick() && click.isShiftClick()) {
                            // Shift+Right: Set Max
                            adminSetLevel(data, module.getLevelManager().getMaxLevel());
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                        } else if (click.isRightClick()) {
                            // Right: Set 1
                            adminSetLevel(data, 1);
                            adminSetXp(data, 0);
                            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        }
                        refresh();
                    }
                }
                case SLOT_LEVEL_UP -> {
                    if (hasRace) {
                        int amount = click.isShiftClick() ? 5 : 1;
                        adminSetLevel(data, data.getLevel() + amount);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                        refresh();
                    }
                }

                // ── Controles de XP ──
                case SLOT_XP_DOWN -> {
                    if (hasRace) {
                        double amount = click.isShiftClick() ? 1000 : 100;
                        adminSetXp(data, data.getExperience() - amount);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                        refresh();
                    }
                }
                case SLOT_XP_INFO -> {
                    if (hasRace) {
                        if (click.isRightClick() && click.isShiftClick()) {
                            // Shift+Right: Fill XP
                            double required = module.getLevelManager().getRequiredExperience(data.getLevel());
                            adminSetXp(data, required - 1);
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        } else if (click.isRightClick()) {
                            // Right: Zero XP
                            adminSetXp(data, 0);
                            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
                        }
                        refresh();
                    }
                }
                case SLOT_XP_UP -> {
                    if (hasRace) {
                        double amount = click.isShiftClick() ? 1000 : 100;
                        adminSetXp(data, data.getExperience() + amount);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                        refresh();
                    }
                }

                // ── Navegação ──
                case SLOT_BACK -> {
                    if (parentGui != null) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        parentGui.open();
                    }
                }
                case SLOT_CLOSE -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                    player.closeInventory();
                }
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error(
                    "Erro no RaceAdminPlayerGui para %s gerenciando %s no slot %d",
                    player.getName(), target.getName(), slot, e);
            player.closeInventory();
        }
    }
}

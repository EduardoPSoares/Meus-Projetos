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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * GUI de Confirmação de Escolha de Raça
 * Design: Hypixel/Wynncraft Premium Style
 * Layout: 3 linhas (27 slots) - Modal compacto
 */
public class RaceConfirmationGui extends BaseGui {

    private final RacesModule module;
    private final Race race;
    private final BaseGui parentGui;
    private final MidgardProfile profile;
    private final RaceData raceData;

    // Layout 3 linhas (27 slots)
    // Linha 1: Decoração
    // Linha 2: Cancelar | Ícone | Confirmar
    // Linha 3: Decoração
    private static final int SLOT_CANCEL = 11;
    private static final int SLOT_RACE_ICON = 13;
    private static final int SLOT_CONFIRM = 15;

    public RaceConfirmationGui(Player player, Race race, BaseGui parentGui) {
        super(player, 3, getTitle());
        if (race == null) {
            throw new IllegalArgumentException("race cannot be null");
        }
        this.module = RacesModule.getInstance();
        this.race = race;
        this.parentGui = parentGui;
        this.profile = MidgardCore.getProfileManager().getProfile(player);
        this.raceData = (profile != null) ? profile.getData(RaceData.class) : null;
    }

    private static String getTitle() {
        return RacesModule.getInstance().getGuiMessage("confirmation.title");
    }

    private String gui(String key) {
        return module.getGuiMessage("confirmation." + key);
    }

    @Override
    public void initializeItems() {
        inventory.clear();

        // Decoração - preencher tudo com vidro
        var pane = RaceGuiTheme.createDarkPane();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, pane);
        }

        // Ícone da raça
        inventory.setItem(SLOT_RACE_ICON, createRaceIcon());

        // Cancelar
        inventory.setItem(SLOT_CANCEL, createCancelButton());

        // Confirmar
        inventory.setItem(SLOT_CONFIRM, createConfirmButton());
    }

    private ItemStack createRaceIcon() {
        return new ItemBuilder(race.getIcon())
                .setName(gui("race_icon.name").replace("%race%", race.getDisplayName()))
                .setLoreMultiline(gui("race_icon.lore"))
                .glow()
                .build();
    }

    private ItemStack createConfirmButton() {
        return new ItemBuilder(Material.LIME_WOOL)
                .setName(gui("confirm.name"))
                .setLoreMultiline(gui("confirm.lore").replace("%race%", race.getDisplayName()))
                .build();
    }

    private ItemStack createCancelButton() {
        return new ItemBuilder(Material.RED_WOOL)
                .setName(gui("cancel.name"))
                .setLoreMultiline(gui("cancel.lore"))
                .build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event == null) { return; }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) { return; }
        if (!player.equals(event.getWhoClicked())) { return; }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) { return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) { return; }

        try {
            switch (slot) {
                case SLOT_CONFIRM -> confirmSelection();
                case SLOT_CANCEL -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                    if (parentGui != null) {
                        parentGui.open();
                    } else {
                        player.closeInventory();
                    }
                }
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error(
                    "Erro no RaceConfirmationGui para %s no slot %d",
                    player.getName(), slot, e);
            player.closeInventory();
        }
    }

    private void confirmSelection() {
        if (profile == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            MessageUtils.send(player, module.getMessage("command.profile_error"));
            player.closeInventory();
            return;
        }

        RaceData currentData = profile.getOrCreateData(RaceData.class);

        // Caso de evolução (sub-raça)
        if (currentData.hasRace() && race.isSubRace()
                && race.getParentRace().equals(currentData.getRaceId())) {
            boolean success = module.getRaceManager().evolve(player, race);
            if (success) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
            player.closeInventory();
            return;
        }

        // Caso de seleção inicial — já tem raça?
        if (currentData.hasRace()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            MessageUtils.send(player, module.getMessage("command.already_has_race"));
            player.closeInventory();
            return;
        }

        // Validação: raça existe?
        if (module.getRaceManager().getRace(race.getId()) == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            MessageUtils.send(player, module.getMessage("command.race_not_found")
                    .replace("%race%", race.getId()));
            player.closeInventory();
            return;
        }

        // Seleção inicial de raça
        module.getRaceManager().setRace(player, race);

        // Feedback
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);

        player.closeInventory();
    }
}

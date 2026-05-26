package me.ray.midgard.modules.races.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.model.Race;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Menu Principal do Sistema de Raças
 * Design: Hypixel/Wynncraft Premium Style
 * Layout: 5 linhas (45 slots) - Abrangente
 */
public class RaceMainMenuGui extends BaseGui {

    private final RacesModule module;
    private final MidgardProfile profile;
    private final RaceData raceData;

    // Layout 5 linhas (45 slots)
    // Linha 0: Decoração superior
    // Linha 1: Ícone do jogador central
    // Linha 2: Navegação principal (info, atributos, habilidades, progressão)
    // Linha 3: Ações (evolução, admin)
    // Linha 4: Decoração inferior + Fechar
    private static final int SLOT_PLAYER_ICON = 13;

    // Navegação - quando TEM raça
    private static final int SLOT_RACE_INFO = 19;
    private static final int SLOT_ATTRIBUTES = 21;
    private static final int SLOT_ABILITIES = 23;
    private static final int SLOT_PROGRESSION = 25;

    // Linha 3 - Ações
    private static final int SLOT_EVOLUTION = 29;
    private static final int SLOT_ADMIN = 33;

    // Seleção - quando NÃO TEM raça
    private static final int SLOT_SELECT_RACE = 22;

    // Fechar
    private static final int SLOT_CLOSE = 40;

    public RaceMainMenuGui(Player player) {
        super(player, 5, getTitle());
        this.module = RacesModule.getInstance();
        this.profile = MidgardCore.getProfileManager().getProfile(player);
        this.raceData = (profile != null) ? profile.getData(RaceData.class) : null;
    }

    private static String getTitle() {
        return RacesModule.getInstance().getGuiMessage("main_menu.title");
    }

    private String gui(String key) {
        return module.getGuiMessage("main_menu." + key);
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        
        // Decoração
        var pane = RaceGuiTheme.createDarkPane();
        RaceGuiTheme.fillSlots(inventory, pane, 0, 1, 2, 3, 4, 5, 6, 7, 8);
        RaceGuiTheme.fillBottomRow(inventory, pane);
        RaceGuiTheme.fillSlots(inventory, pane, 9, 17, 18, 26, 27, 35);
        
        // Ícone do jogador
        inventory.setItem(SLOT_PLAYER_ICON, createPlayerIcon());
        
        if (raceData != null && raceData.hasRace()) {
            buildWithRaceMenu();
        } else {
            buildNoRaceMenu();
        }
        
        // Admin - só aparece se tiver permissão
        if (player.hasPermission("midgard.admin.race")) {
            inventory.setItem(SLOT_ADMIN, createAdminButton());
        }
        
        // Fechar
        inventory.setItem(SLOT_CLOSE, createCloseButton());
    }

    private void buildNoRaceMenu() {
        // Botão central de seleção
        inventory.setItem(SLOT_SELECT_RACE, createSelectRaceButton());
    }

    private void buildWithRaceMenu() {
        // Linha 2 - Navegação principal
        inventory.setItem(SLOT_RACE_INFO, createRaceInfoButton());
        inventory.setItem(SLOT_ATTRIBUTES, createAttributesButton());
        inventory.setItem(SLOT_ABILITIES, createAbilitiesButton());
        inventory.setItem(SLOT_PROGRESSION, createProgressionButton());
        
        // Linha 3 - Ações
        inventory.setItem(SLOT_EVOLUTION, createEvolutionButton());
    }

    private ItemStack createPlayerIcon() {
        String raceName = module.getGuiMessage("general.no_race_name");
        int level = 1;
        double xp = 0;
        double requiredXp = 100;
        String progressBar = RaceGuiTheme.progressBar(0, 12);

        if (raceData != null && raceData.hasRace()) {
            Race race = module.getRaceManager().getRace(raceData.getRaceId());
            if (race != null) {
                raceName = race.getDisplayName();
            }
            level = raceData.getLevel();
            xp = raceData.getExperience();
            requiredXp = module.getLevelManager().getRequiredExperience(level);
            double percent = (requiredXp > 0) ? (xp / requiredXp) * 100 : 100;
            progressBar = RaceGuiTheme.progressBar(percent, 12);
        }

        String loreKey = (raceData != null && raceData.hasRace()) 
                ? "player_icon.lore_with_race"
                : "player_icon.lore_no_race";

        String lore = gui(loreKey)
                .replace("%race%", raceName)
                .replace("%level%", String.valueOf(level))
                .replace("%xp%", String.format("%.0f", xp))
                .replace("%required%", String.format("%.0f", requiredXp))
                .replace("%bar%", progressBar);

        return new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(player)
                .setName(gui("player_icon.name").replace("%player%", player.getName()))
                .setLoreMultiline(lore)
                .build();
    }

    private ItemStack createSelectRaceButton() {
        return new ItemBuilder(Material.NETHER_STAR)
                .setName(gui("select_race.name"))
                .setLoreMultiline(gui("select_race.lore"))
                .glow()
                .build();
    }

    private ItemStack createRaceInfoButton() {
        Race race = module.getRaceManager().getRace(raceData.getRaceId());
        if (race == null) {
            return new ItemStack(Material.BARRIER);
        }

        return new ItemBuilder(race.getIcon())
                .setName(gui("race_info.name").replace("%race%", race.getDisplayName()))
                .setLoreMultiline(gui("race_info.lore"))
                .build();
    }

    private ItemStack createAttributesButton() {
        Race race = module.getRaceManager().getRace(raceData.getRaceId());
        if (race == null) {
            return new ItemStack(Material.BARRIER);
        }

        StringBuilder attrLines = new StringBuilder();
        Map<String, Double> attrs = race.getAttributes();
        Map<String, Double> perLevel = race.getPerLevelAttributes();
        int level = raceData.getLevel();

        if (attrs != null && !attrs.isEmpty()) {
            for (Map.Entry<String, Double> entry : attrs.entrySet()) {
                double total = entry.getValue();
                if (perLevel != null && perLevel.containsKey(entry.getKey())) {
                    total += perLevel.get(entry.getKey()) * (level - 1);
                }
                String sign = total >= 0 ? "+" : "";
                String color = total >= 0 ? "green" : "red";
                attrLines.append("\n<dark_gray>• <").append(color).append(">")
                         .append(sign).append(RaceGuiTheme.formatValue(total))
                         .append(" <gray>").append(module.getAttributeName(entry.getKey()));
            }
        }

        String attrText = attrLines.length() > 0 ? attrLines.toString() : "\n" + gui("attributes.no_attributes");

        return new ItemBuilder(Material.IRON_CHESTPLATE)
                .setName(gui("attributes.name"))
                .setLoreMultiline(gui("attributes.lore")
                        .replace("%level%", String.valueOf(level))
                        .replace("%attributes%", attrText))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private ItemStack createAbilitiesButton() {
        Race race = module.getRaceManager().getRace(raceData.getRaceId());
        int unlocked = 0;
        int locked = 0;
        
        if (race != null && race.getTraits() != null) {
            unlocked = (int) race.getTraits().stream()
                    .filter(t -> t.getMinLevel() <= raceData.getLevel())
                    .count();
            locked = race.getTraits().size() - unlocked;
        }

        String lore = gui("abilities.lore")
                .replace("%unlocked%", String.valueOf(unlocked))
                .replace("%locked%", String.valueOf(locked));

        return new ItemBuilder(Material.ENCHANTED_BOOK)
                .setName(gui("abilities.name"))
                .setLoreMultiline(lore)
                .build();
    }

    private ItemStack createProgressionButton() {
        int level = raceData.getLevel();
        double xp = raceData.getExperience();
        double required = module.getLevelManager().getRequiredExperience(level);
        double percent = (required > 0) ? (xp / required) * 100 : 100;
        String progressBar = RaceGuiTheme.progressBar(percent, 10);

        String lore = gui("progression.lore")
                .replace("%level%", String.valueOf(level))
                .replace("%xp%", String.format("%.0f", xp))
                .replace("%required%", String.format("%.0f", required))
                .replace("%bar%", progressBar);

        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(gui("progression.name"))
                .setLoreMultiline(lore)
                .build();
    }

    private ItemStack createEvolutionButton() {
        Race race = module.getRaceManager().getRace(raceData.getRaceId());
        int available = 0;
        int locked = 0;
        
        if (race != null) {
            for (Race subRace : module.getRaceManager().getRaces()) {
                if (subRace.isSubRace() && subRace.getParentRace().equals(race.getId())) {
                    if (subRace.getMinLevel() <= raceData.getLevel()) {
                        available++;
                    } else {
                        locked++;
                    }
                }
            }
        }

        boolean hasEvolutions = (available + locked) > 0;
        String loreKey = hasEvolutions ? "evolution.lore_available" : "evolution.lore_none";
        
        String lore = gui(loreKey)
                .replace("%available%", String.valueOf(available))
                .replace("%locked%", String.valueOf(locked));

        return new ItemBuilder(hasEvolutions ? Material.END_CRYSTAL : Material.GRAY_DYE)
                .setName(gui("evolution.name"))
                .setLoreMultiline(lore)
                .glowIf(hasEvolutions && available > 0)
                .build();
    }

    private ItemStack createCloseButton() {
        return new ItemBuilder(Material.BARRIER)
                .setName(module.getGuiMessage("general.close"))
                .build();
    }

    private ItemStack createAdminButton() {
        return new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(gui("admin.name"))
                .setLoreMultiline(gui("admin.lore"))
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
            boolean hasRace = raceData != null && raceData.hasRace();
            
            switch (slot) {
                case SLOT_PLAYER_ICON -> {
                    if (!hasRace) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        new RaceSelectionGui(player, this).open();
                    }
                }
                case SLOT_SELECT_RACE -> {
                    if (!hasRace) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        new RaceSelectionGui(player, this).open();
                    }
                }
                case SLOT_RACE_INFO -> {
                    if (hasRace) {
                        Race race = module.getRaceManager().getRace(raceData.getRaceId());
                        if (race != null) {
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                            new RaceDetailGui(player, race, this).open();
                        }
                    }
                }
                case SLOT_ABILITIES -> {
                    if (hasRace) {
                        Race race = module.getRaceManager().getRace(raceData.getRaceId());
                        if (race != null) {
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                            new RaceAbilitiesGui(player, race, this).open();
                        }
                    }
                }
                case SLOT_PROGRESSION -> {
                    if (hasRace) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        new RaceProgressGui(player, this).open();
                    }
                }
                case SLOT_EVOLUTION -> {
                    if (hasRace) {
                        Race race = module.getRaceManager().getRace(raceData.getRaceId());
                        if (race != null) {
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                            new RaceEvolutionGui(player, race, this).open();
                        }
                    }
                }
                case SLOT_ADMIN -> {
                    if (player.hasPermission("midgard.admin.race")) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        new RaceAdminGui(player).open();
                    }
                }
                case SLOT_CLOSE -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                    player.closeInventory();
                }
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error(
                    "Erro no RaceMainMenuGui para %s no slot %d", 
                    player.getName(), slot, e);
            player.closeInventory();
        }
    }
}

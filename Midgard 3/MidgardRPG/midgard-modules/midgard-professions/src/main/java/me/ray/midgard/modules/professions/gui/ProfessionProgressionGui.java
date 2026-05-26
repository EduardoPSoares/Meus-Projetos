package me.ray.midgard.modules.professions.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.ProfessionData;
import me.ray.midgard.modules.professions.ProfessionProgress;
import me.ray.midgard.modules.professions.ProfessionType;
import me.ray.midgard.modules.professions.ProfessionsModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

/**
 * Menu de progressão de profissão.
 * Exibe até 100 níveis distribuídos em 5 páginas, com header informativo e navegação.
 * Reutilizável para todas as 10 profissões — muda dinamicamente com base no ProfessionType.
 */
public class ProfessionProgressionGui extends BaseGui {

    private final ProfessionType professionType;
    private final ProfessionDefinition definition;
    private int page;
    private final int maxLevel;

    // Dados do jogador (cache local para evitar lookups repetidos)
    private int playerLevel;
    private double playerXp;
    private double playerXpToNext;
    private double playerPercent;

    public ProfessionProgressionGui(Player player, ProfessionType professionType, int page) {
        super(player, 6, buildTitle(professionType, page));
        this.professionType = professionType;
        this.definition = ProfessionDefinition.get(professionType);
        this.page = page;

        // Ler max-level da config (default 100)
        ProfessionsModule module = ProfessionsModule.getInstance();
        this.maxLevel = module != null && module.getConfig() != null
                ? module.getConfig().getInt("professions.max-level", 100)
                : 100;

        loadPlayerData();
    }

    private static String buildTitle(ProfessionType type, int page) {
        return msg("menu.title")
                .replace("%symbol%", type.getSymbol())
                .replace("%profession%", type.getDisplayName())
                .replace("%page%", String.valueOf(page + 1));
    }

    private void loadPlayerData() {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) {
            playerLevel = 0;
            playerXp = 0;
            playerXpToNext = 100;
            playerPercent = 0;
            return;
        }

        ProfessionData profData = profile.getData(ProfessionData.class);
        if (profData == null) {
            playerLevel = 0;
            playerXp = 0;
            playerXpToNext = ProfessionProgress.calculateXpNeeded(1);
            playerPercent = 0;
            return;
        }

        ProfessionProgress progress = profData.getProgress(professionType);
        if (progress != null) {
            playerLevel = progress.getLevel();
            playerXp = progress.getXp();
            playerXpToNext = progress.getXpToNextLevel();
            playerPercent = progress.getProgressPercent();
        } else {
            playerLevel = 0;
            playerXp = 0;
            playerXpToNext = ProfessionProgress.calculateXpNeeded(1);
            playerPercent = 0;
        }
    }

    @Override
    public void initializeItems() {
        inventory.clear();

        renderHeader();
        renderProgressionSlots();
        renderFooter();

        // Preencher slots vazios com vidro preto
        ProfessionGuiTheme.fillEmpty(inventory);
    }

    // ========== HEADER (Linha 0: slots 0-8) ==========

    private void renderHeader() {
        inventory.setItem(ProfessionMenuLayout.SLOT_PROFESSION_ICON, createProfessionIcon());
        inventory.setItem(ProfessionMenuLayout.SLOT_EXTRA_BUTTON, createExtraButton());
    }

    private ItemStack createProfessionIcon() {
        Material icon = definition != null ? definition.menuIcon() : professionType.getIcon();
        String gradient = definition != null ? definition.gradient() : "<gold>";

        String iconName = msg("menu.icon.name")
                .replace("%gradient%", gradient)
                .replace("%symbol%", professionType.getSymbol())
                .replace("%profession%", professionType.getDisplayName());

        ItemBuilder builder = new ItemBuilder(icon)
                .setName(iconName)
                .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        builder.addLore("");

        // Descrição
        if (definition != null) {
            builder.addLore(msg("menu.icon.description_label")
                    .replace("%description%", definition.description()));
            builder.addLore("");
        }

        // Progresso do jogador
        String bar = ProfessionGuiTheme.progressBar(playerPercent, 15, gradient);
        builder.addLore(msg("menu.icon.progress_label"));
        builder.addLore(msg("menu.icon.progress_level")
                .replace("%level%", String.valueOf(playerLevel))
                .replace("%max%", String.valueOf(maxLevel)));
        builder.addLore(msg("menu.icon.progress_bar")
                .replace("%bar%", bar)
                .replace("%percent%", String.format("%.1f", playerPercent)));
        builder.addLore(msg("menu.icon.progress_xp")
                .replace("%current%", ProfessionGuiTheme.formatNumber(playerXp))
                .replace("%max%", ProfessionGuiTheme.formatNumber(playerXpToNext)));
        builder.addLore("");

        // Bônus da profissão
        if (definition != null && !definition.bonuses().isEmpty()) {
            builder.addLore(msg("menu.icon.bonuses_label"));
            for (String bonus : definition.bonuses()) {
                builder.addLore("  " + bonus);
            }
            builder.addLore("");
        }

        return builder.glow().build();
    }

    private ItemStack createExtraButton() {
        if (definition == null) {
            return new ItemBuilder(Material.BOOK)
                    .setName(msg("menu.extra_button.default_name"))
                    .addLore(msg("menu.extra_button.default_lore"))
                    .build();
        }

        Material icon = definition.extraButtonIcon();
        ItemBuilder builder = new ItemBuilder(icon)
                .setName(definition.gradient() + "<bold>" + definition.extraButtonName() + "</bold></gradient>");

        builder.addLore("");
        for (String line : definition.extraButtonLore()) {
            builder.addLore(line);
        }

        return builder.build();
    }

    // ========== ÁREA DE PROGRESSÃO (Linhas 1-4) ==========

    private void renderProgressionSlots() {
        int[] slots = ProfessionMenuLayout.getSlotsForPage(page);
        int levelCount = ProfessionMenuLayout.getLevelCountForPage(page, maxLevel);

        for (int i = 0; i < slots.length; i++) {
            if (i < levelCount) {
                int level = (page * ProfessionMenuLayout.LEVELS_PER_PAGE) + i + 1;
                inventory.setItem(slots[i], createLevelSlot(level));
            }
            // Slots além do levelCount ficam null → serão preenchidos por fillEmpty
        }
    }

    private ItemStack createLevelSlot(int level) {
        Material material = ProfessionGuiTheme.getLevelMaterial(level, playerLevel);
        String status = ProfessionGuiTheme.getLevelStatus(level, playerLevel);

        // Nível atual em progresso → destaque amarelo
        boolean isCurrentLevel = (level == playerLevel + 1);
        boolean isCompleted = (level <= playerLevel);

        ItemBuilder builder = new ItemBuilder(material);

        // Nome do nível
        String levelStr = String.valueOf(level);
        if (isCompleted) {
            builder.setName(msg("menu.level.name_completed")
                    .replace("%gradient_green%", ProfessionGuiTheme.GRADIENT_GREEN)
                    .replace("%level%", levelStr));
        } else if (isCurrentLevel) {
            builder.setName(msg("menu.level.name_in_progress")
                    .replace("%gradient_gold%", ProfessionGuiTheme.GRADIENT_GOLD)
                    .replace("%level%", levelStr));
        } else {
            builder.setName(msg("menu.level.name_locked")
                    .replace("%gradient_red%", ProfessionGuiTheme.GRADIENT_RED)
                    .replace("%level%", levelStr));
        }

        builder.addLore("");

        // Recompensas do nível
        ProfessionLevelReward reward = ProfessionRewardRegistry.getReward(professionType, level);
        if (reward != null && reward.hasLevelName()) {
            builder.addLore("<white>  " + reward.levelName() + "</white>");
            builder.addLore("");
        }

        // Bônus passivos
        if (reward != null && reward.hasPassiveBonuses()) {
            builder.addLore(msg("menu.level.rewards_label"));
            String entryTemplate = msg("menu.level.rewards_entry");
            for (String bonus : reward.passiveBonuses()) {
                builder.addLore(entryTemplate.replace("%bonus%", bonus));
            }
            builder.addLore("");
        }

        // Desbloqueio de habilidade
        if (reward != null && reward.hasAbilityUnlock()) {
            builder.addLore(msg("menu.level.ability_label"));
            builder.addLore(msg("menu.level.ability_entry")
                    .replace("%ability%", reward.abilityUnlock()));
            if (reward.abilityDescription() != null) {
                builder.addLore(msg("menu.level.ability_desc")
                        .replace("%description%", reward.abilityDescription()));
            }
            builder.addLore("");
        }

        // Perks
        if (reward != null && reward.hasPerks()) {
            builder.addLore(msg("menu.level.perks_label"));
            String perkTemplate = msg("menu.level.perks_entry");
            for (String perk : reward.perks()) {
                builder.addLore(perkTemplate.replace("%perk%", perk));
            }
            builder.addLore("");
        }

        // Progresso (apenas para nível atual)
        if (isCurrentLevel && playerLevel < maxLevel) {
            double xpNeeded = ProfessionProgress.calculateXpNeeded(level);
            double percent = xpNeeded > 0 ? (playerXp / xpNeeded) * 100.0 : 0;
            String bar = ProfessionGuiTheme.progressBar(percent, 12);

            builder.addLore(msg("menu.level.progress_label"));
            builder.addLore(msg("menu.level.progress_bar")
                    .replace("%bar%", bar)
                    .replace("%percent%", String.format("%.1f", percent)));
            builder.addLore(msg("menu.level.progress_xp")
                    .replace("%current%", ProfessionGuiTheme.formatNumber(playerXp))
                    .replace("%max%", ProfessionGuiTheme.formatNumber(xpNeeded)));
            builder.addLore("");
        }

        // Status
        builder.addLore(status);

        // Glow para nível completado
        if (isCompleted) {
            builder.glow();
        }

        return builder.build();
    }

    // ========== FOOTER (Linha 5: slots 45-53) ==========

    private void renderFooter() {
        // Fechar (sempre no slot 45)
        inventory.setItem(ProfessionMenuLayout.SLOT_CLOSE,
                new ItemBuilder(Material.BARRIER)
                        .setName(msg("menu.navigation.close_name"))
                        .addLore(msg("menu.navigation.close_lore"))
                        .build());

        // Página anterior (slot 52 — a partir da página 2)
        if (ProfessionMenuLayout.hasPrevPage(page)) {
            inventory.setItem(ProfessionMenuLayout.SLOT_PREV_PAGE,
                    new ItemBuilder(Material.ARROW)
                            .setName(msg("menu.navigation.prev_page_name"))
                            .addLore(msg("menu.navigation.prev_page_lore")
                                    .replace("%page%", String.valueOf(page)))
                            .build());
        }

        // Próxima página (slot 53)
        if (ProfessionMenuLayout.hasNextPage(page, maxLevel)) {
            inventory.setItem(ProfessionMenuLayout.SLOT_NEXT_PAGE,
                    new ItemBuilder(Material.ARROW)
                            .setName(msg("menu.navigation.next_page_name"))
                            .addLore(msg("menu.navigation.next_page_lore")
                                    .replace("%page%", String.valueOf(page + 2)))
                            .build());
        }
    }

    // ========== HELPER ==========

    private static String msg(String key) {
        return ProfessionsModule.getInstance().getMessage("professions." + key);
    }

    // ========== CLICK HANDLER ==========

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) { return; }

        switch (slot) {
            case ProfessionMenuLayout.SLOT_CLOSE -> player.closeInventory();
            case ProfessionMenuLayout.SLOT_PREV_PAGE -> {
                if (ProfessionMenuLayout.hasPrevPage(page)) {
                    new ProfessionProgressionGui(player, professionType, page - 1).open();
                }
            }
            case ProfessionMenuLayout.SLOT_NEXT_PAGE -> {
                if (ProfessionMenuLayout.hasNextPage(page, maxLevel)) {
                    new ProfessionProgressionGui(player, professionType, page + 1).open();
                }
            }
            default -> {
                // Clique em slot de progressão — por enquanto apenas visual
            }
        }
    }
}

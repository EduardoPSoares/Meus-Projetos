package me.ray.midgard.modules.spells.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.data.SpellMilestone;
import me.ray.midgard.modules.spells.data.SpellProfile;
import me.ray.midgard.modules.spells.obj.ScalableAttribute;
import me.ray.midgard.modules.spells.obj.Spell;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Menu Detalhado - Minimalista (Card Simples)
 *
 * ┌───┬───┬───┬───┬───┬───┬───┬───┬───┐
 * │   │   │   │   │   │   │   │   │   │  Row 0  Vazio
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │   │   │   │ICN│   │   │   │   │  Row 1  Spell centralizada
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │   │███│███│███│███│███│   │   │  Row 2  XP Bar (painéis de vidro)
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │ ← │   │   │   │   │   │   │   │   │  Row 3  Voltar no canto inferior esquerdo
 * └───┴───┴───┴───┴───┴───┴───┴───┴───┘
 */
public class SpellUpgradeDetailGUI extends BaseGui {

    private final SpellsModule module;
    private final Spell spell;
    private final SpellProfile profile;
    private static final DecimalFormat DF = new DecimalFormat("0.#");
    private static final DecimalFormat DF_INT = new DecimalFormat("#,##0");

    // Layout - Minimalista
    private static final int ICON_SLOT = 13;
    private static final int XP_BAR_START = 20;
    private static final int XP_BAR_END = 24;
    private static final int BACK_SLOT = 27;

    public SpellUpgradeDetailGUI(Player player, SpellsModule module, Spell spell) {
        super(player, 4, module.getMessage("upgrade_gui.detail.title")
                .replace("%spell%", spell.getDisplayName()));
        this.module = module;
        this.spell = spell;
        this.profile = module.getSpellManager().getProfile(player);
        if (this.profile == null) {
            MessageUtils.sendError(player, module.getMessage("errors.profile_not_loaded"));
            me.ray.midgard.core.utils.Task.syncLater(player, player::closeInventory, 1L);
        }
    }

    @Override
    public void initializeItems() {
        if (profile == null) { return; }
        // Background vazio - só ar
        // Não preenche nada, deixa os slots vazios (mais limpo)

        int level = profile.getSpellLevel(spell.getId());
        int maxLevel = spell.getMaxLevel();
        boolean isMaxLevel = level >= maxLevel;
        boolean mastered = profile.isMastered(spell.getId());

        // Spell Icon - TODA informação aqui
        renderSpellIcon(level, maxLevel, isMaxLevel, mastered);

        // XP Bar - Painéis de vidro
        renderXPBar(level, isMaxLevel);

        // Voltar - canto inferior esquerdo
        inventory.setItem(BACK_SLOT, new ItemBuilder(Material.ARROW)
                .name(MessageUtils.parse(module.getMessage("upgrade_gui.detail.back_button")))
                .build());
    }

    private void renderSpellIcon(int level, int maxLevel, boolean isMaxLevel, boolean mastered) {
        String mat = spell.getIconMaterial(false);
        int md = spell.getIconModelData(false);
        Material fallback = switch (spell.getSpellType()) {
            case PASSIVE  -> Material.BREWING_STAND;
            case COMMON   -> Material.ENCHANTED_BOOK;
            case ULTIMATE -> Material.NETHER_STAR;
        };

        ItemBuilder b = (mat != null && !mat.isEmpty()) ? ItemBuilder.smart(mat) : new ItemBuilder(fallback);
        if (md > 0) { b.customModelData(md); }

        String color = getTypeColor();
        List<String> lore = new ArrayList<>();
        
        // Tipo e Nível
        lore.add(module.getMessage("upgrade_gui.detail.type_level")
                .replace("%type%", typeTag())
                .replace("%level%", String.valueOf(level))
                .replace("%max%", String.valueOf(maxLevel)));
        lore.add("");

        // Lore da spell
        if (spell.getLore() != null && !spell.getLore().isEmpty()) {
            lore.addAll(processLore(spell.getLore(), level));
            lore.add("");
        }

        // Stats compactos
        double cd = spell.getCooldown().calculate(level);
        double mana = spell.getManaCost().calculate(level);
        double stam = spell.getStaminaCost().calculate(level);

        String stats = module.getMessage("main_gui.stats_format")
                .replace("%cd%", DF.format(cd))
                .replace("%mana%", DF.format(mana));
        if (stam > 0) {
            stats += module.getMessage("main_gui.stats_format_stamina")
                    .replace("%stamina%", DF.format(stam));
        }
        lore.add(stats);

        // XP info
        if (!isMaxLevel) {
            double xp = profile.getSpellXP(spell.getId());
            int xpNeeded = module.getSpellManager().getXPManager().getXPForLevel(level + 1);
            double percent = Math.min(100.0, (xp / xpNeeded) * 100.0);
            lore.add(module.getMessage("upgrade_gui.detail.xp_progress")
                    .replace("%xp%", DF_INT.format(xp))
                    .replace("%xp_needed%", DF_INT.format(xpNeeded))
                    .replace("%percent%", DF.format(percent)));
        } else {
            lore.add(module.getMessage("upgrade_gui.detail.max_level_badge"));
        }

        // Marcos
        List<SpellMilestone> milestones = spell.getMilestones();
        if (!milestones.isEmpty()) {
            lore.add("");
            StringBuilder marcosLine = new StringBuilder(module.getMessage("upgrade_gui.detail.milestones_label"));
            for (SpellMilestone m : milestones) {
                boolean achieved = profile.hasMilestone(spell.getId(), m.level());
                marcosLine.append(achieved
                        ? module.getMessage("upgrade_gui.detail.milestone_achieved_dot")
                        : module.getMessage("upgrade_gui.detail.milestone_locked_dot")).append(" ");
            }
            lore.add(marcosLine.toString().trim());
        }

        // Maestria
        if (mastered) {
            lore.add("");
            lore.add(module.getMessage("upgrade_gui.detail.mastery_badge"));
        }

        b.name(MessageUtils.parse(color + "<bold>" + spell.getDisplayName() + "</bold>"))
         .lore(parse(lore));
        if (mastered || spell.isUltimate()) { b.glow(); }
        
        inventory.setItem(ICON_SLOT, b.build());
    }

    private void renderXPBar(int level, boolean isMaxLevel) {
        double xp = profile.getSpellXP(spell.getId());
        int xpNeeded = isMaxLevel ? 1 : module.getSpellManager().getXPManager().getXPForLevel(level + 1);
        double percent = isMaxLevel ? 100.0 : Math.min(100.0, (xp / xpNeeded) * 100.0);
        
        int totalSlots = XP_BAR_END - XP_BAR_START + 1;
        int filledSlots = isMaxLevel ? totalSlots : (int) Math.floor(totalSlots * (percent / 100.0));
        
        // Cores baseadas no tipo
        Material filledMat = switch (spell.getSpellType()) {
            case PASSIVE  -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case COMMON   -> Material.LIME_STAINED_GLASS_PANE;
            case ULTIMATE -> Material.ORANGE_STAINED_GLASS_PANE;
        };
        Material emptyMat = Material.GRAY_STAINED_GLASS_PANE;
        
        String xpText = isMaxLevel ?
                module.getMessage("upgrade_gui.detail.xp_bar_max") :
                module.getMessage("upgrade_gui.detail.xp_bar_percent")
                        .replace("%percent%", DF.format(percent));
        
        for (int i = 0; i < totalSlots; i++) {
            int slot = XP_BAR_START + i;
            boolean filled = i < filledSlots;
            
            inventory.setItem(slot, new ItemBuilder(filled ? filledMat : emptyMat)
                    .name(MessageUtils.parse(xpText))
                    .build());
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == BACK_SLOT) {
            new SpellUpgradeListGUI(player, module).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
    }

    // Utilities
    
    private String getTypeColor() {
        return switch (spell.getSpellType()) {
            case PASSIVE  -> "<aqua>";
            case COMMON   -> "<green>";
            case ULTIMATE -> "<gold>";
        };
    }

    private String typeTag() {
        return switch (spell.getSpellType()) {
            case PASSIVE  -> module.getMessage("main_gui.type_tags.passive");
            case COMMON   -> module.getMessage("main_gui.type_tags.common");
            case ULTIMATE -> module.getMessage("main_gui.type_tags.ultimate");
        };
    }

    private List<Component> parse(List<String> lines) {
        List<Component> out = new ArrayList<>();
        for (String l : lines) { out.add(MessageUtils.parse(l)); }
        return out;
    }

    private List<String> processLore(List<String> raw, int level) {
        if (raw == null) { return new ArrayList<>(); }
        List<String> out = new ArrayList<>();
        Map<String, Object> vars = spell.getVariables();
        
        for (String line : raw) {
            String t = line;
            for (Map.Entry<String, Object> e : vars.entrySet()) {
                String val = e.getValue() instanceof ScalableAttribute sa ? 
                        DF.format(sa.calculate(level)) : e.getValue().toString();
                t = t.replace("%" + e.getKey() + "%", val).replace("{" + e.getKey() + "}", val);
            }
            double cd = spell.getCooldown().calculate(level);
            double mana = spell.getManaCost().calculate(level);
            double stam = spell.getStaminaCost().calculate(level);
            t = t.replace("%cooldown%", DF.format(cd)).replace("{cooldown}", DF.format(cd))
                 .replace("%mana%", DF.format(mana)).replace("{mana}", DF.format(mana))
                 .replace("%stamina%", DF.format(stam)).replace("{stamina}", DF.format(stam))
                 .replace("%cast-time%", DF.format(spell.getCastTime()));
            out.add(t);
        }
        return out;
    }
}

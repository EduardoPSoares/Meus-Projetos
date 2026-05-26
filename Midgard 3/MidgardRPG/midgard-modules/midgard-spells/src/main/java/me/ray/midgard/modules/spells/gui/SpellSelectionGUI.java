package me.ray.midgard.modules.spells.gui;

import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.data.SpellProfile;
import me.ray.midgard.modules.spells.obj.Spell;
import me.ray.midgard.modules.spells.obj.SpellType;
import me.ray.midgard.modules.spells.requirement.SpellRequirement;
import me.ray.midgard.modules.spells.requirement.ClassRequirement;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Menu de seleção de spells — Grimório Limpo (minimalista)
 *
 * ┌───┬───┬───┬───┬───┬───┬───┬───┬───┐
 * │   │   │   │   │ 📖│   │   │   │   │  Row 0  Título
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │   │  Row 1  Grid limpo
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │ 8 │ 9 │10 │11 │12 │13 │14 │   │  Row 2  7 spells/linha
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │15 │16 │17 │18 │19 │20 │21 │   │  Row 3  Sem fundo
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │   │   │   │   │   │   │   │   │  Row 4  Vazio
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │ ← │   │   │   │ ✗ │   │   │   │ → │  Row 5  Nav + Clear
 * └───┴───┴───┴───┴───┴───┴───┴───┴───┘
 */
public class SpellSelectionGUI extends PaginatedGui<Spell> {

    private final SpellsModule module;
    private final SpellProfile profile;
    private final int targetSlot;
    private final boolean isSkillBar;
    private static final DecimalFormat DF = new DecimalFormat("0.#");

    // Layout - Grid limpo 7x3
    private static final int HEADER_SLOT = 4;
    private static final int BACK_SLOT = 45;
    private static final int CLEAR_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    private static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,  // Row 1
            19, 20, 21, 22, 23, 24, 25,  // Row 2
            28, 29, 30, 31, 32, 33, 34   // Row 3
    };

    public SpellSelectionGUI(Player player, SpellsModule module, int targetSlot, boolean isSkillBar) {
        super(player, module.getMessage("spell_selection.title"), filterSpells(player, module));
        this.module = module;
        this.profile = module.getSpellManager().getProfile(player);
        this.targetSlot = targetSlot;
        this.isSkillBar = isSkillBar;
        if (this.profile == null) {
            MessageUtils.sendError(player, module.getMessage("errors.profile_not_loaded"));
            me.ray.midgard.core.utils.Task.syncLater(player, player::closeInventory, 1L);
        }
    }

    private static List<Spell> filterSpells(Player player, SpellsModule module) {
        List<Spell> filtered = new ArrayList<>();
        SpellProfile profile = module.getSpellManager().getProfile(player);

        for (Spell spell : module.getSpellManager().getSpells()) {
            // Filtrar passivas e ultimates
            if (spell.getSpellType() == SpellType.PASSIVE) { continue; }
            if (spell.getSpellType() == SpellType.ULTIMATE) { continue; }

            // Só mostrar spells desbloqueadas
            if (profile != null && !profile.hasSpell(spell.getId())) { continue; }

            // Filtrar por classe
            boolean classMismatch = false;
            for (SpellRequirement req : spell.getRequirements()) {
                if (req instanceof ClassRequirement && !req.check(player)) {
                    classMismatch = true;
                    break;
                }
            }
            if (!classMismatch) { filtered.add(spell); }
        }
        return filtered;
    }

    @Override
    public ItemStack createItem(Spell spell) {
        Map<String, Object> vars = spell.getVariables();
        int level = profile != null ? profile.getSpellLevel(spell.getId()) : 1;

        // Verificar requisitos
        boolean locked = false;
        List<String> failedReqs = new ArrayList<>();
        for (SpellRequirement req : spell.getRequirements()) {
            if (!req.check(player)) {
                locked = true;
                failedReqs.add(req.getFailureMessage());
            }
        }

        // Montar lore minimalista
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Nível
        lore.add(MessageUtils.parse(msg("item.level")
                .replace("%level%", String.valueOf(level))
                .replace("%max%", String.valueOf(spell.getMaxLevel()))));

        // Stats compactos
        double cd = spell.getCooldown().calculate(level);
        double mana = spell.getManaCost().calculate(level);
        lore.add(MessageUtils.parse(msg("item.stats")
                .replace("%cd%", DF.format(cd))
                .replace("%mana%", DF.format(mana))));

        lore.add(Component.empty());

        // Requisitos falhados
        if (!failedReqs.isEmpty()) {
            String failPrefix = module.getMessage("requirements.fail_prefix");
            for (String fail : failedReqs) {
                lore.add(MessageUtils.parse(failPrefix + fail));
            }
            lore.add(Component.empty());
        }

        lore.add(MessageUtils.parse(locked ? msg("item.locked") : msg("item.click_equip")));

        return buildIcon(spell, locked, lore);
    }

    private ItemStack buildIcon(Spell spell, boolean locked, List<Component> lore) {
        String iconMat = spell.getIconMaterial(locked);
        int modelData = spell.getIconModelData(locked);

        ItemBuilder builder;
        if (iconMat != null && !iconMat.isEmpty()) {
            builder = ItemBuilder.smart(iconMat);
        } else {
            builder = new ItemBuilder(locked ? Material.BARRIER : Material.ENCHANTED_BOOK);
        }

        if (modelData > 0) { builder.customModelData(modelData); }

        String nameColor = locked ? "<dark_gray><st>" : "<white>";
        builder.name(MessageUtils.parse(nameColor + spell.getDisplayName()))
                .lore(lore);

        return builder.build();
    }

    @Override
    public void initializeItems() {
        if (profile == null) { return; }
        // Grid limpo - sem background

        // Header decorativo
        inventory.setItem(HEADER_SLOT, new ItemBuilder(Material.ENCHANTED_BOOK)
                .name(MessageUtils.parse(msg("header.name")
                        .replace("%slot%", String.valueOf(targetSlot))))
                .lore(parseList(List.of("", msg("header.lore"))))
                .glow()
                .build());

        // Renderizar spells no grid
        int startIndex = page * GRID_SLOTS.length;
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < items.size()) {
                inventory.setItem(GRID_SLOTS[i], createItem(items.get(itemIndex)));
            }
        }

        // Navegação
        if (page > 0) {
            inventory.setItem(BACK_SLOT, new ItemBuilder(Material.ARROW)
                    .name(MessageUtils.parse(msg("nav.previous")))
                    .build());
        }

        if ((page + 1) * GRID_SLOTS.length < items.size()) {
            inventory.setItem(NEXT_SLOT, new ItemBuilder(Material.ARROW)
                    .name(MessageUtils.parse(msg("nav.next")))
                    .build());
        }

        // Clear slot button
        inventory.setItem(CLEAR_SLOT, new ItemBuilder(Material.BARRIER)
                .name(MessageUtils.parse(msg("clear.name")))
                .lore(parseList(List.of("", msg("clear.lore"))))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Navegação
        if (slot == BACK_SLOT && page > 0) {
            page--;
            initializeItems();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);
            return;
        }

        if (slot == NEXT_SLOT && (page + 1) * GRID_SLOTS.length < items.size()) {
            page++;
            initializeItems();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            return;
        }

        // Clear slot
        if (slot == CLEAR_SLOT) {
            if (isSkillBar) {
                profile.setSkillBarSlot(targetSlot, null);
            } else {
                String seq = module.getSpellManager().getDefaultCombo(targetSlot);
                profile.setComboSlot(targetSlot, seq, null);
            }
            saveSpellProfile();
            MessageUtils.send(player, msg("messages.cleared")
                    .replace("%slot%", String.valueOf(targetSlot)));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            new MainSpellGUI(player, module).open();
            return;
        }

        // Click em spell
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            if (slot == GRID_SLOTS[i]) {
                int itemIndex = page * GRID_SLOTS.length + i;
                if (itemIndex >= 0 && itemIndex < items.size()) {
                    Spell selected = items.get(itemIndex);

                    // Verificar requisitos
                    for (SpellRequirement req : selected.getRequirements()) {
                        if (!req.check(player)) {
                            MessageUtils.send(player, "<red>✗ " + req.getFailureMessage());
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                            return;
                        }
                    }

                    handleEquip(selected);
                }
                return;
            }
        }
    }

    private void handleEquip(Spell spell) {
        if (isSkillBar) {
            profile.setSkillBarSlot(targetSlot, spell.getId());
            MessageUtils.send(player, msg("messages.equipped_skillbar")
                    .replace("%spell%", spell.getDisplayName())
                    .replace("%slot%", String.valueOf(targetSlot)));
        } else {
            String seq = module.getSpellManager().getDefaultCombo(targetSlot);
            profile.setComboSlot(targetSlot, seq, spell.getId());
            MessageUtils.send(player, msg("messages.equipped_combo")
                    .replace("%spell%", spell.getDisplayName())
                    .replace("%combo%", seq));
        }
        saveSpellProfile();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        new MainSpellGUI(player, module).open();
    }

    // ════════════════ Helpers ════════════════

    private void saveSpellProfile() {
        me.ray.midgard.core.profile.MidgardProfile coreProfile = me.ray.midgard.core.MidgardCore.getProfileManager().getProfile(player);
        if (coreProfile != null) {
            me.ray.midgard.core.MidgardCore.getProfileManager().saveProfile(coreProfile);
        }
    }

    private String msg(String key) {
        return module.getMessage("spell_selection." + key);
    }

    private List<Component> parseList(List<String> list) {
        if (list == null) { return new ArrayList<>(); }
        List<Component> out = new ArrayList<>();
        for (String s : list) { out.add(MessageUtils.parse(s)); }
        return out;
    }
}

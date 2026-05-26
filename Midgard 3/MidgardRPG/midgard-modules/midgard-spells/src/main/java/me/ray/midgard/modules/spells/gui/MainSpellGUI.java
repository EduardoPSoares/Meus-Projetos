package me.ray.midgard.modules.spells.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.data.SpellProfile;
import me.ray.midgard.modules.spells.obj.ScalableAttribute;
import me.ray.midgard.modules.spells.obj.Spell;
import me.ray.midgard.modules.spells.obj.SpellType;
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
 * Menu principal de magias — Altar de Poder (minimalista)
 *
 * ┌───┬───┬───┬───┬───┬───┬───┬───┬───┐
 * │   │   │   │   │ ⓘ │   │   │   │   │  Row 0  Info central
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │   │   │   │   │   │   │   │   │  Row 1  Vazio
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │[1]│   │   │ ★ │   │   │[2]│   │  Row 2  Cruz - Spells + Ultimate
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │   │   │   │[3]│   │   │   │   │  Row 3  Spell central
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │[4]│   │   │ ◆ │   │   │[5]│   │  Row 4  Cruz - Spells + Passiva
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │ ⚙ │   │   │   │   │   │   │   │ ↑ │  Row 5  Settings + Upgrade
 * └───┴───┴───┴───┴───┴───┴───┴───┴───┘
 */
public class MainSpellGUI extends BaseGui {

    private final SpellsModule module;
    private final SpellProfile profile;
    private static final DecimalFormat DF = new DecimalFormat("0.#");

    // Layout - Cruz Sagrada Minimalista
    private static final int INFO_SLOT = 4;          // Row 0 centro
    private static final int[] EQUIP = {19, 25, 31, 37, 43}; // Cruz 5 slots
    private static final int ULTIMATE_SLOT = 22;     // Row 2 centro absoluto
    private static final int PASSIVE_SLOT = 40;      // Row 4 centro
    private static final int SETTINGS_SLOT = 45;     // Row 5 esquerda
    private static final int UPGRADE_SLOT = 53;      // Row 5 direita

    public MainSpellGUI(Player player, SpellsModule module) {
        super(player, 6, module.getMessage("main_gui.title"));
        this.module = module;
        this.profile = module.getSpellManager().getProfile(player);
        if (this.profile == null) {
            MessageUtils.sendError(player, module.getMessage("errors.profile_not_loaded"));
            me.ray.midgard.core.utils.Task.syncLater(player, player::closeInventory, 1L);
        }
    }

    @Override
    public void initializeItems() {
        if (profile == null) { return; }
        // Background vazio - minimalismo total (sem itens decorativos)

        renderInfo();
        renderUltimate();
        renderEquipSlots();
        renderPassive();
        renderControls();
    }

    // ───────── Info Button (slot 4 - topo) ─────────

    private void renderInfo() {
        List<String> lore = new ArrayList<>();
        lore.add("");

        // Contar spells por tipo
        int totalSpells = profile.getUnlockedSpells().size();
        int commonCount = 0;
        int ultimateCount = 0;
        int passiveCount = 0;

        for (String id : profile.getUnlockedSpells()) {
            Spell s = module.getSpellManager().getSpell(id);
            if (s != null) {
                if (s.getSpellType() == SpellType.COMMON) { commonCount++; }
                else if (s.getSpellType() == SpellType.ULTIMATE) { ultimateCount++; }
                else if (s.getSpellType() == SpellType.PASSIVE) { passiveCount++; }
            }
        }

        lore.add(module.getMessage("main_gui.info.total_spells")
                .replace("%total%", String.valueOf(totalSpells)));
        lore.add(module.getMessage("main_gui.info.common_count")
                .replace("%count%", String.valueOf(commonCount)));
        lore.add(module.getMessage("main_gui.info.ultimate_count")
                .replace("%count%", String.valueOf(ultimateCount)));
        lore.add(module.getMessage("main_gui.info.passive_count")
                .replace("%count%", String.valueOf(passiveCount)));
        lore.add("");

        String style = profile.getCastingStyle() == SpellProfile.CastingStyle.SKILLBAR
                ? module.getMessage("main_gui.info.style_skillbar")
                : module.getMessage("main_gui.info.style_combo");
        lore.add(module.getMessage("main_gui.info.casting_style")
                .replace("%style%", style));

        inventory.setItem(INFO_SLOT, new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name(MessageUtils.parse(module.getMessage("main_gui.info.name")))
                .lore(parse(lore))
                .glow()
                .build());
    }

    // ───────── Ultimate (slot 22 - centro absoluto) ─────────

    private void renderUltimate() {
        String equippedId = profile.getEquippedUltimate();
        Spell ultimate = equippedId != null ? module.getSpellManager().getSpell(equippedId) : null;
        
        // Verificar se tem ultimates disponíveis
        boolean hasUltimates = false;
        for (String id : profile.getUnlockedSpells()) {
            Spell s = module.getSpellManager().getSpell(id);
            if (s != null && s.getSpellType() == SpellType.ULTIMATE) {
                hasUltimates = true;
                break;
            }
        }
        
        if (ultimate != null) {
            inventory.setItem(ULTIMATE_SLOT, ultimateItem(ultimate));
        } else if (hasUltimates) {
            inventory.setItem(ULTIMATE_SLOT, emptyUltimateSlot());
        } else {
            inventory.setItem(ULTIMATE_SLOT, lockedItem("ultimate"));
        }
    }

    private ItemStack ultimateItem(Spell spell) {
        int lvl = profile.getSpellLevel(spell.getId());
        List<String> lore = new ArrayList<>();
        lore.add(module.getMessage("main_gui.items.ultimate.lore_tag"));
        lore.add("");
        if (spell.getLore() != null) { lore.addAll(processLore(spell.getLore(), spell, lvl)); }
        lore.add("");
        addStats(lore, spell, lvl);
        lore.add("");
        lore.add(module.getMessage("main_gui.items.ultimate.click_to_swap"));
        return spellIcon(spell, Material.NETHER_STAR, lore, true);
    }

    private ItemStack emptyUltimateSlot() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(MessageUtils.parse(module.getMessage("main_gui.items.ultimate.empty_name")))
                .lore(parse(List.of(module.getMessage("main_gui.items.ultimate.empty_lore"))))
                .build();
    }

    // ───────── Passiva (slot 40 - base) ─────────

    private void renderPassive() {
        Spell passive = null;
        for (String id : profile.getUnlockedSpells()) {
            Spell s = module.getSpellManager().getSpell(id);
            if (s != null && s.getSpellType() == SpellType.PASSIVE) {
                passive = s;
                break;
            }
        }
        inventory.setItem(PASSIVE_SLOT, passive == null
                ? lockedItem("passive") : passiveItem(passive));
    }

    private ItemStack passiveItem(Spell spell) {
        int lvl = profile.getSpellLevel(spell.getId());
        List<String> lore = new ArrayList<>();
        lore.add(module.getMessage("main_gui.items.passive.lore_tag"));
        lore.add("");
        if (spell.getLore() != null) { lore.addAll(processLore(spell.getLore(), spell, lvl)); }
        lore.add("");
        lore.add(module.getMessage("main_gui.items.passive.always_active"));
        return spellIcon(spell, Material.BREWING_STAND, lore, true);
    }

    private ItemStack lockedItem(String type) {
        String name = type.equals("ultimate")
                ? module.getMessage("main_gui.items.locked.name_ultimate")
                : module.getMessage("main_gui.items.locked.name_passive");
        return new ItemBuilder(Material.COAL)
                .name(MessageUtils.parse(name))
                .lore(parse(List.of(module.getMessage("main_gui.items.locked.lore"))))
                .build();
    }

    // ───────── Equip Slots (slots 19, 25, 37, 43 - cruz) ─────────

    private void renderEquipSlots() {
        boolean skillbar = profile.getCastingStyle() == SpellProfile.CastingStyle.SKILLBAR;
        for (int i = 0; i < EQUIP.length; i++) {
            int slot = i + 1;
            if (skillbar) {
                inventory.setItem(EQUIP[i], skillbarItem(slot, profile.getSkillInSlot(slot)));
            } else {
                String seq = module.getSpellManager().getDefaultCombo(slot);
                SpellProfile.ComboBinding b = profile.getComboSlot(slot);
                String id = b != null ? b.getSpellId() : null;
                inventory.setItem(EQUIP[i], comboItem(slot, id, seq));
            }
        }
    }

    private ItemStack skillbarItem(int num, String spellId) {
        if (spellId == null) { return emptySlot(num); }

        Spell spell = module.getSpellManager().getSpell(spellId);
        if (spell == null) { return emptySlot(num); }

        List<String> lore = new ArrayList<>();
        lore.add(module.getMessage("main_gui.items.common.skillbar_tag")
                .replace("%slot%", String.valueOf(num)));
        lore.add("");
        addStats(lore, spell, profile.getSpellLevel(spell.getId()));
        lore.add("");
        lore.add(module.getMessage("main_gui.items.common.click_to_swap"));
        return spellIcon(spell, Material.ENCHANTED_BOOK, lore, false);
    }

    private ItemStack comboItem(int num, String spellId, String seq) {
        if (spellId == null) { return emptySlot(num); }

        Spell spell = module.getSpellManager().getSpell(spellId);
        if (spell == null) { return emptySlot(num); }

        List<String> lore = new ArrayList<>();
        lore.add(module.getMessage("main_gui.items.common.combo_tag")
                .replace("%combo%", seq));
        lore.add("");
        addStats(lore, spell, profile.getSpellLevel(spell.getId()));
        lore.add("");
        lore.add(module.getMessage("main_gui.items.common.click_to_swap"));
        return spellIcon(spell, Material.ENCHANTED_BOOK, lore, false);
    }

    private ItemStack emptySlot(int num) {
        return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .name(MessageUtils.parse(module.getMessage("main_gui.items.empty_slot.name")
                        .replace("%slot%", String.valueOf(num))))
                .lore(parse(List.of(module.getMessage("main_gui.items.empty_slot.lore"))))
                .build();
    }

    // ───────── Controles (Row 5) ─────────

    private void renderControls() {
        // Settings button (combina style + outras opções)
        boolean sb = profile.getCastingStyle() == SpellProfile.CastingStyle.SKILLBAR;
        List<String> settingsLore = new ArrayList<>();
        settingsLore.add("");
        settingsLore.add(module.getMessage("main_gui.settings.current_style")
                .replace("%style%", sb
                        ? module.getMessage("main_gui.settings.style_skillbar")
                        : module.getMessage("main_gui.settings.style_combo")));
        settingsLore.add("");
        settingsLore.add(module.getMessage("main_gui.settings.click_to_toggle"));

        inventory.setItem(SETTINGS_SLOT, new ItemBuilder(Material.COMPARATOR)
                .name(MessageUtils.parse(module.getMessage("main_gui.settings.name")))
                .lore(parse(settingsLore))
                .build());

        // Upgrade button
        inventory.setItem(UPGRADE_SLOT, new ItemBuilder(Material.ANVIL)
                .name(MessageUtils.parse(module.getMessage("main_gui.upgrade.name")))
                .lore(parse(List.of("", module.getMessage("main_gui.upgrade.lore"))))
                .build());
    }

    // ───────── Click Handler ─────────

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();

        // Info button (apenas visual, sem ação)
        if (slot == INFO_SLOT) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            return;
        }

        // Upgrade menu
        if (slot == UPGRADE_SLOT) {
            new SpellUpgradeListGUI(player, module).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            return;
        }

        // Settings (toggle style)
        if (slot == SETTINGS_SLOT) {
            boolean wasSkillbar = profile.getCastingStyle() == SpellProfile.CastingStyle.SKILLBAR;
            profile.setCastingStyle(wasSkillbar ? SpellProfile.CastingStyle.COMBO : SpellProfile.CastingStyle.SKILLBAR);
            saveSpellProfile();
            player.playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, 1f, wasSkillbar ? 0.8f : 1.2f);
            initializeItems();
            return;
        }

        // Ultimate slot
        if (slot == ULTIMATE_SLOT) {
            new UltimateSelectionGUI(player, module).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        // Equip slots
        for (int i = 0; i < EQUIP.length; i++) {
            if (slot == EQUIP[i]) {
                boolean sb = profile.getCastingStyle() == SpellProfile.CastingStyle.SKILLBAR;
                new SpellSelectionGUI(player, module, i + 1, sb).open();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                return;
            }
        }
    }

    // ───────── Util ─────────

    private void saveSpellProfile() {
        me.ray.midgard.core.profile.MidgardProfile coreProfile = me.ray.midgard.core.MidgardCore.getProfileManager().getProfile(player);
        if (coreProfile != null) {
            me.ray.midgard.core.MidgardCore.getProfileManager().saveProfile(coreProfile);
        }
    }

    private void addStats(List<String> lore, Spell spell, int level) {
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
    }

    private ItemStack spellIcon(Spell spell, Material fallback, List<String> lore, boolean glow) {
        String mat = spell.getIconMaterial(false);
        int md = spell.getIconModelData(false);
        ItemBuilder b = (mat != null && !mat.isEmpty()) ? ItemBuilder.smart(mat) : new ItemBuilder(fallback);
        if (md > 0) { b.customModelData(md); }
        String color = switch (spell.getSpellType()) {
            case PASSIVE  -> module.getMessage("main_gui.spell_color.passive");
            case COMMON   -> module.getMessage("main_gui.spell_color.common");
            case ULTIMATE -> module.getMessage("main_gui.spell_color.ultimate");
        };
        b.name(MessageUtils.parse(color + spell.getDisplayName()))
         .lore(parse(lore));
        if (glow) { b.glow(); }
        return b.build();
    }

    private List<net.kyori.adventure.text.Component> parse(List<String> lines) {
        List<net.kyori.adventure.text.Component> out = new ArrayList<>();
        for (String l : lines) { out.add(MessageUtils.parse(l)); }
        return out;
    }

    private List<String> processLore(List<String> raw, Spell spell, int level) {
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

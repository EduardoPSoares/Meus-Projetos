package me.ray.midgard.modules.spells.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.data.SpellProfile;
import me.ray.midgard.modules.spells.obj.ScalableAttribute;
import me.ray.midgard.modules.spells.obj.Spell;
import me.ray.midgard.modules.spells.obj.SpellType;
import me.ray.midgard.modules.spells.requirement.SpellRequirement;
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
 * Menu de seleção de Ultimate — Showcase Individual (minimalista)
 *
 * ┌───┬───┬───┬───┬───┬───┬───┬───┬───┐
 * │   │   │   │   │   │   │   │   │   │  Row 0  Vazio
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │   │   │ < │ ★ │ > │   │   │   │  Row 1  Navegação + Ultimate
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │   │   │   │   │   │   │   │   │  Row 2  Vazio
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │   │   │   │ ✓ │   │   │   │   │  Row 3  Botão equipar
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │   │   │   │   │   │   │   │   │   │  Row 4  Vazio
 * ├───┼───┼───┼───┼───┼───┼───┼───┼───┤
 * │ ← │   │   │   │   │   │   │   │   │  Row 5  Voltar
 * └───┴───┴───┴───┴───┴───┴───┴───┴───┘
 */
public class UltimateSelectionGUI extends BaseGui {

    private final SpellsModule module;
    private final SpellProfile profile;
    private final List<Spell> ultimates;
    private int currentIndex = 0;
    private static final DecimalFormat DF = new DecimalFormat("0.#");

    // Layout - Showcase Individual
    private static final int SHOWCASE_SLOT = 13;      // Row 1 centro
    private static final int PREVIOUS_SLOT = 12;      // Row 1 esquerda
    private static final int NEXT_SLOT = 14;          // Row 1 direita
    private static final int EQUIP_SLOT = 31;         // Row 3 centro
    private static final int BACK_SLOT = 45;          // Row 5 esquerda

    public UltimateSelectionGUI(Player player, SpellsModule module) {
        super(player, 6, module.getMessage("ultimate_gui.title"));
        this.module = module;
        this.profile = module.getSpellManager().getProfile(player);
        
        if (this.profile == null) {
            this.ultimates = new ArrayList<>();
            MessageUtils.sendError(player, module.getMessage("errors.profile_not_loaded"));
            me.ray.midgard.core.utils.Task.syncLater(player, player::closeInventory, 1L);
            return;
        }
        
        this.ultimates = findUnlockedUltimates();

        // Encontrar índice da ultimate equipada (se houver)
        String equippedId = profile.getEquippedUltimate();
        if (equippedId != null) {
            for (int i = 0; i < ultimates.size(); i++) {
                if (ultimates.get(i).getId().equalsIgnoreCase(equippedId)) {
                    currentIndex = i;
                    break;
                }
            }
        }
    }

    private List<Spell> findUnlockedUltimates() {
        List<Spell> list = new ArrayList<>();
        for (String id : profile.getUnlockedSpells()) {
            Spell s = module.getSpellManager().getSpell(id);
            if (s != null && s.getSpellType() == SpellType.ULTIMATE) {
                list.add(s);
            }
        }
        return list;
    }

    @Override
    public void initializeItems() {
        // Limpar inventário (minimalismo)
        inventory.clear();

        if (ultimates.isEmpty()) {
            renderEmptyState();
            return;
        }

        // Renderizar ultimate atual
        Spell current = ultimates.get(currentIndex);
        inventory.setItem(SHOWCASE_SLOT, showcaseItem(current));

        // Navegação
        if (ultimates.size() > 1) {
            inventory.setItem(PREVIOUS_SLOT, new ItemBuilder(Material.ARROW)
                    .name(MessageUtils.parse(msg("nav.previous")))
                    .build());

            inventory.setItem(NEXT_SLOT, new ItemBuilder(Material.ARROW)
                    .name(MessageUtils.parse(msg("nav.next")))
                    .build());
        }

        // Botão equipar
        boolean isEquipped = current.getId().equalsIgnoreCase(profile.getEquippedUltimate());
        boolean isLocked = !checkRequirements(current);

        if (isLocked) {
            inventory.setItem(EQUIP_SLOT, new ItemBuilder(Material.BARRIER)
                    .name(MessageUtils.parse(msg("equip.locked_name")))
                    .lore(parse(List.of("", msg("equip.locked_lore"))))
                    .build());
        } else if (isEquipped) {
            inventory.setItem(EQUIP_SLOT, new ItemBuilder(Material.LIME_CONCRETE)
                    .name(MessageUtils.parse(msg("equip.equipped_name")))
                    .lore(parse(List.of("", msg("equip.equipped_lore"))))
                    .glow()
                    .build());
        } else {
            inventory.setItem(EQUIP_SLOT, new ItemBuilder(Material.EMERALD)
                    .name(MessageUtils.parse(msg("equip.available_name")))
                    .lore(parse(List.of("", msg("equip.available_lore"))))
                    .build());
        }

        // Botão voltar
        inventory.setItem(BACK_SLOT, new ItemBuilder(Material.ARROW)
                .name(MessageUtils.parse(msg("back_button")))
                .build());
    }

    private void renderEmptyState() {
        inventory.setItem(SHOWCASE_SLOT, new ItemBuilder(Material.BARRIER)
                .name(MessageUtils.parse(msg("empty.name")))
                .lore(parse(List.of("", msg("empty.lore"))))
                .build());

        inventory.setItem(BACK_SLOT, new ItemBuilder(Material.ARROW)
                .name(MessageUtils.parse(msg("back_button")))
                .build());
    }

    private ItemStack showcaseItem(Spell spell) {
        int lvl = profile.getSpellLevel(spell.getId());
        boolean isEquipped = spell.getId().equalsIgnoreCase(profile.getEquippedUltimate());

        // Lore extendida
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(msg("showcase.type_tag"));
        lore.add("");

        // Descrição da spell
        if (spell.getLore() != null && !spell.getLore().isEmpty()) {
            lore.addAll(processLore(spell.getLore(), spell, lvl));
            lore.add("");
        }

        // Stats detalhados
        lore.add(msg("showcase.stats_header"));
        lore.add("");

        double cd = spell.getCooldown().calculate(lvl);
        double mana = spell.getManaCost().calculate(lvl);
        double stam = spell.getStaminaCost().calculate(lvl);
        double castTime = spell.getCastTime();

        lore.add(msg("showcase.cooldown").replace("%value%", DF.format(cd)));
        lore.add(msg("showcase.mana").replace("%value%", DF.format(mana)));
        if (stam > 0) {
            lore.add(msg("showcase.stamina").replace("%value%", DF.format(stam)));
        }
        if (castTime > 0) {
            lore.add(msg("showcase.cast_time").replace("%value%", DF.format(castTime)));
        }

        lore.add("");

        // Nível
        lore.add(msg("showcase.level")
                .replace("%level%", String.valueOf(lvl))
                .replace("%max%", String.valueOf(spell.getMaxLevel())));

        // Status
        if (isEquipped) {
            lore.add("");
            lore.add(msg("showcase.equipped_badge"));
        }

        // Requisitos
        List<String> failedReqs = getFailedRequirements(spell);
        if (!failedReqs.isEmpty()) {
            lore.add("");
            lore.add(msg("showcase.requirements_header"));
            String failPrefix = module.getMessage("requirements.fail_prefix");
            for (String fail : failedReqs) {
                lore.add(failPrefix + fail);
            }
        }

        // Indicador de navegação
        if (ultimates.size() > 1) {
            lore.add("");
            lore.add(msg("showcase.navigation")
                    .replace("%current%", String.valueOf(currentIndex + 1))
                    .replace("%total%", String.valueOf(ultimates.size())));
        }

        ItemBuilder b = new ItemBuilder(Material.NETHER_STAR)
                .name(MessageUtils.parse(module.getMessage("main_gui.spell_color.ultimate") + spell.getDisplayName()))
                .lore(parse(lore))
                .glow();

        return b.build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();

        if (ultimates.isEmpty()) {
            if (slot == BACK_SLOT) {
                new MainSpellGUI(player, module).open();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        // Navegação
        if (slot == PREVIOUS_SLOT && ultimates.size() > 1) {
            currentIndex = (currentIndex - 1 + ultimates.size()) % ultimates.size();
            initializeItems();
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.8f);
            return;
        }

        if (slot == NEXT_SLOT && ultimates.size() > 1) {
            currentIndex = (currentIndex + 1) % ultimates.size();
            initializeItems();
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.2f);
            return;
        }

        // Equipar
        if (slot == EQUIP_SLOT) {
            Spell selected = ultimates.get(currentIndex);

            // Verificar requisitos
            if (!checkRequirements(selected)) {
                MessageUtils.send(player, msg("messages.requirements_failed"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Verificar se já está equipada
            if (selected.getId().equalsIgnoreCase(profile.getEquippedUltimate())) {
                MessageUtils.send(player, msg("messages.already_equipped"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                return;
            }

            // Equipar
            profile.setEquippedUltimate(selected.getId());
            saveSpellProfile();
            MessageUtils.send(player, msg("messages.equipped")
                    .replace("%spell%", selected.getDisplayName()));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
            new MainSpellGUI(player, module).open();
            return;
        }

        // Voltar
        if (slot == BACK_SLOT) {
            new MainSpellGUI(player, module).open();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
    }

    // ───────── Util ─────────

    private void saveSpellProfile() {
        me.ray.midgard.core.profile.MidgardProfile coreProfile = me.ray.midgard.core.MidgardCore.getProfileManager().getProfile(player);
        if (coreProfile != null) {
            me.ray.midgard.core.MidgardCore.getProfileManager().saveProfile(coreProfile);
        }
    }

    private boolean checkRequirements(Spell spell) {
        for (SpellRequirement req : spell.getRequirements()) {
            if (!req.check(player)) {
                return false;
            }
        }
        return true;
    }

    private List<String> getFailedRequirements(Spell spell) {
        List<String> failed = new ArrayList<>();
        for (SpellRequirement req : spell.getRequirements()) {
            if (!req.check(player)) {
                failed.add(req.getFailureMessage());
            }
        }
        return failed;
    }

    private String msg(String key) {
        return module.getMessage("ultimate_gui." + key);
    }

    private List<Component> parse(List<String> lines) {
        List<Component> out = new ArrayList<>();
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

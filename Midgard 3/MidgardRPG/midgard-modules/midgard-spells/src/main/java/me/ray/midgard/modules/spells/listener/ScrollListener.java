package me.ray.midgard.modules.spells.listener;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.data.SpellProfile;
import me.ray.midgard.modules.spells.manager.ScrollManager;
import me.ray.midgard.modules.spells.manager.ScrollManager.ScrollType;
import me.ray.midgard.modules.spells.obj.Spell;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ScrollListener implements Listener {

    private final SpellsModule module;

    public ScrollListener(SpellsModule module) {
        this.module = module;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) { return; }
        if (event.getHand() != EquipmentSlot.HAND) { return; }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        ScrollManager scrollManager = module.getScrollManager();
        if (scrollManager == null || !scrollManager.isScroll(item)) { return; }

        event.setCancelled(true);

        ScrollType type = scrollManager.getScrollType(item);
        if (type == null) { return; }

        switch (type) {
            case UNLEARNING -> useUnlearningScroll(player, item);
            case LEARNING -> useLearningScroll(player, item);
            case RESPEC -> useRespecScroll(player, item);
        }
    }

    private void useUnlearningScroll(Player player, ItemStack item) {
        SpellProfile profile = module.getSpellManager().getProfile(player);
        if (profile == null) {
            MessageUtils.send(player, module.getMessage("errors.profile_not_loaded"));
            return;
        }

        String targetSpellId = module.getScrollManager().getScrollTarget(item);
        if (targetSpellId == null || targetSpellId.isEmpty()) {
            // No specific target — message to use /spell unlearn or GUI
            MessageUtils.send(player, module.getMessage("scrolls.messages.unlearn_no_target"));
            return;
        }

        if (!profile.hasSpell(targetSpellId)) {
            Spell spell = module.getSpellManager().getSpell(targetSpellId);
            String name = spell != null ? spell.getDisplayName() : targetSpellId;
            MessageUtils.send(player, module.getMessage("scrolls.messages.not_learned").replace("%spell%", name));
            return;
        }

        unlearnSpell(player, profile, targetSpellId);
        saveSpellProfile(player);
        consumeScroll(player, item);

        Spell spell = module.getSpellManager().getSpell(targetSpellId);
        String name = spell != null ? spell.getDisplayName() : targetSpellId;
        MessageUtils.send(player, module.getMessage("scrolls.messages.unlearn_success").replace("%spell%", name));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f);
    }

    private void useLearningScroll(Player player, ItemStack item) {
        SpellProfile profile = module.getSpellManager().getProfile(player);
        if (profile == null) {
            MessageUtils.send(player, module.getMessage("errors.profile_not_loaded"));
            return;
        }

        String targetSpellId = module.getScrollManager().getScrollTarget(item);
        if (targetSpellId == null || targetSpellId.isEmpty()) {
            MessageUtils.send(player, module.getMessage("scrolls.messages.learn_no_target"));
            return;
        }

        Spell spell = module.getSpellManager().getSpell(targetSpellId);
        if (spell == null) {
            MessageUtils.send(player, module.getMessage("errors.spell_not_found").replace("%spell%", targetSpellId));
            return;
        }

        if (profile.hasSpell(targetSpellId)) {
            MessageUtils.send(player, module.getMessage("scrolls.messages.already_learned").replace("%spell%", spell.getDisplayName()));
            return;
        }

        // Unlock the spell
        profile.unlockSpell(targetSpellId);
        profile.lockSpell(targetSpellId);

        // Check memory for retained level
        int rememberedLevel = profile.getRememberedLevel(targetSpellId);
        if (rememberedLevel > 0) {
            int startLevel = module.getSpellManager().getXPManager().getStartingLevelFromMemory(rememberedLevel);
            if (startLevel > 1) {
                profile.setSpellLevel(targetSpellId, startLevel);
                MessageUtils.send(player, module.getMessage("scrolls.messages.memory_restored")
                        .replace("%spell%", spell.getDisplayName())
                        .replace("%level%", String.valueOf(startLevel)));
            }
            profile.forgetSpell(targetSpellId);
        }

        saveSpellProfile(player);
        consumeScroll(player, item);

        MessageUtils.send(player, module.getMessage("scrolls.messages.learn_success").replace("%spell%", spell.getDisplayName()));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    private void useRespecScroll(Player player, ItemStack item) {
        SpellProfile profile = module.getSpellManager().getProfile(player);
        if (profile == null) {
            MessageUtils.send(player, module.getMessage("errors.profile_not_loaded"));
            return;
        }

        List<String> spellsToUnlearn = new ArrayList<>(profile.getUnlockedSpells());
        if (spellsToUnlearn.isEmpty()) {
            MessageUtils.send(player, module.getMessage("scrolls.messages.no_spells_to_respec"));
            return;
        }

        int count = 0;
        for (String spellId : spellsToUnlearn) {
            if (profile.isLocked(spellId)) { continue; }
            unlearnSpell(player, profile, spellId);
            count++;
        }

        if (count == 0) {
            MessageUtils.send(player, module.getMessage("scrolls.messages.no_spells_to_respec"));
            return;
        }

        saveSpellProfile(player);
        consumeScroll(player, item);

        MessageUtils.send(player, module.getMessage("scrolls.messages.respec_success")
                .replace("%count%", String.valueOf(count)));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    private void unlearnSpell(Player player, SpellProfile profile, String spellId) {
        // Save level to memory before unlearning
        int currentLevel = profile.getSpellLevel(spellId);
        if (currentLevel > 1) {
            profile.rememberSpell(spellId, currentLevel);
        }
        // Remove mastery attribute modifiers before clearing profile data
        module.getSpellManager().removeSpellMasteryModifiers(player, spellId);
        profile.unlearnSpell(spellId);
    }

    private void saveSpellProfile(Player player) {
        me.ray.midgard.core.profile.MidgardProfile coreProfile = me.ray.midgard.core.MidgardCore.getProfileManager().getProfile(player);
        if (coreProfile != null) {
            me.ray.midgard.core.MidgardCore.getProfileManager().saveProfile(coreProfile);
        }
    }

    private void consumeScroll(Player player, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
}

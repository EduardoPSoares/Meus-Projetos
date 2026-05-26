package me.ray.midgard.modules.spells.listener;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.data.SpellProfile;
import me.ray.midgard.modules.spells.obj.Spell;
import me.ray.midgard.modules.spells.task.ChannelingTask;
import me.ray.midgard.core.utils.Task;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpellsListener implements Listener {

    private final SpellsModule module;
    private final Map<UUID, StringBuilder> comboBuffer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> comboExpiry = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();

    public SpellsListener(SpellsModule module) {
        this.module = module;
    }

    @EventHandler
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {
        // Allow rotation and small movements (< 0.1 blocks squared)
        if (event.getTo().distanceSquared(event.getFrom()) < 0.01) { return; }
        
        Player player = event.getPlayer();
        if (module.getSpellManager().isChanneling(player)) {
            ChannelingTask task = module.getSpellManager().getChannelingTask(player);
            if (task != null && task.getSpell().isInterruptible()) {
                module.getSpellManager().cancelChanneling(player, module.getMessage("casting.interrupt_reasons.movement"));
            }
        }
    }
    
    @EventHandler
    public void onDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) { return; }
        if (!module.getSpellManager().isChanneling(player)) { return; }

        ChannelingTask task = module.getSpellManager().getChannelingTask(player);
        if (task == null) { return; }

        Spell spell = task.getSpell();
        if (!spell.isInterruptible()) { return; }

        double threshold = spell.getInterruptThreshold();

        // threshold <= 0 means any damage interrupts
        if (threshold <= 0) {
            module.getSpellManager().cancelChanneling(player, module.getMessage("casting.interrupt_reasons.damage"));
            return;
        }

        // Check if damage percentage exceeds threshold
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (maxHealth <= 0) { return; }
        double dmgPercent = event.getDamage() / maxHealth;
        if (dmgPercent >= threshold) {
            module.getSpellManager().cancelChanneling(player, module.getMessage("casting.interrupt_reasons.damage"));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        // Cancel channeling first to clean up task and bossbar
        if (module.getSpellManager().isChanneling(player)) {
            module.getSpellManager().cancelChanneling(player, module.getMessage("casting.interrupt_reasons.disconnect"));
        }
        comboBuffer.remove(uuid);
        comboExpiry.remove(uuid);
        lastClick.remove(uuid);
        module.getSpellManager().setComboActive(uuid, false);
        if (module.getSpellManager().isCastingMode(player)) {
            module.getSpellManager().disableCastingMode(player);
        }
        if (module.getDamageListener() != null) {
            module.getDamageListener().cleanup(uuid);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Reaplica bônus de mastery após o perfil ser carregado (delay para garantir)
        Task.syncLater(event.getPlayer(), () -> {
            if (event.getPlayer().isOnline() && module.getSpellManager() != null) {
                module.getSpellManager().reapplyMasteryBonuses(event.getPlayer());
            }
        }, 40L); // 2 second delay to ensure profile is loaded
    }
    
    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        
        // Shift+F allows normal hand swap
        if (player.isSneaking()) { return; }
        
        event.setCancelled(true);
        module.getSpellManager().toggleCastingMode(player);
    }
    
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (module.getSpellManager().isCastingMode(player)) {
            
            // Verifica se o jogador está no modo SKILLBAR
            SpellProfile profile = module.getSpellManager().getProfile(player);
            if (profile != null && profile.getCastingStyle() != SpellProfile.CastingStyle.SKILLBAR) {
                return; // Se for COMBO, ignora a lógica de slots
            }

            // New logic: Check if target slot has a skill IN VIRTUAL VIEW
            int newSlotIndex = event.getNewSlot(); // 0-8
            
            // Usar o getSkillInVirtualSlot para saber o que tem lá visualmente
            String skillId = module.getSpellManager().getSkillInVirtualSlot(player, newSlotIndex);
            
            if (skillId != null) {
                // Has skill -> Cast and Cancel (Stay in previous slot to allow rapid fire)
                event.setCancelled(true);
                module.getSpellManager().castSpell(player, skillId);
            } else {
                // No skill (Empty/Anchor/Void) -> Allow switch (Move hand to new slot)
                // This becomes the new resting place.
                // NOTE: Se o anchor é fixo na ativação, mover a mão não muda o layout.
                // O layout só muda se o anchor mudar.
                // Pelo requisito "se a skill estiver no slot 1 e ele ativar... ela vai para o slot 2",
                // subentende-se que o anchor é fixo na ativação.
                // Se o player mover a mão para um slot vazio, ele apenas move a mão.
            }
        }
    }

    // --- COMBO SYSTEM ---
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.PHYSICAL) { return; }

        Player player = event.getPlayer();
        
        // Verificar se está no modo COMBO antes de processar
        SpellProfile profile = module.getSpellManager().getProfile(player);
        if (profile == null || profile.getCastingStyle() != SpellProfile.CastingStyle.COMBO) {
            return; 
        }

        // Verificar se o Casting Mode (F) está ativo
        if (!module.getSpellManager().isCastingMode(player)) {
            return;
        }
        
        // Debounce para evitar duplo clique (Main Hand + Off Hand)
        long now = System.currentTimeMillis();
        long last = lastClick.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 100) { return; } // 100ms debounce
        lastClick.put(player.getUniqueId(), now);

        String clickType = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) ? "L" : "R";

        // Combos só podem começar com botão direito (R)
        UUID uuid = player.getUniqueId();
        StringBuilder existingBuffer = comboBuffer.get(uuid);
        if ((existingBuffer == null || existingBuffer.isEmpty()) && clickType.equals("L")) {
            return; // Ignora clique esquerdo como primeiro input
        }

        updateCombo(player, clickType);
    }

    private void updateCombo(Player player, String click) {
        UUID uuid = player.getUniqueId();

        // Update expiry time
        long duration = 2000L; // 2 seconds
        comboExpiry.put(uuid, System.currentTimeMillis() + duration);

        StringBuilder builder = comboBuffer.computeIfAbsent(uuid, k -> new StringBuilder());
        builder.append(click);

        // Mark combo active to block SkillBar overlay
        module.getSpellManager().setComboActive(uuid, true);

        // Visual Feedback
        String sep = module.getMessage("combo.input.separator");
        StringBuilder visual = new StringBuilder();
        for (int i = 0; i < builder.length(); i++) {
            char c = builder.charAt(i);
            if (i > 0) { visual.append(sep); }
            visual.append(c == 'L' ? module.getMessage("combo.visual.left") : module.getMessage("combo.visual.right"));
        }
        MessageUtils.sendActionBar(player, visual.toString());
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, click.equals("L") ? 1.5f : 1.0f);

        // Check if valid combo exists
        String currentCombo = builder.toString();

        // 1. Check Profile Bindings (Personal overrides)
        SpellProfile profile = module.getSpellManager().getProfile(player);
        String spellId = (profile != null) ? profile.getSpellByCombo(currentCombo) : null;

        if (spellId != null) {
            // Found a match!
            boolean casted = module.getSpellManager().castSpell(player, spellId);
            if (casted) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
            }
            builder.setLength(0);
            module.getSpellManager().setComboActive(uuid, false);
            MessageUtils.sendActionBar(player, "");
        } else {
            // No exact match yet, but limit length
            if (currentCombo.length() >= 3) { // Combo sempre tem 3 inputs — falhou
                builder.setLength(0);
                MessageUtils.sendActionBar(player, module.getMessage("combo.failed"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);

                Task.syncLater(player, () -> {
                    module.getSpellManager().setComboActive(uuid, false);
                }, 20L); // Show failure for 1s

            } else {
                // Schedule clear
                Task.syncLater(player, () -> {
                    // Check if expired
                    if (System.currentTimeMillis() >= comboExpiry.getOrDefault(uuid, 0L)) {
                        comboBuffer.remove(uuid);
                        module.getSpellManager().setComboActive(uuid, false);
                        MessageUtils.sendActionBar(player, "");
                        comboExpiry.remove(uuid);
                    }
                }, 40L); // 2.0 seconds to finish combo
            }
        }
    }

}

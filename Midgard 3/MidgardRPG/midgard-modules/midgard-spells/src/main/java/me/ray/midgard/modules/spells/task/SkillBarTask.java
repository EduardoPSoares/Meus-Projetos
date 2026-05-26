package me.ray.midgard.modules.spells.task;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.data.SpellProfile;
import me.ray.midgard.modules.spells.manager.SpellManager;
import me.ray.midgard.modules.spells.obj.Spell;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class SkillBarTask {

    private final SpellsModule module;
    private final SpellManager manager;
    private BukkitTask taskId;

    public SkillBarTask(SpellsModule module) {
        this.module = module;
        this.manager = module.getSpellManager();
    }

    public void start() {
        // Task global — roda a cada 5 ticks (4x/segundo) para evitar flickering
        taskId = Task.syncTimer(this::run, 5L, 5L);
    }

    public void stop() {
        if (taskId != null) {
            taskId.cancel();
        }
    }

    private void run() {
        for (UUID uuid : manager.getCastingPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) { continue; }

            // Update on player region thread
            Task.sync(player, () -> updateActionBar(player));
        }
    }

    private void updateActionBar(Player player) {
        // Se estiver canalizando, não sobrescrever a Action Bar
        if (manager.isChanneling(player)) { return; }

        // Se estiver fazendo um combo, não sobrescrever a Action Bar
        if (manager.isComboActive(player)) { return; }

        SpellProfile profile = manager.getProfile(player);
        if (profile == null) { return; }
        
        double currentMana = module.getResourceProvider().getMana(player);

        StringBuilder bar = new StringBuilder();
        
        // Verifica o estilo
        if (profile.getCastingStyle() == SpellProfile.CastingStyle.COMBO) {
            // Removido a exibição de combos na actionbar
        } else {
            // Loop slots 1 to 6 (SkillBar Mode: Anchor + 4 Spells + Ultimate)
            String separator = module.getMessage("actionbar.separator");
            boolean first = true;
            
            for (int i = 1; i <= 6; i++) {
                String spellId = manager.getSkillInVirtualSlot(player, i - 1);
                
                // Pular slot do anchor (retorna null se for o anchor)
                // O problema é que slot vazio também retorna null.
                // Mas queremos mostrar slots vazios se forem de spell (1,2,3), mas talvez não o anchor.
                // Simplicidade: Mostra tudo. Se for null e for o anchor, aparecerá como vazio.
                
                // Melhoria: Se spellId for null, verifica se é o anchor
                if (spellId == null && manager.isAnchorSlot(player, i - 1)) {
                    continue; // Não mostra o slot que está segurando o item de ativação
                }

                if (spellId == null) {
                    if (!first) { bar.append(separator); }
                    first = false;
                    bar.append(module.getMessage("actionbar.skill.empty")
                        .replace("%slot%", String.valueOf(i)));
                } else {
                    Spell spell = manager.getSpell(spellId);
                    if (spell == null) {
                        if (!first) { bar.append(separator); }
                        first = false;
                        bar.append(module.getMessage("actionbar.skill.error")
                            .replace("%slot%", String.valueOf(i)));
                    } else if (spell.isPassive()) {
                        continue;
                    } else {
                        if (!first) { bar.append(separator); }
                        first = false;
                        // Formatação diferente para Ultimate
                        String name = spell.isUltimate() 
                                ? module.getMessage("actionbar.skill.ultimate_format").replace("%name%", spell.getDisplayName())
                                : spell.getDisplayName();

                        int spellLevel = profile != null ? profile.getSpellLevel(spellId) : 1;
                        if (spellLevel < 1) { spellLevel = 1; }

                        double manaCost = spell.getManaCost().calculate(spellLevel);
                        boolean noMana = (currentMana < manaCost);
                        boolean cooldown = profile.isOnCooldown(spellId);
                        
                        if (cooldown) {
                            long cdMs = profile.getCooldownRemainingKey(spellId);
                            long cd = cdMs / 1000;
                            if (cd == 0) { cd = 1; }
                            bar.append(module.getMessage("actionbar.skill.cooldown")
                                .replace("%slot%", String.valueOf(i))
                                .replace("%spell%", name)
                                .replace("%time%", String.valueOf(cd)));

                            applyCooldownOverlay(player, i - 1, cdMs);
                        } else if (noMana) {
                            bar.append(module.getMessage("actionbar.skill.ready_no_mana")
                                .replace("%slot%", String.valueOf(i))
                                .replace("%spell%", name));
                        } else {
                            bar.append(module.getMessage("actionbar.skill.ready")
                                .replace("%slot%", String.valueOf(i))
                                .replace("%spell%", name));
                        }
                    }
                }
            }
        }
        
        MessageUtils.sendActionBar(player, bar.toString());
    }

    private void applyCooldownOverlay(Player player, int slotIndex, long remainingMs) {
        ItemStack item = player.getInventory().getItem(slotIndex);
        if (item == null || item.getType() == Material.AIR) { return; }
        Material material = item.getType();
        int remainingTicks = (int) (remainingMs / 50);
        if (remainingTicks <= 0) { return; }

        // Só atualiza se a diferença for significativa (> 10 ticks)
        // para evitar flickering ao resetar a animação constantemente
        int current = player.getCooldown(material);
        if (current <= 0 || Math.abs(current - remainingTicks) > 10) {
            player.setCooldown(material, remainingTicks);
        }
    }
}

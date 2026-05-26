package me.ray.midgard.modules.professions.penalty;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.professions.ProfessionType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Listener de penalidades para quebra de blocos.
 * Afeta: Minerador, Agricultor, Carpinteiro, Alquimista.
 *
 * Penalidades aplicáveis:
 * - Drop reduzido (chance de não dropar)
 * - Mining Fatigue / Slowness
 * - Desgaste extra de ferramenta
 *
 * Usa prioridade HIGH para interceptar ANTES do MONITOR (que é o XP).
 * ignoreCancelled=true: não penalizar se outro plugin já cancelou.
 */
public class PenaltyBlockListener implements Listener {

    private final ProfessionPenaltyManager penaltyManager;

    public PenaltyBlockListener(ProfessionPenaltyManager penaltyManager) {
        this.penaltyManager = Objects.requireNonNull(penaltyManager);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        try {
            if (!penaltyManager.isGlobalEnabled()) { return; }

            Player player = event.getPlayer();
            Material blockType = event.getBlock().getType();

            // Descobrir qual profissão protege esse material
            ProfessionType requiredType = penaltyManager.findProfessionForMaterial("block-break", blockType);
            if (requiredType == null) { return; }

            // Jogador é profissional de qualquer profissão que dá XP para este bloco? Sem penalidade.
            if (penaltyManager.isPlayerExemptForMaterial(player, "block-break", blockType)) { return; }

            // Buscar config de penalidade
            ProfessionPenaltyConfig config = penaltyManager.getPenaltyFor(requiredType);
            if (config == null) { return; }
            if (!config.affectsMaterial(blockType)) { return; }

            boolean penalized = false;

            // 1. Chance de não dropar
            if (config.dropChance() < 1.0) {
                if (!penaltyManager.rollChance(config.dropChance())) {
                    // Falhou no drop — quebra o bloco mas não dropa nada
                    event.setDropItems(false);
                    penalized = true;
                }
            }

            // 2. Efeito de mineração (Mining Fatigue, Slowness, etc.)
            if (config.miningEffect() != null) {
                penaltyManager.applyEffect(player, config.miningEffect(),
                        config.miningEffectDuration(), config.miningEffectAmplifier());
            }

            // 3. Desgaste extra de ferramenta
            if (config.durabilityMultiplier() > 1.0) {
                applyExtraDurability(player, config.durabilityMultiplier());
            }

            // Mensagem
            if (penalized && config.showMessage()) {
                penaltyManager.sendPenaltyMessage(player, "block_no_drop", requiredType);
            } else if (config.miningEffect() != null && config.showMessage()) {
                penaltyManager.sendPenaltyMessage(player, "block_slow", requiredType);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar penalidade de block-break para %s", event.getPlayer().getName(), e);
        }
    }

    /**
     * Aplica desgaste extra na ferramenta que o jogador está segurando.
     * Usa probabilidade para a parte fracionária do multiplicador:
     * multiplier 1.5 → 50% de +1 extra (média 1.5x)
     * multiplier 2.0 → sempre +1 extra (2.0x)
     * multiplier 2.5 → sempre +1 extra, 50% de +1 adicional (média 2.5x)
     */
    private void applyExtraDurability(Player player, double multiplier) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType() == Material.AIR) { return; }
        if (!(tool.getItemMeta() instanceof Damageable damageable)) { return; }

        double extraFactor = multiplier - 1.0;
        int guaranteedExtra = (int) extraFactor;
        double fractional = extraFactor - guaranteedExtra;
        int extraDamage = guaranteedExtra;
        if (fractional > 0 && ThreadLocalRandom.current().nextDouble() < fractional) {
            extraDamage++;
        }
        if (extraDamage <= 0) { return; }

        int newDamage = damageable.getDamage() + extraDamage;
        int maxDurability = tool.getType().getMaxDurability();

        if (maxDurability <= 0) { return; }

        if (newDamage >= maxDurability) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            return;
        }

        damageable.setDamage(newDamage);
        tool.setItemMeta(damageable);
        player.getInventory().setItemInMainHand(tool);
    }
}

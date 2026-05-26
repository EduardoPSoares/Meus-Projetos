package me.ray.midgard.modules.item.task;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.item.manager.AttributeUpdater;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class EquipmentUpdateTask implements Runnable {

    private static final Map<UUID, Integer> equipmentHashes = new ConcurrentHashMap<>();

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Task.sync(player, () -> processPlayer(player));
        }
    }

    private void processPlayer(Player player) {
        if (!player.isOnline()) {
            return;
        }
        try {
            int currentHash = calculateEquipmentHash(player);
            Integer lastHash = equipmentHashes.get(player.getUniqueId());

            if (lastHash == null || lastHash != currentHash) {
                AttributeUpdater.updateAttributes(player);
                equipmentHashes.put(player.getUniqueId(), currentHash);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao atualizar atributos de equipamentos", e);
        }
    }

    private static int calculateEquipmentHash(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack hand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        int result = Arrays.hashCode(armor);
        result = 31 * result + (hand != null ? hand.hashCode() : 0);
        result = 31 * result + (offHand != null ? offHand.hashCode() : 0);
        return result;
    }
    
    public static void clearCache(UUID uuid) {
        equipmentHashes.remove(uuid);
    }

    /**
     * Recalcula e armazena o hash do equipamento atual.
     * Chamado pelo EquipListener após atualizar atributos via evento,
     * para evitar que o polling duplique a atualização.
     */
    public static void refreshHash(Player player) {
        if (player != null && player.isOnline()) {
            int hash = calculateEquipmentHash(player);
            equipmentHashes.put(player.getUniqueId(), hash);
        }
    }
}

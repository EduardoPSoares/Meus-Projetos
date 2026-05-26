package me.ray.midgardDungeon.engine

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Salva e restaura o inventário do jogador ao entrar/sair de uma dungeon.
 */
object InventoryManager {

    data class SavedInventory(
        val contents: Array<ItemStack?>,
        val armor: Array<ItemStack?>,
        val offhand: ItemStack?,
        val level: Int,
        val exp: Float,
    )

    private val savedInventories = ConcurrentHashMap<UUID, SavedInventory>()

    fun saveAndClear(player: Player) {
        savedInventories[player.uniqueId] = SavedInventory(
            contents = player.inventory.contents.map { it?.clone() }.toTypedArray(),
            armor = player.inventory.armorContents.map { it?.clone() }.toTypedArray(),
            offhand = player.inventory.itemInOffHand.clone(),
            level = player.level,
            exp = player.exp,
        )
        player.inventory.clear()
        player.level = 0
        player.exp = 0f
    }

    fun restore(player: Player) {
        val saved = savedInventories.remove(player.uniqueId) ?: return
        player.inventory.clear()
        player.inventory.contents = saved.contents
        player.inventory.armorContents = saved.armor
        saved.offhand?.let { player.inventory.setItemInOffHand(it) }
        player.level = saved.level
        player.exp = saved.exp
    }

    fun hasSavedInventory(playerId: UUID): Boolean = savedInventories.containsKey(playerId)

    fun shutdown() {
        savedInventories.clear()
    }
}

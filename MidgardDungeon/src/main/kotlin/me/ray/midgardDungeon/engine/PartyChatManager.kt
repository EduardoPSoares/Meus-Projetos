package me.ray.midgardDungeon.engine

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Chat de grupo dentro da dungeon.
 */
object PartyChatManager {

    // Jogadores com chat de grupo ativado
    private val partyChatEnabled = ConcurrentHashMap.newKeySet<UUID>()

    fun togglePartyChat(player: Player): Boolean {
        return if (partyChatEnabled.contains(player.uniqueId)) {
            partyChatEnabled.remove(player.uniqueId)
            player.sendMessage(Component.text("Chat de grupo desativado.", NamedTextColor.YELLOW))
            false
        } else {
            partyChatEnabled.add(player.uniqueId)
            player.sendMessage(Component.text("Chat de grupo ativado.", NamedTextColor.GREEN))
            true
        }
    }

    fun isPartyChatEnabled(playerId: UUID): Boolean = partyChatEnabled.contains(playerId)

    fun sendPartyMessage(sender: Player, message: String) {
        val instance = DungeonManager.getInstanceByPlayer(sender.uniqueId) ?: return

        val formatted = Component.text("[Grupo] ", NamedTextColor.BLUE)
            .append(Component.text("${sender.name}: ", NamedTextColor.WHITE))
            .append(Component.text(message, NamedTextColor.GRAY))

        instance.getOnlinePlayers().forEach { p ->
            p.sendMessage(formatted)
        }
    }

    /**
     * Lida com evento de chat. Retorna true se a mensagem foi interceptada como chat de grupo.
     */
    fun handleChat(player: Player, message: String): Boolean {
        if (!isPartyChatEnabled(player.uniqueId)) return false
        if (!DungeonManager.isPlayerInDungeon(player.uniqueId)) return false
        sendPartyMessage(player, message)
        return true
    }

    fun removePlayer(playerId: UUID) {
        partyChatEnabled.remove(playerId)
    }

    fun shutdown() {
        partyChatEnabled.clear()
    }
}

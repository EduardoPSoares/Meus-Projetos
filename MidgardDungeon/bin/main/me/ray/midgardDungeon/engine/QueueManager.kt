package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import me.ray.midgardDungeon.party.Party
import me.ray.midgardDungeon.party.PartyManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Fila de matchmaking para dungeons.
 */
object QueueManager {

    data class QueueEntry(
        val playerId: UUID,
        val dungeonId: String,
        val joinTime: Long = System.currentTimeMillis(),
    )

    // dungeonId -> fila de jogadores
    private val queues = ConcurrentHashMap<String, ConcurrentLinkedQueue<QueueEntry>>()
    // dungeonId -> mínimo de jogadores necessários para iniciar
    private val minPlayers = ConcurrentHashMap<String, Int>()
    private var checkTaskId: Int = -1

    fun initialize() {
        queues.clear()
        minPlayers.clear()

        val plugin = MidgardPlugin.instance ?: return
        checkTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, Runnable {
            checkQueues()
        }, 100L, 100L) // Verificar a cada 5 segundos
    }

    fun shutdown() {
        if (checkTaskId != -1) {
            Bukkit.getScheduler().cancelTask(checkTaskId)
            checkTaskId = -1
        }
        queues.clear()
        minPlayers.clear()
    }

    fun registerDungeon(dungeonId: String, minRequired: Int) {
        minPlayers[dungeonId] = minRequired
        queues.getOrPut(dungeonId) { ConcurrentLinkedQueue() }
    }

    fun joinQueue(player: Player, dungeonId: String): Boolean {
        if (DungeonManager.isPlayerInDungeon(player.uniqueId)) {
            player.sendMessage(Component.text("Você já está em uma dungeon!", NamedTextColor.RED))
            return false
        }

        // Verificar se já está na fila
        if (isInQueue(player.uniqueId)) {
            player.sendMessage(Component.text("Você já está na fila!", NamedTextColor.RED))
            return false
        }

        val queue = queues.getOrPut(dungeonId) { ConcurrentLinkedQueue() }
        queue.add(QueueEntry(player.uniqueId, dungeonId))

        val required = minPlayers[dungeonId] ?: 1
        player.sendMessage(
            Component.text("Entrou na fila para dungeon! (${queue.size}/$required)", NamedTextColor.GREEN)
        )

        // Verificação imediata
        checkQueue(dungeonId)
        return true
    }

    fun leaveQueue(player: Player): Boolean {
        for ((_, queue) in queues) {
            if (queue.removeIf { it.playerId == player.uniqueId }) {
                player.sendMessage(Component.text("Saiu da fila.", NamedTextColor.YELLOW))
                return true
            }
        }
        return false
    }

    fun isInQueue(playerId: UUID): Boolean {
        return queues.values.any { q -> q.any { it.playerId == playerId } }
    }

    fun getQueueSize(dungeonId: String): Int {
        return queues[dungeonId]?.size ?: 0
    }

    fun getQueuePosition(playerId: UUID): Int {
        for ((_, queue) in queues) {
            val list = queue.toList()
            val index = list.indexOfFirst { it.playerId == playerId }
            if (index >= 0) return index + 1
        }
        return -1
    }

    private fun checkQueues() {
        for (dungeonId in queues.keys) {
            checkQueue(dungeonId)
        }
    }

    private fun checkQueue(dungeonId: String) {
        val queue = queues[dungeonId] ?: return
        val required = minPlayers[dungeonId] ?: 1

        // Remover jogadores offline
        queue.removeIf { Bukkit.getPlayer(it.playerId) == null }

        if (queue.size >= required) {
            val matchedPlayers = mutableListOf<QueueEntry>()
            repeat(required.coerceAtMost(queue.size)) {
                queue.poll()?.let { matchedPlayers.add(it) }
            }

            if (matchedPlayers.size >= required) {
                formGroup(dungeonId, matchedPlayers)
            }
        }
    }

    private fun formGroup(dungeonId: String, entries: List<QueueEntry>) {
        val leader = entries.first()
        val leaderPlayer = Bukkit.getPlayer(leader.playerId) ?: return

        val party = PartyManager.createParty(leader.playerId, entries.size.coerceAtLeast(4))

        for (entry in entries.drop(1)) {
            PartyManager.joinParty(entry.playerId, party.id)
        }

        entries.mapNotNull { Bukkit.getPlayer(it.playerId) }.forEach { p ->
            p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN))
            p.sendMessage(Component.text("Grupo formado pelo matchmaking!", NamedTextColor.GREEN))
            p.sendMessage(Component.text("Dungeon: $dungeonId", NamedTextColor.YELLOW))
            p.sendMessage(Component.text("Jogadores: ${entries.size}", NamedTextColor.GRAY))
            p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN))
        }
    }
}

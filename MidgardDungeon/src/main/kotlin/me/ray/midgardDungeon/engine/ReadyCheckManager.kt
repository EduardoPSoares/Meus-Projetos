package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Sistema de verificação de prontidão antes de iniciar a dungeon.
 */
object ReadyCheckManager {

    data class ReadyCheck(
        val instanceId: UUID,
        val initiator: UUID,
        val players: Set<UUID>,
        val readyPlayers: ConcurrentHashMap.KeySetView<UUID, Boolean> = ConcurrentHashMap.newKeySet(),
        val startTime: Long = System.currentTimeMillis(),
        val timeoutSeconds: Int = 30,
        var onComplete: (() -> Unit)? = null,
        var onFail: (() -> Unit)? = null,
    )

    private val activeChecks = ConcurrentHashMap<UUID, ReadyCheck>()
    // playerId -> instanceId da verificação ativa
    private val playerChecks = ConcurrentHashMap<UUID, UUID>()

    fun startReadyCheck(
        instanceId: UUID,
        initiator: Player,
        players: Set<UUID>,
        timeoutSeconds: Int = 30,
        onComplete: () -> Unit,
        onFail: () -> Unit,
    ) {
        val check = ReadyCheck(
            instanceId = instanceId,
            initiator = initiator.uniqueId,
            players = players,
            timeoutSeconds = timeoutSeconds,
            onComplete = onComplete,
            onFail = onFail,
        )
        activeChecks[instanceId] = check
        players.forEach { playerChecks[it] = instanceId }

        players.mapNotNull { Bukkit.getPlayer(it) }.forEach { p ->
            p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
            p.sendMessage(Component.text("⚔ VERIFICAÇÃO DE PRONTIDÃO ⚔", NamedTextColor.GOLD))
            p.sendMessage(Component.text("Digite /ready para confirmar!", NamedTextColor.YELLOW))
            p.sendMessage(Component.text("Tempo: ${timeoutSeconds}s", NamedTextColor.GRAY))
            p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
        }

        // Auto-pronto para o iniciador
        markReady(initiator)

        // Agendar timeout
        val plugin = MidgardPlugin.instance ?: return
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val current = activeChecks[instanceId] ?: return@Runnable
            if (current.readyPlayers.size < current.players.size) {
                failCheck(instanceId, "Tempo esgotado!")
            }
        }, timeoutSeconds * 20L)
    }

    fun markReady(player: Player): Boolean {
        val instanceId = playerChecks[player.uniqueId] ?: return false
        val check = activeChecks[instanceId] ?: return false

        check.readyPlayers.add(player.uniqueId)

        val ready = check.readyPlayers.size
        val total = check.players.size

        check.players.mapNotNull { Bukkit.getPlayer(it) }.forEach { p ->
            p.sendMessage(
                Component.text("${player.name} está pronto! ($ready/$total)", NamedTextColor.GREEN)
            )
        }

        if (ready >= total) {
            completeCheck(instanceId)
        }

        return true
    }

    fun hasPendingCheck(playerId: UUID): Boolean = playerChecks.containsKey(playerId)

    private fun completeCheck(instanceId: UUID) {
        val check = activeChecks.remove(instanceId) ?: return
        check.players.forEach { playerChecks.remove(it) }

        check.players.mapNotNull { Bukkit.getPlayer(it) }.forEach { p ->
            p.sendMessage(Component.text("✓ Todos prontos! Iniciando dungeon...", NamedTextColor.GREEN))
        }

        check.onComplete?.invoke()
    }

    private fun failCheck(instanceId: UUID, reason: String) {
        val check = activeChecks.remove(instanceId) ?: return
        check.players.forEach { playerChecks.remove(it) }

        val notReady = check.players - check.readyPlayers
        val notReadyNames = notReady.mapNotNull { Bukkit.getPlayer(it)?.name }

        check.players.mapNotNull { Bukkit.getPlayer(it) }.forEach { p ->
            p.sendMessage(Component.text("✗ Verificação falhou: $reason", NamedTextColor.RED))
            if (notReadyNames.isNotEmpty()) {
                p.sendMessage(
                    Component.text("Não prontos: ${notReadyNames.joinToString(", ")}", NamedTextColor.GRAY)
                )
            }
        }

        check.onFail?.invoke()
    }

    fun cancelCheck(instanceId: UUID) {
        failCheck(instanceId, "Cancelado")
    }

    fun shutdown() {
        activeChecks.clear()
        playerChecks.clear()
    }
}

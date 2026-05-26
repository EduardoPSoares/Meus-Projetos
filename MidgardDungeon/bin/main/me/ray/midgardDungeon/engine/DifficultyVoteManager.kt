package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Sistema de votação de dificuldade pré-dungeon.
 * Os jogadores votam em um nível de dificuldade antes de iniciar.
 */
object DifficultyVoteManager {

    enum class Difficulty(val displayName: String, val icon: String, val healthMul: Double, val damageMul: Double, val lootMul: Double, val expMul: Double) {
        FACIL("Fácil", "🟢", 0.75, 0.75, 0.8, 0.8),
        NORMAL("Normal", "🟡", 1.0, 1.0, 1.0, 1.0),
        DIFICIL("Difícil", "🟠", 1.5, 1.3, 1.5, 1.5),
        LENDARIO("Lendário", "🔴", 2.0, 1.75, 2.5, 2.5),
    }

    data class VoteSession(
        val instanceId: UUID,
        val players: Set<UUID>,
        val votes: ConcurrentHashMap<UUID, Difficulty> = ConcurrentHashMap(),
        val startTime: Long = System.currentTimeMillis(),
        val timeoutSeconds: Int = 20,
        var onComplete: ((Difficulty) -> Unit)? = null,
    )

    private val activeSessions = ConcurrentHashMap<UUID, VoteSession>()
    private val playerSessions = ConcurrentHashMap<UUID, UUID>()

    fun startVote(
        instanceId: UUID,
        players: Set<UUID>,
        timeoutSeconds: Int = 20,
        onComplete: (Difficulty) -> Unit,
    ) {
        val session = VoteSession(
            instanceId = instanceId,
            players = players,
            timeoutSeconds = timeoutSeconds,
            onComplete = onComplete,
        )
        activeSessions[instanceId] = session
        players.forEach { playerSessions[it] = instanceId }

        players.mapNotNull { Bukkit.getPlayer(it) }.forEach { p ->
            p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
            p.sendMessage(Component.text("🗳 VOTAÇÃO DE DIFICULDADE", NamedTextColor.GOLD))
            p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
            for (diff in Difficulty.entries) {
                p.sendMessage(
                    Component.text("  ${diff.icon} ${diff.displayName}", NamedTextColor.YELLOW)
                        .append(Component.text(" — Vida: ${diff.healthMul}x, Dano: ${diff.damageMul}x, Loot: ${diff.lootMul}x", NamedTextColor.GRAY))
                )
            }
            p.sendMessage(Component.text("Digite no chat: facil, normal, dificil ou lendario", NamedTextColor.AQUA))
            p.sendMessage(Component.text("Tempo: ${timeoutSeconds}s", NamedTextColor.GRAY))
            p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
        }

        // Timeout automático
        val plugin = MidgardPlugin.instance ?: return
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            finishVote(instanceId)
        }, timeoutSeconds * 20L)
    }

    fun vote(player: Player, difficultyName: String): Boolean {
        val sessionId = playerSessions[player.uniqueId] ?: return false
        val session = activeSessions[sessionId] ?: return false

        val difficulty = try {
            Difficulty.valueOf(difficultyName.uppercase()
                .replace("Á", "A").replace("Í", "I"))
        } catch (_: Exception) {
            // Tentar match parcial
            Difficulty.entries.firstOrNull {
                it.name.lowercase().startsWith(difficultyName.lowercase().take(3))
            } ?: return false
        }

        session.votes[player.uniqueId] = difficulty
        val voteCount = session.votes.size
        val totalPlayers = session.players.size

        session.players.mapNotNull { Bukkit.getPlayer(it) }.forEach { p ->
            p.sendMessage(
                Component.text("${player.name} votou ${difficulty.icon} ${difficulty.displayName} ($voteCount/$totalPlayers)", NamedTextColor.GREEN)
            )
        }

        // Se todos votaram, finalizar imediatamente
        if (voteCount >= totalPlayers) {
            finishVote(sessionId)
        }

        return true
    }

    fun hasPendingVote(playerId: UUID): Boolean = playerSessions.containsKey(playerId)

    private fun finishVote(instanceId: UUID) {
        val session = activeSessions.remove(instanceId) ?: return
        session.players.forEach { playerSessions.remove(it) }

        // Escolher a dificuldade mais votada (empate = maior dificuldade)
        val result = if (session.votes.isEmpty()) {
            Difficulty.NORMAL
        } else {
            session.votes.values
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: Difficulty.NORMAL
        }

        session.players.mapNotNull { Bukkit.getPlayer(it) }.forEach { p ->
            p.sendMessage(
                Component.text("Dificuldade escolhida: ${result.icon} ${result.displayName}!", NamedTextColor.GOLD)
            )
        }

        session.onComplete?.invoke(result)
    }

    fun applyDifficulty(instanceId: UUID, difficulty: Difficulty) {
        ModifierManager.applyModifier(instanceId, ModifierManager.DungeonModifier(
            id = "difficulty_${difficulty.name}",
            name = "Dificuldade: ${difficulty.displayName}",
            description = "Modificador de dificuldade da votação",
            healthMultiplier = difficulty.healthMul,
            damageMultiplier = difficulty.damageMul,
            lootMultiplier = difficulty.lootMul,
            expMultiplier = difficulty.expMul,
        ))
    }

    fun shutdown() {
        activeSessions.clear()
        playerSessions.clear()
    }
}

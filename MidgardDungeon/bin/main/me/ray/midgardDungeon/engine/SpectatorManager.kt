package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Gerencia jogadores em modo espectador dentro da dungeon.
 * Quando um jogador morre sem vidas, entra em modo espectador
 * ao invés de causar falha instantânea da dungeon.
 */
object SpectatorManager {

    // instanceId -> conjunto de jogadores em espectador
    private val spectators = ConcurrentHashMap<UUID, ConcurrentHashMap.KeySetView<UUID, Boolean>>()
    // Salvar o GameMode anterior do jogador
    private val previousGameMode = ConcurrentHashMap<UUID, GameMode>()

    fun enterSpectator(player: Player, instance: DungeonInstance) {
        val specs = spectators.getOrPut(instance.id) { ConcurrentHashMap.newKeySet() }
        specs.add(player.uniqueId)
        previousGameMode[player.uniqueId] = player.gameMode
        player.gameMode = GameMode.SPECTATOR

        player.sendMessage(
            Component.text("Você ficou sem vidas! Assistindo em modo espectador...", NamedTextColor.GRAY)
        )
        instance.getOnlinePlayers().filter { it.uniqueId != player.uniqueId }.forEach { p ->
            p.sendMessage(
                Component.text("${player.name} está assistindo como espectador.", NamedTextColor.GRAY)
            )
        }
    }

    fun exitSpectator(player: Player) {
        val prevMode = previousGameMode.remove(player.uniqueId) ?: GameMode.SURVIVAL
        player.gameMode = prevMode
        for ((_, specs) in spectators) {
            specs.remove(player.uniqueId)
        }
    }

    fun isSpectator(instanceId: UUID, playerId: UUID): Boolean {
        return spectators[instanceId]?.contains(playerId) ?: false
    }

    fun getSpectators(instanceId: UUID): Set<UUID> {
        return spectators[instanceId]?.toSet() ?: emptySet()
    }

    fun getAlivePlayersCount(instance: DungeonInstance): Int {
        val specs = spectators[instance.id] ?: return instance.getOnlinePlayers().size
        return instance.getOnlinePlayers().count { it.uniqueId !in specs }
    }

    fun removeInstance(instanceId: UUID) {
        val specs = spectators.remove(instanceId) ?: return
        specs.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { exitSpectator(it) }
        }
    }

    fun shutdown() {
        for ((_, specs) in spectators) {
            specs.forEach { playerId ->
                Bukkit.getPlayer(playerId)?.let { player ->
                    val prevMode = previousGameMode.remove(playerId) ?: GameMode.SURVIVAL
                    player.gameMode = prevMode
                }
            }
        }
        spectators.clear()
        previousGameMode.clear()
    }
}

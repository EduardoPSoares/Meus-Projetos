package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Sistema de vidas e revive para instâncias de dungeon.
 */
object LivesManager {

    // instanceId -> playerId -> vidas restantes
    private val lives = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, Int>>()

    fun initInstance(instanceId: UUID, playerIds: Set<UUID>, maxLives: Int) {
        val playerLives = ConcurrentHashMap<UUID, Int>()
        playerIds.forEach { playerLives[it] = maxLives }
        lives[instanceId] = playerLives
    }

    fun removeInstance(instanceId: UUID) {
        lives.remove(instanceId)
    }

    fun getLives(instanceId: UUID, playerId: UUID): Int {
        return lives[instanceId]?.get(playerId) ?: -1
    }

    fun getTotalLives(instanceId: UUID): Int {
        return lives[instanceId]?.values?.sum() ?: 0
    }

    /**
     * Consome uma vida. Retorna o número de vidas restantes, ou -1 se sem vidas.
     */
    fun consumeLife(instanceId: UUID, playerId: UUID): Int {
        val playerLives = lives[instanceId] ?: return -1
        val current = playerLives[playerId] ?: return -1
        if (current <= 0) return -1
        val remaining = current - 1
        playerLives[playerId] = remaining
        return remaining
    }

    fun addLife(instanceId: UUID, playerId: UUID, amount: Int = 1) {
        val playerLives = lives[instanceId] ?: return
        val current = playerLives[playerId] ?: return
        playerLives[playerId] = current + amount
    }

    fun isTeamAlive(instanceId: UUID): Boolean {
        val playerLives = lives[instanceId] ?: return false
        return playerLives.values.any { it > 0 }
    }

    /**
     * Revive o jogador no checkpoint após um atraso.
     * Retorna true se o revive foi bem-sucedido (tinha vidas), false se sem vidas.
     */
    fun handleDeath(instance: DungeonInstance, player: Player, reviveDelayTicks: Long = 60L): Boolean {
        val remaining = consumeLife(instance.id, player.uniqueId)
        if (remaining < 0) return false

        instance.getOnlinePlayers().forEach { p ->
            p.sendMessage(
                Component.text("${player.name} caiu! Vidas restantes: $remaining", NamedTextColor.YELLOW)
            )
        }

        if (remaining <= 0) return false

        val plugin = MidgardPlugin.instance ?: return true
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (player.isOnline) {
                player.spigot().respawn()
                player.teleport(instance.getCheckpoint())
                player.sendMessage(Component.text("Você reviveu no checkpoint!", NamedTextColor.GREEN))
            }
        }, reviveDelayTicks)

        return true
    }
}

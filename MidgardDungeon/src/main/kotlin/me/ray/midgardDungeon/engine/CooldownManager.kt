package me.ray.midgardDungeon.engine

import java.util.*
import java.util.concurrent.ConcurrentHashMap

object CooldownManager {
    // Map<PlayerId, Map<DungeonId, TimestampDeExpiracao>>
    private val cooldowns = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>()

    fun initialize() {
        cooldowns.clear()
    }

    fun shutdown() {
        cooldowns.clear()
    }

    fun setCooldown(playerId: UUID, dungeonId: String, cooldownSeconds: Int) {
        if (cooldownSeconds <= 0) return
        val playerCooldowns = cooldowns.getOrPut(playerId) { ConcurrentHashMap() }
        playerCooldowns[dungeonId] = System.currentTimeMillis() + (cooldownSeconds * 1000L)
    }

    fun isOnCooldown(playerId: UUID, dungeonId: String): Boolean {
        val playerCooldowns = cooldowns[playerId] ?: return false
        val expiration = playerCooldowns[dungeonId] ?: return false
        if (System.currentTimeMillis() >= expiration) {
            playerCooldowns.remove(dungeonId)
            return false
        }
        return true
    }

    fun getRemainingSeconds(playerId: UUID, dungeonId: String): Long {
        val playerCooldowns = cooldowns[playerId] ?: return 0
        val expiration = playerCooldowns[dungeonId] ?: return 0
        val remaining = (expiration - System.currentTimeMillis()) / 1000
        return maxOf(0, remaining)
    }

    fun clearCooldown(playerId: UUID, dungeonId: String) {
        cooldowns[playerId]?.remove(dungeonId)
    }

    fun clearAllCooldowns(playerId: UUID) {
        cooldowns.remove(playerId)
    }
}

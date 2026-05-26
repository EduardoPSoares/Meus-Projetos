package me.ray.midgardDungeon.engine

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Sistema de chaves para desbloquear salas, baús e passagens secretas.
 */
object KeyManager {

    // instanceId -> playerId -> conjunto de IDs de chaves
    private val playerKeys = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, ConcurrentHashMap.KeySetView<String, Boolean>>>()

    fun initInstance(instanceId: UUID) {
        playerKeys[instanceId] = ConcurrentHashMap()
    }

    fun removeInstance(instanceId: UUID) {
        playerKeys.remove(instanceId)
    }

    fun giveKey(instanceId: UUID, playerId: UUID, keyId: String) {
        val instanceKeys = playerKeys.getOrPut(instanceId) { ConcurrentHashMap() }
        val keys = instanceKeys.getOrPut(playerId) { ConcurrentHashMap.newKeySet() }
        keys.add(keyId)

        Bukkit.getPlayer(playerId)?.sendMessage(
            Component.text("Chave obtida: $keyId", NamedTextColor.GOLD)
        )
    }

    fun giveKeyToParty(instanceId: UUID, keyId: String) {
        val instance = DungeonManager.getInstance(instanceId) ?: return
        instance.party.memberIds.forEach { playerId ->
            giveKey(instanceId, playerId, keyId)
        }
    }

    fun hasKey(instanceId: UUID, playerId: UUID, keyId: String): Boolean {
        return playerKeys[instanceId]?.get(playerId)?.contains(keyId) ?: false
    }

    fun partyHasKey(instanceId: UUID, keyId: String): Boolean {
        val instance = DungeonManager.getInstance(instanceId) ?: return false
        return instance.party.memberIds.any { hasKey(instanceId, it, keyId) }
    }

    fun useKey(instanceId: UUID, playerId: UUID, keyId: String): Boolean {
        val keys = playerKeys[instanceId]?.get(playerId) ?: return false
        val removed = keys.remove(keyId)
        if (removed) {
            Bukkit.getPlayer(playerId)?.sendMessage(
                Component.text("Chave usada: $keyId", NamedTextColor.YELLOW)
            )
        }
        return removed
    }

    fun usePartyKey(instanceId: UUID, keyId: String): Boolean {
        val instance = DungeonManager.getInstance(instanceId) ?: return false
        for (playerId in instance.party.memberIds) {
            if (useKey(instanceId, playerId, keyId)) return true
        }
        return false
    }

    fun getKeys(instanceId: UUID, playerId: UUID): Set<String> {
        return playerKeys[instanceId]?.get(playerId)?.toSet() ?: emptySet()
    }

    fun shutdown() {
        playerKeys.clear()
    }
}

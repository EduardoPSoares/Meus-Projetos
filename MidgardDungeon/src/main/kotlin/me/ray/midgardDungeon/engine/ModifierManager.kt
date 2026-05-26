package me.ray.midgardDungeon.engine

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Modificadores de dungeon que alteram a dificuldade e recompensas.
 */
object ModifierManager {

    data class DungeonModifier(
        val id: String,
        val name: String,
        val description: String,
        val healthMultiplier: Double = 1.0,
        val damageMultiplier: Double = 1.0,
        val speedMultiplier: Double = 1.0,
        val lootMultiplier: Double = 1.0,
        val expMultiplier: Double = 1.0,
        val extraMobs: Int = 0,
    )

    // instanceId -> lista de modificadores ativos
    private val instanceModifiers = ConcurrentHashMap<UUID, CopyOnWriteArrayList<DungeonModifier>>()

    fun applyModifier(instanceId: UUID, modifier: DungeonModifier) {
        instanceModifiers.getOrPut(instanceId) { CopyOnWriteArrayList() }.add(modifier)
    }

    fun getModifiers(instanceId: UUID): List<DungeonModifier> {
        return instanceModifiers[instanceId] ?: emptyList()
    }

    fun removeModifier(instanceId: UUID, modifierId: String) {
        instanceModifiers[instanceId]?.removeAll { it.id == modifierId }
    }

    fun removeInstance(instanceId: UUID) {
        instanceModifiers.remove(instanceId)
    }

    fun getCombinedHealthMultiplier(instanceId: UUID): Double {
        return getModifiers(instanceId).fold(1.0) { acc, mod -> acc * mod.healthMultiplier }
    }

    fun getCombinedDamageMultiplier(instanceId: UUID): Double {
        return getModifiers(instanceId).fold(1.0) { acc, mod -> acc * mod.damageMultiplier }
    }

    fun getCombinedSpeedMultiplier(instanceId: UUID): Double {
        return getModifiers(instanceId).fold(1.0) { acc, mod -> acc * mod.speedMultiplier }
    }

    fun getCombinedLootMultiplier(instanceId: UUID): Double {
        return getModifiers(instanceId).fold(1.0) { acc, mod -> acc * mod.lootMultiplier }
    }

    fun getCombinedExpMultiplier(instanceId: UUID): Double {
        return getModifiers(instanceId).fold(1.0) { acc, mod -> acc * mod.expMultiplier }
    }

    fun getCombinedExtraMobs(instanceId: UUID): Int {
        return getModifiers(instanceId).sumOf { it.extraMobs }
    }

    fun shutdown() {
        instanceModifiers.clear()
    }
}

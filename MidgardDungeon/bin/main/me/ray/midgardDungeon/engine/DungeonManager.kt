package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.party.Party
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object DungeonManager {
    private val activeInstances = ConcurrentHashMap<UUID, DungeonInstance>()
    private val playerInstanceMap = ConcurrentHashMap<UUID, UUID>()
    private val entityInstanceMap = ConcurrentHashMap<UUID, UUID>()

    fun initialize() {
        activeInstances.clear()
        playerInstanceMap.clear()
        entityInstanceMap.clear()
    }

    fun shutdown() {
        activeInstances.values.forEach { it.cleanup() }
        activeInstances.clear()
        playerInstanceMap.clear()
        entityInstanceMap.clear()
    }

    fun createInstance(
        dungeonId: String,
        party: Party,
        world: World,
        spawnLocation: Location,
    ): DungeonInstance {
        val instance = DungeonInstance(
            dungeonId = dungeonId,
            party = party,
            world = world,
            spawnLocation = spawnLocation,
        )
        activeInstances[instance.id] = instance
        party.memberIds.forEach { playerInstanceMap[it] = instance.id }
        return instance
    }

    fun removeInstance(instanceId: UUID) {
        val instance = activeInstances.remove(instanceId) ?: return
        instance.cleanup()
        instance.party.memberIds.forEach { playerInstanceMap.remove(it) }
    }

    /**
     * Limpa todos os subsistemas de uma instância e remove-a.
     * Ponto central para evitar duplicação de cleanup.
     */
    fun fullCleanup(instance: DungeonInstance) {
        // Restaurar inventário dos jogadores
        instance.getOnlinePlayers().forEach { InventoryManager.restore(it) }

        // Limpar subsistemas
        LivesManager.removeInstance(instance.id)
        ModifierManager.removeInstance(instance.id)
        TrapManager.removeInstance(instance.id)
        KeyManager.removeInstance(instance.id)
        StatsManager.removeInstance(instance.id)
        SpectatorManager.removeInstance(instance.id)
        CutsceneManager.removeInstance(instance.id)

        removeInstance(instance.id)

        // Limpar mundo clonado se aplicável
        if (instance.isClonedWorld) {
            WorldCloneManager.deleteClonedWorld(instance.id)
        }
    }

    fun getInstance(instanceId: UUID): DungeonInstance? = activeInstances[instanceId]

    fun getInstanceByPlayer(playerId: UUID): DungeonInstance? {
        val instanceId = playerInstanceMap[playerId] ?: return null
        return activeInstances[instanceId]
    }

    fun isPlayerInDungeon(playerId: UUID): Boolean = playerInstanceMap.containsKey(playerId)

    fun getActiveInstances(): Collection<DungeonInstance> = activeInstances.values

    fun getInstancesByDungeon(dungeonId: String): List<DungeonInstance> {
        return activeInstances.values.filter { it.dungeonId == dungeonId }
    }

    // Mapeamento de entidade → instância para busca rápida
    fun trackEntity(entityId: UUID, instanceId: UUID) {
        entityInstanceMap[entityId] = instanceId
    }

    fun untrackEntity(entityId: UUID) {
        entityInstanceMap.remove(entityId)
    }

    fun getInstanceByEntity(entityId: UUID): DungeonInstance? {
        val instanceId = entityInstanceMap[entityId] ?: return null
        return activeInstances[instanceId]
    }
}

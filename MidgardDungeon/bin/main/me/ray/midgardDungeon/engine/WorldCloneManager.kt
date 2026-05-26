package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Gerencia a clonagem de mundos para instâncias de dungeon.
 * Cada instância recebe uma cópia do mundo template que é removida ao final.
 */
object WorldCloneManager {

    private val clonedWorlds = ConcurrentHashMap<UUID, String>()

    fun cloneWorld(templateWorldName: String, instanceId: UUID): World? {
        val templateWorld = Bukkit.getWorld(templateWorldName) ?: return null
        templateWorld.save()

        val cloneName = "midgard_${templateWorldName}_${instanceId.toString().take(8)}"
        val serverDir = Bukkit.getWorldContainer()
        val templateDir = File(serverDir, templateWorldName)
        val cloneDir = File(serverDir, cloneName)

        if (!templateDir.exists()) return null
        if (cloneDir.exists()) cloneDir.deleteRecursively()

        templateDir.copyRecursively(cloneDir, overwrite = true)
        // Remover session.lock para permitir carregamento
        File(cloneDir, "session.lock").delete()
        // Remover uid.dat para que um novo UID seja gerado
        File(cloneDir, "uid.dat").delete()

        val creator = WorldCreator(cloneName)
            .environment(templateWorld.environment)
            .seed(templateWorld.seed)

        val world = Bukkit.createWorld(creator) ?: return null
        world.isAutoSave = false

        clonedWorlds[instanceId] = cloneName
        return world
    }

    fun deleteClonedWorld(instanceId: UUID) {
        val worldName = clonedWorlds.remove(instanceId) ?: return
        val world = Bukkit.getWorld(worldName) ?: return

        // Teleportar jogadores restantes para fora
        val mainSpawn = Bukkit.getWorlds().firstOrNull()?.spawnLocation
        world.players.forEach { p ->
            mainSpawn?.let { p.teleport(it) }
        }

        Bukkit.unloadWorld(world, false)

        // Deletar a pasta do mundo
        val plugin = MidgardPlugin.instance ?: return
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val worldDir = File(Bukkit.getWorldContainer(), worldName)
            worldDir.deleteRecursively()
        })
    }

    fun remapClonedWorld(oldKey: UUID, newKey: UUID) {
        val worldName = clonedWorlds.remove(oldKey) ?: return
        clonedWorlds[newKey] = worldName
    }

    fun shutdown() {
        clonedWorlds.keys.toList().forEach { deleteClonedWorld(it) }
        clonedWorlds.clear()
    }
}

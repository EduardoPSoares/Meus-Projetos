package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Sistema de progressão e desbloqueio de dungeons.
 * Jogadores devem completar dungeons pré-requisito antes de acessar dungeons avançadas.
 */
object ProgressionManager {

    // dungeonId -> lista de dungeonIds pré-requisito
    private val prerequisites = ConcurrentHashMap<String, CopyOnWriteArrayList<String>>()
    private val completedDungeons = ConcurrentHashMap<UUID, MutableSet<String>>()
    // playerId -> nível de progressão global
    private val playerLevel = ConcurrentHashMap<UUID, Int>()
    // playerId -> experiência acumulada
    private val playerExp = ConcurrentHashMap<UUID, Int>()

    private var dataFile: File? = null
    @Volatile private var dirty = false
    private var saveTaskId: Int = -1

    // Experiência necessária para cada nível (progressão exponencial)
    private fun expForLevel(level: Int): Int = 100 * level * level

    fun initialize() {
        prerequisites.clear()
        completedDungeons.clear()
        playerLevel.clear()
        playerExp.clear()
        val plugin = MidgardPlugin.instance ?: return
        dataFile = File(plugin.dataFolder, "progression.yml")
        load()
        saveTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, Runnable {
            if (dirty) {
                dirty = false
                saveNow()
            }
        }, 1200L, 1200L)
    }

    fun shutdown() {
        if (saveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(saveTaskId)
            saveTaskId = -1
        }
        saveNow()
        prerequisites.clear()
        completedDungeons.clear()
        playerLevel.clear()
        playerExp.clear()
    }

    fun registerPrerequisite(dungeonId: String, requiredDungeonId: String) {
        prerequisites.getOrPut(dungeonId) { CopyOnWriteArrayList() }.add(requiredDungeonId)
    }

    fun isUnlocked(playerId: UUID, dungeonId: String): Boolean {
        val reqs = prerequisites[dungeonId] ?: return true
        val completed = completedDungeons[playerId] ?: return reqs.isEmpty()
        return reqs.all { it in completed }
    }

    fun getLockedPrerequisites(playerId: UUID, dungeonId: String): List<String> {
        val reqs = prerequisites[dungeonId] ?: return emptyList()
        val completed = completedDungeons[playerId] ?: return reqs
        return reqs.filter { it !in completed }
    }

    fun markCompleted(playerId: UUID, dungeonId: String) {
        completedDungeons.getOrPut(playerId) { ConcurrentHashMap.newKeySet() }.add(dungeonId)
        dirty = true
    }

    fun hasCompleted(playerId: UUID, dungeonId: String): Boolean {
        return completedDungeons[playerId]?.contains(dungeonId) ?: false
    }

    fun getCompletedDungeons(playerId: UUID): Set<String> {
        return completedDungeons[playerId]?.toSet() ?: emptySet()
    }

    fun addExperience(playerId: UUID, amount: Int): Boolean {
        val currentExp = playerExp.getOrPut(playerId) { 0 } + amount
        playerExp[playerId] = currentExp
        val currentLevel = playerLevel.getOrPut(playerId) { 1 }
        val needed = expForLevel(currentLevel)

        if (currentExp >= needed) {
            playerLevel[playerId] = currentLevel + 1
            playerExp[playerId] = currentExp - needed
            dirty = true
            return true // Subiu de nível
        }
        dirty = true
        return false
    }

    fun getLevel(playerId: UUID): Int = playerLevel.getOrDefault(playerId, 1)
    fun getExp(playerId: UUID): Int = playerExp.getOrDefault(playerId, 0)
    fun getExpForNextLevel(playerId: UUID): Int = expForLevel(getLevel(playerId))

    private fun load() {
        val file = dataFile ?: return
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)

        val prereqSection = yaml.getConfigurationSection("prerequisites")
        if (prereqSection != null) {
            for (key in prereqSection.getKeys(false)) {
                prerequisites[key] = CopyOnWriteArrayList(prereqSection.getStringList(key))
            }
        }

        val completedSection = yaml.getConfigurationSection("completed")
        if (completedSection != null) {
            for (key in completedSection.getKeys(false)) {
                val playerId = try { UUID.fromString(key) } catch (_: Exception) { continue }
                val set: MutableSet<String> = ConcurrentHashMap.newKeySet()
                set.addAll(completedSection.getStringList(key))
                completedDungeons[playerId] = set
            }
        }

        val levelSection = yaml.getConfigurationSection("levels")
        if (levelSection != null) {
            for (key in levelSection.getKeys(false)) {
                val playerId = try { UUID.fromString(key) } catch (_: Exception) { continue }
                val sec = levelSection.getConfigurationSection(key) ?: continue
                playerLevel[playerId] = sec.getInt("level", 1)
                playerExp[playerId] = sec.getInt("exp", 0)
            }
        }
    }

    private fun saveNow() {
        val file = dataFile ?: return
        file.parentFile?.mkdirs()
        val yaml = YamlConfiguration()

        for ((dungeonId, reqs) in prerequisites) {
            yaml.set("prerequisites.$dungeonId", reqs)
        }
        for ((playerId, dungeons) in completedDungeons) {
            yaml.set("completed.$playerId", dungeons.toList())
        }
        for ((playerId, level) in playerLevel) {
            yaml.set("levels.$playerId.level", level)
            yaml.set("levels.$playerId.exp", playerExp[playerId] ?: 0)
        }
        yaml.save(file)
    }
}

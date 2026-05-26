package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Cooldowns persistentes salvos em arquivo YAML.
 * Sobrevive reinicializações do servidor.
 */
object PersistentCooldownManager {

    private val cooldowns = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>()
    private var dataFile: File? = null
    @Volatile private var dirty = false
    private var saveTaskId: Int = -1

    fun initialize() {
        cooldowns.clear()
        val plugin = MidgardPlugin.instance ?: return
        dataFile = File(plugin.dataFolder, "cooldowns.yml")
        load()
        // Auto-save a cada 60 segundos se houver alterações
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
        cooldowns.clear()
    }

    fun setCooldown(playerId: UUID, dungeonId: String, cooldownSeconds: Int) {
        if (cooldownSeconds <= 0) return
        val playerCooldowns = cooldowns.getOrPut(playerId) { ConcurrentHashMap() }
        playerCooldowns[dungeonId] = System.currentTimeMillis() + (cooldownSeconds * 1000L)
        dirty = true
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
        dirty = true
    }

    fun clearAllCooldowns(playerId: UUID) {
        cooldowns.remove(playerId)
        dirty = true
    }

    private fun load() {
        val file = dataFile ?: return
        if (!file.exists()) return

        val yaml = YamlConfiguration.loadConfiguration(file)
        for (playerKey in yaml.getKeys(false)) {
            val playerId = try { UUID.fromString(playerKey) } catch (_: Exception) { continue }
            val section = yaml.getConfigurationSection(playerKey) ?: continue
            val playerCooldowns = ConcurrentHashMap<String, Long>()
            for (dungeonId in section.getKeys(false)) {
                val expiration = section.getLong(dungeonId, 0L)
                if (expiration > System.currentTimeMillis()) {
                    playerCooldowns[dungeonId] = expiration
                }
            }
            if (playerCooldowns.isNotEmpty()) {
                cooldowns[playerId] = playerCooldowns
            }
        }
    }

    private fun saveNow() {
        val file = dataFile ?: return
        file.parentFile?.mkdirs()
        val yaml = YamlConfiguration()
        for ((playerId, playerCooldowns) in cooldowns) {
            for ((dungeonId, expiration) in playerCooldowns) {
                if (expiration > System.currentTimeMillis()) {
                    yaml.set("$playerId.$dungeonId", expiration)
                }
            }
        }
        yaml.save(file)
    }
}

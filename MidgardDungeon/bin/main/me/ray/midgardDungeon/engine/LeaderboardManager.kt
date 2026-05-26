package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Leaderboard / ranking de dungeons persistente.
 */
object LeaderboardManager {

    data class LeaderboardEntry(
        val playerName: String,
        val playerId: UUID,
        val dungeonId: String,
        val timeSeconds: Long,
        val score: Int,
        val timestamp: Long = System.currentTimeMillis(),
    )

    // dungeonId -> lista ordenada de entradas
    private val leaderboards = ConcurrentHashMap<String, MutableList<LeaderboardEntry>>()
    private var dataFile: File? = null
    @Volatile private var dirty = false
    private var saveTaskId: Int = -1

    fun initialize() {
        leaderboards.clear()
        val plugin = MidgardPlugin.instance ?: return
        dataFile = File(plugin.dataFolder, "leaderboard.yml")
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
        leaderboards.clear()
    }

    fun submitScore(entry: LeaderboardEntry) {
        val entries = leaderboards.getOrPut(entry.dungeonId) { mutableListOf() }
        entries.add(entry)
        entries.sortByDescending { it.score }
        // Manter top 100
        while (entries.size > 100) entries.removeAt(entries.lastIndex)
        dirty = true
    }

    fun getTopScores(dungeonId: String, limit: Int = 10): List<LeaderboardEntry> {
        return leaderboards[dungeonId]?.take(limit) ?: emptyList()
    }

    fun getFastestTimes(dungeonId: String, limit: Int = 10): List<LeaderboardEntry> {
        return leaderboards[dungeonId]
            ?.sortedBy { it.timeSeconds }
            ?.take(limit) ?: emptyList()
    }

    fun getPlayerBest(playerId: UUID, dungeonId: String): LeaderboardEntry? {
        return leaderboards[dungeonId]?.filter { it.playerId == playerId }?.maxByOrNull { it.score }
    }

    fun getPlayerRank(playerId: UUID, dungeonId: String): Int {
        val entries = leaderboards[dungeonId] ?: return -1
        return entries.indexOfFirst { it.playerId == playerId } + 1
    }

    private fun load() {
        val file = dataFile ?: return
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)

        for (dungeonId in yaml.getKeys(false)) {
            val section = yaml.getConfigurationSection(dungeonId) ?: continue
            val entries = mutableListOf<LeaderboardEntry>()
            for (key in section.getKeys(false)) {
                val entrySection = section.getConfigurationSection(key) ?: continue
                val playerIdStr = entrySection.getString("playerId") ?: continue
                val playerId = try { UUID.fromString(playerIdStr) } catch (_: Exception) { continue }
                entries.add(LeaderboardEntry(
                    playerName = entrySection.getString("playerName", "")!!,
                    playerId = playerId,
                    dungeonId = dungeonId,
                    timeSeconds = entrySection.getLong("timeSeconds", 0),
                    score = entrySection.getInt("score", 0),
                    timestamp = entrySection.getLong("timestamp", 0),
                ))
            }
            entries.sortByDescending { it.score }
            leaderboards[dungeonId] = entries
        }
    }

    private fun saveNow() {
        val file = dataFile ?: return
        file.parentFile?.mkdirs()
        val yaml = YamlConfiguration()
        for ((dungeonId, entries) in leaderboards) {
            for ((index, entry) in entries.withIndex()) {
                yaml.set("$dungeonId.$index.playerName", entry.playerName)
                yaml.set("$dungeonId.$index.playerId", entry.playerId.toString())
                yaml.set("$dungeonId.$index.timeSeconds", entry.timeSeconds)
                yaml.set("$dungeonId.$index.score", entry.score)
                yaml.set("$dungeonId.$index.timestamp", entry.timestamp)
            }
        }
        yaml.save(file)
    }
}

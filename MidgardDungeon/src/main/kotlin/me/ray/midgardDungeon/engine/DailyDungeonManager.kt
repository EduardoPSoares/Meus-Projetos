package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Gerencia dungeons diárias e semanais com rotação automática.
 */
object DailyDungeonManager {

    data class DailyDungeon(
        val dungeonId: String,
        val date: String,
        val bonusExpMultiplier: Double = 2.0,
        val bonusLootMultiplier: Double = 1.5,
    )

    private val availableDungeonIds = CopyOnWriteArrayList<String>()
    private var currentDaily: DailyDungeon? = null
    private var currentWeekly: DailyDungeon? = null
    private var dataFile: File? = null
    // string do playerId -> conjunto de datas completadas
    private val completedDaily = ConcurrentHashMap<String, MutableSet<String>>()
    private val completedWeekly = ConcurrentHashMap<String, MutableSet<String>>()
    @Volatile private var dirty = false
    private var saveTaskId: Int = -1

    fun initialize() {
        val plugin = MidgardPlugin.instance ?: return
        dataFile = File(plugin.dataFolder, "daily_dungeon.yml")
        load()
        refreshIfNeeded()
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
    }

    fun registerDungeonId(dungeonId: String) {
        if (dungeonId !in availableDungeonIds) {
            availableDungeonIds.add(dungeonId)
        }
    }

    fun getCurrentDaily(): DailyDungeon? {
        refreshIfNeeded()
        return currentDaily
    }

    fun getCurrentWeekly(): DailyDungeon? {
        refreshIfNeeded()
        return currentWeekly
    }

    fun isDailyDungeon(dungeonId: String): Boolean {
        return currentDaily?.dungeonId == dungeonId
    }

    fun isWeeklyDungeon(dungeonId: String): Boolean {
        return currentWeekly?.dungeonId == dungeonId
    }

    fun markDailyComplete(playerId: String) {
        val today = LocalDate.now().toString()
        completedDaily.getOrPut(playerId) { ConcurrentHashMap.newKeySet() }.add(today)
        dirty = true
    }

    fun markWeeklyComplete(playerId: String) {
        val week = getWeekKey()
        completedWeekly.getOrPut(playerId) { ConcurrentHashMap.newKeySet() }.add(week)
        dirty = true
    }

    fun hasDailyCompleted(playerId: String): Boolean {
        val today = LocalDate.now().toString()
        return completedDaily[playerId]?.contains(today) ?: false
    }

    fun hasWeeklyCompleted(playerId: String): Boolean {
        val week = getWeekKey()
        return completedWeekly[playerId]?.contains(week) ?: false
    }

    private fun refreshIfNeeded() {
        val today = LocalDate.now().toString()
        val weekKey = getWeekKey()

        if (currentDaily == null || currentDaily?.date != today) {
            if (availableDungeonIds.isNotEmpty()) {
                val hash = today.hashCode().and(Int.MAX_VALUE)
                val idx = hash % availableDungeonIds.size
                currentDaily = DailyDungeon(
                    dungeonId = availableDungeonIds[idx],
                    date = today,
                    bonusExpMultiplier = 2.0,
                    bonusLootMultiplier = 1.5,
                )
            }
        }

        if (currentWeekly == null || currentWeekly?.date != weekKey) {
            if (availableDungeonIds.isNotEmpty()) {
                val hash = weekKey.hashCode().and(Int.MAX_VALUE)
                val idx = hash % availableDungeonIds.size
                currentWeekly = DailyDungeon(
                    dungeonId = availableDungeonIds[idx],
                    date = weekKey,
                    bonusExpMultiplier = 3.0,
                    bonusLootMultiplier = 2.0,
                )
            }
        }
    }

    private fun getWeekKey(): String {
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        return "week-${monday}"
    }

    private fun load() {
        val file = dataFile ?: return
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)

        val dungeonIds = yaml.getStringList("availableDungeonIds")
        availableDungeonIds.clear()
        availableDungeonIds.addAll(dungeonIds)

        val dailySection = yaml.getConfigurationSection("completedDaily")
        if (dailySection != null) {
            for (key in dailySection.getKeys(false)) {
        val dailySet: MutableSet<String> = ConcurrentHashMap.newKeySet()
                dailySet.addAll(dailySection.getStringList(key))
                completedDaily[key] = dailySet
            }
        }

        val weeklySection = yaml.getConfigurationSection("completedWeekly")
        if (weeklySection != null) {
            for (key in weeklySection.getKeys(false)) {
                val weeklySet: MutableSet<String> = ConcurrentHashMap.newKeySet()
                weeklySet.addAll(weeklySection.getStringList(key))
                completedWeekly[key] = weeklySet
            }
        }
    }

    private fun saveNow() {
        val file = dataFile ?: return
        file.parentFile?.mkdirs()
        val yaml = YamlConfiguration()
        yaml.set("availableDungeonIds", availableDungeonIds)
        for ((playerId, dates) in completedDaily) {
            yaml.set("completedDaily.$playerId", dates.toList())
        }
        for ((playerId, weeks) in completedWeekly) {
            yaml.set("completedWeekly.$playerId", weeks.toList())
        }
        yaml.save(file)
    }
}

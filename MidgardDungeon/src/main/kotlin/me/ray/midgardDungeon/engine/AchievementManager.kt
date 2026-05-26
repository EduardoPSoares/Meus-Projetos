package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Sistema de conquistas persistente.
 */
object AchievementManager {

    data class Achievement(
        val id: String,
        val name: String,
        val description: String,
        val icon: String = "⭐",
    )

    private val registeredAchievements = ConcurrentHashMap<String, Achievement>()
    // playerId -> conjunto de IDs de conquistas
    private val playerAchievements = ConcurrentHashMap<UUID, ConcurrentHashMap.KeySetView<String, Boolean>>()
    private var dataFile: File? = null
    @Volatile private var dirty = false
    private var saveTaskId: Int = -1

    fun initialize() {
        registeredAchievements.clear()
        playerAchievements.clear()

        val plugin = MidgardPlugin.instance ?: return
        dataFile = File(plugin.dataFolder, "achievements.yml")
        load()

        registerDefaults()
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
        registeredAchievements.clear()
        playerAchievements.clear()
    }

    private fun registerDefaults() {
        register(Achievement("first_dungeon", "Primeiro Passo", "Complete sua primeira dungeon", "🏰"))
        register(Achievement("speed_runner", "Velocista", "Complete uma dungeon em menos de 5 minutos", "⚡"))
        register(Achievement("no_deaths", "Imortal", "Complete uma dungeon sem morrer", "💀"))
        register(Achievement("boss_slayer", "Matador de Bosses", "Derrote 10 bosses", "👑"))
        register(Achievement("team_player", "Jogador de Equipe", "Complete 5 dungeons em grupo", "🤝"))
        register(Achievement("collector", "Colecionador", "Colete 100 itens de loot", "📦"))
        register(Achievement("survivor", "Sobrevivente", "Sobreviva com menos de 1 coração", "❤"))
        register(Achievement("explorer", "Explorador", "Descubra 5 salas secretas", "🗺"))
        register(Achievement("daily_warrior", "Guerreiro Diário", "Complete 7 dungeons diárias", "📅"))
        register(Achievement("weekly_champion", "Campeão Semanal", "Complete 4 dungeons semanais", "🏆"))
    }

    fun register(achievement: Achievement) {
        registeredAchievements[achievement.id] = achievement
    }

    fun grant(playerId: UUID, achievementId: String): Boolean {
        val achievement = registeredAchievements[achievementId] ?: return false
        val achievements = playerAchievements.getOrPut(playerId) { ConcurrentHashMap.newKeySet() }
        if (!achievements.add(achievementId)) return false // Já possui

        Bukkit.getPlayer(playerId)?.let { player ->
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
            player.sendMessage(
                Component.text("${achievement.icon} CONQUISTA DESBLOQUEADA!", NamedTextColor.GOLD)
            )
            player.sendMessage(
                Component.text(achievement.name, NamedTextColor.YELLOW)
            )
            player.sendMessage(
                Component.text(achievement.description, NamedTextColor.GRAY)
            )
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
        }

        dirty = true
        return true
    }

    fun hasAchievement(playerId: UUID, achievementId: String): Boolean {
        return playerAchievements[playerId]?.contains(achievementId) ?: false
    }

    fun getPlayerAchievements(playerId: UUID): Set<String> {
        return playerAchievements[playerId]?.toSet() ?: emptySet()
    }

    fun getAchievementCount(playerId: UUID): Int {
        return playerAchievements[playerId]?.size ?: 0
    }

    fun getTotalAchievements(): Int = registeredAchievements.size

    fun getAchievement(id: String): Achievement? = registeredAchievements[id]

    private fun load() {
        val file = dataFile ?: return
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)

        for (key in yaml.getKeys(false)) {
            val playerId = try { UUID.fromString(key) } catch (_: Exception) { continue }
            val achievements: ConcurrentHashMap.KeySetView<String, Boolean> = ConcurrentHashMap.newKeySet()
            achievements.addAll(yaml.getStringList(key))
            playerAchievements[playerId] = achievements
        }
    }

    private fun saveNow() {
        val file = dataFile ?: return
        file.parentFile?.mkdirs()
        val yaml = YamlConfiguration()
        for ((playerId, achievements) in playerAchievements) {
            yaml.set(playerId.toString(), achievements.toList())
        }
        yaml.save(file)
    }
}

package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Estatísticas pós-dungeon e históricas.
 */
object StatsManager {

    data class DungeonRunStats(
        val instanceId: UUID,
        val dungeonId: String,
        val startTime: Long,
        val endTime: Long = System.currentTimeMillis(),
        val completed: Boolean,
        val players: List<PlayerRunStats>,
    )

    data class PlayerRunStats(
        val playerId: UUID,
        val playerName: String,
        var kills: Int = 0,
        var deaths: Int = 0,
        var damageDealt: Double = 0.0,
        var damageTaken: Double = 0.0,
        var healingDone: Double = 0.0,
        var keysCollected: Int = 0,
        var trapsTriggered: Int = 0,
        var secretsFound: Int = 0,
    )

    // instanceId -> playerId -> estatísticas ao vivo sendo rastreadas
    private val liveStats = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, PlayerRunStats>>()
    private val playerHistory = ConcurrentHashMap<UUID, CopyOnWriteArrayList<DungeonRunStats>>()
    // playerId -> estatísticas cumulativas
    private val cumulativeStats = ConcurrentHashMap<UUID, PlayerRunStats>()
    private var dataFile: File? = null
    @Volatile private var dirty = false
    private var saveTaskId: Int = -1

    fun initialize() {
        liveStats.clear()
        playerHistory.clear()
        cumulativeStats.clear()
        val plugin = MidgardPlugin.instance ?: return
        dataFile = File(plugin.dataFolder, "stats.yml")
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
        liveStats.clear()
        playerHistory.clear()
        cumulativeStats.clear()
    }

    fun initInstance(instanceId: UUID, players: Set<UUID>) {
        val stats = ConcurrentHashMap<UUID, PlayerRunStats>()
        players.forEach { pid ->
            val name = Bukkit.getPlayer(pid)?.name ?: "Desconhecido"
            stats[pid] = PlayerRunStats(playerId = pid, playerName = name)
        }
        liveStats[instanceId] = stats
    }

    fun removeInstance(instanceId: UUID) {
        liveStats.remove(instanceId)
    }

    fun recordKill(instanceId: UUID, playerId: UUID) {
        liveStats[instanceId]?.get(playerId)?.let { it.kills++ }
    }

    fun recordDeath(instanceId: UUID, playerId: UUID) {
        liveStats[instanceId]?.get(playerId)?.let { it.deaths++ }
    }

    fun recordDamageDealt(instanceId: UUID, playerId: UUID, amount: Double) {
        liveStats[instanceId]?.get(playerId)?.let { it.damageDealt += amount }
    }

    fun recordDamageTaken(instanceId: UUID, playerId: UUID, amount: Double) {
        liveStats[instanceId]?.get(playerId)?.let { it.damageTaken += amount }
    }

    fun recordHealingDone(instanceId: UUID, playerId: UUID, amount: Double) {
        liveStats[instanceId]?.get(playerId)?.let { it.healingDone += amount }
    }

    fun recordKeyCollected(instanceId: UUID, playerId: UUID) {
        liveStats[instanceId]?.get(playerId)?.let { it.keysCollected++ }
    }

    fun recordTrapTriggered(instanceId: UUID, playerId: UUID) {
        liveStats[instanceId]?.get(playerId)?.let { it.trapsTriggered++ }
    }

    fun recordSecretFound(instanceId: UUID, playerId: UUID) {
        liveStats[instanceId]?.get(playerId)?.let { it.secretsFound++ }
    }

    fun finishRun(instanceId: UUID, dungeonId: String, startTime: Long, completed: Boolean) {
        val stats = liveStats[instanceId] ?: return
        val playerStats = stats.values.toList()

        val runStats = DungeonRunStats(
            instanceId = instanceId,
            dungeonId = dungeonId,
            startTime = startTime,
            completed = completed,
            players = playerStats,
        )

        // Armazenar no histórico de cada jogador
        for (ps in playerStats) {
            playerHistory.getOrPut(ps.playerId) { CopyOnWriteArrayList() }.add(runStats)
            // Atualizar cumulativo
            val cumulative = cumulativeStats.getOrPut(ps.playerId) {
                PlayerRunStats(playerId = ps.playerId, playerName = ps.playerName)
            }
            cumulative.kills += ps.kills
            cumulative.deaths += ps.deaths
            cumulative.damageDealt += ps.damageDealt
            cumulative.damageTaken += ps.damageTaken
            cumulative.healingDone += ps.healingDone
            cumulative.keysCollected += ps.keysCollected
            cumulative.trapsTriggered += ps.trapsTriggered
            cumulative.secretsFound += ps.secretsFound
        }

        dirty = true
    }

    fun showEndScreen(instance: DungeonInstance, completed: Boolean) {
        val stats = liveStats[instance.id] ?: return
        val elapsed = instance.getElapsedSeconds()
        val min = elapsed / 60
        val sec = elapsed % 60

        // Determinar MVP e medalhas
        val mvpKills = stats.values.maxByOrNull { it.kills }
        val mvpDamage = stats.values.maxByOrNull { it.damageDealt }
        val mvpHealing = stats.values.maxByOrNull { it.healingDone }
        val leastDeaths = stats.values.minByOrNull { it.deaths }

        instance.getOnlinePlayers().forEach { player ->
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
            player.sendMessage(
                Component.text(
                    if (completed) "⚔ MASMORRA COMPLETA! ⚔" else "✗ MASMORRA FALHOU ✗",
                    if (completed) NamedTextColor.GREEN else NamedTextColor.RED
                )
            )
            player.sendMessage(Component.text("Tempo: ${min}m ${sec}s", NamedTextColor.AQUA))

            // Comparar com melhor run do jogador
            val best = LeaderboardManager.getPlayerBest(player.uniqueId, instance.dungeonId)
            if (best != null) {
                val bestMin = best.timeSeconds / 60
                val bestSec = best.timeSeconds % 60
                val diff = elapsed - best.timeSeconds
                val diffStr = if (diff <= 0) "§a-${-diff}s (novo recorde!)" else "§c+${diff}s"
                player.sendMessage(
                    Component.text("Melhor tempo: ${bestMin}m ${bestSec}s ", NamedTextColor.GRAY)
                        .append(Component.text("($diffStr)", if (diff <= 0) NamedTextColor.GREEN else NamedTextColor.RED))
                )
            }

            player.sendMessage(Component.text("─────────────────────", NamedTextColor.DARK_GRAY))

            // Estatísticas detalhadas de cada jogador
            for ((_, ps) in stats) {
                player.sendMessage(
                    Component.text("${ps.playerName}:", NamedTextColor.YELLOW)
                )
                player.sendMessage(
                    Component.text("  ⚔ ${ps.kills} abates ", NamedTextColor.GREEN)
                        .append(Component.text("💀 ${ps.deaths} mortes ", NamedTextColor.RED))
                        .append(Component.text("🗡 ${String.format("%.0f", ps.damageDealt)} dano", NamedTextColor.GOLD))
                )
                player.sendMessage(
                    Component.text("  🛡 ${String.format("%.0f", ps.damageTaken)} recebido ", NamedTextColor.GRAY)
                        .append(Component.text("❤ ${String.format("%.0f", ps.healingDone)} cura ", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text("🔑 ${ps.keysCollected} chaves ", NamedTextColor.AQUA))
                        .append(Component.text("🗺 ${ps.secretsFound} segredos", NamedTextColor.DARK_AQUA))
                )
            }

            // Medalhas MVP
            player.sendMessage(Component.text("─────────────────────", NamedTextColor.DARK_GRAY))
            player.sendMessage(Component.text("🏅 MEDALHAS:", NamedTextColor.GOLD))
            if (mvpKills != null && mvpKills.kills > 0) {
                player.sendMessage(
                    Component.text("  ⚔ Maior Matador: ${mvpKills.playerName} (${mvpKills.kills} abates)", NamedTextColor.GREEN)
                )
            }
            if (mvpDamage != null && mvpDamage.damageDealt > 0) {
                player.sendMessage(
                    Component.text("  🗡 Mais Dano: ${mvpDamage.playerName} (${String.format("%.0f", mvpDamage.damageDealt)})", NamedTextColor.GOLD)
                )
            }
            if (mvpHealing != null && mvpHealing.healingDone > 0) {
                player.sendMessage(
                    Component.text("  ❤ Melhor Curandeiro: ${mvpHealing.playerName} (${String.format("%.0f", mvpHealing.healingDone)})", NamedTextColor.LIGHT_PURPLE)
                )
            }
            if (leastDeaths != null && stats.size > 1) {
                player.sendMessage(
                    Component.text("  🛡 Sobrevivente: ${leastDeaths.playerName} (${leastDeaths.deaths} mortes)", NamedTextColor.AQUA)
                )
            }

            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
        }
    }

    fun getRunStats(instanceId: UUID, playerId: UUID): PlayerRunStats? = liveStats[instanceId]?.get(playerId)

    fun getPlayerStats(playerId: UUID): PlayerRunStats? = cumulativeStats[playerId]
    fun getPlayerHistory(playerId: UUID): List<DungeonRunStats> = playerHistory[playerId] ?: emptyList()
    fun getTotalCompletions(playerId: UUID): Int = getPlayerHistory(playerId).count { it.completed }
    fun getTotalRuns(playerId: UUID): Int = getPlayerHistory(playerId).size

    private fun load() {
        val file = dataFile ?: return
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)

        val section = yaml.getConfigurationSection("cumulative") ?: return
        for (key in section.getKeys(false)) {
            val playerId = try { UUID.fromString(key) } catch (_: Exception) { continue }
            val ps = section.getConfigurationSection(key) ?: continue
            cumulativeStats[playerId] = PlayerRunStats(
                playerId = playerId,
                playerName = ps.getString("playerName", "")!!,
                kills = ps.getInt("kills"),
                deaths = ps.getInt("deaths"),
                damageDealt = ps.getDouble("damageDealt"),
                damageTaken = ps.getDouble("damageTaken"),
                healingDone = ps.getDouble("healingDone"),
                keysCollected = ps.getInt("keysCollected"),
                trapsTriggered = ps.getInt("trapsTriggered"),
                secretsFound = ps.getInt("secretsFound"),
            )
        }
    }

    private fun saveNow() {
        val file = dataFile ?: return
        file.parentFile?.mkdirs()
        val yaml = YamlConfiguration()
        for ((playerId, stats) in cumulativeStats) {
            val key = "cumulative.$playerId"
            yaml.set("$key.playerName", stats.playerName)
            yaml.set("$key.kills", stats.kills)
            yaml.set("$key.deaths", stats.deaths)
            yaml.set("$key.damageDealt", stats.damageDealt)
            yaml.set("$key.damageTaken", stats.damageTaken)
            yaml.set("$key.healingDone", stats.healingDone)
            yaml.set("$key.keysCollected", stats.keysCollected)
            yaml.set("$key.trapsTriggered", stats.trapsTriggered)
            yaml.set("$key.secretsFound", stats.secretsFound)
        }
        yaml.save(file)
    }
}

package me.ray.midgardDungeon.entries.audience

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.AudienceDisplay
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.TickableDisplay
import me.ray.midgardDungeon.MidgardPlugin
import me.ray.midgardDungeon.engine.DungeonInstance
import me.ray.midgardDungeon.engine.DungeonManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.scoreboard.Criteria as ScoreboardCriteria
import org.bukkit.scoreboard.DisplaySlot

@Entry("dungeon_audience", "Gerencia HUD e scoreboard para jogadores de dungeon", Colors.GREEN, "mdi:monitor-dashboard")
/**
 * A entry `Dungeon Audience` mostra informações da dungeon (wave, tempo, sala) na barra lateral
 * e lida com respawn nos checkpoints.
 *
 * ## Como isso pode ser usado?
 * Adicione a uma página de manifesto para que todos os jogadores dentro de dungeons vejam informações em tempo real.
 */
class DungeonAudienceEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Intervalo de atualização em ticks (20 = a cada segundo).")
    val updateInterval: Int = 20,
    @Help("Mostrar tempo decorrido no scoreboard.")
    val showTimer: Boolean = true,
    @Help("Mostrar número da wave atual no scoreboard.")
    val showWave: Boolean = true,
    @Help("Mostrar contagem de mobs vivos no scoreboard.")
    val showMobCount: Boolean = true,
    @Help("Respawnar jogadores no checkpoint da dungeon ao morrer.")
    val respawnAtCheckpoint: Boolean = true,
) : AudienceEntry {
    override suspend fun display(): AudienceDisplay = DungeonAudienceDisplay(this)
}

/** Renderiza um [Component] do Adventure para uma string codificada com § para chaves de score do scoreboard. */
private fun legacy(component: Component): String =
    LegacyComponentSerializer.legacySection().serialize(component)

class DungeonAudienceDisplay(
    private val config: DungeonAudienceEntry,
) : AudienceDisplay(), TickableDisplay, Listener {

    private var tickCounter = 0

    override fun onPlayerAdd(player: Player) {
        // Registrar este Listener no momento em que o primeiro jogador é adicionado
        if (players.size == 1) {
            val plugin = MidgardPlugin.instance ?: return
            Bukkit.getPluginManager().registerEvents(this, plugin)
        }
    }

    override fun onPlayerRemove(player: Player) {
        player.scoreboard = Bukkit.getScoreboardManager().newScoreboard
        // Desregistrar quando não houver mais jogadores
        if (players.isEmpty()) {
            HandlerList.unregisterAll(this)
        }
    }

    override fun tick() {
        tickCounter++
        if (tickCounter % config.updateInterval != 0) return
        players.forEach { player ->
            val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return@forEach
            updateScoreboard(player, instance)
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (player !in this) return
        if (!config.respawnAtCheckpoint) return

        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return

        // Skip if LivesManager is handling respawn for this instance
        if (me.ray.midgardDungeon.engine.LivesManager.getLives(instance.id, player.uniqueId) >= 0) return

        val checkpoint = instance.getCheckpoint()
        val plugin = MidgardPlugin.instance ?: return

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            player.spigot().respawn()
            player.teleport(checkpoint)
            player.sendMessage(Component.text("Respawnado no checkpoint!", NamedTextColor.YELLOW))
        }, 1L)
    }

    private fun updateScoreboard(player: Player, instance: DungeonInstance) {
        val scoreboard = player.scoreboard.let {
            if (it == Bukkit.getScoreboardManager().mainScoreboard) {
                Bukkit.getScoreboardManager().newScoreboard.also { sb -> player.scoreboard = sb }
            } else it
        }

        scoreboard.getObjective("midgard_dungeon")?.unregister()

        val objective = scoreboard.registerNewObjective(
            "midgard_dungeon",
            ScoreboardCriteria.DUMMY,
            Component.text("Masmorra", NamedTextColor.GOLD, TextDecoration.BOLD)
        )
        objective.displaySlot = DisplaySlot.SIDEBAR

        var line = 14

        val stateName = when (instance.state) {
            me.ray.midgardDungeon.engine.DungeonState.WAITING -> "ESPERANDO"
            me.ray.midgardDungeon.engine.DungeonState.STARTING -> "INICIANDO"
            me.ray.midgardDungeon.engine.DungeonState.IN_PROGRESS -> "EM PROGRESSO"
            me.ray.midgardDungeon.engine.DungeonState.BOSS_FIGHT -> "LUTA DE BOSS"
            me.ray.midgardDungeon.engine.DungeonState.COMPLETED -> "COMPLETA"
            me.ray.midgardDungeon.engine.DungeonState.FAILED -> "FALHOU"
        }
        val stateLabel: String = legacy(Component.text("Estado: ", NamedTextColor.GOLD)
            .append(Component.text(stateName, NamedTextColor.WHITE)))
        objective.getScore(stateLabel).score = line--

        val sep1: String = legacy(Component.text("─────────────", NamedTextColor.DARK_GRAY))
        objective.getScore(sep1).score = line--

        if (config.showWave) {
            val waveLabel: String = legacy(Component.text("Onda: ", NamedTextColor.YELLOW)
                .append(Component.text("${instance.currentWave}", NamedTextColor.WHITE)))
            objective.getScore(waveLabel).score = line--
        }

        if (config.showMobCount) {
            val alive = instance.getAliveTrackedEntities().size
            val mobLabel: String = legacy(Component.text("Mobs: ", NamedTextColor.RED)
                .append(Component.text("$alive", NamedTextColor.WHITE)))
            objective.getScore(mobLabel).score = line--
        }

        if (config.showTimer) {
            val elapsed = instance.getElapsedSeconds()
            val min = elapsed / 60
            val sec = elapsed % 60
            val timeLabel: String = legacy(Component.text("Tempo: ", NamedTextColor.AQUA)
                .append(Component.text("${min}m ${sec}s", NamedTextColor.WHITE)))
            objective.getScore(timeLabel).score = line--
        }

        // Exibição de vidas
        val lives = me.ray.midgardDungeon.engine.LivesManager.getLives(instance.id, player.uniqueId)
        if (lives >= 0) {
            val livesLabel: String = legacy(Component.text("Vidas: ", NamedTextColor.RED)
                .append(Component.text("$lives", NamedTextColor.WHITE)))
            objective.getScore(livesLabel).score = line--
        }

        // Contagem de modificadores
        val modCount = me.ray.midgardDungeon.engine.ModifierManager.getModifiers(instance.id).size
        if (modCount > 0) {
            val modLabel: String = legacy(Component.text("Modificadores: ", NamedTextColor.GOLD)
                .append(Component.text("$modCount", NamedTextColor.WHITE)))
            objective.getScore(modLabel).score = line--
        }

        val sep2: String = legacy(Component.text("─────────────  ", NamedTextColor.DARK_GRAY))
        objective.getScore(sep2).score = line--

        val roomLabel: String = legacy(Component.text("Sala: ", NamedTextColor.LIGHT_PURPLE)
            .append(Component.text("${instance.currentRoom + 1}", NamedTextColor.WHITE)))
        objective.getScore(roomLabel).score = line--

        val online = instance.getOnlinePlayers().size
        val partySize = instance.party.size
        val partyLabel: String = legacy(Component.text("Grupo: ", NamedTextColor.GREEN)
            .append(Component.text("$online/$partySize", NamedTextColor.WHITE)))
        objective.getScore(partyLabel).score = line
    }
}

package me.ray.midgardDungeon.entries.action

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import me.ray.midgardDungeon.engine.DungeonManager
import me.ray.midgardDungeon.engine.StatsManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("show_stats_action", "Mostra as estatísticas do jogador", Colors.CYAN, "mdi:chart-bar")
/**
 * A ação `Show Stats` exibe estatísticas cumulativas e da run atual.
 *
 * ## Como isso pode ser usado?
 * Vincule a um NPC ou menu para mostrar estatísticas históricas.
 */
class ShowStatsAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Se deve mostrar estatísticas cumulativas (true) ou da run atual (false).")
    val showCumulative: Boolean = true,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA))

        if (showCumulative) {
            val stats = StatsManager.getPlayerStats(player.uniqueId)
            if (stats == null) {
                player.sendMessage(Component.text("Nenhuma estatística registrada.", NamedTextColor.GRAY))
            } else {
                player.sendMessage(Component.text("📊 ESTATÍSTICAS GERAIS", NamedTextColor.AQUA))
                player.sendMessage(Component.text("─────────────────────", NamedTextColor.DARK_GRAY))
                player.sendMessage(Component.text("Total de runs: ${StatsManager.getTotalRuns(player.uniqueId)}", NamedTextColor.YELLOW))
                player.sendMessage(Component.text("Completas: ${StatsManager.getTotalCompletions(player.uniqueId)}", NamedTextColor.GREEN))
                player.sendMessage(Component.text("Abates: ${stats.kills}", NamedTextColor.RED))
                player.sendMessage(Component.text("Mortes: ${stats.deaths}", NamedTextColor.DARK_RED))
                player.sendMessage(Component.text("Dano causado: ${String.format("%.0f", stats.damageDealt)}", NamedTextColor.GOLD))
                player.sendMessage(Component.text("Dano recebido: ${String.format("%.0f", stats.damageTaken)}", NamedTextColor.DARK_RED))
                player.sendMessage(Component.text("Chaves coletadas: ${stats.keysCollected}", NamedTextColor.YELLOW))
                player.sendMessage(Component.text("Segredos encontrados: ${stats.secretsFound}", NamedTextColor.LIGHT_PURPLE))
            }
        } else {
            player.sendMessage(Component.text("📊 ESTATÍSTICAS DA RUN", NamedTextColor.AQUA))
            player.sendMessage(Component.text("(disponível na tela final)", NamedTextColor.GRAY))
        }

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.AQUA))
    }
}

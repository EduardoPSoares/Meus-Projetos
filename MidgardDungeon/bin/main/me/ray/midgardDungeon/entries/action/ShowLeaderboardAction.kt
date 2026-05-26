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
import me.ray.midgardDungeon.engine.LeaderboardManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("show_leaderboard_action", "Mostra o ranking da dungeon", Colors.DARK_ORANGE, "mdi:trophy")
/**
 * A ação `Show Leaderboard` exibe o ranking dos melhores jogadores.
 *
 * ## Como isso pode ser usado?
 * Vincule a um NPC de ranking ou painel para mostrar os top scores.
 */
class ShowLeaderboardAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("ID da dungeon para mostrar o ranking.")
    val dungeonId: String = "",
    @Help("Quantidade de posições a mostrar.")
    val topCount: Int = 10,
    @Help("Se deve mostrar por tempo mais rápido em vez de pontuação.")
    val showByTime: Boolean = false,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val entries = if (showByTime) {
            LeaderboardManager.getFastestTimes(dungeonId, topCount)
        } else {
            LeaderboardManager.getTopScores(dungeonId, topCount)
        }

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
        player.sendMessage(
            Component.text(
                if (showByTime) "🏆 TEMPOS MAIS RÁPIDOS" else "🏆 CLASSIFICAÇÃO - $dungeonId",
                NamedTextColor.GOLD
            )
        )
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))

        if (entries.isEmpty()) {
            player.sendMessage(Component.text("Nenhum registro ainda.", NamedTextColor.GRAY))
        } else {
            for ((index, entry) in entries.withIndex()) {
                val medal = when (index) {
                    0 -> "🥇"
                    1 -> "🥈"
                    2 -> "🥉"
                    else -> "#${index + 1}"
                }
                val value = if (showByTime) {
                    "${entry.timeSeconds / 60}m ${entry.timeSeconds % 60}s"
                } else {
                    "${entry.score} pts"
                }
                player.sendMessage(
                    Component.text("$medal ${entry.playerName} - ", NamedTextColor.YELLOW)
                        .append(Component.text(value, NamedTextColor.WHITE))
                )
            }
        }

        // Mostrar posição do próprio jogador
        val rank = LeaderboardManager.getPlayerRank(player.uniqueId, dungeonId)
        if (rank > 0) {
            player.sendMessage(Component.text("─────────────────────", NamedTextColor.DARK_GRAY))
            player.sendMessage(
                Component.text("Sua posição: #$rank", NamedTextColor.AQUA)
            )
        }
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
    }
}

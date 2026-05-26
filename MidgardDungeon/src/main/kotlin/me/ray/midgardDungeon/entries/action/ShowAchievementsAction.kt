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
import me.ray.midgardDungeon.engine.AchievementManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("show_achievements_action", "Mostra as conquistas do jogador", Colors.DARK_ORANGE, "mdi:trophy-variant")
/**
 * A ação `Show Achievements` exibe todas as conquistas do jogador.
 *
 * ## Como isso pode ser usado?
 * Vincule a um NPC ou menu para mostrar progresso de conquistas.
 */
class ShowAchievementsAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val unlocked = AchievementManager.getPlayerAchievements(player.uniqueId)
        val total = AchievementManager.getTotalAchievements()

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
        player.sendMessage(
            Component.text("⭐ CONQUISTAS (${unlocked.size}/$total)", NamedTextColor.GOLD)
        )
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))

        for (achievementId in unlocked) {
            val achievement = AchievementManager.getAchievement(achievementId) ?: continue
            player.sendMessage(
                Component.text("${achievement.icon} ${achievement.name}", NamedTextColor.GREEN)
                    .append(Component.text(" - ${achievement.description}", NamedTextColor.GRAY))
            )
        }

        if (unlocked.isEmpty()) {
            player.sendMessage(Component.text("Nenhuma conquista desbloqueada ainda.", NamedTextColor.GRAY))
        }

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
    }
}

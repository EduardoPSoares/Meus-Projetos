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

@Entry("grant_achievement_action", "Concede uma conquista ao jogador", Colors.DARK_ORANGE, "mdi:trophy-award")
/**
 * A ação `Grant Achievement` desbloqueia uma conquista para o jogador.
 *
 * ## Como isso pode ser usado?
 * Vincule a eventos de dungeon para desbloquear conquistas automaticamente.
 */
class GrantAchievementAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("ID da conquista a conceder.")
    val achievementId: String = "",
) : ActionEntry {
    override fun ActionTrigger.execute() {
        AchievementManager.grant(player.uniqueId, achievementId)
    }
}

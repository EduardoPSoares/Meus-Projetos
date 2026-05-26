package me.ray.midgardDungeon.entries.event

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Query
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.EventEntry
import com.typewritermc.engine.paper.entry.triggerAllFor
import com.typewritermc.core.interaction.context
import org.bukkit.entity.Player

@Entry("achievement_unlocked_event", "Acionado quando uma conquista é desbloqueada", Colors.DARK_ORANGE, "mdi:trophy-award")
/**
 * O `Achievement Unlocked Event` é disparado quando o jogador desbloqueia uma conquista.
 *
 * ## Como isso pode ser usado?
 * Toque efeitos sonoros, exiba animações ou dê recompensas extras.
 */
class AchievementUnlockedEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Filtro por ID da conquista. Vazio = todas.")
    val achievementIdFilter: String = "",
) : EventEntry

fun fireAchievementUnlockedEvent(player: Player, achievementId: String) {
    val entries = Query.find<AchievementUnlockedEventEntry>().filter { entry ->
        entry.achievementIdFilter.isEmpty() || entry.achievementIdFilter == achievementId
    }.toList()
    entries.triggerAllFor(player, context())
}

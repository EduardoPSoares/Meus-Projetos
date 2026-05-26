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
import me.ray.midgardDungeon.engine.DungeonState
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("pause_dungeon_action", "Pausa ou retoma o timer da dungeon", Colors.YELLOW, "mdi:pause-circle")
class PauseDungeonAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Se deve pausar (true) ou retomar (false) a dungeon.")
    val pause: Boolean = true,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return

        if (pause && instance.state == DungeonState.IN_PROGRESS) {
            instance.transition(DungeonState.WAITING)
            instance.getOnlinePlayers().forEach { p ->
                p.sendMessage(Component.text("Dungeon pausada.", NamedTextColor.YELLOW))
            }
        } else if (!pause && instance.state == DungeonState.WAITING) {
            instance.transition(DungeonState.IN_PROGRESS)
            instance.getOnlinePlayers().forEach { p ->
                p.sendMessage(Component.text("Dungeon retomada!", NamedTextColor.GREEN))
            }
        }
    }
}

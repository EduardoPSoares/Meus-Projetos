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
import me.ray.midgardDungeon.engine.QueueManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("leave_queue_action", "Sai da fila de matchmaking", Colors.CYAN, "mdi:account-remove")
/**
 * A ação `Leave Queue` remove o jogador da fila de matchmaking.
 *
 * ## Como isso pode ser usado?
 * Use para sair da fila se o jogador mudar de ideia.
 */
class LeaveQueueAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        if (!QueueManager.leaveQueue(player)) {
            player.sendMessage(Component.text("Você não está em nenhuma fila!", NamedTextColor.RED))
        }
    }
}

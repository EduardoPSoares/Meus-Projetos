package me.ray.midgardDungeon.entries.action

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import me.ray.midgardDungeon.engine.QueueManager
import me.ray.midgardDungeon.entries.statics.DungeonConfigEntry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("join_queue_action", "Entra na fila de matchmaking para uma dungeon", Colors.CYAN, "mdi:account-group")
/**
 * A ação `Join Queue` coloca o jogador na fila de matchmaking.
 *
 * ## Como isso pode ser usado?
 * Use para jogadores individuais que querem encontrar grupo automaticamente.
 */
class JoinQueueAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A dungeon para entrar na fila.")
    val dungeonConfig: Ref<DungeonConfigEntry> = emptyRef(),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val config = dungeonConfig.get() ?: run {
            player.sendMessage(Component.text("Dungeon não encontrada!", NamedTextColor.RED))
            return
        }

        QueueManager.registerDungeon(config.id, config.minPlayers)
        QueueManager.joinQueue(player, config.id)
    }
}

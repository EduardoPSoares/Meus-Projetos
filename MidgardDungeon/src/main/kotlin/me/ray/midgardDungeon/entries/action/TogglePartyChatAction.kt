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
import me.ray.midgardDungeon.engine.PartyChatManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("toggle_party_chat_action", "Alterna o chat de grupo ligado/desligado", Colors.BLUE, "mdi:chat-processing")
/**
 * A ação `Toggle Party Chat` ativa ou desativa o chat exclusivo do grupo.
 *
 * ## Como isso pode ser usado?
 * Permita que jogadores troquem entre chat global e chat de grupo na dungeon.
 */
class TogglePartyChatAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        if (!DungeonManager.isPlayerInDungeon(player.uniqueId)) {
            player.sendMessage(Component.text("Você não está em uma dungeon!", NamedTextColor.RED))
            return
        }
        PartyChatManager.togglePartyChat(player)
    }
}

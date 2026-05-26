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
import me.ray.midgardDungeon.party.PartyManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("create_party_action", "Cria um novo grupo com o jogador como l\u00edder", Colors.GREEN, "mdi:account-group")
class CreatePartyAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Tamanho m\u00e1ximo do grupo.")
    val maxSize: Int = 4,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        if (PartyManager.getPartyByPlayer(player.uniqueId) != null) {
            player.sendMessage(Component.text("Voc\u00ea j\u00e1 est\u00e1 em um grupo!", NamedTextColor.RED))
            return
        }
        val party = PartyManager.createParty(player.uniqueId, maxSize)
        player.sendMessage(
            Component.text("Grupo criado! ID: ${party.id.toString().take(8)}", NamedTextColor.GREEN)
        )
    }
}

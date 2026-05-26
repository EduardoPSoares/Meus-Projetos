package me.ray.midgardDungeon.entries.action

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import me.ray.midgardDungeon.party.PartyManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("disband_party_action", "Desfaz o grupo do jogador", Colors.RED, "mdi:account-group-outline")
class DisbandPartyAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val party = PartyManager.getPartyByPlayer(player.uniqueId)
        if (party == null) {
            player.sendMessage(Component.text("Voc\u00ea n\u00e3o est\u00e1 em um grupo!", NamedTextColor.RED))
            return
        }
        if (!party.isLeader(player.uniqueId)) {
            player.sendMessage(Component.text("Apenas o l\u00edder pode desfazer o grupo!", NamedTextColor.RED))
            return
        }

        party.getOnlineMembers().forEach { p ->
            p.sendMessage(Component.text("O grupo foi desfeito.", NamedTextColor.YELLOW))
        }
        PartyManager.disbandParty(party.id)
    }
}

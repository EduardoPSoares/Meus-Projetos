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

@Entry("leave_party_action", "Faz o jogador sair do grupo atual", Colors.RED, "mdi:account-remove")
class LeavePartyAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        if (!PartyManager.leaveParty(player.uniqueId)) {
            player.sendMessage(Component.text("Você não está em um grupo!", NamedTextColor.RED))
            return
        }
        player.sendMessage(Component.text("Você saiu do grupo.", NamedTextColor.YELLOW))
    }
}

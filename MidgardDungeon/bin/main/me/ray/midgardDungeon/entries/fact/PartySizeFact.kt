package me.ray.midgardDungeon.entries.fact

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData
import me.ray.midgardDungeon.party.PartyManager
import org.bukkit.entity.Player

@Entry("party_size_fact", "Retorna o tamanho atual do grupo do jogador", Colors.GREEN, "mdi:account-group")
class PartySizeFact(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: com.typewritermc.core.entries.Ref<GroupEntry> = emptyRef(),
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData {
        val party = PartyManager.getPartyByPlayer(player.uniqueId)
            ?: return FactData(0)
        return FactData(party.size)
    }
}

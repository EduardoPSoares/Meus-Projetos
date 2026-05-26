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
import org.bukkit.Bukkit

@Entry("invite_party_action", "Convida o jogador mais próximo ou um alvo para o grupo", Colors.BLUE, "mdi:account-plus")
class InvitePartyAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Nome do jogador alvo. Se vazio, convida o jogador mais próximo em 10 blocos.")
    val targetPlayerName: String = "",
    @Help("Raio do convite se nenhum alvo for especificado.")
    val inviteRadius: Double = 10.0,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val party = PartyManager.getPartyByPlayer(player.uniqueId)
        if (party == null) {
            player.sendMessage(Component.text("Você precisa criar um grupo primeiro!", NamedTextColor.RED))
            return
        }
        if (!party.isLeader(player.uniqueId)) {
            player.sendMessage(Component.text("Apenas o líder do grupo pode convidar jogadores!", NamedTextColor.RED))
            return
        }
        if (party.isFull) {
            player.sendMessage(Component.text("O grupo está cheio!", NamedTextColor.RED))
            return
        }

        val target = if (targetPlayerName.isNotEmpty()) {
            Bukkit.getPlayer(targetPlayerName)
        } else {
            player.world.getNearbyEntities(player.location, inviteRadius, inviteRadius, inviteRadius)
                .filterIsInstance<org.bukkit.entity.Player>()
                .filter { it.uniqueId != player.uniqueId }
                .filter { PartyManager.getPartyByPlayer(it.uniqueId) == null }
                .minByOrNull { it.location.distanceSquared(player.location) }
        }

        if (target == null) {
            player.sendMessage(Component.text("Nenhum jogador encontrado para convidar!", NamedTextColor.RED))
            return
        }

        if (PartyManager.getPartyByPlayer(target.uniqueId) != null) {
            player.sendMessage(Component.text("${target.name} já está em um grupo!", NamedTextColor.RED))
            return
        }

        // Entrada direta (simplificado — poderia ser estendido com fluxo de convite/aceitar)
        if (PartyManager.joinParty(target.uniqueId, party.id)) {
            player.sendMessage(Component.text("${target.name} entrou no grupo!", NamedTextColor.GREEN))
            target.sendMessage(Component.text("Você entrou no grupo de ${player.name}!", NamedTextColor.GREEN))
            party.getOnlineMembers().filter { it.uniqueId != player.uniqueId && it.uniqueId != target.uniqueId }
                .forEach { it.sendMessage(Component.text("${target.name} entrou no grupo!", NamedTextColor.GREEN)) }
        } else {
            player.sendMessage(Component.text("Não foi possível adicionar ${target.name} ao grupo!", NamedTextColor.RED))
        }
    }
}

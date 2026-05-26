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
import me.ray.midgardDungeon.engine.MMOCoreManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("mmocore_exp_action", "Dá experiência MMOCore aos jogadores da dungeon", Colors.GREEN, "mdi:star-circle")
/**
 * A ação `MMOCore EXP` concede experiência pelo sistema do MMOCore.
 *
 * ## Como isso pode ser usado?
 * Recompense os jogadores com EXP do MMOCore ao completar dungeons,
 * waves, derrotar bosses ou encontrar segredos.
 * Requer MMOCore instalado — caso contrário a ação é silenciosamente ignorada.
 */
class MMOCoreExpAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Quantidade de experiência MMOCore a conceder.")
    val experience: Double = 100.0,
    @Help("Se deve conceder para todo o grupo.")
    val giveToParty: Boolean = true,
    @Help("Motivo exibido na mensagem (ex: 'Boss derrotado').")
    val reason: String = "",
) : ActionEntry {
    override fun ActionTrigger.execute() {
        if (!MMOCoreManager.isAvailable()) {
            player.sendMessage(Component.text("MMOCore não está disponível!", NamedTextColor.GRAY))
            return
        }

        val targets = if (giveToParty) {
            val instance = DungeonManager.getInstanceByPlayer(player.uniqueId)
            instance?.getOnlinePlayers() ?: listOf(player)
        } else {
            listOf(player)
        }

        targets.forEach { p ->
            MMOCoreManager.rewardExperience(p, experience, reason)
        }
    }
}

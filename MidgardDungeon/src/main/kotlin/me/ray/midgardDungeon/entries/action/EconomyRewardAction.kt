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
import me.ray.midgardDungeon.engine.VaultManager

@Entry("economy_reward_action", "Concede moedas ao jogador via Vault", Colors.YELLOW, "mdi:cash-multiple")
/**
 * A ação `Economy Reward` concede moedas ao jogador usando Vault.
 *
 * ## Como isso pode ser usado?
 * Use como recompensa ao completar dungeons, derrotar bosses,
 * completar puzzles ou encontrar segredos.
 */
class EconomyRewardAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Quantidade de moedas a conceder.")
    val amount: Double = 100.0,
    @Help("Motivo da recompensa (exibido no chat).")
    val reason: String = "Recompensa de dungeon",
) : ActionEntry {
    override fun ActionTrigger.execute() {
        if (!VaultManager.isAvailable()) {
            return
        }
        VaultManager.reward(player, amount, reason)
    }
}

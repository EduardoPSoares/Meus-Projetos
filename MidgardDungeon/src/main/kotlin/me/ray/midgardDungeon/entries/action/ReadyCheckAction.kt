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
import me.ray.midgardDungeon.engine.DungeonManager
import me.ray.midgardDungeon.engine.ReadyCheckManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("ready_check_action", "Inicia verificação de prontidão do grupo", Colors.GREEN, "mdi:check-circle")
/**
 * A ação `Ready Check` verifica se todos os jogadores do grupo estão prontos.
 *
 * ## Como isso pode ser usado?
 * Use antes de iniciar a dungeon para garantir que todos estão preparados.
 */
class ReadyCheckAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Tempo limite em segundos para a verificação.")
    val timeoutSeconds: Int = 30,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId)
        if (instance == null) {
            player.sendMessage(Component.text("Você não está em uma dungeon!", NamedTextColor.RED))
            return
        }

        ReadyCheckManager.startReadyCheck(
            instanceId = instance.id,
            initiator = player,
            players = instance.party.memberIds,
            timeoutSeconds = timeoutSeconds,
            onComplete = {
                instance.getOnlinePlayers().forEach { p ->
                    p.sendMessage(Component.text("Todos prontos! Avançando...", NamedTextColor.GREEN))
                }
            },
            onFail = {
                instance.getOnlinePlayers().forEach { p ->
                    p.sendMessage(Component.text("Verificação de prontidão falhou!", NamedTextColor.RED))
                }
            }
        )
    }
}

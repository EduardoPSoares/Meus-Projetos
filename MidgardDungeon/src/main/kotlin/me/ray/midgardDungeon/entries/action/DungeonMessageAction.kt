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
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import java.time.Duration

enum class MessageType {
    CHAT,
    ACTION_BAR,
    TITLE
}

@Entry("dungeon_message_action", "Envia uma mensagem para o grupo da dungeon", Colors.BLUE, "mdi:message-text")
class DungeonMessageAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A mensagem a enviar (formato MiniMessage).")
    val message: String = "",
    @Help("Subtítulo para mensagens de título (formato MiniMessage).")
    val subtitle: String = "",
    @Help("Como exibir a mensagem.")
    val messageType: MessageType = MessageType.CHAT,
    @Help("Se deve enviar para todo o grupo ou apenas para o jogador que acionou.")
    val sendToParty: Boolean = true,
    @Help("Duração do fade in em ticks (apenas título).")
    val fadeIn: Int = 10,
    @Help("Duração de exibição em ticks (apenas título).")
    val stay: Int = 70,
    @Help("Duração do fade out em ticks (apenas título).")
    val fadeOut: Int = 20,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        if (message.isEmpty()) return
        val miniMessage = MiniMessage.miniMessage()
        val component = miniMessage.deserialize(message)

        val targets = if (sendToParty) {
            val instance = DungeonManager.getInstanceByPlayer(player.uniqueId)
            instance?.getOnlinePlayers() ?: listOf(player)
        } else {
            listOf(player)
        }

        targets.forEach { p ->
            when (messageType) {
                MessageType.CHAT -> p.sendMessage(component)
                MessageType.ACTION_BAR -> p.sendActionBar(component)
                MessageType.TITLE -> {
                    val subtitleComponent = if (subtitle.isNotEmpty()) miniMessage.deserialize(subtitle) else net.kyori.adventure.text.Component.empty()
                    p.showTitle(
                        Title.title(
                            component,
                            subtitleComponent,
                            Title.Times.times(
                                Duration.ofMillis(fadeIn * 50L),
                                Duration.ofMillis(stay * 50L),
                                Duration.ofMillis(fadeOut * 50L)
                            )
                        )
                    )
                }
            }
        }
    }
}

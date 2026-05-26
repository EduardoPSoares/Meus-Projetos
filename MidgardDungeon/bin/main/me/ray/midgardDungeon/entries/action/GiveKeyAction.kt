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
import me.ray.midgardDungeon.engine.KeyManager
import me.ray.midgardDungeon.entries.statics.KeyConfigEntry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("give_key_action", "Dá uma chave ao jogador ou grupo", Colors.YELLOW, "mdi:key-plus")
/**
 * A ação `Give Key` concede uma chave ao jogador ou a todo o grupo.
 *
 * ## Como isso pode ser usado?
 * Dê chaves como recompensa de waves, baús ou puzzles para desbloquear áreas.
 */
class GiveKeyAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A configuração da chave.")
    val keyConfig: Ref<KeyConfigEntry> = emptyRef(),
    @Help("Se deve dar a chave para todo o grupo.")
    val giveToParty: Boolean = true,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val config = keyConfig.get() ?: return
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return

        if (giveToParty) {
            KeyManager.giveKeyToParty(instance.id, config.id)
            instance.getOnlinePlayers().forEach { p ->
                p.sendMessage(
                    Component.text("${config.icon} Chave obtida: ${config.displayName}", NamedTextColor.GOLD)
                )
            }
        } else {
            KeyManager.giveKey(instance.id, player.uniqueId, config.id)
        }
    }
}

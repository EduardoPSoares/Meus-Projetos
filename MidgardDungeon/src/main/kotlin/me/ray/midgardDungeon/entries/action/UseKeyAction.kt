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

@Entry("use_key_action", "Usa uma chave para desbloquear uma área", Colors.YELLOW, "mdi:key-remove")
/**
 * A ação `Use Key` consome uma chave para desbloquear uma sala ou passagem.
 *
 * ## Como isso pode ser usado?
 * Vincule a uma interação com porta ou baú trancado para validar se o grupo tem a chave.
 */
class UseKeyAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A configuração da chave necessária.")
    val keyConfig: Ref<KeyConfigEntry> = emptyRef(),
    @Help("Se deve buscar a chave em todo o grupo.")
    val useFromParty: Boolean = true,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val config = keyConfig.get() ?: return
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return

        val hasKey = if (useFromParty) {
            KeyManager.partyHasKey(instance.id, config.id)
        } else {
            KeyManager.hasKey(instance.id, player.uniqueId, config.id)
        }

        if (!hasKey) {
            player.sendMessage(
                Component.text("Você precisa da chave: ${config.displayName}", NamedTextColor.RED)
            )
            return
        }

        if (config.consumeOnUse) {
            if (useFromParty) {
                KeyManager.usePartyKey(instance.id, config.id)
            } else {
                KeyManager.useKey(instance.id, player.uniqueId, config.id)
            }
        }

        instance.getOnlinePlayers().forEach { p ->
            p.sendMessage(
                Component.text("${config.icon} Área desbloqueada: ${config.unlocksAreaId}", NamedTextColor.GREEN)
            )
        }
    }
}

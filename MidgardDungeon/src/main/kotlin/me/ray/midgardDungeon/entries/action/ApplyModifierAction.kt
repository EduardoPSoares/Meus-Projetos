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
import me.ray.midgardDungeon.engine.ModifierManager
import me.ray.midgardDungeon.entries.statics.ModifierConfigEntry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("apply_modifier_action", "Aplica um modificador à dungeon atual", Colors.ORANGE, "mdi:tune-variant")
/**
 * A ação `Apply Modifier` aplica um modificador à instância de dungeon ativa.
 *
 * ## Como isso pode ser usado?
 * Use para ativar modificadores que alteram dificuldade e recompensas durante a run.
 */
class ApplyModifierAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A configuração do modificador a aplicar.")
    val modifierConfig: Ref<ModifierConfigEntry> = emptyRef(),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val config = modifierConfig.get() ?: return
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return

        val modifier = ModifierManager.DungeonModifier(
            id = config.id,
            name = config.displayName,
            description = config.description,
            healthMultiplier = config.healthMultiplier,
            damageMultiplier = config.damageMultiplier,
            speedMultiplier = config.speedMultiplier,
            lootMultiplier = config.lootMultiplier,
            expMultiplier = config.expMultiplier,
            extraMobs = config.extraMobs,
        )

        ModifierManager.applyModifier(instance.id, modifier)

        instance.getOnlinePlayers().forEach { p ->
            p.sendMessage(
                Component.text("${config.icon} Modificador ativado: ${config.displayName}", NamedTextColor.GOLD)
            )
            p.sendMessage(
                Component.text(config.description, NamedTextColor.GRAY)
            )
        }
    }
}

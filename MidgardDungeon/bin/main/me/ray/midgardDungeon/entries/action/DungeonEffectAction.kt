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
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

@Entry("dungeon_effect_action", "Aplica efeitos de po\u00e7\u00e3o aos jogadores da dungeon", Colors.PURPLE, "mdi:flask")
class DungeonEffectAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Nome do tipo de efeito de po\u00e7\u00e3o (ex: SPEED, REGENERATION, STRENGTH).")
    val effectType: String = "SPEED",
    @Help("Dura\u00e7\u00e3o em ticks (20 ticks = 1 segundo).")
    val duration: Int = 200,
    @Help("N\u00edvel do amplificador (0 = n\u00edvel 1, 1 = n\u00edvel 2, etc).")
    val amplifier: Int = 0,
    @Help("Se deve aplicar a todo o grupo.")
    val applyToParty: Boolean = true,
    @Help("Se deve mostrar efeitos de part\u00edculas.")
    val showParticles: Boolean = true,
    @Help("Se deve remover o efeito ao inv\u00e9s de aplic\u00e1-lo.")
    val removeEffect: Boolean = false,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val type = PotionEffectType.getByName(effectType.uppercase()) ?: return

        val targets = if (applyToParty) {
            val instance = DungeonManager.getInstanceByPlayer(player.uniqueId)
            instance?.getOnlinePlayers() ?: listOf(player)
        } else {
            listOf(player)
        }

        targets.forEach { p ->
            if (removeEffect) {
                p.removePotionEffect(type)
            } else {
                p.addPotionEffect(PotionEffect(type, duration, amplifier, false, showParticles))
            }
        }
    }
}

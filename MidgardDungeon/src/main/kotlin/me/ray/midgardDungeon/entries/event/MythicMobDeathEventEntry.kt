package me.ray.midgardDungeon.entries.event

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Query
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.EventEntry
import com.typewritermc.engine.paper.entry.triggerAllFor
import com.typewritermc.core.interaction.context
import me.ray.midgardDungeon.engine.DungeonInstance
import me.ray.midgardDungeon.entries.statics.DungeonConfigEntry
import org.bukkit.entity.Player

@Entry("mythic_mob_death_event", "Acionado quando um MythicMob morre dentro da dungeon", Colors.RED, "mdi:skull-crossbones")
/**
 * O `Mythic Mob Death Event` é disparado quando um mob do MythicMobs morre na dungeon.
 *
 * ## Como isso pode ser usado?
 * Acione recompensas, diálogos ou mecânicas especiais quando um MythicMob
 * específico é derrotado. Pode filtrar por ID do MythicMob.
 */
class MythicMobDeathEventEntry(
    override val id: String = "",
    override val name: String = "",
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("ID do MythicMob para filtrar (vazio = qualquer MythicMob).")
    val mythicMobId: String = "",
    @Help("Se definido, só aciona para uma dungeon específica.")
    val dungeonFilter: Ref<DungeonConfigEntry> = emptyRef(),
) : EventEntry

fun fireMythicMobDeathEvent(player: Player, instance: DungeonInstance, mobId: String) {
    val entries = Query.find<MythicMobDeathEventEntry>().filter { entry ->
        (entry.dungeonFilter.id.isEmpty() || entry.dungeonFilter.id == instance.dungeonId)
            && (entry.mythicMobId.isEmpty() || entry.mythicMobId.equals(mobId, ignoreCase = true))
    }.toList()
    entries.triggerAllFor(player, context())
}

package me.ray.midgardDungeon.entries.action

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Query
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import me.ray.midgardDungeon.engine.DungeonManager
import me.ray.midgardDungeon.engine.PersistentCooldownManager
import me.ray.midgardDungeon.engine.DailyDungeonManager
import me.ray.midgardDungeon.entries.statics.DungeonConfigEntry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

@Entry("open_dungeon_menu_action", "Abre o menu GUI de seleção de dungeons", Colors.RED, "mdi:view-grid")
/**
 * A ação `Open Dungeon Menu` abre uma interface gráfica com as dungeons disponíveis.
 *
 * ## Como isso pode ser usado?
 * Vincule a um NPC ou comando para abrir o menu de seleção de dungeons.
 */
class OpenDungeonMenuAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Material do item padrão para dungeons no menu.")
    val defaultIcon: Material = Material.SPAWNER,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val configs = Query.find<DungeonConfigEntry>().toList()

        val size = ((configs.size / 9) + 1).coerceIn(1, 6) * 9
        val inventory = Bukkit.createInventory(null, size,
            Component.text("⚔ Menu de Dungeons", NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD)
        )

        for ((index, config) in configs.withIndex()) {
            if (index >= size) break
            val item = ItemStack(defaultIcon)
            val meta = item.itemMeta ?: continue

            val isOnCooldown = PersistentCooldownManager.isOnCooldown(player.uniqueId, config.id)
            val isInDungeon = DungeonManager.isPlayerInDungeon(player.uniqueId)
            val activeCount = DungeonManager.getInstancesByDungeon(config.id).size
            val isDaily = DailyDungeonManager.isDailyDungeon(config.id)
            val isWeekly = DailyDungeonManager.isWeeklyDungeon(config.id)

            val displayName = Component.text(config.name.ifEmpty { config.id }, NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
            meta.displayName(displayName)

            val lore = mutableListOf<Component>()
            lore.add(Component.text(""))
            lore.add(Component.text("Jogadores: ${config.minPlayers}-${config.maxPlayers}", NamedTextColor.GRAY))
            if (config.timeLimitSeconds > 0) {
                val min = config.timeLimitSeconds / 60
                lore.add(Component.text("Tempo limite: ${min}min", NamedTextColor.GRAY))
            }
            if (config.minLevel > 1) {
                lore.add(Component.text("Nível mínimo: ${config.minLevel}", NamedTextColor.GRAY))
            }
            lore.add(Component.text("Instâncias ativas: $activeCount", NamedTextColor.AQUA))

            if (isDaily) {
                lore.add(Component.text("📅 DUNGEON DIÁRIA (2x bônus!)", NamedTextColor.GOLD))
            }
            if (isWeekly) {
                lore.add(Component.text("🏆 DUNGEON SEMANAL (3x bônus!)", NamedTextColor.LIGHT_PURPLE))
            }

            lore.add(Component.text(""))
            when {
                isInDungeon -> {
                    lore.add(Component.text("✗ Você já está em uma dungeon!", NamedTextColor.RED))
                    item.type = Material.BARRIER
                }
                isOnCooldown -> {
                    val remaining = PersistentCooldownManager.getRemainingSeconds(player.uniqueId, config.id)
                    val rMin = remaining / 60
                    val rSec = remaining % 60
                    lore.add(Component.text("✗ Em cooldown: ${rMin}m ${rSec}s", NamedTextColor.RED))
                    item.type = Material.CLOCK
                }
                else -> {
                    lore.add(Component.text("✓ Clique para entrar!", NamedTextColor.GREEN))
                }
            }

            meta.lore(lore)
            item.itemMeta = meta
            inventory.setItem(index, item)
        }

        player.openInventory(inventory)
    }
}

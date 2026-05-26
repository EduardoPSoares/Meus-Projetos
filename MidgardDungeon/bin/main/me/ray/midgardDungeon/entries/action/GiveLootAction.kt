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
import me.ray.midgardDungeon.entries.statics.LootTableEntry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

@Entry("give_loot_action", "Dá loot de uma tabela aos jogadores da dungeon", Colors.YELLOW, "mdi:treasure-chest")
/**
 * A ação `Give Loot` sorteia uma tabela de loot e distribui os itens.
 *
 * ## Como isso pode ser usado?
 * Recompense jogadores ao final de uma dungeon, após matar um boss ou em salas de tesouro.
 */
class GiveLootAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A tabela de loot para sortear.")
    val lootTable: Ref<LootTableEntry> = emptyRef(),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val table = lootTable.get() ?: return
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId)

        val recipients = if (table.perPlayer && instance != null) {
            instance.getOnlinePlayers()
        } else {
            listOf(player)
        }

        val miniMessage = MiniMessage.miniMessage()
        val totalWeight = table.items.sumOf { it.weight }

        recipients.forEach { recipient ->
            repeat(table.rollCount) {
                var roll = Random.nextDouble() * totalWeight
                for (item in table.items) {
                    roll -= item.weight
                    if (roll <= 0) {
                        val material = Material.matchMaterial(item.material) ?: Material.STONE
                        val amount = Random.nextInt(item.minAmount, item.maxAmount + 1)
                        val stack = ItemStack(material, amount)

                        stack.editMeta { meta ->
                            if (item.displayName.isNotEmpty()) {
                                meta.displayName(miniMessage.deserialize(item.displayName))
                            }
                            if (item.lore.isNotEmpty()) {
                                meta.lore(item.lore.map { miniMessage.deserialize(it) })
                            }
                            if (item.customModelData > 0) {
                                meta.setCustomModelData(item.customModelData)
                            }
                        }

                        val leftover = recipient.inventory.addItem(stack)
                        leftover.values.forEach { overflow ->
                            recipient.world.dropItemNaturally(recipient.location, overflow)
                        }
                        break
                    }
                }
            }
            recipient.sendMessage(Component.text("Você recebeu loot!", NamedTextColor.GOLD))
        }
    }
}

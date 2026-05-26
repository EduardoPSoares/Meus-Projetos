package me.ray.midgardDungeon.engine

import com.typewritermc.core.entries.Query
import me.ray.midgardDungeon.entries.statics.DungeonConfigEntry
import me.ray.midgardDungeon.entries.statics.RoomConfigEntry
import me.ray.midgardDungeon.entries.statics.RoomType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

/**
 * Gera e exibe um mapa ASCII da dungeon no chat do jogador.
 * Mostra as salas, conexões e a posição atual do grupo.
 */
object DungeonMapManager {

    private fun getRoomIcon(type: RoomType): String = when (type) {
        RoomType.SPAWN -> "🏠"
        RoomType.NORMAL -> "⚔"
        RoomType.BOSS -> "💀"
        RoomType.TREASURE -> "💎"
        RoomType.PUZZLE -> "🧩"
        RoomType.CORRIDOR -> "🚪"
    }

    private fun getRoomColor(type: RoomType): NamedTextColor = when (type) {
        RoomType.SPAWN -> NamedTextColor.GREEN
        RoomType.NORMAL -> NamedTextColor.YELLOW
        RoomType.BOSS -> NamedTextColor.RED
        RoomType.TREASURE -> NamedTextColor.GOLD
        RoomType.PUZZLE -> NamedTextColor.AQUA
        RoomType.CORRIDOR -> NamedTextColor.GRAY
    }

    fun showMap(player: Player) {
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId)
        if (instance == null) {
            player.sendMessage(Component.text("Você não está em uma dungeon!", NamedTextColor.RED))
            return
        }

        val config = Query.find<DungeonConfigEntry>().firstOrNull { it.id == instance.dungeonId }
        if (config == null) {
            player.sendMessage(Component.text("Configuração da dungeon não encontrada!", NamedTextColor.RED))
            return
        }

        val rooms = config.rooms.mapNotNull { it.get() }

        if (rooms.isEmpty()) {
            player.sendMessage(Component.text("Nenhuma sala configurada nesta dungeon.", NamedTextColor.GRAY))
            return
        }

        val currentRoomIndex = instance.currentRoom

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_PURPLE))
        player.sendMessage(
            Component.text("🗺 MAPA DA DUNGEON", NamedTextColor.DARK_PURPLE)
                .decorate(TextDecoration.BOLD)
        )
        player.sendMessage(
            Component.text("  ${config.name}", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(" — ${instance.state.name}", NamedTextColor.GRAY))
        )
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_PURPLE))

        // Renderizar mapa linear
        for ((index, room) in rooms.withIndex()) {
            val isCurrent = index == currentRoomIndex
            val isCompleted = index < currentRoomIndex
            val icon = getRoomIcon(room.getRoomType())
            val color = getRoomColor(room.getRoomType())

            val statusPrefix = when {
                isCurrent -> "▶ "
                isCompleted -> "✔ "
                else -> "  "
            }

            val statusColor = when {
                isCurrent -> NamedTextColor.WHITE
                isCompleted -> NamedTextColor.DARK_GRAY
                else -> color
            }

            val roomLine = Component.text(statusPrefix, if (isCurrent) NamedTextColor.GREEN else NamedTextColor.DARK_GREEN)
                .append(Component.text("$icon ", color))
                .append(Component.text("[${index + 1}] ", NamedTextColor.DARK_GRAY))
                .append(
                    if (isCurrent) {
                        Component.text(room.name, statusColor).decorate(TextDecoration.BOLD)
                    } else {
                        Component.text(room.name, statusColor)
                    }
                )
                .append(Component.text(" (${room.getRoomType().name})", NamedTextColor.DARK_GRAY))

            player.sendMessage(roomLine)

            // Desenhar conector entre salas
            if (index < rooms.size - 1) {
                val connectorColor = if (isCompleted) NamedTextColor.DARK_GRAY else NamedTextColor.GRAY
                player.sendMessage(Component.text("    │", connectorColor))
            }
        }

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_PURPLE))

        // Legenda
        player.sendMessage(Component.text("Legenda:", NamedTextColor.GRAY))
        player.sendMessage(
            Component.text("  ▶ Sala atual  ", NamedTextColor.GREEN)
                .append(Component.text("✔ Completa  ", NamedTextColor.DARK_GRAY))
        )
        player.sendMessage(
            Component.text("  🏠 Spawn  ⚔ Combate  💀 Boss  💎 Tesouro  🧩 Puzzle", NamedTextColor.GRAY)
        )
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_PURPLE))
    }
}

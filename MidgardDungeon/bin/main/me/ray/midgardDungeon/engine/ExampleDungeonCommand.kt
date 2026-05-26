package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import me.ray.midgardDungeon.entries.statics.RoomType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Comando /midgard que oferece ferramentas para builders.
 *
 * Subcomandos:
 * - /midgard exemplo [tema] — Constrói uma dungeon de exemplo no local do jogador
 * - /midgard schematics — Lista schematics disponíveis
 * - /midgard info — Mostra informações do sistema
 */
object ExampleDungeonCommand : CommandExecutor, TabCompleter {

    // Temas disponíveis
    enum class DungeonTheme(
        val displayName: String,
        val wall: Material,
        val floor: Material,
        val ceiling: Material,
        val accent: Material,
        val light: Material,
        val pillar: Material,
        val decoration: Material,
    ) {
        MEDIEVAL(
            "Medieval",
            Material.STONE_BRICKS, Material.POLISHED_ANDESITE, Material.STONE_BRICKS,
            Material.MOSSY_STONE_BRICKS, Material.LANTERN, Material.OAK_LOG, Material.COBWEB,
        ),
        NETHER(
            "Nether",
            Material.NETHER_BRICKS, Material.BLACKSTONE, Material.NETHER_BRICKS,
            Material.RED_NETHER_BRICKS, Material.SOUL_LANTERN, Material.OBSIDIAN, Material.SOUL_SAND,
        ),
        ICE(
            "Gelo",
            Material.PACKED_ICE, Material.SNOW_BLOCK, Material.BLUE_ICE,
            Material.PRISMARINE, Material.SEA_LANTERN, Material.QUARTZ_PILLAR, Material.POWDER_SNOW,
        ),
        JUNGLE(
            "Selva",
            Material.MOSSY_COBBLESTONE, Material.JUNGLE_PLANKS, Material.DARK_OAK_PLANKS,
            Material.JUNGLE_LOG, Material.GLOWSTONE, Material.JUNGLE_LOG, Material.VINE,
        ),
        END(
            "End",
            Material.END_STONE_BRICKS, Material.PURPUR_BLOCK, Material.END_STONE_BRICKS,
            Material.PURPUR_PILLAR, Material.END_ROD, Material.PURPUR_PILLAR, Material.CHORUS_PLANT,
        ),
    }

    data class BuiltRoom(
        val name: String,
        val type: RoomType,
        val origin: Location,
        val width: Int,
        val depth: Int,
        val height: Int,
        val spawnPoint: Location,
    )

    fun register() {
        val plugin = MidgardPlugin.instance ?: return
        val command = Bukkit.getPluginCommand("midgard")
        if (command != null) {
            command.setExecutor(this)
            command.tabCompleter = this
        } else {
            // Registrar comando dinamicamente via CommandMap
            try {
                val serverClass = Bukkit.getServer().javaClass
                val commandMapField = serverClass.getDeclaredMethod("getCommandMap")
                val commandMap = commandMapField.invoke(Bukkit.getServer())
                        as org.bukkit.command.CommandMap

                val cmd = object : Command("midgard") {
                    init {
                        description = "Ferramentas de dungeon para builders"
                        usage = "/midgard <exemplo|schematics|info>"
                        permission = "midgard.admin"
                    }

                    override fun execute(sender: CommandSender, label: String, args: Array<out String>): Boolean {
                        return this@ExampleDungeonCommand.onCommand(sender, this, label, args)
                    }

                    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                        return this@ExampleDungeonCommand.onTabComplete(sender, this, alias, args)
                            ?: mutableListOf()
                    }
                }

                commandMap.register("midgard", cmd)
            } catch (e: Exception) {
                Bukkit.getLogger().warning("[MidgardDungeon] Não foi possível registrar o comando /midgard: ${e.message}")
            }
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.")
            return true
        }

        // Comando mapa não requer permissão de admin
        if (args.isNotEmpty() && args[0].lowercase() in listOf("mapa", "map")) {
            DungeonMapManager.showMap(sender)
            return true
        }

        if (!sender.hasPermission("midgard.admin")) {
            sender.sendMessage(Component.text("Sem permissão!", NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "exemplo", "example" -> {
                val theme = if (args.size > 1) {
                    try {
                        DungeonTheme.valueOf(args[1].uppercase())
                    } catch (_: Exception) {
                        sender.sendMessage(Component.text("Tema inválido! Temas: ${DungeonTheme.entries.joinToString { it.name.lowercase() }}", NamedTextColor.RED))
                        return true
                    }
                } else {
                    DungeonTheme.MEDIEVAL
                }
                buildExampleDungeon(sender, theme)
            }
            "schematics" -> listSchematics(sender)
            "info" -> showInfo(sender)
            "mapa", "map" -> DungeonMapManager.showMap(sender)
            else -> showHelp(sender)
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        return when (args.size) {
            1 -> mutableListOf("exemplo", "schematics", "info", "mapa")
                .filter { it.startsWith(args[0].lowercase()) }
                .toMutableList()
            2 -> if (args[0].lowercase() in listOf("exemplo", "example")) {
                DungeonTheme.entries.map { it.name.lowercase() }
                    .filter { it.startsWith(args[1].lowercase()) }
                    .toMutableList()
            } else mutableListOf()
            else -> mutableListOf()
        }
    }

    private fun showHelp(player: Player) {
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
        player.sendMessage(Component.text("⚔ MIDGARD DUNGEON - Comandos", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
        player.sendMessage(
            Component.text("/midgard exemplo [tema]", NamedTextColor.YELLOW)
                .append(Component.text(" — Constrói dungeon de exemplo", NamedTextColor.GRAY))
        )
        player.sendMessage(
            Component.text("  Temas: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(DungeonTheme.entries.joinToString(", ") { it.name.lowercase() }, NamedTextColor.AQUA))
        )
        player.sendMessage(
            Component.text("/midgard schematics", NamedTextColor.YELLOW)
                .append(Component.text(" — Lista schematics disponíveis", NamedTextColor.GRAY))
        )
        player.sendMessage(
            Component.text("/midgard info", NamedTextColor.YELLOW)
                .append(Component.text(" — Informações do sistema", NamedTextColor.GRAY))
        )
        player.sendMessage(
            Component.text("/midgard mapa", NamedTextColor.YELLOW)
                .append(Component.text(" — Mostra mapa da dungeon atual", NamedTextColor.GRAY))
        )
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
    }

    private fun listSchematics(player: Player) {
        val schematics = SchematicManager.listSchematics()
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_AQUA))
        player.sendMessage(Component.text("📋 Schematics Disponíveis", NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD))
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_AQUA))

        if (!SchematicManager.isAvailable()) {
            player.sendMessage(Component.text("WorldEdit não encontrado!", NamedTextColor.RED))
            return
        }

        if (schematics.isEmpty()) {
            player.sendMessage(Component.text("Nenhum schematic encontrado.", NamedTextColor.GRAY))
            player.sendMessage(Component.text("Use //copy + //schematic save <nome>", NamedTextColor.YELLOW))
        } else {
            for (name in schematics) {
                player.sendMessage(
                    Component.text("  • ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(name, NamedTextColor.GREEN))
                )
            }
            player.sendMessage(Component.text("Total: ${schematics.size} schematics", NamedTextColor.GRAY))
        }
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_AQUA))
    }

    private fun showInfo(player: Player) {
        val activeInstances = DungeonManager.getActiveInstances().size
        val activePlayers = DungeonManager.getActiveInstances().sumOf { it.getOnlinePlayers().size }

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN))
        player.sendMessage(Component.text("⚙ MidgardDungeon Info", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN))
        player.sendMessage(Component.text("Instâncias ativas: $activeInstances", NamedTextColor.YELLOW))
        player.sendMessage(Component.text("Jogadores em dungeon: $activePlayers", NamedTextColor.YELLOW))
        player.sendMessage(Component.text("WorldEdit: ${if (SchematicManager.isAvailable()) "✅" else "❌"}", NamedTextColor.YELLOW))
        player.sendMessage(Component.text("Schematics: ${SchematicManager.listSchematics().size}", NamedTextColor.YELLOW))
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN))
    }

    // ==================== CONSTRUÇÃO DA DUNGEON DE EXEMPLO ====================

    private fun buildExampleDungeon(player: Player, theme: DungeonTheme) {
        val world = player.world
        val base = player.location.clone()
        base.x = base.blockX.toDouble()
        base.y = base.blockY.toDouble()
        base.z = base.blockZ.toDouble()

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
        player.sendMessage(
            Component.text("🔨 Construindo dungeon de exemplo... ", NamedTextColor.GOLD)
                .append(Component.text("Tema: ${theme.displayName}", NamedTextColor.YELLOW))
        )

        val builtRooms = mutableListOf<BuiltRoom>()
        var currentX = base.blockX

        // 1. Sala de Spawn
        val spawnRoom = buildStyledRoom(
            world, currentX, base.blockY, base.blockZ,
            width = 11, depth = 11, height = 6,
            theme = theme, type = RoomType.SPAWN, name = "Sala de Spawn"
        )
        builtRooms.add(spawnRoom)
        decorateSpawnRoom(world, spawnRoom, theme)
        currentX += spawnRoom.width + 6

        // Corredor 1
        buildStyledCorridor(world, spawnRoom, currentX, base.blockY, base.blockZ + 5, theme)

        // 2. Sala de Combate 1
        val combatRoom1 = buildStyledRoom(
            world, currentX, base.blockY, base.blockZ - 1,
            width = 13, depth = 13, height = 6,
            theme = theme, type = RoomType.NORMAL, name = "Sala de Combate 1"
        )
        builtRooms.add(combatRoom1)
        decorateCombatRoom(world, combatRoom1, theme)
        currentX += combatRoom1.width + 6

        // Corredor 2
        buildStyledCorridor(world, combatRoom1, currentX, base.blockY, base.blockZ + 5, theme)

        // 3. Sala de Puzzle
        val puzzleRoom = buildStyledRoom(
            world, currentX, base.blockY, base.blockZ,
            width = 11, depth = 11, height = 6,
            theme = theme, type = RoomType.PUZZLE, name = "Sala de Puzzle"
        )
        builtRooms.add(puzzleRoom)
        decoratePuzzleRoom(world, puzzleRoom, theme)
        currentX += puzzleRoom.width + 6

        // Corredor 3
        buildStyledCorridor(world, puzzleRoom, currentX, base.blockY, base.blockZ + 5, theme)

        // 4. Sala de Combate 2
        val combatRoom2 = buildStyledRoom(
            world, currentX, base.blockY, base.blockZ - 2,
            width = 15, depth = 15, height = 7,
            theme = theme, type = RoomType.NORMAL, name = "Sala de Combate 2"
        )
        builtRooms.add(combatRoom2)
        decorateCombatRoom(world, combatRoom2, theme)
        currentX += combatRoom2.width + 6

        // Corredor 4
        buildStyledCorridor(world, combatRoom2, currentX, base.blockY, base.blockZ + 5, theme)

        // 5. Sala de Boss
        val bossRoom = buildStyledRoom(
            world, currentX, base.blockY, base.blockZ - 3,
            width = 19, depth = 19, height = 10,
            theme = theme, type = RoomType.BOSS, name = "Sala de Boss"
        )
        builtRooms.add(bossRoom)
        decorateBossRoom(world, bossRoom, theme)
        currentX += bossRoom.width + 6

        // Corredor 5
        buildStyledCorridor(world, bossRoom, currentX, base.blockY, base.blockZ + 5, theme)

        // 6. Sala de Tesouro
        val treasureRoom = buildStyledRoom(
            world, currentX, base.blockY, base.blockZ,
            width = 9, depth = 9, height = 5,
            theme = theme, type = RoomType.TREASURE, name = "Sala de Tesouro"
        )
        builtRooms.add(treasureRoom)
        decorateTreasureRoom(world, treasureRoom, theme)

        // Teleportar jogador para o spawn
        player.teleport(spawnRoom.spawnPoint)

        // Mostrar guia de configuração
        player.sendMessage(Component.text("✅ Dungeon de exemplo construída!", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
        player.sendMessage(Component.empty())

        player.sendMessage(Component.text("📋 SALAS CRIADAS:", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
        player.sendMessage(Component.empty())

        for ((index, room) in builtRooms.withIndex()) {
            val typeIcon = when (room.type) {
                RoomType.SPAWN -> "🏠"
                RoomType.NORMAL -> "⚔"
                RoomType.BOSS -> "💀"
                RoomType.TREASURE -> "💎"
                RoomType.PUZZLE -> "🧩"
                RoomType.CORRIDOR -> "🚪"
            }
            player.sendMessage(
                Component.text("$typeIcon Sala ${index + 1}: ${room.name}", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
            )
            player.sendMessage(
                Component.text("   Tipo: ", NamedTextColor.GRAY)
                    .append(Component.text(room.type.name, NamedTextColor.AQUA))
            )
            player.sendMessage(
                Component.text("   Tamanho: ", NamedTextColor.GRAY)
                    .append(Component.text("${room.width}x${room.depth}x${room.height}", NamedTextColor.WHITE))
            )

            val tpCmd = "/tp ${player.name} ${room.spawnPoint.blockX} ${room.spawnPoint.blockY} ${room.spawnPoint.blockZ}"
            player.sendMessage(
                Component.text("   Spawn: ", NamedTextColor.GRAY)
                    .append(
                        Component.text("${room.spawnPoint.blockX}, ${room.spawnPoint.blockY}, ${room.spawnPoint.blockZ}", NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand(tpCmd))
                            .decorate(TextDecoration.UNDERLINED)
                    )
                    .append(Component.text(" (clique para tp)", NamedTextColor.DARK_GRAY))
            )
            player.sendMessage(Component.empty())
        }

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
        player.sendMessage(Component.text("📝 PRÓXIMOS PASSOS:", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
        player.sendMessage(Component.text("  1. Abra o painel: ", NamedTextColor.GRAY)
            .append(Component.text("/typewriter", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/typewriter"))))
        player.sendMessage(Component.text("  2. Crie um Dungeon Config", NamedTextColor.GRAY))
        player.sendMessage(Component.text("     - templateWorldName: ${world.name}", NamedTextColor.DARK_GRAY))
        player.sendMessage(Component.text("     - minPlayers: 1, maxPlayers: 4", NamedTextColor.DARK_GRAY))
        player.sendMessage(Component.text("  3. Crie Room Configs com as coordenadas acima", NamedTextColor.GRAY))
        player.sendMessage(Component.text("  4. Crie Wave Configs + Mob Configs", NamedTextColor.GRAY))
        player.sendMessage(Component.text("  5. Crie Boss Config para a sala de boss", NamedTextColor.GRAY))
        player.sendMessage(Component.text("  6. Crie Loot Table para recompensas", NamedTextColor.GRAY))
        player.sendMessage(Component.text("  7. Monte a sequência: Start → Waves → Boss → End", NamedTextColor.GRAY))
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))

        player.sendMessage(Component.empty())
        player.sendMessage(
            Component.text("💡 Dica: ", NamedTextColor.GREEN)
                .append(Component.text("Para usar como template, salve cada sala como schematic:", NamedTextColor.GRAY))
        )
        player.sendMessage(Component.text("   //pos1 → //pos2 → //copy → //schematic save <nome>", NamedTextColor.YELLOW))
    }

    // ==================== CONSTRUÇÃO DE SALAS ====================

    private fun buildStyledRoom(
        world: World, x: Int, y: Int, z: Int,
        width: Int, depth: Int, height: Int,
        theme: DungeonTheme, type: RoomType, name: String,
    ): BuiltRoom {
        val x2 = x + width - 1
        val y2 = y + height - 1
        val z2 = z + depth - 1

        for (bx in x..x2) {
            for (by in y..y2) {
                for (bz in z..z2) {
                    val isWallX = bx == x || bx == x2
                    val isWallZ = bz == z || bz == z2
                    val isFloor = by == y
                    val isCeiling = by == y2
                    val isCorner = isWallX && isWallZ

                    val block = world.getBlockAt(bx, by, bz)
                    when {
                        isFloor -> block.type = theme.floor
                        isCeiling -> block.type = theme.ceiling
                        isCorner -> block.type = theme.pillar
                        isWallX || isWallZ -> {
                            // Padrão de acentos nas paredes
                            if (by == y + 1 || by == y2 - 1) {
                                block.type = theme.accent
                            } else {
                                block.type = theme.wall
                            }
                        }
                        else -> block.type = Material.AIR
                    }
                }
            }
        }

        // Iluminação
        addLighting(world, x, y, z, width, depth, height, theme)

        val spawnPoint = Location(world, x + width / 2.0 + 0.5, y + 1.0, z + depth / 2.0 + 0.5)
        return BuiltRoom(name, type, Location(world, x.toDouble(), y.toDouble(), z.toDouble()), width, depth, height, spawnPoint)
    }

    private fun addLighting(world: World, x: Int, y: Int, z: Int, width: Int, depth: Int, height: Int, theme: DungeonTheme) {
        val lightY = y + height - 2
        val interval = 4

        for (lx in (x + 2) until (x + width - 2) step interval) {
            for (lz in (z + 2) until (z + depth - 2) step interval) {
                // Luzes no teto
                world.getBlockAt(lx, lightY, lz).type = theme.light
            }
        }
    }

    private fun buildStyledCorridor(world: World, fromRoom: BuiltRoom, toX: Int, baseY: Int, centerZ: Int, theme: DungeonTheme) {
        val fromX = fromRoom.origin.blockX + fromRoom.width
        val w = 1 // meia-largura
        val h = 4

        for (x in fromX until toX) {
            for (dz in -w..w) {
                for (dy in 0 until h) {
                    val block = world.getBlockAt(x, baseY + dy, centerZ + dz)
                    when {
                        dy == 0 -> block.type = theme.floor
                        dy == h - 1 -> block.type = theme.ceiling
                        dz == -w || dz == w -> block.type = theme.wall
                        else -> block.type = Material.AIR
                    }
                }
            }
        }

        // Luzes no corredor
        for (x in (fromX + 2) until toX step 3) {
            world.getBlockAt(x, baseY + h - 2, centerZ).type = theme.light
        }
    }

    // ==================== DECORAÇÕES POR TIPO ====================

    private fun decorateSpawnRoom(world: World, room: BuiltRoom, theme: DungeonTheme) {
        val cx = room.origin.blockX + room.width / 2
        val cy = room.origin.blockY
        val cz = room.origin.blockZ + room.depth / 2

        // Beacon no centro
        world.getBlockAt(cx, cy, cz).type = Material.SEA_LANTERN
        world.getBlockAt(cx, cy + 1, cz).type = Material.BEACON

        // Carpetes ao redor
        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                world.getBlockAt(cx + dx, cy + 1, cz + dz).type = Material.LIGHT_BLUE_CARPET
            }
        }

        // Banners nos cantos
        val cornerOffset = 2
        world.getBlockAt(cx - cornerOffset, cy + 1, cz - cornerOffset).type = Material.BLUE_BANNER
        world.getBlockAt(cx + cornerOffset, cy + 1, cz - cornerOffset).type = Material.BLUE_BANNER
        world.getBlockAt(cx - cornerOffset, cy + 1, cz + cornerOffset).type = Material.BLUE_BANNER
        world.getBlockAt(cx + cornerOffset, cy + 1, cz + cornerOffset).type = Material.BLUE_BANNER
    }

    private fun decorateCombatRoom(world: World, room: BuiltRoom, theme: DungeonTheme) {
        val cx = room.origin.blockX + room.width / 2
        val cy = room.origin.blockY
        val cz = room.origin.blockZ + room.depth / 2

        // Pilares de combate
        val offset = room.width / 4
        val pillarPositions = listOf(
            cx - offset to cz - offset,
            cx + offset to cz - offset,
            cx - offset to cz + offset,
            cx + offset to cz + offset,
        )

        for ((px, pz) in pillarPositions) {
            for (h in 1 until room.height - 1) {
                world.getBlockAt(px, cy + h, pz).type = theme.pillar
            }
        }

        // Decorações aleatórias nas paredes
        val ox = room.origin.blockX
        val oz = room.origin.blockZ
        for (i in 0 until 4) {
            val dx = ox + 2 + (i * (room.width - 4) / 3)
            world.getBlockAt(dx, cy + 2, oz + 1).type = Material.IRON_BARS
            world.getBlockAt(dx, cy + 2, oz + room.depth - 2).type = Material.IRON_BARS
        }
    }

    private fun decoratePuzzleRoom(world: World, room: BuiltRoom, theme: DungeonTheme) {
        val cx = room.origin.blockX + room.width / 2
        val cy = room.origin.blockY
        val cz = room.origin.blockZ + room.depth / 2

        // Placas de pressão em padrão
        val platePositions = listOf(
            cx - 2 to cz, cx + 2 to cz,
            cx to cz - 2, cx to cz + 2,
            cx to cz,
        )
        for ((px, pz) in platePositions) {
            world.getBlockAt(px, cy + 1, pz).type = Material.HEAVY_WEIGHTED_PRESSURE_PLATE
        }

        // Lâmpadas de redstone nos cantos
        val offset = room.width / 3
        world.getBlockAt(cx - offset, cy + 1, cz - offset).type = Material.REDSTONE_LAMP
        world.getBlockAt(cx + offset, cy + 1, cz - offset).type = Material.REDSTONE_LAMP
        world.getBlockAt(cx - offset, cy + 1, cz + offset).type = Material.REDSTONE_LAMP
        world.getBlockAt(cx + offset, cy + 1, cz + offset).type = Material.REDSTONE_LAMP

        // Baú com chave
        world.getBlockAt(cx, cy + 1, cz + offset).type = Material.TRAPPED_CHEST
    }

    private fun decorateBossRoom(world: World, room: BuiltRoom, theme: DungeonTheme) {
        val cx = room.origin.blockX + room.width / 2
        val cy = room.origin.blockY
        val cz = room.origin.blockZ + room.depth / 2

        // Arena circular com magma
        for (dx in -2..2) {
            for (dz in -2..2) {
                if (dx * dx + dz * dz <= 4) {
                    world.getBlockAt(cx + dx, cy, cz + dz).type = Material.MAGMA_BLOCK
                }
            }
        }

        // Pilares grandes nos 4 cantos
        val offset = room.width / 3
        val pillarPositions = listOf(
            cx - offset to cz - offset,
            cx + offset to cz - offset,
            cx - offset to cz + offset,
            cx + offset to cz + offset,
        )

        for ((px, pz) in pillarPositions) {
            for (h in 0 until room.height) {
                world.getBlockAt(px, cy + h, pz).type = theme.pillar
                world.getBlockAt(px + 1, cy + h, pz).type = theme.pillar
                world.getBlockAt(px, cy + h, pz + 1).type = theme.pillar
                world.getBlockAt(px + 1, cy + h, pz + 1).type = theme.pillar
            }
            // Fogo no topo
            world.getBlockAt(px, cy + room.height - 2, pz).type = Material.SOUL_CAMPFIRE
            world.getBlockAt(px + 1, cy + room.height - 2, pz + 1).type = Material.SOUL_CAMPFIRE
        }

        // Trono/altar do boss
        world.getBlockAt(cx, cy + 1, cz - offset + 1).type = Material.QUARTZ_STAIRS
        world.getBlockAt(cx, cy + 1, cz - offset).type = Material.QUARTZ_BLOCK
        world.getBlockAt(cx, cy + 2, cz - offset).type = Material.SKELETON_SKULL

        // Banners vermelhos
        world.getBlockAt(cx - 1, cy + 3, cz - offset).type = Material.RED_BANNER
        world.getBlockAt(cx + 1, cy + 3, cz - offset).type = Material.RED_BANNER
    }

    private fun decorateTreasureRoom(world: World, room: BuiltRoom, theme: DungeonTheme) {
        val cx = room.origin.blockX + room.width / 2
        val cy = room.origin.blockY
        val cz = room.origin.blockZ + room.depth / 2

        // Piso dourado
        for (dx in -1..1) {
            for (dz in -1..1) {
                world.getBlockAt(cx + dx, cy, cz + dz).type = Material.GOLD_BLOCK
            }
        }

        // Baú principal
        world.getBlockAt(cx, cy + 1, cz).type = Material.CHEST

        // Baús laterais
        world.getBlockAt(cx - 2, cy + 1, cz).type = Material.CHEST
        world.getBlockAt(cx + 2, cy + 1, cz).type = Material.CHEST

        // Blocos de diamante decorativos
        world.getBlockAt(cx, cy + 1, cz - 2).type = Material.DIAMOND_BLOCK
        world.getBlockAt(cx, cy + 1, cz + 2).type = Material.DIAMOND_BLOCK
    }
}

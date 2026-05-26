package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.entries.statics.RoomTemplateEntry
import me.ray.midgardDungeon.entries.statics.RoomType
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import kotlin.random.Random

/**
 * Gerador procedural de dungeons usando blocos de construção ou templates de schematics.
 * Cria layouts aleatórios com salas conectadas por corredores.
 *
 * Modo Template: Builders criam salas bonitas, salvam como schematic, e o gerador
 * monta a dungeon colando esses templates aleatoriamente.
 *
 * Modo Básico: Se não há templates, gera salas simples com blocos.
 */
object ProceduralGenerator {

    data class ProceduralConfig(
        val roomCount: Int = 5,
        val roomMinSize: Int = 7,
        val roomMaxSize: Int = 15,
        val corridorWidth: Int = 3,
        val corridorLength: Int = 5,
        val wallMaterial: Material = Material.STONE_BRICKS,
        val floorMaterial: Material = Material.POLISHED_DEEPSLATE,
        val ceilingMaterial: Material = Material.STONE_BRICKS,
        val roomHeight: Int = 5,
        val hasTorches: Boolean = true,
        val difficulty: Int = 1, // 1-5
        val seed: Long = Random.nextLong(),
        val includeBossRoom: Boolean = true,
        val includeTreasureRoom: Boolean = true,
        val includeSecretRooms: Boolean = true,
        val secretRoomChance: Double = 0.3,
        val trapDensity: Double = 0.2,
        val templates: List<RoomTemplateEntry> = emptyList(),
        val useTemplates: Boolean = false,
    )

    data class GeneratedRoom(
        val id: Int,
        val type: RoomType,
        val originX: Int,
        val originY: Int,
        val originZ: Int,
        val width: Int,
        val depth: Int,
        val height: Int,
        val spawnLocation: Location? = null,
        val connections: MutableList<Int> = mutableListOf(),
        val trapLocations: MutableList<Location> = mutableListOf(),
        val secretEntrance: Location? = null,
        val templateName: String? = null,
        val spawnOffsetX: Double = 0.0,
        val spawnOffsetY: Double = 1.0,
        val spawnOffsetZ: Double = 0.0,
    )

    data class GeneratedDungeon(
        val rooms: List<GeneratedRoom>,
        val spawnLocation: Location,
        val bossRoomIndex: Int,
        val treasureRoomIndex: Int,
        val secretRoomIndices: List<Int>,
        val seed: Long,
    )

    fun generate(world: World, baseLocation: Location, config: ProceduralConfig): GeneratedDungeon {
        return if (config.useTemplates && config.templates.isNotEmpty() && SchematicManager.isAvailable()) {
            generateWithTemplates(world, baseLocation, config)
        } else {
            generateBasic(world, baseLocation, config)
        }
    }

    // ==================== GERAÇÃO COM TEMPLATES ====================

    /**
     * Gera dungeon usando templates de schematics feitos por builders.
     * Seleciona templates aleatoriamente por tipo de sala e cola no mundo.
     */
    private fun generateWithTemplates(world: World, baseLocation: Location, config: ProceduralConfig): GeneratedDungeon {
        val random = Random(config.seed)
        val rooms = mutableListOf<GeneratedRoom>()
        val secretRooms = mutableListOf<Int>()

        val baseX = baseLocation.blockX
        val baseY = baseLocation.blockY
        val baseZ = baseLocation.blockZ

        // Separar templates por tipo
        val templatesByType = config.templates.groupBy { it.getRoomType() }
        val spawnTemplates = templatesByType[RoomType.SPAWN] ?: emptyList()
        val normalTemplates = templatesByType[RoomType.NORMAL] ?: emptyList()
        val bossTemplates = templatesByType[RoomType.BOSS] ?: emptyList()
        val treasureTemplates = templatesByType[RoomType.TREASURE] ?: emptyList()
        val puzzleTemplates = templatesByType[RoomType.PUZZLE] ?: emptyList()
        val corridorTemplates = templatesByType[RoomType.CORRIDOR] ?: emptyList()

        // Selecionar template para spawn
        val spawnTemplate = pickWeightedTemplate(spawnTemplates, random)
        val spawnW: Int
        val spawnD: Int
        val spawnH: Int

        if (spawnTemplate != null) {
            spawnW = spawnTemplate.width
            spawnD = spawnTemplate.depth
            spawnH = spawnTemplate.height
        } else {
            spawnW = config.roomMinSize + 2
            spawnD = config.roomMinSize + 2
            spawnH = config.roomHeight
        }

        val spawnRoom = GeneratedRoom(
            id = 0,
            type = RoomType.SPAWN,
            originX = baseX, originY = baseY, originZ = baseZ,
            width = spawnW, depth = spawnD, height = spawnH,
            templateName = spawnTemplate?.schematicName,
            spawnOffsetX = spawnTemplate?.spawnOffsetX ?: (spawnW / 2.0),
            spawnOffsetY = spawnTemplate?.spawnOffsetY ?: 1.0,
            spawnOffsetZ = spawnTemplate?.spawnOffsetZ ?: (spawnD / 2.0),
        )
        rooms.add(spawnRoom)

        // Gerar salas normais com templates
        var currentX = baseX + spawnW + config.corridorLength
        val normalRoomCount = config.roomCount - (if (config.includeBossRoom) 1 else 0)

        for (i in 1 until normalRoomCount) {
            val offsetZ = random.nextInt(-5, 6)

            // Escolher tipo: normal ou puzzle
            val usePuzzle = puzzleTemplates.isNotEmpty() && random.nextDouble() < 0.2
            val templatePool = if (usePuzzle) puzzleTemplates else normalTemplates
            val template = pickWeightedTemplate(templatePool, random)

            val w: Int
            val d: Int
            val h: Int
            if (template != null) {
                w = template.width
                d = template.depth
                h = template.height
            } else {
                w = random.nextInt(config.roomMinSize, config.roomMaxSize + 1)
                d = random.nextInt(config.roomMinSize, config.roomMaxSize + 1)
                h = config.roomHeight
            }

            val room = GeneratedRoom(
                id = rooms.size,
                type = if (usePuzzle) RoomType.PUZZLE else RoomType.NORMAL,
                originX = currentX, originY = baseY, originZ = baseZ + offsetZ,
                width = w, depth = d, height = h,
                templateName = template?.schematicName,
                spawnOffsetX = template?.spawnOffsetX ?: (w / 2.0),
                spawnOffsetY = template?.spawnOffsetY ?: 1.0,
                spawnOffsetZ = template?.spawnOffsetZ ?: (d / 2.0),
            )
            rooms.add(room)
            currentX += w + config.corridorLength

            // Sala secreta
            if (config.includeSecretRooms && random.nextDouble() < config.secretRoomChance) {
                val secretTemplate = pickWeightedTemplate(treasureTemplates, random)
                val secretDir = if (random.nextBoolean()) 1 else -1
                val sW = secretTemplate?.width ?: 5
                val sD = secretTemplate?.depth ?: 5
                val sH = secretTemplate?.height ?: config.roomHeight

                val secretRoom = GeneratedRoom(
                    id = rooms.size,
                    type = RoomType.TREASURE,
                    originX = room.originX + room.width / 2,
                    originY = baseY,
                    originZ = room.originZ + (room.depth + 3) * secretDir,
                    width = sW, depth = sD, height = sH,
                    templateName = secretTemplate?.schematicName,
                    spawnOffsetX = secretTemplate?.spawnOffsetX ?: (sW / 2.0),
                    spawnOffsetY = secretTemplate?.spawnOffsetY ?: 1.0,
                    spawnOffsetZ = secretTemplate?.spawnOffsetZ ?: (sD / 2.0),
                )
                rooms.add(secretRoom)
                secretRooms.add(secretRoom.id)
                room.connections.add(secretRoom.id)
                secretRoom.connections.add(room.id)
            }
        }

        // Sala de boss
        var bossRoomIndex = -1
        if (config.includeBossRoom) {
            val bossTemplate = pickWeightedTemplate(bossTemplates, random)
            val bW = bossTemplate?.width ?: (config.roomMaxSize + 4)
            val bD = bossTemplate?.depth ?: (config.roomMaxSize + 4)
            val bH = bossTemplate?.height ?: (config.roomHeight + 3)

            val bossRoom = GeneratedRoom(
                id = rooms.size,
                type = RoomType.BOSS,
                originX = currentX, originY = baseY, originZ = baseZ,
                width = bW, depth = bD, height = bH,
                templateName = bossTemplate?.schematicName,
                spawnOffsetX = bossTemplate?.spawnOffsetX ?: (bW / 2.0),
                spawnOffsetY = bossTemplate?.spawnOffsetY ?: 1.0,
                spawnOffsetZ = bossTemplate?.spawnOffsetZ ?: (bD / 2.0),
            )
            bossRoomIndex = rooms.size
            rooms.add(bossRoom)
            currentX += bW + config.corridorLength
        }

        // Sala de tesouro
        var treasureRoomIndex = -1
        if (config.includeTreasureRoom) {
            val treasureTemplate = pickWeightedTemplate(treasureTemplates, random)
            val tW = treasureTemplate?.width ?: config.roomMinSize
            val tD = treasureTemplate?.depth ?: config.roomMinSize
            val tH = treasureTemplate?.height ?: config.roomHeight

            val treasureRoom = GeneratedRoom(
                id = rooms.size,
                type = RoomType.TREASURE,
                originX = currentX, originY = baseY, originZ = baseZ,
                width = tW, depth = tD, height = tH,
                templateName = treasureTemplate?.schematicName,
                spawnOffsetX = treasureTemplate?.spawnOffsetX ?: (tW / 2.0),
                spawnOffsetY = treasureTemplate?.spawnOffsetY ?: 1.0,
                spawnOffsetZ = treasureTemplate?.spawnOffsetZ ?: (tD / 2.0),
            )
            treasureRoomIndex = rooms.size
            rooms.add(treasureRoom)
        }

        // Conectar salas sequencialmente
        for (i in 0 until rooms.size - 1) {
            if (rooms[i].type != RoomType.TREASURE || i == 0) {
                val nextMain = (i + 1 until rooms.size).firstOrNull {
                    rooms[it].type != RoomType.TREASURE || it == treasureRoomIndex
                }
                if (nextMain != null && !rooms[i].connections.contains(nextMain)) {
                    rooms[i].connections.add(nextMain)
                    rooms[nextMain].connections.add(rooms[i].id)
                }
            }
        }

        // Construir salas no mundo
        for (room in rooms) {
            if (room.templateName != null) {
                // Colar schematic do template
                val loc = Location(world, room.originX.toDouble(), room.originY.toDouble(), room.originZ.toDouble())
                val pasted = SchematicManager.pasteSchematic(world, loc, room.templateName)
                if (!pasted) {
                    // Fallback: gerar sala básica se schematic falhar
                    buildRoom(world, room, config)
                }
            } else {
                // Sem template: gerar sala básica
                buildRoom(world, room, config)
            }
        }

        // Construir corredores (sempre gerados proceduralmente)
        val corridorTemplate = pickWeightedTemplate(corridorTemplates, random)
        for (room in rooms) {
            for (connId in room.connections) {
                if (connId > room.id) {
                    val target = rooms[connId]
                    if (corridorTemplate != null) {
                        buildCorridorWithTemplate(world, room, target, corridorTemplate, config)
                    } else {
                        buildCorridor(world, room, target, config)
                    }
                }
            }
        }

        // Armadilhas (apenas em salas sem template ou se configurado)
        if (config.trapDensity > 0) {
            for (room in rooms) {
                if (room.type == RoomType.SPAWN || room.type == RoomType.TREASURE) continue
                if (room.templateName == null) {
                    addTraps(world, room, config, random)
                }
            }
        }

        // Definir spawn locations
        val spawnLoc = Location(
            world,
            spawnRoom.originX + spawnRoom.spawnOffsetX,
            spawnRoom.originY + spawnRoom.spawnOffsetY,
            spawnRoom.originZ + spawnRoom.spawnOffsetZ,
        )

        val roomsWithSpawns = rooms.map { room ->
            room.copy(
                spawnLocation = Location(
                    world,
                    room.originX + room.spawnOffsetX,
                    room.originY + room.spawnOffsetY,
                    room.originZ + room.spawnOffsetZ,
                )
            )
        }

        return GeneratedDungeon(
            rooms = roomsWithSpawns,
            spawnLocation = spawnLoc,
            bossRoomIndex = bossRoomIndex,
            treasureRoomIndex = treasureRoomIndex,
            secretRoomIndices = secretRooms,
            seed = config.seed,
        )
    }

    /**
     * Seleciona um template aleatório com peso.
     */
    private fun pickWeightedTemplate(templates: List<RoomTemplateEntry>, random: Random): RoomTemplateEntry? {
        if (templates.isEmpty()) return null
        if (templates.size == 1) return templates[0]

        val totalWeight = templates.sumOf { it.weight }
        var roll = random.nextDouble() * totalWeight
        for (template in templates) {
            roll -= template.weight
            if (roll <= 0) return template
        }
        return templates.last()
    }

    /**
     * Constrói corredor usando segmentos de template de schematic.
     */
    private fun buildCorridorWithTemplate(
        world: World, from: GeneratedRoom, to: GeneratedRoom,
        template: RoomTemplateEntry, config: ProceduralConfig,
    ) {
        val fromCX = from.originX + from.width / 2
        val fromCZ = from.originZ + from.depth / 2
        val toCX = to.originX + to.width / 2
        val toCZ = to.originZ + to.depth / 2
        val y = from.originY

        // Colar segmentos do template ao longo do corredor
        val dx = toCX - fromCX
        val dz = toCZ - fromCZ
        val steps = maxOf(
            Math.abs(dx) / template.width.coerceAtLeast(1),
            Math.abs(dz) / template.depth.coerceAtLeast(1),
            1
        )

        for (step in 0..steps) {
            val t = step.toDouble() / steps.coerceAtLeast(1)
            val x = fromCX + (dx * t).toInt()
            val z = fromCZ + (dz * t).toInt()
            val loc = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
            if (!SchematicManager.pasteSchematic(world, loc, template.schematicName)) {
                // Fallback para corredor básico
                buildCorridor(world, from, to, config)
                return
            }
        }
    }

    // ==================== GERAÇÃO BÁSICA (SEM TEMPLATES) ====================

    private fun generateBasic(world: World, baseLocation: Location, config: ProceduralConfig): GeneratedDungeon {
        val random = Random(config.seed)
        val rooms = mutableListOf<GeneratedRoom>()
        val secretRooms = mutableListOf<Int>()

        val baseX = baseLocation.blockX
        val baseY = baseLocation.blockY
        val baseZ = baseLocation.blockZ

        // Gerar sala de spawn
        val spawnRoom = generateRoom(
            id = 0,
            type = RoomType.SPAWN,
            x = baseX, y = baseY, z = baseZ,
            width = config.roomMinSize + 2, depth = config.roomMinSize + 2,
            height = config.roomHeight,
            random = random,
        )
        rooms.add(spawnRoom)

        // Gerar salas normais
        var currentX = baseX + spawnRoom.width + config.corridorLength
        for (i in 1 until config.roomCount - (if (config.includeBossRoom) 1 else 0)) {
            val w = random.nextInt(config.roomMinSize, config.roomMaxSize + 1)
            val d = random.nextInt(config.roomMinSize, config.roomMaxSize + 1)
            val offsetZ = random.nextInt(-5, 6)

            val room = generateRoom(
                id = rooms.size,
                type = RoomType.NORMAL,
                x = currentX, y = baseY, z = baseZ + offsetZ,
                width = w, depth = d,
                height = config.roomHeight,
                random = random,
            )
            rooms.add(room)
            currentX += w + config.corridorLength

            // Chance de sala secreta
            if (config.includeSecretRooms && random.nextDouble() < config.secretRoomChance) {
                val secretDir = if (random.nextBoolean()) 1 else -1
                val secretRoom = generateRoom(
                    id = rooms.size,
                    type = RoomType.TREASURE,
                    x = room.originX + room.width / 2,
                    y = baseY,
                    z = room.originZ + (room.depth + 3) * secretDir,
                    width = 5, depth = 5,
                    height = config.roomHeight,
                    random = random,
                )
                rooms.add(secretRoom)
                secretRooms.add(secretRoom.id)
                room.connections.add(secretRoom.id)
                secretRoom.connections.add(room.id)
            }
        }

        // Sala de boss
        var bossRoomIndex = -1
        if (config.includeBossRoom) {
            val bossRoom = generateRoom(
                id = rooms.size,
                type = RoomType.BOSS,
                x = currentX, y = baseY, z = baseZ,
                width = config.roomMaxSize + 4, depth = config.roomMaxSize + 4,
                height = config.roomHeight + 3,
                random = random,
            )
            bossRoomIndex = rooms.size
            rooms.add(bossRoom)
            currentX += bossRoom.width + config.corridorLength
        }

        // Sala de tesouro
        var treasureRoomIndex = -1
        if (config.includeTreasureRoom) {
            val treasureRoom = generateRoom(
                id = rooms.size,
                type = RoomType.TREASURE,
                x = currentX, y = baseY, z = baseZ,
                width = config.roomMinSize, depth = config.roomMinSize,
                height = config.roomHeight,
                random = random,
            )
            treasureRoomIndex = rooms.size
            rooms.add(treasureRoom)
        }

        // Conectar salas sequencialmente
        for (i in 0 until rooms.size - 1) {
            if (rooms[i].type != RoomType.TREASURE || i == 0) {
                val nextMain = (i + 1 until rooms.size).firstOrNull { rooms[it].type != RoomType.TREASURE || it == treasureRoomIndex }
                if (nextMain != null && !rooms[i].connections.contains(nextMain)) {
                    rooms[i].connections.add(nextMain)
                    rooms[nextMain].connections.add(rooms[i].id)
                }
            }
        }

        // Construir todas as salas no mundo
        for (room in rooms) {
            buildRoom(world, room, config)
        }

        // Construir corredores
        for (room in rooms) {
            for (connId in room.connections) {
                if (connId > room.id) { // Evitar corredores duplicados
                    val target = rooms[connId]
                    buildCorridor(world, room, target, config)
                }
            }
        }

        // Adicionar armadilhas
        if (config.trapDensity > 0) {
            for (room in rooms) {
                if (room.type == RoomType.SPAWN || room.type == RoomType.TREASURE) continue
                addTraps(world, room, config, random)
            }
        }

        // Definir locais de spawn
        val spawnLoc = Location(
            world,
            spawnRoom.originX + spawnRoom.width / 2.0,
            (spawnRoom.originY + 1).toDouble(),
            spawnRoom.originZ + spawnRoom.depth / 2.0,
        )

        val roomsWithSpawns = rooms.map { room ->
            room.copy(
                spawnLocation = Location(
                    world,
                    room.originX + room.width / 2.0,
                    (room.originY + 1).toDouble(),
                    room.originZ + room.depth / 2.0,
                )
            )
        }

        return GeneratedDungeon(
            rooms = roomsWithSpawns,
            spawnLocation = spawnLoc,
            bossRoomIndex = bossRoomIndex,
            treasureRoomIndex = treasureRoomIndex,
            secretRoomIndices = secretRooms,
            seed = config.seed,
        )
    }

    private fun generateRoom(
        id: Int, type: RoomType,
        x: Int, y: Int, z: Int,
        width: Int, depth: Int, height: Int,
        random: Random,
    ): GeneratedRoom {
        return GeneratedRoom(
            id = id, type = type,
            originX = x, originY = y, originZ = z,
            width = width, depth = depth, height = height,
        )
    }

    private fun buildRoom(world: World, room: GeneratedRoom, config: ProceduralConfig) {
        val x1 = room.originX
        val y1 = room.originY
        val z1 = room.originZ
        val x2 = x1 + room.width - 1
        val y2 = y1 + room.height - 1
        val z2 = z1 + room.depth - 1

        for (x in x1..x2) {
            for (y in y1..y2) {
                for (z in z1..z2) {
                    val isWall = x == x1 || x == x2 || z == z1 || z == z2
                    val isFloor = y == y1
                    val isCeiling = y == y2

                    val block = world.getBlockAt(x, y, z)
                    when {
                        isFloor -> block.type = config.floorMaterial
                        isCeiling -> block.type = config.ceilingMaterial
                        isWall -> block.type = config.wallMaterial
                        else -> block.type = Material.AIR
                    }
                }
            }
        }

        // Adicionar tochas
        if (config.hasTorches) {
            addTorches(world, room, config)
        }

        // Decorar baseado no tipo da sala
        when (room.type) {
            RoomType.BOSS -> decorateBossRoom(world, room, config)
            RoomType.TREASURE -> decorateTreasureRoom(world, room)
            RoomType.SPAWN -> decorateSpawnRoom(world, room)
            else -> {}
        }
    }

    private fun addTorches(world: World, room: GeneratedRoom, config: ProceduralConfig) {
        val y = room.originY + 2
        val interval = 4

        // Ao longo das paredes
        for (x in (room.originX + 2) until (room.originX + room.width - 2) step interval) {
            world.getBlockAt(x, y, room.originZ + 1).type = Material.WALL_TORCH
            world.getBlockAt(x, y, room.originZ + room.depth - 2).type = Material.WALL_TORCH
        }
    }

    private fun decorateBossRoom(world: World, room: GeneratedRoom, config: ProceduralConfig) {
        // Pilares na sala do boss
        val cx = room.originX + room.width / 2
        val cz = room.originZ + room.depth / 2
        val pillarOffsets = listOf(-3 to -3, 3 to -3, -3 to 3, 3 to 3)

        for ((dx, dz) in pillarOffsets) {
            for (y in (room.originY + 1) until (room.originY + room.height - 1)) {
                world.getBlockAt(cx + dx, y, cz + dz).type = Material.OBSIDIAN
            }
        }

        // Poça de lava no centro
        world.getBlockAt(cx, room.originY, cz).type = Material.MAGMA_BLOCK
    }

    private fun decorateTreasureRoom(world: World, room: GeneratedRoom) {
        val cx = room.originX + room.width / 2
        val cz = room.originZ + room.depth / 2
        world.getBlockAt(cx, room.originY + 1, cz).type = Material.CHEST
    }

    private fun decorateSpawnRoom(world: World, room: GeneratedRoom) {
        // Beacon no spawn
        val cx = room.originX + room.width / 2
        val cz = room.originZ + room.depth / 2
        world.getBlockAt(cx, room.originY, cz).type = Material.SEA_LANTERN
    }

    private fun buildCorridor(world: World, from: GeneratedRoom, to: GeneratedRoom, config: ProceduralConfig) {
        val fromCX = from.originX + from.width / 2
        val fromCZ = from.originZ + from.depth / 2
        val toCX = to.originX + to.width / 2
        val toCZ = to.originZ + to.depth / 2
        val y = from.originY
        val w = config.corridorWidth / 2

        // Corredor horizontal (eixo X)
        val xStart = minOf(fromCX, toCX)
        val xEnd = maxOf(fromCX, toCX)
        for (x in xStart..xEnd) {
            for (dz in -w..w) {
                for (dy in 0 until config.roomHeight) {
                    val block = world.getBlockAt(x, y + dy, fromCZ + dz)
                    when (dy) {
                        0 -> block.type = config.floorMaterial
                        config.roomHeight - 1 -> block.type = config.ceilingMaterial
                        else -> {
                            if (dz == -w || dz == w) {
                                block.type = config.wallMaterial
                            } else {
                                block.type = Material.AIR
                            }
                        }
                    }
                }
            }
        }

        // Corredor vertical (eixo Z) se necessário
        if (fromCZ != toCZ) {
            val zStart = minOf(fromCZ, toCZ)
            val zEnd = maxOf(fromCZ, toCZ)
            for (z in zStart..zEnd) {
                for (dx in -w..w) {
                    for (dy in 0 until config.roomHeight) {
                        val block = world.getBlockAt(toCX + dx, y + dy, z)
                        when (dy) {
                            0 -> block.type = config.floorMaterial
                            config.roomHeight - 1 -> block.type = config.ceilingMaterial
                            else -> {
                                if (dx == -w || dx == w) {
                                    block.type = config.wallMaterial
                                } else {
                                    block.type = Material.AIR
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addTraps(world: World, room: GeneratedRoom, config: ProceduralConfig, random: Random) {
        val floorY = room.originY
        val trapCount = (room.width * room.depth * config.trapDensity / 10).toInt().coerceAtLeast(1)

        repeat(trapCount) {
            val x = random.nextInt(room.originX + 2, room.originX + room.width - 2)
            val z = random.nextInt(room.originZ + 2, room.originZ + room.depth - 2)

            // Armadilha de placa de pressão
            world.getBlockAt(x, floorY + 1, z).type = Material.STONE_PRESSURE_PLATE

            val loc = Location(world, x.toDouble(), (floorY + 1).toDouble(), z.toDouble())
            room.trapLocations.add(loc)
        }
    }
}

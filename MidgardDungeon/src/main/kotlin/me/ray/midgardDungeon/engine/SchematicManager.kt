package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import java.io.File
import java.util.logging.Level

/**
 * Gerenciador de schematics usando a API do WorldEdit.
 * Cola templates de salas feitos por builders no mundo da dungeon.
 *
 * Os builders salvam schematics com:
 * 1. //wand (pegar machado de seleção)
 * 2. Selecionar a sala (pos1 e pos2)
 * 3. //copy
 * 4. //schematic save nome_da_sala
 *
 * Os arquivos ficam em: plugins/WorldEdit/schematics/
 */
object SchematicManager {

    private var worldEditAvailable = false
    private var schematicsFolder: File? = null

    fun initialize() {
        worldEditAvailable = Bukkit.getPluginManager().getPlugin("WorldEdit") != null
                || Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null

        if (worldEditAvailable) {
            // WorldEdit schematics folder
            val wePlugin = Bukkit.getPluginManager().getPlugin("WorldEdit")
                ?: Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit")
            if (wePlugin != null) {
                schematicsFolder = File(wePlugin.dataFolder, "schematics")
                if (!schematicsFolder!!.exists()) {
                    schematicsFolder!!.mkdirs()
                }
            }
            Bukkit.getLogger().info("[MidgardDungeon] SchematicManager inicializado com WorldEdit.")
        } else {
            Bukkit.getLogger().warning("[MidgardDungeon] WorldEdit não encontrado! Templates de sala não funcionarão.")
        }
    }

    fun shutdown() {
        // Nada a limpar
    }

    fun isAvailable(): Boolean = worldEditAvailable

    /**
     * Verifica se um schematic existe.
     */
    fun schematicExists(name: String): Boolean {
        if (!worldEditAvailable) return false
        val folder = schematicsFolder ?: return false
        return File(folder, "$name.schem").exists() || File(folder, "$name.schematic").exists()
    }

    /**
     * Lista todos os schematics disponíveis.
     */
    fun listSchematics(): List<String> {
        val folder = schematicsFolder ?: return emptyList()
        if (!folder.exists()) return emptyList()
        return folder.listFiles()
            ?.filter { it.extension in listOf("schem", "schematic") }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }

    /**
     * Cola um schematic no mundo na posição especificada usando WorldEdit API via reflexão.
     * Retorna true se conseguiu colar.
     */
    fun pasteSchematic(world: World, location: Location, schematicName: String): Boolean {
        if (!worldEditAvailable) return false
        val folder = schematicsFolder ?: return false

        val schematicFile = File(folder, "$schematicName.schem").let {
            if (it.exists()) it else File(folder, "$schematicName.schematic")
        }

        if (!schematicFile.exists()) {
            Bukkit.getLogger().warning("[MidgardDungeon] Schematic não encontrado: $schematicName")
            return false
        }

        return try {
            pasteWithWorldEditApi(world, location, schematicFile)
        } catch (e: Exception) {
            Bukkit.getLogger().log(Level.WARNING, "[MidgardDungeon] Falha ao colar schematic: $schematicName", e)
            false
        }
    }

    /**
     * Cola schematic usando a API do WorldEdit via reflexão para manter compatibilidade.
     */
    private fun pasteWithWorldEditApi(world: World, location: Location, file: File): Boolean {
        // com.sk89q.worldedit.WorldEdit
        val weClass = Class.forName("com.sk89q.worldedit.WorldEdit")
        val weInstance = weClass.getMethod("getInstance").invoke(null)

        // ClipboardFormat
        val formatClass = Class.forName("com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats")
        val format = formatClass.getMethod("findByFile", File::class.java).invoke(null, file)
            ?: return false

        // Abrir reader
        val getReader = format.javaClass.getMethod("getReader", java.io.InputStream::class.java)
        val fis = java.io.FileInputStream(file)
        val reader = getReader.invoke(format, fis)

        // Ler clipboard
        val readMethod = reader.javaClass.getMethod("read")
        val clipboard = readMethod.invoke(reader)

        // Fechar reader
        val closeMethod = reader.javaClass.getMethod("close")
        closeMethod.invoke(reader)
        fis.close()

        // BukkitAdapter.adapt(world)
        val adapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter")
        val weWorld = adapterClass.getMethod("adapt", World::class.java).invoke(null, world)

        // BlockVector3.at(x, y, z)
        val bv3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3")
        val at = bv3Class.getMethod("at", Int::class.java, Int::class.java, Int::class.java)
        val to = at.invoke(null, location.blockX, location.blockY, location.blockZ)

        // EditSession
        val editSessionFactoryMethod = weClass.getMethod("newEditSessionBuilder")
        val builder = editSessionFactoryMethod.invoke(weInstance)
        val worldMethod = builder.javaClass.getMethod("world", Class.forName("com.sk89q.worldedit.world.World"))
        worldMethod.invoke(builder, weWorld)
        val buildMethod = builder.javaClass.getMethod("build")
        val editSession = buildMethod.invoke(builder)

        // ClipboardHolder
        val holderClass = Class.forName("com.sk89q.worldedit.session.ClipboardHolder")
        val holder = holderClass.getConstructor(
            Class.forName("com.sk89q.worldedit.extent.clipboard.Clipboard")
        ).newInstance(clipboard)

        // Operation: paste
        val createPasteMethod = holder.javaClass.getMethod(
            "createPaste",
            Class.forName("com.sk89q.worldedit.extent.Extent")
        )
        val paste = createPasteMethod.invoke(holder, editSession)

        // .to(location)
        val toMethod = paste.javaClass.getMethod("to", bv3Class)
        toMethod.invoke(paste, to)

        // .ignoreAirBlocks(false)
        try {
            val ignoreAir = paste.javaClass.getMethod("ignoreAirBlocks", Boolean::class.java)
            ignoreAir.invoke(paste, false)
        } catch (_: Exception) {
            // Método pode não existir em todas as versões
        }

        // .build()
        val buildOp = paste.javaClass.getMethod("build")
        val operation = buildOp.invoke(paste)

        // Operations.complete(operation)
        val opsClass = Class.forName("com.sk89q.worldedit.function.operation.Operations")
        val completeMethod = opsClass.getMethod(
            "complete",
            Class.forName("com.sk89q.worldedit.function.operation.Operation")
        )
        completeMethod.invoke(null, operation)

        // Fechar editSession
        val closeSession = editSession.javaClass.getMethod("close")
        closeSession.invoke(editSession)

        return true
    }
}

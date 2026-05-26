package me.ray.midgardDungeon.engine

import me.ray.midgardDungeon.MidgardPlugin
import me.ray.midgardDungeon.entries.event.fireCutsceneCompleteEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Gerencia cutscenes cinematográficas para entrada de salas e bosses.
 *
 * Durante uma cutscene:
 * - Jogador entra em modo espectador (câmera livre)
 * - Câmera se move entre pontos definidos (keyframes)
 * - Mensagens/títulos são exibidos em momentos específicos
 * - Jogador fica invisível e imóvel
 * - Ao final, jogador retorna à posição e modo original
 */
object CutsceneManager {

    // Dados de um keyframe (ponto de câmera)
    data class CutsceneKeyframe(
        val location: Location,
        val durationTicks: Int,       // Duração neste ponto antes de mover
        val title: String = "",       // Título exibido neste keyframe
        val subtitle: String = "",    // Subtítulo
        val sound: String = "",       // Som ao chegar neste keyframe
        val soundVolume: Float = 1f,
        val soundPitch: Float = 1f,
        val particle: String = "",    // Partícula no ponto
        val particleCount: Int = 30,
    )

    // Dados de uma cutscene ativa
    data class ActiveCutscene(
        val playerId: UUID,
        val instanceId: UUID?,
        val keyframes: List<CutsceneKeyframe>,
        val originalLocation: Location,
        val originalGameMode: GameMode,
        val cutsceneId: String,
        var currentKeyframe: Int = 0,
        var task: BukkitTask? = null,
        var interpolationTask: BukkitTask? = null,
    )

    // Jogadores em cutscene ativa
    private val activeCutscenes = ConcurrentHashMap<UUID, ActiveCutscene>()

    // Configurações de interpolação
    private const val INTERPOLATION_TICKS = 2L   // A cada 2 ticks (100ms) atualiza posição
    private const val TRANSITION_TICKS = 20       // 1 segundo para transição entre keyframes

    fun initialize() {
        // Nada a inicializar
    }

    /**
     * Inicia uma cutscene para um jogador.
     */
    fun startCutscene(
        player: Player,
        cutsceneId: String,
        keyframes: List<CutsceneKeyframe>,
        instanceId: UUID? = null,
    ) {
        if (keyframes.isEmpty()) return
        if (isInCutscene(player.uniqueId)) {
            stopCutscene(player, skipEvent = true)
        }

        val cutscene = ActiveCutscene(
            playerId = player.uniqueId,
            instanceId = instanceId,
            keyframes = keyframes,
            originalLocation = player.location.clone(),
            originalGameMode = player.gameMode,
            cutsceneId = cutsceneId,
        )
        activeCutscenes[player.uniqueId] = cutscene

        // Preparar jogador
        player.gameMode = GameMode.SPECTATOR
        player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE, 0, false, false))

        // Esconder dos outros jogadores
        val plugin = MidgardPlugin.instance
        if (plugin != null) {
            Bukkit.getOnlinePlayers().filter { it.uniqueId != player.uniqueId }.forEach { other ->
                other.hidePlayer(plugin, player)
            }
        }

        // Começar pela primeira keyframe
        playKeyframe(player, cutscene, 0)
    }

    /**
     * Inicia cutscene para todos os jogadores de uma instância.
     */
    fun startCutsceneForInstance(
        instance: DungeonInstance,
        cutsceneId: String,
        keyframes: List<CutsceneKeyframe>,
    ) {
        instance.getOnlinePlayers().forEach { player ->
            startCutscene(player, cutsceneId, keyframes, instance.id)
        }
    }

    private fun playKeyframe(player: Player, cutscene: ActiveCutscene, index: Int) {
        if (index >= cutscene.keyframes.size) {
            // Cutscene terminou
            stopCutscene(player, skipEvent = false)
            return
        }

        cutscene.currentKeyframe = index
        val keyframe = cutscene.keyframes[index]

        // Teleportar para o início do keyframe (ou interpolar da posição atual)
        if (index == 0) {
            player.teleport(keyframe.location)
        } else {
            // Interpolação suave entre posição atual e próximo keyframe
            startInterpolation(player, player.location.clone(), keyframe.location, TRANSITION_TICKS, cutscene)
        }

        // Exibir título se configurado
        if (keyframe.title.isNotEmpty() || keyframe.subtitle.isNotEmpty()) {
            val titleComp = if (keyframe.title.isNotEmpty())
                Component.text(keyframe.title, NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
            else Component.empty()
            val subtitleComp = if (keyframe.subtitle.isNotEmpty())
                Component.text(keyframe.subtitle, NamedTextColor.YELLOW)
            else Component.empty()

            player.showTitle(Title.title(
                titleComp, subtitleComp,
                Title.Times.times(
                    Duration.ofMillis(300),
                    Duration.ofMillis((keyframe.durationTicks * 50L).coerceAtLeast(1000)),
                    Duration.ofMillis(500),
                )
            ))
        }

        // Tocar som
        if (keyframe.sound.isNotEmpty()) {
            try {
                val sound = Sound.valueOf(keyframe.sound.uppercase())
                player.playSound(keyframe.location, sound, keyframe.soundVolume, keyframe.soundPitch)
            } catch (_: Exception) {
                // Som inválido, ignorar
            }
        }

        // Spawnar partículas
        if (keyframe.particle.isNotEmpty()) {
            try {
                val particle = Particle.valueOf(keyframe.particle.uppercase())
                keyframe.location.world?.spawnParticle(
                    particle, keyframe.location,
                    keyframe.particleCount, 1.0, 1.0, 1.0, 0.05
                )
            } catch (_: Exception) {
                // Partícula inválida, ignorar
            }
        }

        // Adicionar barras cinematográficas (efeito de tela widescreen)
        player.sendMessage(Component.text("")) // Limpa chat para imersão

        // Agendar próximo keyframe após a duração
        val plugin = MidgardPlugin.instance ?: return
        val totalDelay = if (index == 0) keyframe.durationTicks.toLong()
        else (TRANSITION_TICKS + keyframe.durationTicks).toLong()

        cutscene.task = object : BukkitRunnable() {
            override fun run() {
                val p = Bukkit.getPlayer(cutscene.playerId) ?: return
                playKeyframe(p, cutscene, index + 1)
            }
        }.runTaskLater(plugin, totalDelay)
    }

    /**
     * Interpolação suave entre duas posições (câmera cinematográfica).
     */
    private fun startInterpolation(
        player: Player,
        from: Location,
        to: Location,
        ticks: Int,
        cutscene: ActiveCutscene,
    ) {
        val plugin = MidgardPlugin.instance ?: return
        var elapsed = 0

        cutscene.interpolationTask?.cancel()
        cutscene.interpolationTask = object : BukkitRunnable() {
            override fun run() {
                elapsed++
                val progress = elapsed.toDouble() / ticks
                if (progress >= 1.0 || Bukkit.getPlayer(cutscene.playerId) == null) {
                    Bukkit.getPlayer(cutscene.playerId)?.teleport(to)
                    cancel()
                    return
                }

                // Interpolação suave (ease-in-out)
                val smoothProgress = smoothStep(progress)

                val x = lerp(from.x, to.x, smoothProgress)
                val y = lerp(from.y, to.y, smoothProgress)
                val z = lerp(from.z, to.z, smoothProgress)
                val yaw = lerpAngle(from.yaw, to.yaw, smoothProgress.toFloat())
                val pitch = lerp(from.pitch.toDouble(), to.pitch.toDouble(), smoothProgress).toFloat()

                val interpolated = Location(to.world, x, y, z, yaw, pitch)
                Bukkit.getPlayer(cutscene.playerId)?.teleport(interpolated)
            }
        }.runTaskTimer(plugin, 0L, INTERPOLATION_TICKS)
    }

    /**
     * Para a cutscene de um jogador, restaurando estado original.
     */
    fun stopCutscene(player: Player, skipEvent: Boolean = false) {
        val cutscene = activeCutscenes.remove(player.uniqueId) ?: return

        // Cancelar tasks
        cutscene.task?.cancel()
        cutscene.interpolationTask?.cancel()

        // Restaurar estado do jogador
        player.teleport(cutscene.originalLocation)
        player.gameMode = cutscene.originalGameMode
        player.removePotionEffect(PotionEffectType.INVISIBILITY)

        // Mostrar jogador novamente
        val plugin = MidgardPlugin.instance
        if (plugin != null) {
            Bukkit.getOnlinePlayers().filter { it.uniqueId != player.uniqueId }.forEach { other ->
                other.showPlayer(plugin, player)
            }
        }

        // Mensagem de retorno
        player.showTitle(Title.title(
            Component.empty(), Component.empty(),
            Title.Times.times(Duration.ZERO, Duration.ZERO, Duration.ZERO),
        ))

        if (!skipEvent) {
            fireCutsceneCompleteEvent(player, cutscene.cutsceneId, cutscene.instanceId)
        }
    }

    /**
     * Para todas as cutscenes de uma instância.
     */
    fun stopCutsceneForInstance(instanceId: UUID) {
        activeCutscenes.values
            .filter { it.instanceId == instanceId }
            .forEach { cutscene ->
                Bukkit.getPlayer(cutscene.playerId)?.let { stopCutscene(it) }
            }
    }

    fun isInCutscene(playerId: UUID): Boolean = activeCutscenes.containsKey(playerId)

    fun getCutsceneId(playerId: UUID): String? = activeCutscenes[playerId]?.cutsceneId

    fun skipCutscene(player: Player) {
        if (isInCutscene(player.uniqueId)) {
            stopCutscene(player, skipEvent = false)
            player.sendMessage(Component.text("⏩ Cutscene pulada.", NamedTextColor.GRAY))
        }
    }

    fun removeInstance(instanceId: UUID) {
        stopCutsceneForInstance(instanceId)
    }

    fun shutdown() {
        activeCutscenes.values.forEach { cutscene ->
            cutscene.task?.cancel()
            cutscene.interpolationTask?.cancel()
            Bukkit.getPlayer(cutscene.playerId)?.let { player ->
                player.teleport(cutscene.originalLocation)
                player.gameMode = cutscene.originalGameMode
                player.removePotionEffect(PotionEffectType.INVISIBILITY)
                val plugin = MidgardPlugin.instance
                if (plugin != null) {
                    Bukkit.getOnlinePlayers().filter { it.uniqueId != player.uniqueId }.forEach { other ->
                        other.showPlayer(plugin, player)
                    }
                }
            }
        }
        activeCutscenes.clear()
    }

    // --- Funções de interpolação ---

    private fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t

    private fun smoothStep(t: Double): Double = t * t * (3 - 2 * t)

    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var diff = b - a
        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360
        return a + diff * t
    }
}

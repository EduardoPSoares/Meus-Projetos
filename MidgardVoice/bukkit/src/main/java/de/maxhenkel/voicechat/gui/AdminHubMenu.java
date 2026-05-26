package de.maxhenkel.voicechat.gui;

import de.maxhenkel.voicechat.Voicechat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class AdminHubMenu {

    private static final String S = ChatColor.DARK_GRAY + "-------------------------";

    public static String getTitle() {
        return Voicechat.MESSAGES.text("gui.admin.title", "&5&lMidgardVoice&r&5 - Painel Admin");
    }

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, getTitle());

        ItemStack glass = createGlass();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, glass);
        }
        for (int row = 1; row <= 2; row++) {
            inv.setItem(row * 9, glass);
            inv.setItem(row * 9 + 8, glass);
        }
        inv.setItem(27, glass);
        inv.setItem(35, glass);

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(Voicechat.MESSAGES.text("gui.admin.info_name", "&d&l* MidgardVoice"));
            infoMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.admin.info.line1", "&7 > &fCentro de administracao do sistema de voz."),
                    Voicechat.MESSAGES.text("gui.admin.info.line2", "&7 > &fCada opcao abaixo altera um aspecto"),
                    Voicechat.MESSAGES.text("gui.admin.info.line3", "&7   &fespecifico da experiencia de audio."),
                    "",
                    Voicechat.MESSAGES.text("gui.admin.info.line4", "&7 > &fUse este painel para ajustar regras,"),
                    Voicechat.MESSAGES.text("gui.admin.info.line5", "&7   &falcance, gravacao, cooldown e auditoria."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.admin.info.action", "&a > &eClique em uma categoria para abrir")
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        int zoneCount = Voicechat.restrictedZoneManager != null ? Voicechat.restrictedZoneManager.getZones().size() : 0;
        inv.setItem(10, createButton(Material.FILLED_MAP,
                Voicechat.MESSAGES.text("gui.admin.zones.name", "&6* &fZonas de Voz"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.admin.zones.line1", "&7 > &fCria e ajusta regras por regiao."),
                Voicechat.MESSAGES.text("gui.admin.zones.line2", "&7 > &fIdeal para cidades, eventos, dungeons"),
                Voicechat.MESSAGES.text("gui.admin.zones.line3", "&7   &fou qualquer area com regra propria."),
                "",
                Voicechat.MESSAGES.format("gui.admin.zones.count", "&7 > &eZonas ativas&8 - &f%s", zoneCount),
                Voicechat.MESSAGES.text("gui.admin.zones.scope", "&7 > &eAfeta&8 - &fvoz, stage, mute, cooldown e range local"),
                "",
                S,
                Voicechat.MESSAGES.text("gui.admin.zones.action", "&a > &eClique para gerenciar")));

        int rangeCount = Voicechat.playerRangeManager != null ? Voicechat.playerRangeManager.getAllRanges().size() : 0;
        float defaultRange = de.maxhenkel.voicechat.voice.common.Utils.getDefaultDistance();
        inv.setItem(12, createButton(Material.SPYGLASS,
                Voicechat.MESSAGES.text("gui.admin.range.name", "&b* &fRange de Voz"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.admin.range.line1", "&7 > &fDefine alcances personalizados por jogador."),
                Voicechat.MESSAGES.text("gui.admin.range.line2", "&7 > &fUse quando alguem precisa falar mais"),
                Voicechat.MESSAGES.text("gui.admin.range.line3", "&7   &flonge ou mais perto que o padrao."),
                "",
                Voicechat.MESSAGES.format("gui.admin.range.custom_count", "&7 > &eCustomizados&8 - &f%s", rangeCount),
                Voicechat.MESSAGES.format("gui.admin.range.default_value", "&7 > &ePadrao atual&8 - &f%s blocos", (int) defaultRange),
                "",
                S,
                Voicechat.MESSAGES.text("gui.admin.range.action", "&a > &eClique para configurar")));

        int volumeCount = Voicechat.playerRangeManager != null ? Voicechat.playerRangeManager.getAllVolumes().size() : 0;
        int priorityCount = Voicechat.playerRangeManager != null ? Voicechat.playerRangeManager.getAllPriorities().size() : 0;
        inv.setItem(14, createButton(Material.BELL,
                Voicechat.MESSAGES.text("gui.admin.volume_priority.name", "&e* &fVolume e Prioridade"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.admin.volume_priority.line1", "&7 > &fAjusta o ganho de audio e a prioridade"),
                Voicechat.MESSAGES.text("gui.admin.volume_priority.line2", "&7   &findividual de cada jogador."),
                Voicechat.MESSAGES.text("gui.admin.volume_priority.line3", "&7 > &fUtil para staffs, narradores, palco"),
                Voicechat.MESSAGES.text("gui.admin.volume_priority.line4", "&7   &fou jogadores que precisam destaque."),
                "",
                Voicechat.MESSAGES.format("gui.admin.volume_priority.volumes", "&7 > &eVolumes&8 - &f%s", volumeCount),
                Voicechat.MESSAGES.format("gui.admin.volume_priority.priorities", "&7 > &ePrioridades&8 - &f%s", priorityCount),
                "",
                S,
                Voicechat.MESSAGES.text("gui.admin.volume_priority.action", "&a > &eClique para ajustar")));

        int globalCount = Voicechat.playerRangeManager != null ? Voicechat.playerRangeManager.getGlobalPlayers().size() : 0;
        int maxGlobal = Voicechat.playerRangeManager != null ? Voicechat.playerRangeManager.getMaxGlobalPlayers() : 0;
        String maxStr = maxGlobal <= 0
                ? Voicechat.MESSAGES.text("gui.admin.global.unlimited", "ilimitado")
                : String.valueOf(maxGlobal);
        inv.setItem(16, createButton(Material.ENDER_EYE,
                Voicechat.MESSAGES.text("gui.admin.global.name", "&d* &fVoz Global"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.admin.global.line1", "&7 > &fGerencia quem pode ser ouvido por"),
                Voicechat.MESSAGES.text("gui.admin.global.line2", "&7   &ftodo o servidor, sem limite local."),
                Voicechat.MESSAGES.text("gui.admin.global.line3", "&7 > &fUse para anuncios, eventos e perfis"),
                Voicechat.MESSAGES.text("gui.admin.global.line4", "&7   &fcom permissao especial de transmissao."),
                "",
                Voicechat.MESSAGES.format("gui.admin.global.count", "&7 > &eGlobais&8 - &f%s/%s", globalCount, maxStr),
                Voicechat.MESSAGES.text("gui.admin.global.effect", "&7 > &eEfeito&8 - &fignora a distancia normal"),
                "",
                S,
                Voicechat.MESSAGES.text("gui.admin.global.action", "&a > &eClique para gerenciar")));

        int activeRec = Voicechat.voiceRecordingManager != null ? Voicechat.voiceRecordingManager.getActiveRecordings().size() : 0;
        int savedRec = Voicechat.voiceRecordingManager != null ? Voicechat.voiceRecordingManager.getSavedRecordings().size() : 0;
        inv.setItem(28, createButton(Material.JUKEBOX,
                Voicechat.MESSAGES.text("gui.admin.recording.name", "&c* &fGravacoes de Audio"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.admin.recording.line1", "&7 > &fInicia, acompanha e encerra gravacoes"),
                Voicechat.MESSAGES.text("gui.admin.recording.line2", "&7   &fde voz dos jogadores."),
                Voicechat.MESSAGES.text("gui.admin.recording.line3", "&7 > &fUse para registro administrativo,"),
                Voicechat.MESSAGES.text("gui.admin.recording.line4", "&7   &fprovas e acompanhamento de eventos."),
                "",
                Voicechat.MESSAGES.format("gui.admin.recording.active_count", "&7 > &eAtivas&8 - &f%s", activeRec),
                Voicechat.MESSAGES.format("gui.admin.recording.saved_count", "&7 > &eSalvas&8 - &f%s", savedRec),
                "",
                S,
                Voicechat.MESSAGES.text("gui.admin.recording.action", "&a > &eClique para abrir")));

        boolean cooldownOn = Voicechat.voiceCooldownManager != null && Voicechat.voiceCooldownManager.isEnabled();
        String cooldownStatus = cooldownOn
                ? Voicechat.MESSAGES.text("gui.admin.cooldown.status_enabled", "&aAtivado")
                : Voicechat.MESSAGES.text("gui.admin.cooldown.status_disabled", "&cDesativado");
        String cooldownDetail = cooldownOn
                ? Voicechat.MESSAGES.format(
                        "gui.admin.cooldown.detail_enabled",
                        "%ss fala / %ss espera",
                        Voicechat.voiceCooldownManager.getMaxTalkTimeMs() / 1000,
                        Voicechat.voiceCooldownManager.getCooldownMs() / 1000
                )
                : Voicechat.MESSAGES.text("gui.admin.cooldown.detail_disabled", "sem limite global");
        inv.setItem(30, createButton(Material.CLOCK,
                Voicechat.MESSAGES.text("gui.admin.cooldown.name", "&6* &fCooldown de Voz"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.admin.cooldown.line1", "&7 > &fLimita quanto tempo continuo cada"),
                Voicechat.MESSAGES.text("gui.admin.cooldown.line2", "&7   &fjogador pode falar antes de esperar."),
                Voicechat.MESSAGES.text("gui.admin.cooldown.line3", "&7 > &fAjuda a evitar monopolio de voz"),
                Voicechat.MESSAGES.text("gui.admin.cooldown.line4", "&7   &fe spam em canais de proximidade."),
                "",
                Voicechat.MESSAGES.format("gui.admin.cooldown.status_line", "&7 > &eStatus&8 - %s", cooldownStatus),
                Voicechat.MESSAGES.format("gui.admin.cooldown.detail_line", "&7 > &eConfig atual&8 - &f%s", cooldownDetail),
                "",
                S,
                Voicechat.MESSAGES.text("gui.admin.cooldown.action", "&a > &eClique para configurar")));

        boolean viewingParticles = Voicechat.zoneParticleVisualizer != null && Voicechat.zoneParticleVisualizer.isViewing(player.getUniqueId());
        String particleStatus = viewingParticles
                ? Voicechat.MESSAGES.text("gui.admin.particles.status_enabled", "&aAtivado")
                : Voicechat.MESSAGES.text("gui.admin.particles.status_disabled", "&cDesativado");
        inv.setItem(32, createButton(viewingParticles ? Material.GLOWSTONE : Material.REDSTONE_LAMP,
                Voicechat.MESSAGES.text("gui.admin.particles.name", "&a* &fVisualizar Zonas"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.admin.particles.line1", "&7 > &fMostra bordas das zonas com particulas"),
                Voicechat.MESSAGES.text("gui.admin.particles.line2", "&7   &fapenas para voce no mundo."),
                Voicechat.MESSAGES.text("gui.admin.particles.line3", "&7 > &fServe para revisar limites antes de"),
                Voicechat.MESSAGES.text("gui.admin.particles.line4", "&7   &ftestar regras de voz ou stage."),
                "",
                Voicechat.MESSAGES.format("gui.admin.particles.status_line", "&7 > &eStatus&8 - %s", particleStatus),
                Voicechat.MESSAGES.text("gui.admin.particles.scope", "&7 > &eEscopo&8 - &fsomente sua visao"),
                "",
                S,
                Voicechat.MESSAGES.text(
                        viewingParticles ? "gui.admin.particles.disable_action" : "gui.admin.particles.enable_action",
                        viewingParticles ? "&a > &eClique para desativar" : "&a > &eClique para ativar"
                )));

        inv.setItem(34, createButton(Material.EMERALD,
                Voicechat.MESSAGES.text("gui.admin.reload.name", "&a* &fRecarregar Tudo"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.admin.reload.line1", "&7 > &fReler configuracoes, mensagens e"),
                Voicechat.MESSAGES.text("gui.admin.reload.line2", "&7   &farquivos persistidos do plugin."),
                Voicechat.MESSAGES.text("gui.admin.reload.line3", "&7 > &fUse depois de editar arquivos ou"),
                Voicechat.MESSAGES.text("gui.admin.reload.line4", "&7   &fquando quiser sincronizar tudo."),
                "",
                Voicechat.MESSAGES.text("gui.admin.reload.scope", "&7 > &eInclui&8 - &fzonas, ranges, globais, mensagens e config"),
                Voicechat.MESSAGES.text("gui.admin.reload.effect", "&7 > &eAplica&8 - &freinicio do servidor de voz e reconexao"),
                "",
                S,
                Voicechat.MESSAGES.text("gui.admin.reload.action", "&a > &eClique para recarregar")));

        player.openInventory(inv);
    }

    private static ItemStack createButton(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createGlass() {
        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }
}

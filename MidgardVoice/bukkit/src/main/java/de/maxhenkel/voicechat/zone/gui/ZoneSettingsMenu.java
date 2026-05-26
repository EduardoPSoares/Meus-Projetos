package de.maxhenkel.voicechat.zone.gui;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.util.MessageFormatUtil;
import de.maxhenkel.voicechat.zone.RestrictedZone;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZoneSettingsMenu {

    private static final String S = ChatColor.DARK_GRAY + "-------------------------";

    public static String getTitlePrefix() {
        return Voicechat.MESSAGES.gui_zona_prefixo;
    }

    public static void open(Player player, RestrictedZone zone) {
        Inventory inv = Bukkit.createInventory(null, 54, getTitlePrefix() + zone.getName());

        ItemStack glass = createGlass();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, glass);
            inv.setItem(row * 9 + 8, glass);
        }

        ItemStack zoneInfo = new ItemStack(Material.FILLED_MAP);
        ItemMeta infoMeta = zoneInfo.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(Voicechat.MESSAGES.format("gui.zone_settings.info_name", "&d&l* &6%s", zone.getName()));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(S);
            lore.add("");
            lore.add(String.format(Voicechat.MESSAGES.gui_zona_mundo, zone.getWorld()));
            lore.add(String.format(Voicechat.MESSAGES.gui_zona_de, zone.getMinX() + ", " + zone.getMinY() + ", " + zone.getMinZ()));
            lore.add(String.format(Voicechat.MESSAGES.gui_zona_ate, zone.getMaxX() + ", " + zone.getMaxY() + ", " + zone.getMaxZ()));
            lore.add("");
            lore.add(Voicechat.MESSAGES.text("gui.zone_settings.priority", "&7 > &ePrioridade&8 - &bAutomatica"));
            lore.add(Voicechat.MESSAGES.text("gui.zone_settings.priority_line1", "&7 > &fSe existir sub-regiao dentro desta area,"));
            lore.add(Voicechat.MESSAGES.text("gui.zone_settings.priority_line2", "&7   &fa menor zona sempre prevalece."));
            if (zone.isTemporary()) {
                long remaining = zone.getExpiresAt() - System.currentTimeMillis();
                lore.add("");
                lore.add(S);
                lore.add("");
                lore.add(remaining > 0
                        ? String.format(Voicechat.MESSAGES.gui_zona_tempo_restante, MessageFormatUtil.duration(remaining))
                        : Voicechat.MESSAGES.gui_zona_expirada);
            }
            lore.add("");
            lore.add(S);
            lore.add(Voicechat.MESSAGES.text("gui.zone_settings.info_footer", "&7 > &fAbaixo ficam as regras especificas desta zona."));
            infoMeta.setLore(lore);
            zoneInfo.setItemMeta(infoMeta);
        }
        inv.setItem(4, zoneInfo);

        ItemStack voiceToggle;
        if (zone.isVoiceEnabled()) {
            voiceToggle = createItem(Material.LIME_WOOL,
                    Voicechat.MESSAGES.gui_zona_voz_ativada_titulo,
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.zone_settings.voice_enabled.line1", "&7 > &fJogadores desta zona podem falar por padrao."),
                    Voicechat.MESSAGES.text("gui.zone_settings.voice_enabled.line2", "&7 > &fMutados, stage mode e listen only ainda"),
                    Voicechat.MESSAGES.text("gui.zone_settings.voice_enabled.line3", "&7   &fpodem impedir a transmissao."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.zone_settings.voice_enabled.action", "&a > &eClique para desativar"));
        } else {
            voiceToggle = createItem(Material.RED_WOOL,
                    Voicechat.MESSAGES.gui_zona_voz_desativada_titulo,
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.zone_settings.voice_disabled.line1", "&7 > &fA voz esta bloqueada para quem entrar"),
                    Voicechat.MESSAGES.text("gui.zone_settings.voice_disabled.line2", "&7   &fnesta zona, salvo excecoes permitidas."),
                    Voicechat.MESSAGES.text("gui.zone_settings.voice_disabled.line3", "&7 > &fUse para areas silenciosas ou controladas."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.zone_settings.voice_disabled.action", "&a > &eClique para ativar"));
        }
        inv.setItem(20, voiceToggle);

        inv.setItem(22, createItem(Material.PLAYER_HEAD,
                Voicechat.MESSAGES.gui_zona_jogadores_permitidos,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.zone_settings.allowed.line1", "&7 > &fDefine excecoes de fala quando a zona"),
                Voicechat.MESSAGES.text("gui.zone_settings.allowed.line2", "&7   &festiver bloqueada para o publico geral."),
                Voicechat.MESSAGES.format("gui.zone_settings.allowed.current", "&7 > &eAtualmente&8 - &f%s", zone.getAllowedPlayers().size()),
                "",
                S,
                Voicechat.MESSAGES.text("gui.zone_settings.allowed.action", "&a > &eClique para gerenciar")));

        inv.setItem(24, createItem(Material.SKELETON_SKULL,
                Voicechat.MESSAGES.gui_zona_jogadores_mutados,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.zone_settings.muted.line1", "&7 > &fLista bloqueios forcados de voz dentro"),
                Voicechat.MESSAGES.text("gui.zone_settings.muted.line2", "&7   &fdesta zona, independentemente do resto."),
                Voicechat.MESSAGES.format("gui.zone_settings.muted.current", "&7 > &eAtualmente&8 - &f%s", zone.getMutedPlayers().size()),
                "",
                S,
                Voicechat.MESSAGES.text("gui.zone_settings.muted.action", "&a > &eClique para gerenciar")));

        String rangeStr = zone.hasCustomRange()
                ? MessageFormatUtil.blocks((int) zone.getCustomRange())
                : Voicechat.MESSAGES.gui_zona_range_padrao_valor;
        inv.setItem(29, createItem(Material.SPYGLASS,
                Voicechat.MESSAGES.gui_zona_range_titulo,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.zone_settings.range.line1", "&7 > &fAplica um alcance proprio de voz nesta zona."),
                Voicechat.MESSAGES.text("gui.zone_settings.range.line2", "&7 > &fSe ficar no padrao, o sistema usa o valor"),
                Voicechat.MESSAGES.text("gui.zone_settings.range.line3", "&7   &fglobal do servidor para calculo base."),
                "",
                Voicechat.MESSAGES.format("gui.zone_settings.range.current", "&7 > &eAtual&8 - &f%s", rangeStr),
                "",
                S,
                Voicechat.MESSAGES.text("gui.zone_settings.range.action", "&a > &eClique para alternar")));

        inv.setItem(31, createItem(Material.BELL,
                Voicechat.MESSAGES.gui_zona_multiplicador_titulo,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.zone_settings.multiplier.line1", "&7 > &fMultiplica o alcance final da voz dentro"),
                Voicechat.MESSAGES.text("gui.zone_settings.multiplier.line2", "&7   &fdesta zona sem trocar o range base."),
                Voicechat.MESSAGES.format("gui.zone_settings.multiplier.current", "&7 > &eAtual&8 - &f%s", String.format("%.1fx", zone.getRangeMultiplier())),
                "",
                S,
                Voicechat.MESSAGES.text("gui.zone_settings.multiplier.action", "&a > &eClique para alternar")));

        ItemStack listenOnlyItem;
        if (zone.isListenOnly()) {
            listenOnlyItem = createItem(Material.ECHO_SHARD,
                    Voicechat.MESSAGES.gui_zona_escuta_ativada,
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.zone_settings.listen_enabled.line1", "&7 > &fQuem entra nesta area escuta normalmente,"),
                    Voicechat.MESSAGES.text("gui.zone_settings.listen_enabled.line2", "&7   &fmas nao consegue transmitir voz."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.zone_settings.listen_enabled.action", "&a > &eClique para desativar"));
        } else {
            listenOnlyItem = createItem(Material.NOTE_BLOCK,
                    Voicechat.MESSAGES.gui_zona_escuta_desativada,
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.zone_settings.listen_disabled.line1", "&7 > &fA zona nao esta em modo somente escuta."),
                    Voicechat.MESSAGES.text("gui.zone_settings.listen_disabled.line2", "&7 > &fJogadores seguem as demais regras normais"),
                    Voicechat.MESSAGES.text("gui.zone_settings.listen_disabled.line3", "&7   &fde voz, mute e permissao."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.zone_settings.listen_disabled.action", "&a > &eClique para ativar"));
        }
        inv.setItem(33, listenOnlyItem);

        ItemStack stageItem;
        if (zone.isStageMode()) {
            stageItem = createItem(Material.DIAMOND_BLOCK,
                    Voicechat.MESSAGES.gui_zona_stage_ativado,
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.zone_settings.stage_enabled.line1", "&7 > &fSomente speakers podem transmitir"),
                    Voicechat.MESSAGES.text("gui.zone_settings.stage_enabled.line2", "&7   &fvoz nesta zona enquanto estiver ativo."),
                    Voicechat.MESSAGES.format("gui.zone_settings.stage_enabled.count", "&7 > &eSpeakers&8 - &f%s", zone.getSpeakers().size()),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.zone_settings.stage_enabled.action", "&a > &eClique para desativar"));
        } else {
            stageItem = createItem(Material.IRON_BLOCK,
                    Voicechat.MESSAGES.gui_zona_stage_desativado,
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.zone_settings.stage_disabled.line1", "&7 > &fA zona nao exige lista de speakers."),
                    Voicechat.MESSAGES.text("gui.zone_settings.stage_disabled.line2", "&7 > &fAtive quando quiser transformar a area"),
                    Voicechat.MESSAGES.text("gui.zone_settings.stage_disabled.line3", "&7   &fem palco, plateia ou auditorio."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.zone_settings.stage_disabled.action", "&a > &eClique para ativar"));
        }
        inv.setItem(38, stageItem);

        if (zone.isStageMode()) {
            inv.setItem(39, createItem(Material.PLAYER_HEAD,
                    Voicechat.MESSAGES.text("gui.zone_settings.speakers.name", "&6* &fGerenciar Speakers"),
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.zone_settings.speakers.line1", "&7 > &fAbre a lista de quem pode falar"),
                    Voicechat.MESSAGES.text("gui.zone_settings.speakers.line2", "&7   &fenquanto o stage mode estiver ativo."),
                    Voicechat.MESSAGES.format("gui.zone_settings.speakers.current", "&7 > &eAtualmente&8 - &f%s", zone.getSpeakers().size()),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.zone_settings.speakers.action", "&a > &eClique para gerenciar")));
        }

        String falaStr = zone.getZoneCooldownMaxTalkTimeSec() > 0
                ? MessageFormatUtil.seconds(zone.getZoneCooldownMaxTalkTimeSec())
                : Voicechat.MESSAGES.gui_zona_cooldown_desativado;
        String esperaStr = zone.getZoneCooldownSec() > 0
                ? MessageFormatUtil.seconds(zone.getZoneCooldownSec())
                : Voicechat.MESSAGES.gui_zona_cooldown_desativado;
        inv.setItem(40, createItem(zone.hasZoneCooldown() ? Material.CLOCK : Material.GRAY_DYE,
                Voicechat.MESSAGES.gui_zona_cooldown_titulo,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.zone_settings.cooldown.line1", "&7 > &fAplica limite de fala somente dentro"),
                Voicechat.MESSAGES.text("gui.zone_settings.cooldown.line2", "&7   &fdesta zona e substitui o global aqui."),
                "",
                String.format(Voicechat.MESSAGES.gui_zona_cooldown_fala, falaStr),
                String.format(Voicechat.MESSAGES.gui_zona_cooldown_espera, esperaStr),
                "",
                S,
                Voicechat.MESSAGES.gui_zona_cooldown_clique,
                Voicechat.MESSAGES.text("gui.zone_settings.cooldown.shift_action", "&e > &fShift + clique para valor customizado")));

        inv.setItem(49, createItem(Material.ENDER_PEARL,
                Voicechat.MESSAGES.text("gui.zone_settings.teleport.name", "&b* &fTeleportar para a Zona"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.zone_settings.teleport.line1", "&7 > &fLeva voce para um ponto seguro dentro"),
                Voicechat.MESSAGES.text("gui.zone_settings.teleport.line2", "&7   &fou acima do centro desta regiao."),
                Voicechat.MESSAGES.text("gui.zone_settings.teleport.line3", "&7 > &fUse para inspecionar a area rapidamente."),
                "",
                S,
                Voicechat.MESSAGES.text("gui.zone_settings.teleport.action", "&a > &eClique para teleportar")));

        boolean viewing = Voicechat.zoneParticleVisualizer != null
                && Voicechat.zoneParticleVisualizer.isViewingZone(player.getUniqueId(), zone.getName());
        inv.setItem(42, createItem(viewing ? Material.GLOWSTONE : Material.REDSTONE_LAMP,
                Voicechat.MESSAGES.text("gui.zone_settings.particles.name", "&a* &fVisualizar Bordas"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.zone_settings.particles.line1", "&7 > &fMostra o contorno da zona com particulas"),
                Voicechat.MESSAGES.text("gui.zone_settings.particles.line2", "&7   &fvisiveis somente para voce."),
                Voicechat.MESSAGES.format(
                        "gui.zone_settings.particles.status",
                        "&7 > &eStatus&8 - %s",
                        viewing
                                ? Voicechat.MESSAGES.text("gui.zone_settings.particles.enabled", "&aAtivado")
                                : Voicechat.MESSAGES.text("gui.zone_settings.particles.disabled", "&cDesativado")
                ),
                "",
                S,
                Voicechat.MESSAGES.text(
                        viewing ? "gui.zone_settings.particles.disable_action" : "gui.zone_settings.particles.enable_action",
                        viewing ? "&a > &eClique para desativar" : "&a > &eClique para ativar"
                )));

        inv.setItem(45, createItem(Material.ARROW,
                Voicechat.MESSAGES.text("gui.zone_settings.back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.text("gui.zone_settings.back_lore", "&7 > &fRetorna para a lista de zonas")));

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
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

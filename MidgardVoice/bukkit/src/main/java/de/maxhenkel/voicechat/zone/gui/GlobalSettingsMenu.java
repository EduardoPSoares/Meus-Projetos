package de.maxhenkel.voicechat.zone.gui;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.util.MessageFormatUtil;
import de.maxhenkel.voicechat.voice.common.Utils;
import de.maxhenkel.voicechat.zone.GlobalZoneSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class GlobalSettingsMenu {

    private static final String S = ChatColor.DARK_GRAY + "-------------------------";

    public static String getTitle() {
        return Voicechat.MESSAGES.text("gui.global_settings.title", "&5Regiao Global");
    }

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, getTitle());
        GlobalZoneSettings g = Voicechat.globalZoneSettings;

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

        ItemStack info = new ItemStack(Material.FILLED_MAP);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(Voicechat.MESSAGES.text("gui.global_settings.info_name", "&d&l* Regiao Global"));
            infoMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.global_settings.scope", "&7 > &eEscopo&8 - &fServidor inteiro"),
                    Voicechat.MESSAGES.text("gui.global_settings.line1", "&7 > &fEstas regras valem fora de sub-zonas."),
                    Voicechat.MESSAGES.text("gui.global_settings.line2", "&7 > &fQuando o jogador entra em uma zona"),
                    Voicechat.MESSAGES.text("gui.global_settings.line3", "&7   &frestrita, a configuracao local assume."),
                    "",
                    Voicechat.MESSAGES.format("gui.global_settings.allowed_count", "&7 > &ePermitidos&8 - &f%s", g.getAllowedPlayers().size()),
                    Voicechat.MESSAGES.format("gui.global_settings.muted_count", "&7 > &eMutados&8 - &f%s", g.getMutedPlayers().size()),
                    Voicechat.MESSAGES.format("gui.global_settings.speakers_count", "&7 > &eSpeakers&8 - &f%s", g.getSpeakers().size()),
                    "",
                    S
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        ItemStack voiceToggle;
        if (g.isVoiceEnabled()) {
            voiceToggle = createSimpleItem(Material.LIME_WOOL,
                    Voicechat.MESSAGES.gui_zona_voz_ativada_titulo,
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.global_settings.voice_enabled.line1", "&7 > &fA fala global esta liberada por padrao."),
                    Voicechat.MESSAGES.text("gui.global_settings.voice_enabled.line2", "&7 > &fBloqueios locais, mutes e stage mode"),
                    Voicechat.MESSAGES.text("gui.global_settings.voice_enabled.line3", "&7   &fainda podem impedir a transmissao."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.global_settings.voice_enabled.action", "&a > &eClique para desativar"));
        } else {
            voiceToggle = createSimpleItem(Material.RED_WOOL,
                    Voicechat.MESSAGES.gui_zona_voz_desativada_titulo,
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.global_settings.voice_disabled.line1", "&7 > &fA fala global esta bloqueada por padrao."),
                    Voicechat.MESSAGES.text("gui.global_settings.voice_disabled.line2", "&7 > &fSomente excecoes locais ou jogadores"),
                    Voicechat.MESSAGES.text("gui.global_settings.voice_disabled.line3", "&7   &fpermitidos poderao falar."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.global_settings.voice_disabled.action", "&a > &eClique para ativar"));
        }
        inv.setItem(20, voiceToggle);

        inv.setItem(22, createSimpleItem(Material.PLAYER_HEAD,
                Voicechat.MESSAGES.gui_zona_jogadores_permitidos,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.global_settings.allowed.line1", "&7 > &fLista quem pode falar mesmo se a voz"),
                Voicechat.MESSAGES.text("gui.global_settings.allowed.line2", "&7   &fglobal estiver bloqueada."),
                Voicechat.MESSAGES.format("gui.global_settings.allowed.current", "&7 > &eAtualmente&8 - &f%s", g.getAllowedPlayers().size()),
                "",
                S,
                Voicechat.MESSAGES.text("gui.global_settings.allowed.action", "&a > &eClique para gerenciar")));

        inv.setItem(24, createSimpleItem(Material.SKELETON_SKULL,
                Voicechat.MESSAGES.gui_zona_jogadores_mutados,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.global_settings.muted.line1", "&7 > &fLista quem fica impedido de falar na"),
                Voicechat.MESSAGES.text("gui.global_settings.muted.line2", "&7   &fregra global independentemente do resto."),
                Voicechat.MESSAGES.format("gui.global_settings.muted.current", "&7 > &eAtualmente&8 - &f%s", g.getMutedPlayers().size()),
                "",
                S,
                Voicechat.MESSAGES.text("gui.global_settings.muted.action", "&a > &eClique para gerenciar")));

        String rangeStr = MessageFormatUtil.blocks((int) Utils.getDefaultDistance());
        inv.setItem(29, createSimpleItem(Material.SPYGLASS,
                Voicechat.MESSAGES.gui_zona_range_titulo,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.global_settings.range.line1", "&7 > &fDefine o alcance base de audicao fora"),
                Voicechat.MESSAGES.text("gui.global_settings.range.line2", "&7   &fde zonas com regra propria."),
                Voicechat.MESSAGES.format("gui.global_settings.range.current", "&7 > &eAtual&8 - &f%s", rangeStr),
                "",
                S,
                Voicechat.MESSAGES.text("gui.global_settings.range.action", "&a > &eClique para alterar")));

        inv.setItem(31, createSimpleItem(Material.BELL,
                Voicechat.MESSAGES.gui_zona_multiplicador_titulo,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.global_settings.multiplier.line1", "&7 > &fMultiplica o range global sem alterar"),
                Voicechat.MESSAGES.text("gui.global_settings.multiplier.line2", "&7   &fo valor base configurado no servidor."),
                Voicechat.MESSAGES.format("gui.global_settings.multiplier.current", "&7 > &eAtual&8 - &f%s", String.format("%.1fx", g.getRangeMultiplier())),
                "",
                S,
                Voicechat.MESSAGES.text("gui.global_settings.multiplier.action", "&a > &eClique para alternar")));

        ItemStack listenOnlyItem;
        if (g.isListenOnly()) {
            listenOnlyItem = createSimpleItem(Material.ECHO_SHARD,
                    Voicechat.MESSAGES.gui_zona_escuta_ativada,
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.global_settings.listen_enabled.line1", "&7 > &fJogadores escutam normalmente, mas nao"),
                    Voicechat.MESSAGES.text("gui.global_settings.listen_enabled.line2", "&7   &ftransmitem pela regra global."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.global_settings.listen_enabled.action", "&a > &eClique para desativar"));
        } else {
            listenOnlyItem = createSimpleItem(Material.NOTE_BLOCK,
                    Voicechat.MESSAGES.gui_zona_escuta_desativada,
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.global_settings.listen_disabled.line1", "&7 > &fA transmissao global segue liberada"),
                    Voicechat.MESSAGES.text("gui.global_settings.listen_disabled.line2", "&7   &fconforme as demais regras ativas."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.global_settings.listen_disabled.action", "&a > &eClique para ativar"));
        }
        inv.setItem(33, listenOnlyItem);

        ItemStack stageItem;
        if (g.isStageMode()) {
            stageItem = createSimpleItem(Material.DIAMOND_BLOCK,
                    Voicechat.MESSAGES.gui_zona_stage_ativado,
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.global_settings.stage_enabled.line1", "&7 > &fApenas speakers globais conseguem"),
                    Voicechat.MESSAGES.text("gui.global_settings.stage_enabled.line2", "&7   &ftransmitir fora das sub-zonas."),
                    Voicechat.MESSAGES.format("gui.global_settings.stage_enabled.count", "&7 > &eSpeakers&8 - &f%s", g.getSpeakers().size()),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.global_settings.stage_enabled.action", "&a > &eClique para desativar"));
        } else {
            stageItem = createSimpleItem(Material.IRON_BLOCK,
                    Voicechat.MESSAGES.gui_zona_stage_desativado,
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.global_settings.stage_disabled.line1", "&7 > &fO servidor inteiro nao exige speaker"),
                    Voicechat.MESSAGES.text("gui.global_settings.stage_disabled.line2", "&7   &fpara transmitir fora das sub-zonas."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.global_settings.stage_disabled.action", "&a > &eClique para ativar"));
        }
        inv.setItem(38, stageItem);

        if (g.isStageMode()) {
            inv.setItem(39, createSimpleItem(Material.PLAYER_HEAD,
                    Voicechat.MESSAGES.text("gui.global_settings.speakers.name", "&6* &fGerenciar Speakers"),
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.global_settings.speakers.line1", "&7 > &fAbre a lista de quem pode falar"),
                    Voicechat.MESSAGES.text("gui.global_settings.speakers.line2", "&7   &fcom o stage global ativado."),
                    Voicechat.MESSAGES.format("gui.global_settings.speakers.current", "&7 > &eAtualmente&8 - &f%s", g.getSpeakers().size()),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.global_settings.speakers.action", "&a > &eClique para gerenciar")));
        }

        String cooldownStr;
        if (g.getGlobalCooldownMaxTalkTimeSec() > 0 && g.getGlobalCooldownSec() > 0) {
            long talkSec = g.getGlobalCooldownMaxTalkTimeSec();
            long cdSec = g.getGlobalCooldownSec();
            cooldownStr = MessageFormatUtil.cooldownPair(talkSec, cdSec);
        } else {
            cooldownStr = Voicechat.MESSAGES.text("gui.global_settings.cooldown.disabled", "Desativado");
        }
        boolean hasCooldown = g.getGlobalCooldownMaxTalkTimeSec() > 0 && g.getGlobalCooldownSec() > 0;
        inv.setItem(40, createSimpleItem(hasCooldown ? Material.CLOCK : Material.GRAY_DYE,
                Voicechat.MESSAGES.gui_zona_cooldown_titulo,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.global_settings.cooldown.line1", "&7 > &fAplica limite de fala fora das zonas"),
                Voicechat.MESSAGES.text("gui.global_settings.cooldown.line2", "&7   &fque tenham cooldown proprio."),
                Voicechat.MESSAGES.format("gui.global_settings.cooldown.current", "&7 > &eAtual&8 - &f%s", cooldownStr),
                "",
                S,
                Voicechat.MESSAGES.text("gui.global_settings.cooldown.action", "&a > &eClique para alterar")));

        inv.setItem(45, createSimpleItem(Material.ARROW,
                Voicechat.MESSAGES.text("gui.global_settings.back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.text("gui.global_settings.back_lore", "&7 > &fRetorna para a lista de zonas")));

        player.openInventory(inv);
    }

    private static ItemStack createSimpleItem(Material material, String name, String... lore) {
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

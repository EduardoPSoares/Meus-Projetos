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

public class CooldownMenu {

    private static final long[] TALK_VALUES = {0, 10, 15, 30, 60, 120, 300};
    private static final long[] COOLDOWN_VALUES = {0, 5, 10, 15, 30, 60, 120};
    private static final String S = ChatColor.DARK_GRAY + "-------------------------";

    public static String getTitle() {
        return Voicechat.MESSAGES.text("gui.cooldown.title", "&5Cooldown de Voz");
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

        boolean enabled = Voicechat.voiceCooldownManager.isEnabled();
        long maxTalkSec = Voicechat.voiceCooldownManager.getMaxTalkTimeMs() / 1000;
        long cooldownSec = Voicechat.voiceCooldownManager.getCooldownMs() / 1000;

        String statusStr = enabled
                ? Voicechat.MESSAGES.text("gui.cooldown.status_enabled", "&aAtivado")
                : Voicechat.MESSAGES.text("gui.cooldown.status_disabled", "&cDesativado");
        String talkLabel = maxTalkSec > 0
                ? Voicechat.MESSAGES.format("gui.cooldown.seconds", "%ss", maxTalkSec)
                : Voicechat.MESSAGES.text("gui.cooldown.unlimited", "Ilimitado");
        String cdLabel = cooldownSec > 0
                ? Voicechat.MESSAGES.format("gui.cooldown.seconds", "%ss", cooldownSec)
                : Voicechat.MESSAGES.text("gui.cooldown.no_wait", "Sem espera");

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(Voicechat.MESSAGES.text("gui.cooldown.info_name", "&d&l* Cooldown de Voz"));
            infoMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.cooldown.info.line1", "&7 > &fControla fala continua e pausa forcada."),
                    Voicechat.MESSAGES.text("gui.cooldown.info.line2", "&7 > &fQuando o jogador excede o tempo de fala,"),
                    Voicechat.MESSAGES.text("gui.cooldown.info.line3", "&7   &fele precisa esperar para voltar a transmitir."),
                    "",
                    Voicechat.MESSAGES.text("gui.cooldown.info.line4", "&7 > &fO sistema global vale fora das zonas que"),
                    Voicechat.MESSAGES.text("gui.cooldown.info.line5", "&7   &ftenham um cooldown proprio configurado."),
                    "",
                    S,
                    Voicechat.MESSAGES.format("gui.cooldown.info.status", "&7 > &eStatus&8 - %s", statusStr),
                    Voicechat.MESSAGES.format("gui.cooldown.info.talk", "&7 > &eFala&8 - &f%s", talkLabel),
                    Voicechat.MESSAGES.format("gui.cooldown.info.wait", "&7 > &eEspera&8 - &f%s", cdLabel)
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        if (enabled) {
            inv.setItem(10, createButton(Material.LIME_WOOL,
                    Voicechat.MESSAGES.text("gui.cooldown.toggle_enabled_name", "&a* &fCooldown Ativado"),
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.cooldown.toggle_enabled.line1", "&7 > &fA regra global de fala esta valendo"),
                    Voicechat.MESSAGES.text("gui.cooldown.toggle_enabled.line2", "&7   &fpara quem nao estiver em zona especial."),
                    Voicechat.MESSAGES.text("gui.cooldown.toggle_enabled.line3", "&7 > &fDesative somente se quiser liberar"),
                    Voicechat.MESSAGES.text("gui.cooldown.toggle_enabled.line4", "&7   &ffala continua sem nenhuma espera."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.cooldown.toggle_enabled.action", "&a > &eClique para desativar")));
        } else {
            inv.setItem(10, createButton(Material.RED_WOOL,
                    Voicechat.MESSAGES.text("gui.cooldown.toggle_disabled_name", "&c* &fCooldown Desativado"),
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.cooldown.toggle_disabled.line1", "&7 > &fNao existe limite global de fala."),
                    Voicechat.MESSAGES.text("gui.cooldown.toggle_disabled.line2", "&7 > &fAo ativar, o plugin usa uma base util"),
                    Voicechat.MESSAGES.text("gui.cooldown.toggle_disabled.line3", "&7   &fpara depois voce ajustar com cliques."),
                    "",
                    Voicechat.MESSAGES.text("gui.cooldown.toggle_disabled.preset", "&7 > &ePreset inicial&8 - &f30s fala / 10s espera"),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.cooldown.toggle_disabled.action", "&a > &eClique para ativar")));
        }

        inv.setItem(12, createButton(Material.CLOCK,
                Voicechat.MESSAGES.text("gui.cooldown.talk_name", "&6* &fTempo de Fala"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.cooldown.talk.line1", "&7 > &fDefine o teto de transmissao continua."),
                Voicechat.MESSAGES.text("gui.cooldown.talk.line2", "&7 > &fQuando o jogador fica em silencio por tempo"),
                Voicechat.MESSAGES.text("gui.cooldown.talk.line3", "&7   &fsuficiente, a contagem volta do inicio."),
                "",
                Voicechat.MESSAGES.format("gui.cooldown.talk.current", "&7 > &eAtual&8 - &f%s", talkLabel),
                "",
                S,
                Voicechat.MESSAGES.text("gui.cooldown.talk.action", "&a > &eClique para trocar o valor")));

        inv.setItem(14, createButton(Material.HOPPER,
                Voicechat.MESSAGES.text("gui.cooldown.wait_name", "&6* &fTempo de Espera"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.cooldown.wait.line1", "&7 > &fDefine a pausa obrigatoria depois"),
                Voicechat.MESSAGES.text("gui.cooldown.wait.line2", "&7   &fque o limite de fala e atingido."),
                Voicechat.MESSAGES.text("gui.cooldown.wait.line3", "&7 > &fUse valores maiores quando quiser"),
                Voicechat.MESSAGES.text("gui.cooldown.wait.line4", "&7   &fmais rotatividade entre jogadores."),
                "",
                Voicechat.MESSAGES.format("gui.cooldown.wait.current", "&7 > &eAtual&8 - &f%s", cdLabel),
                "",
                S,
                Voicechat.MESSAGES.text("gui.cooldown.wait.action", "&a > &eClique para trocar o valor")));

        inv.setItem(16, createButton(Material.BOOKSHELF,
                Voicechat.MESSAGES.text("gui.cooldown.summary_name", "&b* &fResumo Operacional"),
                "",
                S,
                "",
                Voicechat.MESSAGES.format("gui.cooldown.summary.status", "&7 > &eStatus&8 - %s", statusStr),
                Voicechat.MESSAGES.format("gui.cooldown.summary.talk", "&7 > &eFala continua&8 - &f%s", talkLabel),
                Voicechat.MESSAGES.format("gui.cooldown.summary.wait", "&7 > &ePausa forcada&8 - &f%s", cdLabel),
                "",
                Voicechat.MESSAGES.text(
                        enabled ? "gui.cooldown.summary.enabled_line" : "gui.cooldown.summary.disabled_line",
                        enabled
                                ? "&7 > &fApos falar demais, o jogador aguardara antes de voltar a transmitir."
                                : "&7 > &fSem limite global: a fala segue livre ate outra regra bloquear."
                ),
                "",
                S));

        inv.setItem(36, createButton(Material.ARROW,
                Voicechat.MESSAGES.text("gui.cooldown.back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.text("gui.cooldown.back_lore", "&7 > &fRetorna ao painel administrativo")));

        player.openInventory(inv);
    }

    public static long getNextTalkValue(long currentSec) {
        for (int i = 0; i < TALK_VALUES.length; i++) {
            if (TALK_VALUES[i] == currentSec) {
                return TALK_VALUES[(i + 1) % TALK_VALUES.length];
            }
        }
        return TALK_VALUES[0];
    }

    public static long getNextCooldownValue(long currentSec) {
        for (int i = 0; i < COOLDOWN_VALUES.length; i++) {
            if (COOLDOWN_VALUES[i] == currentSec) {
                return COOLDOWN_VALUES[(i + 1) % COOLDOWN_VALUES.length];
            }
        }
        return COOLDOWN_VALUES[0];
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

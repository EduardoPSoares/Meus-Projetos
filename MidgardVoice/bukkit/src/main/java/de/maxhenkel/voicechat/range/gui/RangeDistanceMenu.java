package de.maxhenkel.voicechat.range.gui;

import de.maxhenkel.voicechat.Voicechat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RangeDistanceMenu {

    private static final String S = ChatColor.DARK_GRAY + "-------------------------";
    private static final Map<UUID, UUID> selectedTargets = new ConcurrentHashMap<>();

    public static String getTitlePrefix() {
        return Voicechat.MESSAGES.gui_range_distancia_prefixo;
    }

    public static void open(Player player, UUID targetUuid) {
        selectedTargets.put(player.getUniqueId(), targetUuid);
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String targetName = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);
        Inventory inv = Bukkit.createInventory(null, 45, getTitlePrefix() + targetName);

        ItemStack glass = createGlass();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, glass);
        }
        for (int row = 1; row <= 3; row++) {
            inv.setItem(row * 9, glass);
            inv.setItem(row * 9 + 8, glass);
        }

        ItemStack playerInfo = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta infoMeta = (SkullMeta) playerInfo.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setOwningPlayer(target);
            infoMeta.setDisplayName(Voicechat.MESSAGES.format("gui.range.distance.player_name", "&d&l* &6%s", targetName));
            Float currentRange = Voicechat.playerRangeManager.getRange(targetUuid);
            float defaultRange = de.maxhenkel.voicechat.voice.common.Utils.getDefaultDistance();

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(S);
            lore.add("");
            lore.add(currentRange != null
                    ? String.format(Voicechat.MESSAGES.gui_range_distancia_atual, String.valueOf(currentRange))
                    : Voicechat.MESSAGES.gui_range_sem_custom_gui);
            lore.add(String.format(Voicechat.MESSAGES.gui_range_range_padrao, String.valueOf(defaultRange)));
            lore.add("");
            lore.add(Voicechat.MESSAGES.text("gui.range.distance.info_line1", "&7 > &fEscolha abaixo a distancia maxima em"));
            lore.add(Voicechat.MESSAGES.text("gui.range.distance.info_line2", "&7   &fque a voz deste jogador sera ouvida."));
            lore.add("");
            lore.add(S);
            infoMeta.setLore(lore);
            playerInfo.setItemMeta(infoMeta);
        }
        inv.setItem(4, playerInfo);

        int[][] distanceOptions = {
                {19, 48}, {20, 64}, {21, 96}, {22, 128}, {23, 200}, {24, 300}, {25, 500}
        };

        for (int[] option : distanceOptions) {
            int slot = option[0];
            int distance = option[1];

            Float currentRange = Voicechat.playerRangeManager.getRange(targetUuid);
            boolean isSelected = currentRange != null && currentRange == distance;

            ItemStack item = new ItemStack(isSelected ? Material.LIME_WOOL : Material.WHITE_WOOL);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(String.format(Voicechat.MESSAGES.gui_range_distancia_blocos, String.valueOf(distance)));
                meta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.format("gui.range.distance.option_line1", "&7 > &fDefine %s blocos como alcance maximo.", distance),
                        Voicechat.MESSAGES.text(
                                isSelected ? "gui.range.distance.option_selected" : "gui.range.distance.option_action",
                                isSelected ? "&7 > &fEste valor esta ativo agora." : "&7 > &fClique para aplicar este alcance."
                        ),
                        "",
                        S
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
        }

        inv.setItem(31, createItem(Material.BARRIER,
                Voicechat.MESSAGES.gui_range_remover,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.range.distance.remove_line1", "&7 > &fRemove o range customizado deste jogador."),
                Voicechat.MESSAGES.text("gui.range.distance.remove_line2", "&7 > &fDepois disso ele volta a usar o valor"),
                Voicechat.MESSAGES.text("gui.range.distance.remove_line3", "&7   &fpadrao do servidor."),
                "",
                S,
                Voicechat.MESSAGES.text("gui.range.distance.remove_action", "&c > &eClique para remover")));

        inv.setItem(36, createItem(Material.ARROW,
                Voicechat.MESSAGES.text("gui.range.distance.back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.text("gui.range.distance.back_lore", "&7 > &fRetorna para a lista de ranges")));

        player.openInventory(inv);
    }

    public static UUID getSelectedTarget(UUID playerUuid) {
        return selectedTargets.get(playerUuid);
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

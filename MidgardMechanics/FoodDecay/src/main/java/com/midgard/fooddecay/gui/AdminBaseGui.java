package com.midgard.fooddecay.gui;

import com.midgard.core.gui.GuiMenu;
import com.midgard.core.item.ItemBuilder;
import com.midgard.core.utils.MessageUtils;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for admin settings GUIs with common utility methods.
 */
public abstract class AdminBaseGui extends GuiMenu {

    protected final FoodDecayModule module;
    protected final FoodDecayConfig config;

    protected AdminBaseGui(String title, int rows, FoodDecayModule module) {
        super(sc(title), rows);
        this.module = module;
        this.config = module.getDecayConfig();
    }

    protected ItemStack toggle(Material mat, String name, boolean on, String... desc) {
        ItemBuilder b = new ItemBuilder(on ? mat : Material.GRAY_DYE)
                .name(sc(name + (on ? " &a✔" : " &c✘")));
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String d : desc) lore.add(sc(d));
        lore.add("");
        lore.add(sc(on ? "&a▸ Clique para desativar" : "&c▸ Clique para ativar"));
        b.lore(lore);
        if (on) b.glow();
        return b.build();
    }

    protected ItemStack val(Material mat, String name, Object current, String hint) {
        return new ItemBuilder(mat)
                .name(sc(name))
                .lore("", sc("&7Atual: &f" + current), "", sc(hint))
                .build();
    }

    protected ItemStack info(Material mat, String name, List<String> lines) {
        ItemBuilder b = new ItemBuilder(mat).name(sc(name));
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String line : lines) lore.add(sc(line));
        b.lore(lore);
        return b.build();
    }

    protected void back(Player player, int slot) {
        setItem(slot, new ItemBuilder(Material.ARROW)
                .name(sc("&f&l← Voltar"))
                .lore("", sc("&7Voltar ao menu principal"))
                .build(), e -> new AdminMainGui(module).open(player));
    }

    protected void close(Player player, int slot) {
        setItem(slot, new ItemBuilder(Material.BARRIER)
                .name(sc("&c&lFechar"))
                .build(), e -> player.closeInventory());
    }

    protected void editLong(Player player, String prompt, String path,
                            long min, Runnable reopen) {
        ChatInput.request(player, sc(prompt), input -> {
            try {
                long v = Long.parseLong(input.trim());
                if (v >= min) config.saveValue(path, v);
                else player.sendMessage(MessageUtils.toComponent(
                        sc("&cValor mínimo: " + min)));
            } catch (NumberFormatException ex) {
                player.sendMessage(MessageUtils.toComponent(sc("&cNúmero inválido!")));
            }
            reopen.run();
        }, reopen);
    }

    protected void editInt(Player player, String prompt, String path,
                           int min, Runnable reopen) {
        ChatInput.request(player, sc(prompt), input -> {
            try {
                int v = Integer.parseInt(input.trim());
                if (v >= min) config.saveValue(path, v);
                else player.sendMessage(MessageUtils.toComponent(
                        sc("&cValor mínimo: " + min)));
            } catch (NumberFormatException ex) {
                player.sendMessage(MessageUtils.toComponent(sc("&cNúmero inválido!")));
            }
            reopen.run();
        }, reopen);
    }

    protected void editDouble(Player player, String prompt, String path,
                              double min, Runnable reopen) {
        ChatInput.request(player, sc(prompt), input -> {
            try {
                double v = Double.parseDouble(input.trim());
                if (v >= min) config.saveValue(path, v);
                else player.sendMessage(MessageUtils.toComponent(
                        sc("&cValor mínimo: " + min)));
            } catch (NumberFormatException ex) {
                player.sendMessage(MessageUtils.toComponent(sc("&cNúmero inválido!")));
            }
            reopen.run();
        }, reopen);
    }
}

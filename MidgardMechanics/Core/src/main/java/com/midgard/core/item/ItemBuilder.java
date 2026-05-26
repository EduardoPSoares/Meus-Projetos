package com.midgard.core.item;

import com.midgard.core.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fluent item stack builder.
 */
public class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.meta = itemStack.getItemMeta();
        if (this.meta == null) {
            throw new IllegalArgumentException("Material " + material + " does not support ItemMeta");
        }
    }

    public ItemBuilder(ItemStack base) {
        this.itemStack = base.clone();
        this.meta = itemStack.getItemMeta();
        if (this.meta == null) {
            throw new IllegalArgumentException("ItemStack does not support ItemMeta");
        }
    }

    public ItemBuilder name(String name) {
        meta.displayName(MessageUtils.toComponent(name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        List<Component> lore = Arrays.stream(lines)
                .map(MessageUtils::toComponent)
                .collect(Collectors.toList());
        meta.lore(lore);
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        List<Component> lore = lines.stream()
                .map(MessageUtils::toComponent)
                .collect(Collectors.toList());
        meta.lore(lore);
        return this;
    }

    public ItemBuilder addLore(String... lines) {
        List<Component> existing = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        for (String line : lines) {
            existing.add(MessageUtils.toComponent(line));
        }
        meta.lore(existing);
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder hideFlags() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder glow() {
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder amount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder customModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }

    public <T, Z> ItemBuilder persistentData(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        meta.getPersistentDataContainer().set(key, type, value);
        return this;
    }

    public ItemBuilder leatherColor(Color color) {
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(color);
        }
        return this;
    }

    public ItemBuilder skullOwner(String playerName) {
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(playerName));
        }
        return this;
    }

    public ItemStack build() {
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    // --- Static helpers ---

    public static ItemStack placeholder(Material material) {
        return new ItemBuilder(material).name(" ").hideFlags().build();
    }

    public static ItemStack placeholder() {
        return placeholder(Material.BLACK_STAINED_GLASS_PANE);
    }
}

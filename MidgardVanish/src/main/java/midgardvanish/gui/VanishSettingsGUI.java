package midgardvanish.gui;

import midgardvanish.data.VanishSettingsManager;
import midgardvanish.data.VanishSettingsManager.VanishSetting;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.common.collect.ArrayListMultimap;

import java.util.List;
import java.util.Set;

public class VanishSettingsGUI {

    public static final String MENU_TITLE = "§8ᴄᴏɴꜰɪɢᴜʀᴀçõᴇs ᴅᴏ ᴠᴀɴɪsʜ";

    private final VanishSettingsManager settingsManager;

    public VanishSettingsGUI(VanishSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, MENU_TITLE);

        // Fill all with dark glass
        ItemStack darkGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, darkGlass);
        }

        // Decorative border with gray glass on edges
        ItemStack borderGlass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, borderGlass);
            inv.setItem(36 + i, borderGlass);
        }
        for (int row = 1; row <= 3; row++) {
            inv.setItem(row * 9, borderGlass);
            inv.setItem(row * 9 + 8, borderGlass);
        }

        // Header item
        ItemStack header = createItem(Material.ENDER_EYE, "§b§lᴄᴏɴꜰɪɢᴜʀᴀçõᴇs ᴅᴏ ᴠᴀɴɪsʜ");
        ItemMeta headerMeta = header.getItemMeta();
        headerMeta.setLore(List.of("§7ᴄᴏɴꜰɪɢᴜʀᴇ sᴇᴜs ᴀᴊᴜsᴛᴇs ᴅᴇ ᴠᴀɴɪsʜ"));
        header.setItemMeta(headerMeta);
        inv.setItem(4, header);

        Set<VanishSetting> enabled = settingsManager.getSettings(player.getUniqueId());

        // Settings in rows 2 and 3, centered
        // Row 2 (slots 10-16): first 5 settings centered at 11-15
        // Row 3 (slots 19-25): last 4 settings centered at 19-25
        int[] slotsRow1 = {11, 12, 13, 14, 15};
        int[] slotsRow2 = {20, 21, 23, 24};
        VanishSetting[] settings = VanishSetting.values();

        for (int i = 0; i < settings.length; i++) {
            VanishSetting setting = settings[i];
            boolean isEnabled = enabled.contains(setting);

            int slot;
            if (i < 5) {
                slot = slotsRow1[i];
            } else {
                slot = slotsRow2[i - 5];
            }

            Material icon = isEnabled ? setting.getIconEnabled() : setting.getIconDisabled();
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((isEnabled ? "§a§l" : "§c§l") + setting.getDisplayName());
            meta.setLore(List.of(
                    "",
                    setting.getDescription(),
                    "",
                    isEnabled ? "§a  ✔ ᴀᴛɪᴠᴀᴅᴏ" : "§c  ✘ ᴅᴇsᴀᴛɪᴠᴀᴅᴏ",
                    "",
                    "§eᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴀʟᴛᴇʀɴᴀʀ"
            ));
            if (isEnabled) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            }
            hideAllFlags(meta);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        }

        player.openInventory(inv);
    }

    public VanishSettingsManager getSettingsManager() {
        return settingsManager;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        hideAllFlags(meta);
        item.setItemMeta(meta);
        return item;
    }

    private void hideAllFlags(ItemMeta meta) {
        meta.addItemFlags(ItemFlag.values());
        meta.setAttributeModifiers(ArrayListMultimap.create());
    }
}

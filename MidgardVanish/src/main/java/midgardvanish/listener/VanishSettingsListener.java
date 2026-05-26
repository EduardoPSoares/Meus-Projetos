package midgardvanish.listener;

import midgardvanish.data.VanishSettingsManager;
import midgardvanish.data.VanishSettingsManager.VanishSetting;
import midgardvanish.gui.VanishSettingsGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class VanishSettingsListener implements Listener {

    private final VanishSettingsGUI gui;
    private final VanishSettingsManager settingsManager;

    public VanishSettingsListener(VanishSettingsGUI gui, VanishSettingsManager settingsManager) {
        this.gui = gui;
        this.settingsManager = settingsManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(VanishSettingsGUI.MENU_TITLE)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String displayName = meta.getDisplayName();

        // Direct click on setting icon (displayName has color prefix §a§l or §c§l)
        for (VanishSetting setting : VanishSetting.values()) {
            if (displayName.equals("§a§l" + setting.getDisplayName()) || displayName.equals("§c§l" + setting.getDisplayName())) {
                toggleAndRefresh(player, setting);
                return;
            }
        }
    }

    private void toggleAndRefresh(Player player, VanishSetting setting) {
        settingsManager.toggle(player.getUniqueId(), setting);
        settingsManager.save();

        boolean nowEnabled = settingsManager.isEnabled(player.getUniqueId(), setting);
        player.sendMessage(nowEnabled
                ? "§a" + setting.getDisplayName() + " §aᴀᴛɪᴠᴀᴅᴏ."
                : "§c" + setting.getDisplayName() + " §cᴅᴇsᴀᴛɪᴠᴀᴅᴏ.");

        gui.open(player);
    }
}

package midgardvanish.gui;

import midgardvanish.MidgardVanish;
import midgardvanish.data.ViewerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class ViewerMenuGUI {

    public static final String MENU_TITLE = "§8ᴠɪᴇᴡᴇʀs ᴅᴇ ";
    public static final String ADD_MENU_TITLE = "§8ᴀᴅɪᴄɪᴏɴᴀʀ ᴠɪᴇᴡᴇʀ ᴅᴇ ";
    private static final int ITEMS_PER_PAGE = 28;

    private final MidgardVanish plugin;
    private final ViewerDataManager viewerDataManager;
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, UUID> managingTarget = new HashMap<>();

    public ViewerMenuGUI(MidgardVanish plugin, ViewerDataManager viewerDataManager) {
        this.plugin = plugin;
        this.viewerDataManager = viewerDataManager;
    }

    public void openMainMenu(Player player, UUID vanishedUUID, int page) {
        managingTarget.put(player.getUniqueId(), vanishedUUID);

        Set<UUID> viewers = viewerDataManager.getViewers(vanishedUUID);
        List<UUID> viewerList = new ArrayList<>(viewers);
        viewerList.sort(Comparator.comparing(uuid -> {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            return op.getName() != null ? op.getName() : uuid.toString();
        }));

        int totalPages = Math.max(1, (int) Math.ceil((double) viewerList.size() / ITEMS_PER_PAGE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        playerPages.put(player.getUniqueId(), page);

        OfflinePlayer vanishedOffline = Bukkit.getOfflinePlayer(vanishedUUID);
        String vanishedName = vanishedOffline.getName() != null ? vanishedOffline.getName() : "???";

        String title = MENU_TITLE + "§e" + vanishedName + " §7(" + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill border with glass
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);
        for (int row = 1; row < 5; row++) {
            inv.setItem(row * 9, glass);
            inv.setItem(row * 9 + 8, glass);
        }

        // Fill viewer heads in the center area
        int[] contentSlots = getContentSlots();
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, viewerList.size());

        for (int i = start; i < end; i++) {
            UUID viewerUUID = viewerList.get(i);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(viewerUUID);
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "???";

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(offlinePlayer);
            meta.setDisplayName("§e" + name);

            boolean online = offlinePlayer.isOnline();
            meta.setLore(List.of(
                    "",
                    online ? "§aᴏɴʟɪɴᴇ" : "§cᴏꜰꜰʟɪɴᴇ",
                    "",
                    "§cᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ʀᴇᴍᴏᴠᴇʀ"
            ));
            skull.setItemMeta(meta);

            int slotIndex = i - start;
            if (slotIndex < contentSlots.length) {
                inv.setItem(contentSlots[slotIndex], skull);
            }
        }

        // Navigation and action buttons
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, "§aᴘáɢɪɴᴀ ᴀɴᴛᴇʀɪᴏʀ"));
        }

        inv.setItem(49, createItem(Material.EMERALD, "§aᴀᴅɪᴄɪᴏɴᴀʀ ᴠɪᴇᴡᴇʀ"));

        if (page < totalPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, "§aᴘʀóxɪᴍᴀ ᴘáɢɪɴᴀ"));
        }

        // Info item
        ItemStack info = createItem(Material.BOOK, "§eᴠɪᴇᴡᴇʀs ᴅᴇ §f" + vanishedName + "§7: §f" + viewers.size());
        inv.setItem(4, info);

        player.openInventory(inv);
    }

    public void openAddMenu(Player player, UUID vanishedUUID, int page) {
        managingTarget.put(player.getUniqueId(), vanishedUUID);

        // Show online players that are NOT already viewers of this vanished player
        Set<UUID> currentViewers = viewerDataManager.getViewers(vanishedUUID);
        List<Player> available = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!currentViewers.contains(online.getUniqueId())
                    && !online.getUniqueId().equals(vanishedUUID)
                    && !online.equals(player)) {
                available.add(online);
            }
        }
        available.sort(Comparator.comparing(Player::getName));

        int totalPages = Math.max(1, (int) Math.ceil((double) available.size() / ITEMS_PER_PAGE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        playerPages.put(player.getUniqueId(), page);

        OfflinePlayer vanishedOffline = Bukkit.getOfflinePlayer(vanishedUUID);
        String vanishedName = vanishedOffline.getName() != null ? vanishedOffline.getName() : "???";

        String title = ADD_MENU_TITLE + "§e" + vanishedName + " §7(" + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill border
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);
        for (int row = 1; row < 5; row++) {
            inv.setItem(row * 9, glass);
            inv.setItem(row * 9 + 8, glass);
        }

        int[] contentSlots = getContentSlots();
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, available.size());

        for (int i = start; i < end; i++) {
            Player target = available.get(i);

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(target);
            meta.setDisplayName("§e" + target.getName());
            meta.setLore(List.of(
                    "",
                    "§aᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ᴀᴅɪᴄɪᴏɴᴀʀ"
            ));
            skull.setItemMeta(meta);

            int slotIndex = i - start;
            if (slotIndex < contentSlots.length) {
                inv.setItem(contentSlots[slotIndex], skull);
            }
        }

        // Navigation
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, "§aᴘáɢɪɴᴀ ᴀɴᴛᴇʀɪᴏʀ"));
        }

        inv.setItem(49, createItem(Material.BARRIER, "§cᴠᴏʟᴛᴀʀ"));

        if (page < totalPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, "§aᴘʀóxɪᴍᴀ ᴘáɢɪɴᴀ"));
        }

        player.openInventory(inv);
    }

    public int getPlayerPage(UUID uuid) {
        return playerPages.getOrDefault(uuid, 0);
    }

    public UUID getManagingTarget(UUID uuid) {
        return managingTarget.get(uuid);
    }

    public void cleanup(UUID uuid) {
        playerPages.remove(uuid);
        managingTarget.remove(uuid);
    }

    private int[] getContentSlots() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }
}

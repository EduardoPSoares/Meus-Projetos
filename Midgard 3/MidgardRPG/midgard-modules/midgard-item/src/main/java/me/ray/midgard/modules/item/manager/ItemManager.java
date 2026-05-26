package me.ray.midgard.modules.item.manager;

import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.model.MidgardItemImpl;
import me.ray.midgard.modules.item.repository.ItemRepository;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gerencia o carregamento, registro e acesso aos itens do MidgardRPG.
 */
public class ItemManager {

    private final ItemModule module;
    private final Map<String, MidgardItem> itemMap;

    /**
     * Construtor do ItemManager.
     *
     * @param module Instância do módulo de itens.
     */
    public ItemManager(ItemModule module) {
        this.module = module;
        this.itemMap = new ConcurrentHashMap<>();
    }

    /**
     * Carrega todos os itens do banco de dados.
     */
    public void loadItems() {
        itemMap.clear();

        ItemRepository repository = module.getItemRepository();
        if (repository != null) {
            Map<String, ItemRepository.ItemData> dbItems = repository.loadAll();
            for (Map.Entry<String, ItemRepository.ItemData> entry : dbItems.entrySet()) {
                try {
                    MidgardItemImpl item = MidgardItemImpl.fromDatabase(
                        entry.getKey(), entry.getValue().categoryId(), entry.getValue().yamlData());
                    if (item != null) {
                        itemMap.put(entry.getKey(), item);
                    }
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao carregar item " + entry.getKey() + " do banco de dados", e);
                }
            }
        }

        MidgardLogger.info("Carregados " + itemMap.size() + " itens do banco de dados.");
        MidgardLogger.debug(DebugCategory.ITEMS, "Carregamento de itens concluído. Total: %d", itemMap.size());
    }

    /**
     * Recarrega um item específico a partir dos dados do banco (usado pelo sync).
     */
    public void reloadItemFromDb(String itemId, ItemRepository.ItemData data) {
        MidgardItemImpl item = MidgardItemImpl.fromDatabase(itemId, data.categoryId(), data.yamlData());
        if (item != null) {
            itemMap.put(itemId, item);
            MidgardLogger.debug(DebugCategory.ITEMS, "Item recarregado do banco: %s", itemId);
        }
    }

    /**
     * Registra um item manualmente.
     *
     * @param item Item a ser registrado.
     */
    public void registerItem(MidgardItem item) {
        itemMap.put(item.getId(), item);
    }

    /**
     * Remove um item do registro.
     *
     * @param id ID do item a ser removido.
     */
    public void unregisterItem(String id) {
        itemMap.remove(id);
    }

    /**
     * Obtém um item pelo ID.
     *
     * @param id ID do item.
     * @return O item correspondente ou null se não encontrado.
     */
    public MidgardItem getMidgardItem(String id) {
        return itemMap.get(id);
    }

    /**
     * Obtém um item pelo ID (alias para getMidgardItem).
     *
     * @param id ID do item.
     * @return O item correspondente ou null se não encontrado.
     */
    public MidgardItem getItem(String id) {
        return getMidgardItem(id);
    }

    /**
     * Obtém o ID do item a partir de um ItemStack.
     *
     * @param item ItemStack a ser verificado.
     * @return O ID do item ou null se não for um item do MidgardRPG.
     */
    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) { return null; }
        ItemMeta meta = item.getItemMeta();
        NamespacedKey idKey = me.ray.midgard.modules.item.utils.ItemPDC.key("midgard_id");
        return meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    /**
     * Obtém uma lista com todos os IDs de itens registrados.
     *
     * @return Lista de IDs.
     */
    public List<String> getItemIds() {
        return new ArrayList<>(itemMap.keySet());
    }

    public ItemStack getItemStack(String id) {
        MidgardItem item = getMidgardItem(id);
        return item != null ? item.build() : null;
    }
    
    public List<MidgardItem> getItemsByCategory(String categoryId) {
        return itemMap.values().stream()
                .filter(item -> item.getCategoryId().equalsIgnoreCase(categoryId))
                .collect(Collectors.toList());
    }

    public void updateAllOnlinePlayers() {
        // Create a queue of players to update
        final java.util.Queue<org.bukkit.entity.Player> playerQueue = new java.util.LinkedList<>(module.getPlugin().getServer().getOnlinePlayers());
        
        if (playerQueue.isEmpty()) {
            return;
        }

        module.getPlugin().getLogger().info("Iniciando atualização de itens para " + playerQueue.size() + " jogadores...");

        // Schedule a repeating task to process the queue
        new Runnable() {
            private org.bukkit.scheduler.BukkitTask task;

            public void start() {
                task = me.ray.midgard.core.utils.Task.syncTimer(this, 1L, 1L);
            }

            @Override
            public void run() {
                // Process up to 2 players per tick to avoid lag
                for (int i = 0; i < 2; i++) {
                    if (playerQueue.isEmpty()) {
                        module.getPlugin().getLogger().info("Atualização de itens concluída.");
                        if (task != null) {
                            task.cancel();
                        }
                        return;
                    }

                    org.bukkit.entity.Player player = playerQueue.poll();
                    if (player != null && player.isOnline()) {
                        updateInventory(player);
                    }
                }
            }
        }.start();
    }

    public void updateInventory(org.bukkit.entity.Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean updated = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || !item.hasItemMeta()) { continue; }

            ItemStack newItem = updateItem(item);
            if (newItem != null) {
                contents[i] = newItem;
                updated = true;
            }
        }

        if (updated) {
            player.getInventory().setContents(contents);
            MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("item.common.updated_to_latest"));
        }
    }

    public ItemStack updateItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) { return null; }

        NamespacedKey idKey = me.ray.midgard.modules.item.utils.ItemPDC.key("midgard_id");
        NamespacedKey revKey = me.ray.midgard.modules.item.utils.ItemPDC.key("midgard_revision");

        String id = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (id == null) { return null; }

        Integer currentRev = meta.getPersistentDataContainer().get(revKey, PersistentDataType.INTEGER);
        if (currentRev == null) { currentRev = 1; }

        MidgardItem template = getMidgardItem(id);
        if (template == null) { return null; }

        if (template.getRevisionId() > currentRev) {
            ItemStack newItem = template.build();
            newItem.setAmount(item.getAmount());
            ItemMeta newMeta = newItem.getItemMeta();
            boolean metaChanged = false;

            // Preserve Upgrades (respecting updater options)
            if (template.isKeepUpgrades() && me.ray.midgard.modules.item.utils.ItemPDC.has(meta, "midgard_upgrade_level")) {
                int level = me.ray.midgard.modules.item.utils.ItemPDC.getInt(meta, "midgard_upgrade_level");
                me.ray.midgard.modules.item.utils.ItemPDC.setInt(newMeta, "midgard_upgrade_level", level);
                metaChanged = true;
            }

            // Preserve Sockets (respecting updater options)
            if (template.isKeepGemStones() && me.ray.midgard.modules.item.utils.ItemPDC.has(meta, "midgard_sockets_data")) {
                String sockets = me.ray.midgard.modules.item.utils.ItemPDC.getString(meta, "midgard_sockets_data");
                me.ray.midgard.modules.item.utils.ItemPDC.setString(newMeta, "midgard_sockets_data", sockets);
                metaChanged = true;
            }

            // Preserve Enchantments (respecting updater options)
            if (template.isKeepEnchantments() && !item.getEnchantments().isEmpty()) {
                for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> ench : item.getEnchantments().entrySet()) {
                    newMeta.addEnchant(ench.getKey(), ench.getValue(), true);
                }
                metaChanged = true;
            }

            // Preserve Soulbind (respecting updater options)
            if (template.isKeepSoulbind() && me.ray.midgard.modules.item.utils.ItemPDC.has(meta, "midgard_soulbind")) {
                String soulbind = me.ray.midgard.modules.item.utils.ItemPDC.getString(meta, "midgard_soulbind");
                if (soulbind != null) {
                    me.ray.midgard.modules.item.utils.ItemPDC.setString(newMeta, "midgard_soulbind", soulbind);
                }
                metaChanged = true;
            }

            // RNG Stats: use the NEW template's freshly rolled values (from build()).
            // Only preserve old rolls for stats whose range did NOT change in the template.
            if (me.ray.midgard.modules.item.utils.ItemPDC.has(meta, "midgard_rng_rolled")) {
                java.util.Map<me.ray.midgard.modules.item.model.ItemStat, me.ray.midgard.modules.item.utils.StatRange> templateStats = template.getStats();
                for (me.ray.midgard.modules.item.model.ItemStat stat : me.ray.midgard.modules.item.model.ItemStat.values()) {
                    if (!me.ray.midgard.modules.item.utils.ItemPDC.hasStat(meta, stat)) { continue; }
                    double oldVal = me.ray.midgard.modules.item.utils.ItemPDC.getStat(meta, stat);
                    me.ray.midgard.modules.item.utils.StatRange range = templateStats.get(stat);
                    // Preserve old roll ONLY if the template still has the same stat with a range
                    // that contains the old value (meaning the range wasn't changed)
                    if (range != null && oldVal >= range.getMin() && oldVal <= range.getMax()) {
                        me.ray.midgard.modules.item.utils.ItemPDC.setStat(newMeta, stat, oldVal);
                    }
                    // Otherwise: the new item already has a fresh roll from build(), keep it
                }
                me.ray.midgard.modules.item.utils.ItemPDC.setString(newMeta, "midgard_rng_rolled", "true");
                metaChanged = true;
            }
            
            if (metaChanged) {
                // Apply meta once to read current stats, then rebuild lore
                newItem.setItemMeta(newMeta);
                
                // Re-read meta from item to avoid stale reference issues
                newMeta = newItem.getItemMeta();
                
                // Re-apply lore if we changed stats or sockets or upgrade level
                // We need to fetch the stats again from newItem (which now has the copied stats)
                java.util.Map<me.ray.midgard.modules.item.model.ItemStat, Double> statsMap = me.ray.midgard.modules.item.utils.ItemPDC.getStats(newItem);
                
                // Also get socket data from newItem
                me.ray.midgard.modules.item.socket.SocketData socketData = me.ray.midgard.modules.item.socket.SocketData.fromItem(newItem);
                
                java.util.List<net.kyori.adventure.text.Component> lore = me.ray.midgard.modules.item.utils.LoreFormatter.formatLore(template, socketData, statsMap);
                
                // Update display name for upgrade
                if (me.ray.midgard.modules.item.utils.ItemPDC.has(newMeta, "midgard_upgrade_level")) {
                     int level = me.ray.midgard.modules.item.utils.ItemPDC.getInt(newMeta, "midgard_upgrade_level");
                     if (level > 0) {
                         String displayName = template.getDisplayName() + " +" + level;
                         newMeta.displayName(me.ray.midgard.core.text.MessageUtils.parse(displayName));
                     }
                }
                
                newMeta.lore(lore);
                newItem.setItemMeta(newMeta);
            }

            return newItem;
        }

        return null;
    }

    public void saveItem(MidgardItem item) {
        item.save();
    }
}

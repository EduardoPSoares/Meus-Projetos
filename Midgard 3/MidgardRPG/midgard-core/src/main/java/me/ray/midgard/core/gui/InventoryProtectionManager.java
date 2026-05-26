package me.ray.midgard.core.gui;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.SerializationUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryProtectionManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, ItemStack[]> savedContents = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new ConcurrentHashMap<>();
    private final File storageFile;
    private YamlConfiguration storageConfig;

    public InventoryProtectionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "protected-inventories.yml");
        if (!storageFile.exists()) {
            try {
                storageFile.createNewFile();
            } catch (IOException e) {
                MidgardLogger.error("Erro ao criar arquivo de inventários protegidos", e);
            }
        }
        this.storageConfig = YamlConfiguration.loadConfiguration(storageFile);
    }

    /**
     * Protege o inventário do jogador, salvando seus itens e limpando-o.
     * Deve ser chamado ao abrir um menu protegido.
     */
    public void protect(Player player) {
        if (savedContents.containsKey(player.getUniqueId())) {
            return;
        }

        // Salva no disco primeiro para segurança contra crashes
        saveToDisk(player);

        // Salva na memória (clone arrays to prevent live-array mutation)
        savedContents.put(player.getUniqueId(), player.getInventory().getContents().clone());
        savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents().clone());

        // Limpa o inventário
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.updateInventory();
    }

    /**
     * Restaura o inventário do jogador.
     * Deve ser chamado ao fechar o menu protegido.
     */
    public void restore(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Se estiver na memória, restaura de lá (mais rápido)
        if (savedContents.containsKey(uuid)) {
            player.getInventory().setContents(savedContents.remove(uuid));
            player.getInventory().setArmorContents(savedArmor.remove(uuid));
            player.updateInventory();
            
            // Remove do disco pois já foi restaurado com sucesso
            removeFromDisk(uuid);
            return;
        }
        
        // Se não estiver na memória, tenta restaurar do disco (ex: entrou após crash)
        if (hasDataOnDisk(uuid)) {
            loadFromDiskAndRestore(player);
        }
    }

    public boolean isProtected(Player player) {
        return savedContents.containsKey(player.getUniqueId()) || hasDataOnDisk(player.getUniqueId());
    }

    private void saveToDisk(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            String contentsBase64 = SerializationUtils.toBase64(player.getInventory().getContents());
            String armorBase64 = SerializationUtils.toBase64(player.getInventory().getArmorContents());

            storageConfig.set(uuid + ".contents", contentsBase64);
            storageConfig.set(uuid + ".armor", armorBase64);
            storageConfig.save(storageFile);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao salvar inventário protegido para " + player.getName(), e);
        }
    }

    private void removeFromDisk(UUID uuid) {
        if (storageConfig.contains(uuid.toString())) {
            storageConfig.set(uuid.toString(), null);
            try {
                storageConfig.save(storageFile);
            } catch (IOException e) {
                MidgardLogger.error("Erro ao remover inventário protegido do disco", e);
            }
        }
    }

    private boolean hasDataOnDisk(UUID uuid) {
        return storageConfig.contains(uuid.toString());
    }

    private void loadFromDiskAndRestore(Player player) {
        String uuid = player.getUniqueId().toString();
        if (!storageConfig.contains(uuid)) { return; }

        try {
            String contentsBase64 = storageConfig.getString(uuid + ".contents");
            String armorBase64 = storageConfig.getString(uuid + ".armor");

            if (contentsBase64 != null) {
                ItemStack[] contents = (ItemStack[]) SerializationUtils.fromBase64(contentsBase64);
                player.getInventory().setContents(contents);
            }
            if (armorBase64 != null) {
                ItemStack[] armor = (ItemStack[]) SerializationUtils.fromBase64(armorBase64);
                player.getInventory().setArmorContents(armor);
            }

            player.updateInventory();
            removeFromDisk(player.getUniqueId());
            
            // Remove da memória se estiver lá por algum motivo inconsistente, para evitar duplicação
            savedContents.remove(player.getUniqueId());
            savedArmor.remove(player.getUniqueId());
            
        } catch (Exception e) {
            MidgardLogger.error("Erro ao restaurar inventário protegido do disco para " + player.getName(), e);
        }
    }

    // --- Listeners --- //

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            // Se o inventário estava protegido, restaura ao fechar.
            // Isso assume que o menu protegido é o que está sendo fechado.
            if (isProtected(player)) {
                restore(player);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (isProtected(event.getPlayer())) {
            restore(event.getPlayer());
        }
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Se o servidor caiu enquanto o player estava com menu aberto,
        // os itens estão no disco. Restaura agora.
        if (hasDataOnDisk(event.getPlayer().getUniqueId())) {
            loadFromDiskAndRestore(event.getPlayer());
            plugin.getLogger().info("Inventário protegido restaurado para " + event.getPlayer().getName() + " (Recuperação de Crash)");
        }
    }

    // Nota: PluginDisableEvent geralmente não é seguro para restaurar inventários se o plugin estiver desativando,
    // pois o servidor pode estar desligando e salvando players. 
    // Mas se for um reload, precisamos restaurar.
    // Se for shutdown, o onQuit/Kick de todos os players deve tratar, ou a persistência em disco salvará.
    
    public void shutdown() {
        // Tenta restaurar para todos online
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (isProtected(p)) {
                restore(p);
            }
        }
    }
}

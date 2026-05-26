package midgardvanish;

import midgardvanish.command.VanishCommand;
import midgardvanish.command.VanishListCommand;
import midgardvanish.command.VanishSettingsCommand;
import midgardvanish.command.VanishTpCommand;
import midgardvanish.command.VanishViewerCommand;
import midgardvanish.data.VanishDataManager;
import midgardvanish.data.VanishSettingsManager;
import midgardvanish.data.ViewerDataManager;
import midgardvanish.gui.VanishSettingsGUI;
import midgardvanish.gui.ViewerMenuGUI;
import midgardvanish.listener.PacketListener;
import midgardvanish.listener.VanishChatListener;
import midgardvanish.listener.VanishListener;
import midgardvanish.listener.VanishSettingsListener;
import midgardvanish.listener.ViewerMenuListener;
import midgardvanish.manager.VanishManager;
import midgardvanish.task.ActionBarTask;
import org.bukkit.plugin.java.JavaPlugin;

public class MidgardVanish extends JavaPlugin {

    private VanishManager vanishManager;
    private VanishDataManager dataManager;
    private ViewerDataManager viewerDataManager;
    private VanishSettingsManager settingsManager;
    private PacketListener packetListener;

    @Override
    public void onEnable() {
        dataManager = new VanishDataManager(this);
        viewerDataManager = new ViewerDataManager(this);
        settingsManager = new VanishSettingsManager(this);
        vanishManager = new VanishManager(this, dataManager, viewerDataManager, settingsManager);

        ViewerMenuGUI viewerMenuGUI = new ViewerMenuGUI(this, viewerDataManager);
        VanishSettingsGUI settingsGUI = new VanishSettingsGUI(settingsManager);

        getCommand("vanish").setExecutor(new VanishCommand(this, vanishManager, settingsGUI));
        getCommand("vanishlist").setExecutor(new VanishListCommand(vanishManager));
        getCommand("vanishtp").setExecutor(new VanishTpCommand(this, vanishManager));
        getCommand("vanishsettings").setExecutor(new VanishSettingsCommand(settingsGUI));
        VanishViewerCommand viewerCmd = new VanishViewerCommand(viewerMenuGUI, vanishManager);
        getCommand("vanishviewer").setExecutor(viewerCmd);
        getCommand("vanishviewer").setTabCompleter(viewerCmd);

        getServer().getPluginManager().registerEvents(new VanishListener(this, vanishManager), this);
        getServer().getPluginManager().registerEvents(new ViewerMenuListener(this, viewerDataManager, viewerMenuGUI, vanishManager), this);
        getServer().getPluginManager().registerEvents(new VanishSettingsListener(settingsGUI, settingsManager), this);
        packetListener = new PacketListener(this, vanishManager);
        vanishManager.setPacketListener(packetListener);
        getServer().getPluginManager().registerEvents(packetListener, this);

        // Register vanish chat if nChat is available
        if (getServer().getPluginManager().getPlugin("nChat") != null) {
            getServer().getPluginManager().registerEvents(new VanishChatListener(vanishManager), this);
            getLogger().info("nChat detectado! Chat de vanish ativado.");
        }

        new ActionBarTask(vanishManager).runTaskTimer(this, 0L, 20L);

        // PlugManX: reaplicar vanish para jogadores já online após reload
        if (!org.bukkit.Bukkit.getOnlinePlayers().isEmpty()) {
            // Players already online = reload (PlugManX). Longer delay to ensure TAB is ready.
            getServer().getScheduler().runTaskLater(this, () -> {
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    vanishManager.handleJoin(p);
                }
                getLogger().info("Vanish reaplicado para " + org.bukkit.Bukkit.getOnlinePlayers().size() + " jogadores online (reload).");
            }, 40L);
        }

        getLogger().info("MidgardVanish habilitado com sucesso!");
    }

    @Override
    public void onDisable() {
        // Cancelar todas as tarefas agendadas (PlugManX: evita tarefas orfas)
        getServer().getScheduler().cancelTasks(this);

        if (vanishManager != null) {
            vanishManager.removeAllGlow();
            vanishManager.restoreAllVisibility();
        }
        if (packetListener != null) {
            packetListener.cleanup();
        }
        if (dataManager != null) {
            dataManager.save();
        }
        if (viewerDataManager != null) {
            viewerDataManager.save();
        }
        if (settingsManager != null) {
            settingsManager.save();
        }

        // Desregistrar todos os listeners (PlugManX: limpeza completa)
        org.bukkit.event.HandlerList.unregisterAll(this);

        getLogger().info("MidgardVanish desabilitado!");
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public VanishDataManager getDataManager() {
        return dataManager;
    }
}

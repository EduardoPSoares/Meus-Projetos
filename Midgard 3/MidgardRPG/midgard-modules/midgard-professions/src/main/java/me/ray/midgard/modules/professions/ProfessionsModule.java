package me.ray.midgard.modules.professions;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.ModulePriority;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeManager;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryListener;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryManager;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryOutputItem;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.admin.SmelteryCreationManager;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.ghost.SmelteryGhostBlockManager;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.listener.SmelteryBuildListener;
import me.ray.midgard.modules.professions.gui.ProfessionDefinition;
import me.ray.midgard.modules.professions.gui.ProfessionRewardRegistry;
import me.ray.midgard.modules.professions.penalty.PenaltyBlockListener;
import me.ray.midgard.modules.professions.penalty.PenaltyBrewListener;
import me.ray.midgard.modules.professions.penalty.PenaltyCraftListener;
import me.ray.midgard.modules.professions.penalty.PenaltyEnchantListener;
import me.ray.midgard.modules.professions.penalty.PenaltyFishListener;
import me.ray.midgard.modules.professions.penalty.PenaltyFurnaceListener;
import me.ray.midgard.modules.professions.penalty.ProfessionPenaltyConfig;
import me.ray.midgard.modules.professions.penalty.ProfessionPenaltyManager;
import me.ray.midgard.modules.professions.xp.ProfessionXpBar;
import me.ray.midgard.modules.professions.xp.ProfessionXpConfig;
import me.ray.midgard.modules.professions.xp.ProfessionXpListener;
import me.ray.midgard.modules.professions.xp.PlacedBlockTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;

public class ProfessionsModule extends RPGModule implements Listener {

    private static volatile ProfessionsModule instance;
    private ProfessionManager professionManager;
    private ForgeManager forgeManager;
    private SmelteryManager smelteryManager;
    private SmelteryCreationManager smelteryCreationManager;
    private SmelteryGhostBlockManager smelteryGhostBlockManager;
    private ProfessionXpBar xpBar;
    private ProfessionXpListener xpListener;
    private PlacedBlockTracker blockTracker;
    private BrewStandTracker brewStandTracker;
    private ProfessionPenaltyManager penaltyManager;

    public ProfessionsModule() {
        super("MidgardProfessions", ModulePriority.NORMAL);
    }

    @Override
    public void onEnable() {
        instance = this;
        MidgardLogger.info("Habilitando Midgard-Professions...");

        // Carregar definições, recompensas, fontes de XP e penalidades
        ProfessionDefinition.loadAll();
        ProfessionRewardRegistry.loadAll();
        ProfessionXpConfig.loadAll();
        ProfessionPenaltyConfig.loadAll();

        // Inicializar sistema base de profissões
        if (MidgardCore.getDatabaseManager() != null) {
            this.professionManager = new ProfessionManager(this, MidgardCore.getDatabaseManager());
            MidgardLogger.info("Sistema de profissões inicializado — %d profissões disponíveis.", ProfessionType.values().length);

            // Registrar listener de join/quit para carregar/salvar dados de profissão
            getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());

            // Registrar listener de XP
            this.xpBar = new ProfessionXpBar(professionManager);
            getPlugin().getServer().getPluginManager().registerEvents(xpBar, getPlugin());
            this.blockTracker = new PlacedBlockTracker();
            this.brewStandTracker = new BrewStandTracker();
            this.xpListener = new ProfessionXpListener(professionManager, xpBar, blockTracker, brewStandTracker);
            getPlugin().getServer().getPluginManager().registerEvents(xpListener, getPlugin());

            // Registrar sistema de penalidades para não-profissionais
            this.penaltyManager = new ProfessionPenaltyManager(this);
            getPlugin().getServer().getPluginManager().registerEvents(new PenaltyBlockListener(penaltyManager), getPlugin());
            getPlugin().getServer().getPluginManager().registerEvents(new PenaltyCraftListener(penaltyManager), getPlugin());
            getPlugin().getServer().getPluginManager().registerEvents(new PenaltyFishListener(penaltyManager), getPlugin());
            getPlugin().getServer().getPluginManager().registerEvents(new PenaltyBrewListener(penaltyManager, brewStandTracker), getPlugin());
            getPlugin().getServer().getPluginManager().registerEvents(new PenaltyEnchantListener(penaltyManager), getPlugin());
            getPlugin().getServer().getPluginManager().registerEvents(new PenaltyFurnaceListener(penaltyManager), getPlugin());
            MidgardLogger.info("Sistema de penalidades de profissões inicializado.");
        } else {
            MidgardLogger.warn("DatabaseManager não disponível — sistema de profissões desabilitado.");
        }

        // Inicializar sistema de Forja
        this.forgeManager = new ForgeManager(this);
        this.forgeManager.initialize();

        // Inicializar sistema de Fundição (Smeltery)
        this.smelteryManager = new SmelteryManager(this);

        // Inicializar itens de saída da fundição (lingotes de liga)
        SmelteryOutputItem.init(getPlugin());
        smelteryManager.initialize();

        // Registrar listener da Fundição
        SmelteryListener smelteryListener = new SmelteryListener(smelteryManager);
        getPlugin().getServer().getPluginManager().registerEvents(smelteryListener, getPlugin());

        // Inicializar sistema de criação de Smeltery (templates + ghost blocks)
        this.smelteryCreationManager = new SmelteryCreationManager(smelteryManager);
        this.smelteryCreationManager.loadTemplates();
        getPlugin().getServer().getPluginManager().registerEvents(smelteryCreationManager, getPlugin());

        this.smelteryGhostBlockManager = new SmelteryGhostBlockManager(getPlugin());
        this.smelteryGhostBlockManager.start();

        // Registrar listener de construção de smeltery
        SmelteryBuildListener smelteryBuildListener = new SmelteryBuildListener(
                smelteryGhostBlockManager, smelteryManager.getRegistry());
        getPlugin().getServer().getPluginManager().registerEvents(smelteryBuildListener, getPlugin());

        // Registrar comandos de profissão e forja
        if (me.ray.midgard.core.MidgardCore.getAdminCommand() != null) {
            me.ray.midgard.core.MidgardCore.getAdminCommand().registerSubcommand(
                    new me.ray.midgard.modules.professions.command.ProfessionCommand(this));
            me.ray.midgard.core.MidgardCore.getAdminCommand().registerSubcommand(
                    new me.ray.midgard.modules.professions.blacksmith.forge.command.ForgeCommand(forgeManager));
            me.ray.midgard.core.MidgardCore.getAdminCommand().registerSubcommand(
                    new me.ray.midgard.modules.professions.blacksmith.forge.command.ForgeAdminCommand(forgeManager));

            var smelteryCmd = new me.ray.midgard.modules.professions.blacksmith.forge.command.SmelteryAdminCommand(smelteryManager);
            smelteryCmd.setCreationManager(smelteryCreationManager);
            smelteryCmd.setGhostBlockManager(smelteryGhostBlockManager);
            me.ray.midgard.core.MidgardCore.getAdminCommand().registerSubcommand(smelteryCmd);
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        ProfessionDefinition.loadAll();
        ProfessionRewardRegistry.loadAll();
        ProfessionXpConfig.loadAll();
        ProfessionPenaltyConfig.loadAll();
        if (xpListener != null) {
            xpListener.reloadMultiplier();
        }
        if (xpBar != null) {
            xpBar.reloadConfig();
        }
        if (penaltyManager != null) {
            penaltyManager.reloadConfig();
        }
        if (forgeManager != null && forgeManager.getRecipeManager() != null) {
            forgeManager.getRecipeManager().loadFromConfig(getConfig().getConfigurationSection("forge.recipes"));
        }
        if (smelteryManager != null) {
            smelteryManager.loadSmelteryRecipes();
        }
    }

    @Override
    public void onDisable() {
        MidgardLogger.info("Desabilitando Midgard-Professions...");

        // Desligar sistema de construção de Smeltery
        if (smelteryGhostBlockManager != null) {
            try { smelteryGhostBlockManager.shutdown(); } catch (Exception ignored) { /* shutdown best-effort */ }
        }

        // Desligar sistema de Fundição (inclui visual manager)
        if (smelteryManager != null) {
            try { smelteryManager.shutdown(); } catch (Exception ignored) { /* shutdown best-effort */ }
        }

        // Desligar sistema de Forja
        if (forgeManager != null) {
            try {
                forgeManager.shutdown();
            } catch (Exception ignored) { /* shutdown best-effort */ }
        }

        // Desligar listener de XP
        if (xpListener != null) {
            xpListener.shutdown();
        }
        if (xpBar != null) {
            xpBar.shutdown();
        }
        if (penaltyManager != null) {
            penaltyManager.shutdown();
        }

        instance = null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            if (professionManager == null) { return; }
            professionManager.loadPlayerDataAsync(event.getPlayer());
        } catch (Exception e) {
            MidgardLogger.error("Erro ao carregar dados de profissão para %s", event.getPlayer().getName(), e);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            if (professionManager == null) { return; }
            professionManager.savePlayerData(event.getPlayer());
        } catch (Exception e) {
            MidgardLogger.error("Erro ao salvar dados de profissão para %s", event.getPlayer().getName(), e);
        }
    }

    public File getDataFolder() {
        return new File(plugin.getDataFolder(), "modules/professions");
    }

    public static ProfessionsModule getInstance() {
        return instance;
    }

    public ProfessionManager getProfessionManager() {
        return professionManager;
    }

    public ForgeManager getForgeManager() {
        return forgeManager;
    }

    public SmelteryManager getSmelteryManager() {
        return smelteryManager;
    }
}

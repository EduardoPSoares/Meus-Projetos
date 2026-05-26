package me.ray.midgardspigot;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import java.util.logging.Level;
// import net.kokoricraft.reviveme.api.ReviveMeAPI; // Removido para usar Reflection

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class MidgardSpigot extends JavaPlugin implements PluginMessageListener {

    public static final String PREFIX = "";
    private static MidgardSpigot instance;
    private MaintenanceListener maintenanceListener;

    public boolean isMaintenanceActive() {
        return maintenanceListener != null;
    }

    public String getMessage(String path) {
        String msg = getConfig().getString("messages." + path);
        if (msg == null) return "Message not found: " + path;
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public void onEnable() {
        try {
            long startTime = System.currentTimeMillis();
            instance = this;
            
            getLogger().info("==========================================");
            getLogger().info("Iniciando MidgardSpigot v" + getDescription().getVersion());
            getLogger().info("==========================================");

            // Carregar Config
            saveDefaultConfig();
            
            // Auto Update
            if (getConfig().getBoolean("update.enabled", true)) {
                int interval = getConfig().getInt("update.check-interval", 60);
                
                me.ray.midgardspigot.utils.AutoUpdater updater = new me.ray.midgardspigot.utils.AutoUpdater(this);
                
                // Check immediately (Async to not block main thread)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        updater.checkForUpdate();
                    }
                }.runTaskAsynchronously(this);
                
                // Schedule periodic check
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        updater.checkForUpdate();
                    }
                }.runTaskTimerAsynchronously(this, interval * 1200L, interval * 1200L);
            }

            // Verificar dependências
            if (Bukkit.getPluginManager().getPlugin("MMOCore") != null) {
                getLogger().info("[Dependencia] MMOCore detectado.");
            } else {
                getLogger().warning("[Dependencia] MMOCore NAO encontrado. Funcionalidades de combate serao limitadas.");
            }

            // A verificação do ReviveMe é feita via classe, mas podemos checar o plugin também
            if (Bukkit.getPluginManager().getPlugin("ReviveMe") != null) {
                getLogger().info("[Dependencia] ReviveMe detectado.");
            } else {
                getLogger().warning("[Dependencia] ReviveMe NAO encontrado. Funcionalidades de reviver serao limitadas.");
            }
            
            // Registrar canal de mensagens para receber comandos do Velocity
            try {
                this.getServer().getMessenger().registerIncomingPluginChannel(this, "midgard:maintenance", this);
                this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord"); // Registrar canal de saída BungeeCord
                getLogger().info("[Conexao] Canal 'midgard:maintenance' registrado com sucesso.");
                getLogger().info("[Conexao] Canal de saida 'BungeeCord' registrado.");
                getLogger().info("[Conexao] Aguardando comunicacao do Velocity...");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "[Conexao] Erro ao registrar canal de mensagens!", e);
            }

            // Registrar Listener de Fix e Comando
            getServer().getPluginManager().registerEvents(new MaintenanceFixListener(), this);
            getCommand("mmaintenance").setExecutor(new MaintenanceCommand());
            
            // Limpeza inicial de imortalidade (caso de reload ou reinicio com players online)
            new BukkitRunnable() {
                @Override
                public void run() {
                    getLogger().info("[Manutencao] Verificando e removendo imortalidade residual de jogadores online...");
                    int count = 0;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getGameMode() != org.bukkit.GameMode.CREATIVE && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                            // Ignora se for Ghost do MidgardPermaDeath
                            if (isMidgardGhost(p)) continue;

                            if (p.isInvulnerable()) {
                                p.setInvulnerable(false);
                                count++;
                            }
                        }
                    }
                    if (count > 0) {
                        getLogger().info("[Manutencao] Imortalidade removida de " + count + " jogadores.");
                    }
                }
            }.runTaskLater(this, 20L); // Executa 1 segundo após o enable

            // Watchdog Task: Verifica periodicamente (a cada 1 minuto) se há jogadores imortais indevidamente
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Se estiver em manutenção, não faz nada (pois todos devem estar imortais)
                    if (isMaintenanceActive()) return;

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getGameMode() != org.bukkit.GameMode.CREATIVE && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                            // Ignora se for Ghost do MidgardPermaDeath
                            if (isMidgardGhost(p)) continue;

                            if (p.isInvulnerable()) {
                                p.setInvulnerable(false);
                                getLogger().warning("[Watchdog] Imortalidade removida de " + p.getName() + " (Varredura Periodica)");
                            }
                        }
                    }
                }
            }.runTaskTimer(this, 1200L, 1200L); // 1200 ticks = 60 segundos

            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info("MidgardSpigot habilitado com sucesso em " + loadTime + "ms.");
            getLogger().info("==========================================");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erro fatal em onEnable", e);
        }
    }

    public boolean isMidgardGhost(Player p) {
        try {
            if (Bukkit.getPluginManager().getPlugin("MidgardPermaDeath") == null) return false;
            
            // Reflection para evitar dependência hard
            // me.ray.midgardPermaDeath.MidgardPermaDeath.getGhostManager().isGhost(Player)
            
            // 1. Pegar plugin instance
            org.bukkit.plugin.Plugin permaPlugin = Bukkit.getPluginManager().getPlugin("MidgardPermaDeath");
            
            // 2. Pegar GhostManager
            java.lang.reflect.Method getGhostManager = permaPlugin.getClass().getMethod("getGhostManager");
            Object ghostManager = getGhostManager.invoke(permaPlugin);
            
            if (ghostManager == null) return false;
            
            // 3. Verificar isGhost
            java.lang.reflect.Method isGhost = ghostManager.getClass().getMethod("isGhost", Player.class);
            Object result = isGhost.invoke(ghostManager, p);
            
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable e) {
            // Silencioso para não spammar se houver erro de versão ou dependência faltando (ex: Vault no MidgardPermaDeath)
            return false;
        }
    }

    @Override
    public void onDisable() {
        try {
            getLogger().info("Desabilitando MidgardSpigot...");

            // Cancelar todas as tarefas agendadas (PlugManX: evita tarefas orfas)
            getServer().getScheduler().cancelTasks(this);

            // Desregistrar canais de mensagens
            this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
            this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
            getLogger().info("Canais de mensagens desregistrados.");

            // Desregistrar todos os listeners (PlugManX: limpeza completa)
            org.bukkit.event.HandlerList.unregisterAll(this);

            // Limpar referência do listener de manutenção
            maintenanceListener = null;

            getLogger().info("MidgardSpigot desabilitado.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erro fatal em onDisable", e);
        } finally {
            instance = null;
        }
    }

    public static MidgardSpigot getInstance() {
        return instance;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        try {
            if (!channel.equals("midgard:maintenance")) {
                return;
            }

            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subChannel = in.readUTF();

            getLogger().info("[Velocity] Mensagem recebida no canal: " + subChannel);

            if (subChannel.equals("PRE_MAINTENANCE")) {
                getLogger().info("[Manutencao] Sinal de PRE_MAINTENANCE recebido! Iniciando protocolos de protecao...");
                executePreMaintenanceActions();
            } else if (subChannel.equals("POST_MAINTENANCE")) {
                getLogger().info("[Manutencao] Sinal de POST_MAINTENANCE recebido! Limpando imortalidade residual...");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
                        // Ignora se for Ghost do MidgardPermaDeath
                        if (isMidgardGhost(p)) continue;

                        if (p.isInvulnerable()) {
                            p.setInvulnerable(false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erro em onPluginMessageReceived", e);
        }
    }

    private void executePreMaintenanceActions() {
        try {
            // Registrar listener de bloqueio total (Lockdown)
            if (maintenanceListener != null) {
                org.bukkit.event.HandlerList.unregisterAll(maintenanceListener);
            }
            maintenanceListener = new MaintenanceListener();
            getServer().getPluginManager().registerEvents(maintenanceListener, this);
            getLogger().info("[Manutencao] Listener de bloqueio registrado (Lockdown ativado: Redstone, Hoppers, Ambiente, Players).");

            int count = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    // 0. Salvar dados do jogador (CRÍTICO) - Fazemos isso primeiro para todos
                    p.saveData();

                    // Se for admin, pula o resto (não chuta, não congela)
                    if (p.hasPermission("midgard.admin")) {
                        p.sendMessage(PREFIX + getMessage("admin-bypass"));
                        continue;
                    }

                    count++;

                    // 1. Tornar imortal (Vanilla)
                    p.setInvulnerable(true);
                    
                    // 2. Fechar inventários (Anti-Dupe)
                    p.closeInventory();
                    
                    // 3. Sair de veículos (Anti-Glitch)
                    p.leaveVehicle();
                    
                    // 4. Aplicar efeitos de congelamento visual/físico
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 255)); // Imobiliza
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1)); // Feedback visual
                    
                    // 5. Remover Combat Tag do MMOCore
                    handleMMOCore(p);
                    
                    // 6. Levantar player (Revive Plugin)
                    handleRevive(p);
                    
                    // 7. Feedback Visual e Sonoro
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f);
                    p.sendTitle(getMessage("maintenance-title"), getMessage("maintenance-subtitle"), 0, 100, 20);

                    // 8. Enviar para o Lobby (Tentativa imediata)
                    sendToLobby(p);
                    
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Erro ao proteger jogador " + p.getName(), e);
                }
            }
            
            // Salvar dados globais e Mundos
            try {
                getLogger().info("[Manutencao] Forcando salvamento de todos os mundos e pausando mecanicas...");
                for (World world : Bukkit.getWorlds()) {
                    world.save();
                    // Pausar mecânicas do mundo para economizar recursos e evitar alterações
                    world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                    world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                    world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0); // Para crescimento de plantações, fogo, etc.
                }
                
                Bukkit.savePlayers();
                
                // Executa save-all para garantir flush no disco
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all flush");
                
                getLogger().info("[Manutencao] Todos os dados salvos e mundos congelados (GameRules).");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Erro ao salvar dados globais.", e);
            }
            
            getLogger().info("[Manutencao] Protocolos aplicados a " + count + " jogadores online.");
            
            // 10. Failsafe: Agendar Kick Geral em 8 segundos (caso o envio para o Lobby falhe)
            Bukkit.getScheduler().runTaskLater(this, () -> {
                try {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!p.hasPermission("midgard.admin")) {
                            p.kickPlayer("§6§lᴍɪᴅɢᴀʀᴅ ʀᴘɢ\n\n§7ᴏ sᴇʀᴠɪᴅᴏʀ ꜰᴏɪ ꜰᴇᴄʜᴀᴅᴏ ᴘᴀʀᴀ ᴍᴀɴᴜᴛᴇɴçãᴏ.\n§8ᴀɢʀᴀᴅᴇᴄᴇᴍᴏ sᴜᴀ ᴄᴏᴍᴘʀᴇᴇɴsãᴏ.");
                        }
                    }
                    getLogger().info("[Failsafe] Verificação de jogadores restantes concluída.");
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Erro no Failsafe task", e);
                }
            }, 160L); // 160 ticks = 8 segundos

            // 11. Monitorar saída de jogadores para reativar o mundo para admins
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        long nonAdmins = Bukkit.getOnlinePlayers().stream()
                                .filter(p -> !p.hasPermission("midgard.admin"))
                                .count();

                        if (nonAdmins == 0) {
                            // Restaurar GameRules
                            getLogger().info("[Manutencao] Apenas admins (ou ninguem) online. Reativando mundos.");
                            for (World world : Bukkit.getWorlds()) {
                                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
                                world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
                                world.setGameRule(GameRule.RANDOM_TICK_SPEED, 3);
                            }
                            
                            // Avisar admins
                            Bukkit.broadcast("§aᴛᴏᴅᴏs ᴏs ᴊᴏɢᴀᴅᴏʀᴇs sᴀíʀᴀᴍ. ᴍᴜɴᴅᴏ ʀᴇᴀᴛɪᴠᴀᴅᴏ ᴘᴀʀᴀ ᴀᴅᴍɪɴɪsᴛʀᴀçãᴏ.", "midgard.admin");
                            
                            // Remover listener de bloqueio para permitir testes (Redstone, Física, etc)
                            if (maintenanceListener != null) {
                                org.bukkit.event.HandlerList.unregisterAll(maintenanceListener);
                                maintenanceListener = null;
                                getLogger().info("[Manutencao] Listeners de bloqueio removidos. Admins podem interagir livremente.");
                            }

                            getLogger().info("[Manutencao] Monitoramento de jogadores encerrado.");
                            this.cancel();
                        }
                    } catch (Exception e) {
                        getLogger().log(Level.SEVERE, "Erro no monitoramento de jogadores", e);
                        this.cancel();
                    }
                }
            }.runTaskTimer(this, 20L, 20L); // Checa a cada 1 segundo
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erro fatal em executePreMaintenanceActions", e);
        }
    }

    private void handleMMOCore(Player p) {
        try {
            if (Bukkit.getPluginManager().getPlugin("MMOCore") != null) {
                try {
                    // Usa Reflection para evitar dependência hard da API (e conflitos de classloader)
                    Class<?> playerDataClass = Class.forName("net.indyuce.mmocore.api.player.PlayerData");
                    java.lang.reflect.Method getMethod = playerDataClass.getMethod("get", Player.class);
                    Object playerData = getMethod.invoke(null, p);

                    if (playerData != null) {
                        java.lang.reflect.Method getCombat = playerDataClass.getMethod("getCombat");
                        Object combat = getCombat.invoke(playerData);
                        if (combat != null) {
                            java.lang.reflect.Method setLastCombat = combat.getClass().getMethod("setLastCombat", long.class);
                            setLastCombat.invoke(combat, 0L);
                            getLogger().info("[MMOCore] Combat tag removida de: " + p.getName());
                        }
                    }
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "[MMOCore] Erro ao manipular dados para " + p.getName(), e);
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erro em handleMMOCore", e);
        }
    }

    private void handleRevive(Player p) {
        try {
            // Implementação usando Reflection para evitar IncompatibleClassChangeError
            // Isso resolve o conflito se a API for Interface no jar de dev mas Class no servidor
            try {
                Class<?> apiClass = Class.forName("net.kokoricraft.reviveme.api.ReviveMeAPI");
                
                // Método hasDowned(Player)
                java.lang.reflect.Method hasDownedMethod = apiClass.getMethod("hasDowned", Player.class);
                Object result = hasDownedMethod.invoke(null, p);
                
                if (result instanceof Boolean && (Boolean) result) {
                    // Método revivePlayer(Player)
                    java.lang.reflect.Method reviveMethod = apiClass.getMethod("revivePlayer", Player.class);
                    reviveMethod.invoke(null, p);
                    getLogger().info("[ReviveMe] Jogador revivido: " + p.getName());
                }
            } catch (ClassNotFoundException e) {
                // Apenas loga se o plugin não estiver presente, sem spammar stacktrace
                getLogger().warning("[ReviveMe] Classe da API nao encontrada. O plugin ReviveMe esta instalado?");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "[ReviveMe] Erro ao reviver " + p.getName() + " via Reflection", e);
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erro em handleRevive", e);
        }
    }

    private void sendToLobby(Player p) {
        try {
            com.google.common.io.ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("lobby"); // Tenta enviar para o servidor chamado "lobby"
            p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            getLogger().warning("Falha ao enviar " + p.getName() + " para o lobby: " + e.getMessage());
        }
    }
}


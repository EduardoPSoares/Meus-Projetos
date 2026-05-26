package me.ray.midgardDiscord;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.model.user.User;
import org.slf4j.Logger;

/**
 * Integração com LuckPerms.
 * Gerencia permissões e grupos dos jogadores, permitindo sincronização com cargos do Discord.
 */
public class LuckPermsHandler {

    private final MidgardVelocity plugin;
    private final Logger logger;
    private LuckPerms luckPerms;

    public LuckPermsHandler(MidgardVelocity plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void init() {
        try {
            this.luckPerms = LuckPermsProvider.get();
            registerPermissions();
            
            // Registrar listeners
            this.luckPerms.getEventBus().subscribe(plugin, NodeAddEvent.class, this::onNodeAdd);
            this.luckPerms.getEventBus().subscribe(plugin, NodeRemoveEvent.class, this::onNodeRemove);
            
            logger.info("LuckPerms hook enabled!");
        } catch (IllegalStateException e) {
            logger.warn("LuckPerms not found! Permissions will not be registered in the editor.");
        } catch (Exception e) {
            logger.error("Erro inesperado ao inicializar LuckPermsHandler: ", e);
        }
    }

    private void registerPermissions() {
        try {
            if (luckPerms == null) return;

            String[] permissions = {
                "midgard.admin",
                "midgard.op",
                "midgard.staff",
                "midgard.maintenance.bypass"
            };

            // Instead of creating a group, we simply check the permissions on the console sender.
            // This forces LuckPerms to "see" these permissions and add them to its internal registry/autocomplete
            // without creating any persistent groups or data.
            com.velocitypowered.api.proxy.ProxyServer server = plugin.getServer();
            if (server != null) {
                for (String perm : permissions) {
                    server.getConsoleCommandSource().hasPermission(perm);
                }
                logger.info("Registered " + permissions.length + " permissions to LuckPerms autocomplete.");
            }
        } catch (Exception e) {
            logger.error("Erro ao registrar permissões no LuckPerms: ", e);
        }
    }

    private void onNodeAdd(NodeAddEvent event) {
        try {
            if (!event.isUser()) return;
            
            Node node = event.getNode();
            String key = node.getKey();
            
            // logger.info("LuckPerms Node Add: " + key); // Debug only
            
            if (isAdminPermission(key)) {
                User user = (User) event.getTarget();
                String username = user.getUsername();
                
                if (username != null) {
                    plugin.addAdmin(username);
                } else {
                    // Busca assíncrona para não bloquear a thread de eventos
                    luckPerms.getUserManager().lookupUsername(user.getUniqueId()).thenAcceptAsync(name -> {
                        if (name != null) {
                            plugin.addAdmin(name);
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao processar evento NodeAdd: ", e);
        }
    }

    private void onNodeRemove(NodeRemoveEvent event) {
        try {
            if (!event.isUser()) return;
            
            Node node = event.getNode();
            String key = node.getKey();
            
            // logger.info("LuckPerms Node Remove: " + key); // Debug only
            
            if (isAdminPermission(key)) {
                User user = (User) event.getTarget();
                String username = user.getUsername();
                
                if (username != null) {
                    plugin.removeAdmin(username);
                } else {
                    // Busca assíncrona para não bloquear a thread de eventos
                    luckPerms.getUserManager().lookupUsername(user.getUniqueId()).thenAcceptAsync(name -> {
                        if (name != null) {
                            plugin.removeAdmin(name);
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao processar evento NodeRemove: ", e);
        }
    }

    private boolean isAdminPermission(String key) {
        return key.equals("midgard.admin") || 
               key.equals("midgard.staff") || 
               key.equals("midgard.op") ||
               key.equals("midgard.maintenance.bypass") ||
               key.startsWith("group.admin") || 
               key.startsWith("group.staff") ||
               key.startsWith("group.dono") ||
               key.startsWith("group.diretor") ||
               key.startsWith("group.gerente");
    }

    public void addGroup(java.util.UUID uuid, String groupName) {
        try {
            if (luckPerms == null) return;
            
            // Validação de segurança para evitar injeção ou nomes inválidos
            if (groupName == null || !groupName.matches("[a-zA-Z0-9_-]+")) {
                logger.warn("Tentativa de adicionar grupo com nome inválido: " + groupName);
                return;
            }

            luckPerms.getUserManager().modifyUser(uuid, user -> {
                user.data().add(net.luckperms.api.node.types.InheritanceNode.builder(groupName).build());
            }).thenRunAsync(() -> {
                logger.info("Adicionado grupo " + groupName + " para UUID " + uuid);
            }).exceptionally(e -> {
                logger.error("Erro ao adicionar grupo LuckPerms para UUID " + uuid, e);
                return null;
            });
        } catch (Exception e) {
            logger.error("Erro ao iniciar adição de grupo LuckPerms: ", e);
        }
    }

    public void removeGroup(java.util.UUID uuid, String groupName) {
        try {
            if (luckPerms == null) return;

            // Validação de segurança
            if (groupName == null || !groupName.matches("[a-zA-Z0-9_-]+")) {
                logger.warn("Tentativa de remover grupo com nome inválido: " + groupName);
                return;
            }

            luckPerms.getUserManager().modifyUser(uuid, user -> {
                user.data().remove(net.luckperms.api.node.types.InheritanceNode.builder(groupName).build());
            }).thenRunAsync(() -> {
                logger.info("Removido grupo " + groupName + " para UUID " + uuid);
            }).exceptionally(e -> {
                logger.error("Erro ao remover grupo LuckPerms para UUID " + uuid, e);
                return null;
            });
        } catch (Exception e) {
            logger.error("Erro ao iniciar remoção de grupo LuckPerms: ", e);
        }
    }
}

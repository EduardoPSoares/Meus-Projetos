package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.admin;

import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryBlockType;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryManager;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelteryBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelterySchematic;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelteryTemplate;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Gerencia a criação de templates de smeltery, edição e o painel admin.
 * Templates são blueprints que jogadores usam para construir fundições.
 */
public class SmelteryCreationManager implements Listener {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }

    private static SmelteryCreationManager instance;

    private final SmelteryManager smelteryManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final Map<UUID, SmelteryCreationSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, ChatCallback> awaitingChatInput = new ConcurrentHashMap<>();

    // Cache em memória de todos os templates
    private final List<SmelteryTemplate> templates = new ArrayList<>();

    public SmelteryCreationManager(SmelteryManager smelteryManager) {
        this.smelteryManager = smelteryManager;
        instance = this;
    }

    public static SmelteryCreationManager getInstance() {
        return instance;
    }

    public void loadTemplates() {
        templates.clear();
        // Templates são carregados em memória. Persistência pode ser adicionada via repositório.
    }

    public List<SmelteryTemplate> getTemplates() {
        return Collections.unmodifiableList(templates);
    }

    // === Admin Panel ===

    public void openAdminPanel(Player admin) {
        SmelteryAdminGui gui = new SmelteryAdminGui(admin, new ArrayList<>(templates));

        gui.setOnTemplateClick((p, template) -> openEditGui(p, template));
        gui.setOnCreateNew(p -> startSession(p));

        gui.open();
    }

    public void openEditGui(Player admin, SmelteryTemplate template) {
        SmelteryEditGui gui = new SmelteryEditGui(admin, template);

        gui.setOnBack(p -> openAdminPanel(p));
        gui.setOnDelete(p -> {
            templates.remove(template);
            openAdminPanel(p);
        });

        gui.open();
    }

    // === Session Management ===

    public void startSession(Player admin) {
        SmelteryCreationSession session = sessions.get(admin.getUniqueId());
        if (session != null) {
            openSetupGui(admin, session);
            return;
        }

        session = new SmelteryCreationSession(admin.getUniqueId());
        sessions.put(admin.getUniqueId(), session);
        openSetupGui(admin, session);
    }

    public void cancelSession(Player admin) {
        SmelteryCreationSession removed = sessions.remove(admin.getUniqueId());
        awaitingChatInput.remove(admin.getUniqueId());
        if (removed != null) {
            removed.stopVisualization();
            admin.sendMessage(mm.deserialize(msg("smeltery.admin.creation_cancelled")));
        } else {
            admin.sendMessage(mm.deserialize(msg("smeltery.admin.no_active_session")));
        }
    }

    public boolean hasSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public SmelteryCreationSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    // === Chat Input Registration ===

    public void setAwaitingSessionNameInput(UUID playerId, SmelteryCreationSession session) {
        awaitingChatInput.put(playerId, new ChatCallback(ChatCallbackType.SET_SESSION_NAME, null, session, null));
    }

    public void setAwaitingNameInput(UUID playerId, SmelteryTemplate template, Consumer<Player> onComplete) {
        awaitingChatInput.put(playerId, new ChatCallback(ChatCallbackType.RENAME_TEMPLATE, template, null, onComplete));
    }

    // === Template Confirmation ===

    public void confirmCreation(Player admin, SmelteryCreationSession session) {
        if (!session.hasRequiredRoles()) {
            admin.sendMessage(mm.deserialize(msg("smeltery.admin.missing_required_blocks")));
            return;
        }

        SmelterySchematic schematic = session.buildSchematic();

        SmelteryTemplate template = new SmelteryTemplate(
                UUID.randomUUID(),
                session.getName(),
                session.getTier(),
                session.getRequiredLevel()
        );
        template.setSchematic(schematic);

        // Adicionar à memória
        templates.add(template);

        sessions.remove(admin.getUniqueId());
        session.stopVisualization();

        admin.sendMessage(mm.deserialize(""));
        admin.sendMessage(mm.deserialize(msg("smeltery.admin.template_created")));
        admin.sendMessage(mm.deserialize(msg("smeltery.admin.template_name").replace("%name%", template.getName())));
        admin.sendMessage(mm.deserialize(msg("smeltery.admin.template_id").replace("%id%", template.getTemplateId().toString().substring(0, 8))));
        admin.sendMessage(mm.deserialize(msg("smeltery.admin.template_type").replace("%type%", session.getTier().getFormattedName())));
        admin.sendMessage(mm.deserialize(msg("smeltery.admin.template_level").replace("%level%", String.valueOf(session.getRequiredLevel()))));
        admin.sendMessage(mm.deserialize(msg("smeltery.admin.template_dimensions").replace("%w%", String.valueOf(session.getWidth())).replace("%h%", String.valueOf(session.getHeight())).replace("%d%", String.valueOf(session.getDepth()))));

        Map<SmelteryBlockType, Integer> roles = session.getRoleCounts();
        for (var entry : roles.entrySet()) {
            admin.sendMessage(mm.deserialize(msg("smeltery.admin.block_entry")
                    .replace("%type%", SmelterySetupGui.getTypeName(entry.getKey()))
                    .replace("%count%", String.valueOf(entry.getValue()))));
        }
        admin.sendMessage(mm.deserialize(""));

        openAdminPanel(admin);
    }

    // === GUI Opening ===

    public void openSetupGui(Player admin, SmelteryCreationSession session) {
        SmelterySetupGui gui = new SmelterySetupGui(admin, session);

        gui.setOnSetName(p -> {
            setAwaitingSessionNameInput(p.getUniqueId(), session);
        });

        gui.setOnOpenBlockAssign(p -> {
            SmelteryBlockAssignGui blockGui = new SmelteryBlockAssignGui(p, session);
            blockGui.setOnBack(bp -> openSetupGui(bp, session));
            blockGui.open();
        });

        gui.setOnConfirm(p -> confirmCreation(p, session));

        gui.setOnCancel(p -> {
            SmelteryCreationSession s = sessions.remove(p.getUniqueId());
            if (s != null) { s.stopVisualization(); }
            p.sendMessage(mm.deserialize(msg("smeltery.admin.creation_cancelled")));
            openAdminPanel(p);
        });

        gui.open();
    }

    // === Event Handlers ===

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        ChatCallback callback = awaitingChatInput.remove(playerId);
        if (callback == null) { return; }

        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        switch (callback.type()) {
            case SET_SESSION_NAME -> {
                if (input.length() > 64) {
                    me.ray.midgard.core.utils.Task.sync(player, () -> {
                        player.sendMessage(mm.deserialize(msg("smeltery.admin.name_too_long")));
                        awaitingChatInput.put(playerId, callback);
                    });
                    return;
                }
                callback.session().setName(input);
                me.ray.midgard.core.utils.Task.sync(player, () -> {
                    player.sendMessage(mm.deserialize(msg("smeltery.admin.name_set").replace("%name%", input)));
                    openSetupGui(player, callback.session());
                });
            }
            case RENAME_TEMPLATE -> {
                if (input.length() > 64) {
                    me.ray.midgard.core.utils.Task.sync(player, () -> {
                        player.sendMessage(mm.deserialize(msg("smeltery.admin.name_too_long")));
                        awaitingChatInput.put(playerId, callback);
                    });
                    return;
                }
                callback.template().setName(input);
                me.ray.midgard.core.utils.Task.sync(player, () -> {
                    player.sendMessage(mm.deserialize(msg("smeltery.admin.template_renamed").replace("%name%", input)));
                    if (callback.onComplete() != null) { callback.onComplete().accept(player); }
                });
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        SmelteryCreationSession removed = sessions.remove(playerId);
        if (removed != null) { removed.stopVisualization(); }
        awaitingChatInput.remove(playerId);
    }

    // === Internal Types ===

    private enum ChatCallbackType {
        SET_SESSION_NAME,
        RENAME_TEMPLATE
    }

    private record ChatCallback(ChatCallbackType type, SmelteryTemplate template,
                                SmelteryCreationSession session, Consumer<Player> onComplete) {}
}

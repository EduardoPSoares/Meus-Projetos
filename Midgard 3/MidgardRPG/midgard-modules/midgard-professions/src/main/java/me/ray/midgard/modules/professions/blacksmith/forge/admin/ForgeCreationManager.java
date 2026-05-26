package me.ray.midgard.modules.professions.blacksmith.forge.admin;

import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeManager;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeSchematic;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeTemplate;
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
 * Manages forge template creation, editing, and the admin panel.
 * Templates are blueprints that players use to build forges.
 */
public class ForgeCreationManager implements Listener {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }

    private static ForgeCreationManager instance;

    private final ForgeManager forgeManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final Map<UUID, ForgeCreationSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, ChatCallback> awaitingChatInput = new ConcurrentHashMap<>();

    // In-memory cache of all templates
    private final List<ForgeTemplate> templates = new ArrayList<>();

    public ForgeCreationManager(ForgeManager forgeManager) {
        this.forgeManager = forgeManager;
        instance = this;
    }

    public static ForgeCreationManager getInstance() {
        return instance;
    }

    /** Loads all templates from DB into memory. Called during initialization. */
    public void loadTemplates() {
        templates.clear();
        if (forgeManager.getRepository() != null) {
            templates.addAll(forgeManager.getRepository().loadAllTemplates());
        }
    }

    public List<ForgeTemplate> getTemplates() {
        return Collections.unmodifiableList(templates);
    }

    // === Admin Panel ===

    public void openAdminPanel(Player admin) {
        ForgeAdminGui gui = new ForgeAdminGui(admin, new ArrayList<>(templates));

        gui.setOnTemplateClick((p, template) -> openEditGui(p, template));
        gui.setOnCreateNew(p -> startSession(p));

        gui.open();
    }

    public void openEditGui(Player admin, ForgeTemplate template) {
        ForgeEditGui gui = new ForgeEditGui(admin, template, forgeManager.getRepository());

        gui.setOnBack(p -> openAdminPanel(p));
        gui.setOnDelete(p -> {
            templates.remove(template);
            openAdminPanel(p);
        });

        gui.open();
    }

    // === Session Management ===

    public void startSession(Player admin) {
        ForgeCreationSession session = sessions.get(admin.getUniqueId());
        if (session != null) {
            openSetupGui(admin, session);
            return;
        }

        session = new ForgeCreationSession(admin.getUniqueId());
        sessions.put(admin.getUniqueId(), session);
        openSetupGui(admin, session);
    }

    public void cancelSession(Player admin) {
        ForgeCreationSession removed = sessions.remove(admin.getUniqueId());
        awaitingChatInput.remove(admin.getUniqueId());
        if (removed != null) {
            removed.stopVisualization();
            admin.sendMessage(mm.deserialize(msg("forge.admin.creation_cancelled")));
        } else {
            admin.sendMessage(mm.deserialize(msg("forge.admin.no_active_session")));
        }
    }

    public boolean hasSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public ForgeCreationSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    // === Chat Input Registration ===

    /**
     * Registers a chat callback for setting the session name during creation.
     */
    public void setAwaitingSessionNameInput(UUID playerId, ForgeCreationSession session) {
        awaitingChatInput.put(playerId, new ChatCallback(ChatCallbackType.SET_SESSION_NAME, null, session, null));
    }

    /**
     * Registers a chat callback for renaming an existing template.
     */
    public void setAwaitingNameInput(UUID playerId, ForgeTemplate template, Consumer<Player> onComplete) {
        awaitingChatInput.put(playerId, new ChatCallback(ChatCallbackType.RENAME_TEMPLATE, template, null, onComplete));
    }

    // === Template Confirmation ===

    public void confirmCreation(Player admin, ForgeCreationSession session) {
        if (!session.hasRequiredRoles()) {
            admin.sendMessage(mm.deserialize(msg("forge.admin.missing_required_blocks")));
            return;
        }

        ForgeSchematic schematic = session.buildSchematic();

        ForgeTemplate template = new ForgeTemplate(
                UUID.randomUUID(),
                session.getName(),
                session.getTier(),
                session.getRequiredLevel()
        );
        template.setSchematic(schematic);

        // Add to memory
        templates.add(template);

        // Save to DB
        if (forgeManager.getRepository() != null) {
            forgeManager.getRepository().saveTemplate(template);
        }

        sessions.remove(admin.getUniqueId());
        session.stopVisualization();

        admin.sendMessage(mm.deserialize(""));
        admin.sendMessage(mm.deserialize(msg("forge.admin.template_created")));
        admin.sendMessage(mm.deserialize(msg("forge.admin.template_name").replace("%name%", template.getName())));
        admin.sendMessage(mm.deserialize(msg("forge.admin.template_id").replace("%id%", template.getTemplateId().toString().substring(0, 8))));
        admin.sendMessage(mm.deserialize(msg("forge.admin.template_type").replace("%type%", session.getTier().getDisplayName())));
        admin.sendMessage(mm.deserialize(msg("forge.admin.template_level").replace("%level%", String.valueOf(session.getRequiredLevel()))));
        admin.sendMessage(mm.deserialize(msg("forge.admin.template_dimensions").replace("%w%", String.valueOf(session.getWidth())).replace("%h%", String.valueOf(session.getHeight())).replace("%d%", String.valueOf(session.getDepth()))));

        Map<ForgeBlock.ForgeBlockType, Integer> roles = session.getRoleCounts();
        for (var entry : roles.entrySet()) {
            admin.sendMessage(mm.deserialize(msg("forge.admin.block_entry")
                    .replace("%type%", ForgeSetupGui.getTypeName(entry.getKey()))
                    .replace("%count%", String.valueOf(entry.getValue()))));
        }
        admin.sendMessage(mm.deserialize(""));

        // Open admin panel
        openAdminPanel(admin);
    }

    // === GUI Opening ===

    public void openSetupGui(Player admin, ForgeCreationSession session) {
        ForgeSetupGui gui = new ForgeSetupGui(admin, session);

        gui.setOnSetName(p -> {
            setAwaitingSessionNameInput(p.getUniqueId(), session);
        });

        gui.setOnOpenBlockAssign(p -> {
            ForgeBlockAssignGui blockGui = new ForgeBlockAssignGui(p, session);
            blockGui.setOnBack(bp -> openSetupGui(bp, session));
            blockGui.open();
        });

        gui.setOnConfirm(p -> confirmCreation(p, session));

        gui.setOnCancel(p -> {
            ForgeCreationSession s = sessions.remove(p.getUniqueId());
            if (s != null) { s.stopVisualization(); }
            p.sendMessage(mm.deserialize(msg("forge.admin.creation_cancelled")));
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
                        player.sendMessage(mm.deserialize(msg("forge.admin.name_too_long")));
                        awaitingChatInput.put(playerId, callback);
                    });
                    return;
                }
                callback.session().setName(input);
                me.ray.midgard.core.utils.Task.sync(player, () -> {
                    player.sendMessage(mm.deserialize(msg("forge.admin.name_set").replace("%name%", input)));
                    openSetupGui(player, callback.session());
                });
            }
            case RENAME_TEMPLATE -> {
                if (input.length() > 64) {
                    me.ray.midgard.core.utils.Task.sync(player, () -> {
                        player.sendMessage(mm.deserialize(msg("forge.admin.name_too_long")));
                        awaitingChatInput.put(playerId, callback);
                    });
                    return;
                }
                callback.template().setName(input);
                me.ray.midgard.core.utils.Task.sync(player, () -> {
                    player.sendMessage(mm.deserialize(msg("forge.admin.template_renamed").replace("%name%", input)));
                    if (callback.onComplete() != null) { callback.onComplete().accept(player); }
                });
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        ForgeCreationSession removed = sessions.remove(playerId);
        if (removed != null) { removed.stopVisualization(); }
        awaitingChatInput.remove(playerId);
    }

    // === Internal Types ===

    private enum ChatCallbackType {
        SET_SESSION_NAME,
        RENAME_TEMPLATE
    }

    private record ChatCallback(ChatCallbackType type, ForgeTemplate template,
                                ForgeCreationSession session, Consumer<Player> onComplete) {}
}

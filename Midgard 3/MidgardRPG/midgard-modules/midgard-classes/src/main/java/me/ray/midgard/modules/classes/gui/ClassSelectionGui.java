package me.ray.midgard.modules.classes.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.config.ConfigWrapper;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.classes.ClassData;
import me.ray.midgard.modules.classes.ClassSkillLink;
import me.ray.midgard.modules.classes.ClassesModule;
import me.ray.midgard.modules.classes.RPGClass;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ClassSelectionGui extends BaseGui {

    private final ClassesModule module;
    private final ConfigWrapper config;
    private final JavaPlugin plugin;
    private static final int MAX_REOPEN_ATTEMPTS = 3;
    private final Map<Integer, String> actions = new HashMap<>();
    private int reopenCount = 0;

    private static ConfigWrapper loadConfig(JavaPlugin plugin) {
        return new ConfigWrapper(plugin, "modules/classes/gui/selection.yml");
    }

    public ClassSelectionGui(JavaPlugin plugin, Player player, ClassesModule module) {
        this(plugin, player, module, loadConfig(plugin));
    }

    private ClassSelectionGui(JavaPlugin plugin, Player player, ClassesModule module, ConfigWrapper config) {
        super(
            player,
            config.getConfig().getInt("size", 27) / 9,
            config.getConfig().getString("title", module.getMessage("gui.selection_title"))
        );
        this.plugin = plugin;
        this.module = module;
        this.config = config;
    }

    @Override
    public void initializeItems() {
        try {
            ConfigurationSection itemsSection = config.getConfig().getConfigurationSection("items");
            if (itemsSection == null) {
                MidgardLogger.warn("Seção 'items' não encontrada em selection.yml");
                return;
            }

            me.ray.midgard.core.gui.MenuLoader.loadItems(player, inventory, itemsSection, actions, null);
        } catch (Exception e) {
             MidgardLogger.error("Erro ao inicializar itens da GUI de seleção de classe", e);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return;
        }

        try {
            String action = actions.get(event.getSlot());
            if (action == null) {
                return;
            }
    
            Player player = (Player) event.getWhoClicked();
            
            // Disable player selection if they already have a class (Admin set only)
            // Unless this menu is specifically for changing classes (which we are disabling for players)
            // But initial selection is fine.
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
            if (profile != null) {
                ClassData data = profile.getData(ClassData.class);
                if (data != null && data.hasClass() && !player.hasPermission("midgard.admin.class")) {
                    MessageUtils.send(player, module.getMessage("class.gui.already_set"));
                    return;
                }
            }
    
            if (action.startsWith("select_class:")) {
                String classId = action.split(":")[1].trim();
                selectClass(player, classId);
            }
        } catch (Exception e) {
             MidgardLogger.error("Erro ao processar clique na GUI de seleção de classe", e);
             MessageUtils.send(player, module.getMessage("class.gui.error_select"));
        }
    }

    @Override
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        try {
            Player player = (Player) event.getPlayer();
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
    
            if (profile != null) {
                ClassData data = profile.getData(ClassData.class);
                // If data is null or className is null, they haven't selected a class
                if (data == null || data.getClassName() == null) {
                    if (reopenCount >= MAX_REOPEN_ATTEMPTS) {
                        return;
                    }
                    reopenCount++;
                    // Reopen in next tick to avoid event conflicts
                    me.ray.midgard.core.utils.Task.syncLater(player, () -> {
                        if (player.isOnline()) {
                            // Re-open same instance to preserve reopenCount
                            try {
                                open();
                            } catch (Exception e) {
                                MidgardLogger.error("Erro ao reabrir seleção de classe", e);
                            }
                        }
                    }, 1L);
                }
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao fechar GUI de seleção de classe", e);
        }
    }

    private void selectClass(Player player, String classId) {
        try {
            if (module.getClassManager() == null) {
                MessageUtils.send(player, module.getMessage("class.gui.error_manager"));
                return;
            }
            RPGClass rpgClass = module.getClassManager().getClass(classId);
            if (rpgClass == null) {
                String errorMsg = module.getMessage("errors.class_not_found")
                    .replace("%class%", classId);
                MessageUtils.send(player, errorMsg);
                return;
            }
    
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
            if (profile == null) {
                return;
            }
    
            ClassData data = profile.getOrCreateData(ClassData.class);

            // Remove all old spells/skills if changing class
            if (data.hasClass()) {
                me.ray.midgard.core.skill.SkillProvider skillProvider = me.ray.midgard.core.MidgardCore.getSkillProvider();
                if (skillProvider != null) {
                    skillProvider.removeAllSkills(profile);
                }
            }
            
            // Logic to set class
            data.setClassName(classId);
            data.setLevel(1);
            data.setExperience(0);
            
            module.applyClassAttributes(profile, rpgClass, 1);
            
            // Unlock level 1 skills for this class
            java.util.List<ClassSkillLink> skills = rpgClass.getSkills();
            if (skills != null) {
                for (ClassSkillLink link : skills) {
                    link.tryUnlock(profile, 1);
                }
            }
            
            // Send selection success message
            String successMsg = module.getMessage("class.selected")
                .replace("%class%", rpgClass.getDisplayName())
                .replace("%class_name%", rpgClass.getDisplayName());
            MessageUtils.send(player, successMsg);
            
            // Send welcome message with class info
            String infoMsg = module.getMessage("class.welcome")
                .replace("%class%", rpgClass.getDisplayName())
                .replace("%class_name%", rpgClass.getDisplayName())
                .replace("%description%", rpgClass.getLore() != null && !rpgClass.getLore().isEmpty() ? 
                    String.join(" ", rpgClass.getLore()) : module.getMessage("gui.default_class_description"));
            MessageUtils.send(player, infoMsg);
            
            // Persistir imediatamente
            MidgardCore.getProfileManager().saveProfile(profile);

            player.closeInventory();
        } catch (Exception e) {
            MidgardLogger.error("Erro ao selecionar classe " + classId, e);
            MessageUtils.send(player, module.getMessage("class.gui.error_confirm"));
        }
    }
}

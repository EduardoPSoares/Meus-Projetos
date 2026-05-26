package me.ray.midgard.modules.classes;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.text.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Comando administrativo para gerenciamento de classes.
 * <p>
 * Permite definir a classe de um jogador manualmente.
 * Uso: /rpg class set <classe>
 */
public class ClassCommand extends MidgardCommand {

    private final ClassesModule module;

    /**
     * Construtor do ClassCommand.
     *
     * @param module Instância do módulo de classes.
     */
    public ClassCommand(ClassesModule module) {
        super("class", "midgard.admin.class", true);
        this.module = module;
    }

    @Override
    public List<String> getAliases() {
        return List.of("classes");
    }

    @Override
    public String getDescription() {
        return module.getMessage("command.class_description");
    }

    @Override
    public String getUsage() {
        return module.getMessage("command.class_usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String sub = args[0].toLowerCase();
        
        switch (sub) {
            case "set":
                if (!sender.hasPermission("midgard.admin.class.set")) {
                    MessageUtils.send(sender, module.getMessage("errors.no_permission"));
                    return;
                }
                handleSet(sender, args);
                break;
            case "list":
                if (!sender.hasPermission("midgard.admin.class.list")) {
                    MessageUtils.send(sender, module.getMessage("errors.no_permission"));
                    return;
                }
                handleList(sender);
                break;
            case "help":
            default:
                sendHelp(sender);
                break;
        }
    }

    /**
     * Unified set command: /class set <player> <class> [level]
     * Sets the player's class and optionally their level (defaults to 1).
     */
    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) { // /class set <player> <class> [level]
            MessageUtils.send(sender, module.getMessage("commands.usage_set"));
            return;
        }

        Player target = org.bukkit.Bukkit.getPlayer(args[1]);
        if (target == null) {
             MessageUtils.send(sender, module.getMessage("errors.player_not_found").replace("%player%", args[1]));
             return;
        }

        String classId = args[2].toLowerCase();
        RPGClass rpgClass = module.getClassManager().getClass(classId);

        if (rpgClass == null) {
            MessageUtils.send(sender, module.getMessage("errors.class_not_found")
                .replace("%class%", classId));
            return;
        }

        // Parse optional level (default 1)
        int level = 1;
        if (args.length >= 4) {
            try {
                level = Integer.parseInt(args[3]);
                if (level < 1) {
                    level = 1;
                }
            } catch (NumberFormatException e) {
                MessageUtils.send(sender, module.getMessage("errors.invalid_number"));
                return;
            }
        }

        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(target);
        if (profile == null) {
            MessageUtils.send(sender, module.getMessage("errors.profile_error"));
            return;
        }

        ClassData data = profile.getOrCreateData(ClassData.class);

        // Remove all old spells/skills before changing class
        me.ray.midgard.core.skill.SkillProvider skillProvider = MidgardCore.getSkillProvider();
        if (skillProvider != null) {
            skillProvider.removeAllSkills(profile);
        }
        
        data.setClassName(classId);
        data.setLevel(level);
        data.setExperience(0);
        data.getSpentPoints().clear(); // Reset spent attribute points on class change

        // Sync CombatData (fonte de verdade do LevelManager)
        me.ray.midgard.modules.combat.CombatData combatData = profile.getOrCreateData(me.ray.midgard.modules.combat.CombatData.class);
        combatData.setLevel(level);
        combatData.setExperience(0);
        
        // Sync vanilla level
        target.setLevel(level);
        target.setExp(0);
        
        // Apply attributes
        module.applyClassAttributes(profile, rpgClass, level);

        // Refresh derived stat scaling (STR→dmg, VIT→hp, INT→mana, etc.)
        me.ray.midgard.modules.combat.listener.StatScalingListener.updateStats(target);

        // Fill resources to full so the player doesn't see a false "damage" effect
        me.ray.midgard.modules.combat.CombatManager cm = me.ray.midgard.modules.combat.CombatManager.getInstance();
        if (cm != null) {
            cm.fillResources(target);
        }

        // Unlock skills for this level
        java.util.List<ClassSkillLink> skills = rpgClass.getSkills();
        if (skills != null) {
            for (ClassSkillLink link : skills) {
                link.tryUnlock(profile, level);
            }
        }
        
        // Persistir imediatamente para evitar perda de dados em quit rápido
        MidgardCore.getProfileManager().saveProfile(profile);

        // Notify admin
        MessageUtils.send(sender, module.getMessage("admin.class_set_success")
             .replace("%player%", target.getName())
             .replace("%class%", rpgClass.getDisplayName())
             .replace("%level%", String.valueOf(level)));

        // Notify player
        MessageUtils.send(target, module.getMessage("class.selected")
             .replace("%class_name%", rpgClass.getDisplayName())
             .replace("%level%", String.valueOf(level)));
    }
    
    private void handleList(CommandSender sender) {
        MessageUtils.send(sender, module.getMessage("commands.list_header"));
        for (RPGClass c : module.getClassManager().getClasses().values()) {
             MessageUtils.send(sender, module.getMessage("commands.list_item")
                 .replace("%name%", c.getDisplayName())
                 .replace("%id%", c.getId()));
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, module.getMessage("help.title"));
        MessageUtils.send(sender, "");
        for (String line : module.getMessageList("help.lines")) {
            MessageUtils.send(sender, line);
        }
        MessageUtils.send(sender, "");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("midgard.admin.class")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], java.util.Arrays.asList("set", "list", "help"), new ArrayList<>());
        } 
        
        if (args.length == 2) {
             if (args[0].equalsIgnoreCase("set")) {
                 return StringUtil.copyPartialMatches(args[1], onlinePlayers(), new ArrayList<>());
             }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return StringUtil.copyPartialMatches(args[2], module.getClassManager().getClasses().keySet(), new ArrayList<>());
        }
        
        if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
            return StringUtil.copyPartialMatches(args[3], java.util.Arrays.asList("1", "5", "10", "20", "50"), new ArrayList<>());
        }
        
        return Collections.emptyList();
    }
}

package me.ray.midgard.modules.spells.command;

import me.ray.midgard.core.MidgardCore; // Added import
import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.data.SpellProfile;
import me.ray.midgard.modules.spells.gui.MainSpellGUI;
import me.ray.midgard.modules.spells.gui.SpellUpgradeListGUI;
import me.ray.midgard.modules.spells.gui.SpellUpgradeDetailGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpellCommand extends MidgardCommand {

    private SpellsModule module;
    private final List<MidgardCommand> subCommands = new ArrayList<>();

    public SpellCommand(SpellsModule module) {
        // Name: "spell", Permission: null (for now), PlayerOnly: true
        super("spell", null, true);
        this.module = module;
    }

    @Override
    public List<String> getAliases() {
        return List.of("spells", "skills");
    }

    @Override
    public String getDescription() {
        SpellsModule m = getModule();
        return m != null ? m.getMessage("commands.description") : "Gerencia magias e habilidades do jogador";
    }

    @Override
    public String getUsage() {
        SpellsModule m = getModule();
        return m != null ? m.getMessage("commands.usage") : "/rpg spell [bind|combo|list|info|learn|unlearn]";
    }

    public void addSubCommand(MidgardCommand cmd) {
        subCommands.add(cmd);
    }
    
    private SpellsModule getModule() {
        if (module != null && module.getSpellManager() != null) {
            return module;
        }
        // Try to recover module reference
        try {
            return (SpellsModule) MidgardCore.getModuleManager().getModule("Spells");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        SpellsModule m = getModule();
        if (m == null) { return Collections.emptyList(); }

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("help");
            options.add("bind");
            options.add("combo");
            options.add("setlevel");
            options.add("info");
            options.add("learn");
            options.add("unlearn");
            options.add("givescroll");
            options.add("top");
            options.add("upgrade");
            
            for (MidgardCommand sub : subCommands) {
                options.add(sub.getName());
            }
            return StringUtil.copyPartialMatches(args[0], options, new ArrayList<>());
        }
        
        // Delegate to subcommand
        String subName = args[0];
        for (MidgardCommand cmd : subCommands) {
            if (cmd.getName().equalsIgnoreCase(subName)) {
                String[] newArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
                // Manually check permission before offering tab completion
                if (cmd.getPermission() != null && !sender.hasPermission(cmd.getPermission())) {
                    return Collections.emptyList();
                }
                return cmd.tabComplete(sender, newArgs);
            }
        }
        
        if (args[0].equalsIgnoreCase("setlevel")) {
            if (args.length == 2) {
                return StringUtil.copyPartialMatches(args[1], new ArrayList<>(m.getSpellManager().getLoadedSpellIds()), new ArrayList<>());
            }
             if (args.length == 3) {
                 List<String> levels = java.util.Arrays.asList("1", "2", "3", "4", "5", "10");
                 return StringUtil.copyPartialMatches(args[2], levels, new ArrayList<>());
             }
        }
        
        if (args[0].equalsIgnoreCase("info")) {
            if (args.length == 2) {
                return StringUtil.copyPartialMatches(args[1], new ArrayList<>(m.getSpellManager().getLoadedSpellIds()), new ArrayList<>());
            }
        }
        
        if (args[0].equalsIgnoreCase("bind")) {
             if (args.length == 2) {
                 List<String> slots = java.util.Arrays.asList("1", "2", "3", "4");
                 return StringUtil.copyPartialMatches(args[1], slots, new ArrayList<>());
             }
             if (args.length == 3) {
                 return StringUtil.copyPartialMatches(args[2], new ArrayList<>(m.getSpellManager().getLoadedSpellIds()), new ArrayList<>());
             }
        }
        
        if (args[0].equalsIgnoreCase("combo")) {
              if (args.length == 2) {
                 List<String> suggestions = java.util.Arrays.asList("L", "R", "LL", "RR", "RL", "LR");
                 return StringUtil.copyPartialMatches(args[1], suggestions, new ArrayList<>());
              }
              if (args.length == 3) {
                  return StringUtil.copyPartialMatches(args[2], new ArrayList<>(m.getSpellManager().getLoadedSpellIds()), new ArrayList<>());
              }
        }

        if (args[0].equalsIgnoreCase("learn") || args[0].equalsIgnoreCase("unlearn")) {
            if (args.length == 2) {
                return StringUtil.copyPartialMatches(args[1], new ArrayList<>(m.getSpellManager().getLoadedSpellIds()), new ArrayList<>());
            }
            if (args.length == 3) {
                return StringUtil.copyPartialMatches(args[2], Bukkit.getOnlinePlayers().stream().map(org.bukkit.entity.Player::getName).collect(java.util.stream.Collectors.toList()), new ArrayList<>());
            }
        }

        if (args[0].equalsIgnoreCase("givescroll")) {
            if (args.length == 2) {
                List<String> types = java.util.Arrays.asList("unlearning", "learning", "respec");
                return StringUtil.copyPartialMatches(args[1], types, new ArrayList<>());
            }
            if (args.length == 3) {
                return StringUtil.copyPartialMatches(args[2], new ArrayList<>(m.getSpellManager().getLoadedSpellIds()), new ArrayList<>());
            }
            if (args.length == 4) {
                return StringUtil.copyPartialMatches(args[3], Bukkit.getOnlinePlayers().stream().map(org.bukkit.entity.Player::getName).collect(java.util.stream.Collectors.toList()), new ArrayList<>());
            }
        }

        if (args[0].equalsIgnoreCase("top")) {
            if (args.length == 2) {
                List<String> types = java.util.Arrays.asList("casts", "mastery", "level");
                return StringUtil.copyPartialMatches(args[1], types, new ArrayList<>());
            }
        }

        if (args[0].equalsIgnoreCase("upgrade")) {
            if (args.length == 2) {
                return StringUtil.copyPartialMatches(args[1], new ArrayList<>(m.getSpellManager().getLoadedSpellIds()), new ArrayList<>());
            }
        }

        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        SpellsModule m = getModule();
        
        // Handle Subcommands
        if (args.length > 0) {
            String sub = args[0];
            for (MidgardCommand cmd : subCommands) {
                if (cmd.getName().equalsIgnoreCase(sub)) {
                    if (cmd.getPermission() != null && !cmd.getPermission().isEmpty() && !sender.hasPermission(cmd.getPermission())) {
                        String noPermMsg = m != null ? m.getMessage("errors.no_permission") : "<red>✖</red> <gray>sᴇᴍ ᴘᴇʀᴍɪssᴀᴏ.</gray>";
                        me.ray.midgard.core.text.MessageUtils.send(sender, noPermMsg);
                        return;
                    }
                    // Shift args
                    String[] newArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
                    cmd.execute(sender, newArgs);
                    return;
                }
            }
        }
        
        Player player = (Player) sender;
        if (m == null || m.getSpellManager() == null) {
            String errorMsg = m != null ? m.getMessage("errors.system_error") : "<red>\u2716</red> <gray>\ua731\u026a\ua731\u1d1b\u1d07\u1d0d\u1d00 \u026a\u0274\u1d05\u026a\ua731\u1d18\u1d0f\u0274\u026a\u1d20\u1d07\u029f.</gray>";
            me.ray.midgard.core.text.MessageUtils.send(player, errorMsg);
            return;
        }

        if (args.length == 0) {
            new MainSpellGUI(player, m).open();
            return;
        }

        if (args[0].equalsIgnoreCase("help")) {
            for (String line : m.getMessageList("commands.help_lines")) {
                me.ray.midgard.core.text.MessageUtils.send(player, line);
            }
            return;
        }

        if (args[0].equalsIgnoreCase("upgrade")) {
            if (args.length >= 2) {
                // /spell upgrade <spellId> â€” abre direto o detalhe da spell
                String spellId = args[1];
                me.ray.midgard.modules.spells.obj.Spell spell = m.getSpellManager().getSpell(spellId);
                if (spell == null) {
                    String notFoundMsg = m.getMessage("errors.spell_not_found").replace("%spell%", spellId);
                    me.ray.midgard.core.text.MessageUtils.send(player, notFoundMsg);
                    return;
                }
                SpellProfile upgradeProfile = m.getSpellManager().getProfile(player);
                if (upgradeProfile == null) {
                    me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("errors.profile_not_loaded"));
                    return;
                }
                if (!upgradeProfile.hasSpell(spellId)) {
                    me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("errors.spell_not_found").replace("%spell%", spellId));
                    return;
                }
                new SpellUpgradeDetailGUI(player, m, spell).open();
            } else {
                // /spell upgrade â€” abre a lista
                new SpellUpgradeListGUI(player, m).open();
            }
            return;
        }

        if (args[0].equalsIgnoreCase("bind")) {
            if (args.length < 3) {
                me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("commands.usage_bind"));
                return;
            }
            try {
                int slot = Integer.parseInt(args[1]);
                String spellId = args[2];
                
                if (m.getSpellManager().getSpell(spellId) == null) {
                    String notFoundMsg = m.getMessage("errors.spell_not_found")
                        .replace("%spell%", spellId);
                    me.ray.midgard.core.text.MessageUtils.send(player, notFoundMsg);
                    return;
                }

                SpellProfile profile = m.getSpellManager().getProfile(player);
                if (profile == null) {
                   me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("errors.profile_not_loaded"));
                   return;
                }

                if (!sender.hasPermission("midgard.admin") && !profile.hasSpell(spellId)) {
                    me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("errors.spell_not_found").replace("%spell%", spellId));
                    return;
                }
                
                profile.setSkillBarSlot(slot, spellId);
                // Persistir imediatamente
                me.ray.midgard.core.profile.MidgardProfile coreProfile = me.ray.midgard.core.MidgardCore.getProfileManager().getProfile(player);
                if (coreProfile != null) {
                    me.ray.midgard.core.MidgardCore.getProfileManager().saveProfile(coreProfile);
                }
                String boundMsg = m.getMessage("commands.spell_bound")
                    .replace("%spell%", spellId)
                    .replace("%slot%", String.valueOf(slot));
                me.ray.midgard.core.text.MessageUtils.send(player, boundMsg);

            } catch (NumberFormatException e) {
                me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("errors.invalid_slot"));
            }
            return;
        }

        if (args[0].equalsIgnoreCase("combo")) {
            if (args.length < 3) {
                me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("commands.usage_combo"));
                return;
            }
            String combo = args[1].toUpperCase();
            String spellId = args[2];

            if (m.getSpellManager().getSpell(spellId) == null) {
                String notFoundMsg = m.getMessage("errors.spell_not_found")
                    .replace("%spell%", spellId);
                me.ray.midgard.core.text.MessageUtils.send(player, notFoundMsg);
                return;
            }

            SpellProfile profile = m.getSpellManager().getProfile(player);
            if (profile == null) {
                me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("errors.profile_not_loaded"));
                return;
            }

            if (!sender.hasPermission("midgard.admin") && !profile.hasSpell(spellId)) {
                me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("errors.spell_not_found").replace("%spell%", spellId));
                return;
            }

            profile.setComboLegacy(combo, spellId);
            // Persistir imediatamente
            me.ray.midgard.core.profile.MidgardProfile coreProfile = me.ray.midgard.core.MidgardCore.getProfileManager().getProfile(player);
            if (coreProfile != null) {
                me.ray.midgard.core.MidgardCore.getProfileManager().saveProfile(coreProfile);
            }
            String comboMsg = m.getMessage("commands.combo_bound")
                .replace("%spell%", spellId)
                .replace("%combo%", combo);
            me.ray.midgard.core.text.MessageUtils.send(player, comboMsg);
            return;
        }

        if (args[0].equalsIgnoreCase("setlevel")) {
            // Usage: /spell setlevel <spell> <level> [player]
            if (!sender.hasPermission("midgard.admin.spell.setlevel")) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("errors.no_permission"));
                return;
            }
            if (args.length < 3) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("commands.usage_setlevel"));
                return;
            }

            String spellId = args[1];
            if (m.getSpellManager().getSpell(spellId) == null) {
                String msg = m.getMessage("errors.spell_not_found").replace("%spell%", spellId);
                me.ray.midgard.core.text.MessageUtils.send(sender, msg);
                return;
            }

            int level;
            try {
                level = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("errors.invalid_level"));
                return;
            }

            if (level < 1) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("errors.invalid_level"));
                return;
            }

            me.ray.midgard.modules.spells.obj.Spell spell = m.getSpellManager().getSpell(spellId);
            if (level > spell.getMaxLevel()) {
                String msg = m.getMessage("commands.level_exceeds_max")
                        .replace("%max%", String.valueOf(spell.getMaxLevel()))
                        .replace("%spell%", spell.getDisplayName());
                me.ray.midgard.core.text.MessageUtils.send(sender, msg);
                return;
            }

            Player target = player;
            if (args.length > 3) {
                target = org.bukkit.Bukkit.getPlayer(args[3]);
                if (target == null) {
                    me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("errors.player_not_found"));
                    return;
                }
            }

            SpellProfile profile = m.getSpellManager().getProfile(target);
            if (profile != null) {
                profile.setSpellLevel(spellId, level);
                // Persistir imediatamente
                me.ray.midgard.core.profile.MidgardProfile coreProfile = me.ray.midgard.core.MidgardCore.getProfileManager().getProfile(target);
                if (coreProfile != null) {
                    me.ray.midgard.core.MidgardCore.getProfileManager().saveProfile(coreProfile);
                }
                String msg = m.getMessage("commands.setlevel_success")
                        .replace("%spell%", spellId)
                        .replace("%level%", String.valueOf(level))
                        .replace("%target%", target.getName());
                me.ray.midgard.core.text.MessageUtils.send(sender, msg);
            }
            return;
        }
        
        if (args[0].equalsIgnoreCase("info")) {
            if (args.length < 2) {
                me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("commands.usage_info"));
                return;
            }
            
            String spellId = args[1];
            me.ray.midgard.modules.spells.obj.Spell spell = m.getSpellManager().getSpell(spellId);
            if (spell == null) {
                String msg = m.getMessage("errors.spell_not_found").replace("%spell%", spellId);
                me.ray.midgard.core.text.MessageUtils.send(player, msg);
                return;
            }
            
            SpellProfile profile = m.getSpellManager().getProfile(player);
            // Default to level 1 if no profile/not mapped logic
            int level = (profile != null) ? profile.getSpellLevel(spellId) : 1;
            
            String header = m.getMessage("commands.info.header").replace("%spell%", spell.getDisplayName());
            me.ray.midgard.core.text.MessageUtils.send(player, header);
            
            String levelMsg = m.getMessage("commands.info.current_level").replace("%level%", String.valueOf(level));
            me.ray.midgard.core.text.MessageUtils.send(player, levelMsg);
             
            // Calculate attributes
            double mana = spell.getManaCost().calculate(level);
            double cooldown = spell.getCooldown().calculate(level);
            java.text.DecimalFormat df = new java.text.DecimalFormat("0.#");
            
            String manaMsg = m.getMessage("commands.info.mana_cost").replace("%value%", df.format(mana));
            me.ray.midgard.core.text.MessageUtils.send(player, manaMsg);
            
            String cdMsg = m.getMessage("commands.info.cooldown").replace("%value%", df.format(cooldown));
            me.ray.midgard.core.text.MessageUtils.send(player, cdMsg);
            
            me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("commands.info.variables_header"));
            for (java.util.Map.Entry<String, Object> entry : spell.getVariables().entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                
                if (val instanceof me.ray.midgard.modules.spells.obj.ScalableAttribute attribute) {
                     double calculated = attribute.calculate(level);
                     String msg = m.getMessage("commands.info.variable_format")
                         .replace("%key%", key)
                         .replace("%value%", df.format(calculated))
                         .replace("%base%", df.format(attribute.base()));
                     me.ray.midgard.core.text.MessageUtils.send(player, msg);
                } else {
                    String msg = m.getMessage("commands.info.variable_simple")
                        .replace("%key%", key)
                        .replace("%value%", val.toString());
                    me.ray.midgard.core.text.MessageUtils.send(player, msg);
                }
            }
            return;
        }

        if (args[0].equalsIgnoreCase("learn")) {
            // Usage: /spell learn <spell> [player]
            if (!sender.hasPermission("midgard.admin.spell.learn")) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("errors.no_permission"));
                return;
            }
            if (args.length < 2) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("commands.usage_learn"));
                return;
            }

            String spellId = args[1];
            me.ray.midgard.modules.spells.obj.Spell spell = m.getSpellManager().getSpell(spellId);
            if (spell == null) {
                String msg = m.getMessage("errors.spell_not_found").replace("%spell%", spellId);
                me.ray.midgard.core.text.MessageUtils.send(sender, msg);
                return;
            }

            Player target = player;
            if (args.length > 2) {
                target = org.bukkit.Bukkit.getPlayer(args[2]);
                if (target == null) {
                    me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("errors.player_not_found"));
                    return;
                }
            }

            SpellProfile profile = m.getSpellManager().getProfile(target);
            if (profile == null) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("errors.profile_not_loaded"));
                return;
            }

            if (profile.hasSpell(spellId)) {
                String msg = m.getMessage("commands.already_learned")
                        .replace("%spell%", spell.getDisplayName())
                        .replace("%target%", target.getName());
                me.ray.midgard.core.text.MessageUtils.send(sender, msg);
                return;
            }

            profile.unlockSpell(spellId);
            // Persistir imediatamente
            me.ray.midgard.core.profile.MidgardProfile coreProfile = me.ray.midgard.core.MidgardCore.getProfileManager().getProfile(target);
            if (coreProfile != null) {
                me.ray.midgard.core.MidgardCore.getProfileManager().saveProfile(coreProfile);
            }
            String msg = m.getMessage("commands.learn_success")
                    .replace("%spell%", spell.getDisplayName())
                    .replace("%target%", target.getName());
            me.ray.midgard.core.text.MessageUtils.send(sender, msg);
            return;
        }

        if (args[0].equalsIgnoreCase("unlearn")) {
            // Usage: /spell unlearn <spell> [player]
            if (!sender.hasPermission("midgard.admin.spell.unlearn")) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("errors.no_permission"));
                return;
            }
            if (args.length < 2) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("commands.usage_unlearn"));
                return;
            }

            String spellId = args[1];
            me.ray.midgard.modules.spells.obj.Spell spell = m.getSpellManager().getSpell(spellId);
            if (spell == null) {
                String msg = m.getMessage("errors.spell_not_found").replace("%spell%", spellId);
                me.ray.midgard.core.text.MessageUtils.send(sender, msg);
                return;
            }

            Player target = player;
            if (args.length > 2) {
                target = org.bukkit.Bukkit.getPlayer(args[2]);
                if (target == null) {
                    me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("errors.player_not_found"));
                    return;
                }
            }

            SpellProfile profile = m.getSpellManager().getProfile(target);
            if (profile == null) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("errors.profile_not_loaded"));
                return;
            }

            if (!profile.hasSpell(spellId)) {
                String msg = m.getMessage("commands.not_learned")
                        .replace("%spell%", spell.getDisplayName())
                        .replace("%target%", target.getName());
                me.ray.midgard.core.text.MessageUtils.send(sender, msg);
                return;
            }

            m.getSpellManager().removeSpellMasteryModifiers(target, spellId);
            profile.unlearnSpell(spellId);
            // Persistir imediatamente
            me.ray.midgard.core.profile.MidgardProfile coreProfile = me.ray.midgard.core.MidgardCore.getProfileManager().getProfile(target);
            if (coreProfile != null) {
                me.ray.midgard.core.MidgardCore.getProfileManager().saveProfile(coreProfile);
            }
            String msg = m.getMessage("commands.unlearn_success")
                    .replace("%spell%", spell.getDisplayName())
                    .replace("%target%", target.getName());
            me.ray.midgard.core.text.MessageUtils.send(sender, msg);
            return;
        }

        if (args[0].equalsIgnoreCase("givescroll")) {
            if (!sender.hasPermission("midgard.admin.spell.givescroll")) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("errors.no_permission"));
                return;
            }
            if (args.length < 2) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("commands.usage_givescroll"));
                return;
            }

            me.ray.midgard.modules.spells.manager.ScrollManager.ScrollType scrollType =
                    me.ray.midgard.modules.spells.manager.ScrollManager.ScrollType.fromString(args[1]);
            if (scrollType == null) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("commands.invalid_scroll_type"));
                return;
            }

            String targetSpell = args.length > 2 ? args[2] : null;
            if (targetSpell != null && m.getSpellManager().getSpell(targetSpell) == null) {
                me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("errors.spell_not_found").replace("%spell%", targetSpell));
                return;
            }

            Player target = player;
            if (args.length > 3) {
                target = org.bukkit.Bukkit.getPlayer(args[3]);
                if (target == null) {
                    me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("errors.player_not_found"));
                    return;
                }
            }

            org.bukkit.inventory.ItemStack scroll = m.getScrollManager().createScroll(scrollType, targetSpell);
            target.getInventory().addItem(scroll);

            me.ray.midgard.core.text.MessageUtils.send(sender, m.getMessage("commands.scroll_given")
                    .replace("%type%", scrollType.name().toLowerCase())
                    .replace("%target%", target.getName()));
            return;
        }

        if (args[0].equalsIgnoreCase("top")) {
            String type = args.length > 1 ? args[1].toLowerCase() : "casts";
            String leaderboard;
            switch (type) {
                case "mastery" -> leaderboard = "spell_mastery_count";
                case "level" -> leaderboard = "spell_total_levels";
                default -> leaderboard = "spell_total_casts";
            }

            try {
                me.ray.midgard.core.leaderboard.LeaderboardManager leaderboardManager =
                        me.ray.midgard.core.MidgardCore.getLeaderboardManager();
                if (leaderboardManager == null) {
                    me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("errors.system_error"));
                    return;
                }

                leaderboardManager.getTop(leaderboard, 10).thenAccept(entries -> {
                    me.ray.midgard.core.utils.Task.sync(player, () -> {
                        me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("leaderboard.header")
                                .replace("%type%", type));
                        int rank = 1;
                        for (redis.clients.jedis.resps.Tuple entry : entries) {
                            me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("leaderboard.entry")
                                    .replace("%rank%", String.valueOf(rank))
                                    .replace("%player%", entry.getElement())
                                    .replace("%value%", String.valueOf((int) entry.getScore())));
                            rank++;
                        }
                    });
                });
            } catch (Exception e) {
                me.ray.midgard.core.text.MessageUtils.send(player, m.getMessage("errors.system_error"));
            }
            return;
        }
    }
}

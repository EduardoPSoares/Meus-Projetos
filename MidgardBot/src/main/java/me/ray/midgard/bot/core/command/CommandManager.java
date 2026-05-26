package me.ray.midgard.bot.core.command;

import me.ray.midgard.bot.MidgardBot;
import me.ray.midgard.bot.core.embed.EmbedFactory;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandManager {

    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);

    private final MidgardBot bot;
    private final Map<String, BaseCommand> commands = new LinkedHashMap<>();
    private final Map<String, Map<String, Method>> subCommandMethods = new HashMap<>();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public CommandManager(MidgardBot bot) {
        this.bot = bot;
    }

    public void registerCommand(BaseCommand command) {
        SlashCommand annotation = command.getClass().getAnnotation(SlashCommand.class);
        if (annotation == null) {
            logger.warn("Command class {} is missing @SlashCommand annotation", command.getClass().getSimpleName());
            return;
        }

        command.init(bot);
        commands.put(annotation.name(), command);

        // Index subcommand methods
        Map<String, Method> subs = new HashMap<>();
        for (Method method : command.getClass().getDeclaredMethods()) {
            SubCommand sub = method.getAnnotation(SubCommand.class);
            if (sub != null) {
                String key = sub.group().isEmpty() ? sub.name() : sub.group() + ":" + sub.name();
                method.setAccessible(true);
                subs.put(key, method);
            }
        }
        if (!subs.isEmpty()) {
            subCommandMethods.put(annotation.name(), subs);
        }

        logger.info("Registered command: /{} ({} subcommands)", annotation.name(), subs.size());
    }

    public void registerCommands(BaseCommand... commands) {
        for (BaseCommand cmd : commands) {
            registerCommand(cmd);
        }
    }

    public void syncCommands() {
        List<SlashCommandData> commandDataList = new ArrayList<>();
        for (BaseCommand cmd : commands.values()) {
            commandDataList.add(cmd.buildCommandData());
        }

        bot.getJda().updateCommands()
                .addCommands(commandDataList)
                .queue(
                        cmds -> logger.info("Synced {} slash commands globally", cmds.size()),
                        err -> logger.error("Failed to sync slash commands", err)
                );
    }

    public void syncCommandsToGuild(long guildId) {
        var guild = bot.getJda().getGuildById(guildId);
        if (guild == null) {
            logger.warn("Guild {} not found for command sync", guildId);
            return;
        }

        List<SlashCommandData> commandDataList = new ArrayList<>();
        for (BaseCommand cmd : commands.values()) {
            commandDataList.add(cmd.buildCommandData());
        }

        guild.updateCommands()
                .addCommands(commandDataList)
                .queue(
                        cmds -> logger.info("Synced {} slash commands to guild {}", cmds.size(), guild.getName()),
                        err -> logger.error("Failed to sync slash commands to guild {}", guildId, err)
                );
    }

    public void handleCommand(SlashCommandInteractionEvent event) {
        String name = event.getName();
        BaseCommand command = commands.get(name);

        if (command == null) {
            event.reply("❌ Comando desconhecido.").setEphemeral(true).queue();
            return;
        }

        SlashCommand annotation = command.getAnnotation();

        // Guild-only check
        if (annotation.guildOnly() && !event.isFromGuild()) {
            event.reply("❌ Este comando só pode ser usado em servidores.").setEphemeral(true).queue();
            return;
        }

        // Permission check
        if (annotation.permissions().length > 0 && event.getMember() != null) {
            Member member = event.getMember();
            for (Permission perm : annotation.permissions()) {
                if (!member.hasPermission(perm)) {
                    event.reply("❌ Você não tem permissão para usar este comando.\nPermissão necessária: `" + perm.getName() + "`")
                            .setEphemeral(true).queue();
                    return;
                }
            }
        }

        // Cooldown check
        if (annotation.cooldown() > 0) {
            String cooldownKey = event.getUser().getId() + ":" + name;
            Long expiry = cooldowns.get(cooldownKey);
            if (expiry != null && System.currentTimeMillis() < expiry) {
                long remaining = (expiry - System.currentTimeMillis()) / 1000;
                event.reply("⏳ Aguarde **" + remaining + "s** antes de usar este comando novamente.")
                        .setEphemeral(true).queue();
                return;
            }
            cooldowns.put(cooldownKey, System.currentTimeMillis() + (annotation.cooldown() * 1000));
        }

        CommandContext ctx = new CommandContext(event, bot);

        try {
            // Check for subcommand
            String subName = event.getSubcommandName();
            if (subName != null) {
                Map<String, Method> subs = subCommandMethods.get(name);
                if (subs != null) {
                    String group = event.getSubcommandGroup();
                    String key = group != null ? group + ":" + subName : subName;
                    Method method = subs.get(key);
                    if (method != null) {
                        method.invoke(command, ctx);
                        return;
                    }
                }
            }

            // Fallback to main execute
            command.execute(ctx);
        } catch (Exception e) {
            logger.error("Error executing command /{}", name, e);
            if (!event.isAcknowledged()) {
                event.reply("❌ Ocorreu um erro ao executar o comando.").setEphemeral(true).queue();
            } else {
                event.getHook().editOriginal("❌ Ocorreu um erro ao executar o comando.").queue();
            }
        }
    }

    public void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        BaseCommand command = commands.get(event.getName());
        if (command != null) {
            try {
                command.onAutoComplete(event);
            } catch (Exception e) {
                logger.error("Error handling autocomplete for /{}", event.getName(), e);
            }
        }
    }

    public void clearCooldown(String userId, String command) {
        cooldowns.remove(userId + ":" + command);
    }

    public void clearAllCooldowns() {
        cooldowns.clear();
    }

    public BaseCommand getCommand(String name) {
        return commands.get(name);
    }

    public Collection<BaseCommand> getCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }

    public List<BaseCommand> getCommandsByCategory(CommandCategory category) {
        List<BaseCommand> result = new ArrayList<>();
        for (BaseCommand cmd : commands.values()) {
            if (cmd.getCategory() == category) {
                result.add(cmd);
            }
        }
        return result;
    }

    public int getCommandCount() {
        return commands.size();
    }
}

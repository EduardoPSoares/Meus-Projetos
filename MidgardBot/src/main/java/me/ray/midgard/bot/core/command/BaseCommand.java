package me.ray.midgard.bot.core.command;

import me.ray.midgard.bot.MidgardBot;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.util.List;

public abstract class BaseCommand {

    protected MidgardBot bot;
    private SlashCommand annotation;

    public final void init(MidgardBot bot) {
        this.bot = bot;
        this.annotation = getClass().getAnnotation(SlashCommand.class);
    }

    public abstract void execute(CommandContext ctx);

    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        // Override to handle autocomplete
    }

    public SlashCommandData buildCommandData() {
        SlashCommand cmd = getAnnotation();
        SlashCommandData data = Commands.slash(cmd.name(), cmd.description());
        data.setGuildOnly(cmd.guildOnly());

        if (cmd.permissions().length > 0) {
            data.setDefaultPermissions(
                    net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(cmd.permissions())
            );
        }

        // Add options from class-level @CommandOption annotations
        CommandOption[] classOptions = getClassOptions();
        for (CommandOption opt : classOptions) {
            OptionData optData = new OptionData(opt.type(), opt.name(), opt.description(), opt.required());
            optData.setAutoComplete(opt.autocomplete());
            data.addOptions(optData);
        }

        // Scan for @SubCommand methods and build subcommand data
        buildSubCommands(data);

        return data;
    }

    private void buildSubCommands(SlashCommandData data) {
        java.util.Map<String, SubcommandGroupData> groups = new java.util.LinkedHashMap<>();

        for (var method : getClass().getDeclaredMethods()) {
            SubCommand sub = method.getAnnotation(SubCommand.class);
            if (sub == null) continue;

            SubcommandData subData = new SubcommandData(sub.name(), sub.description());

            // Add options from method-level @CommandOption
            CommandOption[] opts = method.getAnnotationsByType(CommandOption.class);
            for (CommandOption opt : opts) {
                OptionData optData = new OptionData(opt.type(), opt.name(), opt.description(), opt.required());
                optData.setAutoComplete(opt.autocomplete());
                subData.addOptions(optData);
            }

            if (!sub.group().isEmpty()) {
                groups.computeIfAbsent(sub.group(), g ->
                        new SubcommandGroupData(g, "Grupo " + g)
                ).addSubcommands(subData);
            } else {
                data.addSubcommands(subData);
            }
        }

        for (SubcommandGroupData group : groups.values()) {
            data.addSubcommandGroups(group);
        }
    }

    private CommandOption[] getClassOptions() {
        CommandOptions multi = getClass().getAnnotation(CommandOptions.class);
        if (multi != null) return multi.value();
        CommandOption single = getClass().getAnnotation(CommandOption.class);
        if (single != null) return new CommandOption[]{single};
        return new CommandOption[0];
    }

    // ==================== Accessors ====================

    public SlashCommand getAnnotation() {
        return annotation;
    }

    public String getName() {
        return annotation.name();
    }

    public String getDescription() {
        return annotation.description();
    }

    public CommandCategory getCategory() {
        return annotation.category();
    }

    public long getCooldown() {
        return annotation.cooldown();
    }

    public boolean isEphemeral() {
        return annotation.ephemeral();
    }
}

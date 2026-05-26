package me.ray.midgard.bot.core;

import me.ray.midgard.bot.MidgardBot;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(CoreListener.class);

    private final MidgardBot bot;

    public CoreListener(MidgardBot bot) {
        this.bot = bot;
    }

    @Override
    public void onReady(ReadyEvent event) {
        logger.info("Bot connected to {} guilds", event.getGuildAvailableCount());
        // Command sync is handled by BotInitializer after modules are loaded
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        logger.info("Bot shutdown event received");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        bot.getCommandManager().handleCommand(event);
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        bot.getCommandManager().handleAutoComplete(event);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        bot.getInteractionManager().handleButton(event);
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        bot.getInteractionManager().handleModal(event);
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        bot.getInteractionManager().handleSelectMenu(event);
    }
}

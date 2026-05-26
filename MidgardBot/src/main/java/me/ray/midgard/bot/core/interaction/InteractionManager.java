package me.ray.midgard.bot.core.interaction;

import me.ray.midgard.bot.MidgardBot;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InteractionManager {

    private static final Logger logger = LoggerFactory.getLogger(InteractionManager.class);

    private final ButtonHandler buttons;
    private final ModalHandler modals;
    private final SelectMenuHandler selectMenus;

    public InteractionManager(MidgardBot bot) {
        this.buttons = new ButtonHandler();
        this.modals = new ModalHandler();
        this.selectMenus = new SelectMenuHandler();
    }

    public ButtonHandler getButtons() { return buttons; }
    public ModalHandler getModals() { return modals; }
    public SelectMenuHandler getSelectMenus() { return selectMenus; }

    public void handleButton(ButtonInteractionEvent event) {
        if (!buttons.handle(event)) {
            logger.debug("Unhandled button interaction: {}", event.getComponentId());
        }
    }

    public void handleModal(ModalInteractionEvent event) {
        if (!modals.handle(event)) {
            logger.debug("Unhandled modal interaction: {}", event.getModalId());
        }
    }

    public void handleSelectMenu(StringSelectInteractionEvent event) {
        if (!selectMenus.handle(event)) {
            logger.debug("Unhandled select menu interaction: {}", event.getComponentId());
        }
    }

    public void cleanupExpired() {
        buttons.cleanupExpired();
    }
}

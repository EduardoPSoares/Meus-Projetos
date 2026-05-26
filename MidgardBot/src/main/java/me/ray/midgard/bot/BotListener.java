package me.ray.midgard.bot;

/**
 * @deprecated Use {@link me.ray.midgard.bot.core.CoreListener} instead.
 * This class is kept for backwards compatibility only.
 */
@Deprecated
public class BotListener extends net.dv8tion.jda.api.hooks.ListenerAdapter {

    private final MidgardBot bot;

    public BotListener(MidgardBot bot) {
        this.bot = bot;
    }
}

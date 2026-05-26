package me.ray.midgard.bot.core.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.dv8tion.jda.api.Permission;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SlashCommand {

    String name();

    String description();

    CommandCategory category() default CommandCategory.GENERAL;

    Permission[] permissions() default {};

    boolean guildOnly() default true;

    long cooldown() default 0;

    boolean ephemeral() default false;
}

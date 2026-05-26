package me.ray.midgard.bot.core.embed;

import me.ray.midgard.bot.core.util.ColorPalette;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.awt.Color;
import java.time.Instant;

public final class EmbedFactory {

    private static final String FOOTER_TEXT = "Midgard Bot";
    private static final String FOOTER_ICON = null;

    private EmbedFactory() {}

    // ==================== Themed Embeds ====================

    public static EmbedBuilder base() {
        return new EmbedBuilder()
                .setColor(ColorPalette.PRIMARY)
                .setTimestamp(Instant.now())
                .setFooter(FOOTER_TEXT, FOOTER_ICON);
    }

    public static MessageEmbed success(String title, String description) {
        return base()
                .setColor(ColorPalette.SUCCESS)
                .setTitle("✅ " + title)
                .setDescription(description)
                .build();
    }

    public static MessageEmbed error(String title, String description) {
        return base()
                .setColor(ColorPalette.ERROR)
                .setTitle("❌ " + title)
                .setDescription(description)
                .build();
    }

    public static MessageEmbed warning(String title, String description) {
        return base()
                .setColor(ColorPalette.WARNING)
                .setTitle("⚠️ " + title)
                .setDescription(description)
                .build();
    }

    public static MessageEmbed info(String title, String description) {
        return base()
                .setColor(ColorPalette.INFO)
                .setTitle("ℹ️ " + title)
                .setDescription(description)
                .build();
    }

    public static MessageEmbed simple(String description) {
        return base()
                .setDescription(description)
                .build();
    }

    public static MessageEmbed simple(String title, String description) {
        return base()
                .setTitle(title)
                .setDescription(description)
                .build();
    }

    // ==================== Special Embeds ====================

    public static MessageEmbed loading(String message) {
        return base()
                .setColor(ColorPalette.WARNING)
                .setTitle("⏳ Carregando...")
                .setDescription(message)
                .build();
    }

    public static MessageEmbed cooldown(long remainingSeconds) {
        return base()
                .setColor(ColorPalette.WARNING)
                .setTitle("⏳ Cooldown")
                .setDescription("Aguarde **" + remainingSeconds + "s** antes de usar novamente.")
                .build();
    }

    public static MessageEmbed noPermission(String permission) {
        return base()
                .setColor(ColorPalette.ERROR)
                .setTitle("🔒 Sem Permissão")
                .setDescription("Você não tem permissão para executar esta ação.\nPermissão necessária: `" + permission + "`")
                .build();
    }

    public static EmbedBuilder userEmbed(User user) {
        return base()
                .setAuthor(user.getName(), null, user.getEffectiveAvatarUrl())
                .setThumbnail(user.getEffectiveAvatarUrl());
    }

    // ==================== Builder Helpers ====================

    public static EmbedBuilder create() {
        return base();
    }

    public static EmbedBuilder create(String title) {
        return base().setTitle(title);
    }

    public static EmbedBuilder create(String title, String description) {
        return base().setTitle(title).setDescription(description);
    }

    public static EmbedBuilder create(Color color) {
        return base().setColor(color);
    }

    public static EmbedBuilder create(String title, Color color) {
        return base().setTitle(title).setColor(color);
    }

    // ==================== RPG Themed ====================

    public static EmbedBuilder rpg(String title) {
        return base()
                .setColor(ColorPalette.GOLD)
                .setTitle("⚔️ " + title);
    }

    public static EmbedBuilder mythic(String title) {
        return base()
                .setColor(ColorPalette.MYTHIC)
                .setTitle("🔮 " + title);
    }

    public static EmbedBuilder legendary(String title) {
        return base()
                .setColor(ColorPalette.LEGENDARY)
                .setTitle("🌟 " + title);
    }

    public static EmbedBuilder divine(String title) {
        return base()
                .setColor(ColorPalette.DIVINE)
                .setTitle("✨ " + title);
    }
}

package com.midgardbot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.SelfUser;

import java.awt.Color;
import java.time.Instant;

/**
 * Fábrica de Embeds (Mensagens ricas do Discord).
 * Centraliza a criação de embeds para manter um padrão visual (cores, rodapés, ícones) em todo o bot.
 */
public class EmbedUtils {

    // Paleta de Cores Profissional
    public static final Color COLOR_SUCCESS = Color.decode("#2ECC71");      // Verde Esmeralda
    public static final Color COLOR_ERROR = Color.decode("#E74C3C");        // Vermelho Alizarin
    public static final Color COLOR_WARNING = Color.decode("#F1C40F");      // Amarelo Girassol
    public static final Color COLOR_INFO = Color.decode("#3498DB");         // Azul Peter River
    public static final Color COLOR_PRIMARY = Color.decode("#34495E");      // Azul Wet Asphalt
    public static final Color COLOR_GOLD = Color.decode("#E67E22");         // Laranja Cenoura (Dourado)

    // Ícones Padronizados
    public static final String ICON_SUCCESS = "✅";
    public static final String ICON_ERROR = "❌";
    public static final String ICON_WARNING = "⚠️";
    public static final String ICON_INFO = "ℹ️";
    public static final String ICON_WAIT = "⏳";
    public static final String ICON_RPG = "⚔️";
    public static final String ICON_SHIELD = "🛡️";
    public static final String ICON_SCROLL = "📜";
    public static final String ICON_USER = "👤";
    public static final String ICON_STAFF = "👮";
    public static final String ICON_CALENDAR = "📅";
    public static final String ICON_ID = "🆔";
    public static final String ICON_BOX = "📦";
    public static final String ICON_BOOK = "📖";
    public static final String ICON_CONSOLE = "🎮";
    public static final String ICON_PC = "🖥️";
    public static final String ICON_BEDROCK = "📱";

    // Banners e Imagens
    public static final String IMG_WELCOME = "https://i.imgur.com/aSAj2iC.png";
    public static final String IMG_REJECTED = "https://i.imgur.com/dJt6p5f.png";
    public static final String IMG_WHITELIST_PANEL = "https://i.imgur.com/rLcL49T.png";
    public static final String IMG_SUBMITTED = "https://i.imgur.com/y7vRk9m.png";


    // Elementos Visuais
    public static final String SEPARATOR = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

    private static void applyStandardLook(EmbedBuilder builder, SelfUser selfUser) {
        if (selfUser != null) {
            builder.setFooter("MidgardBOT • Seu portal para o RPG", selfUser.getAvatarUrl());
            builder.setTimestamp(Instant.now());
        }
    }

    public static EmbedBuilder createEmbed(String title, String description, Color color) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(Instant.now())
                .setFooter("MidgardBOT • Seu portal para o RPG");
    }

    public static EmbedBuilder createSuccess(String title, String description, SelfUser selfUser) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(ICON_SUCCESS + " " + title)
                .setDescription(description)
                .setColor(COLOR_SUCCESS);
        applyStandardLook(builder, selfUser);
        return builder;
    }

    public static EmbedBuilder createError(String title, String description, SelfUser selfUser) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(ICON_ERROR + " " + title)
                .setDescription(description)
                .setColor(COLOR_ERROR);
        applyStandardLook(builder, selfUser);
        return builder;
    }

    public static EmbedBuilder createWarning(String title, String description, SelfUser selfUser) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(ICON_WARNING + " " + title)
                .setDescription(description)
                .setColor(COLOR_WARNING);
        applyStandardLook(builder, selfUser);
        return builder;
    }

    public static EmbedBuilder createInfo(String title, String description, SelfUser selfUser) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(ICON_INFO + " " + title)
                .setDescription(description)
                .setColor(COLOR_INFO);
        applyStandardLook(builder, selfUser);
        return builder;
    }

    public static EmbedBuilder createDefault(String title, String description, SelfUser selfUser) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(ICON_SHIELD + " " + title)
                .setDescription(description)
                .setColor(COLOR_PRIMARY);
        applyStandardLook(builder, selfUser);
        return builder;
    }
    
    public static EmbedBuilder createRpg(String title, String description, SelfUser selfUser) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(ICON_RPG + " " + title)
                .setDescription(description)
                .setColor(COLOR_GOLD);
        applyStandardLook(builder, selfUser);
        return builder;
    }
}

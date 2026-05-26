package me.ray.midgard.bot.modules.whitelist;

import me.ray.midgard.bot.core.embed.EmbedFactory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.awt.Color;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class WhitelistEmbeds {

    private WhitelistEmbeds() {}

    // ==================== Registration Embed ====================

    public static MessageEmbed registration(WhitelistConfig config) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(config.getEmbedTitle())
                .setDescription(config.getEmbedDescription())
                .setColor(new Color(config.getEmbedColor()))
                .setFooter(config.getEmbedFooter())
                .setTimestamp(Instant.now());

        String thumbnail = config.getEmbedThumbnail();
        if (thumbnail != null && !thumbnail.isEmpty()) {
            builder.setThumbnail(thumbnail);
        }

        String image = config.getEmbedImage();
        if (image != null && !image.isEmpty()) {
            builder.setImage(image);
        }

        return builder.build();
    }

    // ==================== Terms Embed ====================

    public static MessageEmbed terms(WhitelistConfig config) {
        return new EmbedBuilder()
                .setTitle("📜 " + config.getTermsTitle())
                .setDescription(config.getTermsText())
                .setColor(new Color(0xF1C40F))
                .setFooter("Leia atentamente antes de aceitar")
                .setTimestamp(Instant.now())
                .build();
    }

    // ==================== Progress Embed ====================

    public static MessageEmbed progress(WhitelistConfig config, int completedPart, int totalParts) {
        StringBuilder description = new StringBuilder();
        description.append("**Progresso da sua inscrição:**\n\n");

        for (int i = 0; i < totalParts; i++) {
            String partTitle = config.getPartTitle(i);
            if (i < completedPart) {
                description.append("✅ ~~Parte ").append(i + 1).append("/").append(totalParts)
                        .append(" - ").append(partTitle).append("~~\n");
            } else if (i == completedPart) {
                description.append("📝 **Parte ").append(i + 1).append("/").append(totalParts)
                        .append(" - ").append(partTitle).append("** *(próxima)*\n");
            } else {
                description.append("⬜ Parte ").append(i + 1).append("/").append(totalParts)
                        .append(" - ").append(partTitle).append("\n");
            }
        }

        if (completedPart >= totalParts) {
            description.append("\n✅ **Todas as partes foram preenchidas!**\n");
            description.append("Sua inscrição foi enviada para análise.");
        } else {
            description.append("\nClique no botão abaixo para continuar.");
        }

        return new EmbedBuilder()
                .setTitle("📋 Whitelist - Progresso")
                .setDescription(description.toString())
                .setColor(completedPart >= totalParts ? new Color(0x57F287) : new Color(0x5865F2))
                .setFooter("Midgard RPG • Parte " + Math.min(completedPart + 1, totalParts) + "/" + totalParts)
                .setTimestamp(Instant.now())
                .build();
    }

    // ==================== Submission Complete Embed ====================

    public static MessageEmbed submitted() {
        return new EmbedBuilder()
                .setTitle("✅ Inscrição Enviada!")
                .setDescription(
                        "Sua inscrição foi enviada com sucesso!\n\n" +
                        "📋 **Status:** Aguardando análise\n" +
                        "⏳ Um membro da equipe irá revisar suas respostas.\n\n" +
                        "Você será notificado quando houver uma resposta.")
                .setColor(new Color(0x57F287))
                .setFooter("Midgard RPG • Sistema de Whitelist")
                .setTimestamp(Instant.now())
                .build();
    }

    // ==================== Already Applied Embed ====================

    public static MessageEmbed alreadyApplied(WhitelistApplication.Status status) {
        String statusText;
        Color color;
        switch (status) {
            case PENDING:
                statusText = "📋 Sua inscrição já está **aguardando análise**.\nPor favor, aguarde a revisão da equipe.";
                color = new Color(0xFEE75C);
                break;
            case APPROVED:
                statusText = "✅ Sua inscrição já foi **aprovada**!\nVocê já é um cidadão de Midgard.";
                color = new Color(0x57F287);
                break;
            case REJECTED:
                statusText = "❌ Sua inscrição foi **rejeitada**.\nEntre em contato com a equipe para mais informações.";
                color = new Color(0xED4245);
                break;
            default:
                statusText = "Você já possui uma inscrição em andamento.";
                color = new Color(0x5865F2);
        }

        return new EmbedBuilder()
                .setTitle("⚠️ Inscrição Existente")
                .setDescription(statusText)
                .setColor(color)
                .setFooter("Midgard RPG")
                .setTimestamp(Instant.now())
                .build();
    }

    // ==================== Review Log Embed ====================

    public static MessageEmbed reviewLog(WhitelistApplication app, WhitelistConfig config, User user) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("📋 Nova Inscrição de Whitelist")
                .setColor(new Color(0xFEE75C))
                .setTimestamp(Instant.now())
                .setFooter("ID: " + app.getUserId());

        if (user != null) {
            builder.setAuthor(user.getName(), null, user.getEffectiveAvatarUrl());
            builder.setThumbnail(user.getEffectiveAvatarUrl());
        }

        builder.addField("👤 Usuário", user != null ? user.getAsMention() : app.getUserId(), true);
        builder.addField("📊 Status", "Aguardando Análise", true);
        builder.addField("\u200b", "\u200b", true);

        // Add answers grouped by part
        int totalParts = config.getPartCount();
        for (int i = 0; i < totalParts; i++) {
            StringBuilder partAnswers = new StringBuilder();
            List<WhitelistConfig.QuestionData> questions = config.getQuestions(i);
            for (WhitelistConfig.QuestionData q : questions) {
                String answer = app.getAnswer(q.getId());
                if (answer != null && !answer.isEmpty()) {
                    partAnswers.append("**").append(q.getLabel()).append("**\n")
                            .append(answer).append("\n\n");
                }
            }

            if (partAnswers.length() > 0) {
                String fieldValue = partAnswers.toString();
                // Discord field value limit is 1024
                if (fieldValue.length() > 1024) {
                    fieldValue = fieldValue.substring(0, 1021) + "...";
                }
                builder.addField("📝 " + config.getPartTitle(i), fieldValue, false);
            }
        }

        return builder.build();
    }

    // ==================== Approved/Rejected Embeds ====================

    public static MessageEmbed approved(String reviewerNote) {
        String desc = "🎉 Parabéns! Sua inscrição de whitelist foi **aprovada**!\n\n" +
                "Seja bem-vindo ao **Reino de Midgard**!";
        if (reviewerNote != null && !reviewerNote.isEmpty()) {
            desc += "\n\n📝 **Observação:** " + reviewerNote;
        }

        return new EmbedBuilder()
                .setTitle("✅ Whitelist Aprovada!")
                .setDescription(desc)
                .setColor(new Color(0x57F287))
                .setFooter("Midgard RPG")
                .setTimestamp(Instant.now())
                .build();
    }

    public static MessageEmbed rejected(String reviewerNote) {
        String desc = "Infelizmente sua inscrição de whitelist foi **rejeitada**.";
        if (reviewerNote != null && !reviewerNote.isEmpty()) {
            desc += "\n\n📝 **Motivo:** " + reviewerNote;
        }

        return new EmbedBuilder()
                .setTitle("❌ Whitelist Rejeitada")
                .setDescription(desc)
                .setColor(new Color(0xED4245))
                .setFooter("Midgard RPG")
                .setTimestamp(Instant.now())
                .build();
    }
}

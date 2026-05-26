package me.ray.midgard.bot.core.embed;

import me.ray.midgard.bot.core.util.ColorPalette;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class PaginatedEmbed {

    private final List<MessageEmbed> pages;
    private final String ownerId;
    private final String uniqueId;
    private int currentPage = 0;

    private PaginatedEmbed(List<MessageEmbed> pages, String ownerId) {
        this.pages = pages;
        this.ownerId = ownerId;
        this.uniqueId = UUID.randomUUID().toString().substring(0, 8);
    }

    public void send(IReplyCallback callback, me.ray.midgard.bot.core.interaction.ButtonHandler buttonHandler) {
        if (pages.isEmpty()) {
            callback.reply("Nenhum conteúdo para exibir.").setEphemeral(true).queue();
            return;
        }

        if (pages.size() == 1) {
            callback.replyEmbeds(pages.get(0)).queue();
            return;
        }

        // Register button handlers
        String prevId = "page_prev_" + uniqueId;
        String nextId = "page_next_" + uniqueId;
        String closeId = "page_close_" + uniqueId;

        buttonHandler.registerTemporary(prevId, System.currentTimeMillis() + 300_000, event -> {
            if (!isOwner(event)) {
                event.reply("❌ Apenas quem usou o comando pode navegar.").setEphemeral(true).queue();
                return;
            }
            currentPage = Math.max(0, currentPage - 1);
            event.editMessageEmbeds(pages.get(currentPage))
                    .setComponents(ActionRow.of(getButtons()))
                    .queue();
        });

        buttonHandler.registerTemporary(nextId, System.currentTimeMillis() + 300_000, event -> {
            if (!isOwner(event)) {
                event.reply("❌ Apenas quem usou o comando pode navegar.").setEphemeral(true).queue();
                return;
            }
            currentPage = Math.min(pages.size() - 1, currentPage + 1);
            event.editMessageEmbeds(pages.get(currentPage))
                    .setComponents(ActionRow.of(getButtons()))
                    .queue();
        });

        buttonHandler.registerTemporary(closeId, System.currentTimeMillis() + 300_000, event -> {
            if (!isOwner(event)) {
                event.reply("❌ Apenas quem usou o comando pode fechar.").setEphemeral(true).queue();
                return;
            }
            event.getMessage().delete().queue();
            buttonHandler.removeTemporary(prevId);
            buttonHandler.removeTemporary(nextId);
            buttonHandler.removeTemporary(closeId);
        });

        callback.replyEmbeds(pages.get(0))
                .addActionRow(getButtons())
                .queue();
    }

    private List<Button> getButtons() {
        String prevId = "page_prev_" + uniqueId;
        String nextId = "page_next_" + uniqueId;
        String closeId = "page_close_" + uniqueId;

        return List.of(
                Button.secondary(prevId, Emoji.fromUnicode("◀️")).withDisabled(currentPage == 0),
                Button.secondary("page_info_" + uniqueId, (currentPage + 1) + "/" + pages.size()).asDisabled(),
                Button.secondary(nextId, Emoji.fromUnicode("▶️")).withDisabled(currentPage >= pages.size() - 1),
                Button.danger(closeId, Emoji.fromUnicode("✖️"))
        );
    }

    private boolean isOwner(ButtonInteractionEvent event) {
        return event.getUser().getId().equals(ownerId);
    }

    // ==================== Builder ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<MessageEmbed> pages = new ArrayList<>();
        private String ownerId;
        private String title;
        private int itemsPerPage = 10;

        public Builder setOwner(User user) {
            this.ownerId = user.getId();
            return this;
        }

        public Builder setOwner(String userId) {
            this.ownerId = userId;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setItemsPerPage(int itemsPerPage) {
            this.itemsPerPage = itemsPerPage;
            return this;
        }

        public Builder addPage(MessageEmbed embed) {
            pages.add(embed);
            return this;
        }

        public Builder addPages(List<MessageEmbed> embeds) {
            pages.addAll(embeds);
            return this;
        }

        public <T> Builder fromList(List<T> items, Function<T, String> formatter) {
            if (items.isEmpty()) return this;

            int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);
            for (int page = 0; page < totalPages; page++) {
                int start = page * itemsPerPage;
                int end = Math.min(start + itemsPerPage, items.size());

                StringBuilder sb = new StringBuilder();
                for (int i = start; i < end; i++) {
                    sb.append(formatter.apply(items.get(i))).append("\n");
                }

                EmbedBuilder embed = EmbedFactory.base()
                        .setColor(ColorPalette.PRIMARY)
                        .setDescription(sb.toString());

                if (title != null) {
                    embed.setTitle(title);
                }

                embed.setFooter("Página " + (page + 1) + "/" + totalPages + " | Midgard Bot");
                pages.add(embed.build());
            }
            return this;
        }

        public <T> Builder fromNumberedList(List<T> items, Function<T, String> formatter) {
            if (items.isEmpty()) return this;

            int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);
            for (int page = 0; page < totalPages; page++) {
                int start = page * itemsPerPage;
                int end = Math.min(start + itemsPerPage, items.size());

                StringBuilder sb = new StringBuilder();
                for (int i = start; i < end; i++) {
                    sb.append("`").append(i + 1).append(".` ").append(formatter.apply(items.get(i))).append("\n");
                }

                EmbedBuilder embed = EmbedFactory.base()
                        .setColor(ColorPalette.PRIMARY)
                        .setDescription(sb.toString());

                if (title != null) {
                    embed.setTitle(title);
                }

                embed.setFooter("Página " + (page + 1) + "/" + totalPages + " | Midgard Bot");
                pages.add(embed.build());
            }
            return this;
        }

        public PaginatedEmbed build() {
            if (ownerId == null) throw new IllegalStateException("Owner must be set");
            return new PaginatedEmbed(pages, ownerId);
        }
    }
}

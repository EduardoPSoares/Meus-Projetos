package com.midgardbot.commands.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.midgardbot.integrations.NewsManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.Map;

/**
 * Handler interativo para gerenciamento de notícias do Launcher.
 * Fluxo completo com embeds, botões, select menus e modais.
 */
public class NewsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewsHandler.class);
    private static final Color COLOR_NEWS = Color.decode("#E67E22");
    private static final String SEPARATOR = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

    // ========================
    //    BUTTON IDS
    // ========================
    private static final String BTN_NEWS_CREATE = "news_create";
    private static final String BTN_NEWS_LIST = "news_list";
    private static final String BTN_NEWS_REMOVE = "news_remove";
    private static final String BTN_NEWS_BACK = "news_back";
    private static final String BTN_NEWS_REFRESH = "news_refresh";

    // Select menu / modal prefixes
    private static final String SELECT_NEWS_CATEGORY_CREATE = "news_cat_create";
    private static final String SELECT_NEWS_CATEGORY_LIST = "news_cat_list";
    private static final String SELECT_NEWS_CATEGORY_REMOVE = "news_cat_remove";
    private static final String SELECT_NEWS_REMOVE_ITEM = "news_remove_item:";
    private static final String MODAL_NEWS_CREATE = "modal_news_create:";

    // ========================
    //    SLASH COMMAND ENTRY
    // ========================

    /**
     * Envia o painel principal de notícias (chamado pelo /noticia).
     */
    public static void sendMainPanel(SlashCommandInteractionEvent event) {
        event.replyEmbeds(buildMainPanelEmbed(event.getJDA().getSelfUser()).build())
             .addComponents(buildMainPanelButtons())
             .setEphemeral(true)
             .queue();
    }

    // ========================
    //    BUTTON HANDLER
    // ========================

    public static boolean handleButton(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals(BTN_NEWS_CREATE)) {
            handleCreateCategorySelect(event);
            return true;
        }
        if (id.equals(BTN_NEWS_LIST)) {
            handleListCategorySelect(event);
            return true;
        }
        if (id.equals(BTN_NEWS_REMOVE)) {
            handleRemoveCategorySelect(event);
            return true;
        }
        if (id.equals(BTN_NEWS_BACK)) {
            handleBackToMain(event);
            return true;
        }
        if (id.equals(BTN_NEWS_REFRESH)) {
            handleRefresh(event);
            return true;
        }

        return false;
    }

    // ========================
    //    SELECT MENU HANDLER
    // ========================

    public static boolean handleSelectMenu(StringSelectInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals(SELECT_NEWS_CATEGORY_CREATE)) {
            handleCreateModal(event);
            return true;
        }
        if (id.equals(SELECT_NEWS_CATEGORY_LIST)) {
            handleListByCategory(event);
            return true;
        }
        if (id.equals(SELECT_NEWS_CATEGORY_REMOVE)) {
            handleRemoveArticleSelect(event);
            return true;
        }
        if (id.startsWith(SELECT_NEWS_REMOVE_ITEM)) {
            handleRemoveConfirm(event);
            return true;
        }

        return false;
    }

    // ========================
    //    MODAL HANDLER
    // ========================

    public static boolean handleModal(ModalInteractionEvent event) {
        String id = event.getModalId();

        if (id.startsWith(MODAL_NEWS_CREATE)) {
            handleCreateSubmit(event);
            return true;
        }

        return false;
    }

    // ========================
    //    MAIN PANEL
    // ========================

    private static EmbedBuilder buildMainPanelEmbed(SelfUser selfUser) {
        EmbedBuilder eb = new EmbedBuilder()
            .setTitle("📰 Gerenciador de Notícias do Launcher")
            .setColor(COLOR_NEWS)
            .setDescription(
                "Gerencie as notícias exibidas no **Midgard Launcher**.\n" +
                "As notícias são organizadas por **categorias** e publicadas diretamente no Launcher.\n\n" +
                SEPARATOR + "\n\n" +
                "**Categorias disponíveis:**\n" +
                "📰 **Notícias** — Atualizações gerais e avisos\n" +
                "📜 **Lore** — Histórias e narrativas do RPG\n" +
                "📋 **Changelogs** — Notas de atualização\n\n" +
                SEPARATOR + "\n\n" +
                "Selecione uma ação abaixo para começar:"
            );

        try {
            Map<String, Integer> counts = NewsManager.countByCategory();
            StringBuilder stats = new StringBuilder();
            int total = 0;
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                String emoji = NewsManager.CATEGORY_EMOJIS.getOrDefault(entry.getKey(), "📄");
                String label = NewsManager.CATEGORY_LABELS.getOrDefault(entry.getKey(), entry.getKey());
                stats.append(emoji).append(" ").append(label).append(": **").append(entry.getValue()).append("**\n");
                total += entry.getValue();
            }
            eb.addField("📊 Estatísticas", stats + "\n**Total:** " + total + " notícias", false);
        } catch (Exception e) {
            eb.addField("📊 Estatísticas", "⚠️ Não foi possível carregar estatísticas.", false);
        }

        if (selfUser != null) {
            eb.setFooter("MidgardBOT • Gerenciador de Notícias", selfUser.getAvatarUrl());
        }
        eb.setTimestamp(java.time.Instant.now());

        return eb;
    }

    private static ActionRow buildMainPanelButtons() {
        return ActionRow.of(
            Button.success(BTN_NEWS_CREATE, "✏️ Criar Notícia"),
            Button.primary(BTN_NEWS_LIST, "📋 Listar Notícias"),
            Button.danger(BTN_NEWS_REMOVE, "🗑️ Remover Notícia"),
            Button.secondary(BTN_NEWS_REFRESH, "🔄 Atualizar")
        );
    }

    // ========================
    //    CREATE FLOW
    // ========================

    private static void handleCreateCategorySelect(ButtonInteractionEvent event) {
        StringSelectMenu menu = buildCategorySelectMenu(SELECT_NEWS_CATEGORY_CREATE, "Escolha a categoria da nova notícia");

        event.editMessageEmbeds(
            new EmbedBuilder()
                .setTitle("✏️ Criar Notícia — Selecione a Categoria")
                .setColor(COLOR_NEWS)
                .setDescription(
                    "Escolha em qual categoria a notícia será publicada:\n\n" +
                    "📰 **Notícias** — Atualizações gerais e avisos\n" +
                    "📜 **Lore** — Histórias e narrativas do RPG\n" +
                    "📋 **Changelogs** — Notas de atualização"
                )
                .setFooter("Selecione uma categoria abaixo")
                .build()
        ).setComponents(
            ActionRow.of(menu),
            ActionRow.of(Button.secondary(BTN_NEWS_BACK, "◀ Voltar"))
        ).queue();
    }

    private static void handleCreateModal(StringSelectInteractionEvent event) {
        String category = event.getValues().get(0);
        String categoryLabel = NewsManager.CATEGORY_LABELS.getOrDefault(category, category);

        TextInput titleInput = TextInput.create("news_title", "Título", TextInputStyle.SHORT)
            .setPlaceholder("Ex: Atualização do Servidor v1.5")
            .setMinLength(3)
            .setMaxLength(100)
            .setRequired(true)
            .build();

        TextInput contentInput = TextInput.create("news_content", "Conteúdo (aceita HTML básico)", TextInputStyle.PARAGRAPH)
            .setPlaceholder("O conteúdo da notícia... Pode usar <b>negrito</b>, <i>itálico</i>, <br> para quebra de linha")
            .setMinLength(10)
            .setMaxLength(2000)
            .setRequired(true)
            .build();

        TextInput imageInput = TextInput.create("news_image", "URL da Imagem (opcional)", TextInputStyle.SHORT)
            .setPlaceholder("https://i.imgur.com/exemplo.png")
            .setRequired(false)
            .build();

        Modal modal = Modal.create(MODAL_NEWS_CREATE + category, "Nova " + categoryLabel)
            .addActionRow(titleInput)
            .addActionRow(contentInput)
            .addActionRow(imageInput)
            .build();

        event.replyModal(modal).queue();
    }

    private static void handleCreateSubmit(ModalInteractionEvent event) {
        event.deferEdit().queue();

        String category = event.getModalId().substring(MODAL_NEWS_CREATE.length());
        String title = event.getValue("news_title").getAsString();
        String content = event.getValue("news_content").getAsString();
        String image = event.getValue("news_image") != null ? event.getValue("news_image").getAsString().trim() : "";
        String author = event.getUser().getName();
        String categoryLabel = NewsManager.CATEGORY_LABELS.getOrDefault(category, category);
        String categoryEmoji = NewsManager.CATEGORY_EMOJIS.getOrDefault(category, "📄");
        SelfUser selfUser = event.getJDA().getSelfUser();

        try {
            NewsManager.addNews(title, content, author, category, image.isEmpty() ? null : image);

            EmbedBuilder successEmbed = new EmbedBuilder()
                .setTitle("✅ Notícia Publicada com Sucesso!")
                .setColor(EmbedUtils.COLOR_SUCCESS)
                .setDescription(
                    "A notícia foi publicada e já está disponível no **Midgard Launcher**!\n\n" +
                    SEPARATOR
                )
                .addField(categoryEmoji + " Categoria", categoryLabel, true)
                .addField("👤 Autor", author, true)
                .addField("\u200B", "\u200B", true)
                .addField("📝 Título", title, false)
                .addField("📄 Prévia do Conteúdo", truncate(content, 200), false);

            if (!image.isEmpty()) {
                successEmbed.setThumbnail(image);
            }

            successEmbed.setFooter("MidgardBOT • Notícia publicada", selfUser.getAvatarUrl());
            successEmbed.setTimestamp(java.time.Instant.now());

            event.getHook().editOriginalEmbeds(successEmbed.build())
                .setComponents(ActionRow.of(
                    Button.secondary(BTN_NEWS_BACK, "◀ Voltar ao Painel"),
                    Button.primary(BTN_NEWS_CREATE, "✏️ Criar Outra")
                )).queue();

        } catch (Exception e) {
            LOGGER.error("Erro ao publicar notícia", e);
            event.getHook().editOriginalEmbeds(
                EmbedUtils.createError("Erro ao Publicar",
                    "Não foi possível publicar a notícia: " + e.getMessage(),
                    selfUser).build()
            ).setComponents(ActionRow.of(
                Button.secondary(BTN_NEWS_BACK, "◀ Voltar ao Painel")
            )).queue();
        }
    }

    // ========================
    //    LIST FLOW
    // ========================

    private static void handleListCategorySelect(ButtonInteractionEvent event) {
        StringSelectMenu menu = buildCategorySelectMenu(SELECT_NEWS_CATEGORY_LIST, "Escolha a categoria para listar");

        event.editMessageEmbeds(
            new EmbedBuilder()
                .setTitle("📋 Listar Notícias — Selecione a Categoria")
                .setColor(COLOR_NEWS)
                .setDescription(
                    "Selecione uma categoria para ver as notícias publicadas:\n\n" +
                    "📰 **Notícias** — Atualizações gerais e avisos\n" +
                    "📜 **Lore** — Histórias e narrativas do RPG\n" +
                    "📋 **Changelogs** — Notas de atualização"
                )
                .setFooter("Selecione uma categoria abaixo")
                .build()
        ).setComponents(
            ActionRow.of(menu),
            ActionRow.of(Button.secondary(BTN_NEWS_BACK, "◀ Voltar"))
        ).queue();
    }

    private static void handleListByCategory(StringSelectInteractionEvent event) {
        event.deferEdit().queue();

        String category = event.getValues().get(0);
        String categoryLabel = NewsManager.CATEGORY_LABELS.getOrDefault(category, category);
        String categoryEmoji = NewsManager.CATEGORY_EMOJIS.getOrDefault(category, "📄");
        SelfUser selfUser = event.getJDA().getSelfUser();

        try {
            JsonArray articles = NewsManager.getNewsByCategory(category);

            if (articles.isEmpty()) {
                event.getHook().editOriginalEmbeds(
                    new EmbedBuilder()
                        .setTitle(categoryEmoji + " " + categoryLabel + " — Nenhuma Notícia")
                        .setColor(EmbedUtils.COLOR_WARNING)
                        .setDescription("Não há notícias publicadas nesta categoria.")
                        .setFooter("MidgardBOT • Gerenciador de Notícias", selfUser.getAvatarUrl())
                        .setTimestamp(java.time.Instant.now())
                        .build()
                ).setComponents(ActionRow.of(
                    Button.secondary(BTN_NEWS_BACK, "◀ Voltar ao Painel"),
                    Button.success(BTN_NEWS_CREATE, "✏️ Criar Notícia")
                )).queue();
                return;
            }

            EmbedBuilder listEmbed = new EmbedBuilder()
                .setTitle(categoryEmoji + " " + categoryLabel + " — " + articles.size() + " notícia(s)")
                .setColor(COLOR_NEWS);

            StringBuilder sb = new StringBuilder();
            // Get global articles for proper indexing
            JsonArray allArticles = NewsManager.getNews();

            for (int i = 0; i < articles.size() && i < 15; i++) {
                JsonObject art = articles.get(i).getAsJsonObject();
                String title = art.get("title").getAsString();
                String author = art.has("author") ? art.get("author").getAsString() : "Desconhecido";
                String date = art.has("date") ? art.get("date").getAsString() : "—";

                // Find global index
                int globalIndex = findGlobalIndex(allArticles, art);

                sb.append("**").append(globalIndex + 1).append(".** ")
                  .append(title).append("\n")
                  .append("   └ 👤 *").append(author).append("* — 📅 ").append(date).append("\n\n");
            }

            if (articles.size() > 15) {
                sb.append("*... e mais ").append(articles.size() - 15).append(" notícia(s)*");
            }

            listEmbed.setDescription(sb.toString());
            listEmbed.setFooter("MidgardBOT • Gerenciador de Notícias", selfUser.getAvatarUrl());
            listEmbed.setTimestamp(java.time.Instant.now());

            event.getHook().editOriginalEmbeds(listEmbed.build())
                .setComponents(ActionRow.of(
                    Button.secondary(BTN_NEWS_BACK, "◀ Voltar ao Painel"),
                    Button.danger(BTN_NEWS_REMOVE, "🗑️ Remover Notícia"),
                    Button.success(BTN_NEWS_CREATE, "✏️ Criar Notícia")
                )).queue();

        } catch (Exception e) {
            LOGGER.error("Erro ao listar notícias", e);
            event.getHook().editOriginalEmbeds(
                EmbedUtils.createError("Erro ao Listar",
                    "Não foi possível listar as notícias: " + e.getMessage(),
                    selfUser).build()
            ).setComponents(ActionRow.of(
                Button.secondary(BTN_NEWS_BACK, "◀ Voltar ao Painel")
            )).queue();
        }
    }

    // ========================
    //    REMOVE FLOW
    // ========================

    private static void handleRemoveCategorySelect(ButtonInteractionEvent event) {
        StringSelectMenu menu = buildCategorySelectMenu(SELECT_NEWS_CATEGORY_REMOVE, "Escolha a categoria para remover");

        event.editMessageEmbeds(
            new EmbedBuilder()
                .setTitle("🗑️ Remover Notícia — Selecione a Categoria")
                .setColor(EmbedUtils.COLOR_ERROR)
                .setDescription(
                    "Selecione a categoria da notícia que deseja remover:\n\n" +
                    "📰 **Notícias** — Atualizações gerais e avisos\n" +
                    "📜 **Lore** — Histórias e narrativas do RPG\n" +
                    "📋 **Changelogs** — Notas de atualização"
                )
                .setFooter("Selecione uma categoria abaixo")
                .build()
        ).setComponents(
            ActionRow.of(menu),
            ActionRow.of(Button.secondary(BTN_NEWS_BACK, "◀ Voltar"))
        ).queue();
    }

    private static void handleRemoveArticleSelect(StringSelectInteractionEvent event) {
        event.deferEdit().queue();

        String category = event.getValues().get(0);
        String categoryLabel = NewsManager.CATEGORY_LABELS.getOrDefault(category, category);
        String categoryEmoji = NewsManager.CATEGORY_EMOJIS.getOrDefault(category, "📄");
        SelfUser selfUser = event.getJDA().getSelfUser();

        try {
            JsonArray categoryArticles = NewsManager.getNewsByCategory(category);
            JsonArray allArticles = NewsManager.getNews();

            if (categoryArticles.isEmpty()) {
                event.getHook().editOriginalEmbeds(
                    new EmbedBuilder()
                        .setTitle(categoryEmoji + " " + categoryLabel + " — Nenhuma Notícia")
                        .setColor(EmbedUtils.COLOR_WARNING)
                        .setDescription("Não há notícias nesta categoria para remover.")
                        .build()
                ).setComponents(ActionRow.of(
                    Button.secondary(BTN_NEWS_BACK, "◀ Voltar ao Painel")
                )).queue();
                return;
            }

            StringSelectMenu.Builder menuBuilder = StringSelectMenu.create(SELECT_NEWS_REMOVE_ITEM + category)
                .setPlaceholder("Selecione a notícia para remover")
                .setMinValues(1)
                .setMaxValues(1);

            int count = 0;
            for (int i = 0; i < categoryArticles.size() && count < 25; i++) {
                JsonObject art = categoryArticles.get(i).getAsJsonObject();
                String title = art.get("title").getAsString();
                String date = art.has("date") ? art.get("date").getAsString() : "—";
                int globalIndex = findGlobalIndex(allArticles, art);

                String label = truncate(title, 80);
                String description = "📅 " + truncate(date, 40);

                menuBuilder.addOption(label, String.valueOf(globalIndex), description);
                count++;
            }

            event.getHook().editOriginalEmbeds(
                new EmbedBuilder()
                    .setTitle("🗑️ Remover " + categoryLabel)
                    .setColor(EmbedUtils.COLOR_ERROR)
                    .setDescription(
                        "Selecione a notícia que deseja **remover permanentemente**:\n\n" +
                        "⚠️ *Esta ação não pode ser desfeita!*"
                    )
                    .setFooter("MidgardBOT • Remoção de notícia", selfUser.getAvatarUrl())
                    .build()
            ).setComponents(
                ActionRow.of(menuBuilder.build()),
                ActionRow.of(Button.secondary(BTN_NEWS_BACK, "◀ Voltar ao Painel"))
            ).queue();

        } catch (Exception e) {
            LOGGER.error("Erro ao carregar notícias para remoção", e);
            event.getHook().editOriginalEmbeds(
                EmbedUtils.createError("Erro",
                    "Não foi possível carregar as notícias: " + e.getMessage(),
                    selfUser).build()
            ).setComponents(ActionRow.of(
                Button.secondary(BTN_NEWS_BACK, "◀ Voltar ao Painel")
            )).queue();
        }
    }

    private static void handleRemoveConfirm(StringSelectInteractionEvent event) {
        event.deferEdit().queue();

        String globalIndexStr = event.getValues().get(0);
        int globalIndex;
        try {
            globalIndex = Integer.parseInt(globalIndexStr);
        } catch (NumberFormatException e) {
            event.getHook().editOriginalEmbeds(
                EmbedUtils.createError("Erro", "Índice inválido.", event.getJDA().getSelfUser()).build()
            ).setComponents(ActionRow.of(
                Button.secondary(BTN_NEWS_BACK, "◀ Voltar ao Painel")
            )).queue();
            return;
        }

        SelfUser selfUser = event.getJDA().getSelfUser();

        try {
            // Fetch article info before removing
            JsonArray articles = NewsManager.getNews();
            String removedTitle = "Desconhecido";
            String removedCategory = "noticias";
            if (globalIndex >= 0 && globalIndex < articles.size()) {
                JsonObject art = articles.get(globalIndex).getAsJsonObject();
                removedTitle = art.get("title").getAsString();
                removedCategory = art.has("category") ? art.get("category").getAsString() : "noticias";
            }

            NewsManager.removeNews(globalIndex);

            String categoryEmoji = NewsManager.CATEGORY_EMOJIS.getOrDefault(removedCategory, "📄");
            String categoryLabel = NewsManager.CATEGORY_LABELS.getOrDefault(removedCategory, removedCategory);

            event.getHook().editOriginalEmbeds(
                new EmbedBuilder()
                    .setTitle("🗑️ Notícia Removida com Sucesso")
                    .setColor(EmbedUtils.COLOR_SUCCESS)
                    .setDescription(
                        "A notícia foi removida do **Midgard Launcher**.\n\n" +
                        SEPARATOR + "\n\n" +
                        "**Título:** " + removedTitle + "\n" +
                        "**Categoria:** " + categoryEmoji + " " + categoryLabel + "\n" +
                        "**Removida por:** " + event.getUser().getName()
                    )
                    .setFooter("MidgardBOT • Notícia removida", selfUser.getAvatarUrl())
                    .setTimestamp(java.time.Instant.now())
                    .build()
            ).setComponents(ActionRow.of(
                Button.secondary(BTN_NEWS_BACK, "◀ Voltar ao Painel"),
                Button.danger(BTN_NEWS_REMOVE, "🗑️ Remover Outra")
            )).queue();

        } catch (IndexOutOfBoundsException e) {
            event.getHook().editOriginalEmbeds(
                EmbedUtils.createError("Notícia Não Encontrada",
                    "A notícia pode já ter sido removida. Tente novamente.",
                    selfUser).build()
            ).setComponents(ActionRow.of(
                Button.secondary(BTN_NEWS_BACK, "◀ Voltar ao Painel")
            )).queue();
        } catch (Exception e) {
            LOGGER.error("Erro ao remover notícia", e);
            event.getHook().editOriginalEmbeds(
                EmbedUtils.createError("Erro ao Remover",
                    "Não foi possível remover a notícia: " + e.getMessage(),
                    selfUser).build()
            ).setComponents(ActionRow.of(
                Button.secondary(BTN_NEWS_BACK, "◀ Voltar ao Painel")
            )).queue();
        }
    }

    // ========================
    //    NAVIGATION
    // ========================

    private static void handleBackToMain(ButtonInteractionEvent event) {
        event.editMessageEmbeds(buildMainPanelEmbed(event.getJDA().getSelfUser()).build())
             .setComponents(buildMainPanelButtons())
             .queue();
    }

    private static void handleRefresh(ButtonInteractionEvent event) {
        event.editMessageEmbeds(buildMainPanelEmbed(event.getJDA().getSelfUser()).build())
             .setComponents(buildMainPanelButtons())
             .queue();
    }

    // ========================
    //    UTILITIES
    // ========================

    private static StringSelectMenu buildCategorySelectMenu(String menuId, String placeholder) {
        return StringSelectMenu.create(menuId)
            .setPlaceholder(placeholder)
            .setMinValues(1)
            .setMaxValues(1)
            .addOption("📰 Notícias", "noticias", "Atualizações gerais e avisos")
            .addOption("📜 Lore", "lore", "Histórias e narrativas do RPG")
            .addOption("📋 Changelogs", "changelogs", "Notas de atualização do Launcher")
            .build();
    }

    private static int findGlobalIndex(JsonArray allArticles, JsonObject target) {
        String targetTitle = target.get("title").getAsString();
        String targetDate = target.has("date") ? target.get("date").getAsString() : "";
        for (int i = 0; i < allArticles.size(); i++) {
            JsonObject art = allArticles.get(i).getAsJsonObject();
            String artTitle = art.get("title").getAsString();
            String artDate = art.has("date") ? art.get("date").getAsString() : "";
            if (artTitle.equals(targetTitle) && artDate.equals(targetDate)) {
                return i;
            }
        }
        return -1;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen - 3) + "..." : text;
    }
}

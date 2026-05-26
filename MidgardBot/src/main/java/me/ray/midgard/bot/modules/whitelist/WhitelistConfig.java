package me.ray.midgard.bot.modules.whitelist;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.ray.midgard.bot.core.config.JsonConfig;
// Config is fully read-only - all questions/settings are managed via whitelist.json
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

import java.util.ArrayList;
import java.util.List;

public class WhitelistConfig {

    private final JsonConfig config;

    public WhitelistConfig(JsonConfig config) {
        this.config = config;
        setupDefaults();
    }

    private void setupDefaults() {
        // Config is read-only - all configuration is done via the JSON file directly
        config.setReadOnly(true);
    }

    // ==================== Getters ====================

    public String getEmbedTitle() { return config.getString("embed.title"); }
    public String getEmbedDescription() { return config.getString("embed.description"); }
    public String getEmbedFooter() { return config.getString("embed.footer"); }
    public String getEmbedThumbnail() { return config.getString("embed.thumbnail", ""); }
    public String getEmbedImage() { return config.getString("embed.image", ""); }
    public int getEmbedColor() {
        return Integer.parseInt(config.getString("embed.color", "5865F2"), 16);
    }

    public String getButtonStartText() { return config.getString("buttons.start"); }
    public String getButtonAcceptText() { return config.getString("buttons.accept"); }
    public String getButtonContinueText() { return config.getString("buttons.continue"); }

    public String getTermsTitle() { return config.getString("terms.title"); }
    public String getTermsText() { return config.getString("terms.text"); }

    public String getApprovedRoleId() { return config.getString("roles.approved", ""); }
    public String getPendingRoleId() { return config.getString("roles.pending", ""); }
    public String getLogChannelId() { return config.getString("channels.log", ""); }
    public String getApprovedChannelId() { return config.getString("channels.approved", ""); }

    public int getPartCount() {
        JsonArray parts = config.getArray("questions");
        return parts != null ? parts.size() : 0;
    }

    public String getPartTitle(int partIndex) {
        JsonArray parts = config.getArray("questions");
        if (parts == null || partIndex >= parts.size()) return "Parte " + (partIndex + 1);
        return parts.get(partIndex).getAsJsonObject().get("title").getAsString();
    }

    public List<QuestionData> getQuestions(int partIndex) {
        List<QuestionData> questions = new ArrayList<>();
        JsonArray parts = config.getArray("questions");
        if (parts == null || partIndex >= parts.size()) return questions;

        JsonArray qs = parts.get(partIndex).getAsJsonObject().getAsJsonArray("questions");
        for (int i = 0; i < qs.size(); i++) {
            JsonObject q = qs.get(i).getAsJsonObject();
            questions.add(new QuestionData(
                    q.get("id").getAsString(),
                    q.get("label").getAsString(),
                    q.has("placeholder") ? q.get("placeholder").getAsString() : "",
                    q.has("required") && q.get("required").getAsBoolean(),
                    q.has("style") && q.get("style").getAsString().equals("PARAGRAPH")
                            ? TextInputStyle.PARAGRAPH : TextInputStyle.SHORT
            ));
        }
        return questions;
    }

    public List<List<QuestionData>> getAllQuestions() {
        List<List<QuestionData>> all = new ArrayList<>();
        for (int i = 0; i < getPartCount(); i++) {
            all.add(getQuestions(i));
        }
        return all;
    }

    public JsonConfig getRawConfig() { return config; }

    public void reload() { config.reload(); }

    // ==================== Question Data ====================

    public static class QuestionData {
        private final String id;
        private final String label;
        private final String placeholder;
        private final boolean required;
        private final TextInputStyle style;

        public QuestionData(String id, String label, String placeholder, boolean required, TextInputStyle style) {
            this.id = id;
            this.label = label;
            this.placeholder = placeholder;
            this.required = required;
            this.style = style;
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public String getPlaceholder() { return placeholder; }
        public boolean isRequired() { return required; }
        public TextInputStyle getStyle() { return style; }
    }
}

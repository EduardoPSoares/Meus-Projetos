package me.ray.midgard.modules.races;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.model.Race;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RacePlaceholders extends PlaceholderExpansion {

    private final RacesModule module;

    public RacePlaceholders(RacesModule module) {
        this.module = module;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "midgardraces";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Ray";
    }

    @Override
    public @NotNull String getVersion() {
        return module.getPlugin().getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) { return ""; }

        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return ""; }

        RaceData data = profile.getData(RaceData.class);
        String lower = params.toLowerCase();

        // Placeholders que não precisam de raça
        if (data == null || !data.hasRace()) {
            if (lower.equals("has_race")) { return "false"; }
            if (lower.equals("name") || lower.equals("displayname")) {
                return me.ray.midgard.core.text.MessageUtils.serialize(
                        me.ray.midgard.core.text.MessageUtils.parse(module.getMessage("placeholder.no_race")));
            }
            return "";
        }

        Race race = module.getRaceManager().getRace(data.getRaceId());
        if (race == null) { return ""; }

        // Tratar placeholders dinâmicos (kills_<tipo>, xp_multiplier_<source>)
        if (lower.startsWith("kills_")) {
            String type = lower.substring(6).toUpperCase();
            return String.valueOf(data.getKillsOf(type));
        }
        if (lower.startsWith("xp_multiplier_")) {
            String sourceName = lower.substring(14).toUpperCase();
            try {
                me.ray.midgard.modules.races.api.RaceXpSource source =
                        me.ray.midgard.modules.races.api.RaceXpSource.valueOf(sourceName);
                return String.format("%.2f", race.getXpMultiplier(source));
            } catch (IllegalArgumentException e) {
                return "1.00";
            }
        }

        return switch (lower) {
            // ─── Identidade ──────────────────────────────────────
            case "id" -> race.getId();
            case "name" -> PlainTextComponentSerializer.plainText().serialize(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(race.getDisplayName()));
            case "displayname" -> me.ray.midgard.core.text.MessageUtils.serialize(
                    me.ray.midgard.core.text.MessageUtils.parse(race.getDisplayName()));
            case "has_race" -> "true";

            // ─── Nível e Experiência ─────────────────────────────
            case "level" -> String.valueOf(data.getLevel());
            case "max_level" -> String.valueOf(module.getLevelManager().getMaxLevel());
            case "exp" -> String.format("%.1f", data.getExperience());
            case "exp_req" -> String.format("%.1f", module.getLevelManager().getRequiredExperience(data.getLevel()));
            case "exp_percent" -> {
                double req = module.getLevelManager().getRequiredExperience(data.getLevel());
                double percent = req > 0 ? (data.getExperience() / req) * 100 : 100;
                yield String.format("%.1f", percent);
            }
            case "level_progress" -> {
                int lvl = data.getLevel();
                int max = module.getLevelManager().getMaxLevel();
                yield lvl + "/" + max;
            }

            // ─── Evolução ────────────────────────────────────────
            case "is_evolved" -> String.valueOf(race.isSubRace());
            case "parent" -> {
                if (!race.isSubRace()) { yield ""; }
                Race parent = module.getRaceManager().getRace(race.getParentRace());
                yield parent != null ? PlainTextComponentSerializer.plainText().serialize(
                        LegacyComponentSerializer.legacyAmpersand().deserialize(parent.getDisplayName())) : "";
            }
            case "parent_id" -> race.isSubRace() ? race.getParentRace() : "";
            case "can_devolve" -> String.valueOf(race.isSubRace() && race.isAllowDevolution());
            case "branch" -> race.getExclusionBranch() != null ? race.getExclusionBranch() : "";

            // ─── Estatísticas ────────────────────────────────────
            case "kills" -> String.valueOf(data.getTotalKills());
            case "mutations" -> String.valueOf(data.getUnlockedMutations().size());
            case "history_size" -> String.valueOf(data.getRaceHistory().size());
            case "history" -> {
                List<String> history = data.getRaceHistory();
                yield history.isEmpty() ? "" : String.join(", ", history);
            }

            // ─── Mundo/Tempo ─────────────────────────────────────
            case "time" -> {
                long time = player.getWorld().getTime();
                yield RacesModule.isDayTime(time) ? "day" : "night";
            }

            default -> null;
        };
    }
}

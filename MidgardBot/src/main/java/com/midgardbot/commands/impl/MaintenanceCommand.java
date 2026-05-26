package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.Messages;
import com.midgardbot.data.DataManager;
import com.midgardbot.features.ServerStatusMonitor;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comando de Manutenção.
 * Gerencia o modo de manutenção do Bot e dos Servidores (Lobby/RPG).
 * Permite definir tempo de duração.
 */
public class MaintenanceCommand implements ISlashCommand {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceCommand.class);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public String getName() {
        return "maintenance";
    }

    @Override
    public String getDescription() {
        return "Gerencia o modo de manutenção do Bot e Servidores.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "alvo", "O alvo da manutenção", true)
                .addChoice("🤖 Bot (Discord)", "bot")
                .addChoice("🌍 Lobby", "lobby")
                .addChoice("⚔️ RPG", "rpg")
                .addChoice("🌐 Todos os Servidores", "all"),
            new OptionData(OptionType.BOOLEAN, "estado", "Ativar ou Desativar?", true),
            new OptionData(OptionType.STRING, "tempo", "Duração (ex: 1h, 30m). Deixe vazio para indeterminado.", false)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_MAINTENANCE";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        boolean isConfigured = !com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_MAINTENANCE").isEmpty();
        if (!isConfigured && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError(
                Messages.get("maintenance.access-denied-title"), 
                Messages.get("maintenance.access-denied-desc"), 
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        String target = event.getOption("alvo").getAsString();
        boolean enable = event.getOption("estado").getAsBoolean();
        String timeStr = event.getOption("tempo") != null ? event.getOption("tempo").getAsString() : null;

        long durationSeconds = 0;
        if (enable && timeStr != null) {
            durationSeconds = parseDuration(timeStr);
            if (durationSeconds <= 0) {
                event.replyEmbeds(EmbedUtils.createError(
                    Messages.get("maintenance.invalid-format-title"), 
                    Messages.get("maintenance.invalid-format-desc"), 
                    event.getJDA().getSelfUser()
                ).build()).setEphemeral(true).queue();
                return;
            }
        }

        applyMaintenance(target, enable);

        String targetName = getTargetName(target);
        String durationMsg = (enable && durationSeconds > 0) ? "\n⏳ **Duração:** " + timeStr : "";

        if (enable) {
            event.replyEmbeds(EmbedUtils.createWarning(
                Messages.get("maintenance.enabled-title", "target", targetName),
                Messages.get("maintenance.enabled-desc", "duration", durationMsg),
                event.getJDA().getSelfUser()
            ).build()).queue();
            
            if (durationSeconds > 0) {
                scheduleAutoDisable(target, durationSeconds);
            }
        } else {
            event.replyEmbeds(EmbedUtils.createSuccess(
                Messages.get("maintenance.disabled-title"),
                Messages.get("maintenance.disabled-desc", "target", targetName),
                event.getJDA().getSelfUser()
            ).build()).queue();
        }
    }

    private void applyMaintenance(String target, boolean enable) {
        switch (target) {
            case "bot":
                DataManager.setMaintenanceMode(enable);
                break;
            case "lobby":
                ServerStatusMonitor.setMaintenance("lobby", enable);
                DataManager.setMaintenance("lobby", enable, "DiscordBot");
                break;
            case "rpg":
                ServerStatusMonitor.setMaintenance("rpg", enable);
                DataManager.setMaintenance("rpg", enable, "DiscordBot");
                break;
            case "all":
                ServerStatusMonitor.setMaintenance("lobby", enable);
                ServerStatusMonitor.setMaintenance("rpg", enable);
                DataManager.setMaintenance("lobby", enable, "DiscordBot");
                DataManager.setMaintenance("rpg", enable, "DiscordBot");
                break;
        }
        ServerStatusMonitor.forceUpdate();
    }

    private String getTargetName(String target) {
        switch (target) {
            case "bot": return "Bot (Discord)";
            case "lobby": return "Servidor Lobby";
            case "rpg": return "Servidor RPG";
            case "all": return "Todos os Servidores";
            default: return target;
        }
    }

    private void scheduleAutoDisable(String target, long seconds) {
        scheduler.schedule(() -> {
            applyMaintenance(target, false);
            LOGGER.info("Auto-disabling maintenance for {}", target);
        }, seconds, TimeUnit.SECONDS);
    }

    private long parseDuration(String input) {
        long totalSeconds = 0;
        Pattern p = Pattern.compile("(\\d+)([dhms])");
        Matcher m = p.matcher(input.toLowerCase());
        while (m.find()) {
            int value = Integer.parseInt(m.group(1));
            String unit = m.group(2);
            switch (unit) {
                case "d": totalSeconds += value * 86400L; break;
                case "h": totalSeconds += value * 3600L; break;
                case "m": totalSeconds += value * 60L; break;
                case "s": totalSeconds += value; break;
            }
        }
        return totalSeconds;
    }
}

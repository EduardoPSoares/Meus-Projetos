package com.midgardbot.features.meeting;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.BotConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Comando /reuniao — gerencia gravação de reuniões em canais de voz/palco.
 *
 * Subcomandos:
 * - /reuniao iniciar [titulo] — Inicia a gravação no canal de voz do usuário
 * - /reuniao parar — Para a gravação e salva o áudio
 * - /reuniao status — Mostra o status da gravação ativa
 */
public class MeetingCommand implements ISlashCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeetingCommand.class);

    @Override
    public String getName() {
        return "reuniao";
    }

    @Override
    public String getDescription() {
        return "Gerencia gravação de reuniões em canais de voz/palco";
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_REUNIAO";
    }

    @Override
    public boolean allowedInStaffGuild() {
        return true;
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of(
                new SubcommandData("iniciar", "Inicia a gravação no canal de voz/palco em que você está")
                        .addOption(OptionType.STRING, "titulo", "Título da reunião", true),
                new SubcommandData("parar", "Para a gravação ativa e salva o áudio"),
                new SubcommandData("status", "Mostra o status da gravação ativa")
        );
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        Guild guild = event.getGuild();
        if (member == null || guild == null) return;

        // Validar permissão
        String permKey = getPermissionKey();
        String permValue = BotConfig.get(permKey);
        if (permValue != null && !permValue.isEmpty()) {
            boolean hasRole = member.getRoles().stream()
                    .anyMatch(r -> permValue.contains(r.getId()));
            boolean isAdmin = member.getRoles().stream()
                    .anyMatch(r -> {
                        String adminId = BotConfig.get("ADMIN");
                        return adminId != null && adminId.contains(r.getId());
                    });
            if (!hasRole && !isAdmin) {
                event.reply("❌ Você não tem permissão para usar este comando.").setEphemeral(true).queue();
                return;
            }
        }

        String sub = event.getSubcommandName();
        if (sub == null) return;

        switch (sub) {
            case "iniciar" -> handleStart(event, member, guild);
            case "parar" -> handleStop(event, guild);
            case "status" -> handleStatus(event, guild);
        }
    }

    private void handleStart(SlashCommandInteractionEvent event, Member member, Guild guild) {
        // Verificar se já há gravação ativa
        if (MeetingRecorder.isRecording(guild.getId())) {
            MeetingRecorder active = MeetingRecorder.getActive(guild.getId());
            Duration elapsed = Duration.between(active.getStartTime(), Instant.now());
            event.reply("⚠️ Já existe uma gravação ativa neste servidor! " +
                    "(Reunião #" + active.getMeetingId() + " — " + formatDuration(elapsed) + " de gravação)\n" +
                    "Use `/reuniao parar` para finalizar antes de iniciar uma nova.")
                    .setEphemeral(true).queue();
            return;
        }

        // Verificar se o usuário está em um canal de voz/palco
        GuildVoiceState voiceState = member.getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply("❌ Você precisa estar em um canal de voz ou palco para iniciar a gravação.")
                    .setEphemeral(true).queue();
            return;
        }

        AudioChannel channel = voiceState.getChannel();
        String title = event.getOption("titulo").getAsString().trim();
        if (title.length() > 200) title = title.substring(0, 200);

        event.deferReply().queue();

        try {
            MeetingRecorder recorder = MeetingRecorder.start(guild, channel, title, member);

            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(new Color(240, 180, 41))
                    .setTitle("🎙️ Gravação de Reunião Iniciada")
                    .addField("Título", title, false)
                    .addField("Canal", channel.getName(), true)
                    .addField("Iniciada por", member.getEffectiveName(), true)
                    .addField("ID da Reunião", "#" + recorder.getMeetingId(), true)
                    .setFooter("Use /reuniao parar para finalizar a gravação")
                    .setTimestamp(Instant.now());

            event.getHook().editOriginalEmbeds(eb.build()).queue();

            LOGGER.info("[MEETING] {} iniciou gravação #{} no canal '{}'",
                    member.getEffectiveName(), recorder.getMeetingId(), channel.getName());

        } catch (Exception e) {
            LOGGER.error("[MEETING] Erro ao iniciar gravação", e);
            event.getHook().editOriginal("❌ Erro ao iniciar gravação: " + e.getMessage()).queue();
        }
    }

    private void handleStop(SlashCommandInteractionEvent event, Guild guild) {
        if (!MeetingRecorder.isRecording(guild.getId())) {
            event.reply("❌ Não há nenhuma gravação ativa neste servidor.")
                    .setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();

        try {
            MeetingRecorder recorder = MeetingRecorder.stop(guild);
            Duration elapsed = Duration.between(recorder.getStartTime(), Instant.now());

            long fileSizeBytes = 0;
            try {
                fileSizeBytes = java.nio.file.Files.size(recorder.getFilePath());
            } catch (Exception ignored) {}

            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(new Color(5, 150, 105))
                    .setTitle("✅ Gravação Finalizada!")
                    .addField("ID da Reunião", "#" + recorder.getMeetingId(), true)
                    .addField("Duração", formatDuration(elapsed), true)
                    .addField("Participantes", String.valueOf(recorder.getParticipantCount()), true)
                    .addField("Tamanho", String.format("%.1f MB", fileSizeBytes / 1_048_576.0), true)
                    .setFooter("A gravação estará disponível no painel web em Relatórios → Reuniões")
                    .setTimestamp(Instant.now());

            event.getHook().editOriginalEmbeds(eb.build()).queue();

        } catch (Exception e) {
            LOGGER.error("[MEETING] Erro ao parar gravação", e);
            event.getHook().editOriginal("❌ Erro ao finalizar gravação: " + e.getMessage()).queue();
        }
    }

    private void handleStatus(SlashCommandInteractionEvent event, Guild guild) {
        if (!MeetingRecorder.isRecording(guild.getId())) {
            event.reply("ℹ️ Não há nenhuma gravação ativa neste servidor.").setEphemeral(true).queue();
            return;
        }

        MeetingRecorder recorder = MeetingRecorder.getActive(guild.getId());
        Duration elapsed = Duration.between(recorder.getStartTime(), Instant.now());

        EmbedBuilder eb = new EmbedBuilder()
                .setColor(new Color(220, 38, 38))
                .setTitle("🔴 Gravação em Andamento")
                .addField("ID da Reunião", "#" + recorder.getMeetingId(), true)
                .addField("Duração", formatDuration(elapsed), true)
                .addField("Participantes Detectados", String.valueOf(recorder.getParticipantCount()), true)
                .setFooter("Use /reuniao parar para finalizar")
                .setTimestamp(Instant.now());

        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    private static String formatDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        if (hours > 0) return String.format("%dh %02dmin %02ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dmin %02ds", minutes, seconds);
        return String.format("%ds", seconds);
    }
}

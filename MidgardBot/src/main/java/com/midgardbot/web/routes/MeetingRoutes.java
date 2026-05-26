package com.midgardbot.web.routes;

import com.midgardbot.data.DatabaseManager;
import com.midgardbot.features.meeting.MeetingRecorder;
import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Rotas de reuniões — CRUD + streaming de gravações de áudio.
 */
public class MeetingRoutes {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeetingRoutes.class);
    private static final Gson GSON = new Gson();

    public static void register(Javalin app, JDA jda) {

        // ─── Reuniões em gravação ativa ──────────────────────────────
        app.get("/api/meetings/active", ctx -> {
            var active = MeetingRecorder.getActiveRecordings();
            var list = new ArrayList<Map<String, Object>>();
            for (var entry : active.entrySet()) {
                var rec = entry.getValue();
                var info = new HashMap<String, Object>();
                info.put("meetingId", rec.getMeetingId());
                info.put("guildId", rec.getGuildId());
                info.put("startTime", rec.getStartTime().toString());
                info.put("participantCount", rec.getParticipantCount());
                info.put("durationSeconds", Duration.between(rec.getStartTime(), Instant.now()).getSeconds());
                list.add(info);
            }
            ctx.json(Map.of("active", list));
        });

        // ─── Lista paginada de reuniões ─────────────────────────────
        app.get("/api/meetings", ctx -> {
            int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(20);
            int offset = ctx.queryParamAsClass("offset", Integer.class).getOrDefault(0);

            if (limit < 1) limit = 1;
            if (limit > 100) limit = 100;
            if (offset < 0) offset = 0;

            var meetings = DatabaseManager.getMeetings(limit, offset);
            int total = DatabaseManager.countMeetings();

            ctx.json(Map.of(
                    "meetings", meetings,
                    "total", total,
                    "limit", limit,
                    "offset", offset
            ));
        });

        // ─── Detalhes de uma reunião ─────────────────────────────────
        app.get("/api/meetings/{id}", ctx -> {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            var meeting = DatabaseManager.getMeetingById(id);
            if (meeting == null) {
                ctx.status(404).json(Map.of("error", "Reunião não encontrada"));
                return;
            }
            ctx.json(meeting);
        });

        // ─── Faixas individuais de uma reunião ───────────────────
        app.get("/api/meetings/{id}/tracks", ctx -> {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            var tracks = DatabaseManager.getMeetingTracks(id);
            ctx.json(Map.of("tracks", tracks));
        });

        // ─── Streaming do arquivo de gravação ────────────────────────
        app.get("/api/meetings/recordings/{filename}", ctx -> {
            String filename = ctx.pathParam("filename");

            // Sanitizar filename para evitar path traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                ctx.status(400).json(Map.of("error", "Nome de arquivo inválido"));
                return;
            }

            Path filePath = MeetingRecorder.getRecordingsDir().resolve(filename);
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                ctx.status(404).json(Map.of("error", "Gravação não encontrada"));
                return;
            }

            // Verificar se o arquivo está dentro do diretório esperado
            if (!filePath.toRealPath().startsWith(MeetingRecorder.getRecordingsDir().toRealPath())) {
                ctx.status(403).json(Map.of("error", "Acesso negado"));
                return;
            }

            long fileSize = Files.size(filePath);
            String rangeHeader = ctx.header("Range");

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                // Range request para streaming/seek
                String[] ranges = rangeHeader.substring(6).split("-");
                long start = Long.parseLong(ranges[0]);
                long end = ranges.length > 1 && !ranges[1].isEmpty()
                        ? Long.parseLong(ranges[1])
                        : fileSize - 1;

                if (start >= fileSize || end >= fileSize || start > end) {
                    ctx.status(416).header("Content-Range", "bytes */" + fileSize);
                    return;
                }

                long contentLength = end - start + 1;
                ctx.status(206);
                ctx.header("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
                ctx.header("Content-Length", String.valueOf(contentLength));
                ctx.header("Content-Type", "audio/wav");
                ctx.header("Accept-Ranges", "bytes");

                try (InputStream is = Files.newInputStream(filePath)) {
                    is.skip(start);
                    byte[] buffer = new byte[8192];
                    long remaining = contentLength;
                    OutputStream os = ctx.outputStream();
                    while (remaining > 0) {
                        int toRead = (int) Math.min(buffer.length, remaining);
                        int read = is.read(buffer, 0, toRead);
                        if (read == -1) break;
                        os.write(buffer, 0, read);
                        remaining -= read;
                    }
                }
            } else {
                // Full file
                ctx.header("Content-Type", "audio/wav");
                ctx.header("Content-Length", String.valueOf(fileSize));
                ctx.header("Accept-Ranges", "bytes");
                ctx.header("Content-Disposition", "inline; filename=\"" + filename + "\"");

                try (InputStream is = Files.newInputStream(filePath)) {
                    byte[] buffer = new byte[8192];
                    OutputStream os = ctx.outputStream();
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                }
            }
        });

        // ─── Atualizar observações/notas de uma reunião ──────────────
        app.put("/api/meetings/{id}/notes", ctx -> {
            int id = ctx.pathParamAsClass("id", Integer.class).get();

            var meeting = DatabaseManager.getMeetingById(id);
            if (meeting == null) {
                ctx.status(404).json(Map.of("error", "Reunião não encontrada"));
                return;
            }

            var body = GSON.fromJson(ctx.body(), Map.class);
            String notes = body != null && body.containsKey("notes")
                    ? String.valueOf(body.get("notes"))
                    : "";

            if (notes.length() > 5000) notes = notes.substring(0, 5000);

            DatabaseManager.updateMeetingNotes(id, notes);
            ctx.json(Map.of("success", true));
        });

        // ─── Deletar reunião (e gravação) ────────────────────────────
        app.delete("/api/meetings/{id}", ctx -> {
            int id = ctx.pathParamAsClass("id", Integer.class).get();

            var meeting = DatabaseManager.getMeetingById(id);
            if (meeting == null) {
                ctx.status(404).json(Map.of("error", "Reunião não encontrada"));
                return;
            }

            // Bloquear exclusão de reunião em andamento
            for (var rec : MeetingRecorder.getActiveRecordings().values()) {
                if (rec.getMeetingId() == id) {
                    ctx.status(409).json(Map.of("error", "Não é possível excluir uma reunião que está sendo gravada no momento"));
                    return;
                }
            }

            // Deletar arquivo de gravação principal
            String filename = (String) meeting.get("recordingFilename");
            if (filename != null && !filename.isEmpty()) {
                Path filePath = MeetingRecorder.getRecordingsDir().resolve(filename);
                try {
                    Files.deleteIfExists(filePath);
                } catch (Exception e) {
                    LOGGER.warn("[MEETING] Erro ao deletar arquivo de gravação: {}", e.getMessage());
                }
            }

            // Deletar arquivos das faixas individuais
            var tracks = DatabaseManager.getMeetingTracks(id);
            for (var track : tracks) {
                String trackFile = (String) track.get("filename");
                if (trackFile != null && !trackFile.isEmpty()) {
                    try {
                        Files.deleteIfExists(MeetingRecorder.getRecordingsDir().resolve(trackFile));
                    } catch (Exception ignored) {}
                }
            }

            DatabaseManager.deleteMeeting(id);
            ctx.json(Map.of("success", true));

            LOGGER.info("[MEETING] Reunião #{} deletada por {}",
                    id, ctx.attribute("username"));
        });

        LOGGER.info("[WEB] Rotas de reuniões registradas");
    }
}

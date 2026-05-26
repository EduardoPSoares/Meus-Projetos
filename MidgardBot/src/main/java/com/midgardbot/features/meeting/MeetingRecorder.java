package com.midgardbot.features.meeting;

import com.midgardbot.data.DatabaseManager;
import com.google.gson.Gson;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import net.dv8tion.jda.api.audio.UserAudio;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grava áudio de canais de voz/palco do Discord e salva como WAV.
 * O áudio é recebido como PCM 48kHz 16-bit Stereo via JDA e convertido
 * para Mono 48kHz para reduzir o tamanho do arquivo (~5.7 MB/min).
 * <br>
 * Uso: MeetingRecorder.start() → grava em background → MeetingRecorder.stop() → salva WAV
 */
public class MeetingRecorder implements AudioReceiveHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeetingRecorder.class);
    private static final Path RECORDINGS_DIR = Path.of("data", "recordings");
    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 1; // Mono (mixamos L+R)
    private static final int BITS_PER_SAMPLE = 16;
    private static final int MONO_FRAME_SIZE = 960 * (BITS_PER_SAMPLE / 8); // 1920 bytes por frame de 20ms
    private static final int SPEAKING_GAP_FRAMES = 10; // 200ms sem áudio = novo segmento

    // Gravações ativas — uma por guild
    private static final Map<String, MeetingRecorder> ACTIVE_RECORDINGS = new ConcurrentHashMap<>();

    private final int meetingId;
    private final String guildId;
    private final Instant startTime;
    private final Set<String> participantIds = ConcurrentHashMap.newKeySet();
    private final Map<String, String> participantNames = new ConcurrentHashMap<>();
    private AudioChannel channel;

    private RandomAccessFile wavFile;
    private Path filePath;
    private long totalBytesWritten = 0;
    private volatile boolean recording = false;
    private final Map<String, UserTrack> userTracks = new ConcurrentHashMap<>();
    private long frameIndex = 0;

    private MeetingRecorder(int meetingId, String guildId, AudioChannel channel) {
        this.meetingId = meetingId;
        this.guildId = guildId;
        this.channel = channel;
        this.startTime = Instant.now();
    }

    /**
     * Faixa individual de áudio de um usuário.
     */
    private static class UserTrack {
        final String userId;
        final String userName;
        final Path filePath;
        RandomAccessFile wavFile;
        long totalBytesWritten = 0;
        long lastFrameIndex = -1;
        long lastAudioFrame = -1;
        long speakingStartFrame = -1;
        final List<long[]> speakingSegments = new ArrayList<>();

        UserTrack(String userId, String userName, int meetingId) throws IOException {
            this.userId = userId;
            this.userName = userName;
            String filename = "meeting_" + meetingId + "_track_" + userId + ".wav";
            this.filePath = RECORDINGS_DIR.resolve(filename);
            this.wavFile = new RandomAccessFile(filePath.toFile(), "rw");
            wavFile.write(new byte[44]);
        }

        void padSilenceUpTo(long targetFrame) throws IOException {
            long framesToPad = targetFrame - lastFrameIndex - 1;
            if (framesToPad <= 0) return;
            byte[] silence = new byte[MONO_FRAME_SIZE];
            for (long i = 0; i < framesToPad; i++) {
                wavFile.write(silence);
                totalBytesWritten += MONO_FRAME_SIZE;
            }
        }

        void writeAudio(byte[] monoData) throws IOException {
            wavFile.write(monoData);
            totalBytesWritten += monoData.length;
        }

        void updateSpeaking(long frame) {
            if (speakingStartFrame < 0) speakingStartFrame = frame;
            lastAudioFrame = frame;
        }

        void checkSpeakingGap(long currentFrame) {
            if (speakingStartFrame >= 0 && currentFrame - lastAudioFrame > SPEAKING_GAP_FRAMES) {
                speakingSegments.add(new long[]{ speakingStartFrame * 20, lastAudioFrame * 20 });
                speakingStartFrame = -1;
            }
        }

        void closeSpeaking() {
            if (speakingStartFrame >= 0 && lastAudioFrame >= 0) {
                speakingSegments.add(new long[]{ speakingStartFrame * 20, lastAudioFrame * 20 });
                speakingStartFrame = -1;
            }
        }

        void finalizeWav() throws IOException {
            if (wavFile == null) return;
            int byteRate = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8);
            int blockAlign = CHANNELS * (BITS_PER_SAMPLE / 8);
            wavFile.seek(0);
            wavFile.writeBytes("RIFF");
            wavFile.write(intToLittleEndian((int) (36 + totalBytesWritten)));
            wavFile.writeBytes("WAVE");
            wavFile.writeBytes("fmt ");
            wavFile.write(intToLittleEndian(16));
            wavFile.write(shortToLittleEndian((short) 1));
            wavFile.write(shortToLittleEndian((short) CHANNELS));
            wavFile.write(intToLittleEndian(SAMPLE_RATE));
            wavFile.write(intToLittleEndian(byteRate));
            wavFile.write(shortToLittleEndian((short) blockAlign));
            wavFile.write(shortToLittleEndian((short) BITS_PER_SAMPLE));
            wavFile.writeBytes("data");
            wavFile.write(intToLittleEndian((int) totalBytesWritten));
            wavFile.close();
            wavFile = null;
        }

        String getSpeakingJson() {
            return new Gson().toJson(speakingSegments);
        }
    }

    /**
     * Inicia a gravação de uma reunião num canal de voz/palco.
     */
    public static MeetingRecorder start(Guild guild, AudioChannel channel,
                                         String title, Member startedBy) throws IOException {
        // Verificar se já há gravação nessa guild
        if (ACTIVE_RECORDINGS.containsKey(guild.getId())) {
            throw new IllegalStateException("Já existe uma gravação ativa neste servidor");
        }

        Files.createDirectories(RECORDINGS_DIR);

        // Criar registro no banco
        int meetingId = DatabaseManager.createMeeting(
                title,
                guild.getId(),
                channel.getId(),
                channel.getName(),
                startedBy.getId(),
                startedBy.getEffectiveName()
        );
        if (meetingId < 0) {
            throw new IOException("Falha ao criar registro da reunião no banco");
        }

        MeetingRecorder recorder = new MeetingRecorder(meetingId, guild.getId(), channel);

        // Capturar todos os membros que já estão no canal (plateia + oradores)
        for (Member member : channel.getMembers()) {
            if (!member.getUser().isBot()) {
                recorder.participantIds.add(member.getId());
                recorder.participantNames.putIfAbsent(member.getId(), member.getEffectiveName());
            }
        }

        // Preparar arquivo WAV
        String filename = "meeting_" + meetingId + "_" + System.currentTimeMillis() + ".wav";
        recorder.filePath = RECORDINGS_DIR.resolve(filename);
        recorder.wavFile = new RandomAccessFile(recorder.filePath.toFile(), "rw");
        recorder.writeWavHeader(); // placeholder — atualizado ao parar

        recorder.recording = true;
        ACTIVE_RECORDINGS.put(guild.getId(), recorder);

        // Conectar o bot ao canal de áudio
        AudioManager audioManager = guild.getAudioManager();
        audioManager.setReceivingHandler(recorder);
        audioManager.openAudioConnection(channel);

        LOGGER.info("[MEETING] Gravação #{} iniciada no canal '{}' por {}",
                meetingId, channel.getName(), startedBy.getEffectiveName());

        return recorder;
    }

    /**
     * Para a gravação ativa de uma guild e salva o arquivo.
     */
    public static MeetingRecorder stop(Guild guild) throws IOException {
        MeetingRecorder recorder = ACTIVE_RECORDINGS.remove(guild.getId());
        if (recorder == null) {
            throw new IllegalStateException("Nenhuma gravação ativa neste servidor");
        }

        recorder.recording = false;

        // Desconectar do canal de áudio
        AudioManager audioManager = guild.getAudioManager();
        audioManager.setReceivingHandler(null);
        audioManager.closeAudioConnection();

        // Atualizar header WAV com tamanho real
        recorder.finalizeWav();

        // Finalizar faixas individuais por usuário
        for (UserTrack track : recorder.userTracks.values()) {
            try {
                track.padSilenceUpTo(recorder.frameIndex + 1);
                track.closeSpeaking();
                track.finalizeWav();
                long trackSize = Files.exists(track.filePath) ? Files.size(track.filePath) : 0;
                DatabaseManager.createMeetingTrack(
                        recorder.meetingId,
                        track.userId,
                        track.userName,
                        track.filePath.getFileName().toString(),
                        trackSize,
                        track.getSpeakingJson()
                );
            } catch (IOException e) {
                LOGGER.error("[MEETING] Erro ao finalizar faixa de {}", track.userName, e);
            }
        }

        // Calcular duração
        int duration = (int) (Instant.now().getEpochSecond() - recorder.startTime.getEpochSecond());

        // Fazer scan final do canal antes de desconectar
        if (recorder.channel != null) {
            try {
                for (Member member : recorder.channel.getMembers()) {
                    if (!member.getUser().isBot()) {
                        recorder.participantIds.add(member.getId());
                        recorder.participantNames.putIfAbsent(member.getId(), member.getEffectiveName());
                    }
                }
            } catch (Exception ignored) {}
        }

        // Montar JSON de participantes (com flag de orador)
        List<Map<String, Object>> participants = new ArrayList<>();
        for (String id : recorder.participantIds) {
            String name = recorder.participantNames.getOrDefault(id, "Desconhecido");
            Map<String, Object> p = new HashMap<>();
            p.put("id", id);
            p.put("name", name);
            p.put("speaker", recorder.userTracks.containsKey(id));
            participants.add(p);
        }
        String participantsJson = new Gson().toJson(participants);

        long fileSize = Files.exists(recorder.filePath) ? Files.size(recorder.filePath) : 0;

        // Atualizar banco
        DatabaseManager.finishMeeting(
                recorder.meetingId,
                duration,
                recorder.filePath.getFileName().toString(),
                fileSize,
                recorder.participantIds.size(),
                participantsJson
        );

        LOGGER.info("[MEETING] Gravação #{} finalizada. Duração: {}s, Participantes: {}, Tamanho: {} MB",
                recorder.meetingId, duration, recorder.participantIds.size(),
                String.format("%.1f", fileSize / 1_048_576.0));

        return recorder;
    }

    /**
     * Verifica se há gravação ativa em uma guild.
     */
    public static boolean isRecording(String guildId) {
        return ACTIVE_RECORDINGS.containsKey(guildId);
    }

    /**
     * Retorna a gravação ativa de uma guild (ou null).
     */
    public static MeetingRecorder getActive(String guildId) {
        return ACTIVE_RECORDINGS.get(guildId);
    }

    // ─── AudioReceiveHandler implementation ───

    @Override
    public boolean canReceiveCombined() {
        return recording;
    }

    @Override
    public boolean canReceiveUser() {
        return recording;
    }

    @Override
    public void handleUserAudio(UserAudio userAudio) {
        if (!recording) return;
        if (userAudio.getUser().isBot()) return;

        String userId = userAudio.getUser().getId();
        String userName = userAudio.getUser().getName();
        UserTrack track = userTracks.get(userId);
        if (track == null) {
            try {
                track = new UserTrack(userId, userName, meetingId);
                userTracks.put(userId, track);
                LOGGER.info("[MEETING] Nova faixa individual para '{}' na reunião #{}", userName, meetingId);
            } catch (IOException e) {
                LOGGER.error("[MEETING] Erro ao criar faixa para {}", userName, e);
                return;
            }
        }
        try {
            track.padSilenceUpTo(frameIndex);
            byte[] mono = stereoToMono(userAudio.getAudioData(1.0));
            track.writeAudio(mono);
            track.lastFrameIndex = frameIndex;
            track.updateSpeaking(frameIndex);
        } catch (IOException e) {
            LOGGER.error("[MEETING] Erro ao gravar faixa de {}", userName, e);
        }
    }

    @Override
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        if (!recording || wavFile == null) return;

        frameIndex++;

        // Verificar gaps de fala nas faixas individuais
        for (UserTrack track : userTracks.values()) {
            track.checkSpeakingGap(frameIndex);
        }

        // Registrar participantes que estão falando
        for (var user : combinedAudio.getUsers()) {
            participantIds.add(user.getId());
            participantNames.putIfAbsent(user.getId(), user.getName());
        }

        // A cada ~30s, re-escanear membros do canal para capturar plateia que entrou depois
        if (frameIndex % 1500 == 0 && channel != null) {
            try {
                for (Member member : channel.getMembers()) {
                    if (!member.getUser().isBot()) {
                        participantIds.add(member.getId());
                        participantNames.putIfAbsent(member.getId(), member.getEffectiveName());
                    }
                }
            } catch (Exception ignored) {}
        }

        try {
            byte[] stereoData = combinedAudio.getAudioData(1.0);
            byte[] monoData = stereoToMono(stereoData);
            wavFile.write(monoData);
            totalBytesWritten += monoData.length;

            if (frameIndex % 500 == 0) {
                LOGGER.debug("[MEETING] Gravação #{} — {} KB, {} faixas, {} participantes",
                        meetingId, totalBytesWritten / 1024, userTracks.size(), participantIds.size());
            }
        } catch (IOException e) {
            LOGGER.error("[MEETING] Erro ao escrever áudio da reunião #{}", meetingId, e);
        }
    }

    // ─── WAV helpers ───

    private void writeWavHeader() throws IOException {
        // Escrever header WAV placeholder (44 bytes)
        // Será atualizado com tamanho real em finalizeWav()
        byte[] header = new byte[44];
        wavFile.write(header);
    }

    private void finalizeWav() throws IOException {
        if (wavFile == null) return;

        int byteRate = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8);
        int blockAlign = CHANNELS * (BITS_PER_SAMPLE / 8);

        // Voltar ao início e escrever header real
        wavFile.seek(0);

        // RIFF header
        wavFile.writeBytes("RIFF");
        wavFile.write(intToLittleEndian((int) (36 + totalBytesWritten))); // Tamanho total - 8
        wavFile.writeBytes("WAVE");

        // fmt chunk
        wavFile.writeBytes("fmt ");
        wavFile.write(intToLittleEndian(16));          // Tamanho do chunk fmt
        wavFile.write(shortToLittleEndian((short) 1)); // PCM format
        wavFile.write(shortToLittleEndian((short) CHANNELS));
        wavFile.write(intToLittleEndian(SAMPLE_RATE));
        wavFile.write(intToLittleEndian(byteRate));
        wavFile.write(shortToLittleEndian((short) blockAlign));
        wavFile.write(shortToLittleEndian((short) BITS_PER_SAMPLE));

        // data chunk
        wavFile.writeBytes("data");
        wavFile.write(intToLittleEndian((int) totalBytesWritten));

        wavFile.close();
        wavFile = null;
    }

    /**
     * Converte áudio PCM stereo (L,R interleaved, 16-bit) para mono WAV.
     * JDA entrega big-endian; WAV precisa de little-endian.
     */
    private static byte[] stereoToMono(byte[] stereo) {
        byte[] mono = new byte[stereo.length / 2];
        for (int i = 0; i < stereo.length; i += 4) {
            // Ler samples de 16-bit big-endian (formato JDA)
            int left = (stereo[i] << 8) | (stereo[i + 1] & 0xFF);
            int right = (stereo[i + 2] << 8) | (stereo[i + 3] & 0xFF);
            // Média dos canais
            short mixed = (short) ((left + right) / 2);
            // Escrever como little-endian (formato WAV)
            int monoIdx = i / 2;
            mono[monoIdx] = (byte) (mixed & 0xFF);
            mono[monoIdx + 1] = (byte) ((mixed >> 8) & 0xFF);
        }
        return mono;
    }

    private static byte[] intToLittleEndian(int value) {
        return new byte[]{
            (byte) (value & 0xFF),
            (byte) ((value >> 8) & 0xFF),
            (byte) ((value >> 16) & 0xFF),
            (byte) ((value >> 24) & 0xFF)
        };
    }

    private static byte[] shortToLittleEndian(short value) {
        return new byte[]{
            (byte) (value & 0xFF),
            (byte) ((value >> 8) & 0xFF)
        };
    }

    // ─── Getters ───

    public int getMeetingId() { return meetingId; }
    public Instant getStartTime() { return startTime; }
    public int getParticipantCount() { return participantIds.size(); }
    public Path getFilePath() { return filePath; }

    public String getGuildId() { return guildId; }

    public static Path getRecordingsDir() { return RECORDINGS_DIR; }

    public static Map<String, MeetingRecorder> getActiveRecordings() {
        return Collections.unmodifiableMap(ACTIVE_RECORDINGS);
    }
}

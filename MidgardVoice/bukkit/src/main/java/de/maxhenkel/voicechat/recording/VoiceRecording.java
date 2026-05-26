package de.maxhenkel.voicechat.recording;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.plugins.impl.opus.OpusManager;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class VoiceRecording {

    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 1;

    private final String id;
    private final UUID targetPlayer;
    private final String targetName;
    private final UUID recordedBy;
    private final String recordedByName;
    private final long startTime;
    private long endTime;
    private final List<OpusFrame> frames;
    private boolean active;

    public VoiceRecording(String id, UUID targetPlayer, String targetName, UUID recordedBy, String recordedByName) {
        this.id = id;
        this.targetPlayer = targetPlayer;
        this.targetName = targetName;
        this.recordedBy = recordedBy;
        this.recordedByName = recordedByName;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        this.frames = new ArrayList<>();
        this.active = true;
    }

    public void addFrame(byte[] opusData) {
        if (!active) return;
        frames.add(new OpusFrame(System.currentTimeMillis() - startTime, opusData));
    }

    public void stop() {
        this.active = false;
        this.endTime = System.currentTimeMillis();
    }

    public boolean isActive() {
        return active;
    }

    public String getId() {
        return id;
    }

    public UUID getTargetPlayer() {
        return targetPlayer;
    }

    public String getTargetName() {
        return targetName;
    }

    public UUID getRecordedBy() {
        return recordedBy;
    }

    public String getRecordedByName() {
        return recordedByName;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public int getFrameCount() {
        return frames.size();
    }

    public long getDurationMs() {
        if (endTime > 0) {
            return endTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    public String getFormattedDuration() {
        long totalSec = getDurationMs() / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public String getFormattedStartTime() {
        return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date(startTime));
    }

    public void saveToFile(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File wavFile = new File(directory, id + ".wav");
        File metaFile = new File(directory, id + ".txt");

        saveAsWav(wavFile);

        try (PrintWriter pw = new PrintWriter(new FileWriter(metaFile))) {
            pw.println("ID: " + id);
            pw.println("Jogador: " + targetName);
            pw.println("UUID: " + targetPlayer.toString());
            pw.println("Gravado por: " + recordedByName);
            pw.println("UUID gravador: " + recordedBy.toString());
            pw.println("Inicio: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(startTime)));
            pw.println("Fim: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(endTime)));
            pw.println("Duracao: " + getFormattedDuration());
            pw.println("Frames: " + frames.size());
            pw.println("Arquivo: " + wavFile.getName());
            pw.println("Tamanho: " + wavFile.length() + " bytes");
        } catch (IOException e) {
            Voicechat.LOGGER.error("Failed to save recording metadata: {}", id, e);
        }
    }

    private void saveAsWav(File wavFile) {
        OpusDecoder decoder = OpusManager.createDecoder();
        try {
            // Decode all Opus frames to PCM
            ByteArrayOutputStream pcmStream = new ByteArrayOutputStream();
            for (OpusFrame frame : frames) {
                short[] pcm = decoder.decode(frame.data);
                byte[] pcmBytes = shortsToBytes(pcm);
                pcmStream.write(pcmBytes);
            }
            byte[] pcmData = pcmStream.toByteArray();

            // Write WAV file
            int bitsPerSample = 16;
            int byteRate = SAMPLE_RATE * CHANNELS * (bitsPerSample / 8);
            int blockAlign = CHANNELS * (bitsPerSample / 8);
            int dataSize = pcmData.length;
            int fileSize = 36 + dataSize;

            try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(wavFile)))) {
                // RIFF header
                dos.writeBytes("RIFF");
                dos.write(intToLittleEndian(fileSize));
                dos.writeBytes("WAVE");

                // fmt chunk
                dos.writeBytes("fmt ");
                dos.write(intToLittleEndian(16)); // chunk size
                dos.write(shortToLittleEndian((short) 1)); // PCM format
                dos.write(shortToLittleEndian((short) CHANNELS));
                dos.write(intToLittleEndian(SAMPLE_RATE));
                dos.write(intToLittleEndian(byteRate));
                dos.write(shortToLittleEndian((short) blockAlign));
                dos.write(shortToLittleEndian((short) bitsPerSample));

                // data chunk
                dos.writeBytes("data");
                dos.write(intToLittleEndian(dataSize));
                dos.write(pcmData);

                dos.flush();
            }

            Voicechat.LOGGER.info("Saved recording as WAV: {} ({} bytes)", wavFile.getName(), wavFile.length());
        } catch (Exception e) {
            Voicechat.LOGGER.error("Failed to save recording as WAV: {}", id, e);
        } finally {
            decoder.close();
        }
    }

    private static byte[] intToLittleEndian(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private static byte[] shortToLittleEndian(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
    }

    private static byte[] shortsToBytes(short[] shorts) {
        ByteBuffer bb = ByteBuffer.allocate(shorts.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short s : shorts) {
            bb.putShort(s);
        }
        return bb.array();
    }

    private static class OpusFrame {
        final long timestampMs;
        final byte[] data;

        OpusFrame(long timestampMs, byte[] data) {
            this.timestampMs = timestampMs;
            this.data = data;
        }
    }
}

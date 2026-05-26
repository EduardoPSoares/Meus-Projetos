package de.maxhenkel.voicechat.recording;

import de.maxhenkel.voicechat.Voicechat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceRecordingManager {

    private static final int MAX_SAVED_RECORDINGS = 100;

    private final Map<UUID, VoiceRecording> activeRecordings;
    private final File recordingsDir;

    public VoiceRecordingManager() {
        this.activeRecordings = new ConcurrentHashMap<>();
        this.recordingsDir = new File(Voicechat.INSTANCE.getDataFolder(), "recordings");
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs();
        }
    }

    public VoiceRecording startRecording(UUID targetPlayer, String targetName, UUID recordedBy, String recordedByName) {
        String id = targetName + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        VoiceRecording recording = new VoiceRecording(id, targetPlayer, targetName, recordedBy, recordedByName);
        activeRecordings.put(targetPlayer, recording);
        Voicechat.LOGGER.info("Started recording player {} by {}", targetName, recordedByName);

        if (Voicechat.activityLogger != null) {
            Voicechat.activityLogger.log("[RECORDING_START] " + recordedByName + " started recording " + targetName);
        }

        return recording;
    }

    public VoiceRecording stopRecording(UUID targetPlayer) {
        VoiceRecording recording = activeRecordings.remove(targetPlayer);
        if (recording != null && recording.isActive()) {
            recording.stop();
            recording.saveToFile(recordingsDir);
            enforceRecordingLimit();
            Voicechat.LOGGER.info("Stopped recording {} - {} frames, duration {}", 
                    recording.getTargetName(), recording.getFrameCount(), recording.getFormattedDuration());

            if (Voicechat.activityLogger != null) {
                Voicechat.activityLogger.log("[RECORDING_STOP] " 
                        + recording.getRecordedByName() + " stopped recording " + recording.getTargetName() 
                        + " (duration: " + recording.getFormattedDuration() + ", frames: " + recording.getFrameCount() + ")");
            }
        }
        return recording;
    }

    public boolean isRecording(UUID playerUuid) {
        return activeRecordings.containsKey(playerUuid);
    }

    public VoiceRecording getActiveRecording(UUID playerUuid) {
        return activeRecordings.get(playerUuid);
    }

    public void onAudioPacket(UUID playerUuid, byte[] opusData) {
        VoiceRecording recording = activeRecordings.get(playerUuid);
        if (recording != null && recording.isActive()) {
            recording.addFrame(opusData);
        }
    }

    public Map<UUID, VoiceRecording> getActiveRecordings() {
        return Collections.unmodifiableMap(activeRecordings);
    }

    public List<String> getSavedRecordings() {
        List<String> recordings = new ArrayList<>();
        File[] files = recordingsDir.listFiles((dir, name) -> name.endsWith(".wav"));
        if (files != null) {
            for (File f : files) {
                recordings.add(f.getName().replace(".wav", ""));
            }
            Collections.sort(recordings);
        }
        return recordings;
    }

    public boolean deleteSavedRecording(String id) {
        File dataFile = new File(recordingsDir, id + ".wav");
        File metaFile = new File(recordingsDir, id + ".txt");
        boolean deleted = false;
        if (dataFile.exists()) {
            deleted = dataFile.delete();
        }
        if (metaFile.exists()) {
            metaFile.delete();
        }

        if (deleted && Voicechat.activityLogger != null) {
            Voicechat.activityLogger.log("[RECORDING_DELETE] Deleted recording: " + id);
        }

        return deleted;
    }

    public String getRecordingInfo(String id) {
        File metaFile = new File(recordingsDir, id + ".txt");
        if (!metaFile.exists()) return null;

        try {
            StringBuilder sb = new StringBuilder();
            Scanner scanner = new Scanner(metaFile);
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void enforceRecordingLimit() {
        File[] files = recordingsDir.listFiles((dir, name) -> name.endsWith(".wav"));
        if (files == null || files.length <= MAX_SAVED_RECORDINGS) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        int toDelete = files.length - MAX_SAVED_RECORDINGS;
        for (int i = 0; i < toDelete; i++) {
            String baseName = files[i].getName().replace(".wav", "");
            files[i].delete();
            File metaFile = new File(recordingsDir, baseName + ".txt");
            if (metaFile.exists()) metaFile.delete();
            Voicechat.LOGGER.info("Auto-deleted old recording: {}", baseName);
        }
    }

    public void stopAll() {
        for (UUID uuid : new ArrayList<>(activeRecordings.keySet())) {
            stopRecording(uuid);
        }
    }
}

package com.neeraj.ytsummarizer.util;

import java.io.File;
import java.util.UUID;

public class AudioTranscriptEngine {

    public static File downloadVideoAudio(String videoId) {
        String tempDir = System.getProperty("java.io.tmpdir");
        String uniqueId = UUID.randomUUID().toString();
        String audioOutputPattern = tempDir + File.separator + "yt_audio_" + uniqueId;
        String expectedAudioFile = audioOutputPattern + ".mp3";

        System.out.println("[Audio-Engine] Initializing deep audio download flow for: " + videoId);

        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String dlpCommand = osName.contains("win") ? "yt-dlp.exe" : "yt-dlp";

            // Download raw audio tracks cleanly using native yt-dlp
            ProcessBuilder downloadBuilder = new ProcessBuilder(
                    dlpCommand,
                    "-x", // Extract audio only
                    "--audio-format", "mp3",
                    "--audio-quality", "9", // Low quality taaki file size chota rahe aur fast upload ho
                    "-o", audioOutputPattern + ".%(ext)s",
                    "https://www.youtube.com/watch?v=" + videoId
            );

            Process downloadProcess = downloadBuilder.start();
            int exitCode = downloadProcess.waitFor();

            File fileCheck = new File(expectedAudioFile);
            if (exitCode == 0 && fileCheck.exists()) {
                System.out.println("[Audio-Engine] Audio file saved safely. Size: " + fileCheck.length() + " bytes");
                return fileCheck;
            }

            System.err.println("[Audio-Engine] yt-dlp execution finished but file missing or failed.");
            return null;
        } catch (Exception e) {
            System.err.println("[Audio-Engine] Critical failure downloading audio: " + e.getMessage());
            return null;
        }
    }
}
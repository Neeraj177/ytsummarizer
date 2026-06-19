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

            // 🚀 THE PRO CLOUD FIX: Real User-Agent, custom headers aur extract format mapping
            ProcessBuilder downloadBuilder = new ProcessBuilder(
                    dlpCommand,
                    "-x", // Extract audio only
                    "--audio-format", "mp3",
                    "--audio-quality", "9",
                    "--no-playlist",
                    // 🔥 Fake browser signature taaki Render ka cloud IP block na ho audio fetch karte waqt
                    "--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "--referer", "https://www.google.com/",
                    "-o", audioOutputPattern + ".%(ext)s",
                    "https://www.youtube.com/watch?v=" + videoId
            );

            // Stream logs capturing to see what yt-dlp says on Render cloud
            downloadBuilder.redirectErrorStream(true);
            Process downloadProcess = downloadBuilder.start();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(downloadProcess.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[yt-dlp-audio-stream] " + line); // Yeh cloud dashboard par puri trace dikhayega
            }
            reader.close();

            int exitCode = downloadProcess.waitFor();

            File fileCheck = new File(expectedAudioFile);
            if (exitCode == 0 && fileCheck.exists()) {
                System.out.println("[Audio-Engine] Audio file saved safely. Size: " + fileCheck.length() + " bytes");
                return fileCheck;
            }

            System.err.println("[Audio-Engine] yt-dlp execution finished with exit code " + exitCode + " but file missing.");
            return null;
        } catch (Exception e) {
            System.err.println("[Audio-Engine] Critical failure downloading audio: " + e.getMessage());
            return null;
        }
    }
}
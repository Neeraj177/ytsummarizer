package com.neeraj.ytsummarizer.util;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AudioTranscriptEngine {

    public static File downloadVideoAudio(String videoId) {
        String tempDir = System.getProperty("java.io.tmpdir");
        String uniqueId = UUID.randomUUID().toString();
        String audioOutputPattern = tempDir + File.separator + "yt_audio_" + uniqueId;
        String expectedAudioFile = audioOutputPattern + ".mp3";
        String extractedCookiesPath = tempDir + File.separator + "youtube-cookies.txt";

        System.out.println("[Audio-Engine] Initializing Standard Audio Flow for: " + videoId);

        try {
            File cookieFileTarget = new File(extractedCookiesPath);
            try (InputStream is = AudioTranscriptEngine.class.getResourceAsStream("/youtube-cookies.txt")) {
                if (is != null) {
                    Files.copy(is, cookieFileTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                System.err.println("[Audio-Engine] Cookies copy failed: " + e.getMessage());
            }

            String osName = System.getProperty("os.name").toLowerCase();
            String dlpCommand = osName.contains("win") ? "yt-dlp.exe" : "yt-dlp";

            List<String> command = new ArrayList<>();
            command.add(dlpCommand);

            // 🚀 THE FIX: Fallback formatting strategies to skip client decryption restrictions
            command.add("-f");
            command.add("bestaudio/best");

            command.add("-x");
            command.add("--audio-format");
            command.add("mp3");
            command.add("--audio-quality");
            command.add("9");
            command.add("--no-playlist");
            command.add("--no-check-certificates");

            if (cookieFileTarget.exists() && cookieFileTarget.length() > 0) {
                command.add("--cookies");
                command.add(cookieFileTarget.getAbsolutePath());
            }

            command.add("-o");
            command.add(audioOutputPattern + ".%(ext)s");
            command.add("https://www.youtube.com/watch?v=" + videoId);

            ProcessBuilder downloadBuilder = new ProcessBuilder(command);
            downloadBuilder.redirectErrorStream(true);
            Process downloadProcess = downloadBuilder.start();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(downloadProcess.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[yt-dlp-audio-stream] " + line);
            }
            reader.close();

            int exitCode = downloadProcess.waitFor();

            if (cookieFileTarget.exists()) {
                cookieFileTarget.delete();
            }

            File fileCheck = new File(expectedAudioFile);
            if (exitCode == 0 && fileCheck.exists()) {
                return fileCheck;
            }
            return null;
        } catch (Exception e) {
            System.err.println("[Audio-Engine] Execution exception: " + e.getMessage());
            return null;
        }
    }
}
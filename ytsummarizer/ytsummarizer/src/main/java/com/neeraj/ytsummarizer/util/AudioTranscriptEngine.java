package com.neeraj.ytsummarizer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.UUID;

public class AudioTranscriptEngine {

    public static String transcribeVideoAudio(String videoId) {
        // Temp files storage configurations
        String tempDir = System.getProperty("java.io.tmpdir");
        String uniqueId = UUID.randomUUID().toString();
        String audioOutputPattern = tempDir + File.separator + "yt_audio_" + uniqueId;
        String expectedAudioFile = audioOutputPattern + ".mp3";

        System.out.println("[Audio-Engine] Initializing deep audio download flow for: " + videoId);

        try {
            // 1. Download raw audio tracks cleanly using native yt-dlp binary configuration flags
            ProcessBuilder downloadBuilder = new ProcessBuilder(
                    "yt-dlp",
                    "-x", // Extract audio only
                    "--audio-format", "mp3",
                    "-o", audioOutputPattern + ".%(ext)s",
                    "https://www.youtube.com/watch?v=" + videoId
            );

            Process downloadProcess = downloadBuilder.start();
            downloadProcess.waitFor();

            File fileCheck = new File(expectedAudioFile);
            if (!fileCheck.exists()) {
                System.err.println("[Audio-Engine] Audio extract failed. yt-dlp binary trace skipped.");
                return "";
            }

            System.out.println("[Audio-Engine] Audio file saved to temp storage. Triggering OpenAI Whisper Model...");

            // 2. Trigger OpenAI Whisper using python inline syntax mapping to transcribing text
            String whisperScript = String.format(
                    "import whisper; " +
                            "model = whisper.load_model('tiny'); " + // Using 'tiny' variant for fast memory processing
                            "result = model.transcribe('%s'); " +
                            "print(result['text'])",
                    expectedAudioFile.replace("\\", "\\\\")
            );

            ProcessBuilder whisperBuilder = new ProcessBuilder("python", "-c", whisperScript);

            // Force UTF-8 environmental parameters onto execution context windows
            Map<String, String> env = whisperBuilder.environment();
            env.put("PYTHONIOENCODING", "utf-8");

            Process whisperProcess = whisperBuilder.start();

            // Read the speech-to-text converted words
            StringBuilder transcriptResult = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(whisperProcess.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                transcriptResult.append(line).append(" ");
            }
            reader.close();

            // Check if errors occurred inside the whisper pipeline
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(whisperProcess.getErrorStream(), "UTF-8"));
            String errLine;
            while ((errLine = errorReader.readLine()) != null) {
                System.err.println("[Whisper Core Log] " + errLine);
            }
            errorReader.close();

            whisperProcess.waitFor();

            // 3. Clean up the temp audio footprint file from disk memory safely
            if (fileCheck.exists()) {
                fileCheck.delete();
                System.out.println("[Audio-Engine] Cleaned up temporary audio tracks from storage cache.");
            }

            return transcriptResult.toString().trim();

        } catch (Exception e) {
            System.err.println("[Audio-Engine] Critical failure inside Speech-To-Text fallback architecture: " + e.getMessage());
            return "";
        }
    }
}
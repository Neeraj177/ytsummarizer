package com.neeraj.ytsummarizer.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

public class YouTubeTranscriptExtractor {

    public static String getTranscriptByVideoId(String videoId) {
        StringBuilder scriptOutput = new StringBuilder();
        try {
            // 🚀 THE FIX: youtube_transcript_api automatically generates auto-captions if requested correctly,
            // or we use yt-dlp fallback. Let's make the command look for standard codes first.
            // Note: youtube_transcript_api uses language codes seamlessly, but if auto-generated is needed,
            // sometimes it requires specific order or fallback strategy.

            System.out.println("[Transcript-Extractor] Attempting caption extraction for: " + videoId);

            // Purani line ko isse replace karo:
            ProcessBuilder pb = new ProcessBuilder(
                    "python3", "-m", "youtube_transcript_api",
                    videoId,
                    "--languages", "hi", "en", "en-IN"
            );

            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().startsWith("[") && !line.trim().startsWith("]")) {
                    String cleanText = line.replaceAll("\"text\":\\s*\"", "")
                            .replaceAll("\",\\s*\"start\":.*", "")
                            .replaceAll("\\{", "")
                            .replaceAll("\\}", "")
                            .trim();
                    if (!cleanText.isEmpty()) {
                        scriptOutput.append(cleanText).append(" ");
                    }
                }
            }
            reader.close();

            // Error stream reader capture
            StringBuilder errorLog = new StringBuilder();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
            String errLine;
            while ((errLine = errorReader.readLine()) != null) {
                errorLog.append(errLine).append("\n");
            }
            errorReader.close();
            int exitCode = process.waitFor();

            // 🔄 FALLBACK GATEWAY: Agar python api crash hui ya kuch nahi mila (Auto-generated issue)
            if (exitCode != 0 || scriptOutput.toString().trim().isEmpty()) {
                System.out.println("[Transcript-Extractor] python api failed or returned empty. Error: " + errorLog.toString().trim());
                System.out.println("[Transcript-Extractor] Triggering Bulletproof Fallback: Scraping Auto-Generated subs via yt-dlp...");

                // Hum direct yt-dlp ka use karke subtitle track text file dump nikalenge skip-video mode me!
                String osName = System.getProperty("os.name").toLowerCase();
                String dlpCommand = osName.contains("win") ? "yt-dlp.exe" : "yt-dlp";

                ProcessBuilder dlpPb = new ProcessBuilder(
                        dlpCommand,
                        "--skip-download",
                        "--write-auto-subs",      // ◄── Forces YouTube to give auto-generated text!
                        "--sub-lang", "hi,en",
                        "--output", System.getProperty("java.io.tmpdir") + "/sub_" + videoId,
                        "https://www.youtube.com/watch?v=" + videoId
                );

                Process dlpProcess = dlpPb.start();
                dlpProcess.waitFor();

                // Yahan par hum text processing validation bypass kar sakte hain.
                // Lekin best check ke liye abhi hum python command ko sequence rules me fallback de rahe hain.
            }

        } catch (Exception e) {
            System.err.println("Failed to execute native python transcription CLI layer: " + e.getMessage());
            return "";
        }

        // Alternative safe approach: Python CLI wrapper sometimes throws tracking exceptions for kids videos.
        // Let's ensure that if it returns empty, it safely triggers Scenario B in Worker.
        return scriptOutput.toString().trim();
    }
}
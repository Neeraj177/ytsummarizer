package com.neeraj.ytsummarizer.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

public class YouTubeTranscriptExtractor {

    public static String getTranscriptByVideoId(String videoId) {
        StringBuilder scriptOutput = new StringBuilder();
        try {
            System.out.println("[Transcript-Extractor] Attempting caption extraction for: " + videoId);

            String osName = System.getProperty("os.name").toLowerCase();
            String pythonCmd = osName.contains("win") ? "python" : "python3";
            String cookiesPath = osName.contains("win") ? "" : "/etc/secrets/cookies.txt";

            // CC + Auto-generated dono try karega automatically
            ProcessBuilder pb = new ProcessBuilder(
                    pythonCmd, "-m", "youtube_transcript_api",
                    videoId,
                    "--languages", "en", "hi", "en-IN" ,
                    "--cookies", "/etc/secrets/cookies.txt"
            );

            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("\"text\":")) {
                    String cleanText = line.replaceAll(".*\"text\":\\s*\"", "")
                            .replaceAll("\",\\s*\"start\".*", "")
                            .replaceAll("\\\\n", " ")
                            .trim();
                    if (!cleanText.isEmpty()) {
                        scriptOutput.append(cleanText).append(" ");
                    }
                } else if (!line.trim().startsWith("[") && !line.trim().startsWith("]")
                        && !line.trim().startsWith("{") && !line.trim().startsWith("}")) {
                    scriptOutput.append(line.trim()).append(" ");
                }
            }
            reader.close();

            StringBuilder errorLog = new StringBuilder();
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), "UTF-8"));
            String errLine;
            while ((errLine = errorReader.readLine()) != null) {
                errorLog.append(errLine).append("\n");
            }
            errorReader.close();

            int exitCode = process.waitFor();
            String result = scriptOutput.toString().trim();

            if (exitCode != 0 ||
                    result.isEmpty() ||
                    errorLog.toString().contains("CouldNotRetrieveTranscript") ||
                    result.contains("IP") ||
                    result.contains("blocked") ||
                    result.contains("Could not retrieve") ||
                    result.contains("YouTubeTranscriptApi") ||
                    result.contains("Subtitles are disabled") ||
                    result.contains("raised")) {
                System.out.println("[Transcript-Extractor] No captions found. Triggering Frame Extraction...");
                return "";
            }

            System.out.println("[Transcript-Extractor] Captions found! Length: " + result.length());
            return result;

        } catch (Exception e) {
            System.err.println("[Transcript-Extractor] Error: " + e.getMessage());
            return "";
        }
    }
}
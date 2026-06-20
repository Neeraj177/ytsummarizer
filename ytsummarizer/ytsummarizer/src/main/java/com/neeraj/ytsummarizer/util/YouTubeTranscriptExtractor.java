package com.neeraj.ytsummarizer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class YouTubeTranscriptExtractor {

    // ✅ Cookies file ko resources se /tmp mein copy karo
    private static String getWritableCookiesPath() {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            String targetPath = tmpDir + File.separator + "youtube-cookies.txt";
            File targetFile = new File(targetPath);

            if (!targetFile.exists()) {
                InputStream is = YouTubeTranscriptExtractor.class
                        .getClassLoader()
                        .getResourceAsStream("youtube-cookies.txt");

                if (is != null) {
                    Files.copy(is, Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
                    is.close();
                    System.out.println("[Transcript-Extractor] Cookies loaded from resources ✅");
                } else {
                    System.out.println("[Transcript-Extractor] No cookies file found in resources.");
                    return null;
                }
            }
            return targetPath;
        } catch (Exception e) {
            System.out.println("[Transcript-Extractor] Cookies setup failed: " + e.getMessage());
            return null;
        }
    }

    // ✅ Python script ko resources se /tmp mein copy karo
    private static String getWritablePythonScriptPath() {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            String targetPath = tmpDir + File.separator + "transcript_fetcher.py";
            File targetFile = new File(targetPath);

            // Hamesha fresh copy rakho taaki updates reflect ho
            InputStream is = YouTubeTranscriptExtractor.class
                    .getClassLoader()
                    .getResourceAsStream("transcript_fetcher.py");

            if (is != null) {
                Files.copy(is, Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
                is.close();
                System.out.println("[Transcript-Extractor] Python script loaded from resources ✅");
            } else {
                System.out.println("[Transcript-Extractor] No transcript_fetcher.py found in resources.");
                return null;
            }
            return targetPath;
        } catch (Exception e) {
            System.out.println("[Transcript-Extractor] Script setup failed: " + e.getMessage());
            return null;
        }
    }

    public static String getTranscriptByVideoId(String videoId) {
        StringBuilder scriptOutput = new StringBuilder();

        try {
            System.out.println("[Transcript-Extractor] Attempting caption extraction for: " + videoId);

            String osName = System.getProperty("os.name").toLowerCase();
            String pythonCmd = osName.contains("win") ? "python" : "python3";

            String cookiesPath = getWritableCookiesPath();
            String scriptPath = getWritablePythonScriptPath();

            if (scriptPath == null) {
                System.out.println("[Transcript-Extractor] Script missing, cannot proceed.");
                return "";
            }

            // ✅ Naya custom Python script call karo, CLI module ki jagah
            List<String> command = new ArrayList<>(Arrays.asList(
                    pythonCmd, scriptPath,
                    videoId
            ));

            if (cookiesPath != null) {
                command.add(cookiesPath);
                System.out.println("[Transcript-Extractor] Using bundled cookies ✅");
            }

            ProcessBuilder pb = new ProcessBuilder(command);

            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                scriptOutput.append(line.trim()).append(" ");
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
                    result.contains("ERROR:") ||
                    result.contains("IP") ||
                    result.contains("blocked") ||
                    result.contains("Could not retrieve") ||
                    result.contains("Subtitles are disabled")) {
                System.out.println("[Transcript-Extractor] No captions found. Error: " + errorLog.toString().trim());
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
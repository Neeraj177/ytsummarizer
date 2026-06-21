package com.neeraj.ytsummarizer.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class YouTubeTranscriptExtractor {

    private static final String API_KEY = System.getenv("SUPADATA_API_KEY");

    public static String getTranscriptByVideoId(String videoId) {
        try {
            System.out.println("[Transcript-Extractor] Fetching via Supadata for: " + videoId);

            String youtubeUrl = "https://www.youtube.com/watch?v=" + videoId;
            String apiUrl = "https://api.supadata.ai/v1/transcript?url=" +
                    java.net.URLEncoder.encode(youtubeUrl, "UTF-8") + "&text=true";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("x-api-key", API_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[Transcript-Extractor] Supadata response code: " + response.statusCode());

            if (response.statusCode() == 200) {
                String body = response.body();
                // JSON se "content" field extract karo
                String content = body.replaceAll(".*\"content\":\\s*\"", "")
                        .replaceAll("\",\\s*\"lang\".*", "")
                        .replaceAll("\\\\n", " ")
                        .replaceAll("\\\\\"", "\"")
                        .trim();

                System.out.println("[Transcript-Extractor] Captions found! Length: " + content.length());
                return content;
            } else {
                System.out.println("[Transcript-Extractor] No transcript available. Body: " + response.body());
                return "";
            }

        } catch (Exception e) {
            System.err.println("[Transcript-Extractor] Error: " + e.getMessage());
            return "";
        }
    }
}
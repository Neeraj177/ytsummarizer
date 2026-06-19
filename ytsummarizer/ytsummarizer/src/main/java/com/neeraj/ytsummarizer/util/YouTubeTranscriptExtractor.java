package com.neeraj.ytsummarizer.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class YouTubeTranscriptExtractor {

    public static String extractTranscript(String videoId) {
        String apiKey = System.getenv("SCRAPING_ANT_API_KEY");

        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("PLACEHOLDER_KEY")) {
            System.err.println("[Transcript-Extractor] ⚠️ CRITICAL: SCRAPING_ANT_API_KEY env variable is missing!");
            return null;
        }

        System.out.println("[Transcript-Extractor] Triggering Smart Headless Ant-Bypass for: " + videoId);

        try {
            String targetUrl = "https://www.youtube.com/watch?v=" + videoId;
            String encodedUrl = URLEncoder.encode(targetUrl, StandardCharsets.UTF_8);

            // 🚀 THE FIX: Adding &browser=true triggers ScrapingAnt's real Chrome cluster,
            // preventing the 422 validation error on dynamic streaming sites like YouTube.
            String requestUrl = "https://api.scrapingant.com/v2/general?url=" + encodedUrl
                    + "&x-api-key=" + apiKey
                    + "&browser=true"; // ◄── Forces Chrome rendering to execute signatures

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String htmlContent = response.body();

                if (htmlContent.contains("captionTracks")) {
                    System.out.println("[Transcript-Extractor] Success! Captions payload discovered inside ScrapingAnt browser stream.");
                    return parseTranscriptFromHtml(htmlContent);
                } else {
                    System.out.println("[Transcript-Extractor] Page loaded via Proxy, but no native captionTracks found in this video's source HTML.");
                }
            } else {
                System.err.println("[Transcript-Extractor] ScrapingAnt API Node failed. Status: " + response.statusCode()
                        + " | Response body snapshot: " + (response.body().length() > 200 ? response.body().substring(0, 200) : response.body()));
            }
            return null;
        } catch (Exception e) {
            System.err.println("[Transcript-Extractor] Exception during Proxy Ingestion: " + e.getMessage());
            return null;
        }
    }

    private static String parseTranscriptFromHtml(String html) {
        try {
            int index = html.indexOf("\"captionTracks\":[");
            if (index == -1) return null;

            int start = html.indexOf("[", index);
            int end = html.indexOf("]", start) + 1;
            String jsonArrayStr = html.substring(start, end);

            System.out.println("[Transcript-Extractor] Text structures mapped successfully.");
            return "Scraped Caption Tracks Context Data: " + jsonArrayStr;
        } catch (Exception e) {
            System.err.println("[Transcript-Extractor] Error parsing text blocks from HTML structure: " + e.getMessage());
            return null;
        }
    }
}
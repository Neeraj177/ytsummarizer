package com.neeraj.ytsummarizer.service;

import java.io.File;
import java.util.List;

public interface AiSummarizerService {
    String generateSummary(String videoTranscript);
    String generateVisualSummary(List<File> imageFiles);
    String handleChatContext(String videoSummary, List<com.neeraj.ytsummarizer.entity.ChatMessage> history, String userQuestion);
}
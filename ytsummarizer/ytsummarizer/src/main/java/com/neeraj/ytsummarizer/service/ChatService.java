package com.neeraj.ytsummarizer.service;

import com.neeraj.ytsummarizer.entity.ChatMessage;
import java.util.List;
import java.util.UUID;

public interface ChatService {
    String processUserMessage(UUID jobId, String userQuestion);
    List<ChatMessage> getChatHistory(UUID jobId);
}
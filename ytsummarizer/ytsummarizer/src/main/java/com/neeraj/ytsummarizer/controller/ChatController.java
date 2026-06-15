package com.neeraj.ytsummarizer.controller;

import com.neeraj.ytsummarizer.entity.ChatMessage;
import com.neeraj.ytsummarizer.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin(origins = "http://localhost:5173") // Cross-Origin adjustments for Frontend compatibility
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // 1. End point to post a new question to the specific summary channel
    @PostMapping("/{jobId}")
    public ResponseEntity<Map<String, String>> askQuestion(@PathVariable UUID jobId, @RequestBody Map<String, String> payload) {
        String userQuestion = payload.get("question");
        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question cannot be empty content."));
        }

        String responseAnswer = chatService.processUserMessage(jobId, userQuestion);
        return ResponseEntity.ok(Map.of("answer", responseAnswer));
    }

    // 2. End point to pull history logs for refreshing state layouts
    @GetMapping("/{jobId}")
    public ResponseEntity<List<ChatMessage>> getHistoryLogs(@PathVariable UUID jobId) {
        return ResponseEntity.ok(chatService.getChatHistory(jobId));
    }
}
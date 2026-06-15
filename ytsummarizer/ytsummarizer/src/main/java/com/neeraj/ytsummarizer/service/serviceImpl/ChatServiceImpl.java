package com.neeraj.ytsummarizer.service.serviceImpl;

import com.neeraj.ytsummarizer.entity.ChatMessage;
import com.neeraj.ytsummarizer.entity.VideoJob;
import com.neeraj.ytsummarizer.model.enums.JobStatus;
import com.neeraj.ytsummarizer.repository.ChatMessageRepository;
import com.neeraj.ytsummarizer.repository.VideoJobRepository;
import com.neeraj.ytsummarizer.service.AiSummarizerService;
import com.neeraj.ytsummarizer.service.ChatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final VideoJobRepository videoJobRepository;
    private final AiSummarizerService aiSummarizerService;

    public ChatServiceImpl(ChatMessageRepository chatMessageRepository,
                           VideoJobRepository videoJobRepository,
                           AiSummarizerService aiSummarizerService) {
        this.chatMessageRepository = chatMessageRepository;
        this.videoJobRepository = videoJobRepository;
        this.aiSummarizerService = aiSummarizerService;
    }

    @Override
    @Transactional
    public String processUserMessage(UUID jobId, String userQuestion) {
        // 1. Check if video job exists and is COMPLETED
        VideoJob job = videoJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Video job data context not found."));

        if (job.getStatus() != JobStatus.COMPLETED) {
            return "⚠️ Chat feature is unavailable because the video is not successfully summarized yet.";
        }

        // 2. Save User Message into PostgreSQL
        ChatMessage userMessage = ChatMessage.builder()
                .content(userQuestion)
                .sender("USER")
                .videoJob(job)
                .build();
        chatMessageRepository.save(userMessage);

        // 3. Fetch past chat history logs
        List<ChatMessage> activeHistory = chatMessageRepository.findByVideoJobIdOrderByCreatedAtAsc(jobId);

        // 4. Fire prompt with context to Gemini via AI Service
        String aiResponseText = aiSummarizerService.handleChatContext(job.getSummary(), activeHistory, userQuestion);

        // 5. Save AI Response into PostgreSQL
        ChatMessage aiMessage = ChatMessage.builder()
                .content(aiResponseText)
                .sender("AI")
                .videoJob(job)
                .build();
        chatMessageRepository.save(aiMessage);

        return aiResponseText;
    }

    @Override
    public List<ChatMessage> getChatHistory(UUID jobId) {
        return chatMessageRepository.findByVideoJobIdOrderByCreatedAtAsc(jobId);
    }
}
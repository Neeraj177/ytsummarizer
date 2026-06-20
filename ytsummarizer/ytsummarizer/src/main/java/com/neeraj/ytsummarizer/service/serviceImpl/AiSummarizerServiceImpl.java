package com.neeraj.ytsummarizer.service.serviceImpl;

import com.neeraj.ytsummarizer.entity.ChatMessage;
import com.neeraj.ytsummarizer.service.AiSummarizerService;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.ai.content.Media;
import java.io.File;
import java.util.*;

@Service
public class AiSummarizerServiceImpl implements AiSummarizerService {

    private final ChatModel chatModel;

    public AiSummarizerServiceImpl(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String generateSummary(String transcript) {
        if (transcript == null || transcript.trim().isEmpty()) {
            return "No content available to summarize.";
        }

        String prompt = """
    You are an expert content summarizer. Analyze the provided video transcript and generate a response in this exact format:
    
    ## 📌 Summary
    Write a detailed paragraph covering everything discussed in the video. Include all main points, 
    examples, explanations, and conclusions. Write in English regardless of transcript language.
    """;

        return chatModel.call(new Prompt(prompt)).getResult().getOutput().getText();
    }

    @Override
    public String generateAudioSummary(File audioFile) {
        try {
            if (audioFile == null || !audioFile.exists()) {
                return "⚠️ Audio file context is missing or download failed.";
            }

            System.out.println("[Gemini-Audio] Dispatching audio binary stream to Gemini 2.5 Flash...");

            String audioPrompt = """
        You are an expert audio intelligence AI. Listen to this extracted audio stream carefully.
        The audio belongs to a YouTube video. 
        
        Generate a highly detailed, professional markdown comprehensive summary in English covering:
        1. Main topics discussed.
        2. Key takeaways, code walk-throughs, or steps mentioned.
        3. A final structured wrap-up.
        """;

            org.springframework.ai.content.Media audioMedia = org.springframework.ai.content.Media.builder()
                    .mimeType(org.springframework.util.MimeTypeUtils.parseMimeType("audio/mpeg"))
                    .data(new org.springframework.core.io.FileSystemResource(audioFile))
                    .build();

            org.springframework.ai.chat.messages.UserMessage message = org.springframework.ai.chat.messages.UserMessage.builder()
                    .text(audioPrompt)
                    .media(java.util.List.of(audioMedia))
                    .build();

            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(new org.springframework.ai.chat.prompt.Prompt(message));

            if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getText();
            }

            return "⚠️ Audio engine returned an empty summary template.";

        } catch (Exception e) {
            throw new RuntimeException("Gemini Audio processing pipeline failed: " + e.getMessage());
        }
    }

    @Override
    public String generateVisualSummary(List<File> imageFiles) { return "Not implemented"; }

    @Override
    public String handleChatContext(String videoSummary, List<ChatMessage> history, String userQuestion) {
        try {
            StringBuilder systemPromptBuilder = new StringBuilder();
            systemPromptBuilder.append("You are an AI Assistant specialized in answering questions about a video summary.\nContext:\n" + videoSummary);
            UserMessage messagePayload = new UserMessage(systemPromptBuilder.toString() + "\nUser Question: " + userQuestion);
            ChatResponse response = chatModel.call(new Prompt(messagePayload));
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            return "⚠️ Chat engine error.";
        }
    }
}
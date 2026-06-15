package com.neeraj.ytsummarizer.service.serviceImpl;

import com.neeraj.ytsummarizer.entity.ChatMessage;
import com.neeraj.ytsummarizer.service.AiSummarizerService;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel; // Base interface import
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.ai.content.Media;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

@Service
public class AiSummarizerServiceImpl implements AiSummarizerService {

    private final ChatModel chatModel;

    // Spring will automatically inject the autoconfigured Gemini model bean here!
    public AiSummarizerServiceImpl(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String generateSummary(String input) {  if (input == null || input.trim().isEmpty()) {
        return "No content available to summarize.";
    }

        String systemPrompt = """
    You are an expert technical content summarizer. Your job is to analyze the provided YouTube video transcript 
    and generate a clean, professional, and well-structured markdown summary.
    
    CRITICAL NOTE: The transcript might be auto-generated or written in Hindi, English, or a mix of both (Hinglish). 
    Regardless of the input language, you must understand the core context and ALWAYS generate the final summary 
    strictly in English.
    
    Follow this structural schema strictly:
    # [Professional Video Title Placeholder/Theme]
    
    ## 📌 Executive Summary
    - A concise 2-3 sentence overview of what the video covers.
    
    ## 🔑 Key Takeaways
    - Bullet points highlighting the core concepts, frameworks, or insights discussed.
    - Bold key architectural terms or technical definitions.
    
    ## 💻 Technical Stack / Tools Mentioned (If applicable)
    - List any specific programming languages, libraries, databases, or frameworks referenced.
    
    Ensure your output uses clean markdown headers, bold text, and bullet points. Do not include conversational introductory text.
    """;

        String fullPrompt;

        // YouTube URL hai ya transcript?
        if (input.startsWith("https://www.youtube.com")) {
            fullPrompt = systemPrompt + "\n\nPlease summarize this YouTube video: " + input;
        } else {
            fullPrompt = systemPrompt + "\n\nHere is the video transcript:\n" + input;
        }

        return chatModel.call(new Prompt(fullPrompt))
                .getResult()
                .getOutput()
                .getText();
    }
    @Override
    public String generateVisualSummary(List<File> imageFiles) {
        try {
            String visionInstructionPrompt = """
            You are an expert visual intelligence AI. This is a silent video with absolutely no narration or audio dialogue.
            Analyze these sequential video frames extracted chronologically from the video.
            
            Perform these actions:
            1. Read any text written on the screen, code editors, slides, or software UI via OCR.
            2. Observe visual transitions, actions, mouse movements, or setup procedures.
            3. Generate a highly detailed, professional markdown technical summary in English.
            
            Follow this schema:
            # [Visual Theme/Title of the Video]
            ## 📌 Visual Execution Summary
            - 2-3 sentences explaining what this video shows or teaches visually.
            ## 🔑 Steps / Milestones Tracked Visually
            - Group steps based on frame progressions. 
            - Bold UI buttons clicked, commands written on screen, or code logic blocks implemented.
            """;

            List<Media> mediaList = new ArrayList<>();
            for (File file : imageFiles) {
                if (!file.exists()) continue;

                mediaList.add(Media.builder()
                        .mimeType(MimeTypeUtils.IMAGE_JPEG)
                        .data(new FileSystemResource(file))  // ✅ Resource directly, no Base64 string
                        .build());
            }

            // Build Multi-Modal parameters wrapper safely
            UserMessage multiModalUserMessage = UserMessage.builder()
                    .text(visionInstructionPrompt)
                    .media(mediaList)
                    .build();

            // Fire prompt transaction over the network layer
            ChatResponse response = chatModel.call(new Prompt(multiModalUserMessage));

            if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getText();
            }

            return "⚠️ Vision engine returned an empty response template.";

        } catch (Exception e) {
            throw new RuntimeException("Gemini Vision processing pipeline failed: " + e.getMessage());
        }
    }

    @Override
    public String handleChatContext(String videoSummary, List<com.neeraj.ytsummarizer.entity.ChatMessage> history, String userQuestion) {
        try {
            // 1. Build a strict System Instruction prompt
            StringBuilder systemPromptBuilder = new StringBuilder();
            systemPromptBuilder.append("""
            You are an AI Assistant specialized in answering questions about a specific YouTube video.
            You must strictly answer the user's question based ONLY on the provided Video Summary context below.
            If the answer cannot be inferred from the summary, politely tell the user that the information is not present in the video context.
            
            [START OF VIDEO SUMMARY CONTEXT]
            """);
            systemPromptBuilder.append(videoSummary);
            systemPromptBuilder.append("\n[END OF VIDEO SUMMARY CONTEXT]\n\n");

            // 2. Append Chat History to maintain conversational memory loop
            if (history != null && !history.isEmpty()) {
                systemPromptBuilder.append("[START OF PREVIOUS CHAT HISTORY]\n");
                for (com.neeraj.ytsummarizer.entity.ChatMessage msg : history) {
                    systemPromptBuilder.append(msg.getSender()).append(": ").append(msg.getContent()).append("\n");
                }
                systemPromptBuilder.append("[END OF PREVIOUS CHAT HISTORY]\n\n");
            }

            systemPromptBuilder.append("Now, answer this specific user question comprehensively based on the details above.\n");

            // 3. Create the final UserMessage containing the full loaded context + current question
            org.springframework.ai.chat.messages.UserMessage messagePayload =
                    new org.springframework.ai.chat.messages.UserMessage(systemPromptBuilder.toString() + "User Question: " + userQuestion);

            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(new org.springframework.ai.chat.prompt.Prompt(messagePayload));

            if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getText();
            }

            return "⚠️ Chat engine could not generate a valid response.";
        } catch (Exception e) {
            throw new RuntimeException("Gemini Chat Matrix failed: " + e.getMessage());
        }
    }
}

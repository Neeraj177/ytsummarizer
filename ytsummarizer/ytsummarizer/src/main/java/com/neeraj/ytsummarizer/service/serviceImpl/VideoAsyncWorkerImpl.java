package com.neeraj.ytsummarizer.service.serviceImpl;

import com.neeraj.ytsummarizer.entity.VideoJob;
import com.neeraj.ytsummarizer.model.enums.JobStatus;
import com.neeraj.ytsummarizer.repository.VideoJobRepository;
import com.neeraj.ytsummarizer.service.AiSummarizerService;
import com.neeraj.ytsummarizer.service.VideoAsyncWorker;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.UUID;

@Service
public class VideoAsyncWorkerImpl implements VideoAsyncWorker {

    private final VideoJobRepository videoJobRepository;
    private final AiSummarizerService aiSummarizerService;

    public VideoAsyncWorkerImpl(VideoJobRepository videoJobRepository,
                                AiSummarizerService aiSummarizerService) {
        this.videoJobRepository = videoJobRepository;
        this.aiSummarizerService = aiSummarizerService;
    }

    @Override
    @Async("videoJobExecutor")
    public void processVideoAsynchronously(UUID jobId) {
        try {
            VideoJob job = videoJobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

            job.setStatus(JobStatus.PROCESSING);
            videoJobRepository.save(job);
            System.out.println("[" + Thread.currentThread().getName() + "] Job " + jobId + " → PROCESSING");

            String transcript = com.neeraj.ytsummarizer.util.YouTubeTranscriptExtractor
                    .extractTranscript(job.getVideoId());

            String finalSummaryResult;

            if (transcript != null && !transcript.trim().isEmpty()) {
                // ✅ Scenario A: Captions mile (CC ya Auto-generated)
                System.out.println("[" + Thread.currentThread().getName() + "] Captions found! Sending to Gemini...");
                finalSummaryResult = aiSummarizerService.generateSummary(transcript);
            } else {
                // 🚀 Solution 3 Fallback: No captions → Try Multimodal Audio Ingestion
                System.out.println("[" + Thread.currentThread().getName() + "] No captions. Activating Multimodal Audio Fallback...");
                File audioFile = com.neeraj.ytsummarizer.util.AudioTranscriptEngine.downloadVideoAudio(job.getVideoId());

                if (audioFile != null && audioFile.exists()) {
                    System.out.println("[" + Thread.currentThread().getName() + "] Audio fetched successfully! Feeding to Gemini...");
                    finalSummaryResult = aiSummarizerService.generateAudioSummary(audioFile);

                    // Cleanup audio file immediately after processing
                    if (audioFile.delete()) {
                        System.out.println("[Audio-Engine] Temporary audio file deleted safely.");
                    }
                } else {
                    // ⚠️ Agar audio bhi fail ho gaya, tab safe side frames par chale jao
                    System.out.println("[" + Thread.currentThread().getName() + "] Audio failed. Moving to frame extraction...");
                    List<File> extractedFrames = com.neeraj.ytsummarizer.util.VideoVisionExtractor.extractVideoFrames(job.getVideoId());

                    if (extractedFrames != null && !extractedFrames.isEmpty()) {
                        finalSummaryResult = aiSummarizerService.generateVisualSummary(extractedFrames);
                        com.neeraj.ytsummarizer.util.VideoVisionExtractor.cleanUpFrames(extractedFrames);
                    } else {
                        finalSummaryResult = "### ⚠️ Pipeline Failure\nCaptions nahi mile, Audio bypass fail hua, aur Frames bhi extract nahi ho paaye.";
                    }
                }
            }

            // ✅ State updates database mein save karo
            job.setSummary(finalSummaryResult);
            job.setStatus(finalSummaryResult.contains("### ⚠️ Pipeline Failure") ? JobStatus.FAILED : JobStatus.COMPLETED);
            videoJobRepository.save(job);
            System.out.println("[" + Thread.currentThread().getName() + "] Job " + jobId + " done with status: " + job.getStatus());

        } catch (Exception e) {
            System.err.println("Critical failure for job " + jobId + ": " + e.getMessage());
            markJobFailed(jobId, e.getMessage());
        }
    }

    @Override
    @Async("videoJobExecutor")
    public void processVideoAsynchronously(UUID jobId, String transcript) {
        try {
            VideoJob job = videoJobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

            job.setStatus(JobStatus.PROCESSING);
            videoJobRepository.save(job);
            System.out.println("[" + Thread.currentThread().getName() + "] Job with pre-transcript " + jobId + " → PROCESSING");

            String finalSummaryResult;

            if (transcript != null && !transcript.trim().isEmpty()) {
                // ✅ Frontend se transcript aaya — seedha Gemini ko do
                System.out.println("[Worker] Frontend transcript received! Length: " + transcript.length());
                finalSummaryResult = aiSummarizerService.generateSummary(transcript);
            } else {
                // Backend se extract karo pahle
                String liveTranscript = com.neeraj.ytsummarizer.util.YouTubeTranscriptExtractor
                        .extractTranscript(job.getVideoId());

                if (liveTranscript != null && !liveTranscript.trim().isEmpty()) {
                    System.out.println("[Worker] Backend transcript found! Length: " + liveTranscript.length());
                    finalSummaryResult = aiSummarizerService.generateSummary(liveTranscript);
                } else {
                    // 🚀 Solution 3 Fallback applied here too!
                    System.out.println("[Worker] No transcript found via scraping. Activating Multimodal Audio Fallback...");
                    File audioFile = com.neeraj.ytsummarizer.util.AudioTranscriptEngine.downloadVideoAudio(job.getVideoId());

                    if (audioFile != null && audioFile.exists()) {
                        System.out.println("[Worker] Audio fetched successfully! Feeding to Gemini...");
                        finalSummaryResult = aiSummarizerService.generateAudioSummary(audioFile);

                        if (audioFile.delete()) {
                            System.out.println("[Audio-Engine] Temporary audio file deleted safely.");
                        }
                    } else {
                        // Frame extraction as last resort
                        System.out.println("[Worker] Audio failed. Starting frame extraction...");
                        List<File> extractedFrames = com.neeraj.ytsummarizer.util.VideoVisionExtractor.extractVideoFrames(job.getVideoId());

                        if (extractedFrames != null && !extractedFrames.isEmpty()) {
                            finalSummaryResult = aiSummarizerService.generateVisualSummary(extractedFrames);
                            com.neeraj.ytsummarizer.util.VideoVisionExtractor.cleanUpFrames(extractedFrames);
                        } else {
                            finalSummaryResult = "### ⚠️ Pipeline Failure\nCaptions nahi mile, Audio bypass fail hua, aur Frames bhi extract nahi ho paaye.";
                        }
                    }
                }
            }

            // ✅ Save output states safely
            job.setSummary(finalSummaryResult);
            job.setStatus(finalSummaryResult.contains("### ⚠️ Pipeline Failure") ? JobStatus.FAILED : JobStatus.COMPLETED);
            videoJobRepository.save(job);
            System.out.println("[" + Thread.currentThread().getName() + "] Job " + jobId + " done with status: " + job.getStatus());

        } catch (Exception e) {
            System.err.println("Critical failure for job " + jobId + ": " + e.getMessage());
            markJobFailed(jobId, e.getMessage());
        }
    }

    private void markJobFailed(UUID jobId, String reason) {
        videoJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.FAILED);
            job.setSummary("### ⚠️ Processing Failed\n" + reason);
            videoJobRepository.save(job);
        });
    }
}
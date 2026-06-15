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
            // 1. Mark the job state as PROCESSING in PostgreSQL
            VideoJob job = videoJobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job execution task context not found for ID: " + jobId));

            job.setStatus(JobStatus.PROCESSING);
            videoJobRepository.save(job);
            System.out.println("[" + Thread.currentThread().getName() + "] Job " + jobId + " advanced to PROCESSING status.");

            // 🚀 STRATEGY 1: Extract subtitles track directly
            System.out.println("[" + Thread.currentThread().getName() + "] Scraping closed captions track for videoId: " + job.getVideoId());

            // Yahan temporary testing ke liye agar tum ise "" rakhna chaho toh rakh sakte ho, 
            // Varna live production ke liye original script use hogi:
            String liveExtractedTranscript = com.neeraj.ytsummarizer.util.YouTubeTranscriptExtractor.getTranscriptByVideoId(job.getVideoId());
//            String liveExtractedTranscript = "";

            String finalSummaryResult = "";

            if (liveExtractedTranscript != null && !liveExtractedTranscript.trim().isEmpty()) {
                // Scenario A: Subtitles mil gaye -> Run standard Gemini Text Model
                System.out.println("[" + Thread.currentThread().getName() + "] Subtitles found! Length: " + liveExtractedTranscript.length() + ". Contacting Gemini Text Model...");
                finalSummaryResult = aiSummarizerService.generateSummary(liveExtractedTranscript);
            } else {
                // 🚀 SCENARIO B: No Subtitles -> Direct YouTube URL to Gemini Flash
                System.out.println("[" + Thread.currentThread().getName() + "] No subtitles found. Sending YouTube URL directly to Gemini...");

                String youtubeUrl = "https://www.youtube.com/watch?v=" + job.getVideoId();
                finalSummaryResult = aiSummarizerService.generateSummary(youtubeUrl);
            }

            // 3. Final database entry compilation
            job.setSummary(finalSummaryResult);
            if (finalSummaryResult.contains("### ⚠️ Pipeline Failure")) {
                job.setStatus(JobStatus.FAILED);
            } else {
                job.setStatus(JobStatus.COMPLETED);
            }

            videoJobRepository.save(job);
            System.out.println("[" + Thread.currentThread().getName() + "] Job " + jobId + " → " + job.getStatus());

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
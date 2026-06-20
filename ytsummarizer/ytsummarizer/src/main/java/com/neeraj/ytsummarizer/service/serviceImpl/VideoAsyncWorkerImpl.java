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

            // Direct cookies-based audio extraction workflow trigger
            System.out.println("[" + Thread.currentThread().getName() + "] Activating Cookies-Based Audio Ingestion...");
            File audioFile = com.neeraj.ytsummarizer.util.AudioTranscriptEngine.downloadVideoAudio(job.getVideoId());

            String finalSummaryResult;

            if (audioFile != null && audioFile.exists()) {
                System.out.println("[" + Thread.currentThread().getName() + "] Audio fetched successfully! Feeding to Gemini...");
                finalSummaryResult = aiSummarizerService.generateAudioSummary(audioFile);

                if (audioFile.delete()) {
                    System.out.println("[Audio-Engine] Temporary audio file cleaned safely.");
                }
            } else {
                finalSummaryResult = "### ⚠️ Pipeline Failure\nCloud deployment par audio processing fetch execution limit exceed ho gayi.";
            }

            // Database targets update loop
            job.setSummary(finalSummaryResult);
            job.setStatus(finalSummaryResult.contains("### ⚠️ Pipeline Failure") ? JobStatus.FAILED : JobStatus.COMPLETED);
            videoJobRepository.save(job);
            System.out.println("[" + Thread.currentThread().getName() + "] Job completed with status: " + job.getStatus());

        } catch (Exception e) {
            System.err.println("Critical failure for job " + jobId + ": " + e.getMessage());
            markJobFailed(jobId, e.getMessage());
        }
    }

    @Override
    @Async("videoJobExecutor")
    public void processVideoAsynchronously(UUID jobId, String transcript) {
        processVideoAsynchronously(jobId);
    }

    private void markJobFailed(UUID jobId, String reason) {
        videoJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.FAILED);
            job.setSummary("### ⚠️ Processing Failed\n" + reason);
            videoJobRepository.save(job);
        });
    }
}
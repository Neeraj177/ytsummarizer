package com.neeraj.ytsummarizer.service.serviceImpl;

import com.neeraj.ytsummarizer.entity.VideoJob;
import com.neeraj.ytsummarizer.model.enums.JobStatus;
import com.neeraj.ytsummarizer.repository.VideoJobRepository;
import com.neeraj.ytsummarizer.service.AiSummarizerService;
import com.neeraj.ytsummarizer.service.VideoAsyncWorker;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
    public void processVideoAsynchronously(UUID jobId, String transcript) {
        try {
            VideoJob job = videoJobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

            job.setStatus(JobStatus.PROCESSING);
            videoJobRepository.save(job);

            String finalSummaryResult;

            if (transcript != null && !transcript.trim().isEmpty()) {
                System.out.println("[Worker] Frontend transcript received! Length: " + transcript.length());
                finalSummaryResult = aiSummarizerService.generateSummary(transcript);
            } else {
                String liveTranscript = com.neeraj.ytsummarizer.util.YouTubeTranscriptExtractor
                        .getTranscriptByVideoId(job.getVideoId());

                if (liveTranscript != null && !liveTranscript.trim().isEmpty()) {
                    System.out.println("[Worker] Backend transcript found! Length: " + liveTranscript.length());
                    finalSummaryResult = aiSummarizerService.generateSummary(liveTranscript);
                } else {
                    System.out.println("[Worker] No transcript found anywhere. Sending YouTube URL directly to Gemini...");
                    String youtubeUrl = "https://www.youtube.com/watch?v=" + job.getVideoId();
                    finalSummaryResult = aiSummarizerService.generateSummary(youtubeUrl);
                }
            }

            job.setSummary(finalSummaryResult);
            job.setStatus(finalSummaryResult.contains("### ⚠️ Pipeline Failure")
                    ? JobStatus.FAILED : JobStatus.COMPLETED);
            videoJobRepository.save(job);
            System.out.println("[Worker] Job " + jobId + " → " + job.getStatus());

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
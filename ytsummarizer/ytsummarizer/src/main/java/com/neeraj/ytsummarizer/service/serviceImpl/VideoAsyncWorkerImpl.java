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
                    .getTranscriptByVideoId(job.getVideoId());

            String finalSummaryResult;

            if (transcript != null && !transcript.trim().isEmpty()) {
                // ✅ Scenario A: Captions mile (CC ya Auto-generated)
                System.out.println("[" + Thread.currentThread().getName() + "] Captions found! Sending to Gemini...");
                finalSummaryResult = aiSummarizerService.generateSummary(transcript);
            } else {
                // ✅ Scenario B: No captions → Frame extraction
                System.out.println("[" + Thread.currentThread().getName() + "] No captions. Starting frame extraction...");

                List<File> extractedFrames = com.neeraj.ytsummarizer.util.VideoVisionExtractor
                        .extractVideoFrames(job.getVideoId());

                if (extractedFrames != null && !extractedFrames.isEmpty()) {
                    System.out.println("[" + Thread.currentThread().getName() + "] Sending "
                            + extractedFrames.size() + " frames to Gemini Vision...");
                    finalSummaryResult = aiSummarizerService.generateVisualSummary(extractedFrames);
                    com.neeraj.ytsummarizer.util.VideoVisionExtractor.cleanUpFrames(extractedFrames);
                } else {
                    finalSummaryResult = "### ⚠️ Pipeline Failure\nFrames extract nahi ho paaye aur captions bhi nahi mile.";
                }
            }

            job.setSummary(finalSummaryResult);
            job.setStatus(finalSummaryResult.contains("### ⚠️ Pipeline Failure")
                    ? JobStatus.FAILED : JobStatus.COMPLETED);
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
                // ✅ Frontend se transcript aaya — seedha Gemini ko do
                System.out.println("[Worker] Frontend transcript received! Length: " + transcript.length());
                finalSummaryResult = aiSummarizerService.generateSummary(transcript);
            } else {
                // Backend se extract karo
                String liveTranscript = com.neeraj.ytsummarizer.util.YouTubeTranscriptExtractor
                        .getTranscriptByVideoId(job.getVideoId());

                if (liveTranscript != null && !liveTranscript.trim().isEmpty()) {
                    System.out.println("[Worker] Backend transcript found! Length: " + liveTranscript.length());
                    finalSummaryResult = aiSummarizerService.generateSummary(liveTranscript);
                } else {
                    // Frame extraction
                    System.out.println("[Worker] No transcript. Starting frame extraction...");
                    List<File> extractedFrames = com.neeraj.ytsummarizer.util.VideoVisionExtractor
                            .extractVideoFrames(job.getVideoId());

                    if (extractedFrames != null && !extractedFrames.isEmpty()) {
                        finalSummaryResult = aiSummarizerService.generateVisualSummary(extractedFrames);
                        com.neeraj.ytsummarizer.util.VideoVisionExtractor.cleanUpFrames(extractedFrames);
                    } else {
                        finalSummaryResult = "### ⚠️ Pipeline Failure\nFrames extract nahi ho paaye.";
                    }
                }
            }

            job.setSummary(finalSummaryResult);
            job.setStatus(finalSummaryResult.contains("### ⚠️ Pipeline Failure")
                    ? JobStatus.FAILED : JobStatus.COMPLETED);
            videoJobRepository.save(job);

        } catch (Exception e) {
            System.err.println("Critical failure for job " + jobId + ": " + e.getMessage());
            markJobFailed(jobId, e.getMessage());
        }
    }
}
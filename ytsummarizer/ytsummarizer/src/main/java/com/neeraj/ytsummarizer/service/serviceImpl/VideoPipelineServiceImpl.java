package com.neeraj.ytsummarizer.service.serviceImpl;

import com.neeraj.ytsummarizer.dto.JobStatusResponse;
import com.neeraj.ytsummarizer.entity.VideoJob;
import com.neeraj.ytsummarizer.model.enums.JobStatus;
import com.neeraj.ytsummarizer.repository.VideoJobRepository;
import com.neeraj.ytsummarizer.service.VideoAsyncWorker; // Added import
import com.neeraj.ytsummarizer.service.VideoPipelineService;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VideoPipelineServiceImpl implements VideoPipelineService {

    private final VideoJobRepository videoJobRepository;
    private final VideoAsyncWorker asyncWorker; // Added Dependency

    // Updated Constructor Injection
    public VideoPipelineServiceImpl(VideoJobRepository videoJobRepository, VideoAsyncWorker asyncWorker) {
        this.videoJobRepository = videoJobRepository;
        this.asyncWorker = asyncWorker;
    }

    @Override
    public JobStatusResponse initializeJob(String youtubeUrl, UUID userId) {
        String videoId = extractVideoId(youtubeUrl);

        VideoJob job = VideoJob.builder()
                .userId(userId)
                .youtubeUrl(youtubeUrl)
                .videoId(videoId)
                .status(JobStatus.PENDING)
                .build();

        VideoJob savedJob = videoJobRepository.save(job);

        // KICK OFF ASYNC THREAD WORKER RUNTIME HERE
        asyncWorker.processVideoAsynchronously(savedJob.getId());

        return JobStatusResponse.builder()
                .jobId(savedJob.getId())
                .videoId(savedJob.getVideoId())
                .status(savedJob.getStatus())
                .summary(savedJob.getSummary())
                .build();
    }

    @Override
    public JobStatusResponse getJobStatus(UUID jobId) {
        VideoJob job = videoJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Video job processing task not found for ID: " + jobId));

        return JobStatusResponse.builder()
                .jobId(job.getId())
                .videoId(job.getVideoId())
                .status(job.getStatus())
                .summary(job.getSummary())
                .build();
    }

    private String extractVideoId(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("YouTube link cannot be null or empty");
        }
        String pattern = "(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/(?:[^\\/\\n\\s]+\\/\\S+\\/|(?:v|e(?:mbed)?)\\/|\\S*?[?&]v=)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);

        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Invalid YouTube URL structure provided");
    }
}
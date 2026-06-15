package com.neeraj.ytsummarizer.service;

import com.neeraj.ytsummarizer.dto.JobStatusResponse;
import java.util.UUID;

public interface VideoPipelineService {
    JobStatusResponse initializeJob(String youtubeUrl, UUID userId);
    JobStatusResponse getJobStatus(UUID jobId);
}
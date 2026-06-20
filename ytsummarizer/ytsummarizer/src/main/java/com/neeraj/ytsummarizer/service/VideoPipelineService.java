package com.neeraj.ytsummarizer.service;

import com.neeraj.ytsummarizer.dto.JobStatusResponse;
import java.util.UUID;

public interface VideoPipelineService {

    JobStatusResponse getJobStatus(UUID jobId);
    JobStatusResponse initializeJob(String youtubeUrl, String transcript, UUID userId);
}
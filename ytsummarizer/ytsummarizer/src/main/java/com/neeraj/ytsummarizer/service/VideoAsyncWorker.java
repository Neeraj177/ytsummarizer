package com.neeraj.ytsummarizer.service;

import java.util.UUID;

public interface VideoAsyncWorker {
    void processVideoAsynchronously(UUID jobId);
    void processVideoAsynchronously(UUID jobId, String transcript);
}
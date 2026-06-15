package com.neeraj.ytsummarizer.controller;

import com.neeraj.ytsummarizer.dto.JobStatusResponse;
import com.neeraj.ytsummarizer.dto.VideoRequest;
import com.neeraj.ytsummarizer.service.VideoPipelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "http://localhost:5173") // Connects perfectly to your local Vite + React app
public class VideoController {

    private final VideoPipelineService pipelineService;

    // Constructor Injection ( cleaner and preferred over @Autowired )
    public VideoController(VideoPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @PostMapping("/process")
    public ResponseEntity<JobStatusResponse> processVideo(@RequestBody VideoRequest request) {
        // Dummy user ID for Phase 1 (We will wire actual User Accounts up to this in Phase 4)
        UUID defaultUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        // Initialize the job record in PostgreSQL and kick off the async processing thread
        JobStatusResponse response = pipelineService.initializeJob(request.getUrl(), defaultUserId);

        // Return 202 Accepted because the heavy lifting happens asynchronously in the background
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable UUID jobId) {
        // Allows your React app to poll and track if status changes from PROCESSING to COMPLETED
        JobStatusResponse response = pipelineService.getJobStatus(jobId);
        return ResponseEntity.ok(response);
    }
}
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
        UUID defaultUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        JobStatusResponse response = pipelineService.initializeJob(request.getUrl(), request.getTranscript(), defaultUserId);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable UUID jobId) {
        // Allows your React app to poll and track if status changes from PROCESSING to COMPLETED
        JobStatusResponse response = pipelineService.getJobStatus(jobId);
        return ResponseEntity.ok(response);
    }
}
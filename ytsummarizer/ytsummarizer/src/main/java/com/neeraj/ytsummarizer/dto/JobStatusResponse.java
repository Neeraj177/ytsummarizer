package com.neeraj.ytsummarizer.dto;
// Make sure this matches your package structure
import com.neeraj.ytsummarizer.model.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusResponse {
    private UUID jobId;
    private String videoId;
    private JobStatus status;
    private String summary;
}
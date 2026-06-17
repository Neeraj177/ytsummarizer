package com.neeraj.ytsummarizer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoRequest {
    private String url;
    private String transcript; // ← Yeh add karo
}
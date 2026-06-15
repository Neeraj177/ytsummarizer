package com.neeraj.ytsummarizer.repository;

import com.neeraj.ytsummarizer.entity.VideoJob;
import com.neeraj.ytsummarizer.entity.VideoJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VideoJobRepository extends JpaRepository<VideoJob, UUID> {
    // Standard CRUD operations are inherited automatically from JpaRepository
}
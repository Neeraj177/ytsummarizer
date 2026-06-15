package com.neeraj.ytsummarizer.repository;

import com.neeraj.ytsummarizer.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    // Job ID ke basis par saari messages ascending order (time wise) me fetch karega
    List<ChatMessage> findByVideoJobIdOrderByCreatedAtAsc(UUID jobId);
}
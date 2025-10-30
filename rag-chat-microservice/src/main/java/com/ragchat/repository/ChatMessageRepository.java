package com.ragchat.repository;

import com.ragchat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByTimestampAsc(Long sessionId);

    Page<ChatMessage> findBySessionIdOrderByTimestampAsc(Long sessionId, Pageable pageable);

    Long countBySessionId(Long sessionId);
}
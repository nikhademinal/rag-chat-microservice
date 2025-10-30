package com.ragchat.repository;

import com.ragchat.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUserId(String userId);

    Page<ChatSession> findByUserId(String userId, Pageable pageable);

    List<ChatSession> findByUserIdAndIsFavorite(String userId, Boolean isFavorite);

    Optional<ChatSession> findByIdAndUserId(Long id, String userId);

    void deleteByIdAndUserId(Long id, String userId);
}
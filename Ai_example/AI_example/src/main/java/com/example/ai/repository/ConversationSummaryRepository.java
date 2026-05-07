package com.example.ai.repository;

import com.example.ai.entity.ConversationSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationSummaryRepository extends JpaRepository<ConversationSummary, Long> {

    Optional<ConversationSummary> findBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);

    boolean existsBySessionId(Long sessionId);
}

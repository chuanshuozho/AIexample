package com.example.ai.repository;

import com.example.ai.entity.ConversationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {

    List<ConversationSession> findAllByOrderByUpdateTimeDesc();

    Optional<ConversationSession> findTopByOrderByUpdateTimeDesc();
}

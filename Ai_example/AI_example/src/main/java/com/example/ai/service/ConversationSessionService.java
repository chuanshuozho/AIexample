package com.example.ai.service;

import com.example.ai.entity.ConversationSession;
import com.example.ai.repository.ConversationSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ConversationSessionService {

    private final ConversationSessionRepository sessionRepository;

    public ConversationSessionService(ConversationSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public ConversationSession createSession() {
        ConversationSession session = new ConversationSession();
        return sessionRepository.save(session);
    }

    public List<ConversationSession> getAllSessions() {
        return sessionRepository.findAllByOrderByUpdateTimeDesc();
    }

    public Optional<ConversationSession> getLatestSession() {
        return sessionRepository.findTopByOrderByUpdateTimeDesc();
    }

    public void updateSessionOnNewMessage(Long sessionId, String userMessage) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setUpdateTime(LocalDateTime.now());
            if (session.getTitle() == null || session.getTitle().isEmpty()) {
                String title = userMessage.length() > 30
                        ? userMessage.substring(0, 30)
                        : userMessage;
                session.setTitle(title);
            }
            sessionRepository.save(session);
        });
    }

    public void deleteSession(Long sessionId) {
        sessionRepository.deleteById(sessionId);
    }
}

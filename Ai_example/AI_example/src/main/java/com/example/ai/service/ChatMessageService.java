package com.example.ai.service;

import com.example.ai.entity.ChatMessage;
import com.example.ai.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public ChatMessage saveMessage(String userMessage, String assistantReply) {
        ChatMessage message = new ChatMessage(userMessage, assistantReply);
        return chatMessageRepository.save(message);
    }

    public ChatMessage saveMessage(Long sessionId, String userMessage, String assistantReply) {
        ChatMessage message = new ChatMessage(userMessage, assistantReply);
        message.setSessionId(sessionId);
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getAllMessages() {
        return chatMessageRepository.findAllByOrderByCreateTimeAsc();
    }

    public List<ChatMessage> getMessagesBySessionId(Long sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
    }

    public void deleteMessagesBySessionId(Long sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
    }
}

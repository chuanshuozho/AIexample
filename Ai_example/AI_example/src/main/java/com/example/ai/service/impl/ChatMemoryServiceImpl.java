package com.example.ai.service.impl;

import com.example.ai.entity.ChatMessage;
import com.example.ai.repository.ChatMessageRepository;
import com.example.ai.service.ChatMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatMemoryServiceImpl implements ChatMemoryService {
    
    private final ChatMessageRepository chatMessageRepository;
    
    @Autowired
    public ChatMemoryServiceImpl(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }
    
    @Override
    public List<ChatMessage> getHistory(Long sessionId) {
        if (sessionId == null) {
            return new ArrayList<>();
        }
        return chatMessageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
    }
    
    @Override
    public List<ChatMessage> getHistoryWithTokenLimit(Long sessionId, int maxTokens) {
        List<ChatMessage> history = getHistory(sessionId);
        if (history.isEmpty()) {
            return history;
        }
        
        // Truncate from the beginning to keep most recent messages
        List<ChatMessage> truncated = new ArrayList<>();
        int totalTokens = 0;
        
        // Iterate from the end (most recent) to beginning
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            int msgTokens = countTokens(msg.getUserMessage()) + countTokens(msg.getAssistantReply());
            
            if (totalTokens + msgTokens <= maxTokens) {
                truncated.add(0, msg); // Add to beginning to maintain order
                totalTokens += msgTokens;
            } else {
                break;
            }
        }
        
        return truncated;
    }
    
    @Override
    @Transactional
    public void saveMessage(Long sessionId, String userMessage, String assistantReply) {
        ChatMessage message = new ChatMessage(userMessage, assistantReply);
        message.setSessionId(sessionId);
        chatMessageRepository.save(message);
    }
    
    @Override
    @Transactional
    public void clearHistory(Long sessionId) {
        if (sessionId != null) {
            chatMessageRepository.deleteBySessionId(sessionId);
        }
    }
    
    @Override
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Approximate: ~4 characters per token for Chinese, ~0.75 words per token for English
        // Using a simple approximation: count Chinese characters + English words * 1.33
        int chineseChars = 0;
        int englishWords = 0;
        boolean inWord = false;
        
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseChars++;
                inWord = false;
            } else if (Character.isLetter(c)) {
                if (!inWord) {
                    englishWords++;
                    inWord = true;
                }
            } else {
                inWord = false;
            }
        }
        
        // Chinese: ~1 token per character, English: ~1.33 tokens per word
        return chineseChars + (int)(englishWords * 1.33);
    }
}

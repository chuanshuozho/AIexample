package com.example.ai.service;

import com.example.ai.entity.ChatMessage;
import java.util.List;

public interface ChatMemoryService {
    
    /**
     * Get conversation history for a session
     * @param sessionId Session ID
     * @return List of chat messages
     */
    List<ChatMessage> getHistory(Long sessionId);
    
    /**
     * Get conversation history with token limit
     * @param sessionId Session ID
     * @param maxTokens Maximum token count
     * @return Truncated list of chat messages
     */
    List<ChatMessage> getHistoryWithTokenLimit(Long sessionId, int maxTokens);
    
    /**
     * Save a message to conversation history
     * @param sessionId Session ID
     * @param userMessage User message
     * @param assistantReply AI assistant reply
     */
    void saveMessage(Long sessionId, String userMessage, String assistantReply);
    
    /**
     * Clear conversation history for a session
     * @param sessionId Session ID
     */
    void clearHistory(Long sessionId);
    
    /**
     * Count tokens in text (approximate)
     * @param text Text to count
     * @return Approximate token count
     */
    int countTokens(String text);
}

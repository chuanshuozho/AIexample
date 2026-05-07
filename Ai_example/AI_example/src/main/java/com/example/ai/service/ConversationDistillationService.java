package com.example.ai.service;

import com.example.ai.entity.ChatMessage;
import java.util.List;

/**
 * Service for conversation distillation - compressing conversation history
 * by generating summaries of older messages while preserving recent context.
 */
public interface ConversationDistillationService {
    
    /**
     * Get conversation history with distillation applied.
     * If the history exceeds the token threshold, older messages are
     * replaced with a summary while recent messages are preserved.
     * 
     * @param sessionId Session ID
     * @return Distilled conversation history (summary + recent messages)
     */
    List<ChatMessage> getHistoryWithDistillation(Long sessionId);
    
    /**
     * Perform distillation on a conversation session.
     * Generates or updates the summary for older messages.
     * 
     * @param sessionId Session ID
     * @return true if distillation was performed, false otherwise
     */
    boolean distillConversation(Long sessionId);
    
    /**
     * Get existing summary or create a new one for the given messages.
     * 
     * @param sessionId Session ID
     * @param messages Messages to summarize
     * @return The summary text
     */
    String getOrCreateSummary(Long sessionId, List<ChatMessage> messages);
    
    /**
     * Count total tokens in a list of messages.
     * 
     * @param messages List of chat messages
     * @return Total token count
     */
    int countTotalTokens(List<ChatMessage> messages);
    
    /**
     * Check if a conversation session needs distillation.
     * 
     * @param sessionId Session ID
     * @return true if distillation is needed, false otherwise
     */
    boolean needsDistillation(Long sessionId);
}

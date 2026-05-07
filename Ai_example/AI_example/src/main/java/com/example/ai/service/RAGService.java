package com.example.ai.service;

import com.example.ai.dto.RAGResponse;
import com.example.ai.entity.ChatMessage;

import java.util.List;

public interface RAGService {
    
    /**
     * RAG-enhanced chat
     * @param sessionId Session ID
     * @param question User question
     * @return RAG response with reply and sources
     */
    RAGResponse chatWithRAG(Long sessionId, String question);
    
    /**
     * Build prompt with context
     * @param systemPrompt System prompt
     * @param context Retrieval context
     * @param history Conversation history
     * @param question User question
     * @return Complete prompt
     */
    String buildPrompt(String systemPrompt, String context, List<ChatMessage> history, String question);
}

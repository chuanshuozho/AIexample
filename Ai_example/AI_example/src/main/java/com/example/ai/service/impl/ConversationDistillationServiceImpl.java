package com.example.ai.service.impl;

import com.example.ai.config.DistillationConfig;
import com.example.ai.entity.ChatMessage;
import com.example.ai.entity.ConversationSummary;
import com.example.ai.repository.ChatMessageRepository;
import com.example.ai.repository.ConversationSummaryRepository;
import com.example.ai.service.ChatMemoryService;
import com.example.ai.service.ConversationDistillationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class ConversationDistillationServiceImpl implements ConversationDistillationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationDistillationServiceImpl.class);
    
    private final ChatMemoryService chatMemoryService;
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationSummaryRepository summaryRepository;
    private final DistillationConfig distillationConfig;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String baseUrl;
    
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    
    private static final String SUMMARIZATION_SYSTEM_PROMPT = 
            "你是一个对话摘要助手。请将以下对话历史压缩成一个简洁的摘要，保留关键信息：主要话题、重要决定、用户偏好等。摘要应该简洁但完整，便于理解后续对话。";
    
    @Autowired
    public ConversationDistillationServiceImpl(
            ChatMemoryService chatMemoryService,
            ChatMessageRepository chatMessageRepository,
            ConversationSummaryRepository summaryRepository,
            DistillationConfig distillationConfig) {
        this.chatMemoryService = chatMemoryService;
        this.chatMessageRepository = chatMessageRepository;
        this.summaryRepository = summaryRepository;
        this.distillationConfig = distillationConfig;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public List<ChatMessage> getHistoryWithDistillation(Long sessionId) {
        if (sessionId == null) {
            return new ArrayList<>();
        }
        
        // Check if distillation is enabled
        if (!distillationConfig.isEnabled()) {
            return chatMemoryService.getHistory(sessionId);
        }
        
        // Check if distillation is needed
        if (!needsDistillation(sessionId)) {
            return chatMemoryService.getHistory(sessionId);
        }
        
        // Perform distillation
        List<ChatMessage> fullHistory = chatMemoryService.getHistory(sessionId);
        int preserveCount = distillationConfig.getPreserveRecentPairs() * 2;
        
        // If history is smaller than preserve count, return as is
        if (fullHistory.size() <= preserveCount) {
            return fullHistory;
        }
        
        // Get messages to summarize (older messages)
        List<ChatMessage> toSummarize = fullHistory.subList(0, fullHistory.size() - preserveCount);
        List<ChatMessage> recentMessages = fullHistory.subList(fullHistory.size() - preserveCount, fullHistory.size());
        
        // Get or create summary
        String summary = getOrCreateSummary(sessionId, toSummarize);
        
        // Build distilled history: summary message + recent messages
        List<ChatMessage> distilledHistory = new ArrayList<>();
        
        // Add summary as a system-like message
        if (summary != null && !summary.isEmpty()) {
            ChatMessage summaryMessage = new ChatMessage(
                    "[历史对话摘要]", 
                    summary
            );
            summaryMessage.setSessionId(sessionId);
            distilledHistory.add(summaryMessage);
        }
        
        // Add recent messages
        distilledHistory.addAll(recentMessages);
        
        return distilledHistory;
    }
    
    @Override
    @Transactional
    public boolean distillConversation(Long sessionId) {
        if (sessionId == null || !distillationConfig.isEnabled()) {
            return false;
        }
        
        List<ChatMessage> fullHistory = chatMemoryService.getHistory(sessionId);
        int preserveCount = distillationConfig.getPreserveRecentPairs() * 2;
        
        if (fullHistory.size() <= preserveCount) {
            logger.debug("History size {} is less than preserve count {}, skipping distillation", 
                    fullHistory.size(), preserveCount);
            return false;
        }
        
        List<ChatMessage> toSummarize = fullHistory.subList(0, fullHistory.size() - preserveCount);
        String summary = generateSummary(toSummarize);
        
        if (summary == null || summary.isEmpty()) {
            logger.warn("Failed to generate summary for session {}", sessionId);
            return false;
        }
        
        // Save or update summary
        saveOrUpdateSummary(sessionId, toSummarize, summary);
        
        return true;
    }
    
    @Override
    public String getOrCreateSummary(Long sessionId, List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        
        // Check for existing summary
        Optional<ConversationSummary> existingSummary = summaryRepository.findBySessionId(sessionId);
        
        if (existingSummary.isPresent()) {
            ConversationSummary summary = existingSummary.get();
            // Check if summary is still valid (covers the same messages)
            Long lastMessageId = messages.get(messages.size() - 1).getId();
            if (summary.getLastMessageId() != null && 
                    summary.getLastMessageId().equals(lastMessageId)) {
                return summary.getSummary();
            }
        }
        
        // Generate new summary
        String summary = generateSummary(messages);
        
        if (summary != null && !summary.isEmpty()) {
            saveOrUpdateSummary(sessionId, messages, summary);
        }
        
        return summary != null ? summary : "";
    }
    
    @Override
    public int countTotalTokens(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        
        int totalTokens = 0;
        for (ChatMessage msg : messages) {
            totalTokens += chatMemoryService.countTokens(msg.getUserMessage());
            totalTokens += chatMemoryService.countTokens(msg.getAssistantReply());
        }
        
        return totalTokens;
    }
    
    @Override
    public boolean needsDistillation(Long sessionId) {
        if (sessionId == null || !distillationConfig.isEnabled()) {
            return false;
        }
        
        List<ChatMessage> history = chatMemoryService.getHistory(sessionId);
        int totalTokens = countTotalTokens(history);
        
        return totalTokens > distillationConfig.getTokenThreshold();
    }
    
    /**
     * Generate a summary for the given messages using DeepSeek API.
     */
    private String generateSummary(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        
        // Build the prompt from messages
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("以下是之前的对话历史，请生成一个简洁的摘要：\n\n");
        
        for (ChatMessage msg : messages) {
            promptBuilder.append("用户: ").append(msg.getUserMessage()).append("\n");
            promptBuilder.append("助手: ").append(msg.getAssistantReply()).append("\n\n");
        }
        
        String prompt = promptBuilder.toString();
        
        return callSummarizationAPI(prompt);
    }
    
    /**
     * Call DeepSeek API to generate summary.
     */
    private String callSummarizationAPI(String prompt) {
        String url = baseUrl + "/v1/chat/completions";
        
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", distillationConfig.getSummaryModel());
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", distillationConfig.getMaxSummaryTokens());
            
            ArrayNode messages = requestBody.putArray("messages");
            
            // System message
            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", SUMMARIZATION_SYSTEM_PROMPT);
            
            // User message with conversation history
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    logger.error("Summarization API call failed: {} - {}", response.code(), errorBody);
                    return null;
                }
                
                String responseBody = response.body().string();
                return parseSummaryResponse(responseBody);
            }
        } catch (IOException e) {
            logger.error("Failed to call summarization API: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Parse the API response to extract the summary.
     */
    private String parseSummaryResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText();
        }
        
        logger.warn("Invalid summary response format");
        return null;
    }
    
    /**
     * Save or update the summary for a session.
     */
    @Transactional
    private void saveOrUpdateSummary(Long sessionId, List<ChatMessage> messages, String summary) {
        Optional<ConversationSummary> existingSummary = summaryRepository.findBySessionId(sessionId);
        
        ConversationSummary summaryEntity;
        if (existingSummary.isPresent()) {
            summaryEntity = existingSummary.get();
            summaryEntity.setSummary(summary);
        } else {
            summaryEntity = new ConversationSummary(sessionId, summary);
        }
        
        // Set metadata
        if (!messages.isEmpty()) {
            summaryEntity.setLastMessageId(messages.get(messages.size() - 1).getId());
        }
        
        int originalTokens = countTotalTokens(messages);
        int summaryTokens = chatMemoryService.countTokens(summary);
        
        summaryEntity.setOriginalTokenCount(originalTokens);
        summaryEntity.setSummaryTokenCount(summaryTokens);
        
        summaryRepository.save(summaryEntity);
        
        logger.info("Saved summary for session {}: {} original tokens -> {} summary tokens", 
                sessionId, originalTokens, summaryTokens);
    }
}

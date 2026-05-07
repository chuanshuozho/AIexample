package com.example.ai.service.impl;

import com.example.ai.dto.RAGResponse;
import com.example.ai.dto.RetrievalResult;
import com.example.ai.dto.SourceReference;
import com.example.ai.entity.ChatMessage;
import com.example.ai.service.ChatMemoryService;
import com.example.ai.service.ConversationDistillationService;
import com.example.ai.service.RAGService;
import com.example.ai.service.RetrievalService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RAGServiceImpl implements RAGService {
    
    private final ChatMemoryService chatMemoryService;
    private final RetrievalService retrievalService;
    private final ConversationDistillationService distillationService;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String baseUrl;
    
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String chatModel;
    
    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private double temperature;
    
    @Value("${app.knowledge.retrieval.top-k:3}")
    private int maxContextChunks;
    
    @Value("${app.knowledge.retrieval.max-context-tokens:2000}")
    private int maxContextTokens;
    
    private static final String SYSTEM_PROMPT = "你是AI助手，请基于知识库内容回答问题，没有相关信息请说明。回答简洁专业。";
    
    @Autowired
    public RAGServiceImpl(ChatMemoryService chatMemoryService, 
                          RetrievalService retrievalService,
                          ConversationDistillationService distillationService) {
        this.chatMemoryService = chatMemoryService;
        this.retrievalService = retrievalService;
        this.distillationService = distillationService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public RAGResponse chatWithRAG(Long sessionId, String question) {
        // 使用蒸馏后的历史（如果启用）
        List<ChatMessage> history = distillationService.getHistoryWithDistillation(sessionId);
        
        // 搜索知识库（限制结果数量）
        List<RetrievalResult> retrievalResults = retrievalService.search(question, maxContextChunks, 0.5);
        
        // 构建上下文（限制 Token 数量）
        String context = buildContext(retrievalResults);
        
        // 调用 API（使用正确的消息格式，包含历史）
        String reply = callChatApiWithContext(question, context, history);
        
        // 保存消息
        if (sessionId != null) {
            chatMemoryService.saveMessage(sessionId, question, reply);
        }
        
        // 构建来源引用
        List<SourceReference> sources = retrievalResults.stream()
                .map(r -> new SourceReference(r.getDocumentId(), r.getDocumentName(), 
                        r.getContent(), r.getSimilarityScore()))
                .collect(Collectors.toList());
        
        return new RAGResponse(reply, sources, !retrievalResults.isEmpty());
    }
    
    @Override
    public String buildPrompt(String systemPrompt, String context, List<ChatMessage> history, String question) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(systemPrompt).append("\n\n");
        if (context != null && !context.isEmpty()) {
            prompt.append("知识库内容：\n").append(context).append("\n\n");
        }
        prompt.append("问题：").append(question);
        return prompt.toString();
    }
    
    private String buildContext(List<RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        int estimatedTokens = 0;
        
        for (RetrievalResult r : results) {
            String chunk = String.format("【%s】%s", r.getDocumentName(), r.getContent());
            // 估算 Token 数量（粗略：1 Token ≈ 1.5 字符）
            int chunkTokens = chunk.length() / 2;
            
            if (estimatedTokens + chunkTokens > maxContextTokens) {
                break; // 超过限制，停止添加
            }
            
            context.append(chunk).append("\n\n");
            estimatedTokens += chunkTokens;
        }
        
        return context.toString();
    }
    
    /**
     * 使用正确的 API 格式调用，减少 Token 消耗
     */
    private String callChatApiWithContext(String question, String context, List<ChatMessage> history) {
        String url = baseUrl + "/v1/chat/completions";
        
        try {
            // 使用 ObjectMapper 构建 JSON
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", chatModel);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", 1500); // 限制输出长度
            
            ArrayNode messages = requestBody.putArray("messages");
            
            // 1. 系统消息 - 合并知识库上下文到一条消息
            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            String systemContent = SYSTEM_PROMPT;
            if (context != null && !context.isEmpty()) {
                systemContent += "\n\n相关知识库内容：\n" + context;
            }
            systemMsg.put("content", systemContent);
            
            // 2. 添加历史消息（蒸馏后的历史，可能包含摘要）
            if (history != null && !history.isEmpty()) {
                for (ChatMessage msg : history) {
                    // 检查是否是摘要消息
                    if ("[历史对话摘要]".equals(msg.getUserMessage())) {
                        // 摘要作为系统消息的一部分
                        ObjectNode summaryMsg = messages.addObject();
                        summaryMsg.put("role", "system");
                        summaryMsg.put("content", "历史对话摘要：" + msg.getAssistantReply());
                    } else {
                        // 常规历史消息
                        ObjectNode historyUserMsg = messages.addObject();
                        historyUserMsg.put("role", "user");
                        historyUserMsg.put("content", msg.getUserMessage());
                        
                        ObjectNode historyAssistantMsg = messages.addObject();
                        historyAssistantMsg.put("role", "assistant");
                        historyAssistantMsg.put("content", msg.getAssistantReply());
                    }
                }
            }
            
            // 3. 用户问题
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", question);
            
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
                    throw new RuntimeException("Chat API call failed: " + response.code() + " - " + errorBody);
                }
                
                String responseBody = response.body().string();
                return parseChatResponse(responseBody);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to call chat API: " + e.getMessage(), e);
        }
    }
    
    private String parseChatResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText();
        }
        
        throw new IOException("Invalid chat response format");
    }
}

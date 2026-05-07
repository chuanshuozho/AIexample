package com.example.ai.service.impl;

import com.example.ai.service.EmbeddingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, float[]> embeddingCache;
    
    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String baseUrl;
    
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.openai.embedding.options.model:deepseek-embedding}")
    private String embeddingModel;
    
    @Value("${app.knowledge.embedding-cache-size:1000}")
    private int cacheSize;
    
    public EmbeddingServiceImpl() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        this.embeddingCache = new ConcurrentHashMap<>();
    }
    
    @Override
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[0];
        }
        
        // Check cache first
        String cacheKey = text.hashCode() + "_" + text.length();
        if (embeddingCache.containsKey(cacheKey)) {
            return embeddingCache.get(cacheKey);
        }
        
        try {
            float[] embedding = callEmbeddingApi(text);
            
            // Cache the result
            if (embeddingCache.size() < cacheSize) {
                embeddingCache.put(cacheKey, embedding);
            }
            
            return embedding;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }
    
    @Override
    public int getDimensions() {
        // DeepSeek embedding dimension is typically 1536
        return 1536;
    }
    
    private float[] callEmbeddingApi(String text) throws IOException {
        String url = baseUrl + "/v1/embeddings";
        
        String jsonBody = String.format(
                "{\"model\":\"%s\",\"input\":\"%s\"}",
                embeddingModel,
                escapeJson(text)
        );
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("Embedding API call failed: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            return parseEmbeddingResponse(responseBody);
        }
    }
    
    private float[] parseEmbeddingResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode data = root.path("data");
        
        if (data.isArray() && data.size() > 0) {
            JsonNode embeddingNode = data.get(0).path("embedding");
            float[] embedding = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }
            return embedding;
        }
        
        throw new IOException("Invalid embedding response format");
    }
    
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

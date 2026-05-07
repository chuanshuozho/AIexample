package com.example.ai.service;

import com.example.ai.config.LlmConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AiService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public String chat(String userMessage) throws IOException {
        // 使用 ObjectMapper 构建 JSON，正确处理特殊字符转义
        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("model", "deepseek-v4-flash");
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000); // 限制输出 Token 数量，减少消耗

        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是AI助手，请简洁专业地回答问题。"); // 简化系统提示词

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

        String bodyJson = mapper.writeValueAsString(requestBody);

        RequestBody body = RequestBody.create(
                bodyJson,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(LlmConfig.API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + LlmConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new RuntimeException("LLM调用失败: " + response);
        }

        String result = response.body().string();
        JsonNode root = mapper.readTree(result);

        return root
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
    }
}

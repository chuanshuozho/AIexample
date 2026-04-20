package com.example.ai.service;

import com.example.ai.config.LlmConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
public class AiService {

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();


    public String chat(String userMessage) throws IOException {

        // 1. 构造请求体（核心）
        String bodyJson = """
        {
          "model": "deepseek-chat",
          "messages": [
            {
              "role": "system",
              "content": "你是一个服装专业客服助手，请简洁回答"
            },
            {
              "role": "user",
              "content": "%s"
            }
          ],
          "temperature": 0.7
        }
        """.formatted(userMessage);

        RequestBody body = RequestBody.create(
                bodyJson,
                MediaType.parse("application/json")
        );

        // 2. HTTP请求
        Request request = new Request.Builder()
                .url(LlmConfig.API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + LlmConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        // 3. 执行请求
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new RuntimeException("LLM调用失败: " + response);
        }

        // 4. 解析返回
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
package com.example.ai.service;

import com.example.ai.config.LlmConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        String bodyJson = "{\n" +
                "  \"model\": \"deepseek-v4-flash\",\n" +
                "  \"messages\": [\n" +
                "    {\n" +
                "      \"role\": \"system\",\n" +
                "      \"content\": \"你是一个全能AI助手，能够回答各类问题，包括编程、写作、翻译、数学、科学、生活常识等。请用专业、清晰、友好的方式回答用户的问题。\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"role\": \"user\",\n" +
                "      \"content\": \"" + userMessage.replace("\"", "\\\"").replace("\n", "\\n") + "\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"temperature\": 0.7\n" +
                "}";

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

package com.example.ai.controller;

import com.example.ai.dto.AiRequest;
import com.example.ai.dto.AiResponse;
import com.example.ai.entity.ChatMessage;
import com.example.ai.service.AiService;
import com.example.ai.service.ChatMessageService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class AiController {

    private final AiService aiService;
    private final ChatMessageService chatMessageService;

    public AiController(AiService aiService, ChatMessageService chatMessageService) {
        this.aiService = aiService;
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    @ResponseBody
    @PostMapping("/ai/chat")
    public AiResponse chat(@RequestBody AiRequest request) {
        try {
            String reply = aiService.chat(request.getMessage());
            // Save to database
            chatMessageService.saveMessage(request.getMessage(), reply);
            return new AiResponse(reply);
        } catch (Exception e) {
            return new AiResponse("调用失败: " + e.getMessage());
        }
    }

    @ResponseBody
    @GetMapping("/ai/history")
    public List<ChatMessage> getHistory() {
        return chatMessageService.getAllMessages();
    }
}
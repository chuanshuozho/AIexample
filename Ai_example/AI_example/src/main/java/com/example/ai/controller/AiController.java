package com.example.ai.controller;

import com.example.ai.dto.AiRequest;
import com.example.ai.dto.AiResponse;
import com.example.ai.service.AiService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
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
            return new AiResponse(reply);
        } catch (Exception e) {
            return new AiResponse("调用失败: " + e.getMessage());
        }
    }
}
package com.example.ai.controller;

import com.example.ai.dto.AiRequest;
import com.example.ai.dto.AiResponse;
import com.example.ai.entity.ChatMessage;
import com.example.ai.entity.ConversationSession;
import com.example.ai.service.AiService;
import com.example.ai.service.ChatMessageService;
import com.example.ai.service.ConversationSessionService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class AiController {

    private final AiService aiService;
    private final ChatMessageService chatMessageService;
    private final ConversationSessionService sessionService;

    public AiController(AiService aiService,
                        ChatMessageService chatMessageService,
                        ConversationSessionService sessionService) {
        this.aiService = aiService;
        this.chatMessageService = chatMessageService;
        this.sessionService = sessionService;
    }

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    @ResponseBody
    @PostMapping("/ai/sessions")
    public ConversationSession createSession() {
        return sessionService.createSession();
    }

    @ResponseBody
    @GetMapping("/ai/sessions")
    public List<ConversationSession> getSessions() {
        return sessionService.getAllSessions();
    }

    @ResponseBody
    @GetMapping("/ai/sessions/{sessionId}/messages")
    public List<ChatMessage> getSessionMessages(@PathVariable Long sessionId) {
        return chatMessageService.getMessagesBySessionId(sessionId);
    }

    @ResponseBody
    @DeleteMapping("/ai/sessions/{sessionId}")
    public void deleteSession(@PathVariable Long sessionId) {
        chatMessageService.deleteMessagesBySessionId(sessionId);
        sessionService.deleteSession(sessionId);
    }

    @ResponseBody
    @PostMapping("/ai/chat")
    public ResponseEntity<?> chat(@RequestBody AiRequest request) {
        try {
            Long sessionId = request.getSessionId();
            String reply = aiService.chat(request.getMessage());

            if (sessionId != null) {
                chatMessageService.saveMessage(sessionId, request.getMessage(), reply);
                sessionService.updateSessionOnNewMessage(sessionId, request.getMessage());
            } else {
                chatMessageService.saveMessage(request.getMessage(), reply);
            }

            return ResponseEntity.ok(new AiResponse(reply, sessionId));
        } catch (Exception e) {
            return ResponseEntity.ok(new AiResponse("调用失败: " + e.getMessage()));
        }
    }

    @ResponseBody
    @GetMapping("/ai/history")
    public List<ChatMessage> getHistory() {
        return chatMessageService.getAllMessages();
    }
}

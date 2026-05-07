package com.example.ai.controller;

import com.example.ai.dto.RAGRequest;
import com.example.ai.dto.RAGResponse;
import com.example.ai.service.RAGService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class RAGController {
    
    private final RAGService ragService;
    
    @Autowired
    public RAGController(RAGService ragService) {
        this.ragService = ragService;
    }
    
    @PostMapping("/rag")
    public RAGResponse chatWithRAG(@RequestBody RAGRequest request) {
        return ragService.chatWithRAG(request.getSessionId(), request.getQuestion());
    }
}

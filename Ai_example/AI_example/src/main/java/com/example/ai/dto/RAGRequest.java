package com.example.ai.dto;

public class RAGRequest {
    private Long sessionId;
    private String question;
    
    public RAGRequest() {}
    
    public RAGRequest(Long sessionId, String question) {
        this.sessionId = sessionId;
        this.question = question;
    }
    
    // Getters and Setters
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}
